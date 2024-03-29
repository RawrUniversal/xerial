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
// SilkValue.java
// Since: 2009/02/01 23:42:52
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.silk.model;

import java.io.IOException;

import org.xerial.util.io.BinaryReader;
import org.xerial.util.io.BinaryWriter;

/**
 * text value of nodes or functions
 * 
 * @author leo
 * 
 */
public abstract class SilkValue implements SilkElement {
    public abstract boolean isJSON();

    public abstract boolean isFunction();

    /**
     * Returns the string value of this element
     * 
     * @return
     */
    public abstract String getValue();

    public void toBinary(BinaryWriter out) throws IOException {
        if (isFunction()) {
            out.writeInt(2);
        }
        if (isJSON()) {
            out.writeInt(1);
            out.writeString(getValue());
        }
        else {
            out.writeInt(0);
            out.writeString(getValue());
        }
    }

    public static SilkValue newInstance(BinaryReader in) throws IOException {
        int type = in.readInt();
        switch (type) {
        case 0:
            return new SilkTextValue(in.readString());
        case 1:
            return new SilkJSONValue(in.readString());
        case 2:
            return new SilkFunction();
        }

        throw new IOException("unknown value type: " + type);
    }

}
