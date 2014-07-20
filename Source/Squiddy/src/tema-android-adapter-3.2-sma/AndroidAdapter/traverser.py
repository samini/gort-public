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

import time 

import datetime
#import pytz

import AndroidAdapter.constants as const 
#import AndroidAdapter.monkeydriver as monkeydriver
#import AndroidAdapter.guireader as guireader
import AndroidAdapter.adbcommands as adbcommands
import AndroidAdapter.utils as utils
import AndroidAdapter.imagemagick as imagemagick
import AndroidAdapter.taintlogger as taintlogger
import AndroidAdapter.designrecognizer as designrecognizer
import AndroidAdapter.keyboardrecognizer as keyboardrecognizer
from AndroidAdapter.traverserdb import TraverserDB, Traversal, History, State, Sequence, Interaction, Component, Screenshot
from AndroidAdapter.ui_keywords import LaunchApp, PressKey, TapObject

import networkx as nx
from networkx.algorithms.traversal.breadth_first_search import bfs_edges

#add json support to update progress status
import json

# using pydot we can create the layout of the graph
import pydot

import sys
#import os to get HOME variable
import os

#sample calling of the application
#tema.android-adapter -e com.yelp.android::.ui.activities.RootActivity 393524A9C33F00EC

class Traverser(object):
	PRINT_PROPERTIES = False
	AREA_THRESHOLD_MULTIPLIER = 0.5
	SCREENSHOT_THRESHOLD_MULTIPLIER = 0.15
	SLEEP_DURATION_ONE_SECOND = 1
	SLEEP_DURATION_FIVE_SECONDS = 5
	SLEEP_DURATION = SLEEP_DURATION_FIVE_SECONDS
	MILLISECONDS_IN_MINUTE = 60 * 1000
	SECONDS_IN_MINUTE = 60
	# if after THRESHOLD interactions still in the same 
	# activity, we are stuck and need to try something new
	STUCK_IN_ACTIVITY_THRESHOLD = 50
	
	#EDGE_ATTRIBUTE_KEY = 'sequence_ids'
	
	#also write the graphs to pickle files
	WRITE_PICKLE = False
	
	#try the back button on dialog boxes this number of times
	DIALOG_BACK_BUTTON_TRIES = 3
	
	#try the back button on keyboards this number of times
	KEYBOARD_BACK_BUTTON_TRIES = 3
	
	#timezone to use to get timestamps, if None uses localtime
	#if pytz.UTC uses utc time
	#TIMEZONE = pytz.utc
	#TIMEZONE = None

	# this file is modeled after the original test runner
	# as such the constructor parameters were not moddifed
	def __init__(self, target, monkey, reader):
		# Target is the Serial # string for the device running the traversal 
		self.__target = target
		self.__monkey = monkey
		self.__guireader = reader
		#self.__device = 'crespo'
		self.__app = None
		self.__apk = None
		self.__package = None
		self.__launch_activity = None
		self.__db = None
		#self.__rootactivity = None
		self.__taint_logger_thread = None
		
		# start time of the algorithm so we can cap the total time length of traversal
		self.__start_time = None
		
		# a variable to hold the activity of the first state we arrive in
		self.__first_state_activity = None
		
		# new variables to plug into Gort
		self.__traversal_directory = None
		self.__db_addr = None
		self.__app_index = None
		self.__work_directory = None
		self.__traversal_timeout = None
		
		self.__screen_size = None
		self.__screen_area = None
		self.__area_threshold = None
		self.__screenshot_threshold = None
		
		self.__traversal = None
		
		# graphs for states and explorations
		self.__state_graph = None
		self.__exploration_graph = None
		
		# create a dictionary for states we have seen so far
		self.__states = dict()
		
		# process_dialog_attemps
		self.__process_dialog_attempts = set()
		
		# hold offsets for time of the devices
		self.__time_offset_seconds = None
		self.__timedelta = None
		
		# hold the project directory for access to other folders
		self.__project_directory = None
		
		# variables to store progress info of the app
		self.__gtp = None
		self.__progress_directory = None
	
	def init(self, active_target, traversal_directory, db_addr, app_index, traversal_timeout=0):
		if (traversal_directory is None):
			print 'Traversal folder is None.'
			return False
		elif not os.path.exists(traversal_directory):
			print 'Traversal folder does not exist.'
			print 'Argument supplied', traversal_directory
			return False
		elif not os.path.isdir(traversal_directory):
			print 'Traversal folder is not a directory.'
			print 'Argument supplied', traversal_directory
			return False
		
		# set the traversal directory for this traversal instance
		self.__traversal_directory = traversal_directory
		
		# get the project directory
		if not self._init_project_directory():
			print 'Project directory is invalid.'
			return False
		else:
			print 'Project folder:', self.__project_directory
			if not os.path.exists(self.__project_directory):
				print 'Project folder does not exist.'
				return False
			elif not os.path.isdir(self.__project_directory):
				print 'Project folder is not a directory.'
				return False
		
		if (db_addr is None):
			print 'Supplied database is None.'
			return False
		
		if (app_index is None):
			print 'App index is None.'
			return False
		
		if (isinstance(app_index, str) and not app_index.isdigit()):
			print 'App index requires an integer value'
			return False
		
		# Active target is the actual Android Target
		self.__active_target = active_target
		self.__db_addr = db_addr
		self.__app_index = app_index
		self.__traversal_timeout = traversal_timeout
		
		return True

	def take_screenshot(self, out_path = None):
		if not out_path:
			out_path = "%s/%s_%s.png" % (self.__work_directory, self.__app.apk, utils.timeString())
			
		#print 'Screenshot path', out_path
		
		self.takeScreenshot(out_path)
		
		return out_path

	# old function inherited from testrunner
	def takeScreenshot(self, out_path = None):
		print 'Taking screenshot', os.path.abspath(out_path)
		
		before = time.time()
		
		self.__guireader.takeScreenshot(out_path)
		
		after = time.time()
		duration = after - before
		
		print 'Screenshot took', duration, 'seconds to take.'


	def _write_graphs(self):
		print '\nWriting graphs to file...'
		
		if (self.__state_graph):
			self._write_state_graph()
			if Traverser.WRITE_PICKLE:
				nx.write_gpickle(self.__state_graph, '%s/graph_state.gpickle' % self.__work_directory)
			
		if (self.__exploration_graph):
			filename = '%s/graph_exploration.dot' % self.__work_directory
			nx.write_dot(self.__state_graph, '%s/graph_exploration.dot' % self.__work_directory)
			if Traverser.WRITE_PICKLE:
				nx.write_gpickle(self.__state_graph, '%s/graph_exploration.gpickle' % self.__work_directory)
				
		print 'Done.\n'
	
	# have to process the state graph written by nx to include layout
	# information created by pydot	
	def _write_state_graph(self):
		file_path = '%s/graph_state.dot' % self.__work_directory
		nx.write_dot(self.__state_graph, file_path)
		graph = pydot.graph_from_dot_file(file_path)
		
		# get the graph nodes
		nodes = graph.get_nodes()
		
		if nodes:
			for node in nodes:
				name = node.get_name()
				# id is set based on the id column in state table
				if name:
					# an double quote is read around the name
					node.set('id', int(name.split('-')[-1].strip('"')))
					
				try:
					node.set_shape('box')
				except AttributeError:
					node.set('shape', 'box')
					
				try:
					node.set_width('3')
				except AttributeError:
					node.set('width', '3')
				
				try:
					node.set_height('1')
				except AttributeError:
					node.set('height', '1')
					
				try:
					node.set_imagescale('true')
				except AttributeError:
					node.set('imagescale', 'true')
				
				#TODO: also set the nodes name and the nodes image
		
		"""	
		edges = graph.get_edges()
		
		if edges:
			for edge in edges:
				pass
		"""
		
		graph.write_dot('%s/graph_state_layout.dot' % self.__work_directory)

	# the execute function plugs into the monkey driver
	# the guireader and also the active tema testrunner
	# to execute keywords in a sequence that traverses the app
	def execute(self):

		# set the start time of exploration
		self.__start_time = time.time()

		# connect to the database 
		if self._init_traverser_db():
			print '\nSuccessfully connected to the database.'
		else:
			print '\nError connecting to the database.'
			return
		
		if self._init_app():
			print '\nProcessing', self.__app.apk
			self.__apk = self.__app.apk
			self.__package = self.__app.package
		else:
			print '\nCould not obtain app information from the database'
			return
		
		if not self._init_gtp():
			print '\nCould not initialize the app progress information.'
			return
		
		self.__launch_activity = self.__app.launch_activity()
		
		if self.__launch_activity:
			print 'App main activity', self.__launch_activity.name
		else:
			print 'Could not find main activity to launch'
			return

		self._wait_for_boot_complete()
		
		self._init_time_offset()
		
		self._init_screen_properties()
		
		print 'Traversing application:', self.__apk
		
		# kill the app in case it is already Running
		# killProcess only kills current activity, not the whole package
		adbcommands.forceStop(self.__target, self.__package)

		# start taint logger before the app starts as the app
		# can send sensitive data as soon as it opens
		self._init_traversal_env()
			
		self._start_taint_logger()

		launch_successful = self._launch_app()
		
		if launch_successful:
			
			# print a separator to set up initialization from actual traversal
			print '\n===============================================================================\n'
			
			# initialize the traversal graphs
			self._init_graphs()
			
			# old implementation
			self._traverse()
			
			# write the state and exploration graph to file
			self._write_graphs()
			
			# mark the traversal as finished
			self.__traversal.finished = True
			# if the traversal is not partial make sure it is marked as so
			# it could be None
			if not self.__traversal.partial:
				self.__traversal.partial = False
			self.__db.merge_traversal_item(self.__traversal)
			
			# mark the progress file as dynamic analysis performed
			self._set_gtp()
			
			# after traversal is over. wait and also stop the Taintdroid logger thread
			utils.wait(60 * Traverser.SLEEP_DURATION_ONE_SECOND) # one minute, this make sure taintlog has a chance to flush its buffer
		
		self._stop_taint_logger()
		
		# wait for the logger thread to stop
		utils.wait(Traverser.SLEEP_DURATION_FIVE_SECONDS)
		
		self.__db.close()
		
	def _wait_for_boot_complete(self):
		print ''
		print "Checking android device boot completion..."
		while not adbcommands.bootCompleted(self.__target):
			utils.wait(1)
		print "Device boot has completed"
		
	def _init_project_directory(self):
		if not self.__traversal_directory:
			return None
		
		self.__project_directory = os.path.abspath(os.path.join(self.__traversal_directory, os.pardir))
		
		return self.__project_directory
		
	def _init_screen_properties(self):
		print '\nObtaining screen size values...'
		
		self.__screen_size = self.__monkey.getScreenSize()
		print "Device screen size:", self.__screen_size

		self.__screen_area = utils.area(self.__screen_size)
		print "Device screen area:", self.__screen_area

		self.__area_threshold = int(Traverser.AREA_THRESHOLD_MULTIPLIER * self.__screen_area)
		print "Area threshold:", self.__area_threshold

		self.__screenshot_threshold = int(Traverser.SCREENSHOT_THRESHOLD_MULTIPLIER * self.__screen_area)
		print 'Screenshot threshold:', self.__screenshot_threshold
		
		print ''
		
	def _init_time_offset(self):
		print '\nGetting time offset'
		
		host_time_seconds_initial = time.time()
		device_time_seconds = adbcommands.deviceTime(self.__target)
		host_time_seconds_final = time.time()
		
		host_time_seconds = (host_time_seconds_initial + host_time_seconds_final) / 2.0
		
		if host_time_seconds and device_time_seconds:
			self.__time_offset_seconds = device_time_seconds - host_time_seconds
			self.__timedelta = datetime.timedelta(seconds=self.__time_offset_seconds)
		else:
			self.__time_offset_seconds = 0
			self.__timedelta = datetime.timedelta(0)
			
		print 'Time offset seconds', self.__time_offset_seconds
		print 'Time offset timedelta', self.__timedelta
		print 'Done'
	
	def _init_traversal_env(self):
		timestamp = self._get_time()
		
		print '\nTraversal time:', timestamp.isoformat()
		
		print 'Creating traversal directory...'
		
		# creating a directory for output and change to it as working directory
		relative_directory_name = '%s_%s' % (self.__app.apk, utils.timeString(timestamp))
		self.__work_directory = '%s/%s' % (self.__traversal_directory, relative_directory_name)
		
		utils.mkdir(self.__work_directory)

		print 'Created work directory', self.__work_directory
		print 'Changing to working directory...'
		utils.chdir(self.__work_directory)
		print 'Done'
		
		print '\nResults for this instance will be written to', self.__work_directory
		
		print 'Creating traversal object'
		traversal = Traversal()
		traversal.timestamp = timestamp
		traversal.directory = relative_directory_name
		traversal.app = self.__app
		
		self.__db.insert_traversal(traversal)
		
		self.__traversal = traversal
		print 'Done'
	
	def _init_traverser_db(self):
		print '\nInitializing traverser database...'
		self.__db = TraverserDB(self.__db_addr)
		if self.__db.connect():
			self.__db.prepare_tables()
			return True
		
		return False
	
	def _init_app(self):
		app = self.__db.select_app(self.__app_index)
		
		if app is None:
			return False
		
		self.__app = app
		
		print 'Activities present in this app:'
		for activity in self.__app.activities:
			print activity.name
		
		return True
	
	def _init_gtp(self):
		if not self._get_progress_directory():
			print 'Cannot get progress directory.'
			return False
		
		if not self._get_progress_filename():
			print 'Cannot get progress filename.'
			return False
		
		if not self._get_gtp():
			print 'Cannot get progress file.'
			return False
		
		print 'Progress file content:'
		print self._get_gtp_content(), '\n'
		
		return True
	
	def _init_graphs(self):
		print '\nInitializing State and Exploration graphs...'
		if self.__state_graph is None:
			self.__state_graph = nx.DiGraph()
			
		if self.__exploration_graph is None:
			self.__exploration_graph = nx.DiGraph()
		
	def _start_taint_logger(self):
		print '\nStarting taint logger...'
		self.__taint_logger_thread = taintlogger.TaintLoggerThread(self.__db, self.__traversal.id, self.__target) 
		self.__taint_logger_thread.start()
		print 'Taint logger started.'
		
	def _stop_taint_logger(self):
		print '\nStopping Taintdroid logger thread...'
		
		if self.__taint_logger_thread is None:
			print 'Taintdroid logger thread object is None'
			return
		
		self.__taint_logger_thread.stop()
		print 'Taintdroid logger thread stopped.'
	
	def _traverse(self):
		print 'Running traversal algorithm...'
		
		# create the first state and add edges to the states and exploration graphs
		# for each sequence of the state
		# since we were able to launch the app, last_interacton_succeeded
		current = Environment(timestamp=self.__traversal.timestamp, 
						interaction=self.__db.select_insert_launch_interaction(self.__traversal.id, self.__launch_activity.name),
						interaction_timestamp=self.__traversal.timestamp,
						success=True)
		
		# also keep track of the prev environment
		prev = None
		
		# a variable keeping whether the traversal should continue or not		
		run = True
		
		# counter to see how long we have been in the same state without a change
		stuck_counter = 0
		
		dialog_tried_back = 0
		keyboard_tried_back = 0
		outside_app_tried_back = False
		
		last_graph_write_time = time.time()
		
		# if timeout is 0. we continue until until the traversal stops.
		# otherwise we stop after the timeout has been reached
		while (run):
			
			prev = current
			# get the current state and add it to the graphs if it is not in there
			current_state = self._get_current_state()
			
			# we always add a node, if nodes exist, this is a nop
			self._add_node(current_state)
			
			# create a new Enviroment to describe our current environmental state
			current = Environment(self._get_time(),
								current_state,
								self.__db.select_state_last_screenshot(current_state.id),
								self._keyboard_shown(current_state),
								self._dialog_present(current_state))
			
			# insert the history based on the past environment and the current one
			self._record_history(prev, current)
			
			# look at the last interaction and add an edge to the graphs if need be
			# also looks at the last interaction and checks to see if it remains 
			# deterministic
			self._update_graph_edges(prev, current)
			
			# write the graphs to files every minute to save progress
			if (time.time() - last_graph_write_time >= Traverser.SECONDS_IN_MINUTE):
				self._write_graphs()
				last_graph_write_time = time.time()
			
			# check if the timeout has been reached. we do the above steps
			# to record results from the last interaction
			if (self.__traversal_timeout > 0):
				if (time.time() - self.__start_time >= self.__traversal_timeout * Traverser.SECONDS_IN_MINUTE):
					self.__traversal.partial = True
					print '\nTraversal timeout has been reached.\n'
					return
			
			# for states to be the same, their hash has to be equal
			if current.state and prev.state:
				if current.state.hash == prev.state.hash:
					stuck_counter += 1
			
			
			if stuck_counter > Traverser.STUCK_IN_ACTIVITY_THRESHOLD:
				print 'Traversal seems to be stuck. Re-launching the application'
				# re-launch the app
				self._launch_activity(current, self.__launch_activity)
				stuck_counter = 0
				# wait a few extra seconds to stabalize
				utils.wait(Traverser.SLEEP_DURATION_FIVE_SECONDS)
				continue
			
			# check for keyboards and process it
			if current.keyboard_shown:
				print 'Keyboard is present'
				# first try just pressing the back button
				if keyboard_tried_back < Traverser.KEYBOARD_BACK_BUTTON_TRIES:
					print 'Trying back button...'
					keyboard_tried_back += 1
					self._press_back(current)
					continue
				print 'Keyboard back button tries reaches, continue anyway'
			
			# check for dialogs and process it
			if current.dialog_present:
				print 'Dialog box is present.'
				# first try just pressing the back button
				if dialog_tried_back < Traverser.DIALOG_BACK_BUTTON_TRIES:
					print 'Trying back button...'
					dialog_tried_back += 1
					self._press_back(current)
					continue
				else:
					print 'Back button not successful. Processing dialog...'
					self._process_dialog(current)
					continue
			else:
				# reset dialog related variables
				dialog_tried_back = 0
				# process dialog stores the buttons it tries, these need to be reset
				self._reset_process_dialog_attempts()
				
			# if we are outside of the app, try a back button
			if not self._activity_in_package(current.state.activity.name):
				print 'Activity is not in package:', current.state.activity.name
				if not outside_app_tried_back:
					print 'Trying back button...'
					outside_app_tried_back = True
					self._press_back(current)
					continue
				else:
					print 'Re-launching the application.'
					# if the back button has not been successful
					# try an activity launch to the application's launch activity
					self._launch_activity(current, self.__launch_activity)
					continue
			else:
				outside_app_tried_back = False
					
				
			# if the state has an unexplored transition try it
			unexplored = self.__db.select_first_unexplored_sequence(current_state.id)
			
			if unexplored:
				print 'Performing sequence', unexplored.id
				# TODO: a more complex traversal may have more than 1 interaction per sequence
				current.sequence = unexplored
				current.interaction = unexplored.interactions[0]
				
				# this component should have a way of telling if 
				# it skipped clicking so that something else can be tried
				self._perform_interaction(current)
			else:
				print 'State does not have unexplored sequences.'
				# otherwise find another state that has an unexplored sequence and 
				# move to that state
				explortation_graph_bfs_edges = bfs_edges(self.__exploration_graph, current.state)
				
				state_move_attempt = False
				
				for edge in explortation_graph_bfs_edges:
					print 'enter bfs edge loop'
					if not edge:
						print 'enter no edge'
						continue
					
					if not self.__db.select_first_unexplored_sequence(edge[1].id): 
						print 'No unexplored sequence in next state', edge[1]
						continue

					if not self._activity_in_package(edge[1].activity.name):
						print 'Next state', edge[1], 'is outside the package'
						continue
					
					# edges are tuples with start and end state
					# get the attribute for the edge
					edge_sequence_ids = self.__exploration_graph[edge[0]][edge[1]]['sequence_ids']
					
					# split the sequence ids and grab the first one
					# if this occurs enough times, the stuck threshold should
					# kick in...
					if not edge_sequence_ids:
						print 'enter no sequences'
						continue
					
					for sequence_id in edge_sequence_ids:
						print 'enter sequence loop'
						sequence = self.__db.select_sequence(sequence_id)
						if not sequence:
							print 'enter no sequence'
							continue
						
						if not sequence.interactions:
							print 'enter no interactions' 
							continue
						
						print 'Attempting to move to a different state...'
						current.sequence = sequence
						current.interaction = sequence.interactions[0]
						
						print 'Going to edge:', edge
						#if successfully perform interaction, should continue main loop for record the ACTION_RESULT_OUTSIDE_APP
						state_move_attempt = self._perform_interaction(current)
						if state_move_attempt:
							break
					
					if state_move_attempt:
						break
				if state_move_attempt:
					print 'enter move attempt'
					continue
				else:
				# if all the explorations have been exhausted, try a back button
				# since the algorithm is doing a DFS, back button is useful
				#if not state_exhausted_tried_back:
					#state_exhausted_tried_back = True
					#if prev state is outside, so we relaunch the app and current state has nothing to explore
					print 'enter no move attempt'
					if not self._activity_in_package(prev.state.activity.name):
						print 'Prev state\'s Activity is not in package:', prev.state.activity.name
						#exit Traversing
						break
					else:
						print 'Trying back button...'
						self._press_back(current)
						continue
	
	def _record_history(self, prev, current):
		if not prev and not current:
			return
		
		h = History()
		h.starttimestamp = prev.timestamp
		h.endtimestamp = current.timestamp
		h.sequence = prev.sequence
		h.interaction = prev.interaction
		h.start_state = prev.state
		h.end_state = current.state
		h.start_screenshot = prev.screenshot
		h.end_screenshot = current.screenshot
		h.success = prev.success
		h.result = self._analyze_transition(prev, current)
		
		if prev.interaction_timestamp and prev.interaction_timestamp >= prev.timestamp \
			and prev.interaction_timestamp < current.timestamp:
			h.interactiontimestamp = prev.interaction_timestamp
		
		self.__traversal.history.append(h)
		self.__db.insert_history(h)
		
	
	def _analyze_transition(self, prev, current):
		if prev is None or current is None:
			raise ValueError('Diff on states should not have None arguments.')
		
		if prev == current:
			raise ValueError('Diff should not be done on same state variable.')
		
		if current.state is None:
			raise ValueError('Current environment should always have a state.')
		
		ret_val = 0
		
		# always report if we are outside the app
		if not self._activity_in_package(current.state.activity.name):
			ret_val |= const.ACTION_RESULT_OUTSIDE_APP
			
		# if prev state is None and we had a launch and it was successful
		# just report a change in activity
		if prev.state is None and \
			prev.interaction.type == const.ACTION_LAUNCH_APP and prev.success:
			ret_val |= const.ACTION_RESULT_ACTIVITY_CHANGE
			return ret_val
			
		# if states are the same or we are still in the same activity
		#if prev.state == current.state or prev.state.activity == current.state.activity:
		if prev.state.activity == current.state.activity:
			keyboard_state_change = self._keyboard_state_change(prev, current)
			
			if keyboard_state_change is not None:
				ret_val |= keyboard_state_change
			
			dialog_state_change = self._dialog_state_change(prev, current)
			
			if dialog_state_change is not None:
				ret_val |= dialog_state_change
			
			# we are in the same state and same screen resort to screenshot comparison
			if (prev.state is not None and current.state is not None and\
				prev.state.screenshots and current.state.screenshots):
				# image magick has a problem with two extremely dissimilar images
				print 'Performing imagemagick similarity check...'
				similar = imagemagick.similar(
											'%s/%s' % (self.__work_directory, prev.state.screenshots[-1]),
											'%s/%s' % (self.__work_directory, current.state.screenshots[-1]),
											self.__screenshot_threshold )
			
				# the screens are too similar, maybe nothing has changed?
				if not similar:
					print 'Interaction resulted in screen change.'
					ret_val |= const.ACTION_RESULT_SCREEN_CHANGE
			
		# if the the state has changed and we are not in the same activity
		elif prev.state.activity != current.state.activity:
			# check if the current activity is in or outside package
			ret_val |= const.ACTION_RESULT_ACTIVITY_CHANGE
			
		return ret_val
	
	# write now the algorithm is interaction based but should be updated to be sequence based
	def _update_graph_edges(self, prev, current):
		if prev is None or current is None:
			return
		
		if prev.state is None or current.state is None:
			return
		
		# if the last interaction did not succeed, then there are
		# no changes to the edges
		if not prev.success:
			return
		
		# the transition should have a sequence and an interaction
		if not prev.sequence:
			return
		
		if not prev.interaction:
			return
		
		# determine whether this edge is deterministic or not
		# find all the history elements with this interaction id
		# and see if they have all ended in the same state
		if prev.interaction.deterministic:
			interaction_history = self.__db.select_interaction_history(prev.interaction.id)
			for history in interaction_history:
				if history.end_state != current.state:
					print "Non-deterministic Found"
					prev.interaction.deterministic = False
					prev.sequence.determinsitic = False
					
					self.__db.merge_traversal_item(prev.interaction)
					self.__db.merge_traversal_item(prev.sequence)
					
					self._clean_exploration_edges(prev.sequence)
					
					# all other sequences for the interaction should be
					# set to non-determinsitic
					for sequence in prev.interaction.sequences:
						if not sequence:
							continue
						sequence.deterministic = False
						self.__db.merge_traversal_item(sequence)
						self._clean_exploration_edges(sequence)
					break
		
		
		# edges that go outside the app should only be added to the state graph
		current_activity = current.state.activity
		
		outside_app = self._activity_in_package(current_activity.name)
		
		
		# if edge is outside app or non_deterministic only add to state graph
		if outside_app or not prev.interaction.deterministic:
			self._add_state_edge(prev.state, current.state, prev.interaction)
			return
		
		# otherwise the transition was both deterministic and inside the app
		# as such the edge should be added to both graphs
		self._add_edge(prev.state, current.state, prev.interaction)
			
	def _keyboard_state_change(self, prev, current):
		if prev.keyboard_shown is None or current.keyboard_shown is None:
			raise ValueError('Keyboard state must always be recorded')
		
		if prev.keyboard_shown != current.keyboard_shown:
			if prev.keyboard_shown:
				return const.ACTION_RESULT_KEYBOARD_DOWN
			else:
				return const.ACTION_RESULT_KEYBOARD_UP
			
		return None
	
	def _dialog_state_change(self, prev, current):
		if prev.dialog_present is None or current.dialog_present is None:
			raise ValueError('Dialog state must always be recorded')
		
		if prev.dialog_present != current.dialog_present:
			if prev.dialog_present:
				return const.ACTION_RESULT_DIALOG_HIDDEN
			else:
				return const.ACTION_RESULT_DIALOG_SHOWN
			
		return None

	def _launch_app(self):
		print '\nLaunching application...'
		# it appears if the appname is provided incorrectly tema shuts down
		# crucial to provide the correct app name
		launch = self._launch_activity(None, self.__launch_activity)
	
		if not launch:
			print 'Was not able to launch application... quitting'
			return False
		
		# wait a few seconds to stabalize and get the focused activity
		utils.wait(2 * Traverser.SLEEP_DURATION_FIVE_SECONDS)
		
		focused_activity = adbcommands.focusedActivity(self.__target)
		
		print 'Arrived at activity', focused_activity
		
		if not self._activity_in_package(focused_activity):
			print 'Start activity is not in the application package... quitting'
			return False
		
		print 'Application launched successfully'
		return True

	def _launch_activity(self, env, activity):
		# the name should already be in tema safe name, but if it is not
		# then the following line should make it tema safe
		
		# check to see if the activity name starts with the process name
		if not activity:
			return
		
		print self.__package
		print activity.name
		tema_safe_name = activity.tema_safe_name(self.__package)
		print tema_safe_name
		
		if not tema_safe_name:
			return
		else:
			print 'Launching activity', tema_safe_name
		
		k = LaunchApp()
		attributes = "'" + tema_safe_name + "'"
		print tema_safe_name
		k.initialize(attributes, self.__active_target)
		interaction_timestamp = self._get_time()
		success = k.execute()
		
		if env:
			# find the appropriate interaction
			i = self.__db.select_insert_launch_interaction(self.__traversal.id, attributes)
			env.set_action(interaction_timestamp, None, i, success)
		
		return success

	def _perform_interaction(self, env):
		if env is None:
			return False
		
		interaction = env.interaction
		
		if interaction is None:
			return False
		
		if interaction.type == const.ACTION_TAP_OBJECT:
			return self._click_component(env)
		elif interaction.type == const.ACTION_PRESS_KEY:
			return self._press_key(env, interaction.args)

	def _press_key(self, env, attributes):
		k = PressKey()
		k.initialize(attributes, self.__active_target)
		interaction_timestamp = self._get_time()
		success = k.execute()
		
		if env:
			if env.sequence and env.interaction:
				s = env.sequence
				i = env.interaction
			else:
				sequence_id = None
				if env.sequence:
					sequence_id = env.sequence.id
				(s, i) = self.__db.select_state_sequence_interaction(
					env.state.id, sequence_id, const.ACTION_PRESS_KEY, attributes)
				
			# make sure the sequence is first one created for back
			if s:
				s.visited = True
				s = self.__db.merge_traversal_item(s)
			i.visited = True
			i = self.__db.merge_traversal_item(i)
			
			env.set_action(interaction_timestamp, s, i, success)
		
		return success

	def _press_back(self, env):
		return self._press_key(env, 'back')
	
	def _press_menu(self, env):
		return self._press_key(env, 'menu')

	# for this action the environment variable
	# should have the sequence and interaction set
	def _click_component(self, env):
		if not env:
			return False

		# get the interaction from the environment
		interaction = env.interaction
		
		if not interaction:
			return False
		
		component = interaction.component
		
		if not component:
			return False

		references = component.references()

		env.set_interaction_timestamp(None)
		env.sequence.visited = True
		env.interaction.visited = True
		env.set_success(False)

		success = False

		# previously we would go from least descriptive to most because
		# it would find things better. unfortunately for some apps the
		# developer uses the same reference for multiple buttons and
		# the traversal cannot click on the correct component
		# so we have to reverse the references
		for reference in references:
			
			print "Clicking on", component.id, reference + "..."

			# click it
			k = TapObject()
			k.initialize(reference, self.__active_target)
			interaction_timestamp = self._get_time()
			success = k.execute()

			if (success):
				print 'Click was successful.'
				env.set_interaction_timestamp(interaction_timestamp)
				env.interaction.args = reference
				env.set_success(True)
				break

		# was not able to find the component using any of the finding methods
		if not success:
			print "Was not able to click component"
			
		# merge the updates to sequence and interaction
		self.__db.merge_traversal_item(env.sequence)
		self.__db.merge_traversal_item(env.interaction)
		
		return success
	
	# first uses the adbcommand if the adbcommand is not available then falls back on our
	# image based keyboard recognzier
	def _keyboard_shown(self, state=None):
		adb_property = adbcommands.keyboardShown(self.__target)
		
		if adb_property is not None:
			return adb_property
		
		# fall back on keyboard recognizer
		if state is None:
			return None
		
		if state.screenshots is None:
			return None
		
		screenshot = state.screenshots[-1]
		
		if screenshot is None:
			return None
		
		screenshot_path = screenshot.path
		
		if not screenshot_path:
			return None
		
		# second parameter is the filename of the screenshot. Screenshots are immediately under the work directory
		screenshot_full_path = '%s/%s' % (self.__work_directory, screenshot_path[screenshot_path.rfind('/')+1:])
		
		return keyboardrecognizer.keyboard_on_screen(screenshot_full_path)
		
	def _element_present(self, state, element_classes):
		#print '\n Inside elementPresent'
		
		if not state:
			return None
		
		# convert all the classes to be lower case
		element_classes = [x.lower() for x in element_classes]
		
		components = state.components
		
		if not components:
			return
		
		for component in components:
			
			if not component:
				continue
			
			classname = component.classname
			
			if not classname:
				continue
			
			for element_class in element_classes:
				if classname.lstrip().lower().startswith(element_class):
					return True
				
		return False
	
	# these functions do not necessarily always return the correct output
	# if the developer has decided to extend the classes rather than
	# use them directly
	def _dialog_present(self, state):
		#print '\n Inside checkPotentialDialog'
		
		dialog_classes = ['com.android.internal.widget.DialogTitle']
		
		return self._element_present(state, dialog_classes)
	
	def _map_present(self, state):
		map_classes = ['com.google.android.gms.maps.MapView',  
					'com.google.android.maps.MapView', 
					'com.lbsp.android.mapping.MapDisplay']
		
		return self._element_present(state, map_classes)
	
	def _menu_present(self, state):
		# potentially also android.widget.ListPopupWindow$DropDownListView
		menu_classes = ['com.android.internal.view.menu.ListMenuItemView']
		
		return self._element_present(state, menu_classes)
	
	def _process_menu(self, reReadGUI=False):
		print 'Checking for potential menu...'
		menuPresent = self._menu_present(reReadGUI)
		
		# just press back for now. Another option would be to explore the menu as well
		if (menuPresent):
			print 'Discovered menu. Pressing back...'
			self._press_back()
	
	def _reset_process_dialog_attempts(self):
		if self.__process_dialog_attempts:
			self.__process_dialog_attempts.clear()
	
	# look at if we can pull up our last attempt from the history
	# and try something different than the same button
	def _process_dialog(self, env):
		if not env:
			print 'env is None.'
			return
		
		state = env.state
		
		if not state:
			print 'state is None.'
			return
		
		button_text_options = ['cancel', 'no', 'not', 'not now', 'later', 'don\'t ask',
					'do not ask', 'remind', 'okay', 'ok', 'agree', 'continue', 'accept']
		
		components = state.components
		
		if not components:
			print 'components is None.'
			return
		
		buttons = list()
		
		button_class = 'android.widget.Button'.lower()
		
		for component in components:
			classname = component.classname
			
			if not classname:
				continue
			
			if classname.lower().startswith(button_class):
				print 'Found button:', component.text
				buttons.append(component)
				
		if len(buttons) <= 0:
			return
		
		# get the last components that were tried by process dialog
		
		for button in buttons:
			if not button:
				continue
			
			# if the button has been attempted already try another one
			if button in self.__process_dialog_attempts:
				continue
			
			if not button.text:
				continue
			
			for button_text_option in button_text_options:
				if (button.text.lower().find(button_text_option) >= 0):
					interaction = self.__db.select_state_component_interaction(state.id, button.id, const.ACTION_TAP_OBJECT)
					
					if interaction:
						self.__process_dialog_attempts.add(button)
						env.interaction = interaction
						if interaction.sequences and len(interaction.sequences) > 0:
							env.sequence = interaction.sequences[0]
						success = self._click_component(env)
						env.success = success;
						return
			
	def _read_screen_text(self, reReadGUI=False):
		print '\nInside readScreenText'
		
		if (reReadGUI):
			self.__guireader.readGUI()
			
		guiRoot = self.__guireader.getRoot()
		children = self.__guireader.findAllItems(guiRoot)
		
		screenText = list()
		
		for child in children:
			#print child
			if child is None:
				continue
			
			text = child.getText()
			
			if text is None:
				continue
			
			screenText.append(text)
				
		if len(screenText) > 0:
			print screenText
			return screenText
		
		return None
	
	def _activity_in_package(self, name):
		if name is None:
			return False
		elif name.startswith(self.__package):
			return True
		
		activity = self.__db.select_app_activity(self.__app.id, name)
		
		if activity:
			return True
		else:
			return False
	
	def _focused_activity(self):
		return adbcommands.focusedActivity(self.__target)
	
	# reads the GUI and returns the root element
	def _read_gui(self):
		ret = self.__guireader.readGUI()
		#SLEEP_DURATION_ONE_SECOND
		#_read_gui fails quite often. Add a while loop here. 
		#This has potential side effect: adding loops inside main traverser loop. may be infinite and make time out not accurate
		while ret == False:
			print >> sys.stderr, 'READGUI fails will retry, root', self.__guireader.getRoot() 
			ret = self.__guireader.readGUI()

		return self.__guireader.getRoot()
	
	# gets the current state based on whether it is already in
	# the states dict or in the database
	def _get_current_state(self):
		print '\nGetting current state...'
		
		# always wait 3 seconds before getting the state
		utils.wait(3 * Traverser.SLEEP_DURATION_ONE_SECOND)
		
		# read the gui
		hash = self._get_current_hash()
		
		print "_get_current_hash", hash
		
		if not hash:
			return None
		
		ret_val = None
		
		# get the current activity
		if hash in self.__states:
			print 'Found state in state dictionary.'
			ret_val = self.__states[hash]
		else:
			potential_state = self.__db.select_state(self.__traversal.id, hash)
			if potential_state:
				print 'Found state in the database'
				self.__states[hash] = potential_state
				ret_val = potential_state
		
		# take a new screenshot for the state
		if ret_val is not None:
			screenshot_path = self.take_screenshot()
			screenshot = Screenshot()
			screenshot.path = screenshot_path.replace(\
				self.__traversal_directory[:self.__traversal_directory.rfind('/')+1], '', 1)
			ret_val.screenshots.append(screenshot)
			self.__db.merge_traversal_item(ret_val)
			
			# return the value
			return ret_val
		
		print 'Could not finding an existing state. Creating a new state...'
		
		# since the state does not exist sleep for a bit longer and then recompute the
		# hash to make sure the screen has stabilized
		# another 7 seconds
		utils.wait(Traverser.SLEEP_DURATION_FIVE_SECONDS + 2 * Traverser.SLEEP_DURATION_ONE_SECOND)
		
		return self._create_new_state()
	
	def _create_new_state(self):
		focused_activity = self._focused_activity()
		
		# reads the gui already
		hash = self._get_current_hash(focused_activity)
		
		state = State()
		state.traversal = self.__traversal
		state.hash = hash
		state.activity = self.__db.select_app_activity(self.__app.id, focused_activity)
		
		# if state.activity is still None, the activity is not part of
		# applications activities
		if not state.activity:
			state.activity = self.__db.select_insert_activity(focused_activity)
		
		# take a screenshot and place it in
		screenshot_path = self.take_screenshot()
		
		screenshot = Screenshot()
		# the screenshot paths should be relative to the traversal work directory
		screenshot.path = screenshot_path.replace(\
				self.__traversal_directory[:self.__traversal_directory.rfind('/')+1], '', 1)
		
		state.screenshots.append(screenshot)
		
		# get the root node
		root = self.__guireader.getRoot()
		
		# copy its components into the table
		# and associate interactions
		children = self.__guireader.findAllItems(root)
		
		for child in children:
			if not child:
				pass
			
			childX, childY = self.__guireader.getViewCoordinates(child)
			
			c = Component()
			c.uuid = child.getId()
			c.classname = child.getClassName()
			#components may have non utf-8 characters
			#convert to a utf-8 string to work with database
			#c.text = child.getText()
			#ignore errors
			if child.getText():
				#even if ignore is set, the unicode() result still contain utf-8 characters,
				#when get from database, utf-8 character is not encoding correctly, so when trying to print,
				#it will raise an exception 
				#using encoding = 'ascii' which is default, and errors='ignore' will solve this
				c.text = unicode(child.getText(), errors = 'ignore')
			c.x = childX
			c.y = childY
			c.width = child.getWidth()
			c.height = child.getHeight()
			clickable = child.isClickable()
			if clickable is not None:
				c.clickable = clickable
			# could add other properties here...
			
			state.components.append(c)
			
			# only add the component to sequences if it is traversal clickable
			if child.isTraversalClickable():
				# for each child add a click interaction
				# all interactions are deterministic until proven otherwise
				i = Interaction()
				i.type = const.ACTION_TAP_OBJECT
				i.visited = False
				i.deterministic = True
				i.component = c
				
				# embed the interaction into a sequence
				s = Sequence()
				s.visited = False
				s.deterministic = True
				s.interactions.append(i)
				
				state.interactions.append(i)
				state.sequences.append(s)
				
				self.__traversal.interactions.append(i)
				self.__traversal.sequences.append(s)
			
		# add two new interactions for menu and back press
		menu_interaction = Interaction()
		menu_interaction.type = const.ACTION_PRESS_KEY
		menu_interaction.args = 'menu'
		menu_interaction.visited = False
		menu_interaction.deterministic = True
		
		state.interactions.append(menu_interaction)
		self.__traversal.interactions.append(menu_interaction)
		
		menu_sequence = Sequence()
		menu_sequence.visited = False
		menu_sequence.determinstic = True
		menu_sequence.interactions.append(menu_interaction)
		
		state.sequences.append(menu_sequence)
		self.__traversal.sequences.append(menu_sequence)
		
		back_interaction = Interaction()
		back_interaction.type = const.ACTION_PRESS_KEY
		back_interaction.args = 'back'
		back_interaction.visited = False
		back_interaction.deterministic = True
		
		state.interactions.append(back_interaction)
		self.__traversal.interactions.append(back_interaction)
		
		back_sequence = Sequence()
		back_sequence.visited = False
		back_sequence.deterministic = True
		back_sequence.interactions.append(back_interaction)
		
		state.sequences.append(back_sequence)
		self.__traversal.sequences.append(back_sequence)
		
		self.__traversal.states.append(state)
		
		self.__db.insert_state(state)
		
		return state
	
	def _get_current_hash(self, focused_activity = None):
		# read the gui
		root = self._read_gui()
		
		if not focused_activity:
			focused_activity = self._focused_activity()
		
		if not focused_activity:
			return None
		
		# record the first state activity that we ever see
		if not self.__first_state_activity:
			self.__first_state_activity = focused_activity
		
		hash = root.hash(header=focused_activity)
		
		return hash
	
	def _add_state_node(self, state):
		if not state:
			return 
		
		if not self.__state_graph.has_node(state):
			self.__state_graph.add_node(state)
			
	def _add_exploration_node(self, state):
		if not state:
			return
		
		if not self.__exploration_graph.has_node(state):
			self.__exploration_graph.add_node(state)

	# adds the node to both the state and the exploration graphs
	def _add_node(self, state):
		self._add_state_node(state)
		self._add_exploration_node(state)
	
	def __add_graph_transition_edge(self, graph, start_state, end_state, transition_sequence):
		if not graph:
			return
		
		existing_edge = None
		
		try:
			existing_edge = graph.edge[start_state][end_state]
		except KeyError:
			existing_edge = None
		
		if existing_edge is not None:
			graph.edge[start_state][end_state]['sequence_ids'].add(transition_sequence.id)
		else:
			attribute_set = set([transition_sequence.id])
			graph.add_edge(start_state, end_state, sequence_ids=attribute_set)
	
	def _add_state_edge(self, start_state, end_state, transition_sequence):
		self.__add_graph_transition_edge(self.__state_graph, start_state, end_state, transition_sequence)
	
	def _add_exploration_edge(self, start_state, end_state, transition_sequence):
		self.__add_graph_transition_edge(self.__exploration_graph, start_state, end_state, transition_sequence)
	
	def _clean_exploration_edges(self, non_deterministic_sequence):
		if not non_deterministic_sequence:
			return
		
		_id = non_deterministic_sequence.id
		
		for edge in self.__exploration_graph.edges_iter():
			#self.__exploration_graph[edge[0]][edge[1]]['sequence_ids']
			if _id in self.__exploration_graph[edge[0]][edge[1]]['sequence_ids']:
				self.__exploration_graph[edge[0]][edge[1]]['sequence_ids'].remove(_id)
			
			# if there are no other sequences that lead to this state
			# remove the edge in the graph
			if len(self.__exploration_graph[edge[0]][edge[1]]['sequence_ids']) <= 0:
				print "Remove edge:", edge
				self.__exploration_graph.remove_edge(*edge[:2]) # selects first part of the tuple
	
	def _add_edge(self, start_state, end_state, transition_sequence):
		self._add_state_edge(start_state, end_state, transition_sequence)
		self._add_exploration_edge(start_state, end_state, transition_sequence)
		
	def _get_time(self):
		return datetime.datetime.now() + self.__timedelta
	
	def _get_progress_filename(self):
		if not self.__apk:
			return None
		
		# remove the extension. Note there must be an extension
		name = self.__apk.rstrip('apkAPK')
		
		# append gtp
		return '%sgtp' % name
	
	def _get_progress_directory(self):
		if self.__progress_directory:
			return self.__progress_directory
		
		if not self.__project_directory:
			return None
		
		directory = os.path.join(self.__project_directory, 'progress')
		
		if not directory:
			print 'Progress folder is invalid.'
			return None
		if not os.path.exists(directory):
			print 'Progress folder does not exist.'
			return None
		elif not os.path.isdir(directory):
			print 'Progress folder is not a directory.'
			return None
		
		self.__progress_directory = directory
		
		return self.__progress_directory
	
	def _get_gtp(self):
		if self.__gtp:
			return self.__gtp
		
		filename = os.path.join(self._get_progress_directory(), self._get_progress_filename())
		
		if not filename:
			print 'Gtp is invalid.'
			return None
		if not os.path.exists(filename):
			print 'Gtp does not exist.'
			return None
		
		self.__gtp = filename
		
		return self.__gtp
	
	def _get_gtp_content(self):
		if not self._get_gtp():
			print 'Invalid gtp file. Cannot read gtp.'
			return
		
		progress = None
		
		with open(self._get_gtp(), 'r') as infile:
			progress = json.load(infile)
			
		return progress
	
	# update whether the dynamic analysis has been performed or not
	def _set_gtp(self, performed=True):
		progress = None
		
		with open(self._get_gtp(), 'r') as infile:
			progress = json.load(infile)
			
		if not progress:
			return False
		
		if (performed):
			progress['dynamicAnalysis']=True
		else:
			progress['dynamicAnalysis']=False
		
		with open(self._get_gtp(), 'w') as outfile:
			progress = json.dump(progress, outfile)
			
		return True

# comprehensive state objec that holds the state and whether there was a keyboard or dialog up
class Environment(object):
	def __init__(self, timestamp=None, state=None, screenshot=None, keyboard_shown=None, 
				dialog_present=None, sequence=None, interaction=None, interaction_timestamp=None, success=None):
		self.timestamp = timestamp
		self.state = state
		self.screenshot = screenshot
		self.keyboard_shown = keyboard_shown
		self.dialog_present = dialog_present
		self.sequence = sequence
		self.interaction = interaction
		self.interaction_timestamp = interaction_timestamp
		self.success = success
	
	def set_sequence(self, sequence):
		self.sequence = sequence
	
	def set_interaction(self, interaction):
		self.interaction = interaction
		
	def set_success(self, success):
		self.success = success
		
	def set_interaction_timestamp(self, interaction_timestamp):
		self.interaction_timestamp = interaction_timestamp
		
	def set_action(self, interaction_timestamp=None, sequence=None, interaction=None, success=None):
		if interaction_timestamp:
			self.set_interaction_timestamp(interaction_timestamp)
		
		if sequence:
			self.set_sequence(sequence)
			
		if interaction:
			self.set_interaction(interaction)
			
		self.set_success(success)

