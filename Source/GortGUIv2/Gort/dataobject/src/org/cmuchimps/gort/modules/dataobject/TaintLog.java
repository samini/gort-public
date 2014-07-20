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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.cmuchimps.gort.modules.helper.TaintHelper;

/**
 *
 * @author shahriyar
 */
@Entity
public class TaintLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    
    private String pid;
    private String tid;
    private String priority;
    private String tag;
    private String type;
    
    // message may be much longer than 255
    @Column(columnDefinition="TEXT")
    private String message;
    
    private Integer tainttag;
    private String ip;
    
    // taintlogs can be assigned a batch id so that
    // several taints are associated with one interaction or state
    private Integer batch;
    
    // having back references to traversal, activity, and state helps with
    // taint log processing
    
    // state is the owning side
    @ManyToOne
    @JoinColumn(name="state_fk")
    private State state;
    
    // An interaction can cause a taint log to be registered
    @ManyToOne
    @JoinColumn(name="interaction_fk")
    private Interaction interaction;
    
    @ManyToOne
    @JoinColumn(name="crowdtask_fk")
    private CrowdTask crowdTask;
    
    @ManyToOne
    @JoinColumn(name="server_fk")
    private Server server;
    
    @OneToOne
    private Annotation annotation;
    
    public TaintLog() {
        
    }

    public Integer getId() {
        return id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getPid() {
        return pid;
    }

    public String getTid() {
        return tid;
    }

    public String getPriority() {
        return priority;
    }

    public String getTag() {
        return tag;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Integer getTainttag() {
        return tainttag;
    }

    public String getIp() {
        return ip;
    }

    public Integer getBatch() {
        return batch;
    }

    public State getState() {
        return state;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public CrowdTask getCrowdTask() {
        return crowdTask;
    }

    public Server getServer() {
        return server;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTainttag(Integer tainttag) {
        this.tainttag = tainttag;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setBatch(Integer batch) {
        this.batch = batch;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setInteraction(Interaction interaction) {
        this.interaction = interaction;
    }

    public void setCrowdTask(CrowdTask crowdTask) {
        this.crowdTask = crowdTask;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }
    
    public boolean isTransmission() {
        return TaintHelper.isTransmissionTaintLog(this.getType());
    }
    
    public boolean isEncrypedTransmission() {
        return TaintHelper.isEncryptedTransmission(this.getType());
    }
}
