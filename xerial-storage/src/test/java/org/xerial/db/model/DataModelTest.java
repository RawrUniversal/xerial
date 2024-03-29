/*--------------------------------------------------------------------------
 *  Copyright 2007 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// XerialJ Project
//
// DataModelTest.java
// Since: May 11, 2007
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.db.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xerial.db.DBException;
import org.xerial.db.sql.ConnectionPool;
import org.xerial.db.sql.ConnectionPoolImpl;
import org.xerial.db.sql.sqlite.SQLite;
import org.xerial.db.sql.sqlite.SQLiteAccess;
import org.xerial.util.log.Logger;

public class DataModelTest
{

    private static SQLiteAccess _query;

    private static Logger _logger = Logger.getLogger(DataModelTest.class);

    @BeforeClass
    public static void openDatabase() throws DBException
    {
        ConnectionPool connectionPool = new ConnectionPoolImpl(SQLite.driverName, SQLite.getMemoryDatabaseAddress());
        _query = new SQLiteAccess(connectionPool);
    }

    @AfterClass
    public static void closeDatabase() throws DBException
    {
        if (_query != null)
            _query.dispose();
    }

    private DataModel _dm;

    @Before
    public void setUp() throws Exception
    {
        /*
         * create a data model of the following structure:
         * 
         * <code>A -> B -> C</code> where '->' represents an one to many
         * relationship;
         */

        _dm = new DataModel();
        _dm.addNode("A");
        _dm.addNode("B");
        _dm.addNode("C");
        _dm.connectNodes("A", "B", Relationship.OneToMany);
        _dm.connectNodes("B", "C", Relationship.OneToMany);
    }

    @After
    public void tearDown() throws Exception
    {}

    @Test
    public void contains()
    {
        assertTrue(_dm.contains("A"));
        assertTrue(_dm.contains("B"));
        assertTrue(_dm.contains("C"));
        assertFalse(_dm.contains("D"));

    }

    @Test
    public void rootNode()
    {
        Set<String> rootNodeSet = _dm.getRootNodeNameSet();
        assertEquals(1, rootNodeSet.size());
        assertTrue(rootNodeSet.contains("A"));
    }

    @Test
    public void connection()
    {
        List<String> nodesConnectedToA = _dm.destinationOf("A");
        assertEquals(1, nodesConnectedToA.size());
        assertTrue(nodesConnectedToA.contains("B"));
    }

    @Test
    public void path()
    {
        List<String> pathFromAToC = _dm.path("A", "C");
        assertTrue(_dm.hasPath("A", "C"));
        assertEquals(3, pathFromAToC.size());
        assertEquals("A", pathFromAToC.get(0));
        assertEquals("B", pathFromAToC.get(1));
        assertEquals("C", pathFromAToC.get(2));
    }

    @Test
    public void addNode()
    {
        // add a new node named "D"
        _dm.addNode("D");

        Set<String> rootNodeSet = _dm.getRootNodeNameSet();
        assertEquals(2, rootNodeSet.size());
        assertTrue(rootNodeSet.contains("A"));
        assertTrue(rootNodeSet.contains("D"));

        List<String> nodesConnectedToD = _dm.destinationOf("D");
        assertEquals(0, nodesConnectedToD.size());

        for (String target : new String[] { "A", "B", "C" })
        {
            List<String> pathFromDToSomewhere = _dm.path("D", target);
            assertFalse(_dm.hasPath("D", target));
            assertEquals(0, pathFromDToSomewhere.size());
        }

    }

    @Test
    public void saveGraph() throws DBException
    {
        _dm.save(_query);
    }

    @Test
    public void loadGraph() throws DBException
    {
        _dm.save(_query);

        DataModel dm2 = new DataModel();
        dm2.load(_query);

        _logger.debug(dm2.toString());
    }

}
