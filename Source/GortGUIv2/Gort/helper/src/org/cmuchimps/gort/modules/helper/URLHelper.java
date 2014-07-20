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

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author shahriyar
 */
public class URLHelper {
    // only returns the host name if there is a successful lookup
    // otherwise returns the same ip
    public static String getHost(String s) {
        if (s == null) {
            return null;
        }
        
        try {
            InetAddress address = InetAddress.getByName(s);
            return address.getHostName();
        } catch (UnknownHostException ex) {
            System.out.println("Could not find server hostname " + s);
            ex.printStackTrace();
        }

        return s;
    }
}
