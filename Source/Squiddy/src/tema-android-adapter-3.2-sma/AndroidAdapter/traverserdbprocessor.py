#
# Copyright 2014 Shahriyar Amini
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

__author__ = 'shahriyar'
__copyright__ = 'Copyright 2014, Shahriyar Amini'

import pydot
import traverserdb as db
import sys
import os
import AndroidAdapter.constants as const
import AndroidAdapter.utils as utils
import AndroidAdapter.imagemagick as imagemagick

class TraverserDBProcesser:

	def __init__(self, path):
		self.__path = path
	
	def execute(self):
		if not os.path.exists(self.__path):
			print 'Database does not exist!'
			return

		self.__dirname = os.path.dirname(self.__path)
		utils.chdir(self.__dirname)

		self.__db = db.TraverserDB(self.__path)

		self.activityGraph()

		self.updateTaintLogActivities()

	def activityGraph(self):
		graph = pydot.Dot('CFG', graph_type='digraph')

		# get all the activities, add a node for them
		# for each node add an image attribute
		# for each node add an id equaling to the columns rowid

		dbActivities = self.__db.selectActivities()

		for dbActivity in dbActivities:
			print dbActivity

			rowid = dbActivity[0]
			absoluteName = dbActivity[1]
			screenshot = dbActivity[2]

			# resize the screenshot for node presentation
			if (screenshot):
				screenshotThumbnail = screenshot[:len(screenshot)-4] + '_thumb.png'
				print screenshot
				print screenshotThumbnail
				imagemagick.resize(screenshot, screenshotThumbnail, const.NODE_WIDTH, const.NODE_HEIGHT) 

			dbActivitySplit = absoluteName.split('.')
			name = dbActivitySplit[len(dbActivitySplit)-1]
			# fixed size made grappa viz crash
			# fixed size use to have small node sizes
			#node = pydot.Node(name, id=rowid, image=screenshot, imagescale='true', shape='box', fixedsize='true') 
			#node = pydot.Node(name, id=rowid, image=screenshotThumbnail, comment=screenshot, imagescale='true', shape='box') 

			#jason asked for nodes to have same length and width. when fixedsize is given. the width and height should be mentioned.
			#node = pydot.Node(name, id=rowid, image=screenshotThumbnail, imagescale='true', shape='box') 
			node = pydot.Node(name, id=rowid, image=screenshotThumbnail, imagescale='true', shape='box', width="2.25", height="0.75") 
			graph.add_node(node)

		activityChanges = self.__db.selectActionsByResults(const.ACTION_RESULT_ACTIVITY_CHANGE)

		activityIndex = 1
		nextActivityIndex = 4

		mainActivity = None

		for change in activityChanges:
			print change

			activity = self.__db.selectActivityName(change[activityIndex])
			nextActivity = self.__db.selectActivityName(change[nextActivityIndex])

			print activity, nextActivity, '\n'

			if (activity is None):
				mainActivity = nextActivity
				package = mainActivity.split('/')[0]
				print 'Package name: ', package
			elif (nextActivity is not None):
				# TODO: following split may not work for all applications
				activitySplit = activity.split('.')
				src = activitySplit[len(activitySplit)-1]

				if (nextActivity.startswith(package)):
					nextActivitySplit = nextActivity.split('.')
					dst = nextActivitySplit[len(nextActivitySplit)-1]
				else:
					dst = nextActivity.split('/')[0]

				# see if edge already exists
				edgeList = graph.get_edge(src, dst)

				# this checks to see that we dont add an edge multiple times
				if (edgeList is None or len(edgeList) == 0):
					edge = pydot.Edge((src, dst))
					graph.add_edge(edge)

		graph.write('CFG.dot')
		graph.write_dot('CFG_Layout.dot')
		graph.write_xdot('CFG_Layout.xdot')
		graph.write_jpg('CFG.jpg')

	def updateTaintLogActivities(self):
		# type of all Taint events which we are interested in knowing the activity of
		# libcore.os.send is used for version 4.1.1 of taintdroid
		_type = ('OSNetworkSystem', 'SSLOutputStream', 'libcore.os.send')

		print 'Multi Type', _type

		results = self.__db.selectTaintLogByType(_type)

		# column indices for the results
		activityIndex = 1
		resultIndex = 2
		nextActivityIndex = 4
		_typeIndex = 6
		successIndex = 8

		for r in results:

			print '\n', r

			time = r[7]

			enclosingActions = self.__db.selectEnclosingActions(time)
			before = enclosingActions[0]
			after = enclosingActions[1]

			print before
			print after

			beforeCurrentActivity = None
			beforeNextActivity = None
			afterCurrentActivity = None
			afterNextActivity = None

			# if there were before and after actions at the time of the log
			# get the associated activities for the actions
			# note that the actions can result to a move from on activity to another
			if (before):
				beforeCurrentActivity = self.__db.selectActivity(before[activityIndex])
				beforeNextActivity = self.__db.selectActivity(before[nextActivityIndex])
			if (after):
				afterCurrentActivity = self.__db.selectActivity(after[activityIndex])
				afterNextActivity = self.__db.selectActivity(after[nextActivityIndex])

			print beforeCurrentActivity
			print beforeNextActivity
			print afterCurrentActivity
			print afterNextActivity

			taintLogActivityIndex = 0
			
			# if there were actions before before and after the taint
			if (before and after):
				if (beforeNextActivity and afterCurrentActivity and beforeNextActivity[0] == afterCurrentActivity[0]):
					taintLogActivityIndex = beforeNextActivity[0]
				elif (beforeCurrentActivity and afterCurrentActivity and beforeCurrentActivity[0] == afterCurrentActivity[0]):
					taintLogActivityIndex = beforeCurrentActivity[0]
				elif (beforeNextActivity):
					taintLogActivityIndex = beforeNextActivity[0]
				elif (beforeCurrentActivity):
					taintLogActivityIndex = beforeCurrentActivity[0]
			elif (before):
				if (beforeNextActivity):
					taintLogActivityIndex = beforeNextActivity[0]
				elif (beforeCurrentActivity):
					taintLogActivityIndex = beforeCurrentActivity[0]
			elif (after):
				if (afterCurrentActivity):
					taintLogActivityIndex = afterCurrentActivity[0]

			"""
			# this used to be the previous code for mapping taint logs to activities
			if (beforeNextActivity and afterCurrentActivity):
				# if the activity ids match we know what activity it is 
				if (beforeNextActivity[0] == afterCurrentActivity[0]):
					taintLogActivityIndex = beforeNextActivity[0]
					print 'Activity', beforeNextActivity[1]
				# otherwise if the current activity of both match, then we know what activity we are in
				elif (beforeCurrentActivity and beforeCurrentActivity[0] == afterCurrentActivity[0]):
					taintLogActivityIndex = beforeCurrentActivity[0]
					print 'Activity', beforeCurrentActivity[1]
			"""

			if (taintLogActivityIndex != 0):
				self.__db.updateTaintLogActivity(r[0], taintLogActivityIndex)

			# TODO: not doing guess work at this point
			# we cannot only have the after activity as it would be a poor guess
			# but if we have a before activity and it is less than s seconds old, maybe we can use it?


if __name__ == "__main__":
	args = sys.argv
	if (len(args) < 2):
		print 'Requires path of traversal database'
		print 'e.g., /Users/shahriyar/Traverser/com.yelp.android-20120120-125119/com.yelp.android-20120120-125119.db'
	path = sys.argv[1]

	processor = TraverserDBProcesser(path)
	processor.execute()
