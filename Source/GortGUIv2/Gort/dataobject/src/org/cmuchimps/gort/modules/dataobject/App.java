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

import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 *
 * @author shahriyar
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"apk"})})
public class App {
    
    // Note using the GenerationType.IDENTITY is crucial to make the id column
    // a primary key that can be handled by other ORM managers. Without this
    // hibernate will use its own sequence to generate ids.
    // http://stackoverflow.com/questions/10628099/hibernate-could-not-get-next-sequence-value
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String apk;
    
    private String name;
    private String md5;
    @Column(columnDefinition="TEXT")
    private String description;
    private String process;
    @Column(name="package")
    private String package_;
    private Long size;
    private String category;
    // primary developer
    private String developer;
    
    private Boolean dynamic;
    @Column(name="native")
    private Boolean native_;
    private Boolean obfuscation;
    private Boolean reflection;
    
    private Integer minSDKVersion;
    private Integer targetSDKVersion;
    
    // can have mutliple traversals although usually just 1
    @OneToMany(mappedBy="app")
    private Set<Traversal> traversals;
    
    // has many services, permissions, and receivers
    @OneToMany
    @OrderBy("name ASC")
    private List<Activity> activies;
    
    // Since many apps can be using same permissions and libraries these relationships
    // are many-to-many rather than one-to-many. We do not have Many to Many relationships
    // on the other end because we do not need them to be bi-directional for now.
    @ManyToMany
    @OrderBy("name ASC")
    private List<Library> libraries;
    
    @ManyToMany
    @OrderBy("name ASC")
    private List<Permission> permissions;
    
    @OneToMany
    @OrderBy("name ASC")
    private List<Provider> providers;
    
    @OneToMany
    @OrderBy("name ASC")
    private List<Receiver> receivers;
    
    @OneToMany
    @OrderBy
    private List<Screenshot> screenshots;
    
    @OneToMany
    @OrderBy("name ASC")
    private List<Service> services;
    
    @OneToMany(mappedBy="app")
    private List<Heuristic> heuristics;
    
    @OneToOne
    private Annotation annotation;
    
    public App() {
        
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getApk() {
        return apk;
    }

    public void setApk(String apk) {
        this.apk = apk;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String getPackage() {
        return package_;
    }

    public void setPackage(String package_) {
        this.package_ = package_;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Set<Traversal> getTraversals() {
        return traversals;
    }

    public void setTraversals(Set<Traversal> traversals) {
        this.traversals = traversals;
    }

    public List<Activity> getActivies() {
        return activies;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public List<Receiver> getReceivers() {
        return receivers;
    }

    public List<Screenshot> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(List<Screenshot> screenshots) {
        this.screenshots = screenshots;
    }

    public List<Service> getServices() {
        return services;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }

    public String getCategory() {
        return category;
    }

    public Boolean getDynamic() {
        return dynamic;
    }

    public Boolean getNative_() {
        return native_;
    }

    public Boolean getObfuscation() {
        return obfuscation;
    }

    public Boolean getReflection() {
        return reflection;
    }

    public Integer getMinSDKVersion() {
        return minSDKVersion;
    }

    public Integer getTargetSDKVersion() {
        return targetSDKVersion;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public List<Provider> getProviders() {
        return providers;
    }

    public List<Heuristic> getHeuristics() {
        return heuristics;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }    

    public void setCategory(String category) {
        this.category = category;
    }
    
}
