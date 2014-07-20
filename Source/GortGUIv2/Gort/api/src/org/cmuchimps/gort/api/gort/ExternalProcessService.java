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
package org.cmuchimps.gort.api.gort;

import org.openide.util.Lookup;

/**
 *
 * @author shahriyar
 */
public abstract class ExternalProcessService {
    
    public static final String EXTERNAL_PROCESS_WINDOW_LABEL = "External Process";
    
    public static ExternalProcessService getDefault() {
        return Lookup.getDefault().lookup(ExternalProcessService.class);
    }
    
    public abstract void exec(String command);
    
    public abstract int exitCode(String command);
    
    public abstract String output(String command, String windowLabel);
    
    public abstract String output(String command);
}
