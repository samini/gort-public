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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 *
 * @author shahriyar
 */
@Entity
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private Boolean launcher;
    
    // Based on static analysis determine whether the activity is in app or not
    private Boolean inApp;
    
    @OneToOne
    private Annotation annotation;
    
    public Activity() {
        
    }
    
    public Activity(String name, Boolean launcher) {
        this.name = name;
        this.launcher = launcher;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Boolean getLauncher() {
        return launcher;
    }

    public Boolean getInApp() {
        return inApp;
    }
    
    // at times the manifest declartion does not follow convention
    // resulting in activities in app to be reported as not
    // this function also does a check on the packagename
    public Boolean getInAppLenient(String packageName) {
        Boolean retVal = getInApp();
        
        if (!Boolean.TRUE.equals(retVal) &&
                (name != null && !name.isEmpty()) &&
                (packageName != null && !packageName.isEmpty())) {
            retVal = name.toLowerCase().startsWith(packageName.toLowerCase());
        }
        
        return retVal;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }
    
    
}
