/*
   Copyright 2014 Shahriyar Amini

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.cmuchimps.gort.modules.commander;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import org.cmuchimps.gort.api.gort.AdbService;
import org.cmuchimps.gort.api.gort.GortCommanderService;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shahriyar
 */
@ServiceProvider(service=GortCommanderService.class)
public class GortCommanderServiceProvider extends GortCommanderService {

    private static final int TRIES  = 3;
    
    private static final String HOST = "localhost";
    private static final String PROTOCOL = "tcp";
    private static final String PORT = "38300";
    private static final String PROTOCOL_PORT = String.format("%s:%s", PROTOCOL, PORT);
    
    //in ms
    private static final long DELAY_MS = 50;
    
    //commander errors
    private static final String INVALID = "invalid";
    private static final String UNRECOGNIZED_COMMAND = "unrecognized command";
    
    private static Socket socket;
    private static PrintWriter out;
    //private static InputStreamReader isr;
    private static Scanner in;
    
    private static boolean portForwarded = false;
    
    static {
        
        // Add a shutdown hook to close the socket if the system crashes or exists unexpectedly
        Thread closeSocketOnShutdown = new Thread() {
            @Override
            public void run() {
                GortCommanderServiceProvider.closeIO();
                GortCommanderServiceProvider.closeSocket();
            }
        };
        
        Runtime.getRuntime().addShutdownHook(closeSocketOnShutdown);
    }
    
    private static boolean setup() {
        
        forwardPort();
        
        boolean socketDown = (socket == null || socket.isClosed() || !socket.isConnected());
        
        if (socketDown) {
            closeIO();
            
            if (resetSocket()) {
                
                // setup input
                try {
                    in = new Scanner(socket.getInputStream());
                } catch (IOException ex) {
                    return false;
                }
                
                // set up output
                try {
                    out = new PrintWriter(socket.getOutputStream(), true);
                } catch (IOException ex) {
                    return false;
                }
                
            } else {
                return false;
            }
        }
        
        return true;
    }
    
    private static void forwardPort() {
        if (!portForwarded) {
            // Make sure the port is being forwarded correctly
            AdbService adbService = AdbService.getDefault();

            // forward tcp:38300 from host to device
            portForwarded = adbService.forward(PROTOCOL_PORT, PROTOCOL_PORT);
        }
    }
    
    private static boolean openSocket() {
        try {
            socket = new Socket(HOST, Integer.parseInt(PORT));
        } catch (UnknownHostException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
        
        System.out.println("Opened Gort Commander socket on port " + PORT + '.');
        
        return true;
    }
    
    private static void closeSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            
            socket = null;
        }
    }
    
    private static boolean resetSocket() {
        closeSocket();
        return openSocket();
    }
    
    private static void closeIO() {
        if (out != null) {
            out.close();
            out = null;
        }
        
        if (in != null) {
            in.close();
            in = null;
        }
        
        /*
        if (isr != null) {
            try {
                isr.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            isr = null;
        }*/
    }
    
    public static void close() {
        closeIO();
        closeSocket();
    }
    
    private static synchronized String sendCommand(String command) {
        if (command == null || command.isEmpty()) {
            return null;
        }
        
        if (!setup()) {
            System.out.println("Trouble connecting to Gort Commander on device.");
            return null;
        }
        
        int numTries = 0;
        
        while (numTries < TRIES) {
            
            numTries++;
            
            out.write(command);
            
            if (!command.endsWith("\r\n")) {
                out.write("\r\n");
            }
            
            out.flush();
            
            try {
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            
            if (in.hasNext()) {
                String result = in.next();
                
                if (result == null || result.isEmpty() ||
                        result.trim().toLowerCase().equals(INVALID) ||
                        result.trim().toLowerCase().equals(UNRECOGNIZED_COMMAND)) {
                    continue;
                }
                
                return result;
            }
        }
        
        return null;
    }
    
    @Override
    public String packageName(String appName) {
        return sendCommand(String.format("%s %s", GortCommanderService.COMMAND_PACKAGE, appName));
    }

    @Override
    public String processName(String packageName) {
        return sendCommand(String.format("%s %s", GortCommanderService.COMMAND_PROCESS, packageName));
    }

    @Override
    public String sourceDir(String packageName) {
        return sendCommand(String.format("%s %s", GortCommanderService.COMMAND_SOURCE_DIR, packageName));
    }

    @Override
    public String md5(String packageName) {
        return sendCommand(String.format("%s %s", GortCommanderService.COMMAND_MD5, packageName));
    }

    @Override
    public int fileSize(String packageName) {
        String result = sendCommand(String.format("%s %s", GortCommanderService.COMMAND_FILE_SIZE, packageName));
        try {
            int size = Integer.parseInt(result);
            return size;
        } catch (Exception ex) {
            return -1;
        }
    }
    
}
