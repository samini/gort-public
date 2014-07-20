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


import os
import datetime
import cgi


""" Class for creating html-logs from test runs.

The log includes each keyword with it's execution information and a screenshot
from the device after the keyword was executed.
"""

class KeywordLogger(object):

    def __init__(self):
        self.__logFile = None
        self.__imageDir = None
        self.__screenCount = 0

    def startLog(self):
        
        if not os.path.exists("logs"):
            os.makedirs("logs")
        
        time = datetime.datetime.now()
        timestamp = "%d%.2d%.2d-%.2d%.2d%.2d" % (time.year,time.month,time.day,time.hour,time.minute,time.second)

        self.__imageDir = os.path.join("logs",timestamp,"screens")
        os.makedirs(self.__imageDir)
            
        self.__logFile = open(os.path.join("logs",timestamp,"log.html"),'w')
        self.__logFile.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n")
        self.__logFile.write("<html>\n")
        self.__logFile.write("<head>\n")
        self.__logFile.write("<title> Testrun %s</title>\n" % timestamp)

        self.__logFile.write("</head>\n")
        self.__logFile.write("<body>\n")
        self.__logFile.write("<table border=\"1\">\n")
        
    
    def logKeyword(self,target, kw, result, exectime):
        
        
        self.__logFile.write("<tr>\n")

        self.__logFile.write("<td>\n")
        self.__logFile.write("<b>Target: </b>%s<br>\n" % cgi.escape(target.name))
        self.__logFile.write("<b><u>%s</u></b>\n" % cgi.escape("%s %s" % (kw.__class__.__name__, kw.attributes)))
        self.__logFile.write("<br> <b>Result:</b> %s<br>\n" % str(result))
        self.__logFile.write("<b>Execution time:</b> %s<br>\n" % str(exectime))
        self.__logFile.write("<b>Timestamp:</b> %s<br>\n" % str(datetime.datetime.now()))
        
        log = kw.getLog()
        
        if log != "":
            lines = log.splitlines()
            self.__logFile.write("<b>Log:</b> <br>\n")
            for line in lines:
                self.__logFile.write("%s<br>\n" % cgi.escape(line))

        self.__logFile.write("</td>\n")
        
        if kw.shouldLog:
#            if target.takeScreenShot(self.__imageDir + "screen" + str(self.__screenCount) + ".png"):
            if target.takeScreenShot(os.path.join(self.__imageDir,"screen%d.png" % self.__screenCount)):
                self.__logFile.write("<td><img src =\" screens%sscreen%d.png \" />\n" %(os.sep,self.__screenCount))
                self.__screenCount += 1
                self.__logFile.write("</td>\n")
            
        self.__logFile.write("</tr>\n")
        self.__logFile.flush()

    
    def endLog(self):
        
        if self.__logFile:
            self.__logFile.write("</table>\n")
            self.__logFile.write("</body>\n")
            self.__logFile.flush()
            self.__logFile.close()
