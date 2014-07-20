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

import socket

class ToolProtocol(object):

    """
    Socket client for TEMA MBT protocol. Discusses with the TEMA test engine.
    """

    TEMAPORT = 9090 # test engine default port
    TEMAHOST = "localhost"

    # is client connected to the server
    isConnected = False
    if isConnected:
        isConnected = True
    # connection socket
    #s = None
    
    def __init__(self):
        self.host = ToolProtocol.TEMAHOST
        self.port = ToolProtocol.TEMAPORT
        self.received_data = []

    def init(self, host, port):
        data = ''
        try:
            self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.s.connect( (host, port) )

            sent = self.s.send("HELO\n")
            if sent == 0:
                data = ''
                return data
            data = self.s.recv(1024)
            if data == '':
                return data
            self.isConnected = True
        except socket.error,msg:
            self.isConnected = False
        return data

    def getKeyword(self):
        data = ''
        try:
            while True:
                sent = self.s.send("GET\n")
                if sent == 0:
                    data = ''
                    self.isConnected = False
                    self.s.close()
                    return "ERROR"
                
                data = self.s.recv(1024)
                lines = data.splitlines()
                if data.startswith("ACK"):
                    data = data.partition("ACK")[2].strip()
                    return data
                if data.startswith("BYE"):

                    self.s.send("ACK\n")
                    self.isConnected = False
                    self.s.close()
                    return "ERROR"
        except socket.error,msg:
            self.isConnected = False
            self.s.close()
            return "ERROR"      

    def putResult(self, result):
        data = ''
        if result:
            sendData = "PUT true\n"
        else:
            sendData = "PUT false\n"
        
        try:
            sent = self.s.send( sendData )
            if sent == 0:
                data = ''
                self.isConnected = False
                return data

            data = self.s.recv(1024)
            if data == '':
                self.isConnected = False
        except socket.error, msg:
            self.isConnected = False
            
        return data

    def log(self, msg):
        data = 'LOG %s\n' % (msg,)
        try:
            sent = self.s.send( data )
            if sent == 0:
                data = ''
                self.isConnected = False
                return data
            
            data = self.s.recv(1024)
            if data == '':
                self.isConnected = False
        except socket.error, msg:
            self.isConnected = False
        except UnicodeEncodeError, msg:
            pass
        return data
        
    def bye(self):
        data = ''
        if(self.isConnected):
            while True:
                try:
                    sent = self.s.send( "BYE\n" )
                    if sent == 0:
                        data = ''
                        self.isConnected = False
                        self.s.close()
                        break                       
                    data = self.s.recv(1024)
                    if data.startswith("ACK"):
                        self.isConnected = False
                        self.s.close()
                        break
                    
                except socket.error, msg:
                    self.isConnected = False
                    self.s.close()
                    break
                    
    def hasConnection(self):
        return self.isConnected


    def receiveKeyword(self):
                    
        print "Waiting keyword..."  
        try:
            while True:     
                data = self.s.recv(1024)
                lines = data.splitlines()
                
                if data.startswith("ACK"):
                    data = data.partition("ACK")[2].strip()
                    return data
                
                if data.startswith("BYE"):
                    self.s.send("ACK\n")
                    self.isConnected = False
                    self.s.close()
                    return "ERROR"
                
        except socket.error,msg:
            self.isConnected = False
            self.s.close()
            return "ERROR"      

if __name__ == "__main__":
    c = ToolProtocol()
    print "init -> " + c.init()
    print "getKeyword -> " + c.getKeyword()
    print "putResult -> " + c.putResult(True)
    print "getKeyword -> " + c.getKeyword()
    print "putResult -> " + c.putResult(False)
    print "invalid -> " + c.invalid()
    print "bye -> " + c.bye()
