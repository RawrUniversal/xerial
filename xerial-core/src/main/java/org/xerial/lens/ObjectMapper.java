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
// ObjectMapper.java
// Since: May 19, 2009 1:29:23 PM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.lens;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.xerial.core.XerialError;
import org.xerial.core.XerialErrorCode;
import org.xerial.core.XerialException;
import org.xerial.relation.Node;
import org.xerial.relation.query.AmoebaJoinHandler;
import org.xerial.relation.query.QuerySet;
import org.xerial.relation.query.StreamAmoebaJoin;
import org.xerial.relation.schema.Schema;
import org.xerial.relation.schema.SchemaBuilder;
import org.xerial.util.bean.TypeInformation;
import org.xerial.util.log.Logger;
import org.xerial.util.tree.TreeWalker;

/**
 * Object-Tree mapping processor
 * 
 * @author leo
 * 
 */
public class ObjectMapper
{
    private static Logger _logger = Logger.getLogger(ObjectMapper.class);
    private HashMap<Long, Object> objectHolder = new HashMap<Long, Object>();

    public ObjectMapper()
    {

    }

    public <T> T map(Class<T> targetType, TreeWalker walker) throws XerialException
    {
        T object = TypeInformation.createInstance(targetType);
        return map(object, walker);
    }

    public <T> T map(T object, TreeWalker walker) throws XerialException
    {
        try
        {
            if (object == null)
                throw new XerialError(XerialErrorCode.INVALID_INPUT, "null object");

            QuerySet qs = buildQuery(object.getClass());

            if (_logger.isDebugEnabled())
                _logger.debug("query set: " + qs);

            // set the root object
            objectHolder.put(0L, object);

            StreamAmoebaJoin aj = new StreamAmoebaJoin(qs, new RelationExtracter());
            aj.sweep(walker);
            return object;
        }
        catch (IOException e)
        {
            throw new XerialException(XerialErrorCode.IO_EXCEPTION, e);
        }

    }

    private QuerySet buildQuery(Class< ? > targetType)
    {
        QuerySet qs = new QuerySet();

        Set<Class< ? >> relatedClasses = findRelatedClasses(targetType);
        for (Class< ? > eachClass : relatedClasses)
        {
            ObjectLens lens = ObjectLens.getObjectLens(eachClass);

            for (ParameterSetter each : lens.getSetterList())
            {

            }

            for (RelationSetter each : lens.getRelationSetterList())
            {
                qs.addQueryTarget(new SchemaBuilder().add(each.getCoreNodeName()).add(each.getAttributeNodeName())
                        .build());
            }
        }

        return qs;
    }

    private Set<Class< ? >> findRelatedClasses(Class< ? > targetType)
    {
        HashSet<Class< ? >> foundClasses = new HashSet<Class< ? >>();
        findRelatedClasses_internal(foundClasses, targetType);
        return foundClasses;
    }

    private void findRelatedClasses_internal(Set<Class< ? >> foundClasses, Class< ? > targetType)
    {
        if (TypeInformation.isBasicType(targetType))
            return;

        if (foundClasses.contains(targetType))
            return;

        foundClasses.add(targetType);

        ObjectLens lens = ObjectLens.getObjectLens(targetType);
        if (_logger.isDebugEnabled())
            _logger.debug(String.format("class %s\n%s", targetType.getSimpleName(), lens));

        for (ParameterSetter each : lens.getSetterList())
        {
            findRelatedClasses_internal(foundClasses, each.getTargetClass());
        }

        for (RelationSetter each : lens.getRelationSetterList())
        {
            findRelatedClasses_internal(foundClasses, each.getCoreNodeType());
            findRelatedClasses_internal(foundClasses, each.getAttributeNodeType());
        }

    }

    private class RelationExtracter implements AmoebaJoinHandler
    {

        public void leaveNode(Schema schema, Node node)
        {

        }

        public void newAmoeba(Schema schema, Node coreNode, Node attributeNode)
        {
            if (_logger.isDebugEnabled())
                _logger.debug(String.format("amoeba: (%s, %s) in %s", coreNode, attributeNode, schema));
        }

        public void text(Schema schema, Node coreNode, String nodeName, String text)
        {

        }

    }

}
