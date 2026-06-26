This file contains code for a simulated prisoner's dilemma with 10 different bots (strategies). I got this idea from watching  Veritasium's video on game theory, but I felt that the version shown wasn't very realistic. Specifically, the simulation had each strategy face off against another strategy 200 times in a row before moving on to a different bot in a round-robin format. So, for my simulation, I decided that after each round, they would have new random pairings. This means that two strategies could face off 5 times in a row and then not face off for 100 rounds. This provides a more realistic representation of real-life interactions, since you are likely to be talking to different people each time. Now, strategies can be created to account for how other bots play against each other in the immediate previous rounds, and strategies may be forced to adapt depending on their next opponent. The strategies I used for this were relatively simple and are listed below (some were made to represent more realistic mindsets people may have). 

Tic-for-tac: Cooperates on the first turn, then copies the opponent’s last move against them 
Tic-for-2-tacs: Always cooperates unless opponent deflects twice in a row
Altruistic: Cooperates 95% of the time 
Greedy: Always deflects
Random: 50/50 chance 
Opportunist Type-A:  Defects if opponent's average earnings per round > x (set to 3 usually )
Opportunist Type-B: Deflect if the opponent defects more than it cooperates 
Social Proof: Bases its decision on how other bots played its opponent
Temper: Cooperates until someone deflects. Then it will deflect for a random number of turns, regardless of the opponent
Grudge: Cooperates until someone deflects. Then it will deflect against those bots specifically for the rest of the simulation 

