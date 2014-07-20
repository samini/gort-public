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

import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author shahriyar
 */
public abstract class AdbService {
    public static AdbService getDefault() {
        return Lookup.getDefault().lookup(AdbService.class);
    }
    
    //provide protocol and port so example input would be (tcp:6100, tcp:7100)
    //port 6100 on host to port 7100 on emualtor/device
    public abstract boolean forward(String host, String device);
    
    public abstract boolean deviceConnected(String serial);
    
    public abstract boolean installPackage(FileObject apk);
    
    public abstract boolean uninstallPackage(String packageName);
    
    public abstract String adbPath();
}
