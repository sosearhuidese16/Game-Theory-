import java.util.*;

public class Begin {
    interface Bot {
        String getName();
        int decideMove();
        default void setOpponent(String opponentName) {
            // default no-op; bots that need opponent info can override this 
        }
        default void setRound(int currentRound, int totalRounds) {
            // default no-op; bots that need round info can override this
        }
    }
    // Most Map keys were made with slight AI assistance to avoid typos and ensure consistency across the codebase.
    // lastMoveAgainst: for each bot (key), a map of opponentName and their last move per opponent
    static Map<String, Map<String, Integer>> lastMoveAgainst = new HashMap<>();
    // tracks opponentName -> botName -> (cooperateCount, defectCount) which is used for certain strategies 
    static Map<String, Map<String, int[]>> opponentMoveHistory = new HashMap<>();
    // botEarnings: maps bot name -> total earnings accumulated across all rounds
    static Map<String, Integer> botEarnings = new HashMap<>();
    // moveCounts: maps bot name -> [cooperateCount, defectCount]
    static Map<String, int[]> moveCounts = new HashMap<>();
    // pairCounts: botName -> otherBotName -> count
    static Map<String, Map<String, Integer>> pairCounts = new HashMap<>();
    // repeats per unordered pair, basically total rounds played for each bots 
    static int repeatsPerPair = 100;
    // effective allowed matches per pair = repeatsPerPair - 1 (could remove this for more realism?)
    static int allowedPerPair = Math.max(0, repeatsPerPair - 1);
    //No onto the strategies them 

    // #1 Tic-for-Tac: cooperate first, then copy opponent's previous move
    static class TicforTac implements Bot {
        private String currentOpponent = null;  // Current opponent's name
        public String getName() { return "Tic-for-Tac"; }

        // Called before a match so the bot knows which opponent it's about to face
        public void setOpponent(String opponentName) {
            currentOpponent = opponentName;
        }

        // Tit-for-Tat consults the per-bot-per-opponent global map
        public int decideMove() {
            if (currentOpponent == null) return 1; // cooperate by default

            Map<String, Integer> map = lastMoveAgainst.get(getName());
            if (map == null) return 1;
            int last = map.getOrDefault(currentOpponent, -1);
            if (last == -1) return 1; // cooperate on first encounter
            return last; // copy opponent's last move against this bot
        }
    }

    // #2 Greedy: Always defect
    static class Greed implements Bot {
        public String getName() { return "Greedy"; }
        public int decideMove() { return 0; } // always defect
    }

    // #3 Opportunist type-B: looks at the opponent's overall cooperate/defect counts and
    // cooperates if opponent has cooperated at least as often as they defected. 
    static class OpportunistB implements Bot {
        private String currentOpponent = null;  // Current opponent's name
        public String getName() { return "OpportunistB"; }
        public void setOpponent(String opponentName) { currentOpponent = opponentName; }
        public int decideMove() {
            if (currentOpponent == null) return 1; // cooperate by default
            int[] counts = moveCounts.getOrDefault(currentOpponent, new int[]{0,0});
            int coop = counts[0];
            int defect = counts[1];
            return coop >= defect ? 1 : 0;
        }
    }

    // #4 Random: 50/50
    static class RandomBot implements Bot {
        public String getName() { return "Random"; }
        public int decideMove() {
            return Math.random() * 100 > 50 ? 0 : 1;
        }
    }

    // #5 Grudge: defects forever against any opponent that ever defected against it
    static class Grudge implements Bot {
        private String currentOpponent = null;
        private Map<String, Boolean> grudges = new HashMap<>();
        public String getName() { return "Grudge"; }
        public void setOpponent(String opponentName) { currentOpponent = opponentName; }
        public int decideMove() {
            if (currentOpponent == null) return 1;
            Map<String, Integer> map = lastMoveAgainst.get(getName());
            if (map == null) return 1;
            Integer last = map.get(currentOpponent);
            if (last != null && last == 0) {
                grudges.put(currentOpponent, true);
            }
            return grudges.getOrDefault(currentOpponent, false) ? 0 : 1;
        }
    }

    // #6 Altruistic: mostly cooperates
    static class Altruistic implements Bot {
        public String getName() { return "Altruistic"; }
        public int decideMove() {
            return Math.random() * 100 > 95 ? 0 : 1;
        }
    }

    // #7 Opportunist type-A: defect if opponent's average earnings per round > 3 (or sum number))
    static class OpportunistA implements Bot {
        private String currentOpponent = null;
        private int currentRound = 0;
        public String getName() { return "OpportunistA"; }
        public void setOpponent(String opponentName) { currentOpponent = opponentName; }
        public void setRound(int currentRound, int totalRounds) {
            this.currentRound = currentRound;
        }
        public int decideMove() {
            if (currentOpponent == null || currentRound <= 0) return 1;
            Integer oppEarnings = botEarnings.getOrDefault(currentOpponent, 0);
            double oppAvg = (double) oppEarnings / (double) currentRound;
            return oppAvg > 3.1 ? 0 : 1;
        }
    }

    // #8 Angry: cooperates until an opponent defects against it; then defects for 2-6 turns (AI assistance was used for this bot))
    static class Rage implements Bot {
        private String currentOpponent = null;
        private int rageRoundsLeft = 0; // remaining turns to defect
        private Random rng = new Random();
        public String getName() { return "Rage"; }
        // remember the opponent's last move when they faced Rage (to detect steals against Rage)
        private Map<String, Integer> lastSeenMoveAgainstRage = new HashMap<>();
        public void setOpponent(String opponentName) { currentOpponent = opponentName; }
        public int decideMove() {
            Map<String, Integer> map = lastMoveAgainst.get(getName());
            // If currently raging, mark any new moves as seen (so they won't retrigger) and consume one rage turn and defect.
            if (rageRoundsLeft > 0) {
                if (map != null) {
                    for (Map.Entry<String, Integer> e : map.entrySet()) {
                        lastSeenMoveAgainstRage.put(e.getKey(), e.getValue());
                    }
                }
                rageRoundsLeft--;
                return 0;
            }
            if (currentOpponent == null) return 1; // cooperate by default
            if (map == null) return 1;
            // Initialize seen snapshot on first use to avoid false triggers from historical data
            if (lastSeenMoveAgainstRage.isEmpty()) {
                for (Map.Entry<String, Integer> e : map.entrySet()) {
                    lastSeenMoveAgainstRage.put(e.getKey(), e.getValue());
                }
                return 1;
            }

            // Look for any opponent who newly defected against Angry and trigger a global rage.
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                String opp = e.getKey();
                Integer lastAgainstRage = e.getValue();
                Integer prevSeen = lastSeenMoveAgainstRage.getOrDefault(opp, -1);

                if (lastAgainstRage != null && lastAgainstRage == 0 && prevSeen != 0) {
                    int n = rng.nextInt(10) + 5; // 3..5
                    // Count this current move as the first of n, so remaining = n-1
                    rageRoundsLeft = Math.max(0, n - 1);
                    // snapshot current seen moves so defections during rage won't retrigger
                    for (Map.Entry<String, Integer> e2 : map.entrySet()) {
                        lastSeenMoveAgainstRage.put(e2.getKey(), e2.getValue());
                    }
                    return 0;
                }
            }
            // No new defects found; update our seen map for all opponents
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                lastSeenMoveAgainstRage.put(e.getKey(), e.getValue());
            }
            return 1;
            }
    }

    // #9 Tic-for-two-Tacs: cooperate first; if opponent's last two moves against (AI assistance)
    // TF2T were both defections, TF2T defects. Otherwise it cooperates.
    static class TF2T implements Bot {
        private String currentOpponent = null;  // Current opponent's name
        // map: opponentName -> [mostRecent, secondMostRecent] moves opponent made against TF2T
        private Map<String, Integer[]> lastTwo = new HashMap<>();
        public String getName() { return "TF2T"; }

        // Called before a match so the bot knows which opponent it's about to face
        public void setOpponent(String opponentName) {
            currentOpponent = opponentName;
        }
        public int decideMove() {
            if (currentOpponent == null) return 1; // cooperate by default
            // Read the opponent's most recent move against TF2T from the global map.
            Map<String, Integer> map = lastMoveAgainst.get(getName());
            int last = -1;
            if (map != null) {
                last = map.getOrDefault(currentOpponent, -1);
            }

            // Update our per-opponent buffer: shift previous mostRecent to secondMostRecent,
            Integer[] arr = lastTwo.get(currentOpponent);
            if (arr == null) arr = new Integer[]{-1, -1};
            arr[1] = arr[0];
            arr[0] = last;
            lastTwo.put(currentOpponent, arr);
            // If no previous data (first encounter), cooperate
            if (arr[1] == -1 && arr[0] == -1) return 1;
            // If there are fewer than two historical moves (second is -1), treat as cooperate
            if (arr[1] == -1) return 1;

            // Defects only when the opponent's two most recent moves against it were both defects (0)
            if (arr[0] == 0 && arr[1] == 0) return 0;
            return 1;
        }
    }

    // #10 Social Proof: Tracks how other bots behave toward the current opponent.
    static class SocialProof implements Bot {
        private String currentOpponent = null;
        public String getName() { return "SocialProof"; }
        public void setOpponent(String opponentName) {
            currentOpponent = opponentName;
        }
        public int decideMove() {
            if (currentOpponent == null) return 1; // cooperate by default
            // Get opponent-specific move history from all bots
            Map<String, int[]> opponentHistory = opponentMoveHistory.get(currentOpponent);
            if (opponentHistory == null || opponentHistory.isEmpty()) return 1; // no data yet, cooperate
            // Count total cooperates vs defects across all bots against this opponent
            int totalCooperates = 0;
            int totalDefects = 0;
            for (Map.Entry<String, int[]> entry : opponentHistory.entrySet()) {
                String botName = entry.getKey();
                if (botName.equals(getName())) continue; // skip self

                int[] counts = entry.getValue();
                totalCooperates += counts[0];
                totalDefects += counts[1];
            }
            // If no data, cooperate
            if (totalCooperates == 0 && totalDefects == 0) return 1;
            // If most bots cooperated with this opponent, cooperate, else deflect
            return totalCooperates >= totalDefects ? 1 : 0;
        }
    }


    // Place to add bots for simuation (can add as many as needed, can also loop it to add multiple copies of each bot)
    static List<Bot> createBots() {
        List<Bot> bots = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            bots.add(new TicforTac());
            bots.add(new TF2T());
            bots.add(new Altruistic());
            bots.add(new Greed());
            bots.add(new OpportunistA());
            bots.add(new OpportunistB());
            bots.add(new RandomBot());
            bots.add(new Grudge());
            bots.add(new Rage());
            bots.add(new SocialProof()); 
        }
        return bots;
    }
    
    /**
     * Shuffle and return pairings from existing bot list. 
     */
    static List<Bot[]> randomPairing(List<Bot> bots, Random rng) {
        // Try to generate a pairing where no unordered pair exceeds `repeatsPerPair`.
        List<Bot> shuffled = new ArrayList<>(bots);
        int maxAttempts = 2000;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Collections.shuffle(shuffled, rng);
            List<Bot> available = new ArrayList<>(shuffled);
            List<Bot[]> pairs = new ArrayList<>();
            // track increments locally; only merge into global pairCounts when successful
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
                        // pair them in this trial
                        pairs.add(new Bot[] { a, b });
                        // record trial increments both ways
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
                // merge trialIncrements into global pairCounts
                for (Map.Entry<String, Map<String, Integer>> e : trialIncrements.entrySet()) {
                    String x = e.getKey();
                    for (Map.Entry<String, Integer> ee : e.getValue().entrySet()) {
                        String y = ee.getKey();
                        int inc = ee.getValue();
                        pairCounts.computeIfAbsent(x, k -> new HashMap<>()).merge(y, inc, Integer::sum);
                    }
                }
                return pairs;
            }
        }

        // fallback: if there was no restriction-respecting shuffle, return a simple random pairing
        Collections.shuffle(shuffled, rng);
        List<Bot[]> fallback = new ArrayList<>();
        for (int i = 0; i + 1 < shuffled.size(); i += 2) fallback.add(new Bot[] { shuffled.get(i), shuffled.get(i + 1) });
        return fallback;
    }
    
    public static void main(String[] args) {
        Random rng = new Random();
        // Cmpute totalRounds so each unordered pair meets repeatsPerPair-1 times
        int repeatsPerPair = 101; // user-provided x; AMOUNT OF ROUNDS WILL BE (x-1)*9
        // set static allowedPerPair so pairing logic uses x-1
        allowedPerPair = Math.max(0, repeatsPerPair - 1);
        int totalRounds;
        
        // Create bots once - they persist across all rounds to maintain history
        List<Bot> bots = createBots();
        // compute rounds needed: pairs = N*(N-1)/2, matchesPerRound = floor(N/2)
        int N = bots.size();
        int pairCount = N * (N - 1) / 2;
        int matchesPerRound = Math.max(1, N / 2);
        totalRounds = (int) Math.ceil((allowedPerPair * (double) pairCount) / (double) matchesPerRound);
        System.out.println("Simulating " + totalRounds + " rounds so each pair meets up to " + allowedPerPair + " times (user x=" + repeatsPerPair + ", pairs=" + pairCount + ", matchesPerRound=" + matchesPerRound + ")");
        // Initialize nested maps for lastMoveAgainst so every bot has its own opponent map
        for (Bot bot : bots) {
            lastMoveAgainst.putIfAbsent(bot.getName(), new HashMap<>());
            botEarnings.putIfAbsent(bot.getName(), 0);
                moveCounts.putIfAbsent(bot.getName(), new int[]{0,0});
            // initialize pairCounts entry for this bot
            pairCounts.putIfAbsent(bot.getName(), new HashMap<>());
        }
        
        for (int round = 1; round <= totalRounds; round++) {
            // THIS IS FOR LATER System.out.println("\nROUND " + round + " PAIRINGS:");
            // Get new random pairings for this round
            List<Bot[]> pairs = randomPairing(bots, rng);
            // For each pair in this round
            for (Bot[] p : pairs) {
                if (p == null || p.length < 2) {
                    if (p != null && p.length == 1) {
                        System.out.println(p[0].getName() + " has no opponent this round.");
                    }
                    continue;
                }

                Bot a = p[0];
                Bot b = p[1];
                
                // Set opponents and pass round info to bots that need this information
                a.setOpponent(b.getName());
                b.setOpponent(a.getName());
                a.setRound(round, totalRounds);
                b.setRound(round, totalRounds);

                // Get moves from both bots
                int moveA = a.decideMove();
                int moveB = b.decideMove();
                /* !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                System.out.printf("Moves: %s (%s) -> %s, %s (%s) -> %s%n",
                    a.getName(), botEarnings.get(a.getName()), moveA == 1 ? "Cooperate" : "Defect",
                    b.getName(),  botEarnings.get(b.getName()), moveB == 1 ? "Cooperate" : "Defect");
                */
                // Update last-move-against maps so bot X remembers what opponent Y played
                // the last time they faced X.
                lastMoveAgainst.get(a.getName()).put(b.getName(), moveB);
                lastMoveAgainst.get(b.getName()).put(a.getName(), moveA);

                // Update opponentMoveHistory: track how each bot treated the opponent
                // a faced b and played moveA; b faced a and played moveB
                Map<String, int[]> bHistory = opponentMoveHistory.computeIfAbsent(b.getName(), k -> new HashMap<>());
                int[] aCountsAgainstB = bHistory.computeIfAbsent(a.getName(), k -> new int[]{0,0});
                if (moveA == 1) aCountsAgainstB[0]++; else aCountsAgainstB[1]++;

                Map<String, int[]> aHistory = opponentMoveHistory.computeIfAbsent(a.getName(), k -> new HashMap<>());
                int[] bCountsAgainstA = aHistory.computeIfAbsent(b.getName(), k -> new int[]{0,0});
                if (moveB == 1) bCountsAgainstA[0]++; else bCountsAgainstA[1]++;

                // Update move counts for each bot (their own moves)
                int[] aCounts = moveCounts.get(a.getName());
                if (moveA == 1) aCounts[0]++; else aCounts[1]++;
                int[] bCounts = moveCounts.get(b.getName());
                if (moveB == 1) bCounts[0]++; else bCounts[1]++;

                // Calculate and add earnings based on previous rounds 
                // moveA = 1 (Cooperate), moveA = 0 (Defect)
                if (moveA == 1 && moveB == 1) {
                    // Both cooperate: 3 points each
                    botEarnings.put(a.getName(), botEarnings.get(a.getName()) + 3);
                    botEarnings.put(b.getName(), botEarnings.get(b.getName()) + 3);
                } else if (moveA == 1 && moveB == 0) {
                    // A cooperates, B defects: A gets 0, B gets 5
                    botEarnings.put(a.getName(), botEarnings.get(a.getName()) + 0);
                    botEarnings.put(b.getName(), botEarnings.get(b.getName()) + 5);
                } else if (moveA == 0 && moveB == 1) {
                    // A defects, B cooperates: A gets 5, B gets 0
                    botEarnings.put(a.getName(), botEarnings.get(a.getName()) + 5);
                    botEarnings.put(b.getName(), botEarnings.get(b.getName()) + 0);
                } else {
                    // Both defect: 1 point each
                    botEarnings.put(a.getName(), botEarnings.get(a.getName()) + 1);
                    botEarnings.put(b.getName(), botEarnings.get(b.getName()) + 1);
                }
            }
        }
        
        // Print final standings
        System.out.println("\n========== FINAL STANDINGS ==========");
        List<String> botNames = new ArrayList<>(botEarnings.keySet());
        botNames.sort((b1, b2) -> botEarnings.get(b2).compareTo(botEarnings.get(b1)));
        
        for (String botName : botNames) {
            System.out.println(botName + ": " + botEarnings.get(botName) + " points");
        }
    }
    
}
