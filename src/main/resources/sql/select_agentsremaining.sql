SELECT COUNT(*)
FROM agentTransient AS t
JOIN agents AS a
	ON t.aid = a.aid
	AND t."simId" = a."simId"
WHERE a."simId" = ?
	AND a."name" LIKE ?
	AND t."time" = ?

