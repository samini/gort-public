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
"""
Module for running keyword-driven tests
"""
from __future__ import with_statement

import time
import datetime
import re

from adapterlib.ToolProtocol import *
from adapterlib.ToolProtocolHTTP import *
import adapterlib.keyword as keyword
import adapterlib.keywordproxy as keywordproxy
from adapterlib.logger import KeywordLogger

class AdapterCompleter(object):
    """ Simple class for doing tab-completion in interactive mode"""
    def __init__(self, keywords ):
        self.keywords = sorted(keywords)

    def complete(self, text, state ):
        response = None
        if state == 0:
            if text:
                self.matches = [s for s in self.keywords if s and s.startswith(text)]
            else:
                self.matches = self.keywords[:]
        try:
            response = self.matches[state]
        except IndexError:
            response = None

        return response

class Target(object):
    
    def __init__(self,name,):
        self.__name = name
    
    
    def setup(self):         
        raise NotImplementedError()

    def cleanup(self):
        raise NotImplementedError()
        
    @property
    def name(self):
        return self.__name

    def takeScreenShot(self, path):
        return False
        

class TestRunner(object):

    """   
    TestRunner class is used to run Keyword-driven tests.
    
    The class allows test to be run interactively (given through stdin), from 
    file or from server.
    
    To run tests from a server, TestRunner uses classes ToolProtocol and 
    ToolProtocolHTTP.
    """
    
    def __init__(self, targets, delay, record = False ):
        
        """
        Initializer.
        
        @type targets: list
        @param targets: list of System under test (SUT) identifiers.
        
        @type delay: float
        @param delay: Wait-time between consequtive keywords (in seconds)
        
        @type record: boolean
        @param record: Is the test recorded to html-file
        """
            
        self._targetNames = targets
        self._targets = []
        
        self.delay = delay  

        self._rec_process = None
        self._kwCount = 1
        
        self._logger = None
        self._separator = " "
        if record:
            self._logger = KeywordLogger()

        self._kw_cache = {}

        # Special commands listed here for interactive mode completer
        self._commands = {}
        self._commands["exit"] = ["quit","q","exit"]
        self._commands["kws"] = ["list","kws","list full","kws full"]
        self._commands["info"] = ["info"]
        self._commands["special"] = []
    
    def _setupTestAutomation(self):
        """Sets up test automation environment
        
        @rtype: boolean
        @returns: True if success, False otherwise
        """
        raise NotImplementedError()


    def _cleanupTestAutomation(self):
        """Cleans up test automation environment"""
        raise NotImplementedError()


    def __setTarget(self,targetName):
        
        if re.match("['\"].*['\"]",targetName):
            targetName = targetName[1:-1]
    
        if targetName == "test" or targetName == "testi":
            print "Warning: 'test' and 'testi' considered dummy targets."
            return True
    
        for t in self._targets:
            if t.name == targetName:
                self._activeTarget = t
                return True 
        return False
        
        
    def initTest(self):
        """
        
        Inits a test run.
        
        Creates a log file and starts recording if defined.
        
        """
        print "Setting up testing environment..."
        if not self._setupTestAutomation():
            return False
        print "setup complete"
        
        self._activeTarget = self._targets[0]
        
        if self._logger:
            print "Recording test to a file"
            self._logger.startLog()
            
        return True
          
    def _stopTest(self):
        """
        Stops a test run.
        
        Closes the log-file and stops recording process.
        """
        print "Cleaning up testing environment..."
        self._cleanupTestAutomation()
        print "clean up complete"

        if self._logger:
            self._logger.endLog()
        print "Test finished"
       
       
    def endTest(self):    
        print "Shutting down"
        self._stopTest()    
    

    def keywordInfo(self, kw ):
        kws = self._getKeywords()
        if kw in kws:
            print kw
            self.printKw(kw,"#",kws[kw][1])

    def printKw(self,kw,header,text):
        print header*len(kw)
        print
        docstring = text.splitlines()
        strip_len = 0
        if len(docstring[0]) == 0:
            docstring = docstring[1:]
        for line in docstring:
            if len(line.strip()) > 0:
                first_line = line.lstrip()
                strip_len = len(line) - len(first_line)
                break

        for line in docstring:
            print line[strip_len:].rstrip()
        print


    def listKeywords(self, basekw = keyword.Keyword,full=False,header="#"):
        kws = self._getKeywords({},basekw)
        kws_keys = sorted(kws.keys())
        for kw in kws_keys:
            print kw
            if full:
                self.printKw(kw,header,kws[kw][1])
    
    def _getKeywords(self, kw_dictionary = {}, basekw = keyword.Keyword):
        use_cache = len(kw_dictionary) == 0
        if use_cache and basekw in self._kw_cache:
            return self._kw_cache[basekw]

        for kw in basekw.__subclasses__():
            kw_name = str(kw)[str(kw).rfind('.')+1:str(kw).rfind("'")]
            if not kw_name.endswith("Keyword"):
                kw_dictionary[kw_name] = (str(kw.__module__),str(kw.__doc__))
            self._getKeywords(kw_dictionary,kw)
        if use_cache:
            self._kw_cache[basekw] = kw_dictionary
        return kw_dictionary    

    def __instantiateKeywordProxyObject(self,kwproxy, kwName,kwAttr,kwproxy_class):
        kwobject = None
        try:
            kwmodule = __import__(kwproxy_class, globals(), locals(), [kwproxy], -1)
#                    kwobject = eval("kwmodule." + kw + "()")
            kwobject = getattr(kwmodule,kwproxy)()
                                       
            if not kwobject.initialize(kwName, kwAttr,self._activeTarget):
                kwobject = None
            if kwobject:
                print 'Recognized keyword: %s' % kwName
                print 'Attributes: %s' % kwAttr
        except Exception, e:
            print e
            print "Error: KeywordProxy error"
            kwobject = None
        return kwobject
            
    def __instantiateKeywordObject(self,kw_name,attributes,kw_class):
        kwobject = None
        try:
            kwmodule = __import__(kw_class, globals(), locals(), [kw_name], -1)
#                    kwobject = eval("kwmodule." + kw + "()")
            kwobject = getattr(kwmodule,kw_name)()
                    
            print 'Recognized keyword: %s' % kw_name
            print 'Attributes: %s' % attributes
                    
            if not kwobject.initialize(attributes,self._activeTarget):
                print "Invalid parameters"
                kwobject = None
        except Exception, e:
            print e
            print "Error: Keyword not recognized!"
            kwobject = None
        return kwobject

    def _instantiateKeyword(self, kwName, kwAttr):
    
        kw_dictionary = self._getKeywords()
        kwproxy_dictionary = self._getKeywords({}, keywordproxy.KeywordProxy)
        kwobject = None
        
        for kw in kw_dictionary:    
            if kw.lower() == kwName.lower():         
                kwobject = self.__instantiateKeywordObject(kw,kwAttr,kw_dictionary[kw][0])
                break
        else:
            for kwproxy in kwproxy_dictionary:
                kwobject = self.__instantiateKeywordProxyObject(kwproxy, kwName,kwAttr,kwproxy_dictionary[kwproxy][0])
                if kwobject:
                    break
        if not kwobject:
            print "Error: Keyword not recognized!"

        return kwobject       
        
    def __executeKeyword(self, kw):
        """
        Executes a single keyword.
        
        Searches a corresponding keyword object from the list of keywords and executes the keyword with that object.
        
        @type kw: string
        @param kw: executed keyword
        
        @rtype: boolean or string
        @return: True if execution was succesfull; False if execution was succesdfull, but the keyword returned False;
            Error if there was problems in the execution.
        
        """
        
        print ""    
        print "Executing keyword: %s" % kw
        #Which keyword
        result = False
        
        kw = kw.strip()
        
        if kw.startswith("kw_"):
            kw = kw[3:].strip()

        # Testengine-note: generate-taskswitcher uses space as separator
        if kw.startswith("LaunchApp") or kw.startswith("SetTarget"):
            if not (kw.startswith("LaunchApp#") or kw.startswith("SetTarget#")):
                kw = kw.replace(" ",self._separator,1)

        kw_split = kw.split(self._separator,1)
        kwName = kw_split[0].strip()
        if len(kw_split) == 2:
            kwAttr = kw_split[1].strip()
        else:
            kwAttr = ""

        #Changing target
        if kwName.lower() ==  "settarget":
            result = self.__setTarget(kwAttr)
            print 'result: %s' % str(result)
            return result
        
        kwobject = self._instantiateKeyword(kwName,kwAttr)
        
        if not kwobject:
            return "ERROR"
        
        startTime = datetime.datetime.now()        
        result = kwobject.execute()       
        execTime = datetime.datetime.now() - startTime
        
        print 'result: %s' % str(result)
        
        kwDelay = kwobject.delay
        
        if kwDelay != -1:
        
            if self.delay > kwDelay:
                kwDelay = self.delay
            
            time.sleep(kwDelay) 
         
        if self._logger:
            
            self._logger.logKeyword(self._activeTarget, kwobject, result, str(execTime))
   
        self.kwCount = self._kwCount + 1 
        
        return result     
    

    def _handleSpecialCommands(self,command):
        return False
   
    #def _traverseApp(self, appname, device):
    def _traverseApp(self, folder, db, index):
        raise NotImplementedError

    def _processDB(self, path):
        raise NotImplementedError
 
    def runInteractive(self):
        """
        Runs an interactive test.
        
        Keywords are read from stdin.
        
        """
        # Only import here, so that we can use completion mechanism
        # Readline only available in unix
        try:
            import readline
            kws = self._getKeywords({}, keyword.Keyword).keys()
            for command_list in self._commands.values():
                kws.extend(command_list)
            readline.set_completer(AdapterCompleter(kws).complete)
            readline.parse_and_bind('tab: complete')
        except:
            pass    
        
        while True:
            try:
                kw = raw_input(">").strip()
                if kw in self._commands["exit"]:
                    return
                    
                elif kw == "":
                    continue

                kw_split = kw.split(" ")
                if kw_split[0] in self._commands["kws"]:
                    if len(kw_split) > 1 and kw_split[1]=="full" and " ".join(kw_split[0:2] )in self._commands["kws"]:
                        if len(kw_split) == 3:
                            char = kw_split[2]
                        else:
                            char = "#"
                        self.listKeywords(full=True,header=char)
                    else:
                        self.listKeywords(full=False)
                elif kw_split[0] in self._commands["info"] and len(kw_split) == 2:
                    self.keywordInfo(kw_split[1])
                elif not self._handleSpecialCommands(kw):
                    self.__executeKeyword(kw)

            except EOFError:
                break
                
    #SMA: App exploration
    #def runTraversal(self, appname, device):
    def runTraversal(self, folder, database, index, timeout=0):
        #if (appname is None or appname == ""):
        #    print "Please supply an application argument for traversal"
        #    return
        
        if (folder is None or database is None or index is None):
            print "Incorrect arguments supplied to runTraversal"
            return
            
        #print "Launching and exploring", appname, "app..."
        print "Delegating traversal to Android module..."

        self._traverseApp(folder, database, index, timeout)	

    #SMA: Process a database created through APP exploration
    def processDB(self, db):
        if (db is None or db == ""):
           print "Please supply appropriate database path argument"
           return

        print "Launching Traverse Database Processor..."
        self._processDB(db)
            
    def runFromServer(self, address, port, username = None, protocol= None ):
        """
        Runs a test from server.
        
        @type address: string
        @param address: Address of the server
        
        @type port: integer
        @param port: Port of the server
        
        @type username: string
        @param username: Username is required when using http or https protocol
        
        @type protocol: string
        @param protocol: Protocol that is used in the connection. Options are http and https. 
            Plain socketis used if parameter not given. 
        
        """ 

        toolProtocol = None 
        #while True:
        if(address != None and port != None): 
            if(protocol):
                base,path = address.split("/",1)
                toolProtocol = ToolProtocolHTTP()
                toolProtocol.init(base,path,port,username,protocol)
            else:    
                toolProtocol = ToolProtocol()
                toolProtocol.init(address,port)
                            
            if toolProtocol.hasConnection() == False:
                #print "Connection to the MBT server failed, reconnecting..."
                print "Connection to the MBT server failed."
            #    time.sleep(5)
                return
            #else:
            #    break
          
         
        while True:
            
            kw = ""
            #if passive:  
            #    kw = toolProtocol.receiveKeyword()
            #else:
            kw = toolProtocol.getKeyword()
            if (kw == '' or kw =='\n' or kw == "ERROR"):
                return
            
            result = self.__executeKeyword(kw)
            
            if(result == "ERROR"):
                toolProtocol.putResult(False)
                toolProtocol.bye()
                return
            
            toolProtocol.putResult(result)
            
            
    def runFromFile(self, fileName ):
        """      
        Runs a test from file.
        
        @type fileName: string
        @param fileName: path to the file that contains the test
        
        """
        try:
            with open(fileName,'r') as inputFile:
                for line in inputFile:
                    kw = line.strip()
                    if not kw:
                        break

                    result = self.__executeKeyword(kw)

                    if(result == "ERROR"):
                        break
        except IOError:
            print "Error when reading file: %s" % fileName
