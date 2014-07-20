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

"""
MonkeyDriver communicates with the Monkey tool network interface. Requires 
platfrom version 1.6 or later
"""
class MonkeyDriver:

	PORT = 1080
	HOST = "localhost"
	
	#SMA: 4.1.1 has a problem returning screen size
	RECOVERY_SCREEN_DIMENSIONS = True
	DEBUG_DEFAULT_DIMENSIONS = False
	NEXUS_S_SCREEN_SIZE = 480, 800
	GALAXY_NEXUS_SCREEN_SIZE = 720, 1280
	
	#SMA: introduce a timeout for the socket in seconds
	SOCKET_TIMEOUT = 10
	
	#SMA: problems with socket retires
	SOCKET_TRIES = 5
	
	def __init__(self, port = PORT, host = HOST):
		self.host = host
		self.port = port
		self.__isConnected = False
		
		#SMA: save the screen height and width as sometimes
		#the driver is not able to get them and traversal stops
		#when this is not possible just return the last screen height
		self.__screen_height = None
		self.__screen_width = None

	def connectMonkey(self):
		#SMA: inform of connection attempt
		print 'Connecting Monkey...'
		try:
			self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			#SMA: set a timeout
			self.sock.settimeout(MonkeyDriver.SOCKET_TIMEOUT)
			self.sock.connect( (self.host, self.port) )
			self.__isConnected = True
			return True
		except socket.error,msg:
			print msg
			return False
	
	#SMA: have a function to disconnect monkey
	def disconnectMonkey(self):
		print 'Disconnecting Monkey'
		if self.sock is not None:
			self.sock.close()
		self.__isConnected = False
		
	#SMA: have a function to reconnect
	def reConnectMoney(self):
		print 'Reconnecting Monkey'
		self.disconnectMonkey()
		return self.connectMonkey()
	
	"""
	#Gets a system variable.
	#If variable_name is not defined, returns None
	def getVariable(self, variable_name):
		if not self.__isConnected:
			return None
	
		try:
			self.sock.send("getvar " + variable_name + "\n")	
			data = self.sock.recv(4096).strip()
			if not data.startswith("OK:"):
				return None
			return data.split("OK:")[1]
			
		except socket.error:
			self.sock.close()
			self.__isConnected = False
			return None
	"""
	
	#Gets a system variable.
	#If variable_name is not defined, returns None
	#SMA: created a new version to use the commend __socketSend__ function
	def getVariable(self, variable_name):
		# __socketSend__ automatically appends a '\n' to the end
		command = "getvar %s" % variable_name
		
		data = self.__socketSend__(command)
		
		if data is None:
			return None
		elif not data.startswith("OK:"):
			return None
		return data.split("OK:")[1]

	
	def getScreenSize(self, device=None):
		try:
			height = int(self.getVariable("display.height"))
			width = int(self.getVariable("display.width"))
			
			#SMA: keep the last screen dimensions we receive
			self.__screen_height = height
			self.__screen_width = width
		
		except TypeError:
			# SMA code update
			print 'Caught exception while getting screen height and width.'
			
			if MonkeyDriver.RECOVERY_SCREEN_DIMENSIONS \
			and self.__screen_height is not None and self.__screen_width is not None:
				print 'Returning last values obtained successfully.'
				return self.__screen_width, self.__screen_height
			
			# SMA return default screen dimensions based on device
			if MonkeyDriver.DEBUG_DEFAULT_DIMENSIONS:
				print 'Returning default device screen dimensions.'
				if device == 'crespo':
					return MonkeyDriver.NEXUS_S_SCREEN_SIZE
				elif device == 'maguro':
					return MonkeyDriver.GALAXY_NEXUS_SCREEN_SIZE
			
			return None,None
			
		return width,height
	
	def getPlatformVersion(self):
	
		return self.getVariable("build.version.release")
	
	#SMA: a socket error causes all the tests to stop, before stopping
	#do a number of retries
	#createD the socketSend function to have retries and 
	#it can be used for both __sendCommand__ and getVar
	#This has a bunch of the old __sencCommand__ code.
	def __socketSend__(self,command):
		if not self.__isConnected:
			# attempt to reconnect money
			if not self.reConnectMoney():
				return None
		
		numTries = 0
		reconnectAttempted = False
		
		while numTries < MonkeyDriver.SOCKET_TRIES:
			numTries += 1
			try:
				#SMA
				#print "Sending command:", command, "Try:", numTries
				
				self.sock.send(command + "\n")	
				data = self.sock.recv(4096).strip()
				
				# SMA
				#print "Data from monkeydriver:", data 
				
				return data
			
			except socket.timeout:
				print 'MoneyDriver socket timed out.'
			
			except socket.error:
				print "MonkeyDriver socket error!"
				# SMA Code change
				"""
				self.sock.close()
				self.__isConnected = False
				return False
				"""
			
			if numTries >= MonkeyDriver.SOCKET_TRIES and not reconnectAttempted:
				# try a reconnect and reset the num tries
				print 'Maximum number of retries reach. Attempting a reconnect...'
				numTries = 0
				reconnectedAttempt = True
				self.reConnectMoney()
			
		print "MonkeyDriver retries reached. Reconnect attempted tried. Stopping."
		self.disconnectMonkey()
		return None
	
	def __sendCommand__(self, command):
		data = self.__socketSend__(command)
		
		if data is None:
			return False
		
		if data == "OK":
			return True
		
		return False
	
	"""
	# old version of send command
	def __sendCommand__(self, command):
		if not self.__isConnected:
			return False
		
		numTries = 0
		reconnectAttempted = False
		
		while numTries < MonkeyDriver.SOCKET_TRIES:
			numTries += 1
			try:
				#SMA
				print "Socket sending:", command, "Try:", numTries
				
				self.sock.send(command + "\n")	
				data = self.sock.recv(4096).strip()
				
				# SMA
				print "Data from monkeydriver:", data 
				
				if data == "OK":
					return True
				return False
			
			except socket.error:
				print "MonkeyDriver socket error!"
				# SMA Code change
			
			if numTries >= MonkeyDriver.SOCKET_TRIES and not reconnectAttempted:
				# try a reconnect and reset the num tries
				print 'Maximum number of retries reach. Attempting a reconnect...'
				numTries = 0
				reconnectedAttempt = True
				self.reConnectMoney()
			
		print "MonkeyDriver retries reached. Reconnect attempted tried. Stopping."
		self.disconnectMonkey()
		return False
	"""
		
	# android 2.3.4 could accept floating point coordinates
	# android 4.1.1 does not like floating point and only accepts integers
	# changed str(x) to str(int(x)) to fix this problem
	def sendTap(self,xCoord,yCoord):
		return self.__sendCommand__("tap " + str(int(xCoord)) + " " + str(int(yCoord)))	
		
	def sendKeyUp(self,key):
		return self.__sendCommand__("key up " + key)
		
	def sendKeyDown(self,key):
		return self.__sendCommand__("key down " + key)
	
	def sendTouchUp(self,xCoord,yCoord):
		return self.__sendCommand__("touch up " + str(int(xCoord)) + " " + str(int(yCoord)))
	
	def sendTouchDown(self,xCoord,yCoord,):
		return self.__sendCommand__("touch down " + str(int(xCoord)) + " " + str(int(yCoord)))
	
	def sendTouchMove(self,xCoord,yCoord,):
		return self.__sendCommand__("touch move " + str(int(xCoord)) + " " + str(int(yCoord)))
	
	def sendTrackBallMove(self,dx,dy):
		return self.__sendCommand__("trackball " + str(dx) + " " + str(dy))
	
	def sendPress(self, key):
		return self.__sendCommand__("press " + key)
	
	def sendType(self, text):
		return self.__sendCommand__("type " + text)
	
	def closeMonkey(self):
		#pass
		#SMA
		self.disconnectMonkey()
		

