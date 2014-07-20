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
from sqlalchemy.orm import relation, mapper, sessionmaker, clear_mappers
from sqlalchemy.ext.declarative import declarative_base

import AndroidAdapter.constants as const

#Base = declarative_base()

class Activity(object):
    
    def name_tuple(self, prefix):
        if not self.name:
            return None
        
        if not prefix or len(prefix) <= 0:
            return (None, self.name)
        
        if self.name.find(prefix) == 0:
            suffix = self.name.replace(prefix, '', 1)
            return (prefix, suffix)

        if self.name.find(prefix) < 0:
            return (prefix, self.name)
    
    def suffix_name(self, prefix):
        tuple = self.name_tuple(prefix)
        
        if not tuple:
            return None
        
        return tuple[1]
            
    def tema_safe_name(self, prefix):
        return self.delimited_name(prefix, '::')
    
    def path_safe_name(self, prefix):
        return delimited_name(prefix, "-")
    
    def delimited_name(self, prefix, delimiter):
         tuple = self.name_tuple(prefix)
         print 'xxx',tuple
         
         if not tuple:
             return None
         
         if not tuple[0]:
             return tuple[1]
         
         return '%s%s%s' %(tuple[0], delimiter, tuple[1])
        

#Do not need to use annotation on traverser side don't mape the table
#class Annotation(object):
#    pass

class App(object):
    def launch_activity(self):
        if not self.activities:
            return None
        
        for activity in self.activities:
            if (activity.launcher):
                return activity
            
        return None
    
    def activity(self, name):
        if not self.activities:
            return None
        
        for activity in self.activities:
            if activity.name == name:
                return activity
            
        return None

class Assignment(object):
    pass

class Component(object):
    def references(self):
        print "getRefernces called.", self.uuid, self.text, self.classname

        if (self.uuid == None and self.text == None):
            return None

        references = list()
    
        if (self.classname != None):
            
            if (self.uuid != None):
                references.append("%s;%s" % (self.uuid, self.classname))

            if (self.text != None):
                references.append("'%s';%s" % (self.text, self.classname))

        if (self.uuid != None):
            references.append(self.uuid)

        if (self.text != None):
            references.append("'%s'" % self.text)

        return references

class CrowdTask(object):
    __tablename__ = 'crowdtask'
    id = Column(Integer, primary_key=True)
    endscreenshot_id = Column('endscreenshot_id', Integer, ForeignKey('screenshot.id'))
    startscreenshot_id = Column('startscreenshot_id', Integer, ForeignKey('screenshot.id'))
    

class History(object):
    __tablename__ = 'history'
    id = Column(Integer, primary_key=True)
    startstate_id = Column('startstate_id', Integer, ForeignKey('state.id'))
    endstate_id = Column('endstate_id', Integer, ForeignKey('state.id'))
    endscreenshot_id = Column('endscreenshot_id', Integer, ForeignKey('screenshot.id'))
    startscreenshot_id = Column('startscreenshot_id', Integer, ForeignKey('screenshot.id'))

class HIT(object):
    pass

class Interaction(object):
    pass

class Library(object):
    pass

class Permission(object):
    pass

class Provider(object):
    pass

class Receiver(object):
    pass

class Screenshot(object):
    pass

class Sequence(object):
    pass

class Service(object):
    pass

class State(object):
    def __repr__(self):
        activity_name = None
        ret_val = None
        if self.activity and self.activity.name:
            activity_name = self.activity.name.split('.')[-1]
        if activity_name:
            ret_val = '%s-%d' %(activity_name, self.id)
        else:
            ret_val = 's-%d' %self.id
        return ret_val
    
    def __str__(self):
        return self.__repr__()

class TaintLog(object):
    pass

class Traversal(object):
    pass

class TraverserDB(object):
    
    NO_ROW = -1
    TABLES_STR = 'activity,annotation,app,app_activity,app_library,app_permission,app_provider,app_receiver,app_service,app_screenshot,assignment,component,crowdtask,history,hit,interaction,library,permission,provider,receiver,screenshot,sequence,sequence_interaction,service,state,state_component,state_screenshot,taintlog,traversal,traversal_history,traversal_taintlog'
    TABLES = TABLES_STR.split(',')
    
    #URL is in the form of postgresql://gort:@localhost:5432/gort-325146ca-3dc4-44aa-a7bd-fb8acf5e250b
    #which includes the dialect, username, password, host, port, and the db name
    def __init__(self, url):
        self.__url = url
        self.__engine = None
        self.__metadata = None
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
        if self.__session_traversal:
            self.__session_traversal.close()
            
        if self.__session_taintlog:
            self.__session_taintlog.close()
            
        # clear the mappers
        clear_mappers()
        
        self.__engine = None
        self.__metadata = None
        self.__sessionmaker = None
        
    def url(self):
        return self.__url
    
    def name(self):
        if not self.__url:
            return None
        
        return self.url().split('/')[-1]
    
    # create a new session for transcations
    def session(self):
        if not self.__sessionmaker:
            self.__sessionmaker = sessionmaker(bind=self.__engine)
            
        return self.__sessionmaker()
    
    # create two sessions one for interaction with UI and one for taintlogs
    def _session_traversal(self):
        if not self.__session_traversal:
            self.__session_traversal = self.session()
            
        return self.__session_traversal
    
    # to make things simpler use 1 session for now
    def _session_taintlog(self):
        if not self.__session_taintlog:
            self.__session_taintlog = self.session()
            
        return self.__session_taintlog
    
    def prepare_tables(self):
        self._init_tables()
        self._map_tables()
    
    #Tables should already be defined by the hibernate ORM from java side of Gort
    def _init_tables(self):
        self.__tables = dict()
        for table in TraverserDB.TABLES:
            self.__tables['%s_table' % table] = Table(table, self.__metadata, autoload=True)
    
    def _map_tables(self):
        # first map independent tables
        mapper(Activity, self.__tables['activity_table'])
        mapper(Assignment, self.__tables['assignment_table'])
        mapper(Component, self.__tables['component_table'])
        mapper(Library, self.__tables['library_table'])
        mapper(Permission, self.__tables['permission_table'])
        mapper(Provider, self.__tables['provider_table'])
        mapper(Receiver, self.__tables['receiver_table'])
        mapper(Screenshot, self.__tables['screenshot_table'])
        mapper(Service, self.__tables['service_table'])
        mapper(TaintLog, self.__tables['taintlog_table'])
        
        mapper(HIT, self.__tables['hit_table'],
               properties={'assignment':relation(Assignment, backref='hit')})
        
        crowdtask_mapper = mapper(CrowdTask, self.__tables['crowdtask_table'],
               properties={'hits':relation(HIT, backref='crowdtask', order_by=HIT.id)})
        crowdtask_mapper.add_property('start_screenshot', relation(Screenshot, foreign_keys=[CrowdTask.startscreenshot_id]))
        crowdtask_mapper.add_property('end_screenshot', relation(Screenshot, foreign_keys=[CrowdTask.endscreenshot_id]))
        
        mapper(Interaction, self.__tables['interaction_table'],
               properties={'component':relation(Component),
                           'taintlogs':relation(TaintLog, backref='interaction')})
        
        mapper(Sequence, self.__tables['sequence_table'],
               properties={'interactions':relation(Interaction, secondary=self.__tables['sequence_interaction_table'], backref='sequences')})
        
        mapper(State, self.__tables['state_table'],
               properties={'activity':relation(Activity),
                           'components':relation(Component, secondary=self.__tables['state_component_table']),
                           'interactions':relation(Interaction, backref='state'),
                           'sequences':relation(Sequence, backref='state'),
                           'screenshots':relation(Screenshot, secondary=self.__tables['state_screenshot_table']),
                           'taintlogs':relation(TaintLog, backref='state')})
        
        history_mapper = mapper(History, self.__tables['history_table'])
        history_mapper.add_property('sequence', relation(Sequence))
        history_mapper.add_property('interaction', relation(Interaction))
        history_mapper.add_property('start_state', relation(State, foreign_keys=[History.startstate_id]))
        history_mapper.add_property('end_state', relation(State, foreign_keys=[History.endstate_id]))
        history_mapper.add_property('start_screenshot', relation(Screenshot, foreign_keys=[History.startscreenshot_id]))
        history_mapper.add_property('end_screenshot', relation(Screenshot, foreign_keys=[History.endscreenshot_id]))
        
        # crowd tasks are ordered by in increasing order of comfortaverage and decreasing comfortstddev
        # it is assumed that a higher std dev for comfort leads to high variance in comfort which is also a poor indicator for comfort
        mapper(Traversal, self.__tables['traversal_table'],
               properties={'crowdtasks':relation(CrowdTask, backref='traversal', order_by=[asc(CrowdTask.comfortaverage),desc(CrowdTask.comfortstddev)]),
                           'history':relation(History, secondary=self.__tables['traversal_history_table']),
                           'interactions':relation(Interaction, backref='traversal'),
                           'sequences':relation(Sequence, backref='traversal'),
                           'states':relation(State, backref='traversal'),
                           'taintlogs':relation(TaintLog, secondary=self.__tables['traversal_taintlog_table'])})
        
        mapper(App, self.__tables['app_table'],
               properties={'activities':relation(Activity, secondary=self.__tables['app_activity_table']),
                           'libraries':relation(Library, secondary=self.__tables['app_library_table']),
                           'permissions':relation(Permission, secondary=self.__tables['app_permission_table']),
                           'providers':relation(Provider, secondary=self.__tables['app_provider_table']),
                           'receivers':relation(Receiver, secondary=self.__tables['app_receiver_table']),
                           'services':relation(Service, secondary=self.__tables['app_service_table']),
                           'screenshots':relation(Screenshot, secondary=self.__tables['app_screenshot_table'], order_by=Screenshot.id),
                           'traversals':relation(Traversal, backref='app')})

    def print_tables(self):
        if self.__tables:
            for table in self.__tables:
                print table
                
    def select_activity(self, _id=None, name=None):
        session = self._session_traversal()
        
        if _id:
            query = session.query(Activity).filter(Activity.id==_id)
            return query.first()
        
        if name:
            query = session.query(Activity).filter(Activity.name==name)
            return query.first()
        
        return None
    
    def select_insert_activity(self, name):
        exists = self.select_activity(name=name)
        
        if exists:
            return exists
        
        activity = Activity()
        activity.name = name
        
        self.insert_activity(activity)
        
        return activity
        
    def select_app_activity(self, app_id, name):
        if app_id is None:
            return None
        
        if name is None:
            return None
        
        session = self._session_traversal()
        
        query = session.query(Activity).join(App.activities).filter(App.id==app_id, Activity.name==name)
        return query.first()
    
    def select_app(self, _id=None):
        if _id is None:
            return None
        
        session = self._session_traversal()
        
        query = session.query(App).filter(App.id==_id)
        return query.first()
    
    def select_apps(self):
        session = self._session_traversal()
        query = session.query(App)
        return query.all()
    
    def select_state(self, traversal_id, hash):
        if hash is None:
            return
        
        session = self._session_traversal()
        query = session.query(State).join(State.traversal).filter(Traversal.id==traversal_id, State.hash==hash)
        return query.first()
    
    def select_first_unexplored_sequence(self, state_id):
        if hash is None:
            return None
        
        session = self._session_traversal()
        query = session.query(Sequence).join(State.sequences).\
            filter(State.id==state_id, Sequence.visited==False).\
            order_by(Sequence.id)
        return query.first()
    
    def select_launch_interaction(self, traversal_id, args):
        if traversal_id is None:
            return None
        
        session = self._session_traversal()
        
        query = session.query(Interaction).join(Interaction.traversal).\
            filter(Traversal.id==traversal_id, Interaction.type==const.ACTION_LAUNCH_APP, Interaction.args==args)
        
        return query.first()
    
    # for now does not take into account args
    def select_state_component_interaction(self, state_id, component_id, type, args=None):
        if state_id is None:
            return None
        
        if component_id is None:
            return None
        
        session = self._session_traversal()
        
        query = session.query(Interaction).join(State.interactions).\
            filter(State.id==state_id).filter(Interaction.type==type)
        
        interactions = query.all()
        
        if not interactions:
            return None
        
        for i in interactions:
            
            component = i.component
            
            if not component:
                continue
            
            if component.id == component_id:
                return i
    
    def select_state_sequence_interaction(self, state_id, sequence_id, type, args):
        if state_id is None:
            return None
        
        session = self._session_traversal()
        
        if sequence_id:
            query = session.query(Sequence).join(Sequence.state).\
                filter(State.id==state_id, Sequence.id==sequence_id)

            s = query.first()
            
            if s:
                # TODO: this may have multiple interactions with same args
                for i in s.interactions:
                    if i.type == type and i.args == args:
                        return (s, i)

        # try a query without the sequence
        query = session.query(Interaction).join(Interaction.state).\
            filter(State.id==state_id, Interaction.type==type, Interaction.args==args)
    
        i = query.first()
    
        if not i:
            return None
        
        if i.sequences and len(i.sequences) > 0:
            s = i.sequences[0]
        
        """
        # return the associated sequence as well. The first sequence for the interaction
        query = session.query(Sequence).join(Sequence.interactions).\
            filter(Interaction.id==i.id).order_by(Interaction.id)
    
        # return the first sequence that claims this interaction
        s = query.first()
        """
        
        return (s, i)
            
    
    def select_state_interaction(self, state_id, type, args):
        return self.select_state_sequence_interaction(state_id, None, type, args)
    
    def select_state_back_interaction(self, state_id):
        return self.select_state_interaction(state_id, const.ACTION_PRESS_KEY, 'back')
    
    def select_state_menu_interaction(self, state_id):
        return self.select_state_interaction(state_id, const.ACTION_PRESS_KEY, 'menu')
    
    def select_sequence(self, sequence_id):
        if sequence_id is None:
            return None
        
        session = self._session_traversal()
        query = session.query(Sequence).filter(Sequence.id==sequence_id)
        return query.first()
    
    def select_insert_launch_interaction(self, traversal_id, args):
        
        exists = self.select_launch_interaction(traversal_id, args)
        
        if exists:
            return exists
        
        session = self._session_traversal()
        
        traversal = session.query(Traversal).filter(Traversal.id==traversal_id).first()
        
        i = Interaction()
        i.traversal = traversal
        i.type = const.ACTION_LAUNCH_APP
        i.visited = False
        i.deterministic = True
        i.args = args
        
        self.insert_interaction(i)
        
        return i
    
    # returns the last screenshot for a state
    def select_state_last_screenshot(self, state_id):
        if (state_id is None):
            return None
        
        session = self._session_traversal()
        
        query = session.query(Screenshot).join(State.screenshots).filter(State.id==state_id).order_by(Screenshot.id.desc())
        
        return query.first()
    
    def select_taintlogger_traversal(self, _id=None):
        if _id is None:
            return None
        
        session = self._session_taintlog()
        
        query = session.query(Traversal).filter(Traversal.id==_id)
        return query.first()
    
    def select_interaction_history(self, interaction_id):
        if interaction_id is None:
            return None
        
        session = self._session_traversal()
        
        query = session.query(History).join(History.interaction).\
            filter(Interaction.id==interaction_id)
        
        return query.all()
    
    def select_last_history(self, traversal_id):
        if traversal_id is None:
            return None
        
        session = self._session_traversal()
        
        query = session.query(History).join(Traversal.history).\
            filter(Traversal.id==traversal_id).order_by(History.id.desc())
        return query.first()
    
    def insert(self, session, item):
        if not session:
            return TraverserDB.NO_ROW
        
        if not item:
            return TraverserDB.NO_ROW
        
        session.add(item)
        session.commit()
        return item.id
    
    def insert_traversal_item(self, i):
        return self.insert(self._session_traversal(), i)
    
    def insert_activity(self, a):
        return self.insert_traversal_item(a)
    
    def insert_component(self, c):
        return self.insert_traversal_item(c)
    
    def insert_history(self, h):
        return self.insert_traversal_item(h)
    
    def insert_interaction(self, i):
        return self.insert_traversal_item(i)
    
    def insert_screenshot(self, s):
        return self.insert_traversal_item(s)
    
    def insert_state(self, s):
        return self.insert_traversal_item(s)
    
    def insert_traversal(self, t):
        return self.insert_traversal_item(t)
    
    def insert_taint_item(self, t):
        return self.insert(self._session_taintlog(), t)
    
    def merge_traversal_item(self, i):
        if not i:
            return
        
        session = self._session_traversal()
        session.merge(i)
        session.commit()
        
    
if __name__ == "__main__":
    print TraverserDB.TABLES_STR
    print TraverserDB.TABLES
    
    db = TraverserDB('postgresql://gort:@localhost:5432/gort-1759489e-30cf-47eb-a54e-6116268e8723')
    print db.connect()
    db.prepare_tables()
    
    # Do not need to add the child to the database before inserting it to a parent
    seq = Sequence()
    i = Interaction()
    seq.interactions.append(i)
    session = db.session()
    session.add(seq)
    session.commit()

    # a state has interactions and interactions have a state
    state = State()
    i0 = Interaction()
    i1 = Interaction()
    state.interactions.append(i0)
    state.interactions.append(i1)
    session.add(state)
    session.commit()
    
    history = History()
    history.start_state = state
    history.interaction = i
    history.sequence = seq
    session.add(history)
    session.commit()
    
    a = db.select_app_activity("1", "com.evernote.ui.HomeActivity")
    if a: print a.name, a
    
    s = db.select_state("1", "-5414879507898703290")
    if s: print s.hash, s
    
    print 'Testing select_state_component'
    i = db.select_state_component_interaction(s.id, 101, const.ACTION_TAP_OBJECT)
    print i
    
    s = db.select_state("14", "-5414879507898703290")
    if s: print s, s.hash
    
    i = db.select_first_unexplored_sequence("1")
    if i: print i.id, i, i.interactions[0]
    
    s = db.select_state_last_screenshot("1")
    if s: print s.path

