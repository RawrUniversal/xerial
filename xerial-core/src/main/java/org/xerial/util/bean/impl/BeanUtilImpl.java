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
// BeanUtilImpl.java
// Since: Dec 19, 2007 4:38:26 PM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.util.bean.impl;

import java.util.Map;

import org.xerial.core.XerialException;
import org.xerial.util.bean.MapWalker;
import org.xerial.util.tree.TreeWalker;

/**
 * Implementations of BeanUtil
 * 
 * @author leo
 * 
 */
public class BeanUtilImpl {
    /**
     * Create a bean of the specified type from the tree walker input
     * 
     * @param <E>
     * @param treeWalker
     * @param beanType
     * @return bean
     * @throws XerialException
     */

    /**
     * Create a bean object using a given tree walker
     * 
     * @param <E>
     * @param treeWalker
     * @param beanType
     * @return
     * @throws XerialException
     */
    public static <E> E createTypedBean(TreeWalker treeWalker, Class<E> beanType)
            throws XerialException {
        return beanType.cast(createBean(treeWalker, new BeanBindingProcess(beanType)));
        //return new ObjectMapper().map(beanType, treeWalker);
    }

    /**
     * Create a bean object using a given tree walker
     * 
     * @param treeWalker
     * @param bean
     * @return
     * @throws XerialException
     */
    @SuppressWarnings("unchecked")
    public static <E> E createBean(TreeWalker treeWalker, E bean) throws XerialException {
        return (E) createBean(treeWalker, new BeanBindingProcess(bean));
        //return new ObjectMapper().map(bean, treeWalker);
    }

    public static Object createBean(TreeWalker treeWalker, BeanBindingProcess beanBindingVisitor)
            throws XerialException {
        treeWalker.walk(beanBindingVisitor);
        return beanBindingVisitor.getResultBean();
    }

    public static <E> E createBeanFromMap(Class<E> beanType, Map< ? , ? > map)
            throws XerialException {
        return createTypedBean(new MapWalker(map), beanType);
    }

    public static Object populateBeanWithMap(Object bean, Map< ? , ? > map) throws XerialException {
        return createBean(new MapWalker(map), bean);
    }

}
