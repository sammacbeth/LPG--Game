## LPG'

This is an implementation of the LPG' game using [Presage2] with the [Drools] rule engine. This is a variant of the Linear Public Good game and is described in the following papers:

 - Jeremy Pitt, Julia Schaumeier, Didac Busquests and Sam Macbeth, "Self-Organising Common-Pool Resource Allocation and Canons of Distributive Justice" _SASO 2012_.
 - Jeremy Pitt and Julia Schaumeier, "Provision and Appropriation of Common-Pool Resources without Full Disclosure" _PRIMA 2012_
 
## Usage

You will need [maven] and a JDK installed.

Clone the repository and compile the sources:

```bash
git clone https://github.com/sammacbeth/LPG--Game.git
mvn compile
```

### Database Configuration

Data is stored into a PostgreSQL database (>= v9.1) and requires the [hstore] extension. You should modify the `src/main/resources/db.properties` file to give login details for your database. See the [presage2-sqldb] docs for full configuration options.

### CLI usage

The `lpg-cli` script provides and alias of `mvn exec:java` and can be used add and run simulations. Navigate to the project directory and run the script to no arguments to see all available commands:
```bash
cd /path/to/LPG--Game
./lpg-cli
```

The `insert` command includes three different experiments which were presented in the SASO paper. The script will insert a set of simulations with appropriate parameters for the scenarios under test. You must specify a number of repeats to run and optionally include a starting random seed. For example we could create 5 repeats of the 'het_hom' experiment, with seeds starting from 13 as follows:
```bash
./lpg-cli insert het_hom 5 --seed 13
```
This will insert the simulations into the database, but will not run them yet. To run simulations use the `runall` command. This runs all simulations which have yet to be executed.
```bash
./lpg-cli runall
```
Once simulations have completed we can process the data to create global metrics from each simulation. The `summarise` command does this:
```bash
./lpg-cli summarise
```
This creates several tables and views in the database with the relevant data.


 [Presage2]: http://www.presage2.info
 [Drools]: http://www.jboss.org/drools/
 [hstore]: http://www.postgresql.org/docs/9.1/static/hstore.html
 [presage2-sqldb]: https://github.com/Presage/presage2-sqldb
 [maven]: http://maven.apache.org/
