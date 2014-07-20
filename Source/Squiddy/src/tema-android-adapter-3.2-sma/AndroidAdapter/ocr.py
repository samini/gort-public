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
import os
import os.path

def read_image_lines(input_file, delete_output_file=False):
    output_file = read_image_to_file(input_file)
    
    if output_file is None or len(output_file) <= 0:
        return None
    
    if not os.path.exists(output_file):
        return None
    
    if not os.path.isfile(output_file):
        return None
    
    f = open(output_file, 'r')
    ret_val = f.readlines()
    f.close()
    
    if delete_output_file:
        try:
            os.remove(output_file)
        except OSError:
            pass
    
    return ret_val

def read_image_to_file(input_file, output_file = None):
    
    if input_file is None or len(input_file) <= 0:
        return None
    
    if not os.path.exists(input_file):
        return None
    
    if not os.path.isfile(input_file):
        return None
    
    if output_file is None or len(output_file) <= 0:
        # tesseract automatically appends a text to the image name
        output_file = input_file
    
    try:
        output = subprocess.call(['tesseract', input_file, output_file, '-l', 'eng'], stderr = subprocess.PIPE)
        
        if not output:
            return '%s.txt' % output_file
        
        return None
    
    except subprocess.CalledProcessError:
        return None
    
    
