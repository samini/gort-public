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
package org.cmuchimps.gort.api.gort.heuristic;

import java.util.List;
import java.util.Set;
import org.cmuchimps.gort.api.gort.ProjectUtility;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.Permission;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public abstract class AbstractHeuristic {
    
    public static final String TYPE_STATIC = "Static";
    public static final String TYPE_DYNAMIC = "Dynamic";
    
    private String name;
    private String summary;
    private String description;
    
    private int concernLevel;

    public AbstractHeuristic() {
        // default concern level is medium
        this.concernLevel = IHeuristic.CONCERN_LEVEL_MIDIUM;
    }

    public AbstractHeuristic(String name, String summary) {
        this();
        this.name = name;
        this.summary = summary;
    }
    
    public AbstractHeuristic(String name, String summary, String description) {
        this(name, summary);
        this.description = description;
    }

    public AbstractHeuristic(String name, String summary, String description, int concernLevel) {
        this(name, summary, description);
        this.concernLevel = concernLevel;
    }
    
    public String getName() {
        return name;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public int getConcernLevel() {
        return concernLevel;
    }

    public void setConcernLevel(int concernLevel) {
        this.concernLevel = concernLevel;
    }
    
    public static Boolean hasPermission(Project project, FileObject apk, String permission) {
        if (permission == null || permission.isEmpty()) {
            return null;
        }
        
        return hasAllPermissions(project, apk, new String[]{permission});
    }
    
    public static Boolean hasAllPermissions(Project project, FileObject apk, String[] permissions) {
        if (project == null || apk == null || permissions == null || permissions.length <= 0) {
            return null;
        }
        
        GortEntityManager gem = ProjectUtility.getGortEntityManager(project);
        
        if (gem == null) {
            return null;
        }
        
        List<Permission> appPermissions = gem.selectPermission(apk.getNameExt());
        
        if (appPermissions == null || appPermissions.isEmpty()) {
            return false;
        }

        Set appPermissionNames = Permission.getPermissionNames(appPermissions);
        
        if (appPermissionNames == null) {
            return false;
        }

        for (String permission : permissions) {
            if (!appPermissionNames.contains(permission)) {
                return false;
            }
        }

        return true;
    }
    
    public static Boolean hasAnyPermission(Project project, FileObject apk, String[] permissions) {
        if (project == null || apk == null || permissions == null || permissions.length <= 0) {
            return null;
        }
        
        GortEntityManager gem = ProjectUtility.getGortEntityManager(project);
        
        if (gem == null) {
            return null;
        }
        
        List<Permission> appPermissions = gem.selectPermission(apk.getNameExt());
        
        if (appPermissions == null || appPermissions.isEmpty()) {
            return false;
        }

        Set appPermissionNames = Permission.getPermissionNames(appPermissions);
        
        if (appPermissionNames == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (appPermissionNames.contains(permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    public abstract String getType();
}
