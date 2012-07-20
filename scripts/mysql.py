import _mysql

""" Add arguments to an argparse parser for configuring the mysql connection.
"""
def add_args(parser):
	parser.add_argument('--host', default='localhost', help='mysql host')
	parser.add_argument('--db', default='lpg_final', help='mysql db')
	parser.add_argument('--user', default='python', help='mysql user')
	parser.add_argument('--password', default='', help='mysql password')

""" Connect to the mysql server from the provided args.
"""
def connect(args):
	con = _mysql.connect(args.host, args.user, args.password, args.db)
	return con
