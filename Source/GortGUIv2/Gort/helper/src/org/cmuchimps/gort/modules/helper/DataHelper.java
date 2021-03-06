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

import java.util.List;

/**
 *
 * @author shahriyar
 */
public class DataHelper {
    public static String[][] to2DArray(List<String[]> list) {
        if (list == null) {
            return null;
        }
        
        int size = list.size();
        
        String[][] retVal = new String[size][];
        
        int index = 0;
        
        for (String[] os : list) {
            retVal[index++] = os;
        }
        
        return retVal;
    }
}
