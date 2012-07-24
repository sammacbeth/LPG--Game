import _mysql
import sys
import argparse

def add_mysql_args(parser):
	parser.add_argument('--host', default='ee-sm1106.ee.ic.ac.uk', help='mysql host')
	parser.add_argument('--db', default='lpg_further', help='mysql db')
	parser.add_argument('--user', default='python', help='mysql user')
	parser.add_argument('--password', default='GdZKnDnQrx488PYw', help='mysql password')

def connect_mysql(args):
	con = _mysql.connect(args.host, args.user, args.password, args.db)
	return con

def add_global_args(parser):
	parser.add_argument('repeats', type=int, metavar='REPEATS', help='repeats')
	parser.add_argument('--seed', type=int, default=0, metavar='SEED', help='start seed')

def insert_simulation(con, name, rounds):
	con.query("INSERT INTO simulations (name,state,classname,finishTime) VALUES ('{}', '{}', '{}', {})".format(name, 'AUTO START','uk.ac.imperial.lpgdash.LPGGameSimulation', rounds))
	simID = con.insert_id()
	print "Created sim: {} - {}".format(simID, name)
	return simID

def add_parameter(con, simId, key, value):
	paramInsert = "INSERT INTO parameters (simID, name, value) VALUES (%d, '{}', '{}')" % simId
	con.query(paramInsert.format(key, value))

def lc_comparison(args):
	con = connect_mysql(args)
	for seed in range(args.repeats):
		for cluster in ['lc_f1a','lc_f1b','lc_f1c','lc_f2','lc_f3','lc_f4','lc_f5','lc_f6','lc_fixed','lc_so']:
			rounds = '1002'
			simID = insert_simulation(con, "{}".format(cluster), rounds)
			add_parameter(con, simID, 'finishTime', rounds)
			add_parameter(con, simID, 'alpha', '0.1')
			add_parameter(con, simID, 'beta', '0.1')
			add_parameter(con, simID, 'gamma', '0.1')
			add_parameter(con, simID, 'cCount', 20)
			add_parameter(con, simID, 'cPCheat', 0.02)
			add_parameter(con, simID, 'ncCount', 10)
			add_parameter(con, simID, 'ncPCheat', 0.25)
			add_parameter(con, simID, 'seed', seed + args.seed)
			add_parameter(con, simID, 'soHack', '1')
			add_parameter(con, simID, 'clusters', cluster)
			add_parameter(con, simID, 'cheatOn', 'provision')

def het_hom(args):
	con = connect_mysql(args)
	for seed in range(args.repeats):
		for cluster in ['ration','random','lc_fixed','lc_so']:
			for pop in ['het01','hom01','hom04']:
				rounds = '1002'
				beta = '0.1'
				c = 30
				cPCheat = 0.0
				if pop == 'hom04':
					beta = '0.4'
				if pop == 'het01':
					c = 20
					cPCheat = 0.02
				simID = insert_simulation(con, "{0}_{1}".format(cluster, pop), rounds)
				add_parameter(con, simID, 'finishTime', rounds)
				add_parameter(con, simID, 'alpha', '0.1')
				add_parameter(con, simID, 'beta', beta)
				add_parameter(con, simID, 'gamma', '0.1')
				add_parameter(con, simID, 'cCount', c)
				add_parameter(con, simID, 'cPCheat', cPCheat)
				add_parameter(con, simID, 'ncCount', 30-c)
				add_parameter(con, simID, 'ncPCheat', 0.25)
				add_parameter(con, simID, 'seed', seed + args.seed)
				add_parameter(con, simID, 'soHack', '1')
				add_parameter(con, simID, 'clusters', cluster)
				add_parameter(con, simID, 'cheatOn', 'provision')

def multi_cluster(args):
	con = connect_mysql(args)
	cluster = "lc_so.random"
	for seed in range(args.repeats):
		for beta in ['0.1','0.4']:
			rounds = '3000'
			simID = insert_simulation(con, "3_{0}_beta={1}".format(cluster, beta), rounds)
			add_parameter(con, simID, 'finishTime', rounds)
			add_parameter(con, simID, 'alpha', '0.1')
			add_parameter(con, simID, 'beta', beta)
			add_parameter(con, simID, 'gamma', '0.1')
			add_parameter(con, simID, 'cCount', 20)
			add_parameter(con, simID, 'cPCheat', 0.02)
			add_parameter(con, simID, 'ncCount', 20)
			add_parameter(con, simID, 'ncPCheat', 0.25)
			add_parameter(con, simID, 'seed', seed + args.seed)
			add_parameter(con, simID, 'soHack', '1')
			add_parameter(con, simID, 'clusters', cluster)
			add_parameter(con, simID, 'cheatOn', 'provision')

def nc_proportions(args):
	con = connect_mysql(args)
	for seed in range(args.repeats):
		for cluster in ['random','lc_fixed','lc_so']:
			for c in [30,27,24,21,18,15,12,9,6,3,0]:
				rounds = '1002'
				simID = insert_simulation(con, "{0}_{1}c_{2}nc".format(cluster, c, 30-c), rounds)
				add_parameter(con, simID, 'finishTime', rounds)
				add_parameter(con, simID, 'alpha', '0.1')
				add_parameter(con, simID, 'beta', '0.1')
				add_parameter(con, simID, 'gamma', '0.1')
				add_parameter(con, simID, 'cCount', c)
				add_parameter(con, simID, 'cPCheat', 0.02)
				add_parameter(con, simID, 'ncCount', 30-c)
				add_parameter(con, simID, 'ncPCheat', 0.25)
				add_parameter(con, simID, 'seed', seed + args.seed)
				add_parameter(con, simID, 'soHack', '1')
				add_parameter(con, simID, 'clusters', cluster)
				add_parameter(con, simID, 'cheatOn', 'provision')

parser = argparse.ArgumentParser(description='')
add_mysql_args(parser)

subparsers = parser.add_subparsers(help='command help')
parsers = []

parsers.append(subparsers.add_parser('lc_comparison'))
add_mysql_args(parsers[-1])
add_global_args(parsers[-1])
parsers[-1].set_defaults(func=lc_comparison)

parsers.append(subparsers.add_parser('het_hom'))
add_mysql_args(parsers[-1])
add_global_args(parsers[-1])
parsers[-1].set_defaults(func=het_hom)

parsers.append(subparsers.add_parser('multi_cluster'))
add_mysql_args(parsers[-1])
add_global_args(parsers[-1])
parsers[-1].set_defaults(func=multi_cluster)

parsers.append(subparsers.add_parser('nc_proportions'))
add_mysql_args(parsers[-1])
add_global_args(parsers[-1])
parsers[-1].set_defaults(func=nc_proportions)

args = parser.parse_args()
args.func(args)

