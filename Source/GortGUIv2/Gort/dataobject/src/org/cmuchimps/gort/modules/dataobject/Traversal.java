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
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author shahriyar
 */
@Entity
public class Traversal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    
    private String directory;
    
    // mark whether this is the apps main traversal
    private Boolean main;
    
    // mark whether the traversal was only partially finished
    private Boolean partial;
    
    // mark whether this traversal ran until completion or if this
    // value is not set to True, then the traversal did not finish
    private Boolean finished;
    
    // marks whether this traversal has been post processed
    private Boolean processed;
    
    @ManyToOne
    @JoinColumn(name="app_fk")
    private App app;
    
    @OneToMany(mappedBy="traversal")
    @OrderBy
    private List<Heuristic> heuristics;
    
    @OneToMany
    @OrderBy
    private List<History> history;
    
    @OneToMany(mappedBy="traversal")
    private Set<Interaction> interactions;
    
    @OneToMany(mappedBy="traversal")
    private Set<Sequence> sequence;
    
    @OneToMany(mappedBy="traversal")
    private Set<State> states;
    
    @OneToMany
    @OrderBy
    private List<TaintLog> taintLogs;
    
    @OneToMany(mappedBy="traversal")
    @OrderBy
    private List<CrowdTask> crowdTasks;
    
    @OneToMany(mappedBy="traversal")
    @OrderBy("ip ASC")
    private List<Server> servers;
    
    @OneToOne
    private Annotation annotation;
    
    public Traversal() {
        
    }

    public Integer getId() {
        return id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Boolean getMain() {
        return main;
    }

    public void setMain(Boolean main) {
        this.main = main;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public List<History> getHistory() {
        return history;
    }

    public Set<Interaction> getInteractions() {
        return interactions;
    }

    public void setInteractions(Set<Interaction> interactions) {
        this.interactions = interactions;
    }

    public Set<Sequence> getSequence() {
        return sequence;
    }

    public void setSequence(Set<Sequence> sequence) {
        this.sequence = sequence;
    }

    public Set<State> getStates() {
        return states;
    }

    public void setStates(Set<State> states) {
        this.states = states;
    }

    public List<TaintLog> getTaintLogs() {
        return taintLogs;
    }

    public List<CrowdTask> getCrowdTasks() {
        return crowdTasks;
    }

    public void setCrowdTasks(List<CrowdTask> crowdTasks) {
        this.crowdTasks = crowdTasks;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public Boolean getFinished() {
        return finished;
    }

    public Boolean getPartial() {
        return partial;
    }

    public List<Heuristic> getHeuristics() {
        return heuristics;
    }

    public void setHeuristics(List<Heuristic> heuristics) {
        this.heuristics = heuristics;
    }
    
}
