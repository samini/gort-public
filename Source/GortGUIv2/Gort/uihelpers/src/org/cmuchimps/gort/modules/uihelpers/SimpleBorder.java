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
package org.cmuchimps.gort.modules.uihelpers;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 *
 * @author shahriyar
 */
public class SimpleBorder {
    private static Border simpleBorder = null, emptyBorder = null;
    
    public static Border getSimpleBorder() {
        if (simpleBorder == null) {
            //simpleBorder = BorderFactory.createLineBorder(Color.BLACK);
            simpleBorder = BorderFactory.createEtchedBorder();
        }
        
        return simpleBorder;
    }
    
    public static Border getEmptyBorder() {
        if (emptyBorder == null) {
            //simpleBorder = BorderFactory.createLineBorder(Color.BLACK);
            emptyBorder = BorderFactory.createEmptyBorder();
        }
        
        return emptyBorder;
    }
}
