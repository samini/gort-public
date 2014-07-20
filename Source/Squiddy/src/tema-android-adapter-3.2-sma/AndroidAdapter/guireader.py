# -*- coding: utf-8 -*-
# Copyright (c) 2006-2010 Tampere University of Technology
# 
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
# 
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

import socket
import re
import subprocess
import time
import datetime

from AndroidAdapter.monkeydriver import *
from AndroidAdapter import screencapture

"""
ViewItem holds the information of single GUI component
"""
class ViewItem:

	def __init__(self, className, code, indent, properties, parent):
		self.__className = className
		self.__code = code
		self.__indent = indent
		self.__properties = properties
		self.__parent = parent
		self.__children = []
		self.__visited = False #SMA
		self.__dbid = None #SMA the primary key id for the component in the Component table
	
	def getClassName(self):
	
		return self.__className
	
	def getCode(self):
	
		return self.__code
	
	def getId(self):
	
		return self.getProperty("mID")
		
	def getText(self):
	
		output = None
	
		if (self.getProperties().has_key("text:mText")):
			output = self.getProperty("text:mText")
		elif (self.getProperties().has_key("mText")):
			output = self.getProperty("mText")
			
		#print "getText called. Returning", output
			
		return output

	# SMA edited	
	def getProperty(self, propertyName):
	
		if self.getProperties().has_key(propertyName):
			return self.getProperties()[propertyName]

		keys = ["%s:%s" % ('layout', propertyName),
			 "%s:%s" % ('scrolling', propertyName)]

		for key in keys:
			#print key
			if self.getProperties().has_key(key):
				return self.getProperties()[key]		

		return None
	
	def getIndent(self):
		return self.__indent
	
	def getProperties(self):
		return self.__properties
		
	def getParent(self):
		return self.__parent
		
	def addChild(self,child):
		self.__children.append(child)
	
	def getChildren(self):
		return self.__children

	#SMA
	def getDBId(self):
		return self.__dbid

	def setDBId(self, dbid):
		self.__dbid = dbid

	def getVisited(self):
		return self.__visited

	def setVisited(self, visited):
		self.__visited = visited

	def getHeight(self):
		return self.getProperty("getHeight()")


	def getWidth(self):
		return self.getProperty("getWidth()")

	def getArea(self):
		height = self.getHeight()
		width = self.getWidth()

		if (height and width):
			return (int(height) * int(width))
	
	# this function returns True for any item the traversal
	# algorithm should click on
	def isTraversalClickable(self):
		return self.isClickable() or self.getOnClickListener() or\
			self.isListsChildOrGrandchild()
	
	# changed isListsChild to only look up to parent and grandparent
	# if any of the item's parents has an OnItemClickListener
	# then this is a child of a list
	"""
	def isListsChild(self):
		parent = self.getParent()
		
		if parent is None:
			return False
		
		onItemClickListener = parent.getOnItemClickListener()
		
		if onItemClickListener:
			return True
		else:
			return parent.isListsChild()
	"""
	
	# only checks parent and grandparent
	def isListsChildOrGrandchild(self):
		parent = self.getParent()
		
		if parent is None:
			return False
		
		if parent.getOnItemClickListener():
			return True
		
		grand_parent = parent.getParent()

		if grand_parent and grand_parent.getOnItemClickListener():
			return True
		
		return False
		
	def isClickable(self):
		key = 'isClickable()'
		
		tmp = self.getProperty(key)
		
		if not tmp:
			return None
		elif tmp.strip().lower() == "true":
			return True
		else:
			return False
		
	#SMA: functions to get callback handlers
	def getOnClickListener(self):
		keys = ['callback:myOnClickListener', 'myOnClickListener']
		
		for key in keys:
			if self.getProperties().has_key(key):
				return self.getProperties()[key]
			
		return None	
	
	# if the UI component extends the AdapterView class in Android
	# then it will inherent an onItemClickListener. This may not be
	# set to a listener however.
	def getOnItemClickListener(self):
		keys = ['callback:myOnItemClickListener', 'myItemOnClickListener']
		
		for key in keys:
			if self.getProperties().has_key(key):
				return self.getProperties()[key]
			
		return None

	def getReference(self):
		reference = None
		if (self.getId() != None):
			reference = self.getId()
		if (reference == None):
			if (self.getText() != None):
				reference = "'%s'" % self.getText()
		if (reference == None):
			return None
		if (self.getClassName() != None):
			reference = reference + ';' + self.getClassName()

		return reference

	# provides a list of references for the element
	# click-ability changes based on the reference provided
	# most specific reference methods first
	def getReferences(self):
		
		cId = self.getId()
		text = self.getText()
		className = self.getClassName()

		print "getRefernces called.", cId, text, className

		if (cId == None and text == None):
			return None

		references = list()
	
		if (className != None):
			
			if (cId != None):
				references.append("%s;%s" % (cId, className))

			if (text != None):
				references.append("'%s';%s" % (text, className))

		if (cId != None):
			references.append(cId)

		if (text != None):
			references.append("'%s'" % text)

		return references
	
	#SMA: Provide a hash for this view
	def hash(self, header=None):
		# create a DFS string of the UI node and use the python hash function
		hashGUIList = self._hashGUIList(0, 0)
		
		if header is not None:
			hashGUIList.insert(0, header)
		
		retVal = str(hash(''.join(hashGUIList)))
		
		#print hashGUIList
		#print retVal
		
		return retVal
	
	# DFS returning classname, level and order
	def _hashGUIList(self, level, order):
		retVal = list()
	
		# first add the current node	
		retVal.append(self.getClassName())
		retVal.append(str(level))
		retVal.append(str(order))
		
		# check to see if we have a list type. list types generally
		# have a onItemClickListener. if there is a list type
		# and it is set only consider first two items in the list
		onItemClickListener = self.getOnItemClickListener()
		extendsAdapterView = False
		
		if (onItemClickListener is not None and onItemClickListener.strip()):
			extendsAdapterView = True
		
		order = 0
		
		childrenLevel = level + 1
		
		children = self.getChildren()
		
		for child in children:
			# only consider first two items for lists with handlers
			if extendsAdapterView and order == 2:
				break
			
			if child is None:
				continue
			
			childGUIList = child._hashGUIList(childrenLevel, order)
			
			if childGUIList is None:
				continue
			
			retVal.extend(childGUIList)
			order += 1
		
		return retVal

	def __repr__(self):
		s = "%s %s %s %s\nWidth: %s Height: %s" % (self.getClassName(), self.getCode(), self.getId(), self.getText(),
								self.getWidth(), self.getHeight() )	
		return s

class GuiReaderError(Exception):
	def __init__(self, value):
		self.value = value
	def __str__(self):
		return repr(self.value)


"""
GuiReader reads the contents of a GUI using the window service in the device.
"""
class GuiReader:

	PORT = 4939
	HOST = "localhost"
	DUMP_RETRIES = 5

	def __init__(self, target, monkey, host = HOST, port = PORT):
		self.__host = host
		self.__port = port
		self.__root = None
		self.__items = []
		self.__target = target
		self.__monkey = monkey

	def readGUI(self):
	
		#SMA
		# figure out how long it takes to read the gui, this depends on the 
		# content of the screen
		
		before = time.time()
		
		self.__items = []
		tried = 0
		while len(self.__items) == 0:
			try:
				sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
				sock.connect( (self.__host, self.__port) )
			except socket.error,msg:
				print msg
				return False
			
			data = ''
			
			#DUMP -1 dumps the foreground window information
			try:
				sent = sock.send("DUMP -1\n")
				if sent == 0:
					sock.close()
					return False
				

				data = ""
				#Read whle the last line is "DONE"
				while True:
					
					try:
						newData = sock.recv(4096)
					except socket.timeout:
						continue

					data += newData
					if len(newData) < 4096 or newData.splitlines()[-1] == "DONE":
						break

				lines = data.splitlines()
				#If prosessing the screen dump fails, retry
				if not self.__processScreenDump__(lines):
										
					self.__items = []
					tried += 1
					time.sleep(0.2)
					if tried >= GuiReader.DUMP_RETRIES:
						sock.close()
						return False
					
				sock.close()
			except socket.error,msg:
				print msg
				return False
				
		after = time.time()
		
		duration = after - before
		
		print 'Reading the screen took', duration, 'seconds.'
		
		# produce a hash for testing
		self.getRoot().hash()
		
		return True

	
	#Processes the raw dump data and creates a tree of ViewItems
	def __processScreenDump__(self, dump):
		
		self.__items = []
		
		parent = None
		currentIndent = 0
		
		
		for line in dump:
			if (line == "DONE."):
				break
			
			#separate indent, class and properties for each GUI object
			matcher = re.compile("(?P<indent>\s*)(?P<class>[\w.$]+)@(?P<id>\w+) (?P<properties>.*)").match(line)	
			
			if not matcher:
				print "Invalid screen dump data while processing line: " + line
				return None
			
			#Indent specifies the hierarchy level of the object
			indent = len(matcher.group("indent")) 
			
			#If the indent is bigger that previous, this object is a child for the previous object
			if indent > currentIndent:
				parent = self.__items[-1]
				
			elif indent < currentIndent:
				for tmp in range(0,currentIndent - indent):
					parent = parent.getParent()
			
			currentIndent = indent
			
			propertiesData = matcher.group("properties")
			properties = {}
			index = 0
			
			#Process the properties of each GUI object
			while index < len(propertiesData):
				
				#separate name and value for each property
				propMatch = re.compile("(?P<prop>(?P<name>[^=]+)=(?P<len>\d+),)(?P<data>.*)").match(propertiesData[index:-1])
				
				if not propMatch or len(propMatch.group("data")) < int(propMatch.group("len")):
					print "Invalid screen dump data while processing line: " + line
					self.__items = None
					return None
				
				length = int(propMatch.group("len"))
					
				properties[propMatch.group("name")] = propMatch.group("data")[0:length]
				index += len(propMatch.group("prop")) + length + 1
				
			self.__items.append(ViewItem(matcher.group("class"),matcher.group("id"),indent,properties,parent))
			
			if parent:
				parent.addChild(self.__items[-1])
			
			if indent == 0:
				self.__root = self.__items[-1]
			
			
		return self.__items
	"""
	def printScreenDump(self):
		
		for i in self.__items:
			for tmp in range(0,i.getIndent()):
				print " ",

			print i.getClassName() + ":" + i.getId() + " id: "  + i.getProperties()["mID"],
			if i.getProperties().has_key("mText"):
				print " text: \"" + i.getProperties()["mText"] + "\"",
			print ""
			print i.getProperties()
	"""
	
	def printDump(self):

		self.__printChildren__(self.__root)
		
	
	#Takes a screenshot. TODO: EXPERIMENTAL! does not work well
	def takeScreenshot(self, out_path = None):
		
		if out_path == None:
		
			time = datetime.datetime.now()		
			month = "%.2d" % time.month
			day = "%.2d" % time.day
			hour = "%.2d" % time.hour
			min = "%.2d" % time.minute
			sec = "%.2d" % time.second
			
			out_path =  "screen_" + str(time.year) +  str(month) + str(day) + "-" + str(hour) + str(min) + str(sec) + ".png"

			
		screencapture.captureScreen(out_path, self.__target)
	
	
	#prints out the screen dump
	def __printChildren__(self, node):
		for tmp in range(0,node.getIndent()):
			print " ",
		print node.getClassName() + ":" + node.getId() + " id: "  + node.getProperties()["mID"],
		if node.getProperties().has_key("mText"):
			print " text: '" + node.getProperties()["mText"] + "'",
		if node.getProperties().has_key("text:mText"):
			print " text: '" + node.getProperties()["text:mText"] + "'",
		if node.getProperties().has_key("mLeft"):
			print "coordinates:", node.getProperties()["mLeft"], node.getProperties()["mTop"], node.getProperties()["mRight"],node.getProperties()["mBottom"]
		elif node.getProperties().has_key("layout:mLeft"):
			print "coordinates:", node.getProperties()["layout:mLeft"], node.getProperties()["layout:mTop"], node.getProperties()["layout:mRight"],node.getProperties()["layout:mBottom"]
		#SMA: print out all of the properties
		#print node.getProperties()
		for c in node.getChildren():
			self.__printChildren__(c)
	
	
	#Helper function for finding a listview from the GUI
	def getListView(self,rootItem = None):
		
		root = rootItem
		if not rootItem:
			root = self.__items[0]
			
		comp = lambda x: x.getClassName().find("ListView") != -1	
		
		return self.__findFirstItem__(root,comp)
		
	#searches a component that satisfies the given lamda comparator
	def findComponent(self, comparator, rootItem = None, searchAll = False):
	
		root = rootItem
		
		if self.__items == None or len(self.__items) == 0:
			return None
		
		if not rootItem:
			root = self.__items[0]

		if searchAll:
			return self.__findAllItems__(root, comparator)
		
		return self.__findFirstItem__(root, comparator)
	
	
	#Searches the GUI hiearhy for a object with a given text
	def findComponentWithText(self, text, roleName = "", rootItem = None, searchAll = False, partial = False):
		"""
		for i in self.__items:
			if i.getProperties().has_key("mText") and i.getProperties()["mText"].find(text) != -1:
				return i
				
		return None
		"""
		
		# in version 4.1.1 of android mText is refered to as text:mText
		
		if roleName == None:
			roleName = ""
		if not partial:
			#comp = lambda x: x.getClassName().find(roleName) != -1 and x.getProperties().has_key("mText") and x.getProperties()["mText"] == text
			comp = lambda x: x.getClassName().find(roleName) != -1 and \
				((x.getProperties().has_key("mText") and x.getProperties()["mText"] == text) or \
				(x.getProperties().has_key("text:mText") and x.getProperties()["text:mText"] == text))
		else:
			#comp = lambda x: x.getClassName().find(roleName) != -1 and x.getProperties().has_key("mText") and x.getProperties()["mText"].find(text) != -1
			comp = lambda x: x.getClassName().find(roleName) != -1 and \
				((x.getProperties().has_key("mText") and x.getProperties()["mText"].find(text) != -1) or \
				(x.getProperties().has_key("text:mText") and x.getProperties()["text:mText"].find(text) != -1))

		return self.findComponent(comp,rootItem,searchAll)
		
	#Searches the GUI hiearhy for a object with the given id
	def findComponentWithId(self, id, roleName = "", rootItem = None, searchAll = False):
	
		"""
		items = []
		for i in self.__items:
			if i.getProperties().has_key("mId") and i.getProperties()["mId"] == id:
			
				if searchAll:
					items.append(i)
				else:
					return i	
		
		if len(items) == 0:
			return None
			
		return items
		"""
		if roleName == None:
			roleName = ""
			
		#If id is empty, all components are accepted
		if id == "":
			comp = lambda x: x.getClassName().find(roleName) != -1 
		else:			
			comp = lambda x: x.getClassName().find(roleName) != -1 and x.getProperties().has_key("mID") and x.getProperties()["mID"] == id
	
		return self.findComponent(comp,rootItem,searchAll)

		
	#This method finds the coordinates of the foreground window.
	#TODO: Warning, very dependent on the dumsys command. If output is changed (likely between platform versions), the method will break!
	
	def __getForegroundWindowCoordinates__(self):
		
		retcode = subprocess.call("adb -s " + self.__target + " shell dumpsys window > windowinfo.txt",shell=True)
		
		if retcode != 0:
			print "Error when calling adb, make android sdk tools are in the path"
			exit()
		
		file = open("windowinfo.txt","r")
		windowInfo = file.read()
		
		focusId = "mCurrentFocus="
		focusIndex = windowInfo.find(focusId)
		
		if focusIndex != -1:
			currentWindowName = windowInfo[focusIndex + len(focusId) : windowInfo.find("\n",focusIndex)].strip()
			
			# SMA dumpsys has changed based from whatever version of Android TEMA was developed for
			# the following helps make this code work for version 2.3.3 and 4.1.1. The versions of Android
			# we currently have access to
			windowManagerTitles = ["Current Window Manager state", "WINDOW MANAGER WINDOWS"]
			windowManagerTitleIndex = -1

			for windowManagerTitle in windowManagerTitles:
				windowManagerTitleIndex = windowInfo.find(windowManagerTitle)
				if (windowManagerTitleIndex != -1):
					break

			if (windowManagerTitleIndex == -1):
				windowManagerTitleIndex = 0
			# End of code addition
			
			shownFrameText = "mShownFrame="
			
			# added the windowManagerTitleIndex to the find arguments
			#windowListingIndex = windowInfo.find(currentWindowName)
			windowListingIndex = windowInfo.find(currentWindowName, windowManagerTitleIndex)
			if windowListingIndex != -1:
				coordIndex = windowInfo.find(shownFrameText, windowListingIndex) + len(shownFrameText)
				try:
					return  eval(windowInfo[windowInfo.find("[",coordIndex) + 1 : windowInfo.find(",",coordIndex) ]) , eval(windowInfo[windowInfo.find(",",coordIndex) + 1 : windowInfo.find("]",coordIndex) ]) 
				except:
					errormessage = "error evaluating coordinates of focused window"
			
			else:
				errormessage = "'mShownFrame=' attribute not found"
			
		else:
			errormessage = "'mCurrentFocus=' attribute not found"
				
		print "Fatal error: reading foreground window coordinates failed!"	
		
		raise GuiReaderError("Error when reading 'shell dumpsys window' output: " + errormessage)
		

	
	def __findAllItems__(self, node, comparator):

		items = []
		if comparator(node) and node.getProperties()["getVisibility()"] == "VISIBLE":
			items.append(node)
		for c in node.getChildren():
			if c.getProperties().has_key("getVisibility()") and c.getProperties()["getVisibility()"] == "VISIBLE":
				items.extend(self.__findAllItems__(c,comparator))
		return items


	# SMA: function to return all visible items from current node
	def findAllItems(self, node):	
		return self.__findAllItems__(node, lambda x: True)
	
	def __findFirstItem__(self,node,comparator):

		if comparator(node):
			return node
		for c in node.getChildren():
			if c.getProperties().has_key("getVisibility()") and c.getProperties()["getVisibility()"] == "VISIBLE":
				item = self.__findFirstItem__(c,comparator)
				if item: return item
			
		return None
	
	#Calculates absolute GUI object coordinates (middle) from parent and window coordinates
	def getViewCoordinates(self, view, device=None):

		if (view.getProperties().has_key("mLeft")):
			left = int(view.getProperties()["mLeft"])
			top = int(view.getProperties()["mTop"])
		elif (view.getProperties().has_key("layout:mLeft")):
			left = int(view.getProperties()["layout:mLeft"])
			top = int(view.getProperties()["layout:mTop"])
		
		parent = view.getParent()
		while parent:

			if (view.getProperties().has_key("mLeft")):
				left += int(parent.getProperties()["mLeft"]) - int(parent.getProperties()["mScrollX"])
				top += int(parent.getProperties()["mTop"]) - int(parent.getProperties()["mScrollY"])
			elif (view.getProperties().has_key("layout:mLeft")):
				left += int(parent.getProperties()["layout:mLeft"]) - int(parent.getProperties()["scrolling:mScrollX"])
				top += int(parent.getProperties()["layout:mTop"]) - int(parent.getProperties()["scrolling:mScrollY"])
			parent = parent.getParent()
		
		if (view.getProperties().has_key("getHeight()")):
			height = int(view.getProperties()["getHeight()"])
			width = int(view.getProperties()["getWidth()"])
		elif (view.getProperties().has_key("layout:getHeight()")):
			height = int(view.getProperties()["layout:getHeight()"])
			width = int(view.getProperties()["layout:getWidth()"])
		
		#Get absolute window coordinates. If the window is not full screen, they can be different that 0,0
		window_top_x, window_top_y = self.__getForegroundWindowCoordinates__()
		
		#print 'The parent window coordinates are:', window_top_x, window_top_y
		
		if (view.getProperties().has_key("getHeight()")):
			windowheight = int(self.__items[0].getProperties()["getHeight()"])
		elif (view.getProperties().has_key("layout:getHeight()")):
			windowheight = int(self.__items[0].getProperties()["layout:getHeight()"])
		screenheight = int(self.__monkey.getScreenSize()[1])

		if window_top_y + top + height > windowheight:
			y = window_top_y + top + 1
			if y > screenheight:
				y = screenheight - 1

		elif top <= 0:
			y =  window_top_y + height - 1
		
		else: 
			y = window_top_y + top + (height) / 2
		
		x = window_top_x + left + (width) / 2

		#SMA: the y value calculated almost always is at the bottom of the coordinate
		#subtract 50% of the height from y so that the coordinate finds its way in the middle of the object

		#TODO: double check this. It seems to create a problem for Dialog Boxes. If the window_top_y
		#is non-negative avoid subtracting the height from the coordinate
		#y -= (height / 2)
		if (window_top_y == 0):
			y -= (height / 2)

		#print "Component found from coordinates: ",x,y
		return  x,y
		
	
	def getRoot(self):
		return self.__root
			
"""
if __name__ == "__main__":

	g = GuiReader()
	m = MonkeyDriver()
	m.connectMonkey()
	
	
	while True:
	
		kw = raw_input(">").strip()
		
		if (kw == "dump"):
			g.readGUI()
			g.printDump()
			continue

		name = kw.split(" ",1)[0]
		attr = kw.split(" ",1)[1]
		
		if name == "tap":
			g.readGUI()
			view = g.findComponentWithText(attr)
			if view:
				x , y = g.getViewCoordinates(view)
				print x , y
				m.sendTap(x,y)
			else:
				print "not found"
		if name == "press":
			m.sendPress(attr)
		
"""
