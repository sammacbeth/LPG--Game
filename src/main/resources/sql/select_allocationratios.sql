SELECT name, ratio 
FROM allocationratios 
WHERE \"simId\" = ? AND CAST(cluster AS int) = ? AND time = ?
