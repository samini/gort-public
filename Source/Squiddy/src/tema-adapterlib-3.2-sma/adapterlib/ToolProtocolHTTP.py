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

import httplib
import urllib
import socket

class ToolProtocolHTTP(object):

    """
    HTTP/HTTPS client for TEMA MBT protocol. Discusses with the TEMA test engine.
    """

    # is client connected to the server
    isConnected = False

    
    def __init__(self):
        self.host = "localhost"
        self.port = 80
        self.php_file = "temagui_http_proxy.php"
        socket.setdefaulttimeout(1800)
                
    
    def __del__(self):
        if self.isConnected:
            http_params = urllib.urlencode({"User" : self.username, "Message" : 'CLOSE', "Parameter" : 'Empty'})
            http_data = self.__requestreply(http_params)
            
    
    def __requestreply(self,message ):
        """ One http(s) request/reply.

        Message: Message to send string.
        Returns: Reply string.
        """
        http_data = ''
        try:
            http_connection = None
            if self.protocol == "HTTP":
                                
                http_connection = httplib.HTTPConnection(self.host, self.port)
            elif self.protocol == "HTTPS":
                http_connection = httplib.HTTPSConnection(self.host, self.port)

            else:
                return ''

            http_connection.connect()
            http_connection.request("POST", self.php_file, message , self.http_headers)

            http_response = http_connection.getresponse()
            http_data = http_response.read()
            http_response.close()
            http_connection.close()
        
        except Exception, e:
            http_data = ''
            
        return http_data

    
    def init(self, host, path, port, username, protocol):
        """ Initialises connection. Sends HELO.
        
        host: Server hostname.
        path: path to http proxy in server.
        port: port
        username: wwwgui username
        protocol: http/https
        returns: Reply to ACK. On error returns ''
        """

                
        self.http_headers = {"Content-type": "application/x-www-form-urlencoded", "Accept": "text/plain"}
        
        self.host = host
        self.php_file = "/".join(["",path,"temagui_http_proxy.php"])
        
        self.port = port
        self.username = username
        self.protocol = protocol.upper()
        
        try:
            # SEND HELO
            http_params = urllib.urlencode({"User" : username, "Message" : 'HELO', "Parameter" : 'Empty'})

            http_data = self.__requestreply(http_params)
                
            
            self.isConnected = True
            
            lines = http_data.splitlines()
            if lines != []:
                message = lines.pop()
            
                if message == "CLOSE":
                    http_data = ''
                    self.isConnected = False
            
        except Exception, e:
            self.isConnected = False
            return ''
        
        return http_data
            
        
    def getKeyword(self):
        """ Gets keyword from testserver.
        
        Sends GET to testserver and waits for reply.
        Returns: Reply to GET. On error return ''
        """
        http_data = ''
        try:
    
            http_params = urllib.urlencode({"User" : self.username, "Message" : 'GET', "Parameter" : 'Empty'})
            
            http_data = self.__requestreply(http_params)
            
            lines = http_data.splitlines()
            if lines != []:
                message = lines.pop()
                
                if message == "CLOSE":
                    self.isConnected = False
                    return 'ERROR'
                if message == 'ERR':

                    # TODO: don't send ack. 
                    http_data = self.__requestreply(http_params)
                                
                    http_params = urllib.urlencode({"User" : self.username, "Message" : 'ACK', "Parameter" : 'Empty'})
                
                    http_data = self.__requestreply(http_params)
                
                    self.isConnected = False
                    return 'ERROR'
                
            if not http_data.startswith("ACK"):
                print http_data
                return "ERROR"
            else:
                #http_data = http_data.partition("ACK")[2].strip()
                http_data = http_data.split("ACK")[1].strip()
                
            if http_data == '' or http_data == None:
                http_data = ''
                self.isConnected = False
                
        except Exception, e:
            self.isConnected = False
        
        return http_data

    def putResult(self, result):
        """ Puts result to testserver.
        
        result: True/False
        returns: Reply message to PUT
        """

        try:
            if result:
                http_params = urllib.urlencode({"User" : self.username, "Message" : 'PUT', "Parameter" : 'true'})
            else:
                http_params = urllib.urlencode({"User" : self.username, "Message" : 'PUT', "Parameter" : 'false'})
        except Exception, e:
            self.isConnected = False
            return ''
        
        try:
            http_data = self.__requestreply(http_params)
            
            lines = http_data.splitlines()
            if lines != []:
                message = lines.pop()
                if message == "CLOSE":
                    self.isConnected = False
                    return ''

            if http_data == '':
                self.isConnected = False
        except Exception, e:
            self.isConnected = False
            http_data = ''
            
        return http_data

    def log(self, msg):
        """ Sends log message to testserver
        
        returns: Reply to message.
        """
        
        http_data = ''
        try:
            http_params = urllib.urlencode({"User" : self.username, "Message" : 'LOG', "Parameter" : msg })
        
            http_data = self.__requestreply(http_params)
            lines = http_data.splitlines()
            if lines != []:
                message = lines.pop()
                if message == "CLOSE":
                    self.isConnected = False
                    return ''
            
            
            if http_data == '':
                self.isConnected = False
        except Exception, e:
            self.isConnected = False
            http_data = ''

        return http_data
        
    def bye(self):
        """ Sends message BYE to testserver. """
                
        http_data = ''
        try:
            http_params = urllib.urlencode({"User" : self.username, "Message" : 'BYE', "Parameter" : 'None'})
        
            
            http_data = self.__requestreply(http_params)
            self.isConnected = False
        except Exception, e:
            self.isConnected = False
         
        return ''

    def hasConnection(self):
        return self.isConnected

if __name__ == "__main__":
    c = ToolProtocol()
    print "init -> " + c.init()
    print "getKeyword -> " + c.getKeyword()
    print "putResult -> " + c.putResult(True)
    print "getKeyword -> " + c.getKeyword()
    print "putResult -> " + c.putResult(False)
    print "invalid -> " + c.invalid()
    print "bye -> " + c.bye()
