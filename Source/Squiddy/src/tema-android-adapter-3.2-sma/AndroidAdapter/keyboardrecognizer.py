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

import AndroidAdapter.sequencealignment as align
import AndroidAdapter.imagemagick as imagemagick
import AndroidAdapter.ocr as ocr
import os.path
import os

DEBUG = True
DELETE_INTERMEDIATE_TEXT_FILES = True

SIMPLE_KEYBOARD = ['q,w,e,r,t,y,u,i,o,p',
                   'a,s,d,f,g,h,j,k,l',
                   'z,x,c,v,b,n,m']
SIMPLE_KEYBOARD = [x.lower().replace(',','') for x in SIMPLE_KEYBOARD]

SYMBOLS_KEYBOARD =['1,2,3,4,5,6,7,8,9,0',
                  '@,#,$,%,&,*,-,+,(,)',
                  """!,",',:,;,/,?"""]
SYMBOLS_KEYBOARD = [x.lower().replace(',','') for x in SYMBOLS_KEYBOARD]

PHONE_PAD = ['1,2,a,b,c,3,d,e,f',
             '4,g,h,i,5,j,k,l,6,m,n,o',
             '7,p,q,r,s,8,t,u,v,9,w,x,y,z']
#             '*,#,0,+']
PHONE_PAD = [x.lower().replace(',','') for x in PHONE_PAD]

NUMBER_PAD = ['1,2,3,-',
              '4,5,6,.',
              '7,8,9']
#              '0,_']
NUMBER_PAD = [x.lower().replace(',','') for x in NUMBER_PAD]

KEYBOARDS = [SIMPLE_KEYBOARD, SYMBOLS_KEYBOARD, PHONE_PAD, NUMBER_PAD]

# the score total of 50 requires that multiple lines be matched on the number pad
IDENTITY_SUM_THRESHOLD = 60
SCORE_SUM_THRESHOLD = 50

#TODO: instead of doing all the rotations and ocrs in the beginning
#search for the keyboard first, if we do not find it, then rotate the
#image, do ocr, and re-run the keyboard check
def keyboard_on_screen(input_file):
    if input_file is None:
        return None
    
    if not os.path.exists(input_file):
        return None
    
    if not os.path.isfile(input_file):
        return None
    
    rotation_degrees = [0, 90, 180, 270]
    
    rotations = []
    
    for rotation_degree in rotation_degrees:
        if (rotation_degree == 0):
            file = input_file
        else:
            # produce rotations of the keyboard by 90, 180, 270
            file = imagemagick.rotate(input_file, rotation_degree)
            rotations.append(file)
        
        if file is None or len(file) <= 0:
            continue
        
        # perform OCR on the images and obtain all their texts
        # read the lines for all the files
        lines = ocr.read_image_lines(file, DELETE_INTERMEDIATE_TEXT_FILES)
        
        if lines is None or len(lines) <= 0:
            continue
        
        is_keyboard = compare_lines_to_keyboards(lines)
        
        #print file, is_keyboard
        
        if (is_keyboard):
            break
    
    # remove the produced rotations
    for file in rotations:
        if file is None or len(file) <= 0:
            continue
        try:
            os.remove(file)
        except:
            print 'Was not able to remove image rotation:', file
    
    if (DEBUG):        
        print input_file, is_keyboard
        print ''
            
    return is_keyboard

# compares the lines to all keyboard instances and if
# at least one of the keyboards is similar to the lines
# return true
def compare_lines_to_keyboards(lines):
    if lines is None or len(lines) <= 0:
        return None
    
    for keyboard in KEYBOARDS:
        if compare_lines_to_keyboard(lines, keyboard):
            return True
        
    return False

# compares an array of lines to a keyboard and returns true
# if the sum of identities is larger than the thresholds
def compare_lines_to_keyboard(lines, keyboard):
    if lines is None or len(lines) <= 0:
        return None
    
    if keyboard is None or len(keyboard) <= 0:
        return None
    
    identity_sum = 0
    score_sum = 0
    
    for line in lines:
        result = compare_to_keyboard(line, keyboard)
        
        # values are only added to the total if both the score and the identity were positive
        if result is None or min(result) <= 0:
            continue
        
        identity, score = result
        
        identity_sum += identity
        score_sum += score
        
    #print 'Sums:', identity_sum, score_sum
        
    return identity_sum >= IDENTITY_SUM_THRESHOLD and score_sum >= SCORE_SUM_THRESHOLD

# compares a line of text to rows in a keyboard and 
# returns the maximum identity score based on comparison
def compare_to_keyboard(line, keyboard):
    if line is None:
        return None
    
    if keyboard is None or len(keyboard) <= 0:
        return None
    
    # remove unnecessary characters from the line
    line = line.rstrip('\r\n').replace(' ','').lower()
    
    if len(line) <= 0:
        return None
    
    max_identity = 0
    max_score = 0
    
    # run alignment on all keys in the keyboard and return the maximum
    for row in keyboard:
        identity, score = align.needle(line, row)
        
        # require the score to be positive as to have some matches
        if score <= 0:
            continue
        
        if max_identity <= identity and max_score < score:
            max_identity = identity
            max_score = score
                
    return max_identity, max_score

"""
if __name__ == "__main__":
    # check a bunch of files to see if they are keyboard or not
    files = files_string.split(',')
    for file in files:
        keyboard_on_screen(file)
"""
