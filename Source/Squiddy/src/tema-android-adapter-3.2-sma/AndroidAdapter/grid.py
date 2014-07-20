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

import math

class Grid(object):
    INVALID = -1

    # hStep and vStep are just numbers but for our implementation we turn them into points
    def __init__(self, nw=None, se=None, hStep=-1, vStep=-1):
        self.__nw = nw
        self.__se = se
        self.__hStep = Point(hStep, 0)
        self.__vStep = Point(0, vStep)
        self.__cells = None
        self.__numCols = -1
        self.__numRows = -1
        self.__numCells = -1
    
    def __repr__(self):
        if self.__cells is None or len(self.__cells) <= 0:
            return 'Grid is empty.'

        strings = []

        for cell in self.__cells:
           strings.append(str(cell))

        return '\n'.join(strings)

    def populate(self):
        if self.__nw is None or self.__se is None:
            return

        if self.__hStep is None or self.__hStep <= 0:
            return

        if self.__vStep is None or self.__vStep <= 0:
            return

        if self.__nw.distance(self.__se) <= 0:
            return

        width = self.width()
        height = self.height()

        if width is None or height is None:
            return

        self.__numRows = int(math.ceil(float(height) / self.__vStep.Y()))
        #print self.__numRows
        self.__numCols = int(math.ceil(float(width) / self.__hStep.X()))
        #print self.__numCols
        self.__numCells = int(self.__numRows * self.__numCols)
        #print self.__numCells


        self.__cells = []

        for i in range(0, self.__numCells):
            self.__cells.append(None)

        # create the first cell
        initCellNW = self.__nw
        initCellNE = initCellNW + self.__hStep
        initCellSE = initCellNE + self.__vStep
        initCellSW = self.__nw + self.__vStep
        initVertices = [initCellNW, initCellNE, initCellSE, initCellSW]
        self.correctVertices(initVertices)

        initCell = Cell(0, initVertices)

        self.__cells[0] = initCell
        
        # create the first row
        for i in range(1, self.__numCols):
            prevCell = self.__cells[i - 1]
            prevVertices = prevCell.getVertices()

            vertices = [None, None, None, None]
            vertices[Cell.INDEX_NW] = prevVertices[Cell.INDEX_NE]
            vertices[Cell.INDEX_SW] = prevVertices[Cell.INDEX_SE]
            vertices[Cell.INDEX_NE] = vertices[Cell.INDEX_NW] + self.__hStep
            vertices[Cell.INDEX_SE] = vertices[Cell.INDEX_SW] + self.__hStep

            self.correctVertices(vertices)

            self.__cells[i] = Cell(i, vertices)


        for i in range(1, self.__numRows):
            for j in range(0, self.__numCols):
                _id = self.idByIndices(i, j)
                northernId = self.idByIndices(i - 1, j)
                northernCell = self.__cells[northernId]
                northernCellVertices = northernCell.getVertices()

                vertices = [None, None, None, None]
                vertices[Cell.INDEX_NW] = northernCellVertices[Cell.INDEX_SW]
                vertices[Cell.INDEX_NE] = northernCellVertices[Cell.INDEX_SE]
                vertices[Cell.INDEX_SE] = vertices[Cell.INDEX_NE] + self.__vStep
                vertices[Cell.INDEX_SW] = vertices[Cell.INDEX_NW] + self.__vStep

                self.correctVertices(vertices)

                self.__cells[_id] = Cell(_id, vertices)

    def cellById(self, _id):
        if _id is None or _id < 0:
            return None

        return self.__cells[_id]

    def cellByIndex(self, row, col):
        _id = self.idByIndices(row, col)

        return self.__cells[_id]

    def cells(self):
        return self.__cells

    def cellCenters(self):
        return map(lambda x:x.center(), self.__cells)

    def cellSpecificPoints(self, index):
        return map(lambda x:x.getVertices()[index], self.__cells)

    def correctVertices(self, vertices):
        if vertices is None:
            return None

        for vertex in vertices:
            if vertex.X() < self.__nw.X():
                vertex.setX(self.__nw.X())
            if vertex.X() > self.__se.X():
                vertex.setX(self.__se.X())
            if vertex.Y() < self.__nw.Y():
                vertex.setY(self.__nw.Y())
            if vertex.Y() > self.__se.Y():
                vertex.setY(self.__se.Y())

    def idByIndices(self, row, col):
        if row is None or col is None:
            return None
        
        if row < 0:
            return None

        if col < 0:
            return None

        return row * self.__numCols + col

    def height(self):
        try:
            return self.__se.Y() - self.__nw.Y()
        except:
            return None

    def width(self):
        try:
            return self.__se.X() - self.__nw.X()
        except:
            return None

    def printNWPoints(self):
        if self.__cells is None or len(self.__cells) <= 0:
            print 'Grid is empty.'

        strings = []

        for nwPoint in self.cellSpecificPoints(Cell.INDEX_NW):
            strings.append('%d, %d' % (nwPoint.X(), nwPoint.Y()))

        print '\n'.join(strings)

    def printCenterPoints(self):
        if self.__cells is None or len(self.__cells) <= 0:
            print 'Grid is empty.'

        strings = []

        for centerPoint in self.cellCenters():
            strings.append('%d, %d' % (centerPoint.X(), centerPoint.Y()))

        print '\n'.join(strings)

class Cell(object):
    INVALID_ID = -1
    INDEX_NW = 0
    INDEX_NE = 1
    INDEX_SE = 2
    INDEX_SW = 3

    def __init__(self, _id=-1, vertices=None):
        self.__id = _id

        # index 0 used for initial vertex, index clockwise
        # should be 4 points, we assume cells are rectangular
        self.__vertices = vertices

    def __repr__(self):
        string = 'vertices:'

        if self.__vertices is None:
            string += '\tNone'
        else:
            for vertex in self.__vertices:
                string += '\t' + str(vertex)

        return 'id:\t%d\t%s' % (self.getId(), string)

    def getId(self):
        return self.__id

    def setId(self, _id):
        self.__id = _id

    def getVertices(self):
        return self.__vertices

    def setVertices(self, vertices):
        self.__vertices = vertices

    def center(self):
        if self.__vertices is None:
            return None

        sumPoint = Point()

        for vertex in self.__vertices:
            sumPoint += vertex

        return Point(sumPoint.X() / 4.0, sumPoint.Y() / 4.0)

    def width(self):
        if self.__vertices is None:
            return None

        return self.__vertices[INDEX_NW].distance(self.__vertices[INDEX_NE])

    def height(self):
        if self.__vertices is None:
            return None

        return self.__vertices[INDEX_NW].distance(self.__vertices[INDEX_SW])

class Point(object):
    def __init__(self, x=0, y=0):
        self.reset(x, y)

    def __add__(self, p):
        return Point(self.X() + p.X(), self.Y() + p.Y())

    def __repr__(self):
        return '(%d, %d)' % (self.X(), self.Y())

    def distance(self, other):
        dx = self.X() - other.X()
        dy = self.Y() - other.Y()
        return math.hypot(dx, dy)

    def reset(self, x=0, y=0):
        self.setX(x)
        self.setY(y)

    def setX(self, x):
        self.__x = x

    def setY(self, y):
        self.__y = y

    def X(self):
        return self.__x

    def Y(self):
        return self.__y

if __name__ == "__main__":
    nw = Point(0, 20)
    se = Point(720 - 1, 1280 -1)
    hStep = 50
    vStep = 50
    g = Grid(nw, se, hStep, vStep)
    g.populate()
    #print g
    g.printNWPoints()
    #g.printCenterPoints()

    # get the center points and order them by x,y coordinates
    centerPoints = g.cellCenters()
    s = sorted(centerPoints, key = lambda p: (p.X(), p.Y()))
    print '\n', s

    # sort s again, this time by y, x coordinates
    s = sorted(s, key = lambda p: (p.Y(), p.X()))
    print '\n', s
