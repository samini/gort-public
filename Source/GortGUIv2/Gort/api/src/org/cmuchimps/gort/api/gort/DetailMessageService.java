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

import java.util.Collections;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author shahriyar
 */
public abstract class DetailMessageService implements Lookup.Provider {
    
    protected final InstanceContent content;
    protected final Lookup lookup;
    
    public DetailMessageService() {
        content = new InstanceContent();
        lookup = new AbstractLookup(content);
    }
    
    public static DetailMessageService getDefault() {
        DetailMessageService result = Lookup.getDefault().lookup(DetailMessageService.class);
        if (result == null) {
            result = DetailMessageServiceProvider.getInstance();
        }
        return result;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    public abstract void sendMessage(String message);
    
    private static class DetailMessageServiceProvider extends DetailMessageService {
        
        private static DetailMessageServiceProvider instance = null;
        
        private DetailMessageServiceProvider() {
            super();
        }
        
        public static synchronized DetailMessageServiceProvider getInstance() {
            if (instance == null) {
                instance = new DetailMessageServiceProvider();
            }
            
            return instance;
        }
        
        @Override
        public void sendMessage(String message) {
            System.out.println("" + DetailMessageService.class.getSimpleName() + " received message: " + message);
            content.set(Collections.singleton(message), null);
        }
    }
}
