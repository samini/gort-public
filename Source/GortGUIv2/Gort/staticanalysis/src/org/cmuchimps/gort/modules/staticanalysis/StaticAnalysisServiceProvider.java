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
package org.cmuchimps.gort.modules.staticanalysis;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import org.cmuchimps.gort.api.gort.GortDatabaseService;
import org.cmuchimps.gort.api.gort.ProgressStatusService;
import org.cmuchimps.gort.api.gort.ProjectDirectoryService;
import org.cmuchimps.gort.api.gort.PythonScriptService;
import org.cmuchimps.gort.api.gort.WebInfoService;
import org.cmuchimps.gort.api.gort.analysis.StaticAnalysisService;
import org.cmuchimps.gort.api.gort.heuristic.AbstractStaticHeuristic;
import org.cmuchimps.gort.api.gort.heuristic.HeuristicService;
import org.cmuchimps.gort.modules.dataobject.App;
import org.cmuchimps.gort.modules.dataobject.GortEntityManager;
import org.cmuchimps.gort.modules.dataobject.Heuristic;
import org.cmuchimps.gort.modules.dataobject.Progress;
import org.cmuchimps.gort.modules.dataobject.Screenshot;
import org.cmuchimps.gort.modules.helper.HashHelper;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author shahriyar
 */
public class StaticAnalysisServiceProvider extends StaticAnalysisService {

    private static final boolean DEBUG = true;
    
    public StaticAnalysisServiceProvider(Project project) {
        super(project);
    }
    
    @Override
    public void analyze(FileObject apk) {
        if (project == null) {
            return;
        }
        
        if (apk == null) {
            return;
        }
        
        analyzeInThread(apk);
        
    }
    
    // TODO: Note this function can be chupped up into multiple threads
    // to improve performance
    private void analyzeInThread(final FileObject apk) {
        Thread t = new Thread() {
            @Override
            public void run() {
                System.out.println("Performing static analysis on " + apk.getNameExt());
        
                // Step 0: Obtain the application index and path and database connection url
                GortDatabaseService gds = project.getLookup().lookup(GortDatabaseService.class);
                ProjectDirectoryService pds = project.getLookup().lookup(ProjectDirectoryService.class);
                
                if (gds == null) {
                    return;
                }
                
                GortEntityManager gem = gds.getGortEntityManager();
                
                if (gem == null) {
                    return;
                }
                
                EntityManager em = null;
                
                try {
                    em = gem.getEntityManager();
                    App app = gem.selectApp(em, apk.getNameExt());

                    if (app == null) {
                        return;
                    }

                    Integer id = app.getId();

                    if (id == null || id < 0) {
                        return;
                    }

                    String dbURL = gds.dbConnectionURL();

                    if (dbURL == null || dbURL.isEmpty()) {
                        return;
                    }

                    // Step 1: put the file size of the apk in the table
                    System.out.println("Computing the apk file size...");
                    if (app.getSize() == null || app.getSize().intValue() <= 0) {
                        Long size = apk.getSize();
                        app.setSize(size);
                        gem.updateEntity(em, app);
                    }
                    System.out.println("Done");

                    // Step 3: put the md5 of the apk if it doesn't exist
                    System.out.println("Computing the apk md5...");
                    if (app.getMd5() == null || app.getMd5().isEmpty()) {
                        String md5 = HashHelper.md5(apk);
                        app.setMd5(md5);
                        gem.updateEntity(em, app);
                    }
                    System.out.println("Done");

                    // Step 4: run basic info python algorithm on the apk
                    PythonScriptService pyss = PythonScriptService.getDefault();
                    if (pyss != null) {
                        System.out.println("Running python basic info script...");
                        pyss.appBasicInfo(dbURL, id, apk);
                        System.out.println("Done");


                        // Step 5: run dalvik info on the apk to figure out 
                        System.out.println("Running python dalvik info script...");
                        pyss.appDalvikInfo(dbURL, id, apk);
                        System.out.println("Done");
                    }

                    // Step 6: if the app does not have description. Set app description
                    if (app.getDescription() == null || app.getDescription().isEmpty()) {
                        System.out.println("\nObtaining application description");
                        String packageName = app.getPackage();

                        System.out.println("App package name: " + packageName);

                        if (packageName != null && !packageName.isEmpty()) {
                            WebInfoService wis = WebInfoService.getDefault();

                            if (wis != null) {
                                String desc = wis.appDescription(packageName);
                                if (desc != null) {
                                    System.out.println("Application description: " + desc);
                                    app.setDescription(desc);
                                    gem.updateEntity(em, app);
                                }
                            }
                        }
                    }
                    
                    // Step 7: if app does not have name. get its name
                    if (app.getName() == null || app.getName().isEmpty()) {
                        System.out.println("\nObtaining application name");
                        String packageName = app.getPackage();

                        System.out.println("App package name: " + packageName);

                        if (packageName != null && !packageName.isEmpty()) {
                            WebInfoService wis = WebInfoService.getDefault();

                            if (wis != null) {
                                String name = wis.appName(packageName);
                                if (name != null) {
                                    System.out.println("Application name: " + name);
                                    app.setName(name);
                                    gem.updateEntity(em, app);
                                }
                            }
                        }
                    }
                    
                    // if app doesn't have category or developer also obtain those
                    if (app.getCategory() == null || app.getCategory().isEmpty()) {
                        System.out.println("\nObtaining application category");
                        String packageName = app.getPackage();
                        if (packageName != null && !packageName.isEmpty()) {
                            WebInfoService wis = WebInfoService.getDefault();

                            if (wis != null) {
                                String category = wis.appCategory(packageName);
                                if (category != null) {
                                    System.out.println("Application category: " + category);
                                    app.setCategory(category);
                                    gem.updateEntity(em, app);
                                }
                            }
                        }
                    }
                    
                    if (app.getDeveloper() == null || app.getDeveloper().isEmpty()) {
                        System.out.println("\nObtaining application developer");
                        String packageName = app.getPackage();
                        if (packageName != null && !packageName.isEmpty()) {
                            WebInfoService wis = WebInfoService.getDefault();

                            if (wis != null) {
                                String developer = wis.appDeveloper(packageName);
                                if (developer != null) {
                                    System.out.println("Application developer: " + developer);
                                    app.setDeveloper(developer);
                                    gem.updateEntity(em, app);
                                }
                            }
                        }
                    }

                    // Step 8: Download screenshots for the app
                    app.getScreenshots().size(); // initialize the screenshots (lazy loading)
                    if (app.getScreenshots() == null || app.getScreenshots().isEmpty()) {
                        System.out.println("\nGetting screenshots for the app");
                        String packageName = app.getPackage();
                        if (packageName != null && !packageName.isEmpty()) {
                            WebInfoService wis = WebInfoService.getDefault();

                            if (wis != null && pds != null) {
                                 // check if the app has screenshots associated with it
                                List<FileObject> downloadedScreenshots =
                                        wis.downloadAppScreenshots(pds.getScreenshotsDir(true), app.getPackage());

                                if (downloadedScreenshots != null && !downloadedScreenshots.isEmpty()) {
                                    for (FileObject screenshot : downloadedScreenshots) {
                                        if (screenshot == null || !screenshot.canRead()) {
                                            continue;
                                        }

                                        FileObject parent = screenshot.getParent();

                                        String relativePath = (parent == null) ? screenshot.getNameExt() :
                                                String.format("%s%s%s", parent.getName(), 
                                                File.separator,
                                                screenshot.getNameExt());

                                        Screenshot s = new Screenshot(relativePath);
                                        gem.insertEntity(em, s);
                                        app.getScreenshots().add(s);
                                    }

                                    gem.updateEntity(em, app);
                                }
                            }
                        }
                    }

                    // Stp 9: run the static heuristics
                    HeuristicService hs = HeuristicService.getDefault();
                    if (hs != null) {
                        // TODO: combine with the database
                        Collection<? extends AbstractStaticHeuristic> c = hs.getStaticHeuristics();
                        if (c == null) {
                            System.out.println("Static heuristic collection is null.");
                        } else if (c.isEmpty()) {
                            System.out.println("Static heuristic collection is empty.");
                        } else {
                            for (AbstractStaticHeuristic h : c) {
                                if (h == null) {
                                    continue;
                                }

                                System.out.println("Running static heuristic: " + h.getName());
                                Boolean result = h.output(project, apk);
                                System.out.println("Result: " + result);

                                // update the changes in the database
                                Heuristic heuristicResult = gem.selectHeuristic(em, app.getId(), h.getClass().getName());

                                if (heuristicResult == null) {
                                    //Heuristic(App app, String name, String summary, String description, Integer concernLevel, String type, String clazz, Boolean result, Date timestamp) {
                                    heuristicResult = new Heuristic(h.getName(),
                                            h.getSummary(),
                                            h.getDescription(),
                                            h.getConcernLevel(),
                                            h.getType(),
                                            h.getClass().getName(),
                                            result,
                                            new Date());
                                    heuristicResult.setApp(app);
                                } else {
                                    heuristicResult.setName(h.getName());
                                    heuristicResult.setSummary(h.getSummary());
                                    heuristicResult.setDescription(h.getDescription());
                                    heuristicResult.setConcernLevel(h.getConcernLevel());
                                    heuristicResult.setResult(result);
                                    heuristicResult.setTimestamp(new Date());
                                    heuristicResult.setType(h.getType());
                                }

                                gem.updateEntity(em, heuristicResult);
                            }
                            System.out.println("Done");
                        }
                    } else {
                        System.out.println("No heuristic provider found.");
                    }

                    // Final step: Mark the progress file for APK as complete for the static analysis
                    ProgressStatusService pss = project.getLookup().lookup(ProgressStatusService.class);
                    if (pss != null) {
                        System.out.println("\nUpdating Gort Progress file...");
                        Progress progress = pss.progressForAPK(apk);
                        progress.setStaticAnalysis(true);
                        // write back the progress to file
                        progress.toJson(pss.gtpForAPK(apk));
                        System.out.println("Done");
                    }
                
                } finally {
                    GortEntityManager.closeEntityManager(em);
                }
            }
        };
        
        t.start();
    }
    
}
