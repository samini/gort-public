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
AdapterMain contains functionality for parsing general command line options
and starting a test based on the given options.

Example main program for adapter:

from ExampleAdapter.testrunner import TestRunner
from adapterlib.main import AdapterMain
if __name__ == "__main__":
    
    am = AdapterMain()
    options, args = am.parseArguments()
    if options:
        testRunner = TestRunner(args,options.delay,options.record,"lastrun.log")
        am.runAdapter(testRunner,options)
"""

import sys
from optparse import OptionParser

#SL
import traceback
class AdapterMain(object):

    def __init__(self):
    
        #Parse command line arguments
        parser = OptionParser(usage="%s [options] target\nUse -h for list of available options" % sys.argv[0])
        #SMA: Adding Traversal Option
        parser.add_option("-e", "--explore", action="store_true", default = False, dest="explore",
                          help="launch and explore/traverse an app")
        #SMA: Adding Process TraversalDB Option
        parser.add_option("-d", "--database", dest="db",
                          help="database to use for the traversal, e.g., " +
                          "postgresql://gort:@localhost:5432/gort-database-0")
        #                  "/Users/shahriyar/Traverser/app/traverser.db")
        parser.add_option("-n", "--app-index", dest="appindex",
                          help="app index in database, e.g., 1")
        #SMA: removing the device option. keyboard testing is done either using the added system
        #property (adb getprop keyboard.status) or based on OCR recognition
        #parser.add_option("-m", "--device-model", dest="device", default="crespo",
        #                  help="model of the app traversal device, e.g., crespo, maguro")
        #SMA: changing the file option to include a folder for the algorithm to use for traversals
        #parser.add_option("-f", "--file", dest="filename",
        #                  help="read keywords from FILENAME")
        parser.add_option("-f", "--folder", dest="folder",
                          help="store traversal files to FOLDER, e.g., " +
                          "~/NetBeansProjects/GortProject/traversals")
        parser.add_option("-m", "--minutes", type="int", dest="mins",
                          default=0, help="length of traversal in minutes.\n" +
                          "0 is no time limit. Default is 0.")
        parser.add_option("-i", "--interactive", dest="interactive", 
                          action="store_true", default = False,
                          help="read keywords from command line")
        parser.add_option("-t", "--time", dest="delay", default = "0",
                          help ="Delay (in seconds) between consecutive keywords")
        parser.add_option("-a", "--address", dest="address",
                          help ="MBT server address")
        parser.add_option("-p", "--port", dest="port", 
                          help ="MBT server port")
        parser.add_option("--protocol", dest="protocol", 
                          help ="HTTP or HTTPS. If not given, socket is used")
        #parser.add_option("--passive", dest="passive", default = False, action="store_true",
        #                 help ="Use MBT server in a passive mode => "+
        #                 "Server will send a keyword when there is on to execute")
        parser.add_option("-u","--username", dest="username", 
                          help ="MBT server username")
        parser.add_option("--record", dest="record",action="store_true", default = False, help="Records the test to a file")
        
        self.parser = parser
        
        
    def parseArguments(self):
            
        #Argument parsing...
        (options, args) = self.parser.parse_args()
        #SMA: db option does not require a serial #
        #if len(args) == 0:
        if not options.db and len(args) == 0:
            self.parser.error("incorrect number of arguments")
            return None
        else:
            #SMA: updated to also traverse apps
            #if options.interactive or options.filename:
            if options.explore:
                if not options.db:
                    self.parser.error("Exploring an app requires a database to write results.")
                    return None
                if not options.folder:
                    self.parser.error("Exploring an app requires a folder to store files.")
                    return None
                if not options.appindex:
                    self.parser.error("Exploring an app requires an app index in the db.")
                    return None
                if options.mins < 0:
                    self.parser.error("Traversal timeout should be non-negative.")
                    return None
            elif options.interactive or options.filename:
                pass
            elif options.address or options.port:
                if not options.address:
                    options.address = "localhost"
                if(options.port):
                    try:
                        options.port = int(options.port)
                        if options.port < 0 or options.port > 65535:
                            self.parser.error("Illegal port")
                            return None
                    except Exception:
                        self.parser.error("Illegal port")
                        return None
                else:
                    options.port = 9090
                if(options.protocol):
                    if not options.username:
                        self.parser.error("No username specified")
                if(options.username):
                    if not options.protocol:
                        self.parser.error("No protocol specified")                          
            else:
                self.parser.error("No mode specified")
                sys.exit(1)
            if(options.delay):
                try:
                    options.delay = float(options.delay)
                    if options.delay < 0:
                        self.parser.error("Illegal delay")
                        return None
                except Exception:
                    self.parser.error("Illegal delay")
                    return None
        #Argument parsing ends...
        return options, args
    
    def runAdapter(self, testrunner, options):
        # Following code has been changed. DB processing is now happens under the NetBeans side
        # SMA: if all we want is to process the traversal database
    	# do not really need to initialize testrunner
    	# traversal database processor is depended on testrunner
    	# which generated it, as such it is implemented as a testrunner function
    	#if options.db:
    	#	testrunner.processDB(options.db)
    	#	return 
        
        if not testrunner.initTest():
            return

        try:
            try:

                #SMA: Run app exploration
                if options.explore:
                    #testrunner.runTraversal(options.appname, options.device)
                    testrunner.runTraversal(options.folder, options.db, options.appindex, options.mins)

                #Run interactive test   
                elif options.interactive:
                    testrunner.runInteractive()

                #Run test from file
                elif(options.filename != None):
                    testrunner.runFromFile(options.filename)

                #Run test from server
                elif(options.address != None):
                    testrunner.runFromServer(options.address,options.port,options.username,options.protocol)

            except KeyboardInterrupt:
                pass

        #SL
        except Exception as e:
            print >> sys.stderr, 'Exception in runAdapter, exit'
            print >> sys.stderr, e
            traceback.print_exc(file=sys.stderr)

        finally:
            testrunner.endTest()
            sys.exit(0)
