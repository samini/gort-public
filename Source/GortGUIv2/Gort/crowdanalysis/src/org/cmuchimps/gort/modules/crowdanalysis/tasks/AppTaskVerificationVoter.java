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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import org.cmuchimps.gort.modules.helper.MathHelper;

/**
 *
 * @author shahriyar
 */
public class AppTaskVerificationVoter {

    private LinkedHashMap<String, List<Integer>> votes = null;
    
    public AppTaskVerificationVoter() {
        votes = new LinkedHashMap<String, List<Integer>>();
    }
    
    public static AppTaskVerificationVoter getInstance(String[] candidates) {
        if (candidates == null || candidates.length <= 0) {
            return new AppTaskVerificationVoter();
        }
        
        AppTaskVerificationVoter retVal = new AppTaskVerificationVoter();
        
        for (String s : candidates) {
            retVal.addCandidate(s);
        }
        
        return retVal;
    }
    
    public void addCandidate(String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        
        if (!votes.containsKey(s)) {
            votes.put(s, new LinkedList<Integer>());
        }
    }
    
    public void addVote(String s, String vote) {
        if (vote == null || vote.isEmpty()) {
            return;
        }
        
        try {
            Integer v = Integer.parseInt(vote);
            addVote(s, v);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
    
    public void addVote(String s, Integer v) {
        if (s == null || s.isEmpty() || v == null) {
            return;
        }
        
        if (!votes.containsKey(s)) {
            votes.put(s, new LinkedList<Integer>());
        }
        
        votes.get(s).add(v);
    }
    
    // uses a Majority Judgment algorithm based on the median
    // selects the first candidate if two candidates have the same max median value
    // returns the name of the candidate.
    public String getWinningCandidate() {
        HashMap<String, Double> medians = new HashMap<String, Double>();
        
        for (String s : votes.keySet()) {
            List<Integer> candidateVotes = votes.get(s);
            
            if (candidateVotes == null || candidateVotes.isEmpty()) {
                continue;
            }
            
            Integer[] candidateVotesArray = candidateVotes.toArray(new Integer[candidateVotes.size()]);
            
            Double median = MathHelper.median(candidateVotesArray);
            
            if (median == null) {
                continue;
            }
            
            medians.put(s, median);
        }
        
        if (medians.size() <= 0) {
            return null;
        }
        
        // not the most efficient way of finding max, but arrays have a maximum length 10
        // find the largest median
        List<Double> mediansList = new ArrayList<Double>();
        mediansList.addAll(medians.values());
        Collections.sort(mediansList);
        
        // get the max value
        double maxMedian = mediansList.get(mediansList.size() - 1);
        
        for (String s : medians.keySet()) {
            if (medians.get(s) == null) {
                continue;
            }
            
            if (medians.get(s).doubleValue() == maxMedian) {
                return s;
            }
        }
        
        return null;
    }
}
