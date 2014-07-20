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

import javax.persistence.Column;
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
public class Component {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String uuid;
    private String classname;
    @Column(columnDefinition="TEXT")
    private String text;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private Boolean clickable;
    private String properties;
    
    //@ManyToOne
    //private Traversal traversal;
    
    @OneToOne
    private Annotation annotation;
    
    // Components belong to an activity
    //@ManyToOne
    //private Activity activity;
        
    public Component() {
        
    }

    public Integer getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getClassname() {
        return classname;
    }

    public String getText() {
        return text;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Boolean getClickable() {
        return clickable;
    }

    public String getProperties() {
        return properties;
    }
    
}
