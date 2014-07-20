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
package org.cmuchimps.gort.modules.dataobject;

import com.google.gson.Gson;

/**
 *
 * @author shahriyar
 */
public class WhoisRecord {
    
    public String DomainName;
    
    public RegistryData RegistryData;
    
    public static class RegistryData {
        
        public Registrant Registrant;
        
        public static class Registrant {
            public String Address;
            public String City;
            public String Country;
            public String Name;
            public String PostalCode;
            public String StateProv;

            @Override
            public String toString() {
                return "Registrant{" + "Address=" + Address +
                        ", City=" + City + ", Country=" + Country +
                        ", Name=" + Name + ", PostalCode=" +
                        PostalCode + ", StateProv=" + StateProv + '}';
            }
        }
        
        @Override
        public String toString() {
            return "RegistryData{" + "Registrant=" + Registrant.toString() + '}';
        }
    }
    
    public static WhoisRecord fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, WhoisRecord.class);
    };

    @Override
    public String toString() {
        return "WhoisRecord{" + "RegistryData=" + RegistryData.toString() + '}';
    }
    
}
