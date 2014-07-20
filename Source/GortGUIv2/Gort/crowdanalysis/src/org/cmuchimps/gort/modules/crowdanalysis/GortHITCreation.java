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

import com.amazonaws.mturk.requester.HIT;
import java.util.Date;

/**
 *
 * @author shahriyar
 */
public class GortHITCreation {
    private String successFile;
    private String failureFile;
    private HIT[] hits;
    private Date start;
    private Date end;

    public GortHITCreation() {
    }
    
    public GortHITCreation(String successFile, String failureFile, HIT[] hits) {
        this();
        this.successFile = successFile;
        this.failureFile = failureFile;
        this.hits = hits;
    }

    public GortHITCreation(String successFile, String failureFile, HIT[] hits, Date start, Date end) {
        this(successFile, failureFile, hits);
        this.start = start;
        this.end = end;
    }

    public String getSuccessFile() {
        return successFile;
    }

    public void setSuccessFile(String successFile) {
        this.successFile = successFile;
    }

    public String getFailureFile() {
        return failureFile;
    }

    public void setFailureFile(String failureFile) {
        this.failureFile = failureFile;
    }

    public HIT[] getHits() {
        return hits;
    }

    public void setHits(HIT[] hits) {
        this.hits = hits;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }
    
}
