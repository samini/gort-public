import Queue

# one can add objects to predefined classes!
if __name__ == "__main__":
	q = Queue.Queue()
	q.marked = True

	print q.marked 

	q.marked = False

	print q.marked

	comp = lambda x: True

	print comp(-1)	
	print comp(0)	
	print comp(1)	
