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
from traverserdb import App

#class related to androapkinfo.py
#http://code.google.com/p/androguard/source/browse/androapkinfo.py
class DalvikInfo(AndroCommand):

    COMPONENTS_KEYS = "n,o,r,y"
    COMPONENTS_STR = "native,obfuscation,reflection,dynamic"

    def __init__(self):
        AndroCommand.__init__(self)
        """
        parser = OptionParser(
            usage="%s [self.options. -i file.apk\nUse -h for a list of available self.options. % sys.argv[0])
        """

        self.components = DalvikInfo.COMPONENTS_STR.split(',')
        self.component_keys = DalvikInfo.COMPONENTS_KEYS.split(',')

        component_dict = dict(zip(self.component_keys, self.components))

        for key in component_dict:
            component = component_dict[key]
            explaination = None
            if key == "n" or key == "r" or key == "y":
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
        """
        parser.add_option("-i", "--input", default=None, dest="apk",
            help="apk file to use")
        parser.add_option("-j", "--index", default=None, dest="index",
            help="index of the app in database")
        parser.add_option("-d", "--database", default=None, dest="db",
            help="database url to use")
        self.parser = parser
        """
        
        self.parser.add_option("", "--all-options",
            action="store_true",
            default=False,
            dest="all_options",
            help="All options are selected.")


    def execute(self):
        AndroCommand.execute(self)
        
        if self.options.all_options:
            self.options.dynamic = True
            self.options.native = True
            self.options.obfuscation = True
            self.options.reflection = True
        
        dalvik_options= [self.options.dynamic, self.options.native, self.options.obfuscation, self.options.reflection]

        a = apk.APK(self.options.apk, zipmodule=2)

        if not a.is_valid_APK():
            sys.stderr.write("Not a valid APK file.")
            exit(1)

        if not any(dalvik_options):
            sys.stderr.write("No output option selected.")
            exit(1)
            

        # create a session, get the underlying app and use a transaction to update 
        # the values
        session = self.db.session()

        try:
            app = session.query(App).filter(App.id==self.options.index).first()

            if not app:
                sys.stderr.write("Could not find app by index.")
                exit(1)

            #print 'Doing vm analysis'
            dex = a.get_dex()
            vm = dvm.DalvikVMFormat(dex)
            vmx = analysis.uVMAnalysis(vm)

            if self.options.dynamic:
                print 'Checking if application uses dynamic code...'
                app.dynamic = analysis.is_dyn_code(vmx)
                session.flush()

            if self.options.native:
                print 'Checking if application uses native code...'
                app.native = analysis.is_native_code(vmx)
                session.flush()

            # androguard 1.9 no longer supports this
            """
            if self.options.obfuscation:
                print 'Checking if application uses obfuscation...'
                app.obfuscation = analysis.is_ascii_obfuscation(vm)
                session.flush()
            """

            if self.options.reflection:
                print 'Checking if application uses reflection...'
                app.reflection = analysis.is_reflection_code(vmx)
                session.flush()

            session.commit()
            print 'Success'
        except:
            session.rollback()
            exit(1)

        exit(0)

# put the print here
def print_output(output_count, component, output):
    if (output_count == 1):
        print output
    elif (output_count > 1):
        print "%s: %s" %(component, output)

#use the following call python dalvikinfo.py -i APK -j 1 -db "gort-325146ca-3dc4-44aa-a7bd-fb8acf5e250b"
if __name__ == "__main__":
    bi = DalvikInfo()
    bi.execute()
