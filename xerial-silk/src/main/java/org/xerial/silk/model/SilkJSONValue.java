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
// SilkJSONValue.java
// Since: 2009/02/01 23:49:10
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.silk.model;

/**
 * text value of JSON format
 * 
 * @author leo
 * 
 */
public class SilkJSONValue extends SilkValue {
    String json;

    public SilkJSONValue(String json) {
        this.json = json;
    }

    @Override
    public String getValue() {
        return json;
    }

    @Override
    public boolean isJSON() {
        return true;
    }

    @Override
    public boolean isFunction() {
        return false;
    }

    @Override
    public String toString() {
        return json;
    }

    public boolean isObject() {
        return json.trim().startsWith("{");
    }

    public boolean isArray() {
        return json.trim().startsWith("[");
    }

}
