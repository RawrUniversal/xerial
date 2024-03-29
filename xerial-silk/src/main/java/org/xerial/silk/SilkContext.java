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
// SilkContext.java
// Since: 2009/03/31 19:56:23
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.silk;

import java.io.IOException;

import org.xerial.silk.model.SilkNode;
import org.xerial.util.io.BinaryWriter;

class SilkContext {
    public final SilkNode contextNode;
    boolean isOpen;

    /**
     * @param contextNode
     * @param isOpen
     *            if true this contest must be closed
     */
    public SilkContext(SilkNode contextNode, boolean isOpen) {
        this.contextNode = contextNode;
        this.isOpen = isOpen;
    }

    @Override
    public String toString() {
        return String.format("%s%s", contextNode, isOpen ? "(open)" : "");
    }

    public void toBinary(BinaryWriter out) throws IOException {
        out.writeInt(isOpen ? 1 : 0);
        contextNode.toBinary(out);
    }

}
