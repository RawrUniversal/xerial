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
// Help.java
// Since: Apr 23, 2009 6:16:11 PM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.core.cui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.xerial.util.log.Logger;

/**
 * Help command
 * 
 * @author leo
 * 
 */
public class Help implements XerialCommand {
    private static Logger _logger = Logger.getLogger(Help.class);

    public String getName() {
        return "help";
    }

    public String getOneLineDescription() {
        return "display help message";
    }

    public void execute() throws Exception {
        displayCommandList();
    }

    public static void displayCommandList() {
        ArrayList<XerialCommand> commandList = new ArrayList<XerialCommand>();
        for (String commandName : XerialMain.commandTable.keySet()) {
            commandList.add(XerialMain.commandTable.get(commandName));
        }

        // alphabetically sort the commands 
        Collections.sort(commandList, new Comparator<XerialCommand>() {
            public int compare(XerialCommand o1, XerialCommand o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        System.out.println("[command list]");
        // display each command description
        for (XerialCommand each : commandList) {
            System.out.println(String.format(" %-13s %s", each.getName(), each
                    .getOneLineDescription()));
        }
    }

}