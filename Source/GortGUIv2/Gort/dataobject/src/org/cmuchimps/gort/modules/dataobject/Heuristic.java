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

import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author shahriyar
 */
@Entity
public class Heuristic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name="app_fk")
    private App app;
    
    private String name;
    private String summary;
    
    // description may be larger than 255
    @Column(columnDefinition="TEXT")
    private String description;
    
    private Integer concernLevel;
    
    // static or dynamic
    private String type;
    
    // the class object for the heuristic
    private String clazz;
    
    private Boolean result;
    
    // when the value was computed
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    
    @ManyToMany
    @OrderBy
    private List<State> states;
    
    @ManyToOne
    @JoinColumn(name="traversal_fk")
    private Traversal traversal;

    public Heuristic() {
    }

    public Heuristic(String name, String summary, String description, Integer concernLevel, String type, String clazz, Boolean result, Date timestamp) {
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.concernLevel = concernLevel;
        this.type = type;
        this.clazz = clazz;
        this.result = result;
        this.timestamp = timestamp;
    }
    
    public Heuristic(App app, String name, String summary, String description, Integer concernLevel, String type, String clazz, Boolean result, Date timestamp) {
        this.app = app;
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.concernLevel = concernLevel;
        this.type = type;
        this.clazz = clazz;
        this.result = result;
        this.timestamp = timestamp;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getConcernLevel() {
        return concernLevel;
    }

    public void setConcernLevel(Integer concernLevel) {
        this.concernLevel = concernLevel;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }

    public Traversal getTraversal() {
        return traversal;
    }

    public void setTraversal(Traversal traversal) {
        this.traversal = traversal;
    }
    
}
