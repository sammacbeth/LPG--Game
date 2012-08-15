SELECT s.id, s."name", "finishTime" 
FROM simulations AS s
LEFT JOIN simulationsummary AS ss ON ss."simId" = s.id
WHERE s.state LIKE 'COMPLETE' AND ss."simId" IS NULL
ORDER BY s.id ASC
