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
package org.cmuchimps.gort.modules.apkfile;

import org.cmuchimps.gort.api.gort.ViewService;
import org.netbeans.api.project.Project;
import org.openide.cookies.CloseCookie;
import org.openide.cookies.OpenCookie;
import org.openide.loaders.OpenSupport;
import org.openide.windows.CloneableTopComponent;

/**
 *
 * @author shahriyar
 */
public class APKOpenSupport extends OpenSupport implements OpenCookie, CloseCookie {
    Project project;
    APKDataObject apk;
    
    // based on https://platform.netbeans.org/tutorials/70/nbm-filetype.html
    public APKOpenSupport(APKDataObject.Entry entry) {
        super(entry);
    }
    
    public APKOpenSupport(APKDataObject apk) {
        this(apk.getPrimaryEntry());
        this.project = apk.getProject();
        this.apk = apk;
    }
    
    public APKOpenSupport(Project project, APKDataObject apk) {
        this(apk.getPrimaryEntry());
        this.project = project;
        this.apk = apk;
    }
    
    @Override
    protected CloneableTopComponent createCloneableTopComponent() {
        ViewService vs = ViewService.getDefault();
        
        return vs.createCloneableTopComponent(project, apk);
    }
}
