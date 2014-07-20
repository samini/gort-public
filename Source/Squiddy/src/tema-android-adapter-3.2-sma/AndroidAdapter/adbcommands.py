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
import AndroidAdapter.utils as utils

"""
def focusedActivity(serial_id): 

	try:
		output =  subprocess.check_output(["adb", "-s", serial_id, "shell", "dumpsys", "activity"], stderr = subprocess.PIPE)
	except subprocess.CalledProcessError:
		return None

	outputSplit = output.split('\n')

	# sample 
	#mFocusedActivity: HistoryRecord{408238d8 com.metago.astro/.FileManagerActivity}
	for line in outputSplit:
		if (line.find("mFocusedActivity") >= 0):
			lineSplit = line.split(' ')
			retval = lineSplit[len(lineSplit)-1].rstrip('}\r\n')
			return retval

	return None
"""

# returns the device time in seconds since epoch
def deviceTime(serial_id):
	if not serial_id:
		return None
	
	commandArray = ["adb", "-s", serial_id, "shell", "date", '+"%s"']
	output = None
	
	try:
		output = subprocess.check_output(commandArray, stderr = subprocess.PIPE)
	except AttributeError:
		output = subprocess.Popen(commandArray, stdout = subprocess.PIPE).communicate()[0]
	except subprocess.CalledProcessError:
		pass
	
	if output is None:
		return None
	
	return float(output)

def focusedActivity(serial_id):
	tmp = recentActivities(serial_id)
	
	if not tmp:
		return None

	if "mFocusedActivity" in tmp:
		return tmp["mFocusedActivity"]
	else:
		return None 

def pausingActivity(serial_id):
	tmp = recentActivities(serial_id)
	
	if not tmp:
		return None

	if "mPausingActivity" in tmp:
		return tmp["mPausingActivity"]
	else:
		return None

def resumedActivity(serial_id):
	tmp = recentActivities(serial_id)

	if "mResumedActivity" in tmp:
		return tmp["mResumedActivity"]
	else:
		return None

def lastPausedActivity(serial_id):
	tmp = recentActivities(serial_id)
	
	if not tmp:
		return None
	
	if "mLastPausedActivity" in tmp:
		return tmp["mLastPausedActivity"]
	else:
		return None

def recentActivities(serial_id): 
	commandArray = ["adb", "-s", serial_id, "shell", "dumpsys", "activity"]

	try:
		output = subprocess.check_output(commandArray, stderr = subprocess.PIPE)
	except AttributeError:
		output = subprocess.Popen(commandArray, stdout = subprocess.PIPE).communicate()[0]
	except subprocess.CalledProcessError:
		return None

	outputSplit = output.split('\n')

	keys = ['mPausingActivity', 'mResumedActivity', 'mFocusedActivity', 'mLastPausedActivity']

	activities = dict()

	for line in outputSplit:
		for key in keys:
			if (line.find(key) >= 0):
				lineSplit = line.split(' ')
				tmp = lineSplit[len(lineSplit)-1].rstrip('}\r\n')
				# remove any 'slashes'
				tmp = tmp.replace('/', '')
				activities[key] = tmp

	if (len(activities) > 0):
		return activities

	return None


# Most recent first in the list
#  Running activities (most recent first):
#    TaskRecord{40aa7610 #6 A com.yelp.android}
#      Run #1: HistoryRecord{40a008c0 com.yelp.android/.ui.activities.ActivityHome}
#    TaskRecord{4084ac80 #2 A com.android.launcher}
#      Run #0: HistoryRecord{408bd8c0 com.android.launcher/com.android.launcher2.Launcher}
def runningActivities(serial_id):
	commandArray = ["adb", "-s", serial_id, "shell", "dumpsys", "activity"]

	try:
		output = subprocess.check_output(commandArray, stderr = subprocess.PIPE)
	except AttributeError:
	        output = subprocess.Popen(commandArray, stdout = subprocess.PIPE).communicate()[0]
	except subprocess.CalledProcessError:
		return None

	outputSplit = output.split('\n')

	found = False
	activities = list()

	#count = 0

	for line in outputSplit:
		if not found:
			if (line.find('Running activities') >= 0):
				found = True
		else:
			tmp = line.strip(' \r\n')
			#count += 1
			#print count
			if (tmp == ''):
				break
			if (tmp.find('HistoryRecord') >= 0):
				tmpSplit = tmp.split(' ')
				activities.append(tmpSplit[len(tmpSplit)-1].rstrip('}'))

	if (len(activities) > 0):
		return activities

	return None

def getProp(serial_id, prop):

	if prop is None or prop == "":
		return None

	commandArray = ["adb", "-s", serial_id, "shell", "getprop", prop]    

	output = None

	try:
		output = subprocess.check_output(commandArray, stderr = subprocess.PIPE)
	except AttributeError:
		output = subprocess.Popen(commandArray, stdout = subprocess.PIPE).communicate()[0]
	except subprocess.CalledProcessError:
		return None

	if output:
		return output.rstrip('\r\n')

	return None



def bootCompleted(serial_id):
	return (bool(getProp(serial_id, "dev.bootcomplete")) and bool(getProp(serial_id, "sys.boot_completed")))



def keyboardShown(serial_id):
	temp = getProp(serial_id,"keyboard.status")
	if not temp:
		return None
	else:
		value = temp.strip().lower()
		if (value == "true"):
			return True
		elif (value == "false"):
			return False
		
		return None

# adb shell ps | grep com.newgt.musicbest | awk '{print $2}' | xargs adb shell kill
def processStatus(serial_id):
	commandArray = ["adb", "-s", serial_id, "shell", "ps"]
	
	output = None

	try:
		output = subprocess.check_output(commandArray, stderr = subprocess.PIPE)
	except AttributeError:
		output = subprocess.Popen(commandArray, stdout = subprocess.PIPE).communicate()[0]
	except subprocess.CalledProcessError:
		return None

	if output:
		return output.rstrip('\r\n')

	return None

def processId(serial_id, packageName):
	
	if packageName is None or packageName == "":
		return None
	
	packageName = packageName.strip().lower()
	
	psOutput = processStatus(serial_id)
	psOutputSplit = psOutput.split('\n')
	
	processInfo = None
	pid = None
	
	for process in psOutputSplit:
		process = process.rstrip('\r\n').lower()
		
		if (process.find(packageName) > 0):
			# sample process
			# u0_a69    5631  88    316884 48312 ffffffff 40072a40 s com.newgt.musicbest
			processInfo = process
			processInfo = re.sub(r'\s+', ' ', processInfo)
			
			processInfoSplit = processInfo.split(' ')
			pid = int(processInfoSplit[1])
		
	return pid

def killProcess(serial_id, pid=None, packageName=None):
	if (pid is None and packageName is None):
		return None
	
	# assign pid if it is None
	if (pid == None):
		pid = processId(serial_id, packageName)
	
	# return if pid is still None	
	if (pid == None):
		return None

	return subprocess.call(["adb", "-s", serial_id, "shell", "kill", str(pid)])

def forceStop(serial_id, packageName):
        stdout = subprocess.Popen(["adb", "-s", serial_id, "shell", "am", "force-stop", packageName], stdout=subprocess.PIPE).communicate()[0]
        if stdout == '':
                print "force stop succeed"
                return True
        else:
                for cnt in range(5):
                    ret = killProcess(serial_id, None, packageName)
                    utils.wait(1)
                    if ret == None:
                            print "force stop succeed"
                            return True
                print "force stop 5 times"
                return False

def killADB():
	commandArray = ["pkill", "-9", "-f", "adb"]    

	output = None

	try:
		output = subprocess.check_output(commandArray, stderr = subprocess.PIPE)
	except AttributeError:
		output = subprocess.Popen(commandArray, stdout = subprocess.PIPE).communicate()[0]
	except subprocess.CalledProcessError:
		return None
	
	return output

def startADB():
	
	commandArray = ["adb", "start-server"]    

	output = None

	try:
		output = subprocess.check_output(commandArray, stderr = subprocess.PIPE)
	except AttributeError:
		output = subprocess.Popen(commandArray, stdout = subprocess.PIPE).communicate()[0]
	except subprocess.CalledProcessError:
		return None
	
	return output

def restartADB():
	killADB()
	startADB()

def taintLog(serial_id):
	try:
		p = subprocess.Popen(['adb', '-s', serial_id, 'logcat', '-v', 'long', 'dalvikvm:V', '*:S'], stdout = subprocess.PIPE)
	
		#The header includes the time and date and also the process and thread id
		# note we do not include logcat priority level and tag since we specify those in the Popen function call
		header = re.compile(r'\[\s*(?P<month>[0-1][0-9])-(?P<day>[0-3][0-9])'
				    r'\s*(?P<hour>[0-2][0-9]):(?P<min>[0-5][0-9]):(?P<sec>[0-5][0-9])\.(?P<msec>[0-9]+)' 
				    r'\s*(?P<pid>[0-9]*):(?P<tid>0x[0-9|a-f]*)'
				    r'\s*(?P<priority>.)/(?P<tag>\S*)')

		message = re.compile(r'data=|\[')

		#http://blog.kagesenshi.org/2008/02/teeing-python-subprocesspopen-output.html

		# if a long TaintLog message with data= extension is found
		# keeps reading and getting output all the way to the end of data segment
		extendedMessage = False
		extendedPid = None
		extendedTid = None

		while (True):
			o = p.stdout.readline().strip()
			if o == '':
				if p.poll():
					break
				else:
					# skip the line
					continue

			print '============================'
			print 'Line:', o , 'Lenght', len(o), 'END'
	
			m = header.search(o)
			if (m):
				#print m.group()
				#print o
				print 'month:', m.group('month'), 'day:', m.group('day'), 'hour:', m.group('hour'), 'minute:', m.group('min'), 'second:', m.group('sec'), 'msec:', m.group('msec'), 'pid:', m.group('pid'), 'tid:', m.group('tid'), m.group('priority'), m.group('tag')
				lastPid = m.group('pid')
				lastTid = m.group('tid')
			else: # none header message
				n = message.search(o)
				#if (n):
				if (o.startswith('TaintLog')):
					#print 'REPEAT:', o
					#if (o.find('data') != -1):
					if (n):
						#print 'EXTENDED MESSAGE'
						logMessage = list()
						logMessage.append(o)
						extendedMessage = True
						extendedPid = lastPid
						extendedTid = lastTid
					else:
						# if it is a taintlog but not a message that spans several log outputs, just print it
						print '============================'
						print o
				elif (extendedMessage):
					#print 'REPEAT:', o
					#TODO: Using pid and tid as a way to stop message extension might cause some data loss
					logMessage.append(o)
					if (o.endswith(']') or (extendedPid != lastPid and extendedTid != lastTid)):
						#print 'END EXTENDED MESSAGE'
						extendedMessage = False
						print '============================'
						print ''.join(logMessage)
						logMessage = None
				

	except (ValueError, KeyboardInterrupt, OSError):
		#print 'taintLog exception'
		if (p):
			p.terminate()

if __name__ == "__main__":
	serial_id = "393524A9C33F00EC"
	#serial_id = "39352528C3CC00EC"
	#serial_id = "emulator-5554"
	print keyboardShown(serial_id)
	print keyboardShown(serial_id)
	print focusedActivity(serial_id)
	print lastPausedActivity(serial_id)
	print resumedActivity(serial_id)
	print pausingActivity(serial_id)
	print recentActivities(serial_id)
	print bootCompleted(serial_id)
	print getProp(serial_id, "sys.boot_completed")
	print getProp(serial_id, "dev.bootcomplete")
	print runningActivities(serial_id)
	#taintLog(serial_id)
	print processId(serial_id, "com.newgt.musicbest")
	print killProcess(serial_id, packageName="com.newgt.musicbest")
	print deviceTime(serial_id)
