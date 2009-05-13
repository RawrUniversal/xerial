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
// Tuple.java
// Since: 2009/05/13 9:19:34
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.relation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.xerial.core.XerialError;
import org.xerial.core.XerialErrorCode;

/**
 * Tuple is a list of {@link Cell}s. Tuple class allows Non-1NF representation
 * of the data.
 * 
 * @author leo
 * 
 */
public class Tuple implements Cell
{

    private final List<Cell> nodeList;

    public Tuple()
    {
        this.nodeList = new ArrayList<Cell>();
    }

    public Tuple(Tuple other)
    {
        this(other.nodeList);
    }

    public Tuple(int tupleSize)
    {
        this.nodeList = new ArrayList<Cell>(tupleSize);
    }

    public Tuple(List<Cell> nodeList)
    {
        this.nodeList = new ArrayList<Cell>(nodeList.size());
        for (Cell each : nodeList)
        {
            this.nodeList.add(each);
        }
    }

    //    public static Tuple emptyTuple(Schema schema)
    //    {
    //        List<Cell> nodeList = new ArrayList<Cell>(schema.size());
    //        for (int i = 0; i < schema.size(); i++)
    //        {
    //            Schema subSchema = schema.get(i);
    //            if (subSchema.isAtom())
    //                nodeList.add(null);
    //            else
    //                nodeList.add(emptyTuple(subSchema));
    //        }
    //        return new Tuple(nodeList);
    //    }

    public void add(Cell node)
    {
        nodeList.add(node);
    }

    public void set(int index, Cell node)
    {
        nodeList.set(index, node);
    }

    public void set(TupleIndex index, Cell node)
    {
        if (!index.hasTail())
        {
            set(index.get(0), node);
            return;
        }

        // nested node
        Cell target = get(index.get(0));
        if (target == null || !target.isTuple())
            throw new XerialError(XerialErrorCode.INVALID_STATE, String.format(
                    "set to invalid element: index = %s in %s", index, this));

        ((Tuple) target).set(index.tail(), node);
    }

    public int size()
    {
        return nodeList.size();
    }

    public void clear()
    {
        nodeList.clear();
    }

    public boolean isEmpty()
    {
        return nodeList.isEmpty();
    }

    public void sort(Comparator<Cell> comparator)
    {
        Collections.sort(nodeList, comparator);
    }

    public Iterator<Cell> iterator()
    {
        return nodeList.iterator();
    }

    public Cell get(int index)
    {
        return nodeList.get(index);
    }

    private static <T> String join(Collection<T> c, String concatinator)
    {
        if (c == null)
            return "";
        int size = c.size();
        if (size == 0)
            return "";

        Iterator<T> it = c.iterator();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; it.hasNext() && i < size - 1; i++)
        {
            Object data = it.next();
            if (data != null)
                buf.append(data.toString());
            else
                buf.append("null");
            buf.append(concatinator);
        }
        Object lastData = it.next();
        if (lastData != null)
            buf.append(lastData.toString());
        else
            buf.append("null");
        return buf.toString();
    }

    @Override
    public String toString()
    {
        return String.format("[%s]", join(nodeList, ", "));
    }

    public boolean addAll(List<Cell> relationFragment)
    {
        return nodeList.addAll(relationFragment);
    }

    public Node getNode()
    {
        throw new XerialError(XerialErrorCode.UNSUPPORTED);
    }

    public List<Cell> getNodeList()
    {
        return nodeList;
    }

    public boolean isAtom()
    {
        return true;
    }

    public boolean isTuple()
    {
        return true;
    }

    public Cell get(TupleIndex index)
    {
        Cell cell = nodeList.get(index.get(0));
        if (index.hasTail())
            return cell.get(index.tail());
        else
            return cell;
    }

    public Node getNode(int index)
    {
        Cell node = get(index);
        if (node.isAtom())
            return Node.class.cast(node);
        else
            throw new XerialError(XerialErrorCode.MISSING_ELEMENT, "node is not found: " + index);
    }

    public Node getNode(TupleIndex index)
    {
        Cell node = get(index);
        if (node == null)
            return null;

        if (node.isAtom())
            return Node.class.cast(node);
        else
            throw new XerialError(XerialErrorCode.MISSING_ELEMENT, "node is not found: " + index);

    }

    public void accept(CellVisitor visitor)
    {
        visitor.visitTuple(this);
    }

    public Tuple getTuple()
    {
        return this;
    }

}