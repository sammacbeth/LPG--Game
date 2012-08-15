CREATE TABLE IF NOT EXISTS simulationsummary (
	"simId" bigint NOT NULL REFERENCES simulations,
	name varchar(255) NOT NULL,
	cluster int NOT NULL,
	"ut. C" float,
	"stddev ut. C" float,
	"ut. NC" float,
	"stddev ut. NC" float,
	"total ut." float,
	"rem. C" int,
	"rem. NC" int,
	PRIMARY KEY ("simId", cluster)
)
