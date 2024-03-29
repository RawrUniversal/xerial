/*--------------------------------------------------------------------------
 *  Copyright 2009 Taro L. Saito
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
// XerialJ
//
// TabAsTreeParserTest.java
// Since: 2010/03/24 13:26:53
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.util.text;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xerial.util.FileResource;
import org.xerial.util.log.Logger;
import org.xerial.util.tree.TreeEventHandlerBase;

public class TabAsTreeParserTest {

    private static Logger _logger = Logger.getLogger(TabAsTreeParserTest.class);

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void parse() throws Exception {
        TabAsTreeParser p = new TabAsTreeParser(FileResource.open(TabAsTreeParserTest.class,
                "sample.tab"));
        p.parse(new TreeEventHandlerBase() {

            @Override
            public void finish() throws Exception {
                // TODO Auto-generated method stub

            }

            @Override
            public void init() throws Exception {
                // TODO Auto-generated method stub

            }

            @Override
            public void leaveNode(String nodeName) throws Exception {
                // TODO Auto-generated method stub

            }

            @Override
            public void text(String nodeName, String textDataFragment) throws Exception {
                // TODO Auto-generated method stub

            }

            @Override
            public void visitNode(String nodeName, String immediateNodeValue) throws Exception {

                _logger.debug(String.format("visit %s:%s", nodeName, immediateNodeValue));
            }
        });
    }

}
