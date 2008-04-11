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
// MapPutter.java
// Since: Aug 9, 2007 9:43:22 AM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.util.bean.impl;

import java.lang.reflect.Method;

import org.xerial.json.JSONArray;
import org.xerial.util.bean.BeanException;
import org.xerial.util.bean.BeanUpdator;
import org.xerial.util.bean.BeanUpdatorType;
import org.xerial.util.bean.BeanUtil;

public class MapPutter extends BeanBinderBase implements BeanUpdator
{
    Class keyType;

    Class valueType;

    public MapPutter(Method method, String parameterName, Class keyType, Class valueType) throws BeanException
    {
        super(method, parameterName);
        this.keyType = keyType;
        this.valueType = valueType;

        constractableTest(keyType);
        constractableTest(valueType);
    }

    public Class getInputType()
    {
        throw new UnsupportedOperationException("getElementType() for MapPutter is not supported yet");
    }

    public BeanUpdatorType getType()
    {
        return BeanUpdatorType.MAP_PUTTER;
    }

    public Class getKeyType()
    {
        return keyType;
    }

    public Class getValueType()
    {
        return valueType;
    }

    // @Override
    // public void setXMLData(Object bean, Object xmlData) throws BeanException
    // {
    // if(!TypeInformation.isDOMElement(xmlData.getClass()))
    // return;
    //        
    // Element mapEntryElement = (Element) xmlData;
    // // TODO support for complex putter argument such as putSomething(String
    // key, Map value);
    // String key = mapEntryElement.getAttribute("key");
    // Object value = BeanUtil.createXMLBean(valueType, mapEntryElement);
    // invokeMethod(bean, new Object[] { key, value } );
    // }

}
