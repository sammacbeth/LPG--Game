import _mysql
import mysql
import sys
import argparse

parser = argparse.ArgumentParser(description='')
mysql.add_args(parser)
args = parser.parse_args()
con = None

try:
	con = mysql.connect(args)

	print 'Summarising data for simulations in {}'.format(args.db)

	print "Creating views..."
	# Create mysql views
	con.query("""
		CREATE OR REPLACE VIEW `allocationRatios` AS 
		select `playerScore`.`simID` AS `ID`,
		`playerScore`.`player` AS `player`,
		`playerScore`.`round` AS `round`,
		`playerScore`.`cluster` AS `cluster`,
		least((`playerScore`.`r` / `playerScore`.`d`),1) AS `ratio` 
		from `playerScore` where (`playerScore`.`d` > 0);
	""")
	con.query("""
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
	""")

	con.query("SELECT s.ID, s.name FROM simulations AS s LEFT JOIN simulationSummary ss ON ss.ID = s.ID WHERE state LIKE 'COMPLETE' AND ss.ID IS NULL");
	simIt = con.store_result()
	print '{} simulations to process...'.format(simIt.num_rows())
	for sim in simIt.fetch_row(maxrows=0):
		simId = int(sim[0])
		print '.',
		sys.stdout.flush()

		con.query("REPLACE INTO `simulationFairness` SELECT `f`.`ID` AS `ID`, `f`.`cluster`, std(`f`.`ratio`) AS `fairness`, NULL, NULL, NULL from `allocationRatios` `f` WHERE `f`.`ID` = %d group by `f`.`ID`, `f`.`cluster`;" % simId)
		con.query("""
		REPLACE INTO `simulationFairness`
		SELECT `r`.`ID`, 
		r.cluster, 
		f.fairness, 
		STD( `r`.`fairness`), 
		STD( CASE WHEN r.player like 'c%%' then r.fairness else null end),
		STD( CASE WHEN r.player like 'nc%%' then r.fairness else null end)
		FROM (
			SELECT `f`.`ID` AS `ID`, `f`.`player`, `f`.`cluster`,  avg(`f`.`ratio`) AS `fairness` from `allocationRatios` `f` WHERE `f`.`ID` = %d group by `f`.`ID`, `f`.`player`, `f`.`cluster`
			) AS `r`
		LEFT JOIN `simulationFairness` AS `f` ON f.simID = r.ID AND f.cluster = r.cluster
		GROUP BY `r`.`ID`, r.cluster
		""" % simId )

		con.query("REPLACE INTO `aggregatePlayerScore` SELECT `playerScore`.`simID` AS `simID`, `playerScore`.`player` AS `player`, `playerScore`.`cluster`, sum(`playerScore`.`U`) AS `USum` FROM `playerScore` WHERE `playerScore`.`simID` = %d GROUP BY `playerScore`.`simID`,`playerScore`.`player`,`playerScore`.`cluster`;" % simId)

		cutoff = 499
		name = sim[1]
		utilities = """
			SELECT
			ag.cluster,
			avg(CASE WHEN `ag`.`player` like 'c%%' then `ag`.`USum` else null end) AS `ut. C`, 
			stddev(CASE WHEN `ag`.`player` like 'c%%' then `ag`.`USum` else null end) AS `stddev ut. C`, 
			avg(CASE WHEN `ag`.`player` like 'nc%%' then `ag`.`USum` else null end) AS `ut. NC`, 
			stddev(CASE WHEN `ag`.`player` like 'nc%%' then `ag`.`USum` else null end) AS `stddev ut. NC` , 
			sum(`ag`.`USum`) AS `total ut.`
			FROM `aggregatePlayerScore` `ag`
			WHERE `ag`.`simID` = {0}
			GROUP BY ag.cluster
			""".format(simId)
		con.query(utilities)
		r2 = con.store_result()
		for cl in r2.fetch_row(maxrows=0):
			cluster = cl[0]

			con.query("select count(0) from `playerScore` `crem` where `crem`.`simID` = {0} and `crem`.`player` like 'c%%' and `crem`.`round` = {2} and `crem`.`cluster` = {1}".format(simId, cluster, cutoff))
			r3 = con.store_result()
			crem = r3.fetch_row(maxrows=0)[0][0]
			con.query("select count(0) from `playerScore` `crem` where `crem`.`simID` = {0} and `crem`.`player` like 'nc%%' and `crem`.`round` = {2} and `crem`.`cluster` = {1}".format(simId, cluster, cutoff))
			r3 = con.store_result()
			ncrem = r3.fetch_row(maxrows=0)[0][0]
			con.query("""
				SELECT avg(`f`.`fairness`) AS `fairness`, 
					avg(`f`.`fairness2`) AS `fairness2`,
					avg(`f`.`fairness2 C`),
					avg(`f`.`fairness2 NC`)
				FROM `simulationFairness` `f`
				WHERE f.simID = {0} AND f.cluster = {1}
				""".format(simId, cluster))
			r3 = con.store_result()
			fairness = r3.fetch_row(maxrows=0)[0]
			cl2 = range(6)
			for i in range(len(cl)):
				if cl[i] == None:
					cl2[i] = '0'
				else:
					cl2[i] = cl[i]
			fairness2 = range(4)
			for i in range(len(fairness)):
				if fairness[i] == None:
					fairness2[i] = 'NULL'
				else:
					fairness2[i] = fairness[i]
			insert = "REPLACE INTO `simulationSummary` VALUES ({0}, '{1}', {2}, {3}, {4}, {5}, {6}, {7}, {8}, {9}, {10}, {11}, {12}, {13})".format(simId, name, cluster, cl2[1], cl2[2], cl2[3], cl2[4], cl2[5], crem, ncrem, fairness2[0], fairness2[1], fairness2[2], fairness2[3])
			#print insert
			con.query(insert)

	print 'done.'

except _mysql.Error, e:
	print "Error %d: %s" % (e.args[0], e.args[1])
	sys.exit(1)

finally:
	if con:
		con.close()

