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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author shahriyar
 * 
 * Contains the mapping associated with Taints.
 * 
 */
public final class TaintHelper {
    public static final int TAINT_NONE          = 0x00000000;
    public static final int TAINT_LOCATION      = 0x00000001;
    public static final int TAINT_CONTACTS      = 0x00000002;
    public static final int TAINT_MIC           = 0x00000004;
    public static final int TAINT_PHONE_NUMBER  = 0x00000008;
    public static final int TAINT_LOCATION_GPS  = 0x00000010;
    public static final int TAINT_LOCATION_NET  = 0x00000020;
    public static final int TAINT_LOCATION_LAST = 0x00000040;
    public static final int TAINT_CAMERA        = 0x00000080;
    public static final int TAINT_ACCELEROMETER = 0x00000100;
    public static final int TAINT_SMS           = 0x00000200;
    public static final int TAINT_IMEI          = 0x00000400;
    public static final int TAINT_IMSI          = 0x00000800;
    public static final int TAINT_ICCID         = 0x00001000;
    public static final int TAINT_DEVICE_SN     = 0x00002000;
    public static final int TAINT_ACCOUNT       = 0x00004000;
    public static final int TAINT_HISTORY       = 0x00008000;
    
    // These are based on TaintDroid version 2.3 types
    public static final String TYPE_OS_NETWORK  = "OSNetworkSystem";
    public static final String TYPE_SSL_OUTPUT  = "SSLOutputStream";
    public static final String TYPE_OS_FILE_SYSTEM  = "OSFileSystem";
    public static final String TYPE_LIBCORE_OS_SEND = "libcore.os.send";
    
    public static final Map<Integer, String> DEFINITIONS, CROWD_DEFINITIONS;
    public static final Map<Integer, String[]> PERMISSIONS;
    
    static {
        DEFINITIONS = new HashMap<Integer, String>();
        DEFINITIONS.put(TAINT_LOCATION, "location");
        DEFINITIONS.put(TAINT_CONTACTS, "contacts");
        DEFINITIONS.put(TAINT_MIC, "audio recording");
        DEFINITIONS.put(TAINT_PHONE_NUMBER, "phone number");
        DEFINITIONS.put(TAINT_LOCATION_GPS, "GPS location");
        DEFINITIONS.put(TAINT_LOCATION_NET, "network-based location");
        DEFINITIONS.put(TAINT_LOCATION_LAST, "last known location");
        DEFINITIONS.put(TAINT_CAMERA, "camera roll media");
        DEFINITIONS.put(TAINT_ACCELEROMETER, "accelerometer data");
        DEFINITIONS.put(TAINT_SMS, "SMS data");
        //DEFINITIONS.put(TAINT_IMEI, "International Mobile Equipment Identity");
        DEFINITIONS.put(TAINT_IMEI, "unique device identifier (IMEI)");
        DEFINITIONS.put(TAINT_IMSI, "unique device identifier (IMSI)");
        DEFINITIONS.put(TAINT_ICCID, "unique device identifier (SIM)");
        DEFINITIONS.put(TAINT_DEVICE_SN, "device serial number");
        DEFINITIONS.put(TAINT_ACCOUNT, "account information");
        DEFINITIONS.put(TAINT_HISTORY, "history information (e.g., browser history)");
        
        PERMISSIONS = new HashMap<Integer, String[]>();
        // Not needed for location, either GPS or NET will be selected
        //PERMISSIONS.put(TAINT_LOCATION, );
        PERMISSIONS.put(TAINT_CONTACTS, new String[]{AndroidPermissions.READ_CONTACTS});
        PERMISSIONS.put(TAINT_MIC, new String[]{});
        PERMISSIONS.put(TAINT_PHONE_NUMBER, new String[]{AndroidPermissions.READ_PHONE_STATE, AndroidPermissions.GET_ACCOUNTS});
        PERMISSIONS.put(TAINT_LOCATION_GPS, new String[]{AndroidPermissions.ACCESS_FINE_LOCATION});
        PERMISSIONS.put(TAINT_LOCATION_NET, new String[]{AndroidPermissions.ACCESS_COARSE_LOCATION});
        PERMISSIONS.put(TAINT_LOCATION_LAST, new String[]{});
        PERMISSIONS.put(TAINT_CAMERA, new String[]{});
        PERMISSIONS.put(TAINT_ACCELEROMETER, new String[]{});
        PERMISSIONS.put(TAINT_SMS, new String[]{AndroidPermissions.READ_SMS,
                    AndroidPermissions.WRITE_SMS,
                    AndroidPermissions.RECEIVE_SMS,
                    AndroidPermissions.SEND_SMS,
                    AndroidPermissions.RECEIVE_MMS,
                    AndroidPermissions.BROADCAST_SMS,
        });
        //PERMISSIONS.put(TAINT_IMEI, "International Mobile Equipment Identity");
        PERMISSIONS.put(TAINT_IMEI, new String[]{AndroidPermissions.READ_PHONE_STATE});
        PERMISSIONS.put(TAINT_IMSI, new String[]{AndroidPermissions.READ_PHONE_STATE});
        PERMISSIONS.put(TAINT_ICCID, new String[]{AndroidPermissions.READ_PHONE_STATE});
        PERMISSIONS.put(TAINT_DEVICE_SN, new String[]{AndroidPermissions.READ_PHONE_STATE});
        PERMISSIONS.put(TAINT_ACCOUNT, new String[]{AndroidPermissions.GET_ACCOUNTS,
                    AndroidPermissions.ACCOUNT_MANAGER,
                    AndroidPermissions.MANAGE_ACCOUNTS,
                    AndroidPermissions.USE_CREDENTIALS,
        });
        PERMISSIONS.put(TAINT_HISTORY, new String[]{});
        
        // crowd definitions
        CROWD_DEFINITIONS = new HashMap<Integer, String>();
        // should be covered by other location tags
        //CROWD_DEFINITIONS.put(TAINT_LOCATION, "location");
        CROWD_DEFINITIONS.put(TAINT_CONTACTS, "contacts");
        CROWD_DEFINITIONS.put(TAINT_MIC, "microphone audio data");
        CROWD_DEFINITIONS.put(TAINT_PHONE_NUMBER, "phone number");
        CROWD_DEFINITIONS.put(TAINT_LOCATION_GPS, "exact location");
        CROWD_DEFINITIONS.put(TAINT_LOCATION_NET, "approximate location");
        CROWD_DEFINITIONS.put(TAINT_LOCATION_LAST, "last known location");
        CROWD_DEFINITIONS.put(TAINT_CAMERA, "photos");
        CROWD_DEFINITIONS.put(TAINT_ACCELEROMETER, "motion data");
        CROWD_DEFINITIONS.put(TAINT_SMS, "text message data");
        //DEFINITIONS.put(TAINT_IMEI, "International Mobile Equipment Identity");
        CROWD_DEFINITIONS.put(TAINT_IMEI, "unique device identifier");
        CROWD_DEFINITIONS.put(TAINT_IMSI, "unique device identifier");
        CROWD_DEFINITIONS.put(TAINT_ICCID, "unique device identifier");
        CROWD_DEFINITIONS.put(TAINT_DEVICE_SN, "device serial number");
        CROWD_DEFINITIONS.put(TAINT_ACCOUNT, "account information (for example: google maps or facebook account information)");
        CROWD_DEFINITIONS.put(TAINT_HISTORY, "history information (for example: browser history)");
    }
    
    // takes in a definition and outputs the associated taint constant
    public static int taintFromDefinition(String definition) {
        if (definition == null || definition.isEmpty()) {
            return 0;
        }
        
        Set<Entry<Integer, String>> set = DEFINITIONS.entrySet();
        
        for (Entry<Integer,String> e : set) {
            if (e.getValue().equals(definition))
                return e.getKey();
        }
        
        return 0;
    }
    
    // output all individual taints in an integer tag
    public static int[] taintsFromTag(int tag) {
        
        if (tag == TAINT_NONE) {
            return null;
        }
        
        LinkedList<Integer> retList = new LinkedList<Integer>();
        
        int bit = 0x01;
        
        while (bit != 0) {
            if ((tag & bit) == bit) {
                retList.add(bit);
            }
            
            bit = bit << 1;
        }
        
        int size = retList.size();
        
        if (size <= 0)
            return null;
        else {
            int[] retArray = new int[size];
            
            int index = 0;
            
            for (Integer i : retList) {
                retArray[index] = i;
                index++;
            }
            
            return retArray;
        }
    }
    
    public static String[] definitionsFromTag(int tag) {
        
        int[] taints = taintsFromTag(tag);
        
        if (taints == null) {
            return null;
        }

        LinkedList<String> retList = new LinkedList<String>();
        
        for (int t : taints) {
            retList.add(DEFINITIONS.get(t));
        }
        
        int size = retList.size();
        
        return (size > 0) ? retList.toArray(new String[size]) : null;
    }
    
    // used to get a list of sensitive resources that can be used by the app
    // which is presented to the crowd in crowd tasks
    public static String crowdDefinitionsFromTag(int tag) {
        int[] taints = taintsFromTag(tag);
        
        if (taints == null) {
            return null;
        }

        // always want the same order
        Set<String> crowdDefinitions = new LinkedHashSet<String>();
        
        for (int t : taints) {
            String cd = CROWD_DEFINITIONS.get(t);
            
            if (cd == null) {
                continue;
            }
            
            crowdDefinitions.add(cd);
        }

        if (crowdDefinitions.size() <= 0) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (String cd : crowdDefinitions) {
            sb.append(cd);
            sb.append(", ");
        }
        
        // remove the extra comma appened
        sb.replace(sb.length() - 2, sb.length(), "");
        
        // replace the very last comma with ", and"
        String retVal = sb.toString();
        
        int i = sb.lastIndexOf(", ");
        
        if (i != -1) {
            return retVal.substring(0, i) + ", and " + retVal.substring(i + 2, retVal.length());
        }
        
        return retVal;
    }
    
    public static String[] permissionsFromTag(int tag) {
        int[] taints = taintsFromTag(tag);
        
        if (taints == null) {
            return null;
        }

        Set<String> permissions = new LinkedHashSet<String>();
        
        for (int t : taints) {
            String[] ps = PERMISSIONS.get(t);
            
            if (ps == null) {
                continue;
            }
            
            for (String p : ps) {
                if (p == null) {
                    continue;
                }
                
                permissions.add(p);
            }
            
        }

        return (permissions.size() > 0) ? permissions.toArray(new String[permissions.size()]) : null;
    }
    
    public static int taintTag(int[] taints) {
        if (taints == null)
            return 0;
        
        int tag = 0;
        
        for (int i = 0; i < taints.length; i++) {
            if (DEFINITIONS.containsKey(taints[i])) {
                tag |= taints[i];
            } else {
                return - 1;
            }
        }
        
        return tag;
    }
    
    public static boolean checkTag(int tag, int constant) {
        if ((tag & constant) == constant)
            return true;
        
        return false;
    }
    
    public static boolean isTransmissionTaintLog(String type) {
        if (type == null) {
            return false;
        }
        
        type = type.toLowerCase();
        
        return type.startsWith(TYPE_OS_NETWORK.toLowerCase()) ||
                type.startsWith(TYPE_LIBCORE_OS_SEND.toLowerCase()) ||
                type.startsWith(TYPE_SSL_OUTPUT.toLowerCase());
    }
    
    public static boolean isEncryptedTransmission(String type) {
        if (type == null) {
            return false;
        }
        
        type = type.toLowerCase();
        
        return type.startsWith(TYPE_SSL_OUTPUT.toLowerCase());
    }
}
