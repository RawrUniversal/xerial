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
// XerialJ
//
// LoggerTest.java
// Since: Sep 10, 2007 3:11:41 PM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.util.log;


import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xerial.util.FileResource;

public class LoggerTest
{

    @Before
    public void setUp() throws Exception
    {}

    @After
    public void tearDown() throws Exception
    {}

    @Test
    public void configure() throws IOException
    {
        Logger.configure(FileResource.open(LoggerTest.class, "logconfig1.txt"));
        
        Logger logger = Logger.getLogger("org.xerial.util.log");
        assertEquals(LogLevel.TRACE, logger.getLogLevel());
        
        Logger logger2 = Logger.getLogger(LoggerTest.class);
        assertEquals(LogLevel.DEBUG, logger2.getLogLevel());
        assertEquals(true, logger2.isEnabledColor());
    }
}