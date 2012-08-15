CREATE TABLE IF NOT EXISTS aggregatePlayerScore (
	"simId" bigint NOT NULL REFERENCES simulations,
	player varchar(10) NOT NULL,
	cluster int NOT NULL,
	USum float NOT NULL,
	PRIMARY KEY ("simId", player, cluster)
)
