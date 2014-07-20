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
package org.cmuchimps.gort.modules.externalprocess;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import org.cmuchimps.gort.api.gort.ExternalProcessService;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author shahriyar
 */
@ServiceProvider(service=ExternalProcessService.class)
public class ExternalProcessServiceProvider extends ExternalProcessService {
    
    RequestProcessor requestProcessor;
    
    private static final int REQUEST_PROCESSOR_THREADS = 10;
    
    public ExternalProcessServiceProvider() {
        requestProcessor = new RequestProcessor(
                ExternalProcessService.class.getSimpleName(), REQUEST_PROCESSOR_THREADS);
    }
    
    /*
    @Override
    public void setLabel(String label) {
        this.label = label;
    }*/

    @Override
    public void exec(String command) {
        if (command == null || command.isEmpty()) {
            return;
        }
        
        try {
            final Process process = Runtime.getRuntime().exec(command);
            Installer.addProcess(process);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    // Does not track the process since we are expecting it to finish
    public int exitCode(String command) {
        int exitCode = -1;
        
        if (command == null || command.isEmpty()) {
            return exitCode;
        }
        
        Process process = null;
        
        try {
            process = Runtime.getRuntime().exec(command);
            exitCode = process.waitFor();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);   
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        
        return exitCode;
    }
    
    @Override
    public String output(String command) {
        return output(command, ExternalProcessService.EXTERNAL_PROCESS_WINDOW_LABEL);
    }
    
    @Override
    public String output(String command, String windowLabel) {
        if (command == null || command.isEmpty()) {
            return null;
        }
        
        String retVal = null;
        
        Process process = null;
        OutHandler processSystemOut = null, processSystemErr = null;
        
        try {
            
            //Getting the output window may be slow if netbeans is just starting
            //so printing to output window is optional just so we don't slow down
            //loading.
            
            //Get an InputOutput to write to the output window
            //newIO - if true, a new InputOutput is returned, 
            //else an existing InputOutput of the same name may be returned
            boolean newIO = false;
            InputOutput io = IOProvider.getDefault().getIO(windowLabel, newIO);

            //Force it to open the output window/activate our tab
            //io.select();

            //Print the command line we're calling for debug purposes
            io.getOut().println(command);
            
            // call the process
            process = Runtime.getRuntime().exec(command);
            
            // keep track of open processes
            Installer.addProcess(process);
            
            //Get the standard out of the process
            InputStream out = new BufferedInputStream(process.getInputStream(), 8192);
            
            //Get the standard in of the process
            InputStream err = new BufferedInputStream(process.getErrorStream(), 8192);
            
            //Create readers for each
            final BufferedReader outReader = new BufferedReader(new InputStreamReader(out));
            final BufferedReader errReader = new BufferedReader(new InputStreamReader(err));
            
            // Create a count down latch for processing the out and err streams
            CountDownLatch latch = new CountDownLatch(2);
            
            //Create runnables to poll each output stream
            processSystemOut = new OutHandler(outReader, io.getOut(), true, latch);
            processSystemErr = new OutHandler(errReader, io.getErr(), latch);
            
            //Get two different threads listening on the output & err
            //using the system-wide thread pool
            //RequestProcessor.getDefault().post(processSystemOut);
            //RequestProcessor.getDefault().post(processSystemErr);
            requestProcessor.post(processSystemOut);
            requestProcessor.post(processSystemErr);
            
            try {
                //Hang this thread until the process exits
                process.waitFor();
                latch.await();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            
            // only return the output if we have a successful exit code
            if (process.exitValue() == 0) {
                //io.getOut().println("Process exited successfully.");
                
                retVal = processSystemOut.checkOutput();
                
                System.out.println(retVal);
                
                if (retVal == null || retVal.isEmpty()) {
                    retVal = "SUCCESS";
                }
            } else {
                //io.getErr().println("Process failed.");
                retVal = null;
            }
            
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            
        } finally {
            
            // OutHandler closes all the streams;
            
            /*
            try {
                // Wait before closing the streams
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            
            //Close the output window's streams (title will become non-bold)
            if (processSystemOut != null) {
                processSystemOut.close();
            }
            
            if (processSystemErr != null) {
                processSystemErr.close();
            }
            
            if (process != null) {
                try {
                    // close the process streams
                    process.getErrorStream().close();
                    process.getInputStream().close();
                    process.getOutputStream().close();
                } catch (IOException ex) {
                    //ignore
                }
            }*/
            
            // if the process is still alive kill it and remove it from the list
            if (process != null) {
                try {
                    process.destroy();
                    Installer.removeProcess(process);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        
        System.out.println("Process return value: " + retVal);
        
        return retVal;
    }
    
}
