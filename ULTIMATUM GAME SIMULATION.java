import java.util.*;

/**
 * ULTIMATUM GAME SIMULATION
 * ---------------------------------------------------------------------------
 * Deliberately built to feel similar tothe Prisoner's Dilemma simulation:
 * a Bot interface, a pile of top-level static HashMaps that hold all shared
 * state, and simple per-round bookkeeping in main(). The big structural
 * difference is WHAT each bot actually decides: instead of a single
 * decideMove(), every bot has two numbers -- baseOffer (BO) and
 * minimumAccept (MA) -- and "strategy" mostly means "how do BO/MA change
 * over time," not "what do I do this instant." Because of that, proposing
 * an offer / deciding whether to accept one are both trivial lookups for certain strategies
 *
 * VISIBILITY: totalEarnings / avgLoanerEarn / avgLoaneeEarn live in global
 * static maps (same as the PD reference's botEarnings map) for simplicity,
 * but by DESIGN every bot's strategy code is only ever allowed to look up
 * these maps using `currentOpponent` (never enumerate every bot's stats)
 * This mirrors how OpportunistA in the PD reference only ever read
 * botEarnings.get(currentOpponent). The one deliberate exception is the
 * Social Proof bot, which is intentionally uses public/aggregate
 * data (like the PD reference's opponentMoveHistory), not private earnings.
 * If you can't tell I'm big on always including some social proof / global bot
 * 
 * SOME JUDGMENT CALLS
 *   - Generous moves its underlying BO by true +5/-5 steps, but since BO
 *     always rounds to the nearest 10 when actually offered, that creates
 *     a deliberate buffer -- see rawGenerousBO and the Generous class.
 *   - Self Optimal's BO rule text had what looked like a copy/paste
 *     artifact. Implemented as: every 5 rounds, if BO*0.75 > avgLoanerEarn,
 *     increase BO by 10, else decrease BO by 10.
 *   - Master of One: "whichever [avg] is higher gets boosted by 10" is
 *     read as avgLoaneeEarn-higher -> boost MA, avgLoanerEarn-higher ->
 *     boost BO, since MA/loanee and BO/loaner are the natural pairings.
 *   - The Global Aggression Count toggle is implemented as ONE shared
 *     on/off switch (GLOBAL_AGGRESSION_ENABLED below), not a separate
 *     toggle per bot, since the spec only describes the mechanism once.
 * 
 * IMPORTANT
 *     In my opinion, there's still ALOT of issues with how this simulation, 
 *     and how the quota/global agressions counter skews certain bots to 
 *     too high of a base offer without allowing it to drop off later. A simple
 *     solution to this would be just removing the GAC, but then each bot would 
 *     need specific way to increase/decease their BO and MA numbers. Alternatively,
 *     considering different percentage thresolds, or completely changing the 
 *     requirements within the GAC, or even lowering the quota. Feel free to play
 *     around with the variables.
 *     
 */
public class UltimatumGameSimulation {

    // Bot interface
    interface Bot {
        String getName();

        // The starting BO/MA values, read once during setup. Fixed
        // constants for most bots; RandomBot rolls its own at construction.
        double initialBO();
        double initialMA();

        default void setOpponent(String opponentName) { }
        default void setRound(int currentRound, int totalRounds) { }

        // Called once per PAIRING, before either of the two games are
        // played, for bots that need to react to who they're facing
        // (Predator, Ratioed, Appeal to Average, Social Proof).
        default void onMatchStart() { }

        // Called once per bot per ROUND (after both of its games, if any,
        // are recorded) for bots with their own periodic check-ins
        // (Self Optimal, Master of One).
        default void onRoundComplete(int round) { }

        // Called right after this bot plays a game AS LOANER (proposer),
        // for bots that react to their own offer's fate immediately
        // (Humane, Greedy, Generous).
        default void onLoanerResult(boolean accepted) { }

        // Default decision logic: every bot just reads its own current
        // global BO/MA. This is intentional -- see the header comment.
        default double proposeOffer() { return currentBO.getOrDefault(getName(), 50.0); }
        default boolean decideAccept(double offerReceived) {
            return offerReceived >= currentMA.getOrDefault(getName(), 50.0);
        }
    }

    // Global static state (mirrors the PD reference's style)
    static Random rng = new Random();

    // Earnings bookkeeping
    static Map<String, Double> totalEarnings = new HashMap<>();
    static Map<String, Double> loanerEarnSum = new HashMap<>();
    static Map<String, Integer> loanerRounds = new HashMap<>();
    static Map<String, Double> loaneeEarnSum = new HashMap<>();
    static Map<String, Integer> loaneeRounds = new HashMap<>();

    // Every bot's current, live BO/MA -- this is what proposeOffer() /
    // decideAccept() actually read. All changes to these two maps are
    // GLOBAL (keyed only by the bot's own name), never opponent-specific.
    static Map<String, Double> currentBO = new HashMap<>();
    static Map<String, Double> currentMA = new HashMap<>();

    // Streak counters used by the Humane/Greedy-style "4 declines in a row
    // lower BO, 3 accepts in a row raise BO" mechanic.
    static Map<String, Integer> boDeclineStreak = new HashMap<>();
    static Map<String, Integer> boAcceptStreak = new HashMap<>();

    // Generous moves its BO by +5/-5, which doesn't line up with "BO always
    // rounds to the nearest 10." Rather than collapse that nuance away, we
    // track the true, unrounded value here; currentBO (what actually gets
    // offered) is always just clampBO(rawGenerousBO) -- see Generous below.
    // The "5" steps act as a buffer: it takes two same-direction results
    // in a row to actually move the offered amount by a full tier.
    static Map<String, Double> rawGenerousBO = new HashMap<>();

    // Rolling windows for the Global Aggression Count, reset every 10-round
    // checkpoint. "Proposer" = this bot's own offers being accepted/not;
    // "responder" = offers this bot received being accepted/not.
    static Map<String, Integer> proposerGamesWindow = new HashMap<>();
    static Map<String, Integer> proposerAcceptedWindow = new HashMap<>();
    static Map<String, Integer> responderGamesWindow = new HashMap<>();
    static Map<String, Integer> responderAcceptedWindow = new HashMap<>();

    // Social Proof ledger: PUBLIC, aggregate, anonymized behavioral data --
    // "what BO/MA has the field generally used when playing against bot X."
    // boUsedAgainst[X]  = BO amounts other bots offered TO X (X was loanee)
    // maUsedAgainst[X]  = MA thresholds other bots held while responding TO
    //                     X's offers (X was loaner)
    static Map<String, List<Double>> boUsedAgainst = new HashMap<>();
    static Map<String, List<Double>> maUsedAgainst = new HashMap<>();

    // Pairing-cap tracking, same idea as the PD reference's pairCounts.
    static Map<String, Map<String, Integer>> pairCounts = new HashMap<>();

    // How many times each unordered pair is allowed to be RANDOMLY PAIRED.
    // Each pairing event immediately plays 2 games (1 loaner, 1 loanee), so
    // 10 pairing events = 20 games = "10 times each way" per the spec.
    static int allowedPerPair = 10;

    // Global Aggression Count config
    static boolean GLOBAL_AGGRESSION_ENABLED = true; // single master switch
    static final double QUOTA_RATIO = 0.45; //Quota ratio
    static int quotaProposerRounds = 0; // (N-1) opponents * 10 proposer-games each, set in main()
    static double quotaTarget = 0.0;    // quotaProposerRounds * 100 * 0.90, set in main()
    static int totalRoundsGlobal = 0;   // set in main()
    static int currentSimRound = 0;     // updated once per round in main()

    // Round to the nearest multiple of `multiple` (e.g. 5 for MA, 10 for BO).
    static double roundToMultiple(double value, double multiple) {
        return Math.round(value / multiple) * multiple;
    }

    // MA: rounded to nearest 5, capped at 50, floored at 0.
    static double clampMA(double v) {
        double r = roundToMultiple(v, 5.0);
        return Math.max(0.0, Math.min(50.0, r));
    }

    // BO: rounded to nearest 10, floored at 50 ("can't go lower than 50"),
    // capped at 100 (can't offer more than the whole pie).
    static double clampBO(double v) {
        double r = roundToMultiple(v, 10.0);
        return Math.max(50.0, Math.min(100.0, r));
    }

    // All displayed/used averages are rounded to the nearest whole number,
    // per the spec -- used both for printing AND inside strategy logic,
    // so every bot reasons about the same clean numbers it can see.
    static double avgLoanerEarn(String name) {
        int rounds = loanerRounds.getOrDefault(name, 0);
        if (rounds == 0) return 0.0;
        return Math.round(loanerEarnSum.getOrDefault(name, 0.0) / rounds);
    }

    static double avgLoaneeEarn(String name) {
        int rounds = loaneeRounds.getOrDefault(name, 0);
        if (rounds == 0) return 0.0;
        return Math.round(loaneeEarnSum.getOrDefault(name, 0.0) / rounds);
    }

    static double average(List<Double> values) {
        double sum = 0;
        for (double d : values) sum += d;
        return sum / values.size();
    }

    // Is this bot's cumulative earnings on pace for 90% of quota by now? 
    // Can be changed from 90% since there is no specific reason the quota is set to that value 
    static boolean isAtOrAboveQuotaPace(double earnings, int round) {
        if (totalRoundsGlobal <= 0) return true;
        double expectedByNow = quotaTarget * ((double) round / totalRoundsGlobal);
        return earnings >= expectedByNow;
    }

    // Shared streak-based BO adjuster used by both Humane and Greedy:
    // 4 declines in a row -> BO down 10 (and every decline after that while
    // the streak holds); 3 accepts in a row -> BO up 10 (same idea).
    static void applyStreakBOAdjustment(String name, boolean accepted) {
        if (accepted) {
            int streak = boAcceptStreak.merge(name, 1, Integer::sum);
            boDeclineStreak.put(name, 0);
            if (streak >= 3) currentBO.put(name, clampBO(currentBO.get(name) + 10));
        } else {
            int streak = boDeclineStreak.merge(name, 1, Integer::sum);
            boAcceptStreak.put(name, 0);
            if (streak >= 4) currentBO.put(name, clampBO(currentBO.get(name) - 10));
        }
    }

    // The Global Aggression Count checkpoint, run for every bot every 10
    // rounds (if enabled). Only kicks in when the bot is BELOW its 90%
    // quota pace; if it's on pace, the window just resets quietly.
    static void applyGlobalAggression(String name) {
        if (!GLOBAL_AGGRESSION_ENABLED) return;

        double earnings = totalEarnings.getOrDefault(name, 0.0);
        if (isAtOrAboveQuotaPace(earnings, currentSimRound)) {
            resetAggressionWindow(name);
            return;
        }

        // MA condition: based on how often THIS bot accepted offers made
        // to it (as loanee) during the window.
        int rGames = responderGamesWindow.getOrDefault(name, 0);
        int rAcc = responderAcceptedWindow.getOrDefault(name, 0);
        if (rGames > 0) {
            double acceptRate = (double) rAcc / rGames;
            double ma = currentMA.get(name);
            if (acceptRate >= 0.75) currentMA.put(name, clampMA(ma + 5));
            else if (acceptRate <= 0.25) currentMA.put(name, clampMA(ma - 5));
        }

        // BO condition: based on how often THIS bot's own offers (as
        // loaner) were accepted during the window.
        int pGames = proposerGamesWindow.getOrDefault(name, 0);
        int pAcc = proposerAcceptedWindow.getOrDefault(name, 0);
        if (pGames > 0) {
            double acceptRate = (double) pAcc / pGames;
            double bo = currentBO.get(name);
            if (acceptRate < 0.5) {
                currentBO.put(name, clampBO(bo - 10));
            } else if (rng.nextBoolean()) {
                currentBO.put(name, clampBO(bo + 10)); // 50/50: raise it...
            } // ...or leave it alone.
        }

        resetAggressionWindow(name);
    }

    static void resetAggressionWindow(String name) {
        proposerGamesWindow.put(name, 0);
        proposerAcceptedWindow.put(name, 0);
        responderGamesWindow.put(name, 0);
        responderAcceptedWindow.put(name, 0);
    }

    // ==========================================================
    // Strategies
    // ==========================================================

    // #1 Humane: fair-ish fixed MA (moved only by Global Aggression);
    // BO reacts to real-time decline/accept streaks against the field.
    static class Humane implements Bot {
        public String getName() { return "Humane"; }
        public double initialMA() { return 35.0; }
        public double initialBO() { return 70.0; }
        public void onLoanerResult(boolean accepted) { applyStreakBOAdjustment(getName(), accepted); }
    }

    // #2 Random: rolls both numbers once, before anything starts. No
    // bespoke logic afterward -- Global Aggression is its only mover.
    static class RandomBot implements Bot {
        private final double startMA;
        private final double startBO;
        RandomBot() {
            startMA = clampMA(rng.nextDouble() * 50.0);       // 0-50
            startBO = clampBO(50.0 + rng.nextDouble() * 50.0); // 50-100
        }
        public String getName() { return "Random"; }
        public double initialMA() { return startMA; }
        public double initialBO() { return startBO; }
    }

    // #3 Generous: fixed MA; BO moves in true +5/-5 steps under the hood,
    // but only the rounded-to-nearest-10 value is ever actually offered.
    // Because 5 is exactly half of the rounding granularity, this creates
    // a natural buffer: it takes two accepts in a row to push the offer up
    // a full tier, and two declines in a row to pull it down one -- a
    // single result just fills or drains the buffer without changing what
    // gets offered next.
    static class Generous implements Bot {
        public String getName() { return "Generous"; }
        public double initialMA() { return 20.0; }
        public double initialBO() { return 50.0; }
        public void onLoanerResult(boolean accepted) {
            String name = getName();
            double raw = rawGenerousBO.getOrDefault(name, currentBO.get(name));
            raw = raw + (accepted ? 5.0 : -5.0);
            raw = Math.max(45.0, Math.min(100.0, raw)); // keep the buffer itself from wandering off forever
            rawGenerousBO.put(name, raw);
            currentBO.put(name, clampBO(raw)); // rounding (ties round up) is what actually moves the offer
        }
    }

    // #4 Appeal to Average: before each pairing, re-derives BOTH numbers
    // fresh from whatever it can currently see about the matched opponent
    // (their avgLoanerEarn / avgLoaneeEarn) -- same formula every time,
    // just fed different (opponent-visible-only) data each match.
    static class AppealToAverage implements Bot {
        private String currentOpponent;
        public String getName() { return "AppealToAverage"; }
        public double initialMA() { return 40.0; }
        public double initialBO() { return 60.0; }
        public void setOpponent(String opponentName) { this.currentOpponent = opponentName; }
        public void onMatchStart() {
            if (currentOpponent == null) return;
            int oppLoanerRounds = loanerRounds.getOrDefault(currentOpponent, 0);
            int oppLoaneeRounds = loaneeRounds.getOrDefault(currentOpponent, 0);
            double newMA = oppLoanerRounds > 0
                    ? clampMA(100.0 - avgLoanerEarn(currentOpponent))
                    : 40.0; // no data on this opponent yet -> fall back to default
            double newBO = oppLoaneeRounds > 0
                    ? clampBO(100.0 - avgLoaneeEarn(currentOpponent))
                    : 60.0;
            currentMA.put(getName(), newMA);
            currentBO.put(getName(), newBO);
        }
    }

    // #5 Greedy: same streak mechanic as Humane, just starting from
    // greedier numbers.
    static class Greedy implements Bot {
        public String getName() { return "Greedy"; }
        public double initialMA() { return 50.0; }
        public double initialBO() { return 80.0; }
        public void onLoanerResult(boolean accepted) { applyStreakBOAdjustment(getName(), accepted); }
    }

    // #6 Mr. Optimal: fixed numbers, no bespoke logic at all. Moves only
    // when Global Aggression kicks in -- a clean baseline.
    static class MrOptimal implements Bot {
        public String getName() { return "Mr. Optimal"; }
        public double initialMA() { return 40.0; }
        public double initialBO() { return 50.0; }
    }

    // #7 Mrs. Optimal: same idea as Mr. Optimal, different starting point.
    static class MrsOptimal implements Bot {
        public String getName() { return "Mrs. Optimal"; }
        public double initialMA() { return 30.0; }
        public double initialBO() { return 60.0; }
    }

    // #8 Self Optimal: every 5 rounds, checks its own recent averages and
    // adjusts BOTH numbers based purely on its own performance (never
    // looks at any specific opponent). Can be run with or without Global
    // Aggression Count also active -- see GLOBAL_AGGRESSION_ENABLED.
    static class SelfOptimal implements Bot {
        public String getName() { return "Self Optimal"; }
        public double initialMA() { return 30.0; }
        public double initialBO() { return 70.0; }
        public void onRoundComplete(int round) {
            if (round % 5 != 0) return;
            String name = getName();

            double avgLoanee = avgLoaneeEarn(name);
            double ma = currentMA.get(name);
            if (avgLoanee - ma > 10) currentMA.put(name, clampMA(ma + 5));
            else if (avgLoanee - ma <= 0) currentMA.put(name, clampMA(ma - 5));

            double avgLoaner = avgLoanerEarn(name);
            double bo = currentBO.get(name);
            if (bo * 0.75 > avgLoaner) currentBO.put(name, clampBO(bo + 10));
            else currentBO.put(name, clampBO(bo - 10));
        }
    }

    // #9 Master of One: every 10 rounds, compares its own avgLoanerEarn to
    // avgLoaneeEarn and doubles down on whichever role is winning.
    static class MasterOfOne implements Bot {
        public String getName() { return "Master of One"; }
        public double initialMA() { return 20.0; }
        public double initialBO() { return 50.0; }
        public void onRoundComplete(int round) {
            if (round % 10 != 0) return;
            String name = getName();
            double avgLoaner = avgLoanerEarn(name);
            double avgLoanee = avgLoaneeEarn(name);
            if (avgLoanee > avgLoaner) {
                currentMA.put(name, clampMA(currentMA.get(name) + 10));
            } else if (avgLoaner > avgLoanee) {
                currentBO.put(name, clampBO(currentBO.get(name) + 10));
            }
            // tie -> leave both alone this checkpoint
        }
    }

    // #10 Social Proof Type 1: before each pairing, looks up the PUBLIC
    // ledger for the matched opponent (what BO/MA the rest of the field
    // has used against them) and adopts the average as its own numbers.
    static class SocialProofType1 implements Bot {
        private String currentOpponent;
        public String getName() { return "Social Proof Type 1"; }
        public double initialMA() { return 20.0; }
        public double initialBO() { return 50.0; }
        public void setOpponent(String opponentName) { this.currentOpponent = opponentName; }
        public void onMatchStart() {
            if (currentOpponent == null) return;
            List<Double> bos = boUsedAgainst.get(currentOpponent);
            List<Double> mas = maUsedAgainst.get(currentOpponent);
            currentBO.put(getName(), (bos != null && !bos.isEmpty()) ? clampBO(average(bos)) : 50.0);
            currentMA.put(getName(), (mas != null && !mas.isEmpty()) ? clampMA(average(mas)) : 20.0);
        }
    }

    // #11 Predator: before each pairing, checks whether the OPPONENT is
    // currently behind its own 90% quota pace (opponent's totalEarnings is
    // the only opponent-visible stat this needs). If they're struggling,
    // Predator turns the screws; otherwise it leaves its numbers alone.
    static class Predator implements Bot {
        private String currentOpponent;
        public String getName() { return "Predator"; }
        public double initialMA() { return 30.0; }
        public double initialBO() { return 50.0; }
        public void setOpponent(String opponentName) { this.currentOpponent = opponentName; }
        public void onMatchStart() {
            if (currentOpponent == null) return;
            double oppEarnings = totalEarnings.getOrDefault(currentOpponent, 0.0);
            if (!isAtOrAboveQuotaPace(oppEarnings, currentSimRound)) {
                currentMA.put(getName(), clampMA(40));
                currentBO.put(getName(), clampBO(70));
            }
            // else: opponent is doing fine -- keep current numbers as-is
        }
    }

    // #12 Ratioed: before each pairing, sets both numbers directly from
    // the relative-wealth ratio between itself and the matched opponent.
    static class Ratioed implements Bot {
        private String currentOpponent;
        public String getName() { return "Ratioed"; }
        public double initialMA() { return 20.0; }
        public double initialBO() { return 50.0; }
        public void setOpponent(String opponentName) { this.currentOpponent = opponentName; }
        public void onMatchStart() {
            if (currentOpponent == null) return;
            double mine = totalEarnings.getOrDefault(getName(), 0.0);
            double theirs = totalEarnings.getOrDefault(currentOpponent, 0.0);
            double ratio = (mine + theirs == 0) ? 0.5 : mine / (mine + theirs);
            currentMA.put(getName(), clampMA(100.0 * ratio));
            currentBO.put(getName(), clampBO(100.0 * (1.0 - ratio)));
        }
    }

    // ==========================================================
    // Bot roster -- add/remove/duplicate freely, the simulation adapts to
    // however many bots are in this list.
    // ==========================================================
    static List<Bot> createBots() {
        List<Bot> bots = new ArrayList<>();
        bots.add(new Humane());
        bots.add(new RandomBot());
        bots.add(new Generous());
        bots.add(new AppealToAverage());
        bots.add(new Greedy());
        bots.add(new MrOptimal());
        bots.add(new MrsOptimal());
        bots.add(new SelfOptimal());
        bots.add(new MasterOfOne());
        bots.add(new SocialProofType1());
        bots.add(new Predator());
        bots.add(new Ratioed());
        return bots;
    }

    // ==========================================================
    // Random pairing with a per-pair cap, adapted from the PD reference.
    // Any bot left over when the count is odd simply isn't included in
    // any pair this round -- it sits out.
    // ==========================================================
    static List<Bot[]> randomPairing(List<Bot> bots, Random rng) {
        List<Bot> shuffled = new ArrayList<>(bots);
        int maxAttempts = 2000;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Collections.shuffle(shuffled, rng);
            List<Bot> available = new ArrayList<>(shuffled);
            List<Bot[]> pairs = new ArrayList<>();
            Map<String, Map<String, Integer>> trialIncrements = new HashMap<>();
            boolean failed = false;

            while (available.size() >= 2) {
                Bot a = available.remove(0);
                String aName = a.getName();
                boolean paired = false;

                for (int i = 0; i < available.size(); i++) {
                    Bot b = available.get(i);
                    String bName = b.getName();

                    Map<String, Integer> aMap = pairCounts.get(aName);
                    int current = aMap == null ? 0 : aMap.getOrDefault(bName, 0);
                    Map<String, Integer> tAMap = trialIncrements.get(aName);
                    int trial = tAMap == null ? 0 : tAMap.getOrDefault(bName, 0);

                    if (current + trial < allowedPerPair) {
                        pairs.add(new Bot[] { a, b });
                        trialIncrements.computeIfAbsent(aName, k -> new HashMap<>()).merge(bName, 1, Integer::sum);
                        trialIncrements.computeIfAbsent(bName, k -> new HashMap<>()).merge(aName, 1, Integer::sum);
                        available.remove(i);
                        paired = true;
                        break;
                    }
                }

                if (!paired) { failed = true; break; }
            }

            if (!failed) {
                for (Map.Entry<String, Map<String, Integer>> e : trialIncrements.entrySet()) {
                    String x = e.getKey();
                    for (Map.Entry<String, Integer> ee : e.getValue().entrySet()) {
                        pairCounts.computeIfAbsent(x, k -> new HashMap<>()).merge(ee.getKey(), ee.getValue(), Integer::sum);
                    }
                }
                return pairs;
            }
        }

        // Fallback: simple random pairing with no cap respected (rare).
        Collections.shuffle(shuffled, rng);
        List<Bot[]> fallback = new ArrayList<>();
        for (int i = 0; i + 1 < shuffled.size(); i += 2) fallback.add(new Bot[] { shuffled.get(i), shuffled.get(i + 1) });
        return fallback;
    }

    // ==========================================================
    // Plays one Ultimatum game: proposer offers, responder accepts/rejects,
    // and every relevant global map gets updated.
    // ==========================================================
    static void playGame(Bot proposer, Bot responder) {
        String pName = proposer.getName();
        String rName = responder.getName();

        double offer = proposer.proposeOffer();   // BO = proposer's share
        double toResponder = 100.0 - offer;
        boolean accepted = responder.decideAccept(toResponder);
        double proposerEarn = accepted ? offer : 0.0;
        double responderEarn = accepted ? toResponder : 0.0;

        totalEarnings.merge(pName, proposerEarn, Double::sum);
        totalEarnings.merge(rName, responderEarn, Double::sum);
        loanerEarnSum.merge(pName, proposerEarn, Double::sum);
        loanerRounds.merge(pName, 1, Integer::sum);
        loaneeEarnSum.merge(rName, responderEarn, Double::sum);
        loaneeRounds.merge(rName, 1, Integer::sum);

        proposerGamesWindow.merge(pName, 1, Integer::sum);
        if (accepted) proposerAcceptedWindow.merge(pName, 1, Integer::sum);
        responderGamesWindow.merge(rName, 1, Integer::sum);
        if (accepted) responderAcceptedWindow.merge(rName, 1, Integer::sum);

        // Public reputation ledger: record what was just used, for Social Proof.
        boUsedAgainst.computeIfAbsent(rName, k -> new ArrayList<>()).add(offer);
        maUsedAgainst.computeIfAbsent(pName, k -> new ArrayList<>()).add(currentMA.get(rName));

        proposer.onLoanerResult(accepted);
    }

    static void printSnapshot(List<Bot> bots, int round) {
        System.out.println("\n---- Snapshot after round " + round + " ----");
        for (Bot b : bots) {
            String n = b.getName();
            System.out.printf("%-20s total=%.0f | avgLoanerEarn=%.0f | avgLoaneeEarn=%.0f | BO=%.0f | MA=%.0f%n",
                    n, totalEarnings.getOrDefault(n, 0.0), avgLoanerEarn(n), avgLoaneeEarn(n),
                    currentBO.getOrDefault(n, 0.0), currentMA.getOrDefault(n, 0.0));
        }
    }

    public static void main(String[] args) {
        List<Bot> bots = createBots();
        int n = bots.size();

        // Initialize every global map for every bot.
        for (Bot b : bots) {
            String name = b.getName();
            totalEarnings.put(name, 0.0);
            loanerEarnSum.put(name, 0.0);
            loanerRounds.put(name, 0);
            loaneeEarnSum.put(name, 0.0);
            loaneeRounds.put(name, 0);
            currentBO.put(name, b.initialBO());
            currentMA.put(name, b.initialMA());
            rawGenerousBO.put(name, b.initialBO()); // only ever read/written by Generous, irrelevant for everyone else
            boDeclineStreak.put(name, 0);
            boAcceptStreak.put(name, 0);
            resetAggressionWindow(name);
            pairCounts.putIfAbsent(name, new HashMap<>());
        }

        // Quota target: (N-1 opponents * 10 proposer-games each) * $100 * 90%. 
        // Can be altered if quota is too high and skews strategies poorly 
        quotaProposerRounds = (n - 1) * 10;
        quotaTarget = quotaProposerRounds * 100.0 * QUOTA_RATIO;

        // Total rounds needed so each unordered pair gets `allowedPerPair`
        // pairing events (same formula shape as the PD reference).
        int pairCount = n * (n - 1) / 2;
        int matchesPerRound = Math.max(1, n / 2);
        totalRoundsGlobal = (int) Math.ceil((allowedPerPair * (double) pairCount) / (double) matchesPerRound);

        System.out.println("Simulating " + totalRoundsGlobal + " rounds so each pair meets " + allowedPerPair
                + " times (20 games each: 10 as loaner, 10 as loanee). Quota target per bot: $" + quotaTarget);

        for (int round = 1; round <= totalRoundsGlobal; round++) {
            currentSimRound = round;
            List<Bot[]> pairs = randomPairing(bots, rng);

            for (Bot[] p : pairs) {
                if (p == null || p.length < 2) continue;
                Bot a = p[0];
                Bot b = p[1];

                a.setOpponent(b.getName());
                b.setOpponent(a.getName());
                a.setRound(round, totalRoundsGlobal);
                b.setRound(round, totalRoundsGlobal);

                // Pre-match bespoke adjustments (Predator, Ratioed, Appeal
                // to Average, Social Proof) before either game is played.
                a.onMatchStart();
                b.onMatchStart();

                // The pairing plays out as 2 games: each bot proposes once.
                playGame(a, b); // a = loaner, b = loanee
                playGame(b, a); // b = loaner, a = loanee

                // Post-round bespoke periodic checks (Self Optimal, Master of One).
                a.onRoundComplete(round);
                b.onRoundComplete(round);
            }

            // Universal Global Aggression Count checkpoint: fires once for
            // EVERY bot (not just whoever happened to be paired this round,
            // so no one is skipped by sitting out an odd-count round) right
            // after round 10 (20, 30, ...) fully finishes, before round 11
            // begins -- a single clean pass between rounds, not something
            // tangled into mid-round pair processing.
            if (round % 10 == 0) {
                for (Bot b : bots) applyGlobalAggression(b.getName());
            }

            if (round % 50 == 0) printSnapshot(bots, round);
        }

        // Final standings
        System.out.println("\n========== FINAL STANDINGS ==========");
        List<Bot> ranked = new ArrayList<>(bots);
        ranked.sort((x, y) -> Double.compare(
                totalEarnings.getOrDefault(y.getName(), 0.0),
                totalEarnings.getOrDefault(x.getName(), 0.0)));

        for (Bot b : ranked) {
            String name = b.getName();
            double earned = totalEarnings.getOrDefault(name, 0.0);
            String quotaFlag = earned >= quotaTarget ? "MET QUOTA" : "missed quota";
            System.out.printf("%-20s total=%.0f | avgLoanerEarn=%.0f | avgLoaneeEarn=%.0f | BO=%.0f | MA=%.0f | %s%n",
                    name, earned, avgLoanerEarn(name), avgLoaneeEarn(name),
                    currentBO.getOrDefault(name, 0.0), currentMA.getOrDefault(name, 0.0), quotaFlag);
        }
    }
}
