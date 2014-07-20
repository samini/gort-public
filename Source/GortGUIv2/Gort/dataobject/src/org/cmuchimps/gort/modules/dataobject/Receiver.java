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
public class Receiver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    //@ManyToOne
    //private App app;
    
    @OneToOne
    private Annotation annotation;
    
    // attributes based on:
    // http://developer.android.com/guide/topics/manifest/receiver-element.html
    private Boolean enabled;
    private Boolean exported;
    private String icon;
    private String label;
    private String name;
    private String permission;
    private String process;

    public Integer getId() {
        return id;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean getExported() {
        return exported;
    }

    public String getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public String getProcess() {
        return process;
    }
}
