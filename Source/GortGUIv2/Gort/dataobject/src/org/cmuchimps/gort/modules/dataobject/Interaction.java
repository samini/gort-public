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

/**
 *
 * @author shahriyar
 */
@Entity
public class Interaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String type;
    private String args;
    private Boolean visited;
    private Boolean deterministic;
    
    @ManyToOne
    @JoinColumn(name="traversal_fk")
    private Traversal traversal;
    
    @ManyToOne
    @JoinColumn(name="state_fk")
    private State state;
    
    @ManyToMany
    private Set<Sequence> sequences;
    
    // An interaction can involve only 1 component, but many interactions could
    // be done with the same component
    @ManyToOne
    private Component component;
    
    @OneToMany(mappedBy="interaction")
    private Set<TaintLog> taintLogs;
    
    @OneToOne
    private Annotation annotation;
    
    public Interaction() {
        
    }
}
