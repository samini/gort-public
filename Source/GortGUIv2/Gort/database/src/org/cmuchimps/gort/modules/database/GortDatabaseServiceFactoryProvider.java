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
package org.cmuchimps.gort.modules.database;

import org.cmuchimps.gort.api.gort.ExternalProcessService;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.GortDatabaseServiceFactory;
import org.netbeans.api.project.Project;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shahriyar
 */
@ServiceProvider(service=GortDatabaseServiceFactory.class)
public class GortDatabaseServiceFactoryProvider extends GortDatabaseServiceFactory {

    private static String EXECUTE_CREATEUSER = "createuser -h " + GortDatabaseService.HOST + 
            " --createdb --no-superuser --no-createrole " + GortDatabaseService.USERNAME;
    
    @Override
    public GortDatabaseService getInstance(Project project) {
        return new GortDatabaseServiceProvider(project);
    }
    
    @Override
    public boolean createGortUser() {
        
        ExternalProcessService eps = ExternalProcessService.getDefault();
        // only returns a non-null output if the command was successful
        String result = eps.output(EXECUTE_CREATEUSER);
        return (result != null && result.length() > 0);
        
        /*
        try {
            Process p = Runtime.getRuntime().exec(EXECUTE_CREATEUSER);
            int exitCode = p.waitFor();
            return (0 == exitCode);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            System.out.println("Attempted to create database user gort.");
            System.out.println("Attempt failed. Assuming user already created");
            Exceptions.printStackTrace(ex);
        }
        
        return false;
        */
    }
    
}
