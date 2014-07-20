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

import java.util.HashSet;
import java.util.Set;
import org.cmuchimps.gort.api.gort.ExternalProcessService;
import org.openide.modules.ModuleInstall;
import org.openide.util.RequestProcessor;

public class Installer extends ModuleInstall implements Runnable {

    private static final String PATH_INFO = "The External Process module can only call " +
            "executables that are part of your path. Make sure all necessary " +
            "executables are available";
    private static final String PATH_VARIABLE = "PATH";
    
    private static final Set<Process> processes = new HashSet<Process>();
    
    @Override
    public void restored() {
        printPathInfo();
    }
    
    public void printPathInfo() {
        System.out.println(PATH_INFO);
        System.out.println("Your environmental PATH variable is: " + System.getenv(PATH_VARIABLE));
    }
    
    @Override
    public void close() {
        if (processes == null) {
            return;
        }
        
        synchronized (processes) {
            for (Process p : processes) {
                if (p == null) {
                    continue;
                }
                
                try{
                    p.destroy();
                } catch (Exception e) {
                    System.out.println("Could not destory process: " + p);
                }
            }
        }
        
        super.close();
    }
    
    public static void addProcess(Process p) {
        synchronized(processes) {
            processes.add(p);
        }
    }
    
    public static void removeProcess(Process p) {
        synchronized(processes) {
            processes.remove(p);
        }
    }

    ExternalProcessService service = ExternalProcessService.getDefault();
    RequestProcessor rp = new RequestProcessor(Installer.class);
    RequestProcessor.Task task = rp.create(this);
            
    private void test() {   
        task.schedule(10000);
    }

    @Override
    public void run() {
        Thread t = new Thread() {
            public void run() {
                service.output("date");
            }
        };
        
        t.start();
        
        task.schedule(10000);
    }
}
