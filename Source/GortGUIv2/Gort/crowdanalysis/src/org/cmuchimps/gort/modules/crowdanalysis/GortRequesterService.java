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
package org.cmuchimps.gort.modules.crowdanalysis;

import com.amazonaws.mturk.addon.HITDataInput;
import com.amazonaws.mturk.addon.HITDataOutput;
import com.amazonaws.mturk.addon.HITProperties;
import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.addon.QAPValidator;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.ReviewPolicy;
import com.amazonaws.mturk.service.axis.AsyncReply;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ValidationException;
import com.amazonaws.mturk.util.ClientConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author shahriyar
 */
public class GortRequesterService extends RequesterService {
    
    private static Logger log = Logger.getLogger(GortRequesterService.class);
    
    private static final int MAX_BATCH = 500;     // maximum batch size for batch chunk
    
    public GortRequesterService() {
        super();
    }
    
    public GortRequesterService(ClientConfig config) {
        super(config);
    }
    
  /**
   * Creates multiple HITs. 
   * 
   * @param input          the input data needed for the HITs
   * @param props          the properties of the HITs
   * @param question       a question structure that contains the question
   *                       asked in the HITs 
   * @param numHITToLoad   the number of HITs to load
   * @param success        the file that contains the HIT Ids and HIT type Ids of the
   *                       created HITs
   * @param failure        the failure file 
   * @param assignmentReviewPolicy  the assignment review policy
   * @param hitReviewPolicy the hit review policy
   * @return an array of HIT objects
   * @throws Exception
   */
  public HIT[] createHITs(HITDataInput input, HITProperties props,
          HITQuestion question, int numHITToLoad, HITDataOutput success,
          HITDataOutput failure, ReviewPolicy assignmentReviewPolicy,
          ReviewPolicy hitReviewPolicy) throws Exception {     
    // Create HITs
    List<HIT> hits = new ArrayList<HIT>();

    String[] fieldHeaders = new String[] { 
        HITProperties.HITField.HitId.getFieldName(), 
        HITProperties.HITField.HitTypeId.getFieldName()
    };

    boolean hasFailures = false;
    if( success != null ) {
      success.setFieldNames( fieldHeaders );
    }
    
    int numRecords;
    if (numHITToLoad != RequesterService.LOAD_ALL) {
      numRecords = Math.min(numHITToLoad, input.getNumRows()-1);
    }
    else {
      numRecords = input.getNumRows() - 1;
    }

    // submit hits to work pool
    AsyncReply[] replies = new AsyncReply[MAX_BATCH];

    // Map of HIT types created
    String hitTypeForBatch = null;
    HIT hit = null;

    // split work
    log.debug(String.format("Creating %d HITs with max memory %d", numRecords, Runtime.getRuntime().maxMemory()));

    int numBatches = numRecords / MAX_BATCH;
    for (int curBatch=0; curBatch<=numBatches; curBatch++) {
      int iStart = curBatch * MAX_BATCH;
      int iEnd = iStart + MAX_BATCH;
      if (iEnd > numRecords) {
        iEnd = numRecords;
      }

      log.debug(String.format("Processing batch %d (%d to %d)", curBatch, iStart, iEnd)); 

      for (int i = iStart; i < iEnd; i++) {
        // Merge the input with the question
        // Start from the second line since the first line contains the field names
        Map<String, String> inputMap = input.getRowAsMap(i + 1);   

        // Merge the input with the properties
        props.setInputMap(inputMap);

        // we need to make sure to not create multiple hittypes for matching HITs
        // due to multithreaded calls being processed at the same time
        if (hitTypeForBatch == null) {
          hitTypeForBatch = super.registerHITType(
              props.getAutoApprovalDelay(), 
              props.getAssignmentDuration(), 
              props.getRewardAmount(), 
              props.getTitle(), 
              props.getKeywords(), 
              props.getDescription(), 
              props.getQualificationRequirements());
        }

        replies[i-iStart] = super.createHITAsync(
            hitTypeForBatch,    
            null,       // title
            null,       // description
            null,       // keywords
            question.getQuestion(inputMap), 
            null,       // reward
            null,       // assignmentDurationInSeconds
            null,       // autoApprovalDelayInSeconds
            props.getLifetime(), 
            props.getMaxAssignments(), 
            props.getAnnotation(), 
            null,       // qualification requirements 
            null,       // response group
            null,       // uniqueRequestToken
            assignmentReviewPolicy,       // assignmentReviewPolicy
            hitReviewPolicy,       // hitReviewPolicy
            null);      // async callback   
      }

      // wait for thread pool to finish processing these requests and evaluate results        
      for (int i = iStart; i < iEnd; i++) {
        try {
          hit = ((HIT[])replies[i-iStart].getResult())[0];         
          hits.add(hit);

          log.info("Created HIT " + (i + 1) + ": HITId=" + hit.getHITId());

          if( success != null ) {
            // Print to the success file
            HashMap<String,String> good = new HashMap<String,String>();
            good.put( fieldHeaders[0], hit.getHITId() );
            good.put( fieldHeaders[1], hit.getHITTypeId() );
            success.writeValues(good);
          }
        }
        catch (Exception e) {         
          // Validate the question
          Map<String,String> row = input.getRowAsMap(i+1);
          try {
            Map<String, String> inputMap = input.getRowAsMap(i + 1);
            QAPValidator.validate(question.getQuestion(inputMap));
            // If it passed validation, then log the exception e
            log.error("[ERROR] Error creating HIT " + (i+1) 
                + " (" + input.getRowValues(i+1)[0] + "): " + e.getLocalizedMessage());         
          }
          catch (ValidationException ve) {
            // Otherwise, log the validation exception in place of the service exception
            log.error("[ERROR] Error creating HIT " + (i+1) 
                + " (" + input.getRowValues(i+1)[0] + "): " + ve.getLocalizedMessage());
          }

          if( failure != null ) {
            // Create the failure file
            if (!hasFailures) {              
              hasFailures = true;
              failure.setFieldNames( input.getFieldNames() );
            }

            // Print to the failure file
            failure.writeValues(row);
          }
        }         
      }

    }

    if (hit != null && log.isInfoEnabled()) {
      // Print the URL at which the new HIT can be viewed at the end as well
      // so the user doesn't have to "scroll" up in case lots of HITs have been loaded
      log.info(System.getProperty("line.separator") + "You may see your HIT(s) with HITTypeId '" + hit.getHITTypeId() + "' here: ");
      log.info(System.getProperty("line.separator") + "  " + getWebsiteURL() 
          + "/mturk/preview?groupId=" + hit.getHITTypeId() + System.getProperty("line.separator"));
    }
      
    return (HIT[])hits.toArray(new HIT[hits.size()]);      
  }
}
