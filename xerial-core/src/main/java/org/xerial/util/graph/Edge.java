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
// XerialJ
//
// Edge.java
// Since: 2004/12/27
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.util.graph;

import org.xerial.util.Pair;

public class Edge implements Comparable<Edge>
{
	int srcNodeID;
	int destNodeID;

	/**
	 * @param edge
	 * @param src
	 * @param dest
	 */
	public Edge(int src, int dest) {
		this.srcNodeID = src;
		this.destNodeID = dest;
	}

	public int getDestNodeID() {
		return destNodeID;
	}

	public int getSourceNodeID() {
		return srcNodeID;
	}

	public boolean equals(Object o)
	{
		if(o instanceof Edge) {
			Edge e = (Edge) o;
			return (srcNodeID == e.getSourceNodeID()) && (destNodeID == e.getDestNodeID());
		}
		else 
			return false;
	}
	
	public int compareTo(Edge o) {
		int diff = srcNodeID - o.getSourceNodeID();
		return (diff != 0) ? diff : destNodeID - o.getDestNodeID();
	}
	
	public String toString()
	{
		return "(" + srcNodeID + ", " + destNodeID + ")";
	}
}
