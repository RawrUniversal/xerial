/*--------------------------------------------------------------------------
 *  Copyright 2004 Taro L. Saito
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
//-----------------------------------
// XerialJ Project
// 
// DataGuide.java 
// Since: 2005/05/31
//
// $Date$ 
// $Author$
//--------------------------------------
package org.xerial.util.xml.index;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Set;
import java.util.Stack;

import org.xerial.core.XerialException;
import org.xerial.util.StringUtil;
import org.xerial.util.XMLParserException;
import org.xerial.util.cui.OptionParser;
import org.xerial.util.cui.OptionParserException;
import org.xerial.util.graph.AdjacencyList;
import org.xerial.util.graph.GraphException;
import org.xerial.util.xml.SinglePath;
import org.xerial.util.xml.XMLException;
import org.xerial.util.xml.pullparser.AbstractSAXEventHandler;
import org.xerial.util.xml.pullparser.PullParserUtil;
import org.xerial.util.xml.pullparser.SAXParser;
import org.xmlpull.v1.XmlPullParser;


/**
 * DataGuide���쐬����
 * @author leo
 *
 */
public class DataGuide extends AbstractSAXEventHandler
{
    /**
     * Adjacency list of String:tagID (node) and String:label (edge) 
     */
    private AdjacencyList<String, String> _graph = new AdjacencyList<String, String>();
//    private HashMap<String, Integer> _tag2idMap = new HashMap<String, Integer>();
//    private HashMap<Integer, String> _id2tagMap = new HashMap<Integer, String>();
    private int _currentNodeID = 0;
    private Stack<Integer> _cursorHistory = new Stack<Integer>();
    private int _rootNodeID;

    public static final String ROOT_TAG = "_root"; 
    
    /**
     * 
     */
    public DataGuide()
    {
        _rootNodeID = getTagID_internal(ROOT_TAG);
        init();
    }

    private static XmlPullParser getParser(Reader xmlReader) throws XMLParserException
    {
        XmlPullParser parser = PullParserUtil.newParser(xmlReader);
        return parser;
    }
    
    /**
     * DataGuide�����ǂ��āA�^����ꂽpath�ɊY������m�[�hID�𓾂�
     * @param path
     * @return �m�[�hID. �Y������p�X�����݂��Ȃ��Ƃ��ɂ�-1
     */
    public int getPathID(SinglePath path)
    {
        return getTagID(path.getLeaf());
    }
    
    /**
     * DataGuide���̃m�[�hID��Ԃ�
     * @param tagName �^�O�̖��O�Battribute�̏ꍇ��, @id �̂悤�Ȍ`��
     * @return node ID. �^�O�ɑΉ�����m�[�h��������Ȃ��ꍇ�ɂ́A-1
     */
    public int getTagID(String tagName)
    {
    	return _graph.getNodeID(tagName);
    }

    
    public int newTag(String tagName) throws XMLException
    {
        try
        {
            int tagID = this.getTagID_internal(tagName);
            this.moveCursor(tagID);
            return _currentNodeID;
        }
        catch(GraphException e)
        {
            throw new XMLException(e);
        }
    }
    public void newAttribute(String attributeName) throws XMLException
    {
        try
        {
            int attributeID = this.getTagID_internal("@" + attributeName);
            this.moveCursor(attributeID);
            this.traceBack();
        }
        catch(GraphException e)
        {
            throw new XMLException(e);
        }
    }
    
    public void closeTag() 
    {
        this.traceBack();
    }
    
        
    public void init()
    {
        // some initialization
        _currentNodeID = _rootNodeID;
        
    }
    
    //------------------------------------------------------------------------------------------------
    // SAX�n���h���p�̃��\�b�h
    // @see org.xerial.util.xml.pullparser.SAXEventHandler#startDocument(org.xmlpull.v1.XmlPullParser)
    @Override
    public void startDocument(XmlPullParser parser) throws XMLException
    {
        init();
    }


    @Override
    public void startTag(XmlPullParser parser) throws XMLException
    {
        newTag(parser.getName());
        for(int i=0; i<parser.getAttributeCount(); i++)
        {
            newAttribute(parser.getAttributeName(i));
        }
    }
    
    @Override
    public void endTag(XmlPullParser parser) throws XMLException
    {
        closeTag();
    }
    //--------------------------------------------------------------------------------------------------

    /** 
     * xml file��ǂ��DataGuide�𐶐�����B
     * @param xmlFile
     * @throws FileNotFoundException
     * @throws XMLParserException
     * @throws XMLException
     * @throws IOException
     * @throws XerialException
     */
    public void generateFrom(String xmlFile) throws FileNotFoundException, XMLParserException, XMLException, IOException, XerialException 
    {
        Reader reader = new BufferedReader(new FileReader(xmlFile));
        generateFrom(reader);
    }
    
    /**
     * reader����XML��ǂݍ���ŁADataGuide�𐶐�����
     * @param xmlReader
     * @throws FileNotFoundException
     * @throws XMLParserException
     */
    public void generateFrom(Reader xmlReader) throws XMLParserException, XMLException, IOException, XerialException 
    {
        
        SAXParser saxParser = new SAXParser(this);
        saxParser.parse(xmlReader);            
    }
    
    void moveCursor(int tagID) throws GraphException
    {
        _graph.addEdge(_currentNodeID, tagID, "edge");

        _cursorHistory.push(_currentNodeID);
        _currentNodeID = tagID;
    }
    void traceBack() 
    {
        assert !_cursorHistory.empty();
        _currentNodeID = _cursorHistory.pop();
    }
    
    
    
    int getTagID_internal(String tagName)
    {
        int tagID = _graph.getNodeID(tagName);
        if(tagID == -1)
        {
            tagID = _graph.add(tagName);
        }
        return tagID;
    }
    
    public void outputGraphviz(OutputStream out) 
    {
        PrintWriter gout = new PrintWriter(out);
        gout.println("digraph G {");
        // output node labels
        for(int tagID : _graph.nodeIDSet())
        {
            gout.println(tagID + " [label=" + StringUtil.quote(_graph.getNode(tagID), "\"") + "];");
        }
        for(int tagID : _graph.nodeIDSet())
        {
            String tagName = _graph.getNode(tagID);
            assert tagName != null;
            Set<Integer> destNodeIDSet = _graph.destNodeIDSet(tagID);
            for(int destNodeID : destNodeIDSet)
            {
            	gout.println(tagID + " -> " + destNodeID + ";");
            }
        }
        
        gout.println("}");
        gout.flush();
    }
    
    private static enum Opt { help }
    
    public static void main(String[] args) throws OptionParserException
    {
        OptionParser<Opt> opt = new OptionParser<Opt>();
        opt.addOption(Opt.help, "h", "help", "display help messages");
        
        opt.parse(args);
        if(opt.isSet(Opt.help) || opt.getArgumentLength() < 1)
        {
            printHelpMessage(opt);
            return;
        }
    
        DataGuide dg = new DataGuide();
        try
        {
            dg.generateFrom(opt.getArgument(0));
            dg.outputGraphviz(System.out);
        }
        catch (XerialException e)
        {
            System.err.println(e.getMessage());
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
        }
    }
    
    private static void printHelpMessage(OptionParser<Opt> opt)
    {
        System.out.println("usage: > java -jar DataGuide.jar [option] xml_file");
        System.out.println(opt.helpMessage());
    }
}