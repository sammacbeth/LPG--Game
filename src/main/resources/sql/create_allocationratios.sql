CREATE OR REPLACE VIEW allocationRatios AS 
	SELECT t."simId",
	a.name,
	t."time",
	t.state->'cluster' AS "cluster",
	LEAST(CAST(t.state->'r' AS float) / CAST( t.state->'d' AS float) ,1) AS "ratio"
	FROM agenttransient AS t
	JOIN agents AS a ON a."simId" = t."simId" AND a.aid = t.aid
	WHERE CAST(t.state->'d' AS float) > 0

