/*--------------------------------------------------------------------------
 *  Copyright 2008 Taro L. Saito
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
// BeanHandler.java
// Since: Jan 22, 2008 10:25:50 AM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.util.bean;

/**
 * An interface to handle a Bean object
 * 
 * @author leo
 * 
 * @param <T>
 */
public interface BeanHandler<T> {
    public void handle(T bean) throws Exception;

    public void handleException(Exception e) throws Exception;
}
