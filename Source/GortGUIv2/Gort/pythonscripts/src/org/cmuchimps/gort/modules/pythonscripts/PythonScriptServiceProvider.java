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
package org.cmuchimps.gort.modules.pythonscripts;

import org.cmuchimps.gort.api.gort.PythonScriptService;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shahriyar
 */
@ServiceProvider(service=PythonScriptService.class)
public class PythonScriptServiceProvider extends PythonScriptService {

    @Override
    public boolean appActivities(String dbURL, Integer id, FileObject apk) {
        return AndroGuard.appActivities(dbURL, id, apk);
    }

    @Override
    public boolean appFiles(String dbURL, Integer id, FileObject apk) {
        return AndroGuard.appFiles(dbURL, id, apk);
    }

    @Override
    public boolean appPermissions(String dbURL, Integer id, FileObject apk) {
        return AndroGuard.appPermissions(dbURL, id, apk);
    }

    @Override
    public boolean appProviders(String dbURL, Integer id, FileObject apk) {
        return AndroGuard.appProviders(dbURL, id, apk);
    }

    @Override
    public boolean appReceivers(String dbURL, Integer id, FileObject apk) {
        return AndroGuard.appReceivers(dbURL, id, apk);
    }

    @Override
    public boolean appServices(String dbURL, Integer id, FileObject apk) {
        return AndroGuard.appServices(dbURL, id, apk);
    }

    @Override
    public boolean appBasicInfo(String dbURL, Integer id, FileObject apk) {
        return AndroGuard.appAllOptions(dbURL, id, apk);
    }
    
    @Override
    public boolean appDalvikInfo(String url, Integer id, FileObject apk) {
        return AndroGuard.dalvikAllOptions(url, id, apk);
    }
}
