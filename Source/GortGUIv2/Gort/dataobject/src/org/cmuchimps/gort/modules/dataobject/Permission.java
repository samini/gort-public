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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 *
 * @author shahriyar
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    //There should be one row for each unique permission, i.e., there is a unique
    //constraint here.
    private String name;
    
    @Column(columnDefinition="TEXT")
    private String description;
    
    //@ManyToOne
    //private App app;
    
    @OneToOne
    private Annotation annotation;
    
    public Permission() {
        
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Annotation getAnnotation() {
        return annotation;
    }
    
    public static Set<String> getPermissionNames(List<Permission> permissions) {
        if (permissions == null) {
            return null;
        }
        
        Set<String> retVal = new HashSet<String>();
        
        for (Permission p : permissions) {
            if (p == null) {
                continue;
            }

            String permissionName = p.getName();

            if (permissionName == null || permissionName.isEmpty()) {
                continue;
            }
            
            retVal.add(permissionName);
        }
        
        return retVal;
    }
    
    public static Map<String, Permission> getPermissionMap(List<Permission> permissions) {
        if (permissions == null) {
            return null;
        }
        
        Map<String, Permission> retVal = new HashMap<String, Permission>();
        
        for (Permission p : permissions) {
            if (p == null) {
                continue;
            }

            String permissionName = p.getName();

            if (permissionName == null || permissionName.isEmpty()) {
                continue;
            }
            
            retVal.put(permissionName, p);
        }
        
        return (retVal.size() > 0) ? retVal : null;
    }
    
}
