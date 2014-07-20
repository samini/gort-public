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
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author shahriyar
 */
@Entity
public class History {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTimestamp;
    
    // this should be the timestamp of next history element
    // but will reduce number of accesses to the db
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTimestamp;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date interactionTimestamp;
    
    @ManyToOne
    private Sequence sequence;
    
    @ManyToOne
    private Interaction interaction;
    
    @ManyToOne
    private State startState;
    
    @ManyToOne
    private State endState;
    
    @ManyToOne
    private Screenshot startScreenshot;
    
    @ManyToOne
    private Screenshot endScreenshot;
    
    private Boolean success;
    
    // whether the action popped up keyboard, dialog, nothing, change in screen
    // changed activity etc...
    private Integer result;

    public Integer getId() {
        return id;
    }

    public Date getStartTimestamp() {
        return startTimestamp;
    }

    public Date getEndTimestamp() {
        return endTimestamp;
    }

    public Date getInteractionTimestamp() {
        return interactionTimestamp;
    }

    public Sequence getSequence() {
        return sequence;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public State getStartState() {
        return startState;
    }

    public State getEndState() {
        return endState;
    }

    public Screenshot getStartScreenshot() {
        return startScreenshot;
    }

    public Screenshot getEndScreenshot() {
        return endScreenshot;
    }

    public Boolean getSuccess() {
        return success;
    }

    public Integer getResult() {
        return result;
    }
    
}
