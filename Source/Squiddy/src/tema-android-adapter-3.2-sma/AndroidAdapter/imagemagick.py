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

# if PythonMagick works on your system, use PythonMagick, otherwise use this

import subprocess
import os.path

#these dimensions are written for the Nexus S
WIDTH = 480
HEIGHT = 800
KEYBOARD_WIDTH = WIDTH
KEYBOARD_HEIGHT = 300 # approximately
KEYBOARD_AREA = KEYBOARD_WIDTH * KEYBOARD_HEIGHT
KEYBOARD_WIDTH_OFFSET = 0
KEYBOARD_HEIGHT_OFFSET = 500
#TODO: create a env variable for resources or also package resources
KEYBOARD_FILE = '/Users/shahriyar/tmp/keyboardcrop.png' 
KEYBOARD_FILE_CRESPO = '/Users/shahriyar/tmp/keyboardcropcrespo.png'
KEYBOARD_FILE_MAGURO = '/Users/shahriyar/tmp/keyboardcropmaguro.png'
SCRATCH = '/Users/shahriyar/tmp'
KEYBOARD_ERROR_THRESHOLD = 0.15 #only 15% error acceptable

# metrics
"""
  AE     absolute error count, number of different pixels (-fuzz effected)
  FUZZ   mean color distance
  MAE    mean absolute error (normalized), average channel error distance
  MEPP   mean error per pixel (normalized mean error, normalized peak error)
  MSE    mean error squared, average of the channel error squared
  NCC    normalized cross correlation
  PAE    peak absolute (normalize peak absolute)
  PSNR   peak signal to noise ratio
  RMSE   root mean squared (normalized root mean squared)
"""
# needs ImageMagick installed
def compare(fileA, fileB, metric='AE', fileOut='/dev/null'):
	output = None
	try:
		#ImageMagick returns output on stderr
		#output =  subprocess.check_output(["compare", "-metric", metric, fileA, fileB, fileOut], stderr = subprocess.PIPE)
		output =  subprocess.check_output(["compare", "-metric", metric, fileA, fileB, fileOut], stderr = subprocess.STDOUT)
	except AttributeError:
		output =  subprocess.Popen(["compare", "-metric", metric, fileA, fileB, fileOut], stdout = subprocess.STDOUT).communicate()[0]
	except subprocess.CalledProcessError:
		pass 

	if output:
		return float(output.strip().split(' ')[0]) # someoutputs have multiple entries

	return None

#convert function written for Nexus S which has a 480x800 display resolution
#mainly written to cut off the keyboard portion of a screenshot, but can also be used to cut crop other images
#returns whether crop was successful or not
def crop(inputFile, outputFile, width=KEYBOARD_WIDTH, height=KEYBOARD_HEIGHT, widthOffset=KEYBOARD_WIDTH_OFFSET, heightOffset=KEYBOARD_HEIGHT_OFFSET):
	try:
		dimensions = '%dx%d+%d+%d' % (width, height, widthOffset, heightOffset)
		output =  subprocess.call(["convert", inputFile, "-crop", dimensions, outputFile], stderr = subprocess.PIPE)
		if not (output):
			return True
		return False
	except subprocess.CalledProcessError:
		return False
        
#check to see if page has a keyboard in it
def keyboardOn(inputFile, outputFile, keyboardFile=KEYBOARD_FILE_CRESPO):
	val = crop(inputFile, outputFile)
	if not val:
		raise Exception('Crop')

	threshold = KEYBOARD_ERROR_THRESHOLD * KEYBOARD_AREA	
	return similar(outputFile, keyboardFile, threshold)

def resize(inputFile, outputFile, newWidth, newHeight):
	try:
		newsize = '%dx%d' % (newWidth, newHeight)
		output =  subprocess.call(["convert", inputFile, "-resize", newsize, outputFile], stderr = subprocess.PIPE)
		# return code is 0 if successful
		if not (output):
			return True
		return False
	except subprocess.CalledProcessError:
		return False

def rotate(inputFile, degrees, outputFile=None):
	if inputFile is None or len(inputFile) <= 0:
		return None

	if not os.path.exists(inputFile):
		return None

	if not os.path.isfile(inputFile):
		return None
	
	if degrees is None:
		return None
	
	if outputFile is None or len(outputFile) <= 0:
		basename = os.path.basename(inputFile)
		dirname = os.path.dirname(inputFile)
		root, ext = os.path.splitext(inputFile)

		outputFile = '%s-rotated%d' % (root, degrees)

		if ext is not None and len(ext) > 0:
			outputFile = outputFile + ext

		outputFile = os.path.join(dirname, outputFile)

	try:
		output =  subprocess.call(["convert", "-rotate", str(degrees), inputFile, outputFile], stderr = subprocess.PIPE)
		if not (output):
			return outputFile
		return None
	
	except subprocess.CalledProcessError:
		return None

# rotates the image by 90, 180, and 270 degrees
# and returns the filename for all images including
# the original
def rotations(inputFile):
    if inputFile is None:
        return None
    
    if not os.path.exists(inputFile):
        return None
    
    if not os.path.isfile(inputFile):
        return None
    
    retVal = [inputFile]
    
    rotations = [90, 180, 270]
    
    for rotation in rotations:
        result = rotate(inputFile, rotation)
        
        if result is None:
            pass
        
        retVal.append(result)
        
    return retVal

# threshold is provided in terms of # of pixels
def similar(fileA, fileB, threshold):
	error = compare(fileA, fileB)

	if (error < threshold):
		return True

	return False

if __name__ == "__main__":
	print 'Keyboard area:', KEYBOARD_AREA

