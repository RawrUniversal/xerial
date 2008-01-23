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
// Sample.java
// Since: Dec 19, 2007 4:47:44 PM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.util.bean.impl;

public class Sample
{
    int id;
    String name;
    
    public Sample()
    {}
    
    /**
     * @param id
     * @param name
     */
    public Sample(int id, String name)
    {
        this.id = id;
        this.name = name;
    }
    public int getId()
    {
        return id;
    }
    public void setId(int id)
    {
        this.id = id;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    
    public String toString()
    {
        return "id=" + id + " name=" + name;
    }
}