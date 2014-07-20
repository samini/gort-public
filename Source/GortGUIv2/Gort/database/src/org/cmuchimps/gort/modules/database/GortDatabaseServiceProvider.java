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

import java.util.Properties;
import org.cmuchimps.gort.api.gort.ExternalProcessService;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.netbeans.api.project.Project;

/**
 *
 * @author shahriyar
 */
//@ServiceProvider(service=GortDatabaseService.class)
public final class GortDatabaseServiceProvider implements GortDatabaseService {
    
    // functions in this class are best effort. if the running user does
    // not have permissions or the command does not work, at this point
    // we just ignore and continue. Admin can create and setup the gort user
    // with PostgreSQL manually.
    
    // the Gort database uses PostgreSQL, we therefore, require that PostgreSQL
    // be installed. A user named 'gort' should be added to PostgresSQL with 
    // no password. The user 'gort' should have the right to create
    // databases but does not need to be a super user
    
    private static final String DB_NAME_PREFIX = "gort-";
    
    private static String EXECUTE_CREATEDB = "createdb -h " + GortDatabaseService.HOST + 
            " -U " + GortDatabaseService.USERNAME + ' ';
    
    private static String EXECUTE_DROPDB = "dropdb -h " + GortDatabaseService.HOST + 
            " -U " + GortDatabaseService.USERNAME + ' ';
    
    // Lookup key to get the unique identifier for the project
    private static final String KEY_UUID = "uuid";
    
    private final Project project;
    
    private GortEntityManager gem = null;
    
    public GortDatabaseServiceProvider(Project project) {
        this.project = project;
        
        /*
        System.out.println("Project database name: " + getDatabaseName());
        
        if (createGortDatabase()) {
            System.out.println("Project database successfully created.");
        }*/
    }

    @Override
    public String dbName() {
        //Properties properties = (Properties) proj.getLookup().lookup(Properties.class);
        Properties props = (Properties) project.getLookup().lookup(Properties.class);
        String uuid = props.getProperty(KEY_UUID);

        if (uuid == null || uuid.isEmpty()) {
            System.out.println("uuid property not in lookup.");
            return null;
        }
        
        return DB_NAME_PREFIX + uuid;
    }
    
    @Override
    public String dbConnectionURL() {
        String name = dbName();
        
        if (name == null) {
            return null;
        }
        
        //example connection url postgresql://gort:@localhost:5432/gort-325146ca-3dc4-44aa-a7bd-fb8acf5e250b
        String url = String.format("postgresql://%s:@%s:%s/%s", GortDatabaseService.USERNAME, 
                GortDatabaseService.HOST, GortDatabaseService.PORT, name);
        return url;
    }

    @Override
    public boolean createGortDatabase() {
        return createDb(dbName());
    }

    private static boolean executeOnDb(String command, String dbName) {
        if (dbName == null || dbName.length() <= 0) {
            return false;
        }
        
        ExternalProcessService eps = ExternalProcessService.getDefault();
        // only returns a non-null output if the command was successful
        String result = eps.output(command + dbName);
        return (result != null && result.length() > 0);
        
        /*
        try {
            Process p = Runtime.getRuntime().exec(command + dbName);
            int exitCode = p.waitFor();
            return (0 == exitCode);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);   
        } catch (IOException ex) {
            System.out.println("Was not able to process command on database.");
            Exceptions.printStackTrace(ex);
        }
        
        return false;
        */
    }
    
    private static boolean dropDb(String dbName) {
        return executeOnDb(EXECUTE_DROPDB, dbName);
    }
    
    private static boolean createDb(String dbName) {
        return executeOnDb(EXECUTE_CREATEDB, dbName);
    }

    @Override
    public synchronized GortEntityManager getGortEntityManager() {
        if (gem == null) {
            gem = new GortEntityManager(dbName());
        }
        
        return gem;
    }

    /*
    @Override
    public boolean persistObject(Object o) {
        GortEntityManager g = getGortEntityManager();
        EntityManager em = g.getEntityManager();
                
        return false;
    }*/
    
}
