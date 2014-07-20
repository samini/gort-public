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

import os
import sys
from optparse import OptionParser
from traverserdb import TraverserDB

class AndroCommand(object):
    def __init__(self):
        self.db = None

        parser = OptionParser(
            usage="%s [options] -i file.apk -j index -d databaseurl\nUse -h for a list of available options" % sys.argv[0])

        parser.add_option("-i", "--input", default=None, dest="apk",
            help="apk file to use")
        parser.add_option("-j", "--index", default=None, dest="index",
            help="index of app in database")
        parser.add_option("-d", "--database", default=None, dest="db",
            help="database url to use")
        self.parser = parser

    def _init_db(self, url):
        self.db = TraverserDB(url)
        return self.db.connect()    

    def execute(self):
        (self.options, self.args) = self.parser.parse_args()

        if not self.options.apk:
            self.parser.error("No APK file provided.")
            exit(1)

        if not self.options.index:
            self.parser.error("No APK database index provided.")
            exit(1)

        if not self.options.db:
            self.parser.error("No database url provided.")
            exit(1)

        if not os.path.isfile(self.options.apk):
            sys.stderr.write('APK file does not exist.')
            exit(1)

        if not self._init_db(self.options.db):
            sys.stderr.write('Could not connect to database')
            exit(1)

        # map the tables for use
        self.db.prepare_tables()
