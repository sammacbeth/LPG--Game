CREATE OR REPLACE VIEW `allocationRatios` AS 
select `playerScore`.`simID` AS `ID`,
`playerScore`.`player` AS `player`,
`playerScore`.`round` AS `round`,
`playerScore`.`cluster` AS `cluster`,
least((`playerScore`.`r` / `playerScore`.`d`),1) AS `ratio` 
from `playerScore` where (`playerScore`.`d` > 0);

CREATE OR REPLACE VIEW `aggregatedSimulations` AS 
select `simulationSummary`.`Name` AS `strategy`,
`simulationSummary`.`cluster` AS `cluster`,
avg(`simulationSummary`.`ut. C`) AS `ut. C`,
std(`simulationSummary`.`ut. C`) AS `stddev ut. C`,
avg(`simulationSummary`.`ut. NC`) AS `ut. NC`,
std(`simulationSummary`.`ut. NC`) AS `stddev ut. NC`,
avg(`simulationSummary`.`rem. C`) AS `rem. C`,avg(`simulationSummary`.`rem. NC`) AS `rem. NC`,
avg(`simulationSummary`.`fairness`) AS `fairness`,
avg(`simulationSummary`.`fairness2`) AS `fairness2`,
avg(`simulationSummary`.`fairness2 C`) AS `fairness2 C`,
avg(`simulationSummary`.`fairness2 NC`) AS `fairness2 NC`,
count(0) AS `repeats` from `simulationSummary` group by `simulationSummary`.`Name`, `simulationSummary`.`cluster`;
