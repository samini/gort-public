class DynamicLocationHeuristic(DynamicHeuristic):
    
    PERMISSIONS = [ACCESS_FINE_LOCATION, INTERNET]
    
    HIT_HEADER = "screenshot2"
    
    def init(apk):
        self.__result = false
        result = apk.has_permissions(self.PERMISSIONS)
        
    def on_taint_log(self, taint):
        if taint.matches_taint(Taint.TAINT_LOCATION):
            append_hit_input(taint.last_screenshot())

    def final_output():
        return result