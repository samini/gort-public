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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author shahriyar
 */
@Entity
public class HIT {

    public static final String TYPE_TASK_EXTRACTION = "task_extraction";
    public static final String TYPE_TASK_VERIFICATION = "task_verification";
    public static final String TYPE_RESOURCE_JUSTIFICATION = "resource_justification";
    
    public HIT() {
        super();
    }

    public HIT(Date submission, String hitId, String hitTypeId, String hitGroupId, String hitLayoutId) {
        this.submission = submission;
        this.hitId = hitId;
        this.hitTypeId = hitTypeId;
        this.hitGroupId = hitGroupId;
        this.hitLayoutId = hitLayoutId;
    }
    
    public HIT(String type, Date submission, String hitId, String hitTypeId, String hitGroupId, String hitLayoutId) {
        this(submission, hitId, hitTypeId, hitGroupId, hitLayoutId);
        this.type = type;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String type;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date submission;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date completion;
    
    @ManyToOne
    @JoinColumn(name="crowdtask_fk")
    private CrowdTask crowdTask;
    
    // mechanical turk hit id
    private String hitId;
    
    // mechanical turk hit type id
    private String hitTypeId;
    
    private String hitGroupId;
    
    private String hitLayoutId;
    
    private String title;
    
    @Column(columnDefinition="TEXT")
    private String description;
    
    @Column(columnDefinition="TEXT")
    private String question;
    
    private String keywords;
    
    private String properties;
    
    @Column(columnDefinition="TEXT")
    private String input;
    
    private String result;
    
    private Boolean inFlight;
    
    private String inputFile;
    
    private String failureFile;
    
    private String successFile;
    
    private String resultFile;
    
    @OneToMany(mappedBy="hit")
    @OrderBy // ordering by primary key is assumed
    private List<Assignment> assignments;

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public CrowdTask getCrowdTask() {
        return crowdTask;
    }

    public void setCrowdTask(CrowdTask crowdTask) {
        this.crowdTask = crowdTask;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public Boolean getInFlight() {
        return inFlight;
    }

    public void setInFlight(Boolean inFlight) {
        this.inFlight = inFlight;
    }

    public String getHitId() {
        return hitId;
    }

    public void setHitId(String hitId) {
        this.hitId = hitId;
    }

    public String getHitTypeId() {
        return hitTypeId;
    }

    public void setHitTypeId(String hitTypeId) {
        this.hitTypeId = hitTypeId;
    }

    public String getHitGroupId() {
        return hitGroupId;
    }

    public void setHitGroupId(String hitGroupId) {
        this.hitGroupId = hitGroupId;
    }

    public String getHitLayoutId() {
        return hitLayoutId;
    }

    public void setHitLayoutId(String hitLayoutId) {
        this.hitLayoutId = hitLayoutId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }
    
}
