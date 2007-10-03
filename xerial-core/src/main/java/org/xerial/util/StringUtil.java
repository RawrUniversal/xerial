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
//--------------------------------------
// org.xerial.util Project
// 
// StringUtil.java 
// Since: 2004/12/30
//
// $URL$ 
// $Author$
//--------------------------------------

package org.xerial.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Collection;
import java.util.Iterator;

/**
 * A utility for manipulating Strings
 * @author leo
 *
 */
public class StringUtil
{
    public static final String SINGLE_QUOTE = "\'";
    public static final String DOUBLE_QUOTE = "\"";
    public static final String NEW_LINE = System.getProperty("line.separator");

    private StringUtil() {} 
    
    /**
     * Concatinates all elements in the given collection c into a single string with the separator
     * @param c a collection of elements to concatenate
     * @param concatinator a concatenator: ex. ", ", "." etc.  
     * @return a concatenated string
     */
    public static String join(Collection c, String concatinator)
    {
    	if(c == null)
    		return "";
        int size = c.size();
        if(size == 0)
        	return "";
        
        Iterator it = c.iterator();
        StringBuilder buf = new StringBuilder();	
        for(int i=0; it.hasNext() && i<size-1; i++)
        {
            Object data = it.next();
            buf.append(data.toString());
            buf.append(concatinator);
        }
        Object lastData = it.next();
        buf.append(lastData.toString());
        return buf.toString();
    }
    
    /**
     * Concatinates all elements in the given array c into a single string with the separator
     * @param c an array of elements to concatenate
     * @param concatinator a concatenator: ex. ", ", "." etc.  
     * @return the concatenated string
     */
    public static String join(Object[] c, String concatinator)
    {
    	if(c == null)
    		return "";
        int size = c.length;
        if(size == 0)
        	return "";
        
        StringBuilder buf = new StringBuilder();	
        for(int i=0; i<size-1; i++)
        {
            Object data = c[i];
            buf.append(data != null ? data.toString() : "");
            buf.append(concatinator);
        }
        buf.append(c[size-1]);
        return buf.toString();
    }

    /**
     * quote a given message with an quotation mark
     * @param message 
     * @param quotationMark StringUtil.SINGLE_QUOTE, StringUtil.DOUBLE_QUOTE, or other strings.
     * @return the quoted message
     */
    public static String quote(String message, String quotationMark)
    {
    	if(message == null)
    		return message;
        return quotationMark + message + quotationMark;
    }
    
    public static String unquote(String message)
    {
    	if(message == null)
    		return message;
    	if(message.length() > 1)
    	{
    		if((message.charAt(0) == '"' && message.charAt(message.length()-1) == '"') 
    		 || (message.charAt(0) == '\'' && message.charAt(message.length()-1) == '\'')) 
    			return message.substring(1, message.length()-1);
    	}
    	return message;
    }

    
    static Pattern _whiteSpacePattern = Pattern.compile("[ \t\n\r]*"); 
    
    static public boolean isWhiteSpace(String s)
    {
        Matcher m = _whiteSpacePattern.matcher(s);
        return m.matches();
    }

	public static String concatinateWithTab(Object... data) {
		return StringUtil.join(data, "\t");
	}
	
	public static String newline()
	{
	    return NEW_LINE;
	}
}


