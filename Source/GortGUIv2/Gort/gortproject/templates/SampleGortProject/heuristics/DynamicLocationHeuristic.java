import gort

public class DynamicLocationHeuristic {
    public static String[] PERMISSIONS = {
        ACCESS_FINE_LOCATION,
        INTERNET
    };
    
    public static String HIT_HEADER = "screenshot2";
    
    private boolean result;
    
    @Override
    public void init(APK apk) {
        result = apk.hasPermissions(PERMISSIONS);
    }
    
    @Override
    public void onTaintLog(Taint t) {
        if (taint.matchesTaint(Taint.TAINT_LOCATION)) {
            appendHITInput(t.lastScreenshot());
        }
    }
    
    @Override
    public boolean finalInput() {
        return result;
    }
}
