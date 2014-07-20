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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Transient;

/**
 *
 * @author shahriyar
 */
@Entity
public class State {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    // a hash describing the state. This is generated in the python side
    // by taking a hash of the elements (classname, level, order) in a 
    // DFS search order
    private String hash;
    
    @ManyToOne
    @JoinColumn(name="traversal_fk")
    private Traversal traversal;
    
    @ManyToOne
    private Activity activity;
    
    @OneToMany
    private Set<Component> components;
    
    @OneToMany(mappedBy="state")
    private Set<Sequence> sequences;
    
    // interactions that could occur while at this state
    @OneToMany(mappedBy="state")
    private Set<Interaction> interaction;
    
    @OneToMany(mappedBy="state")
    @OrderBy
    private List<TaintLog> taintLogs;
    
    // a state can have many crowd tasks associated with it
    // where it is either the start state or the end state
    @OneToMany(mappedBy="startState")
    private Set<CrowdTask> crowdTasksStartOwner;
    
    @OneToMany(mappedBy="endState")
    private Set<CrowdTask> crowdTasksEndOwner;
    
    @ManyToMany
    @OrderBy("ip ASC")
    private List<Server> servers;
    
    // Define a one-to-many relationship to screenshots
    // This does not have to be bi-directional as we don't need to know
    // which state a screenshot belongs to
    @OneToMany
    @OrderBy
    private List<Screenshot> screenshots;
    
    @ManyToMany(mappedBy="states")
    @OrderBy
    private List<Heuristic> heuristics;
    
    @OneToOne
    private Annotation annotation;
    
    public Set<CrowdTask> crowdTasks() {
        if (crowdTasksStartOwner == null) {
            return crowdTasksEndOwner;
        }
        
        if (crowdTasksEndOwner == null) {
            return crowdTasksStartOwner;
        }
        
        Set<CrowdTask> combinedSet = new HashSet<CrowdTask>();
        combinedSet.addAll(crowdTasksStartOwner);
        combinedSet.addAll(crowdTasksEndOwner);
        
        return combinedSet;
    }

    public Integer getId() {
        return id;
    }

    public Traversal getTraversal() {
        return traversal;
    }

    public Activity getActivity() {
        return activity;
    }

    public Set<Component> getComponents() {
        return components;
    }

    public Set<Sequence> getSequences() {
        return sequences;
    }

    public Set<Interaction> getInteraction() {
        return interaction;
    }

    public List<TaintLog> getTaintLogs() {
        return taintLogs;
    }

    public List<Server> getServers() {
        return servers;
    }

    public List<Screenshot> getScreenshots() {
        return screenshots;
    }

    public Set<CrowdTask> getCrowdTasksStartOwner() {
        return crowdTasksStartOwner;
    }

    public void setCrowdTasksStartOwner(Set<CrowdTask> crowdTasksStartOwner) {
        this.crowdTasksStartOwner = crowdTasksStartOwner;
    }

    public Set<CrowdTask> getCrowdTasksEndOwner() {
        return crowdTasksEndOwner;
    }

    public void setCrowdTasksEndOwner(Set<CrowdTask> crowdTasksEndOwner) {
        this.crowdTasksEndOwner = crowdTasksEndOwner;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }

    public List<Heuristic> getHeuristics() {
        return heuristics;
    }

    public void setHeuristics(List<Heuristic> heuristics) {
        this.heuristics = heuristics;
    }

    public String getHash() {
        return hash;
    }
    
    public boolean hasTransmissionTaintlogs() {
        List<TaintLog> ts = getTaintLogs();
        
        if (ts == null || ts.isEmpty()) {
            //System.out.println("State does not have taintlogs.");
            return false;
        }
        
        Iterator<TaintLog> iterator = ts.iterator();
        
        while (iterator.hasNext()) {
            if (iterator.next().isTransmission()) {
                return true;
            }
        }
        
        return false;
    }
    
    @Transient
    private Integer combinedTaintTag = null;
    
    public Integer getCombinedTaintTag() {
        if (combinedTaintTag == null) {
            List<TaintLog> ts = getTaintLogs();

            combinedTaintTag = new Integer(0);
            
            if (ts != null && !ts.isEmpty()) {
                
                Iterator<TaintLog> iterator = ts.iterator();
                
                while (iterator.hasNext()) {
                    TaintLog t = iterator.next();
                    
                    if (t.getTainttag() == null) {
                        continue;
                    }
                    
                    combinedTaintTag |= t.getTainttag().intValue();
                }
            }
        }
        
        return combinedTaintTag;
    }
    
    public static Integer mergeCombinedTaintTags(State s0, State s1) {
        if (s0 == null && s1 == null) {
            return null;
        }
        
        int tag0 = 0;
        int tag1 = 0;
        
        if (s0 != null && s0.getCombinedTaintTag() != null) {
            tag0 = s0.getCombinedTaintTag().intValue();
        }
        
        if (s1 != null && s1.getCombinedTaintTag() != null) {
            tag1 = s1.getCombinedTaintTag().intValue();
        }
        
        return tag0 | tag1;
    }
}
