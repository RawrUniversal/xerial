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
// SilkUtilTest.java
// Since: 2009/02/14 5:57:08
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.silk;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xerial.core.XerialException;
import org.xerial.util.FileResource;
import org.xerial.util.log.Logger;

public class SilkUtilTest
{
    private static Logger _logger = Logger.getLogger(SilkUtilTest.class);

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testToJSON() throws IOException, XerialException {
        StringWriter buf = new StringWriter();
        SilkUtil.toJSON(FileResource.find(SilkUtilTest.class, "suzaku-config.silk"), buf);
        _logger.debug(buf.toString());
    }

    @Test
    public void testToJSON2() throws IOException, XerialException {
        StringWriter buf = new StringWriter();
        SilkUtil.toJSON(FileResource.find(SilkUtilTest.class, "example.silk"), buf);
        _logger.debug(buf.toString());
    }

    @Test
    public void testToJSON3() throws IOException, XerialException {
        StringWriter buf = new StringWriter();
        SilkUtil.toJSON(FileResource.find(SilkUtilTest.class, "small.silk"), buf);
        _logger.debug(buf.toString());
    }

}
