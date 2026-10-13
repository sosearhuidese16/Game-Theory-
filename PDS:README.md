Prisoner’s Dilemma Simulation By Sose Arhuidese

Overview

This project is a simulation of the Prisoner’s Dilemma, a game-theory scenario in which two players repeatedly choose whether to cooperate or defect. The goal of this project was to investigate how different strategies perform when interacting repeatedly with one another at random.

The idea was inspired by a Veritasium video on game theory, which demonstrated a tournament in which different Prisoner’s Dilemma strategies competed against one another. However, I felt that the simulation's structure did not fully represent how interactions work in real life. Then again, game theory itself isn't an exactly perfect way to predict human behavior, but I felt that the version shown in the video particularly was lacking. The biggest change in my simulation is how opponents are selected.


In the version shown by Veritasium, each strategy played against the same opponent for 200 consecutive rounds. After those 200 rounds, strategies moved on to another strategy and played that opponent for another 200 rounds. In other words, the strategies had long, uninterrupted interactions with the same opponent. This setup makes it possible for a strategy to learn a great deal about one particular opponent's behavior and adjust specifically to that opponent. While this is useful for studying repeated interactions, I wanted to explore what would happen if opponents changed from round to round.

--- 

My Simulation

The defining feature of my simulation is that matchups are randomized between rounds rather than having every pair of strategies play hundreds of consecutive rounds together. As a result, two strategies could potentially face each other several times in a row, but they could also go dozens or even 100 rounds without interacting; however, there is a set amount of time each bot will play each other by the end, so it's impossible for strategies to entirely avoid one another. This creates a more dynamic environment. A strategy cannot simply spend 200 consecutive rounds learning how one particular opponent behaves. Instead, it has to make decisions while dealing with a constantly changing group of opponents.

For example, a strategy could encounter a player who frequently defects one round and then immediately encounter a highly cooperative player in the next. This creates opportunities for strategies to use information from the broader population, rather than relying exclusively on the behavior of the opponent they are currently facing. This also allows strategies to be designed around information from previous rounds, such as how other bots behaved or how successful a particular opponent has been.The overall goal was to make the simulation closer to situations where people repeatedly interact with a larger and changing social environment.

---
 
Point System 

There are many ways to determine payoffs system used in the prisoner's dilemma, but I went with 5-3-1-0 since it's commonly used, and generally well balanced.
Defection vs Cooperation: 5-0
Cooperation vs Defection: 0-5
Cooperate vs Cooperation: 3-3
Defection vs Defection: 1-1 
---

The Strategies

The simulation contains ten different strategies. Some are based on well-known game-theory approaches, while others were designed to represent more realistic human decision-making patterns.

1. Tit-for-Tat

-Cooperates on the first turn.
-After that, copies that specific opponent's previous move against them.

The idea is to assume cooperation, but then respond to the opponent based on their most recent behavior. A classic approuch

 2. Tit-for-2-Tats

-Cooperates on the first turn Initially c
-Continues cooperating unless the opponent defects twice in a row against them.

This strategy is more forgiving than Tit-for-Tat because one isolated defection does not immediately cause retaliation.

3. Altruistic

 -Cooperates approximately 95% of the time.

This strategy prioritizes cooperation even when doing so may result in lower individual earnings.

4. Greedy

-Always defects (can be toggled to 95% if necessary).

Greedy never attempts to establish cooperation and instead consistently chooses the action that can produce a larger immediate payoff when the opponent cooperates.

5. Random

Has a 50/50 chanceof cooperating or defecting.

This strategy does not attempt to learn from previous interactions or optimize its behavior.

6. Opportunist Type-A

-Looks at an opponent's "average earnings per round"
-Defects if that average is above a specified threshold
-The threshold was generally set to 3 or 2.5 when using a 5-3-1-0 point system.

Rather than looking directly at whether an opponent has cooperated or defected, this strategy attempts to infer something about an opponent from their overall performance.

7. Opportunist Type-B

-Compares how frequently an opponent has cooperated and defected.
-Defects when the opponent has defected more often than they have cooperated.

Similar to Opportunist Type-A, but instead of judging an opponent based on point average, it judges them based on past decisions.

8. Social Proof

- Bases its decision on how other bots have played against its current opponent.

This strategy attempts to use information from the wider population. Instead of asking only, "How has this opponent treated me?", it asks, "How has this opponent treated everyone else?"

9. Angry/Rage/Temper

-Begins by cooperating.
-Once someone defects, it retaliates for a random number of turns (2-6), regardless of who their opponent is

-this strategy is designed to represent a less calculated emotional response to being betrayed. Though there are many way it can be improved upon, such as making it similar to Tit-for-2-Tacs in that it doesn't immediately start defecting after a singular defection. 

10. Grudge

-Initially cooperates.
-If another bot defects against it, it remembers that specific bot.
-It then defects against that bot for the remainder of the simulation.

Unlike Tit-for-Tat, Grudge does not forgive the opponent after they begin cooperating again. Similarly to Angry bot, I could made this one more forgiving. 

---

Simulation Versions

Four different versions of the simulation were tested. The purpose of changing the number of bots and interactions was to see whether the results remained consistent as the size and length of the simulation increased.

| Version     |    Population    |               Matchups             |             Total Rounds          |
| ------------- | ------------------: | --------------------------------: | --------------------------------: |
| Version 1 |  1 of each strategy |   Each possible matchup 100 times |          900     |
| Version 2 |  1 of each strategy | Each possible matchup 1,000 times |        9,000  |
| Version 3 | 10 of each strategy |   Each possible matchup 100 times |        9,900  |
| Version 4 | 10 of each strategy |   Each possible matchup 500 times |       49,500 |

The first two versions therefore examined how the strategies behaved with a relatively small population, while Versions 3 and 4 introduced ten copies of every strategy creating a much larger population.

---

 Results

## Version 1 — 1 of Each Strategy, 100 Matchups

Version 1 consisted of **12 simulations**.

The average rankings across those simulations were:

| Strategy           | Average Place | Average Points |
| ------------------ | ------------: | -------------: |
| **Grudge**         |          1.00 |       2,755.33 |
| **Tic-for-Tac**    |          2.92 |       2,390.25 |
| **Random**         |          3.42 |       2,390.58 |
| **Opportunist-B**  |          4.08 |       2,324.83 |
| **Tic-for-2-Tacs** |          4.50 |       2,321.92 |
| **Social Proof**   |          5.83 |       2,253.00 |
| **Greedy**         |          6.83 |       2,214.33 |
| **Rage/Temper**    |          8.08 |       2,175.17 |
| **Opportunist-A**  |          8.42 |       2,150.92 |
| **Altruistic**     |          9.92 |       2,004.92 |

The most notable result was **Grudge**, which finished first in average placement. Tic-for-Tac also performed strongly, while Altruistic consistently occupied the lower end of the results.

Random was somewhat unusual in this version: despite having no deliberate strategy, it achieved a relatively high average placement.

---

## Version 2 — 1 of Each Strategy, 1,000 Matchups

Version 2 increased the number of interactions by a factor of ten and consisted of **5 simulations**.

| Strategy           | Average Place | Average Points |
| ------------------ | ------------: | -------------: |
| **Grudge**         |          1.00 |       27,230.0 |
| **Opportunist-B**  |          3.00 |       23,521.8 |
| **Tic-for-Tac**    |          3.00 |       23,587.0 |
| **Social Proof**   |          3.80 |       23,338.2 |
| **Rage/Temper**    |          5.00 |       23,000.8 |
| **Tic-for-2-Tacs** |          5.60 |       22,934.4 |
| **Greedy**         |          7.50 |       21,230.4 |
| **Opportunist-A**  |          7.90 |       21,215.4 |
| **Random**         |          9.00 |       19,206.8 |
| **Altruistic**     |          9.20 |       18,923.0 |

Grudge remained in **first place in every simulation**, suggesting that its strong performance was not simply a result of a short simulation.

Tic-for-Tac and Opportunist-B also remained near the top, while Random and Altruistic moved toward the bottom.

---

## Version 3 — 10 of Each Strategy, 100 Matchups

Version 3 dramatically increased the population by having **10 copies of every strategy**. It consisted of **5 simulations** and 9,900 total rounds per simulation.

| Strategy           | Average Place | Average Points |
| ------------------ | ------------: | -------------: |
| **Grudge**         |          1.00 |      22,510.40 |
| **Social Proof**   |          2.00 |      21,764.00 |
| **Opportunist-B**  |          3.80 |      20,636.80 |
| **Tic-for-2-Tacs** |          4.80 |      20,038.80 |
| **Tic-for-Tac**    |          3.20 |      20,683.00 |
| **Altruistic**     |          6.40 |      15,394.20 |
| **Greedy**         |          7.40 |      15,072.00 |
| **Opportunist-A**  |          7.80 |      15,119.40 |
| **Random**         |          8.80 |      13,984.00 |
| **Rage/Temper**    |          9.20 |      14,167.80 |

The larger population produced one particularly interesting change: **Social Proof moved into second place**.

This supports the idea behind the strategy. When there are many copies of each strategy interacting with one another, there is substantially more information available about how individual opponents behave with other players. A strategy that can use that population-level information therefore has more opportunities to benefit from it.

Tic-for-2-Tacs also improved relative to several of the other strategies.

---

## Version 4 — 10 of Each Strategy, 500 Matchups

Version 4 was the largest experiment, with **49,500 rounds** and 5 simulations.

| Strategy           | Average Place | Average Points |
| ------------------ | ------------: | -------------: |
| **Grudge**         |          1.40 |      1,214,911 |
| **Social Proof**   |          1.60 |      1,222,638 |
| **Tic-for-Tac**    |          3.00 |      1,164,672 |
| **Tic-for-2-Tacs** |          4.00 |      1,134,232 |
| **Opportunist-B**  |          5.00 |      1,097,184 |
| **Altruistic**     |          6.60 |        830,688 |
| **Opportunist-A**  |          7.40 |        813,847 |
| **Greedy**         |          7.60 |        813,131 |
| **Rage/Temper**    |          9.20 |        805,631 |
| **Random**         |          9.20 |        784,000 |

The largest simulation produced a very similar overall pattern.

**Grudge and Social Proof were the two highest-performing strategies**, followed by Tic-for-Tac and Tic-for-2-Tacs.

The result for Social Proof is particularly interesting because its performance improved as the population became larger. In a larger population, there are more interactions from which it can gather information about an opponent.

---

Major Findings

1. Grudge Was Consistently Extremely Successful

Grudge was the most consistent strategy across the experiments.

It finished first in Versions 1, 2, and 3, and had an average placement of **1.40** in Version 4.

One possible explanation is that Grudge combines two characteristics:

1. It initially gives other strategies an opportunity to cooperate.
2. Once another strategy defects, it refuses to cooperate with that particular strategy again.

This means that Grudge can benefit from cooperative relationships while protecting itself from repeatedly exploiting relationships.

Its behavior is also particularly well suited to the randomized-opponent structure. Because the Grudge is directed toward **specific opponents**, it can maintain a different relationship with different bots rather than applying the same response to everyone.


2. Tic-for-Tac Was Robust

Tic-for-Tac performed well across nearly every version.

Its basic approach is simple: **start cooperatively, then respond to the opponent's behavior**.

The randomized matchup system creates an interesting environment for this strategy because Tic-for-Tac does not need to know an opponent's entire history. It can respond immediately to the most recent action.

Its performance remained strong even when the population increased.


3. Tic-for-2-Tacs Became More Competitive With Larger Populations

Tic-for-2-Tacs generally performed below Tic-for-Tac, but its performance improved when the number of bots increased.

This makes sense given the difference between the strategies. Tic-for-2-Tacs is more forgiving: one defection does not immediately cause retaliation.

That can be beneficial in a larger population where a strategy encounters many different opponents and isolated defections may occur frequently.


4. Social Proof Benefits From a Large Population

One of the most interesting results was the performance of **Social Proof**.

It ranked fifth in Version 1, fourth in Version 2, second in Version 3, and second again in Version 4.

This suggests that the strategy's access to information about **other players' interactions** becomes more useful as the population grows.

With only one copy of each strategy, there is relatively little information available. With ten copies of each strategy, however, a bot can observe many more interactions involving its current opponent.

This demonstrates an important difference between **individual-level information** and **population-level information**.

---

## 5. Opportunist-A and Opportunist-B Demonstrate Two Different Ways of Judging Players

The two Opportunist strategies were designed around different types of information.

**Opportunist-A** judges an opponent using their average point earnings.

**Opportunist-B** judges an opponent based on how often they have cooperated versus defected.

Their results suggest that these two measurements can lead to very different outcomes.

Opportunist-B generally performed substantially better than Opportunist-A, particularly in the larger experiments.

This raises an interesting game-theory question:

> Is it more useful to judge an opponent based on how successful they have been, or based on what they have actually done?

The simulation suggests that, within this particular environment, looking directly at previous decisions was more consistently successful than using average point earnings.

---

6. Random Performed Poorly in the Larger Experiments

Random was one of the more interesting cases because it performed relatively well in Version 1 but dropped considerably as the experiments became larger.

By Version 2, Random ranked ninth, and it ranked eighth or ninth in the larger-population experiments.

This suggests that random behavior may occasionally benefit from unpredictability in a small sample, but it lacks the ability to adapt as the simulation becomes longer and more complex.

Unlike strategies such as Tic-for-Tac, Social Proof, or Grudge, Random does not learn from the environment.

---

7. Altruism Was Expensive

The Altruistic strategy cooperated 95% of the time.

While cooperation can create mutually beneficial relationships, the results demonstrate the danger of cooperating without sufficiently responding to exploitation.

Altruistic finished near the bottom in most of the experiments and was last in Version 2.

The result illustrates a central tension within the Prisoner's Dilemma: **cooperation can benefit both players, but unconditional cooperation can also be exploited by defecting strategies.**

---

!!! What Makes This Simulation Different? !!! 

The most important contribution of this project is not simply the number of strategies. It is the structure of the interactions. In a traditional tournament where two bots play hundreds of consecutive rounds, strategies have a predictable opportunity to learn about one another.

In this simulation, the opponent can change every round.

That means a successful strategy has to deal with two different problems:

1. How should I respond to the person I am currently playing?

2. What information can I use from the larger population to make that decision?

This creates room for strategies such as Social Proof and the Opportunist strategies, which would have less information to work with in a purely isolated head-to-head tournament.

It also makes memory more complicated. A strategy such as Grudge can remember specific individuals, while a strategy such as Social Proof can use information about interactions that did not involve itself directly.

---

Overall Conclusions

Across the four versions, several patterns emerged.

**Grudge was the most consistently successful strategy**, finishing first in the first three versions and averaging 1.40th place in the largest experiment.

**Tic-for-Tac was consistently competitive**, demonstrating that a relatively simple reactive strategy can remain effective across different population sizes and simulation lengths.

**Social Proof became substantially more successful when the population increased**, suggesting that strategies based on information about other players can benefit from having a larger pool of interactions to observe.

**Tic-for-2-Tacs also became more competitive with larger populations**, potentially because its more forgiving behavior allows it to maintain cooperation despite occasional defections.

Meanwhile, **Random and Altruistic generally struggled in the longer and larger experiments**, suggesting that strategies that either ignore information or respond insufficiently to exploitation can have difficulty in a competitive environment.

The results also demonstrate that there is no single obvious definition of an "informed" strategy. A bot can evaluate an opponent based on their immediate actions, their long-term average performance, their reputation among other players, or its own previous experiences with them. The simulation provides a way to compare these different approaches.

---

 Future Experiments

There are several ways the simulation could be expanded on that I think would be very fun.

### Randomize the Number of Matchups

Instead of ensuring that every possible matchup occurs a predetermined number of times, future versions could use completely random matchmaking for the entire simulation.

This would make the population even less predictable and would allow some strategies to encounter certain opponents much more frequently than others.

### More Aggressive Strategies

Additional strategies could be designed specifically around exploitation, retaliation, or manipulation.

This could help determine whether Grudge's success comes from its ability to cooperate or from its ability to punish individual defectors.

### Add a Third Option

A future version could introduce a third possible decision beyond cooperation and defection.

For example, a player could have an option to **wait, punish, negotiate, or change opponents**, creating a more complex strategic environment.

### Larger Populations

Increasing the number of copies of each strategy could provide an even stronger test of Social Proof and other population-based strategies.

### More Complex Learning

Strategies could track more detailed information about individual opponents and the overall population. For example, a strategy could assign each opponent a reputation score that changes over time.

---

Final Takeaway

The main purpose of this simulation was to explore how different Prisoner's Dilemma strategies behave when interactions are dynamic rather than isolated.

The results suggest that strategies capable of remembering, adapting, and responding to information about other players can perform particularly well in this environment. At the same time, the experiments show that different kinds of information lead to very different strategic outcomes.

Most importantly, changing the matchmaking structure changes the game itself. By forcing strategies to interact with a constantly changing set of opponents, the simulation creates a setting where reputation, memory, adaptation, and population-level information become important parts of decision-making.

That makes the simulation useful not only for comparing classic Prisoner's Dilemma strategies, but also for experimenting with how different approaches to trust, retaliation, reputation, and social information might behave in a larger population.
