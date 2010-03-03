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
// RaquelXMLBuilderTest.java
// Since: Mar 3, 2010 5:04:44 PM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.lens.relation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xerial.silk.cui.SilkMain;
import org.xerial.util.FileResource;

public class RaquelXMLBuilderTest {

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void purifyHelp() throws Exception {

        SilkMain.execute(new String[] { "purify", "--help" });

    }

    @Test
    public void purify() throws Exception {
        SilkMain.execute(new String[] { "purify",
                FileResource.find(RaquelXMLBuilderTest.class, "s.xml").getPath(), "-s",
                FileResource.find(RaquelXMLBuilderTest.class, "common.schema").getPath() });
    }
}
