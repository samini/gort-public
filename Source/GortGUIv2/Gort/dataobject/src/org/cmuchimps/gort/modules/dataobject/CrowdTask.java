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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
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
public class CrowdTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    // uuid signifying crowdtasks that are made in the same run
    private String batch;
    
    // not currently used, only added for future support
    private String type;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date submission;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date completion;
    
    // do an or of all the taint tags associated with this CrowdTask
    private Integer combinedTaintTag;
    
    @ManyToOne
    @JoinColumn(name="traversal_fk")
    private Traversal traversal;
    
    // a crowd tasks involves one start state and one end state
    // but a state could have many crowd tasks where it could
    // be either the startstate or the end state
    @ManyToOne
    @JoinColumn(name="startstate_fk")
    private State startState;
    
    @ManyToOne
    @JoinColumn(name="endstate_fk")
    private State endState;
    
    // do not need back links for screenshots
    @ManyToOne
    private Screenshot startScreenshot;
    
    @ManyToOne
    private Screenshot endScreenshot;
    
    @OneToMany(mappedBy="crowdTask")
    private Set<TaintLog> taintLogs;
    
    @OneToMany(mappedBy="crowdTask")
    @OrderBy // ordering by primary key is assumed
    private List<HIT> hits;
    
    // the crowd task should store the results for each stage
    // should be approximately 10 labels. once the hit is complete. 
    // the 10 labels should be put in?
    @Column(columnDefinition="TEXT")
    private String labels;
    
    private String selectedLabel;
    
    // number of valid comfort, expectation pairs based on validation
    private Integer valid;
    
    // final results of the task with respect to comfort and expectation
    private Double comfortAverage;
    
    private Double comfortMedian;
    
    private Double comfortStdDev;
    
    // True if the median is that the crowd expected the resource to be used
    private Boolean expectedMedian;
    
    private String inputFile;
    
    private String failureFile;
    
    private String successFile;
    
    private String resultFile;
    
    private Boolean inFlight;
    
    @OneToOne
    private CrowdTask next;
    
    @OneToOne(mappedBy="next")
    private CrowdTask prev;

    public CrowdTask() {
        inFlight = true;
    }

    public CrowdTask(Integer combinedTaintTag, Traversal traversal, State startState, State endState, Screenshot startScreenshot, Screenshot endScreenshot) {
        this();
        this.combinedTaintTag = combinedTaintTag;
        this.traversal = traversal;
        this.startState = startState;
        this.endState = endState;
        this.startScreenshot = startScreenshot;
        this.endScreenshot = endScreenshot;
    }

    public CrowdTask(String batch, Integer combinedTaintTag, Traversal traversal, State startState, State endState, Screenshot startScreenshot, Screenshot endScreenshot) {
        this(combinedTaintTag, traversal, startState, endState, startScreenshot, endScreenshot);
        this.batch = batch;
    }
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCombinedTaintTag() {
        return combinedTaintTag;
    }

    public void setCombinedTaintTag(Integer combinedTaintTag) {
        this.combinedTaintTag = combinedTaintTag;
    }

    public Traversal getTraversal() {
        return traversal;
    }

    public void setTraversal(Traversal traversal) {
        this.traversal = traversal;
    }

    public State getStartState() {
        return startState;
    }

    public void setStartState(State startState) {
        this.startState = startState;
    }

    public State getEndState() {
        return endState;
    }

    public void setEndState(State endState) {
        this.endState = endState;
    }

    public Screenshot getStartScreenshot() {
        return startScreenshot;
    }

    public void setStartScreenshot(Screenshot startScreenshot) {
        this.startScreenshot = startScreenshot;
    }

    public Screenshot getEndScreenshot() {
        return endScreenshot;
    }

    public void setEndScreenshot(Screenshot endScreenshot) {
        this.endScreenshot = endScreenshot;
    }

    public Set<TaintLog> getTaintLogs() {
        return taintLogs;
    }

    public void setTaintLogs(Set<TaintLog> taintLogs) {
        this.taintLogs = taintLogs;
    }

    public List<HIT> getHits() {
        return hits;
    }

    public void setHits(List<HIT> hits) {
        this.hits = hits;
    }

    public Double getComfortAverage() {
        return comfortAverage;
    }

    public void setComfortAverage(Double comfortAverage) {
        this.comfortAverage = comfortAverage;
    }

    public Double getComfortMedian() {
        return comfortMedian;
    }

    public void setComfortMedian(Double comfortMedian) {
        this.comfortMedian = comfortMedian;
    }

    public Double getComfortStdDev() {
        return comfortStdDev;
    }

    public void setComfortStdDev(Double comfortStdDev) {
        this.comfortStdDev = comfortStdDev;
    }

    public Boolean getExpectedMedian() {
        return expectedMedian;
    }

    public void setExpectedMedian(Boolean expectedMedian) {
        this.expectedMedian = expectedMedian;
    }

    public Boolean getInFlight() {
        return inFlight;
    }

    public void setInFlight(Boolean inFlight) {
        this.inFlight = inFlight;
    }

    public Date getSubmission() {
        return submission;
    }

    public void setSubmission(Date submission) {
        this.submission = submission;
    }

    public Date getCompletion() {
        return completion;
    }

    public void setCompletion(Date completion) {
        this.completion = completion;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getFailureFile() {
        return failureFile;
    }

    public void setFailureFile(String failureFile) {
        this.failureFile = failureFile;
    }

    public String getSuccessFile() {
        return successFile;
    }

    public void setSuccessFile(String successFile) {
        this.successFile = successFile;
    }

    public String getResultFile() {
        return resultFile;
    }

    public void setResultFile(String resultFile) {
        this.resultFile = resultFile;
    }

    public CrowdTask getNext() {
        return next;
    }

    public void setNext(CrowdTask next) {
        this.next = next;
    }

    public CrowdTask getPrev() {
        return prev;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public String getSelectedLabel() {
        return selectedLabel;
    }

    public void setSelectedLabel(String selectedLabel) {
        this.selectedLabel = selectedLabel;
    }

    public Integer getValid() {
        return valid;
    }

    public void setValid(Integer valid) {
        this.valid = valid;
    }
    
    public HIT getHITByType(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        
        Iterator<HIT> iterator = hits.iterator();
        
        while (iterator.hasNext()) {
            HIT h = iterator.next();
            if (type.equals(h.getType())) {
                return h;
            }
        }
        
        return null;
    }
    
    public HIT getTaskExtractionHIT() {
        return getHITByType(HIT.TYPE_TASK_EXTRACTION);
    }
    
    public HIT getTaskVerificationHIT() {
        return getHITByType(HIT.TYPE_TASK_VERIFICATION);
    }
    
    public HIT getResourceJustificationHIT() {
        return getHITByType(HIT.TYPE_RESOURCE_JUSTIFICATION);
    }
    
}
