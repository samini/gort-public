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
package org.cmuchimps.gort.modules.heuristics;

import org.cmuchimps.gort.api.gort.heuristic.HeuristicService;
import org.cmuchimps.gort.modules.heuristics.dynamic.*;
import org.cmuchimps.gort.modules.heuristics.static_.*;

import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shahriyar
 */
@ServiceProvider(service=HeuristicService.class)
public class HeuristicServiceProvider extends HeuristicService {

    public HeuristicServiceProvider() {
        super();
        init();
    }
    
    private void init() {
        //Add static heuristics
        //this.addHeuristic(? extends AbstractStaticHeuristic);
        
        //Add permission based static heuristic
        
        this.addHeuristic(new DeleteApp());
        this.addHeuristic(new EavesdropOnCalls());
        this.addHeuristic(new EavesdropOnPhoneState());
        
        this.addHeuristic(new InstallApp());
        this.addHeuristic(new InstallShortcuts());

        this.addHeuristic(new LeakCoarseLocation());
        this.addHeuristic(new LeakPreciseLocation());
        this.addHeuristic(new LeakContacts());
        
        this.addHeuristic(new ReadSystemLog());
        this.addHeuristic(new ReceiveAndDropSMS());
        this.addHeuristic(new SendAndObfuscateSMS());

        this.addHeuristic(new SetAppDebugMode());
        
        //Add non-permission based static heuristic
        this.addHeuristic(new ExceedTypicalSize());
        this.addHeuristic(new LoadDynamicCode());
        this.addHeuristic(new UninstallShortcuts());
        this.addHeuristic(new UseJavaReflection());
        this.addHeuristic(new UseNativeCode());
        
        //Add mixed static heuristic
        
        //Add dynamic heuristics
        this.addHeuristic(new FourPlusServers());
        this.addHeuristic(new MixedSensitiveTransmissions());
        this.addHeuristic(new NoSSLSensitiveTransmission());
        this.addHeuristic(new NonMappedIPTransmission());
        this.addHeuristic(new TransmitsAccelerometerData());
        this.addHeuristic(new TransmitsAccountInformation());
        this.addHeuristic(new TransmitsApproximateLocation());
        this.addHeuristic(new TransmitsCameraData());
        this.addHeuristic(new TransmitsContacts());
        this.addHeuristic(new TransmitsDeviceSerialNumber());
        this.addHeuristic(new TransmitsExactLocation());
        this.addHeuristic(new TransmitsLastLocation());
        this.addHeuristic(new TransmitsLocation());
        this.addHeuristic(new TransmitsLocationNoMaps());
        this.addHeuristic(new TransmitsPhoneNumber());
        this.addHeuristic(new TransmitsRecordedAudio());
        this.addHeuristic(new TransmitsSMSData());
        this.addHeuristic(new TransmitsUniqueDeviceIdentifier());
        this.addHeuristic(new TransmitsUserHistory());
    }
    
}
