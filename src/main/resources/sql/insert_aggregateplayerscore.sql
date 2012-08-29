DELETE FROM aggregatePlayerScore WHERE "simId" = ?;
INSERT INTO aggregatePlayerScore
	SELECT a."simId", a."name", 
	CAST(t.state->'cluster' AS int) AS cluster, 
	SUM(CAST(t.state->'U' AS float)) AS usum 
	FROM agents AS a 
	LEFT JOIN  agenttransient AS t 
		ON a."simId" = t."simId" 
		AND a.aid = t.aid 
		AND exist(t.state, 'cluster') 
		AND exist(t.state, 'U') 
	WHERE a."simId" = ? AND exist(t.state, 'cluster') AND defined(t.state, 'cluster')
	GROUP BY a."simId", a.aid, t.state->'cluster';
