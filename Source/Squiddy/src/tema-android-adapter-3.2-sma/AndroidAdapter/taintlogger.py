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

import subprocess
import re
import AndroidAdapter.traverserdb as db
import threading
import datetime
import time

class TaintLoggerThread(threading.Thread):
	
	PRINT_TAINT_INSERTIONS = False
	DEBUG = False

	def __init__(self, db, traversal_id, serial_id):
		threading.Thread.__init__(self)
		self.__db = db
		self.__traversal = self.__db.select_taintlogger_traversal(traversal_id)
		self.__serial_id = serial_id
		self.__stop = False

	def stop(self):
		self.__stop = True

	def run(self):
		try:
			p = subprocess.Popen(['adb', '-s', self.__serial_id, 'logcat', '-v', 'long', 'dalvikvm:V', 'TaintLog:V', '*:S'], stdout = subprocess.PIPE)
		
			header = re.compile(r'\[\s*(?P<month>[0-1][0-9])-(?P<day>[0-3][0-9])'
					    r'\s*(?P<hour>[0-2][0-9]):(?P<min>[0-5][0-9]):(?P<sec>[0-5][0-9])\.(?P<msec>[0-9]+)' 
					    r'\s*(?P<pid>[0-9]*):\s*(?P<tid>(0x)?[0-9|a-f]+)'
					    r'\s*(?P<priority>.)/(?P<tag>\S*)\s*\]')
	
			message = re.compile(r'data=|\[')
	
			extendedMessage = False
			extendedPid = None
			extendedTid = None
			
			lastHeaderMatch = None
			lastTag = None
	
			while not self.__stop:
				o = p.stdout.readline().strip()
				if o == '':
					if p.poll():
						break
					else:
						# skip the line
						continue
				#print o
	
				m = header.search(o)
				if (m):
					lastHeaderMatch = m
					lastTag = m.group('tag')
					#print o
					#print lastTag
				else: # none header message
					
					# we need a header to be able to process any logs
					if lastHeaderMatch is None or lastTag is None:
						continue 
					
					#print o
					
					n = message.search(o)
					#if (n):
					if (lastTag.startswith('dalvik') and o.startswith('TaintLog')) or (lastTag.startswith('TaintLog')):
					#if (o.startswith('TaintLog')):
						#if (o.find('data') != -1):
						if (n):
							#print 'EXTENDED MESSAGE'
							partialMessage = list()
							partialMessage.append(o)
							extendedMessage = True
							extendedHeaderMatch = lastHeaderMatch
						else:
							# if it is a taintlog but not a message that spans several log outputs, just print it
							logMessage = o
							self._insertMessage(lastHeaderMatch, logMessage)
					elif (extendedMessage):
						#TODO: Using pid and tid as a way to stop message extension might cause some data loss
						if (extendedHeaderMatch.group('pid') != lastHeaderMatch.group('pid')  or 
                                                                extendedHeaderMatch.group('tid') != lastHeaderMatch.group('tid') ):
							logMessage = ''.join(partialMessage)
							extendedMessage = False
							partialMessage = None
							self._insertMessage(extendedHeaderMatch, logMessage)
						elif (o.endswith(']')): 
							partialMessage.append(o)
							logMessage = ''.join(partialMessage)
							extendedMessage = False
							partialMessage = None
							self._insertMessage(extendedHeaderMatch, logMessage)
						else:
							partialMessage.append(o)
		
		except (ValueError, KeyboardInterrupt, OSError):
			#raise
			print 'taintLog exception'
			if (p):
				p.terminate()

	# under assumption that pc and mobile clocks are relatively synchronized
	def _insertMessage(self, headerMatch, message):
		if TaintLoggerThread.DEBUG:
			print "_insertMessage called:", headerMatch.group(), '\n', message
		
		if not self.__db:
			return
		
		pid = headerMatch.group('pid')
		tid = headerMatch.group('tid')
		priority = headerMatch.group('priority')
		tag = headerMatch.group('tag')
		_type = None
		day = int(headerMatch.group('day'))
		month = int(headerMatch.group('month'))
		hour = int(headerMatch.group('hour'))
		minute = int(headerMatch.group('min'))
		sec = int(headerMatch.group('sec'))
		msec = int(headerMatch.group('msec'))

		year = datetime.date.today().year

		dt = datetime.datetime(year, month, day, hour, minute, sec, msec)

		# in the unlikely event that the year is calculated incorrectly by going over to first night
		# the following check should provide a fix
		if (dt - datetime.datetime.now() > datetime.timedelta(days = 1)):
			print 'Year overlap... Correcting...'
			dt = datetime.datetime(year - 1, month, day, hour, minute, sec, msec)

		#time = dt.isoformat()

		# make the message a unicode
		unicode_message = unicode(message, errors='ignore')
		
		if TaintLoggerThread.PRINT_TAINT_INSERTIONS:
			#print '\nInserting TaintLog:', unicode_message
			print '\nInserting TaintLog:', tag, message[:20]

		#self.__db.insertTaintLog(pid, tid, priority, tag, _type, unicode_message, time);

		taint_log = db.TaintLog()
		taint_log.pid = pid
		taint_log.tid = tid
		taint_log.priority = priority
		taint_log.tag = tag
		taint_log.type = _type
		taint_log.message = unicode_message
		taint_log.timestamp = dt
		
		self.__traversal.taintlogs.append(taint_log)
		self.__db.insert_taint_item(taint_log)


if __name__ == "__main__":
	#serial_id = '393524A9C33F00EC'
	serial_id = '39352528C3CC00EC'
	t = TaintLoggerThread('tmp.db', serial_id)
	t.setDebug(True)
	try:
		t.start()
		while (True):
			time.sleep(60)
	except KeyboardInterrupt:
		t.stop()

