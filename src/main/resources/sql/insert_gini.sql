UPDATE environmenttransient
SET state = state || hstore(?,?)
WHERE "simId" = ?
AND "time" = ?
