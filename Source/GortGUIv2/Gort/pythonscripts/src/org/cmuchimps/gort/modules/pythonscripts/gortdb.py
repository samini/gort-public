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

from sqlalchemy import *
from sqlalchemy.orm import relation, mapper, sessionmaker

class Activity(object):
    pass

class App(object):
    pass

class Library(object):
    pass

class Permission(object):
    pass

class Provider(object):
    pass

class Receiver(object):
    pass

class Service(object):
    pass

# sequence for using the following should be instantiation, connecting, preparing tables, and then using
class GortDB(object):
    #URL is in the form of postgresql://gort:@localhost:5432/gort-325146ca-3dc4-44aa-a7bd-fb8acf5e250b
    #which includes the dialect, username, password, host, port, and the db name
    def __init__(self, url):
        self.__url = url
        self.__engine = None
        self.__metadata = None
        self.__session = None
        self.__sessionmaker = None
        self.__session_taintlog = None
        self.__session_traversal = None

    def connect(self):
        self.__engine = create_engine(self.__url)
        if not self.__engine:
            return False

        self.__metadata = MetaData(self.__engine)
        if not self.__metadata:
            return False

        return True

    def close(self):
        self.__engine = None
        self.__metadata = None
        self.__sessionmaker = None

    def prepareTables(self):
        self._initTables()
        self._mapTables()

    def _initTables(self):
        self.activity_table = Table('activity', self.__metadata, autoload=True)
        self.app_table = Table('app', self.__metadata, autoload=True)
        self.library_table = Table('library', self.__metadata, autoload=True)
        self.permission_table = Table('permission', self.__metadata, autoload=True)
        self.provider_table = Table('provider', self.__metadata, autoload=True)
        self.receiver_table = Table('receiver', self.__metadata, autoload=True)
        self.service_table = Table('service', self.__metadata, autoload=True)

        self.app_activity_table = Table('app_activity', self.__metadata, autoload=True)
        self.app_library_table = Table('app_library', self.__metadata, autoload=True)
        self.app_permission_table = Table('app_permission', self.__metadata, autoload=True)
        self.app_provider_table = Table('app_provider', self.__metadata, autoload=True)
        self.app_receiver_table = Table('app_receiver', self.__metadata, autoload=True)
        self.app_service_table = Table('app_service', self.__metadata, autoload=True)

    def _mapTables(self):
        mapper(Activity, self.activity_table)
        mapper(Library, self.library_table)
        mapper(Permission, self.permission_table)
        mapper(Provider, self.provider_table)
        mapper(Receiver, self.receiver_table)
        mapper(Service, self.service_table)

        mapper(App, self.app_table,
               properties={'activity':relation(Activity, secondary=self.app_activity_table),
                           'library':relation(Library, secondary=self.app_library_table),
                           'permission':relation(Permission, secondary=self.app_permission_table),
                           'provider':relation(Provider, secondary=self.app_provider_table),
                           'receiver':relation(Receiver, secondary=self.app_receiver_table),
                           'service':relation(Service, secondary=self.app_service_table)})

    def connection(self):
        if not self.__engine:
            return None

        return self.__engine.connect()

    # create a new session for transcations
    def session(self):
        if not self.__sessionmaker:
            self.__sessionmaker = sessionmaker(bind=self.__engine)

        return self.__sessionmaker()

if __name__ == "__main__":
    db = GortDB('postgresql://gort:@localhost:5432/gort-325146ca-3dc4-44aa-a7bd-fb8acf5e250b')
    db.connect()
    db.prepareTables()
    print dir(db.activity_table)
    print dir(Activity)
    print db.session()
    db.close()
