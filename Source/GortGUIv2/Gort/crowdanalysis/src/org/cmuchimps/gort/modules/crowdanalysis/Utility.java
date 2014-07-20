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

import com.amazonaws.mturk.addon.HITDataCSVReader;
import com.amazonaws.mturk.addon.HITDataCSVWriter;
import com.amazonaws.mturk.addon.HITDataInput;
import com.amazonaws.mturk.addon.HITTypeResults;
import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.EntityManager;
import org.cmuchimps.gort.modules.dataobject.CrowdTask;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.helper.DateHelper;

/**
 *
 * @author shahriyar
 */
public class Utility {
    public static String sanitizeMTurkInput(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        
        // replace non ascii characters
        s = s.replaceAll("[^\\p{ASCII}]", "");
        
        // remove all new lines
        s = s.replaceAll("(\\r|\\n|\\t)", "");
        
        // change commas to html encoding
        s = s.replace(",",  "&#44");
        
        // remove all <p>
        s = s.replaceAll("\\<\\s*p\\s*\\>", "<br/><br/>");
        
        // change </p> to empty string
        s = s.replaceAll("\\<\\s*/p\\s*\\>", "");
        
        return s;
    }
    
    public static boolean writeResultsToCSV(RequesterService service,
            String successFilePath, String outputFilePath) {
        if (service == null) {
            return false;
        }
        
        if (successFilePath == null || successFilePath.isEmpty()) {
            return false;
        }
        
        if (outputFilePath == null || outputFilePath.isEmpty()) {
            return false;
        }
        
        //Loads the .success file containing the HIT IDs and HIT Type IDs of HITs to be retrieved.
        HITDataInput success = null;
        
        try {
            success = new HITDataCSVReader(successFilePath);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        
        //Retrieves the submitted results of the specified HITs from Mechanical Turk
        HITTypeResults results = service.getHITTypeResults(success);
        results.setHITDataOutput(new HITDataCSVWriter(outputFilePath));
        try {
            //Writes the submitted results to the defined output file.
            //The output file is a tab delimited file containing all relevant details
            //of the HIT and assignments.  The submitted results are included as the last set of fields
            //and are represented as tab separated question/answer pairs
            results.writeResults();
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        System.out.println("Results have been written to: " + outputFilePath);
        
        return true;
    }
    
    public static Assignment[] getSubmittedAssignments(RequesterService service, String hitId) {
        if (service == null) {
            return null;
        }
        
        if (hitId == null || hitId.isEmpty()) {
            return null;
        }
        
        Assignment[] assignments = null;
        
        try {
            assignments = service.getAllAssignmentsForHIT(hitId);
        } catch (Exception e) {
            System.out.println("Could not connect to receive HIT assignments.");
            //e.printStackTrace();
        }
        
        if (assignments == null || assignments.length <= 0) {
            return null;
        }
        
        List<Assignment> retVal = new LinkedList<Assignment>();
        
        //Only assignments that have been submitted will contain answer data
        for (Assignment a : assignments) {
            if (a == null) {
                continue;
            }
            
            if (a.getAssignmentStatus() == AssignmentStatus.Submitted) {
                retVal.add(a);
            }
            
        }
        
        return (retVal.size() > 0) ? retVal.toArray(new Assignment[retVal.size()]) : null;
    }
    
    public static org.cmuchimps.gort.modules.dataobject.Assignment createDBAssignment(GortEntityManager gem, EntityManager em, Assignment mturkAssignment) {
        if (gem == null || em == null || mturkAssignment == null) {
            return null;
        }
        
        org.cmuchimps.gort.modules.dataobject.Assignment retVal = new org.cmuchimps.gort.modules.dataobject.Assignment();
        
        retVal.setAnswer(mturkAssignment.getAnswer());
        retVal.setAssignmentId(mturkAssignment.getAssignmentId());
        retVal.setWorkerId(mturkAssignment.getWorkerId());
        retVal.setAssignmentStatus(mturkAssignment.getAssignmentStatus().getValue());
        retVal.setSubmission(mturkAssignment.getSubmitTime().getTime());
        gem.insertEntity(em, retVal);
        
        return retVal;
    }
    
    public static org.cmuchimps.gort.modules.dataobject.HIT createDBHIT(GortEntityManager gem, EntityManager em, HIT mturkHIT) {
        if (gem == null || em == null || mturkHIT == null) {
            return null;
        }
        
        //(String type, Date submission, String hitId, String hitTypeId, String hitGroupId, String hitLayoutId)
        org.cmuchimps.gort.modules.dataobject.HIT gortHIT = 
                new org.cmuchimps.gort.modules.dataobject.HIT(
                (mturkHIT.getCreationTime() != null) ? mturkHIT.getCreationTime().getTime() : DateHelper.getUTC(), 
                mturkHIT.getHITId(), mturkHIT.getHITTypeId(), 
                mturkHIT.getHITGroupId(), mturkHIT.getHITLayoutId());
        gortHIT.setInput(mturkHIT.getQuestion());
        gem.insertEntity(em, gortHIT);
        
        return gortHIT;
    }
    
    public static org.cmuchimps.gort.modules.dataobject.HIT[] createDBHITs(GortEntityManager gem, EntityManager em, HIT[] mturkHITs) {
        if (gem == null || em == null || mturkHITs == null) {
            return null;
        }
        
        if (mturkHITs.length <= 0) {
            return null;
        }
        
        List<org.cmuchimps.gort.modules.dataobject.HIT> retVal = new LinkedList<org.cmuchimps.gort.modules.dataobject.HIT>();
        
        for (HIT h : mturkHITs) {
            org.cmuchimps.gort.modules.dataobject.HIT gortHIT = createDBHIT(gem, em, h);
            
            if (gortHIT == null) {
                continue;
            }
            retVal.add(gortHIT);
        }
        
        return (retVal.size() > 0) ? 
                retVal.toArray(new org.cmuchimps.gort.modules.dataobject.HIT[retVal.size()]) :
                null;
    }
    
    public static void assignCrowdTaskHITs(GortEntityManager gem, EntityManager em,
            CrowdTask crowdTask, org.cmuchimps.gort.modules.dataobject.HIT[] hits) {
        if (gem == null || em == null || crowdTask == null || hits == null) {
            return;
        }
        
        for (org.cmuchimps.gort.modules.dataobject.HIT h : hits) {
            if (h == null) {
                continue;
            }
            
            h.setCrowdTask(crowdTask);
            gem.updateEntity(em, h);
            
            /*
            if (crowdTask.getHits() == null) {
                crowdTask.setHits(new LinkedList<org.cmuchimps.gort.modules.dataobject.HIT>());
            }
            
            crowdTask.getHits().add(h);
            */
        }
        
        //gem.updateEntity(crowdTask);
    }
}
