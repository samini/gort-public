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
package org.cmuchimps.gort.modules.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author shahriyar
 */
public class HTTPRequestHelper {
    
    public static final int TRIES = 3;
    
    public static String HTTPRequest(String addr) {
        if (addr == null) {
            return null;
        }
        
        int tries = 0;
        
        String retVal;
        
        while (tries < TRIES) {
            tries++;
            retVal = HTTPRequestNoRetry(addr);
            if (retVal != null) {
                return retVal;
            }
        }
        
        return null;
    }
    
    public static String HTTPRequestNoRetry(String addr) {
        if (addr == null) {
            return null;
        }
        
        URL url;
        try {
            url = new URL(addr);
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);  
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            
            InputStream in = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);
            
            BufferedReader rd = new BufferedReader(isr);
            StringBuilder s = new StringBuilder();
            
            String line;
            
            while ((line = rd.readLine()) != null)
            {
                s.append(line);
                //System.out.println(line);
            }
            
            rd.close();
            isr.close();
            in.close();

            return s.toString();

            
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        
        return null;
    }
}
