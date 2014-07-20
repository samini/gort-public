# -*- coding: utf-8 -*-
# Copyright (c) 2006-2010 Tampere University of Technology
# 
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
# 
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

"""
This module defines a superclass for a set of keywords that are used to 
execute GUI actions on GUI components.

All keywords use common refencing style to refer different GUI components.
The referencing style is defined in the UIKeyword superclass
"""

import re
from adapterlib.keyword import Keyword

class UIKeyword(Keyword):

    """
    Base class for User Interface keywords

    @cvar componentPattern: Pattern of how GUI components can be referenced in
                            the keywords. Ancestor and parent references 
                            ('::' and ':::') are also included in the pattern
    @type componenPattern: str
    @cvar searchroot: Node where the search will be started
    @type searchroot: Node
    @cvar searchrootReference: 
    @type searchrootReference: str
    """
    # ([^\;]*)(;([^\']+))?    	
    componentPattern = "(?P<ref>[^\;]*)(;(?P<role>[^\']+))?"

    def __init__(self):
        super(UIKeyword,self).__init__()
        self.searchroot = None
        self.searchrootReference = None

    def matchComponent(self, component):
        """      
        Matches the given string with the component regexp pattern.
        Returns the component name (this will contain all the ancestor and 
        parent references as well) and the rolename.
		 
        None is returned if string does not match.   
		 
        @type component: string
        @param component: string representing a component reference. 
		 
        @rtype: string, string (tuple)
        @return: component name reference and rolename of the component
        """
        if component == None:
            return None,None
        matcher = re.compile(self.componentPattern).match(component)
        if matcher == None:
            return None, None
        return matcher.group("ref"), matcher.group("role")
	
    def findComponentReference(self, reference, searchAll = False):
        """
        Find component with given reference. May include a rolename.
        
        The function separates the component name(or text) reference and 
        rolename and uses findComponent() function to search the component.

        @type reference: string
        @param reference: A component reference that may contain rolename.
		
        @type searchAll: boolean
        @param searchAll: True: all components satisfying the reference are 
                          searched. False: only the first component is searched
		
        @rtype: Node or list
        @return If searchAll is False, returns only the first component that
                satisfies the reference, or None if no components were found. 
                If searchAll is	True, returns all components (list) that are 
                satisfactory, or None if none is found.
	"""
		
        componentName, rolename = self.matchComponent(reference)
        if componentName == None:
            return None
        
        self.log("Component: %s" % str(componentName))
        if rolename != None:
            self.log("Rolename: %s" % str(rolename))
			
        return self.findComponent(componentName, rolename, searchAll)
	
    def findComponent(self, name, rolename, searchAll = False):
        """
        Searches components. Component is divided between ":::" operators and
        the findHierarchically is used to search between them (parent 
        hierarchies).
		
        @type  name: string
        @param name: The component reference.

        @type rolename: string
        @param rolename: Rolename of the searched component.  
		
        @type searchAll: Boolean
        @param searchAll: True: all components satisfying the reference are 
                          returned. Otherwise only the first component is 
                          returned.
		
        @rtype: list if searchAll is true, Component otherwise. (None if 
                nothing is found)
        @return: component(s) that satisfy the reference.
        """
		
        #There can not be double ':'-chars (::) in names!
        namePattern = re.compile("(?P<direct>([^:]*:?[^:]+)*)(?P<decend>(:::*).*)?")
        #decendantPattern = re.compile("([^:]+)(:::([^:]+))")
        matcher = namePattern.match(name)
		
        if matcher == None:
            self.log("Invalid component name pattern")
            return None

        #Direct reference - no child (::) or decendant (:::) references.
        if matcher.group("decend") == None:
            results = self.searchChildren(self.searchroot, matcher.group("direct"), roleName = rolename)
            if results == None or len(results) == 0:
                self.log("No components found")
                return None
            if searchAll:
                return results
            else:
                return results[0]
            
        #Separete the reference between ancestor (:::) references
        temp = re.split("(?<!:):::(?=(::)*(?!:))",name)
        nodeNames = []
		
        #Remove leftover "::" and None-items from the list that the (::) group
        # causes
        for t in temp:
            if (t != None and t != "::") or t == temp[-1]:
                nodeNames.append(t)

        #Only child (::) references
        if len(nodeNames) == 1:
            searchNodes = self.findHierarchically(self.searchroot, nodeNames[0])
        #Decendant (:::) references
        else:   
            print "::: separated: ",
            print nodeNames
			
            #Goes thorugh the list of descendant references. The list may 
            # contain direct references or hierarchical (child) references. 
            # The nodes that were accepted in the previous decendant reference
            # are used as root nodes for the next reference in the list. After
            # going through all the separate decendant references the 
            # searchNodes list will contain all the nodes that satisfy the 
            # whole reference.
            searchNodes =[self.searchroot] 
            for n in nodeNames:
                nextNodes = []
                if searchNodes == None or len(searchNodes) == 0:
                    break
                for s in searchNodes:
                    temp = self.findHierarchically(s, n)
                    if temp != None:
                        if type(temp) == list:
                            nextNodes.extend(temp)
                        else:
                            nextNodes.append(temp)
	
		searchNodes = nextNodes
		
        if searchNodes == None or len(searchNodes) == 0:
            self.log("No components found")
            return None
		
        if rolename == None or rolename.strip() == "":
            if searchAll:
                return searchNodes
            else:
                return searchNodes[0]
		
        results = []
        #If rolename given, remove the nodes of different role from the search
        # results.
        for n in searchNodes:
            if self.checkNodeImplementsRole(n,rolename):
                if not searchAll:
                    return n
                results.append(n)
        if len(results) == 0:        
            return None
        return results    
			  
	
    def findHierarchically(self, searchNode, component):   
        """
        Searches hierarchical (separated with '::') component references. 
		
        @type searchNode: string
        @param searchNode: The Component that is used as a root for the search.
        
        @type component: string
        @param component: component reference
		
        @rtype: list
        @return: A list of Components satisfying the reference 
        """
        raise NotImplementedError

    def searchChildren(self, searchRoot, component, roleName = None, searchOnlyFirst = False):
        """
        Helper function for searching child nodes, used from findComponent.
        function finds nodes with it name or with its text contents.
        If text is to be searched, the component parameter needs to be enclosed
        with single quote marks.

        @param searchRoot: The node where the search is started
        
        @type component: string
        @param component: A component name or the text content that is searched
		
        @type roleName: string
        @param roleName: Rolename of the searched component
		
        @type searchOnlyFirst: boolean
        @param searchOnlyFirst: if true, only the first child that satisfies 
                                the reference found is returned; if true, all 
                                children satisfying the reference are searched.

        @rtype: node or list or None
        @return: If searchOnlyFirst is True, returns only the first component 
                 that satisfies the name and rolename, or None if no components
                 were found. If searchOnlyFirst is false, returns all 
                 components (list) that are satisfactory, or None if none is 
                 found. 
        """
        raise NotImplementedError

    def checkNodeImplementsRole(self, node, rolename):
        """ Checks if node implements rolename
        @param node: Node object
        @type rolename: str
        @rtype: bool
        """
        raise NotImplementedError
