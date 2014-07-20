#
# Copyright 2014 Shahriyar Amini
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

__author__ = 'shahriyar'
__copyright__ = 'Copyright 2014, Shahriyar Amini'

import sys, os, os.path
from optparse import OptionParser

from androguard.core import androconf
from androguard.core.bytecodes import apk, dvm
from androguard.core.analysis import analysis

from androcommand import AndroCommand
from traverserdb import Activity, App, Library, Permission, Provider, Receiver, Service

#class related to androapkinfo.py
#http://code.google.com/p/androguard/source/browse/androapkinfo.py
class BasicInfo(AndroCommand):

    COMPONENTS_KEYS = "a,f,l,m,p,q,r,s,t,z"
    COMPONENTS_STR = "activities,files,libraries,minsdk,permissions,providers,receivers,services,targetsdk,package"

    def __init__(self):
        AndroCommand.__init__(self)

        self.components = BasicInfo.COMPONENTS_STR.split(',')
        self.component_keys = BasicInfo.COMPONENTS_KEYS.split(',')

        component_dict = dict(zip(self.component_keys, self.components))

        for key in component_dict:
            component = component_dict[key]
            explaination = None
            if key == "d" or key == "n" or key == "x":
                explaination = "output if apk uses %s code" % component
            elif key == "o":
                explaination = "output if apk uses ascii obfuscation"
            else:
                explaination = "output apk %s" % component
            self.parser.add_option("-%s" % key,
                "--%s" % component,
                action="store_true",
                default=False,
                dest=component,
                help=explaination)

        self.parser.add_option("", "--all-options",
            action="store_true",
            default=False,
            dest="all_options",
            help="All options other than files are processed.")

    def execute(self):
        AndroCommand.execute(self)

        # if all options is selected enable analysis all options but
        # the files. we do not currently use the file option
        if (self.options.all_options):
            self.options.activities = True
            self.options.libraries = True
            self.options.minsdk = True
            self.options.permissions = True
            self.options.providers = True
            self.options.receivers = True
            self.options.services = True
            self.options.targetsdk = True
            self.options.package = True

        simple_options = [self.options.activities, self.options.files, self.options.libraries, self.options.minsdk, self.options.permissions, self.options.providers, self.options.receivers, self.options.services, self.options.targetsdk, self.options.package]

        a = apk.APK(self.options.apk, zipmodule=2)

        if not a.is_valid_APK():
            sys.stderr.write('Not a valid APK file.')
            exit(1)

        if not any(simple_options):
            sys.stderr.write("No output option selected.")
            exit(1)

        session = self.db.session()
        #print session

        try:
            app = session.query(App).filter(App.id==self.options.index).first()

            if not app:
                sys.stderr.write("Could not find app by index.")
                exit(1)

            count = 1

            if self.options.activities:
                print 'Extracting application activities...'
                self.updateActivities(app, a, session)

            #if self.options.files:
            #    print_output(count, 'files', a.get_files())

            if self.options.libraries:
                print 'Extracting application libraries...'
                self.updateLibraries(app, a, session)

            if self.options.minsdk:
                print 'Extracting application minimum SDK...'
                app.minsdkversion = a.get_min_sdk_version()

            if self.options.permissions:
                print 'Extracting application permissions...'
                self.updatePermissions(app, a, session)

            if self.options.providers:
                print 'Extracting application providers...'
                self.updateProviders(app, a, session)

            if self.options.receivers:
                print 'Extracting application receivers...'
                self.updateReceivers(app, a, session)

            if self.options.services:
                print 'Extracting application services...'
                self.updateServices(app, a, session)

            if self.options.targetsdk:
                print 'Extracting application target SDK...'
                app.targetsdkversion = a.get_target_sdk_version()

            if self.options.package:
                print 'Determining application package...'
                app.package = a.get_package()

            session.commit()
            print 'Success'
        except:
            sys.stderr.write('Could not perform transaction on database.\n')
            session.rollback()
            raise
            exit(1)

    def updateActivities(self, app, app_apk, session):
        main_activity = app_apk.get_main_activity()
        activities = app_apk.get_activities()
        
        for activity in activities:
            query = session.query(App, Activity)
            query = query.filter(App.id==app.id).filter(Activity.name==activity)
            if (query.count() > 0):
                continue
            a = Activity()
            a.name = activity
            a.inapp = True
            if a.name == main_activity:
                a.launcher = True
            app.activities.append(a)
        session.flush()

    def updateLibraries(self, app, app_apk, session):
        libraries = app_apk.get_libraries()
        for library in libraries:
            query = session.query(Library).filter(Library.name==library)
            if (query.count() <= 0):
                l = Library()
                l.name = library
                app.libraries.append(l)
            else:
                l = query.first()
                if l not in app.libraries:
                    app.libraries.append(l)
                
        session.flush()

    # following needs to be fixed with respect to duplicated!
    def updatePermissions(self, app, app_apk, session):
        permissions = app_apk.get_details_permissions() 
        for permission in permissions:
            query = session.query(Permission).filter(Permission.name==permission)
            if query.count() <= 0:
                p = Permission()
                p.name = permission
                p.description = permissions[permission][2]
                app.permissions.append(p)
            else:
                p = query.first()
                if p not in app.permissions:
                    app.permissions.append(p)
        session.flush()

    def updateProviders(self, app, app_apk, session):
        providers = app_apk.get_providers()
        for provider in providers:
            query = session.query(App, Provider)
            query = query.filter(App.id==app.id).filter(Provider.name==provider)
            if query.count() > 0:
                continue
            p = Provider()
            p.name = provider
            app.providers.append(p)
        session.flush()

    def updateReceivers(self, app, app_apk, session):
        receivers = app_apk.get_receivers()
        for receiver in receivers:
            query = session.query(App, Receiver)
            query = query.filter(App.id==app.id).filter(Receiver.name==receiver)
            if query.count() > 0:
                continue
            r = Receiver()
            r.name = receiver
            app.receivers.append(r)
        session.flush()

    def updateServices(self, app, app_apk, session):
        services = app_apk.get_services()
        for service in services:
            query = session.query(App, Service)
            query = query.filter(App.id==app.id).filter(Service.name==service)
            if query.count() > 0:
                continue
            s = Service()
            s.name = service
            app.services.append(s)
        session.flush()

# put the print here
def print_output(output_count, component, output):
    if (output_count == 1):
        print output
    elif (output_count > 1):
        print "%s: %s" %(component, output)

if __name__ == "__main__":
    bi = BasicInfo()
    bi.execute()

