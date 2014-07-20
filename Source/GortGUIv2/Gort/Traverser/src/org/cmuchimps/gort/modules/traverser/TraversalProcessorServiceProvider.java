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
package org.cmuchimps.gort.modules.traverser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import javax.persistence.EntityManager;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.ProjectUtility;
import org.cmuchimps.gort.api.gort.TraversalProcessorService;
import org.cmuchimps.gort.api.gort.WebInfoService;
import org.cmuchimps.gort.api.gort.heuristic.AbstractDynamicHeuristic;
import org.cmuchimps.gort.api.gort.heuristic.HeuristicService;
import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.Heuristic;
import org.cmuchimps.gort.modules.dataobject.History;
import org.cmuchimps.gort.modules.dataobject.Server;
import org.cmuchimps.gort.modules.dataobject.State;
import org.cmuchimps.gort.modules.dataobject.TaintLog;
import org.cmuchimps.gort.modules.dataobject.Traversal;
import org.cmuchimps.gort.modules.dataobject.WhoisRecord;
import org.cmuchimps.gort.modules.helper.RegexHelper;
import org.cmuchimps.gort.modules.helper.TaintHelper;
import org.cmuchimps.gort.modules.helper.URLHelper;
import org.netbeans.api.progress.aggregate.AggregateProgressFactory;
import org.netbeans.api.progress.aggregate.AggregateProgressHandle;
import org.netbeans.api.progress.aggregate.ProgressContributor;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.RequestProcessor;

/**
 *
 * @author shahriyar
 */
public class TraversalProcessorServiceProvider extends TraversalProcessorService {

    private final static boolean DEBUG = true;
    private final static boolean SET_NON_TRANSMISSION_TAINTLOG_STATES = false;
    private final static int MATCHER_TRUNCATION_LENGTH = 128;
    private final static long ACCEPTED_TAINTLOG_TIME_DIFF = 30 * 1000; // 20 msec
    private final static String PROGRESS_LABEL = "Processing %s results";
    private final static int PROCESS_DATA_COMPILE_TAINTLOGS_WORK_UNITS = 100;
    private final static int PROCESS_DATA_COMPUTE_HEURISTICS_WORK_UNITS = 100;
    private final static int PROCESS_GRAPH_WORK_UNITS = 100;
    
    public TraversalProcessorServiceProvider(Project project) {
        super(project);
    }

    @Override
    public Boolean isProcessed(FileObject traversal) {
        if (project == null) {
            return null;
        }
        
        if (traversal == null) {
            return null;
        }
        
        String name = traversal.getNameExt();
        
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
        
        if (gds == null) {
            return null;
        }
        
        GortEntityManager gem = gds.getGortEntityManager();
        
        if (gem == null) {
            return null;
        }
        
        Traversal t = gem.selectTraversal(name);
        
        return Boolean.TRUE.equals(t.getProcessed());
    }
    
    @Override
    public void processTraversal(FileObject traversal, boolean redo) {
        processTraversalInThread(traversal, redo);
    }
    
    private void processTraversalInThread(final FileObject traversalFO, boolean redo) {
        if (project == null) {
            return;
        }
        
        if (traversalFO == null) {
            return;
        }
        
        String name = traversalFO.getNameExt();
        
        if (name == null || name.isEmpty()) {
            return;
        }
        
        GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
        
        if (gds == null) {
            return;
        }
        
        final GortEntityManager gem = gds.getGortEntityManager();
        
        if (gem == null) {
            return;
        }
        
        final Traversal traversal = gem.selectTraversal(name);
        
        if (traversal == null) {
            return;
        }
        
        // if we are not re-processing and the traversal has been processed
        if (!redo && Boolean.TRUE.equals(traversal.getProcessed())) {
            return;
        }
        
        final CountDownLatch latch = new CountDownLatch(2);
        
        final ProgressContributor compileTaintLogsPC = 
                AggregateProgressFactory.createProgressContributor("Compile Taint Logs Processor");
        
        final ProgressContributor computeHeuristicsPC = 
                AggregateProgressFactory.createProgressContributor("Compute Heuristics Processor");
        
        final ProgressContributor graphPC =
                AggregateProgressFactory.createProgressContributor("Graph Processor");
        
        ProgressContributor[] pcs = {compileTaintLogsPC, computeHeuristicsPC, graphPC};
        
        final AggregateProgressHandle aph = 
                AggregateProgressFactory.createHandle(
                String.format(PROGRESS_LABEL, traversalFO.getNameExt()), 
                pcs, null, null);
        
        aph.start();
        compileTaintLogsPC.start(PROCESS_DATA_COMPILE_TAINTLOGS_WORK_UNITS);
        computeHeuristicsPC.start(PROCESS_DATA_COMPUTE_HEURISTICS_WORK_UNITS);
        graphPC.start(PROCESS_GRAPH_WORK_UNITS);
        
        RequestProcessor rp = RequestProcessor.getDefault();
        
        // process the graph and the traversal in separate threads as they are
        // independent of each other
        rp.post(new Runnable() {

            @Override
            public void run() {
                processData(traversalFO, latch, compileTaintLogsPC, computeHeuristicsPC);
                compileTaintLogsPC.progress(PROCESS_DATA_COMPILE_TAINTLOGS_WORK_UNITS);
                compileTaintLogsPC.finish();
                computeHeuristicsPC.progress(PROCESS_DATA_COMPUTE_HEURISTICS_WORK_UNITS);
                computeHeuristicsPC.finish();
            }
            
        });
        
        rp.post(new Runnable() {

            @Override
            public void run() {
                processGraph(traversalFO, latch, graphPC);
                graphPC.progress(PROCESS_GRAPH_WORK_UNITS);
                graphPC.finish();
            }
            
        });
        
        rp.post(new Runnable() {

            @Override
            public void run() {
                try {
                    latch.await();
                    //p.finish();
        
                    // set the traversal as already processed
                    traversal.setProcessed(true);
                    gem.updateEntity(traversal);
        
                    // add a new event to the lookup to indicate the processing is done
                    // content.set(Arrays.asList(new MainTraversalChangeEvent(retVal)), null);
                    System.out.println("Updating TraversalProcessor lookup...");
                    content.set(
                            Arrays.asList(new TraversalProcessedChangeEvent(traversalFO)), null);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            
        });

    }
    
    private void processData(FileObject traversalFO, CountDownLatch latch,
            ProgressContributor compileTaintLogsPC, ProgressContributor computeHeuristicsPC) {
        System.out.println("Processing traversal database...");
        EntityManager em = null;
        try {
            GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
            
            if (gds == null) {
                return;
            }
            
            GortEntityManager gem = gds.getGortEntityManager();
            
            if (gem == null) {
                return;
            }
            
            em = gem.getEntityManager();
            
            Traversal traversal = gem.selectTraversal(em, traversalFO.getNameExt());
            
            if (traversal == null) {
                return;
            }
            
            List<TaintLog> taintLogs = traversal.getTaintLogs();
            
            if (taintLogs == null || taintLogs.isEmpty()) {
                return;
            }
            
            // Also get the associated history elements;
            List<History> history = traversal.getHistory();
            
            if (history == null || history.isEmpty()) {
                return;
            }
            
            // on first pass compile the taintlog data
            processDataTaintLogCompilePass(compileTaintLogsPC, gem, em,
                    traversal, history, taintLogs);
            
            // on the second pass compute the heuristics
            processDataDynamicHeuristicPass(computeHeuristicsPC, gem, em,
                    traversal, history, taintLogs);
            
        } finally {
            System.out.println("Processing traversal database done.");
            GortEntityManager.closeEntityManager(em);
            latch.countDown();
        }
    }
    
    
    private void processDataDynamicHeuristicPass(ProgressContributor computeHeuristicsPC,
            GortEntityManager gem, EntityManager em, Traversal traversal,
            List<History> history, List<TaintLog> taintLogs) {
        // go over taintlogs and go over history and execute the heuristics
        HeuristicService hs = HeuristicService.getDefault();
        
        if (hs == null) {
            return;
        }
        
        // produce a new set of heuristics for this computation. Note dynamic
        // heuristics hold state, so we always need a new instance for computation
        Collection<? extends AbstractDynamicHeuristic> c = hs.getDynamicHeuristics();
        
        List<AbstractDynamicHeuristic> heuristics = new LinkedList<AbstractDynamicHeuristic>();
        
        // initialize the heuristics
        if (c == null || c.isEmpty()) {
            return;
        } else {
            for (AbstractDynamicHeuristic h : c) {
                if (h == null) {
                    continue;
                }
                
                heuristics.add(h.getInstance(project,
                        ProjectUtility.getAPK(project, traversal),
                        traversal));
            }
        }
        
        if (heuristics == null || heuristics.isEmpty()) {
            return;
        }
        
        // call the initialization
        for (AbstractDynamicHeuristic h : heuristics) {
            h.init();
        }
        
        // current just supports onTransmissionTaintLog and onStateChange
        int numUnits = taintLogs.size() + history.size();
        double index = 0.0;
        
        for (TaintLog taintLog: taintLogs) {
            index += 1;
            computeHeuristicsPC.progress((int)((index / numUnits) * PROCESS_DATA_COMPUTE_HEURISTICS_WORK_UNITS));
            
            if (taintLog == null) {
                continue;
            }
            
            String type = taintLog.getType();
            
            if (TaintHelper.isTransmissionTaintLog(type)) {
                for (AbstractDynamicHeuristic h : heuristics) {
                    h.onTransmissionTaintLog(taintLog);
                }
            }
        }
        
        // go over history elements as well
        // TODO: the following is more onHistory rather than onStateChange
        for (History h : history) {
            index += 1;
            computeHeuristicsPC.progress((int)((index / numUnits) * PROCESS_DATA_COMPUTE_HEURISTICS_WORK_UNITS));
            
            if (h == null) {
                continue;
            }
            
            for (AbstractDynamicHeuristic heuristic: heuristics) {
                heuristic.onStateChange(h);
            }
        }
        
        App app = traversal.getApp();
        
        // get the output for each of the heuristics and place them in the database
        for (AbstractDynamicHeuristic heuristic: heuristics) {
            System.out.println("Finished processing dynamic heuristic: " + heuristic.getName());
            
            Boolean result = heuristic.output();
            
            System.out.println("Heuristic output is: " + result);
            
            Heuristic heuristicResult = gem.selectHeuristic(em, app.getId(), traversal.getId(), heuristic.getClass().getName());
            
            if (heuristicResult == null) {
                heuristicResult = new Heuristic(heuristic.getName(),
                        heuristic.getSummary(),
                        heuristic.getDescription(),
                        heuristic.getConcernLevel(),
                        heuristic.getType(),
                        heuristic.getClass().getName(),
                        result,
                        new Date());
                heuristicResult.setApp(app);
                heuristicResult.setTraversal(traversal);
            } else {
                heuristicResult.setName(heuristic.getName());
                heuristicResult.setSummary(heuristic.getSummary());
                heuristicResult.setDescription(heuristic.getDescription());
                heuristicResult.setConcernLevel(heuristic.getConcernLevel());
                heuristicResult.setResult(result);
                heuristicResult.setTimestamp(new Date());
                heuristicResult.setType(heuristic.getType());
                // app and traversal are already set
            }
            
            // clear out old states associated with the heuristics and put new ones
            List<State> associatedStates = heuristicResult.getStates();
            
            if (associatedStates == null) {
                heuristicResult.setStates(new LinkedList<State>());
                associatedStates = heuristicResult.getStates();
            }
            
            // clear out old associated states
            associatedStates.clear();
            
            LinkedHashSet<Integer> stateIds = heuristic.getAssociatedStateIds();
            
            if (stateIds != null && stateIds.size() > 0) {
                for (Integer id : stateIds) {
                    if (id == null) {
                        continue;
                    }
                    
                    State state = gem.selectState(em, id);
                    
                    if (state == null) {
                        continue;
                    }
                    
                    associatedStates.add(state);
                }
            }
            
            gem.updateEntity(em, heuristicResult);
        }
    }
    
    private void processDataTaintLogCompilePass(ProgressContributor compileTaintLogsPC,
            GortEntityManager gem, EntityManager em, Traversal traversal,
            List<History> history, List<TaintLog> taintLogs) {
        
        WebInfoService wis = WebInfoService.getDefault();
        
        History prevHistoryElement = null;
        History currentHistoryElement = null;

        Iterator<History> iterator = history.iterator();

        Matcher m = null;

        int numTaintLogs = taintLogs.size();
        double index = 0.0;

        //... go over the taintlogs and find the matching histories
        for (TaintLog taintLog : taintLogs) {
            index += 1;
            compileTaintLogsPC.progress((int)((index / numTaintLogs) * PROCESS_DATA_COMPILE_TAINTLOGS_WORK_UNITS));

            if (taintLog == null) {
                continue;
            }

            // set the server, tainttag, type, etc...
            String message = taintLog.getMessage();

            if (message != null && !message.isEmpty()) {
                if (message.length() > MATCHER_TRUNCATION_LENGTH) {
                    message = message.substring(0, MATCHER_TRUNCATION_LENGTH);
                }
                setTaintIP(taintLog, message);
                setTaintServer(wis, gem, em, traversal, taintLog);
                setTaintTag(taintLog, message);
                setTaintType(taintLog, message);
            }

            if (taintLog.getTimestamp() == null) {
                // update the taintLog
                gem.updateEntity(em, taintLog);
                continue;
            }

            System.out.println("Processing taintLog with timestamp " + taintLog.getTimestamp().toGMTString());

            // find the history element which occurs just after the curren taintLog
            if (currentHistoryElement == null || !currentHistoryElement.getStartTimestamp().after(taintLog.getTimestamp())) {
            // set the state
                while (iterator.hasNext()) {
                    prevHistoryElement = currentHistoryElement;
                    currentHistoryElement = iterator.next();
                    if (currentHistoryElement.getStartTimestamp().after(taintLog.getTimestamp())) {
                        break;
                    }
                }
            }
            
            // if we have reached the end of the iterator set the prev and current histories to be the same
            if (!iterator.hasNext()) {
                prevHistoryElement = currentHistoryElement;
            }

            if (taintLog.isTransmission() || SET_NON_TRANSMISSION_TAINTLOG_STATES) {
                setTaintState(gem, em, taintLog, prevHistoryElement, currentHistoryElement);
            }

            // update the taintLog
            gem.updateEntity(em, taintLog);
        }
    }
    
    private void setTaintIP(TaintLog t, String message) {
        if (t.getIp() == null) {
            Matcher m = RegexHelper.ipMatcher(message);

            if (m.find()) {
                t.setIp(m.group());
            }
        }
    }
    
    private void setTaintServer(WebInfoService wis, GortEntityManager gem, EntityManager em,
            Traversal traversal, TaintLog taintLog) {
        String ip = taintLog.getIp();
        
        if (ip == null || ip.isEmpty()) {
            return;
        }
        
        Server s = taintLog.getServer();
        
        if (s != null) {
            return;
        }
        
        // select or create a server for the associated ip address and traversal
        s = gem.selectServer(em, traversal.getId(), ip);
        
        if (s != null) {
            taintLog.setServer(s);
            return;
        }
        
        // create a server
        //s = new Server(String ip, String hostname, String name, String address, String city, String country, String phone, String email)
        if (wis == null) {
            return;
        }
        
        WhoisRecord whois = wis.serverInfo(ip);
        String hostname = URLHelper.getHost(ip);
        
        if (whois != null) {    
            hostname = (hostname != null) ? hostname : whois.DomainName;
            if (whois.RegistryData != null && whois.RegistryData.Registrant != null) {
                s = new Server(ip, hostname,
                        whois.RegistryData.Registrant.Name,
                        whois.RegistryData.Registrant.Address,
                        whois.RegistryData.Registrant.City,
                        whois.RegistryData.Registrant.StateProv,
                        whois.RegistryData.Registrant.Country,
                        whois.RegistryData.Registrant.PostalCode);
            } else {
                s = new Server(ip, whois.DomainName);
            }
        } else {
            s = new Server(ip, hostname);
        }
        
        // set the traversal for the server
        s.setTraversal(traversal);
        
        if (s.getTaintLogs() == null) {
            s.setTaintLogs(new LinkedList<TaintLog>());
        }
        
        s.getTaintLogs().add(taintLog);
        
        em.getTransaction().begin();
        em.merge(taintLog);
        em.persist(s);
        em.getTransaction().commit();
    }
    
    private void setTaintTag(TaintLog t, String message) {
        if (t.getTainttag() == null || t.getTainttag() <= 0) {
        
            Matcher m = RegexHelper.tagMatcher(message);

            if (m.find()) {
                String s = m.group().replaceFirst("tag 0x", "");
                t.setTainttag(Integer.parseInt(s, 16));
            }
        }
    }
    
    // covers taints for TaintDroid 2.+
    private void setTaintType(TaintLog t, String message) {
        if (t.getType() == null || t.getTag().isEmpty()) {
            if (message.indexOf(TaintHelper.TYPE_OS_FILE_SYSTEM) >= 0) {
                t.setType(TaintHelper.TYPE_OS_FILE_SYSTEM);
            } else if (message.indexOf(TaintHelper.TYPE_OS_NETWORK) >= 0) {
                t.setType(TaintHelper.TYPE_OS_NETWORK);
            } else if (message.indexOf(TaintHelper.TYPE_SSL_OUTPUT) >= 0) {
                t.setType(TaintHelper.TYPE_SSL_OUTPUT);
            }
        }
    }
    
    private void setTaintState(GortEntityManager gem, EntityManager em, TaintLog taintLog, History prev, History current) {
        if (DEBUG) {
            System.out.println("setTaintState called.");
        }
        
        if (current == null) {
            return;
        }
        
        if (DEBUG) {
            System.out.println(
                    String.format(
                    "Taint timestamp: %s",
                    taintLog.getTimestamp().toGMTString()));
            if (prev != null && prev.getStartTimestamp() != null) {
                System.out.println("Prev timestamp: " + prev.getStartTimestamp().toGMTString());
            }
            if (current != null && current.getStartTimestamp() != null) {
                System.out.println("Current timestamp: " + current.getStartTimestamp().toGMTString());
            }
        }
        
        long diff = 0;
                
        // note if the time different is greater than n seconds
        // we have to skip the taintLog. specifically if it occured
        // way before we started the app or way after. it usually takes
        // 10-15s to read a screen so if the difference is much longer
        // should not be counted
        if (prev == null) {
            if (DEBUG) {
                System.out.println("Previous state is null.");
            }
            // just starting the analysis
            diff = Math.abs(current.getStartTimestamp().getTime() - taintLog.getTimestamp().getTime());

            if (diff <= ACCEPTED_TAINTLOG_TIME_DIFF) {
                taintLog.setState(current.getStartState());
            }
        } else if (prev == current) {
            if (DEBUG) {
                System.out.println("Previous and current state are the same.");
            }
            // reached the end of history
            diff = Math.abs(current.getEndTimestamp().getTime() - taintLog.getTimestamp().getTime());

            if (diff <= ACCEPTED_TAINTLOG_TIME_DIFF) {
                taintLog.setState(current.getEndState());
            }
        } else {
            if (DEBUG) {
                System.out.println("Previous and current state are different.");
            }
            
            // timestamp of the taintlog should be bigger or equal to the 
            // starttimestamp and less than the endtimestamp
            if (taintLog.getTimestamp().before(prev.getStartTimestamp()) ||
                    taintLog.getTimestamp().after(prev.getEndTimestamp())) {
                System.err.print("TaintLog timestamp should be in between start and end time");
                return;
            }
            
            // taintLog happened in between two history elements

            State prevStartState = prev.getStartState();
            State prevEndState = prev.getEndState();
            /*
            State currentStartState = current.getStartState();
            State currentEndState = current.getEndState();
            */
            
            // if either state is null assign state to one that is not null
            if (prevStartState == null && prevEndState == null) {
                // both start and end states should not be null
                return;
            }
            
            if (prevStartState == null) {
                taintLog.setState(prevEndState);
            } else if (prevEndState == null) {
                taintLog.setState(prevStartState);
            } else if (prevStartState == prevEndState) {
                taintLog.setState(prevStartState);
            } else {
                // neither state is null and the states are different
                // use the interaction time to decide
                Date interactionTimestamp = prev.getInteractionTimestamp();
                
                if (interactionTimestamp == null) {
                    // taintlog most likely to be associated with the current state
                    // as it takes more time to read the End state and decide
                    // what to do
                    taintLog.setState(prevEndState);
                } else {
                    if (taintLog.getTimestamp().before(interactionTimestamp)) {
                        taintLog.setState(prevStartState);
                    } else if (taintLog.getTimestamp().after(interactionTimestamp)) {
                        taintLog.setState(prevEndState);
                    } else {
                        taintLog.setState(prevStartState);
                    }
                }
            }
            
            /*
            // this is the default go to
            if (prevEndState != null) {
                taintLog.setState(prevEndState);
            } else if (prevStartState != null) {
                taintLog.setState(prevStartState);
            } else if (currentStartState != null) {
                taintLog.setState(currentStartState);
            } else if (currentEndState != null) {
                taintLog.setState(currentEndState);
            }*/
        }
        
        State assignedState = taintLog.getState();
        Server assignedServer = taintLog.getServer();
        
        if (assignedState != null && assignedServer != null) {
            if (!assignedState.getServers().contains(assignedServer)) {
                assignedState.getServers().add(assignedServer);
                gem.updateEntity(em, assignedState);
            }
        }
    }
    
    private void processGraph(FileObject fo, CountDownLatch latch, ProgressContributor pc) {
        System.out.println("Processing traversal graph...");
        EntityManager em = null;
        try {
            GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
            
            if (gds == null) {
                return;
            }
            
            GortEntityManager gem = gds.getGortEntityManager();
            
            if (gem == null) {
                return;
            }
            
            em = gem.getEntityManager();
            
            Traversal t = gem.selectTraversal(em, fo.getNameExt());
            
        } finally {
            System.out.println("Processing traversal graph done.");
            GortEntityManager.closeEntityManager(em);
            latch.countDown();
        }
    }
    
}
