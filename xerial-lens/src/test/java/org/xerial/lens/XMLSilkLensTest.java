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
// XMLSilkLensTest.java
// Since: Jul 28, 2009 4:44:07 PM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.lens;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xerial.core.XerialException;
import org.xerial.silk.SilkWalker;
import org.xerial.util.FileResource;
import org.xerial.util.log.Logger;
import org.xerial.util.tree.TreeWalkLog;
import org.xerial.xml.XMLTreeWalker;
import org.xerial.xml.pullparser.PullParserUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XMLSilkLensTest
{

    private static Logger _logger = Logger.getLogger(XMLSilkLensTest.class);

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testToXML() throws IOException, XerialException, XmlPullParserException {
        String xml = XMLSilkLens.toXML(FileResource.find(XMLSilkLensTest.class, "small.silk"));
        _logger.debug(xml);

        XmlPullParser pullParser = PullParserUtil.newParser(new StringReader(xml));

        int event;
        while ((event = pullParser.next()) != XmlPullParser.END_DOCUMENT) {

        }
    }

    @Test
    public void toSilk() throws Exception {
        String silk = XMLSilkLens.toSilk(FileResource.open(XMLSilkLensTest.class, "track-config.xml"));
        _logger.info(silk);

        TreeWalkLog l1 = new TreeWalkLog();
        TreeWalkLog l2 = new TreeWalkLog();

        XMLTreeWalker x = new XMLTreeWalker(FileResource.open(XMLSilkLensTest.class, "track-config.xml"));
        SilkWalker sw = new SilkWalker(new StringReader(silk));
        x.walk(l1);
        sw.walk(l2);

        boolean doesMatch = TreeWalkLog.compare(l1, l2);
        assertTrue(doesMatch);

    }
}
