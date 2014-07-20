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
public abstract class TraverserService {
    
    public static TraverserService getDefault() {
        return Lookup.getDefault().lookup(TraverserService.class);
    }
    
    // have a target Serial associated with project properties
    // if it is not set set it and add it to the properties
    public abstract boolean traverse(String dbURL, Integer id, FileObject traversalDirectory, String target);
}
