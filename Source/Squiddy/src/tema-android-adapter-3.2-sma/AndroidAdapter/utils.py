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
import os
import sys
import subprocess
import re
		
def area(a):
	if (len(a) != 2):
		return -1

	return (a[0] * a[1])

def chdir(path):
	os.chdir(path)

def matchText(hayStack, needle):
    """
    Returns true if instances of needle elements are
    present in the hayStack. The regex matching
    is case-insensitive.
    """
    if hayStack is None or needle is None:
        return False

    hayStackType = type(hayStack)

    if hayStackType is list:
        hayStack = " ".join(hayStack)
    elif hayStackType is str:
        pass
    else:
        return False

    needleType = type(needle)
    needleString = None

    if needleType is str:
        needleString = needle
    elif needleType is list:
        if len(needle) <= 0:
            return False
        elif len(needle) == 1:
            needleString = needle[0]
        else:
            needleString = "|".join(needle)
    else:
        return False
       
    needleString = needleString.strip()   
       
    if len(needleString) <= 0:
    	return False

	# append the needleString with \b on both ends to indicate word boundaries
	needleString = r'\b%s\b' %(needleString)

    regex = re.compile(needleString, re.IGNORECASE)

    return regex.search(hayStack) is not None

def mkdir(path):
	try:
		os.mkdir(path)
	except OSError:
		if os.path.exists(path):
			# We are nearly safe
			pass
		else:
			raise

def timeString(time=None):
	if not time:
		time = datetime.datetime.now()
	month = "%.2d" % time.month
	day = "%.2d" % time.day
	hour = "%.2d" % time.hour
	minute = "%.2d" % time.minute
	sec = "%.2d" % time.second

	return "%s%s%s-%s%s%s" % (str(time.year) ,  str(month) , str(day) , str(hour) , str(minute) , str(sec))

def timeIsoFormat():
	return datetime.datetime.now().isoformat().strip()

def wait(duration):
	if (duration == None):
		return

	sys.stdout.write("sleeping.")
	sys.stdout.flush()

	while(duration > 0):
		time.sleep(1)
		sys.stdout.write(".")
		sys.stdout.flush()
		duration -= 1

	print ''

def whois(address):
	if (address is None):
		return None

	try:
		output =  subprocess.check_output(["whois", address])
	except AttributeError:
		output =  subprocess.Popen(["whois", address], stdout = subprocess.PIPE).communicate()[0]
	except subprocess.CalledProcessError:
			return None

	if not output:
		return None

	lines = output.split('\n')

	for line in lines:
		#line = line.strip()
		#if (line == ''):
		#	continue
		if not line.startswith('#'):
			print line

if __name__ == "__main__":
	print timeString()
	whois('199.255.189.146')
	print '================================='
	whois('74.125.113.99')
	
