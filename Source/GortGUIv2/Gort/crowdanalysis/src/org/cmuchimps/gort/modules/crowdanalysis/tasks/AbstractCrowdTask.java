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
package org.cmuchimps.gort.modules.crowdanalysis.tasks;

import org.cmuchimps.gort.modules.crowdanalysis.GortHITQuestion;
import com.amazonaws.mturk.addon.HITDataCSVReader;
import com.amazonaws.mturk.addon.HITDataCSVWriter;
import com.amazonaws.mturk.addon.HITDataInput;
import com.amazonaws.mturk.addon.HITDataOutput;
import com.amazonaws.mturk.addon.HITProperties;
import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.addon.QAPValidator;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.AsyncReply;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ValidationException;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.persistence.EntityManager;
import org.cmuchimps.gort.modules.crowdanalysis.GortHITCreation;
import org.cmuchimps.gort.modules.crowdanalysis.GortRequesterService;
import org.cmuchimps.gort.modules.dataobject.CrowdTask;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.helper.DateHelper;
import org.cmuchimps.gort.modules.helper.ISO8601Helper;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public abstract class AbstractCrowdTask {
    // borrows functions from RequesterService and modifies them
    
    private static final int MAX_BATCH = 500;     // maximum batch size for batch chunk
    public static final int LOAD_ALL = -1;
    
    protected final RequesterService service;
    protected final FileObject resultsFO;
    protected final FileObject mTurkPropertiesFO;
    protected final FileObject questionFO;
    protected final FileObject taskPropertiesFO;
    protected final FileObject inputFO;
    
    // variables to add tasks to the database
    protected final GortEntityManager gem;
    protected final EntityManager em;
    protected final List<CrowdTask> crowdTasks;

    public AbstractCrowdTask(FileObject resultsFO, FileObject mTurkPropertiesFO,
            FileObject questionFO, FileObject propertiesFO, FileObject inputFO,
            List<CrowdTask> crowdTasks, GortEntityManager gem, EntityManager em) {
        this.resultsFO = resultsFO;
        this.mTurkPropertiesFO = mTurkPropertiesFO;
        this.questionFO = questionFO;
        this.taskPropertiesFO = propertiesFO;
        this.inputFO = inputFO;
        this.gem = gem;
        this.em = em;
        this.crowdTasks = crowdTasks;
        
        //instead of the aws requester serivce use the Gort reviewer service
        //that allows the use of assignment and hit review policies
        //service = new RequesterService(new PropertiesClientConfig(mTurkPropertiesFO.getPath()));
        service = new GortRequesterService(new PropertiesClientConfig(mTurkPropertiesFO.getPath()));
    }
    
    public boolean hasEnoughFunds() {
        double balance = service.getAccountBalance();
        System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
        return balance > 0;
    }
    
    public GortHITCreation createHITs() {
        if (service == null) {
            System.out.println("Cannot create hits. No requestor service");
            return null;
        }
        
        if (questionFO == null || !questionFO.canRead()) {
            System.out.println("Crowd task question file is not valid.");
            return null;
        }
        
        if (taskPropertiesFO == null || !taskPropertiesFO.canRead()) {
            System.out.println("Crowd task properties file is not valid.");
            return null;
        }
        
        if (inputFO == null || !inputFO.canRead()) {
            System.out.println("Crowd task input file is not valid.");
            return null;
        }
        
        if (!hasEnoughFunds()) {
            System.out.println("Cannot create tasks. Not enough funds.");
            return null;
        }
        
        GortHITCreation retVal = new GortHITCreation();
        
        HIT[] hits = null;
        
        try {
            //Loading the input file.  The input file is a tab delimited file where the first row
            //defines the fields/variables and the remaining rows contain the values for each HIT.
            //Each row represents a unique HIT.  The SDK uses the Apache Velocity engine to merge
            //the input values into a templatized QAP file.
            //Please refer to http://velocity.apache.org for more details on this engine and
            //how to specify variable names.  Apache Velocity is fully supported so there may be
            //additional functionality you can take advantage of if you know the Velocity syntax.
            HITDataInput input = new HITDataCSVReader(inputFO.getPath());

            //Loading the question (QAP) file.  This QAP file contains Apache Velocity variable names where the values
            //from the input file are to be inserted.  Essentially the QAP becomes a template for the input file.
            //HITQuestion question = new HITQuestion(questionFO.getPath());
            // Treat html input as html
            HITQuestion question = new GortHITQuestion(questionFO.getPath());
            
            // Validate the question (QAP) against the XSD Schema before making the call.
            // If there is an error in the question, ValidationException gets thrown.
            // This method is extremely useful in debugging your QAP.  Use it often.
            //QAPValidator.validate(question.getQuestion());

            //Loading the HIT properties file.  The properties file defines two system qualifications that will
            //be used for the HIT.  The properties file can also be a Velocity template.  Currently, only
            //the "annotation" field is allowed to be a Velocity template variable.  This allows the developer
            //to "tie in" the input value to the results.
            HITProperties props = new HITProperties(taskPropertiesFO.getPath());
            
            // Create multiple HITs using the input, properties, and question files

            System.out.println("--[Loading HITs]----------");
            Date startTime = DateHelper.getUTC();
            System.out.println("  Start time: " + startTime);

            //The simpliest way to bulk load a large number of HITs where all details are defined in files.
            //When using this method, it will automatically create output files of the following type:
            //  - <your input file name>.success - A file containing the HIT IDs and HIT Type IDs of
            //                                     all HITs that were successfully loaded.  This file will
            //                                     not exist if there are no HITs successfully loaded.
            //  - <your input file name>.failure - A file containing the input rows that failed to load.
            //                                     This file will not exist if there are no failures.
            //The .success file can be used in subsequent operations to retrieve the results that workers submitted.
            HITDataOutput success = null;
            HITDataOutput failure = null;
            
            String successFile = null;
            String failureFile = null;
            
            if (resultsFO != null && resultsFO.canWrite()) {
                String filename = String.format("%s/%s_%s",
                        resultsFO.getPath(), inputFO.getName(), 
                        ISO8601Helper.toISO8601(System.currentTimeMillis(), 
                        TimeZone.getDefault().getID()) );
                System.out.println(filename);
                
                successFile = filename + ".success";
                failureFile = filename + ".failure";
                
                success = new HITDataCSVWriter(successFile);
                failure = new HITDataCSVWriter(failureFile);
            }
            
            //hits = service.createHITs(input, props, question, success, failure);
            hits = this.createHITs(input, props, question, LOAD_ALL, success, failure);
            
            // SMA output the successfile
            retVal.setHits(hits);
            retVal.setSuccessFile(successFile);
            retVal.setFailureFile(failureFile);
            retVal.setStart(startTime);
            
            System.out.println("--[End Loading HITs]----------");
            Date endTime = DateHelper.getUTC();
            System.out.println("  End time: " + endTime);
            System.out.println("--[Done Loading HITs]----------");
            System.out.println("  Total load time: "
                + (endTime.getTime() - startTime.getTime())/1000 + " seconds.");

            if (hits == null) {
              throw new Exception("Could not create HITs");
            }

        } catch (ValidationException e) {
            //The validation exceptions will provide good insight into where in the QAP has errors.  
            //However, it is recommended to use other third party XML schema validators to make 
            //it easier to find and fix issues.
            System.err.println("QAP contains an error: " + e.getLocalizedMessage());  
            e.printStackTrace();
        } catch (Exception e) {
          e.printStackTrace();
        }
        
        return retVal;
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
   * @return an array of HIT objects
   * @throws Exception
   */
  public HIT[] createHITs(HITDataInput input, HITProperties props, HITQuestion question, int numHITToLoad,
      HITDataOutput success, HITDataOutput failure) throws Exception {     
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
    System.out.println(String.format("Creating %d HITs with max memory %d", numRecords, Runtime.getRuntime().maxMemory()));

    int numBatches = numRecords / MAX_BATCH;
    for (int curBatch=0; curBatch<=numBatches; curBatch++) {
      int iStart = curBatch * MAX_BATCH;
      int iEnd = iStart + MAX_BATCH;
      if (iEnd > numRecords) {
        iEnd = numRecords;
      }

      System.out.println(String.format("Processing batch %d (%d to %d)", curBatch, iStart, iEnd)); 

      for (int i = iStart; i < iEnd; i++) {
        // Merge the input with the question
        // Start from the second line since the first line contains the field names
        Map<String, String> inputMap = input.getRowAsMap(i + 1);   

        // Merge the input with the properties
        props.setInputMap(inputMap);

        // we need to make sure to not create multiple hittypes for matching HITs
        // due to multithreaded calls being processed at the same time
        if (hitTypeForBatch == null) {
          hitTypeForBatch = service.registerHITType(
              props.getAutoApprovalDelay(), 
              props.getAssignmentDuration(), 
              props.getRewardAmount(), 
              props.getTitle(), 
              props.getKeywords(), 
              props.getDescription(), 
              props.getQualificationRequirements());
        }

        replies[i-iStart] = service.createHITAsync(
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
            null,       // assignmentReviewPolicy
            null,       // hitReviewPolicy
            null);      // async callback   
      }

      // wait for thread pool to finish processing these requests and evaluate results        
      for (int i = iStart; i < iEnd; i++) {
        try {
          hit = ((HIT[])replies[i-iStart].getResult())[0];
          hits.add(hit);
          
          // SMA add the hit to the database and also to the associated crowdtask
          // get the appropriate CrowdTask
          CrowdTask crowdTask = crowdTasks.get(i);
          
          /*
          // hits are assigned the crowdtask. will not need to call getHits
          if (crowdTask.getHits() == null) {
              crowdTask.setHits(new LinkedList<org.cmuchimps.gort.modules.dataobject.HIT>());
          }*/
          
          org.cmuchimps.gort.modules.dataobject.HIT gortHIT = 
                  org.cmuchimps.gort.modules.crowdanalysis.Utility.createDBHIT(gem, em, hit);
          
          if (gortHIT != null) {
              gortHIT.setType(this.getType());
              gortHIT.setInFlight(true);
              gortHIT.setCrowdTask(crowdTask);
              gem.updateEntity(em, gortHIT);
          }
          ///////////////////////////////////
          
          System.out.println("Created HIT " + (i + 1) + ": HITId=" + hit.getHITId());

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
            System.out.println("[ERROR] Error creating HIT " + (i+1) 
                + " (" + input.getRowValues(i+1)[0] + "): " + e.getLocalizedMessage());         
          }
          catch (ValidationException ve) {
            // Otherwise, log the validation exception in place of the service exception
            System.out.println("[ERROR] Error creating HIT " + (i+1) 
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

    if (hit != null) {
      // Print the URL at which the new HIT can be viewed at the end as well
      // so the user doesn't have to "scroll" up in case lots of HITs have been loaded
      System.out.println(System.getProperty("line.separator") + "You may see your HIT(s) with HITTypeId '" + hit.getHITTypeId() + "' here: ");
      System.out.println(System.getProperty("line.separator") + "  " + service.getWebsiteURL()
          + "/mturk/preview?groupId=" + hit.getHITTypeId() + System.getProperty("line.separator"));
    }
      
    return (HIT[])hits.toArray(new HIT[hits.size()]);      
  }
  
  public abstract String getType();
}
