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
// SilkMainTest.java
// Since: Apr 9, 2009 5:29:18 PM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.silk.cui;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SilkMainTest
{

    @Before
    public void setUp() throws Exception
    {}

    @After
    public void tearDown() throws Exception
    {}

    @Test
    public void testMain() throws Exception
    {
        SilkMain.main(new String[] { "help" });
        SilkMain.main(new String[] { "scan", "input.silk" });
        SilkMain.main(new String[] { "scan", "--help" });
    }

}
