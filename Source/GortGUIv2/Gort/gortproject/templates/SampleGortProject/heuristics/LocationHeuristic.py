import Gort

PERMISSIONS = [ACCESS_FINE_LOCATION, INTERNET, BOOT_COMPLETE_RECEIVER]

class StaticLocationHeuristic(StaticHeuristic):
    
    def final_output():
        return True