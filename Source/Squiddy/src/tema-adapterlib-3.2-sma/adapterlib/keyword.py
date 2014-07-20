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

import re

#-----------------------------------------------------------------------------#

class Keyword(object):
                
    """   
    Superclass for keywords
    
    Defines methods for checking and executing keywords, as well as methods
    for searching components

    @type shouldLog: boolean
    @cvar shouldLog: Tells if this keyword be included into the screenshot log.
    @cvar delay: Delay after keyword execution (in seconds). If -1, no delay 
                 at all including delay given to adapter with -t command line 
                 option.
    @cvar attributes: Keyword attributes
    @type attributes: None or str
    @cvar attributePattern:  regexp that defines attributes for this keyword.
    @type attributePattern: re.pattern
    """

    def __init__(self, name="", delay=0):
        self.__name = name
        self.delay = delay
        self.attributePattern = re.compile(".*")
        self.attributes = None
        self._log = None
        self._target = None
        self.shouldLog = True

    def initialize(self,attributes,target):
        """    
        Initialize keyword with attributes and target. Must be called before 
        execute. Initializes log with empty string.

        @type attributes: string
        @param attributes: String representation of the keyword attributes. 
        @param target: Target used for keyword executiuon
        @rtype: boolean
        @returns: True if attributes are valid for this keyword, 
                  False otherwise
        """
        self._target = target
        self.attributes = attributes
        self._log = ""

        return self.isMyKeyword(attributes)

    #Checks if given string corresponds to this keyword
    def isMyKeyword(self, attributes):        
        """    
        Check if the given string corresponds to the keyword attribute pattern
        of this keyword. Uses the regular expression set in instance variable 
        attributePattern

        @type kwstring: string
        @param kwstring: String representation of the keyword attributes. 
        @returns: True if attributes are valid for this keyword, 
                  False otherwise
        """                
        return self.attributePattern.match(attributes) != None
        
    def execute(self):   
        """ 
        Executes the keyword. Should be overridden in child classes 
                
        @rtype: boolean or "ERROR"
        @return: Returns a value representing the result of the keyword 
        execution. "ERROR" is returned if the exection fails for some reason. 
        """
        raise NotImplementedError

    def log(self, message):
        """ Logs the given message. """
        self._log += "%s\n" % message
        print message

    def getLog(self):
        """" Returns log
        @rtype: str
        """
        return self._log

