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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author shahriyar
 */
@Entity
public class Annotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String text;
    private Boolean important;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    
    @OneToOne
    private Activity activity;
    
    @OneToOne
    private App app;
    
    @OneToOne
    private Component component;
    
    @OneToOne
    private Interaction interaction;
    
    @OneToOne
    private Library library;
    
    @OneToOne
    private Permission permission;
    
    @OneToOne
    private Receiver receiver;
    
    @OneToOne
    private Screenshot screenshot;
    
    @OneToOne
    private Service service;
    
    @OneToOne
    private State state;
    
    @OneToOne
    private TaintLog taintLog;
    
    @OneToOne
    private Traversal traversal;
    
}
