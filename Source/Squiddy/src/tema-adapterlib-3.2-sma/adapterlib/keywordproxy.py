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

class KeywordProxy(object):
    """
    Superclass for keyword proxies.
        
    Keyword proxies can be used as proxy for other keyword-based tools.
    """

        
    def __init__(self, name="", delay=-1):
        self.__name = name
        self.delay = delay
        self._log = None
        self._target = None
        self.attributePattern = re.compile(".*")
        self.kwPattern = re.compile(".*")
        self.shouldLog = True
        self.attributes = None
        self.kw = None
        
    def initialize(self,kw,attributes,target):
        """    
        Initialize keyword proxy with attributes, kw and target. Must be called
        before execute. Initializes log with empty string.

        @type kw: string
        @param kw: String representation of the keyword.
        @type attributes: string
        @param attributes: String representation of the keyword attributes. 
        @param target: Target used for keyword executiuon
        @rtype: boolean
        @returns: True if attributes and kw are valid for this keyword proxy, 
                  False otherwise
        """
        self._target = target
        self._log = ""

        self.kw = kw
        self.attributes = attributes        
        return self.isMyKeyword(kw,attributes)

    #Checks if given string corresponds to this keyword
    def isMyKeyword(self, kw, attributes ):    
        """    
        Check if the given string corresponds to the keyword pattern 
        of this keyword proxy. Uses the regular expression in kwPattern
        instance variable.
        
        @type kw: string
        @param kw: String representation of the checked keyword. 
        @type attributes: string
        @param attributes: String representation of the keyword attributes
        """        
        return self.kwPattern.match(kw) != None
                
    def execute(self):
        """ 
        Executes the keyword. Should be overridden in child classes 
                
        @rtype: boolean or "ERROR"
        @return: Returns a value representing the result of the keyword 
        execution. "ERROR" is returned if the execution fails for some reason. 
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
