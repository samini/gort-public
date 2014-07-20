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
package org.cmuchimps.gort.modules.webinfoservice;

import org.cmuchimps.gort.modules.dataobject.WhoisRecord;
import org.cmuchimps.gort.modules.helper.HTTPRequestHelper;

/**
 *
 * @author shahriyar
 */
public class WhoisService {
    enum RequestType {JSON, CSV, XML};
    
    private static String url = "http://adam.kahtava.com/services/whois.^?query=";
    
    public static String whoisRequest(String addr) {
        RequestType type = RequestType.JSON;
        return whoisRequest(addr, type);
    }
    
    public static String whoisRequest(String addr, RequestType type) {
        if (addr == null)
            return null;
        
        StringBuilder sb = new StringBuilder();
        sb.append(url.replace("^", type.name().toLowerCase()));
        sb.append(addr);

        return HTTPRequestHelper.HTTPRequest(sb.toString());
    }
    
    public static WhoisRecord whois(String addr) {
        if (addr == null || addr.isEmpty()) {
            return null;
        }
        
        String json = whoisRequest(addr);
        
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        return WhoisRecord.fromJson(json);
    }
}
