CREATE OR REPLACE VIEW `allocationRatios` AS 
select `playerScore`.`simID` AS `ID`,
`playerScore`.`player` AS `player`,
`playerScore`.`round` AS `round`,
least((`playerScore`.`r` / `playerScore`.`d`),1) AS `ratio` 
from `playerScore` where (`playerScore`.`d` > 0);

CREATE OR REPLACE VIEW `simulationFairness` AS 
select `f`.`ID` AS `ID`,std(`f`.`ratio`) AS `fairness` 
from `allocationRatios` `f` group by `f`.`ID`;

CREATE OR REPLACE VIEW `aggregatePlayerScore` AS 
select `playerScore`.`simID` AS `simID`,
`playerScore`.`player` AS `player`,
sum(`playerScore`.`U`) AS `USum` 
from `playerScore` group by `playerScore`.`simID`,`playerScore`.`player`;

CREATE OR REPLACE VIEW `simulationSummary` AS 
select `s`.`id` AS `ID`,`s`.`Name` AS `Name`,
avg(`ac`.`USum`) AS `ut. C`,
avg(`anc`.`USum`) AS `ut. NC`,
(select count(0) from `playerScore` `crem` where ((`crem`.`simID` = `s`.`id`) and (`crem`.`player` like 'c%') and (`crem`.`round` = 499))) AS `rem. C`,
(select count(0) from `playerScore` `crem` where ((`crem`.`simID` = `s`.`id`) and (`crem`.`player` like 'nc%') and (`crem`.`round` = 499))) AS `rem. NC`,
avg(`f`.`fairness`) AS `fairness` 
from (((`simulations` `s` 
left join `t_aggregatePlayerScore` `ac` on(((`ac`.`simID` = `s`.`id`) and (`ac`.`player` like 'c%')))) 
left join `t_aggregatePlayerScore` `anc` on(((`anc`.`simID` = `s`.`id`) and (`anc`.`player` like 'nc%')))) 
left join `t_simulationFairness` `f` on((`f`.`simID` = `s`.`id`))) group by `s`.`id`;

CREATE OR REPLACE VIEW `aggregatedSimulations` AS 
select `t_simulationSummary`.`Name` AS `strategy`,
avg(`t_simulationSummary`.`ut. C`) AS `ut. C`,
std(`t_simulationSummary`.`ut. C`) AS `stddev ut. C`,
avg(`t_simulationSummary`.`ut. NC`) AS `ut. NC`,
std(`t_simulationSummary`.`ut. NC`) AS `stddev ut. NC`,
avg(`t_simulationSummary`.`rem. C`) AS `rem. C`,avg(`t_simulationSummary`.`rem. NC`) AS `rem. NC`,
avg(`t_simulationSummary`.`fairness`) AS `fairness`,
count(0) AS `repeats` from `t_simulationSummary` group by `t_simulationSummary`.`Name`;
