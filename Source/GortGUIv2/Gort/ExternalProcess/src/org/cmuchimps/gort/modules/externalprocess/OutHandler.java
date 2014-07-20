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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import org.openide.util.Exceptions;
import org.openide.windows.OutputWriter;

/**
 *
 * @author shahriyar
 */
public class OutHandler implements Runnable {
    private BufferedReader out;

    private OutputWriter writer;
    
    // attempts to save the entire output.
    
    private static int CHAR_BUFFER_CAPACITY = 4096;
    
    private ByteArrayOutputStream baos;
    private PrintWriter result;
    
    private final boolean storeOutput;
    
    private CountDownLatch latch;
    
    public OutHandler(BufferedReader out, OutputWriter writer, boolean storeOutput, CountDownLatch latch) {
        this.storeOutput = storeOutput;
        this.out = out;
        this.writer = writer;
        this.latch = latch;
        
        if (this.storeOutput) {
            baos = new ByteArrayOutputStream();
            result = new PrintWriter(baos);
        }
        
    }
    
    public OutHandler(BufferedReader out, OutputWriter writer, CountDownLatch latch) {
        this(out, writer, false, latch);
    }

    /*
    @Override
    public void run() {
        System.out.println("Outhanler run called");
        while (true) {
            try {
                while (!out.ready()) {
                    try {
                        Thread.currentThread().sleep(200);
                        System.out.println("Stream not ready. Sleeping");
                    } catch (InterruptedException e) {
                        System.out.printf("InterruptedException. Closing OutHandler");
                        e.printStackTrace();;
                        close();
                        return;
                    }
                }
                
                if (Thread.currentThread().isInterrupted()) {
                    System.out.printf("Could not read or thread interrupted. Closing OutHandler");
                    close();
                    return;
                }
                
                readBuffer();
                
                return;
                
            } catch (IOException ioe) {
                //Stream already closed, this is fine
                System.out.println("Stream closed. Returning");
                ioe.printStackTrace();
                return;
            }
        }
    }*/
    
    @Override
    public void run() {
        try {

            if (Thread.currentThread().isInterrupted()) {
                System.out.printf("Could not read or thread interrupted. Closing OutHandler");
            } else {
                readBuffer();
                //resultReady = true;
            }

        } catch (IOException ioe) {
            System.out.println("Stream closed. Returning");
            ioe.printStackTrace();
        } finally {
            close();
        }
        
        latch.countDown();
    }

    private void readLine() throws IOException {
        String line = out.readLine();
        
        if (line == null) {
            return;
        }
        
        System.out.println(line);
        
        writer.write(line);
        writer.flush();
        
        if (storeOutput) {
            result.append(line);
        }     
    }
    
    private void readBuffer() throws IOException {
        
        char[] cbuf = new char[255];
        int read;
        
        while ((read = out.read(cbuf)) != -1) {

            if (storeOutput) {
                //result.append(new String(cbuf, 0, read));
                result.write(cbuf, 0, read);
            }
            
            writer.write(cbuf, 0, read);
        }
        
        if (storeOutput) {
            result.flush();
        }
        
        writer.flush();
        
    }
    
    public String checkOutput() {
        if (this.storeOutput) {
            return baos.toString();
        }
        
        return null;
    }

    public void close() {
        try {
            out.close();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            writer.close();
        }
    }
}
