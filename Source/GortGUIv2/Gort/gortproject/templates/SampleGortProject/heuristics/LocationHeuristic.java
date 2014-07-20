import gort

public class LocationHeuristic extends StaticHeuristic {
    
    public static String[] PERMISSIONS = {
        ACCESS_FINE_LOCATION,
        INTERNET,
        BOOT_COMPLETE_RECEIVER
    };
    
    public boolean finalOutput() throws Exception {
        return apk.hasPermissions(PERMISSIONS);
    }
}