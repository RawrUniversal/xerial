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
// SilkParser.java
// Since: Jun 1, 2009 2:45:39 PM
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.silk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.xerial.core.XerialErrorCode;
import org.xerial.core.XerialException;
import org.xerial.json.JSONArray;
import org.xerial.json.JSONEvent;
import org.xerial.json.JSONException;
import org.xerial.json.JSONObject;
import org.xerial.json.JSONPullParser;
import org.xerial.json.JSONUtil;
import org.xerial.json.JSONValue;
import org.xerial.json.JSONValueType;
import org.xerial.silk.model.SilkDataLine;
import org.xerial.silk.model.SilkFunction;
import org.xerial.silk.model.SilkFunctionArg;
import org.xerial.silk.model.SilkJSONValue;
import org.xerial.silk.model.SilkNode;
import org.xerial.silk.model.SilkNodeOccurrence;
import org.xerial.silk.model.SilkValue;
import org.xerial.silk.plugin.SilkFunctionArgument;
import org.xerial.silk.plugin.SilkFunctionPlugin;
import org.xerial.util.FileResource;
import org.xerial.util.ReflectionUtil;
import org.xerial.util.StringUtil;
import org.xerial.util.TypeInfo;
import org.xerial.util.log.Logger;
import org.xerial.util.tree.TreeEventHandler;
import org.xerial.util.tree.TreeParser;
import org.xerial.util.tree.impl.TreeEventQueue;

/**
 * Push-style Silk format parser
 * 
 * @author leo
 * 
 */
public class SilkParser implements SilkEventHandler, TreeParser
{
    private static Logger          _logger     = Logger.getLogger(SilkParser.class);

    private final SilkParserBase   parser;
    private final SilkEnv          parseContext;
    private final SilkParserConfig config;

    private final TreeEventQueue   eventQueue  = new TreeEventQueue();
    private long                   numReadLine = 0;

    /**
     * Creates a new reader with the specified reader
     * 
     * @param input
     * @throws IOException
     */
    public SilkParser(Reader input) throws IOException {
        this(input, SilkEnv.newEnv());
    }

    public SilkParser(InputStream input) throws IOException {
        this(new InputStreamReader(input));
    }

    /**
     * Creates a new reader for the given input file
     * 
     * @param input
     * @throws IOException
     */
    public SilkParser(File input) throws IOException {
        this(new BufferedReader(new FileReader(input)), SilkEnv.newEnv(getResourceBasePath(input)));
    }

    /**
     * Creates a new reader inherited the given environment
     * 
     * @param input
     * @param env
     * @throws IOException
     */
    public SilkParser(Reader input, SilkEnv env) throws IOException {
        this(input, env, new SilkParserConfig());
    }

    /**
     * Create a new reader for reading the specified resource URL
     * 
     * @param resourcePath
     * @throws IOException
     */
    public SilkParser(URL resourcePath) throws IOException {
        this(resourcePath, SilkEnv.newEnv());
    }

    public SilkParser(URL resourcePath, SilkParserConfig config) throws IOException {
        this(resourcePath, SilkEnv.newEnv(), config);
    }

    public SilkParser(URL resource, SilkEnv env) throws IOException {
        this(resource, env, new SilkParserConfig());
    }

    public SilkParser(URL resource, SilkEnv env, SilkParserConfig config) throws IOException {
        this(new InputStreamReader(resource.openStream(), "UTF8"), SilkEnv.newEnv(env, getResourceBasePath(resource)),
                config);
    }

    public SilkParser(Reader input, SilkEnv env, SilkParserConfig config) throws IOException {
        this.config = config;
        if (config.numWorkers > 1)
            this.parser = new SilkLineFastParser(input);
        else
            this.parser = new SilkLinePushParser(input);
        this.parseContext = env;
    }

    static String getResourceBasePath(File resourcePath) throws IOException {
        try {
            URL resource = resourcePath.toURI().toURL();
            return getResourceBasePath(resource);
        }
        catch (MalformedURLException e) {
            throw new IOException("invalid path: " + resourcePath);
        }
    }

    static String getResourceBasePath(URL resource) {
        String path = resource.toExternalForm();
        int fileNamePos = path.lastIndexOf("/");
        String resourceBasePath = fileNamePos > 0 ? path.substring(0, fileNamePos) : null;
        return resourceBasePath;
    }

    /**
     * Concatenates the base path and the resource name
     * 
     * @param resourceBasePath
     * @param resourceName
     * @return
     */
    static String getResourcePath(String resourceBasePath, String resourceName) {
        String resourcePath = resourceBasePath;
        if (!resourcePath.endsWith("/"))
            resourcePath += "/";
        resourcePath += resourceName;
        return resourcePath;
    }

    TreeEventHandler handler;

    public void parse(TreeEventHandler handler) throws Exception {
        this.handler = handler;

        handler.init();
        parser.parse(this);
        closeContextUpTo(parseContext.getIndentationOffset());

        handler.finish();
    }

    public void parseWithoutInitAndFinish(TreeEventHandler handler) throws Exception {
        this.handler = handler;

        parser.parse(this);
        closeContextUpTo(parseContext.getIndentationOffset());
    }

    public void handle(SilkEvent event) throws Exception {
        try {
            switch (event.getType()) {
            case DATA_LINE:
                parseDataLine(event);
                break;
            case NODE:
            case BLOCK_NODE:
                SilkNode newContextNode = SilkNode.class.cast(event.getElement());
                openContext(newContextNode);
                break;
            case FUNCTION:
                SilkFunction function = SilkFunction.class.cast(event.getElement());
                evalFunction(function);
                break;
            case BLANK_LINE:
                break;
            case END_OF_FILE:
                break;
            case PREAMBLE:
                break;
            case COMMENT_LINE:
                break;
            case MULTILINE_SEPARATOR:
                break;
            case UNKNOWN:
            default:
                _logger.warn(String.format("unknown event type (line=%d): %s", numReadLine, event));
                break;
            }
        }
        catch (XerialException e) {
            // When finding the parse error, only report warning message with the line number
            if (e.getErrorCode() == XerialErrorCode.PARSE_ERROR)
                _logger.warn(String.format("parse error at line=%d: %s", numReadLine, e));
            else
                throw e;

        }

        numReadLine++;
    }

    private void parseDataLine(SilkEvent currentEvent) throws Exception {
        // pop the context stack until finding a node for stream data node occurrence
        while (!parseContext.isContextNodeStackEmpty()) {
            SilkContext context = parseContext.peekLastContext();
            SilkNode node = context.contextNode;
            if (!node.getOccurrence().isFollowedByStreamData()) {
                parseContext.popLastContext();
                if (context.isOpen)
                    leave(node.getName());
            }
            else
                break;
        }

        if (parseContext.isContextNodeStackEmpty()) {
            // use default column names(c1, c2, ...) 
            SilkDataLine line = SilkDataLine.class.cast(currentEvent.getElement());
            ArrayList<String> columns = StringUtil.splitAtTab(line.getTrimmedDataLine());
            int index = 1;
            visit("row", null);
            for (String each : columns) {
                String columnName = String.format("c%d", index++);

                // TODO use evalColumnData
                visit(columnName, each);
                leave(columnName);
            }
            leave("row");
        }
        else {
            SilkContext context = parseContext.peekLastContext();
            SilkNode schema = context.contextNode;
            SilkDataLine line = SilkDataLine.class.cast(currentEvent.getElement());
            switch (schema.getOccurrence()) {
            case SEQUENCE:
                text(line.getTrimmedDataLine());
                break;
            case SEQUENCE_PRESERVING_WHITESPACES:
                text(line.getDataLine() + "\n");
                break;
            case ZERO_OR_MORE:
                // CSV data
            {
                evalDatalineColumn(schema, line.getTrimmedDataLine());
            }
                break;
            case TABBED_SEQUENCE: {
                ArrayList<String> columns = StringUtil.splitAtTab(line.getTrimmedDataLine());

                int columnIndex = 0;
                visit(schema.getName(), schema.hasValue() ? schema.getValue().toString() : null);
                for (int i = 0; i < schema.getChildNodes().size(); i++) {
                    SilkNode child = schema.getChildNodes().get(i);
                    if (child.hasValue()) {
                        // output the default value for the column 
                        evalDatalineColumn(child, child.getValue().toString());
                    }
                    else {
                        if (columnIndex < columns.size()) {
                            String columnData = columns.get(columnIndex++).trim();
                            if (columnData.length() > 0)
                                evalDatalineColumn(child, columnData);
                        }
                    }
                }
                leave(schema.getName());
                break;
            }
            }
        }

    }

    /**
     * Enqueues a visit event.
     * 
     * @param nodeName
     * @param immediateNodeValue
     * @throws Exception
     */
    private void visit(String nodeName, String immediateNodeValue) throws Exception {
        handler.visitNode(nodeName, immediateNodeValue);

    }

    /**
     * Enqueues a leave event
     * 
     * @param nodeName
     * @throws Exception
     */
    private void leave(String nodeName) throws Exception {
        handler.leaveNode(nodeName);
    }

    /**
     * Enqueues a text event
     * 
     * @param textFragment
     * @throws XerialException
     */
    private void text(String textFragment) throws Exception {
        handler.text(parseContext.getContextNode().getName(), textFragment);
    }

    /**
     * Closed pre-opened contexts up to the specified indent level
     * 
     * @param newIndentLevel
     * @throws Exception
     */
    private void closeContextUpTo(int newIndentLevel) throws Exception {
        while (!parseContext.isContextNodeStackEmpty()) {
            SilkContext context = parseContext.peekLastContext();
            SilkNode node = context.contextNode;
            if (node.getIndentLevel() >= newIndentLevel) {
                parseContext.popLastContext();

                if (context.isOpen) {
                    // close context
                    String nodeName = node.getName();
                    leave(nodeName);
                }
            }
            else
                return;
        }
    }

    /**
     * Opens a new context for the given node
     * 
     * @param node
     *            new context node
     * @param visitor
     * @throws Exception
     */
    private void openContext(SilkNode node) throws Exception {
        int indentLevel = node.getIndentLevel();
        if (indentLevel != SilkNode.NO_INDENT)
            indentLevel += parseContext.getIndentationOffset();

        closeContextUpTo(indentLevel);
        openContext_internal(node);
    }

    private void openContext_internal(SilkNode node) throws Exception {

        // no name node handling
        if (node.getName() == null) {
            for (SilkNode eachChild : node.getChildNodes()) {
                openContext_internal(eachChild);
            }
            return;
        }

        // push the current node context to the stack
        SilkContext currentContext = new SilkContext(node, true);
        parseContext.pushContext(currentContext);

        SilkNodeOccurrence occurrence = node.getOccurrence();
        if (node.isTableSchema()) {
            currentContext.isOpen = false;
            return; // do not invoke visit events
        }

        String nodeName = node.getName();
        SilkValue textValue = node.getValue();

        // process text values attached to the node
        if (textValue != null) {
            // When the text data is JSON, traverses the JSON data 
            if (textValue.isJSON()) {

                String type = node.getDataType();
                if (type != null && type.equals("json")) {
                    SilkJSONValue jsonValue = SilkJSONValue.class.cast(textValue);
                    if (jsonValue.isObject()) {
                        visit(nodeName, null);
                        JSONObject jsonObj = new JSONObject(jsonValue.getValue());
                        walkJSONObject(jsonObj);
                    }
                    else {
                        currentContext.isOpen = false;
                        JSONArray jsonArray = new JSONArray(jsonValue.getValue());
                        walkJSONAray(jsonArray, nodeName);
                    }
                }
                else {
                    visit(nodeName, textValue.toString());
                }
            }
            else if (textValue.isFunction()) {
                // evaluate the function 
                visit(nodeName, null);
                SilkFunction function = SilkFunction.class.cast(textValue);
                evalFunction(function);

                return;
            }
            else if (occurrence == SilkNodeOccurrence.ZERO_OR_MORE) {
                currentContext.isOpen = false;
                // CSV data: A, B, C, ...
                for (String each : StringUtil.splitCSV(textValue.getValue())) {
                    visit(nodeName, each.trim());
                    leave(nodeName);
                }
                return;
            }
            else {
                // Simple text value will be reported as it is.
                visit(nodeName, textValue.toString());
            }
        }
        else {
            if (occurrence == SilkNodeOccurrence.ZERO_OR_MORE) {
                // CSV data
                return; // do not invoke visit events
            }

            // Report a visit event without text value
            visit(nodeName, null);
        }

        // Traverse attribute nodes having text values. If no text value is specified for these attributes, 
        // they are schema elements for the following DATA_LINE. 
        for (SilkNode eachChild : node.getChildNodes()) {
            if (eachChild.hasValue()) {
                openContext(eachChild);
            }
        }

    }

    /**
     * Evaluate the function
     * 
     * @param function
     * @throws Exception
     */
    private void evalFunction(SilkFunction function) throws Exception {
        SilkFunctionPlugin plugin = getPlugin(function.getName());
        if (plugin == null) {
            _logger.error(String.format("plugin %s not found", function.getName()));
            return;
        }
        // fill the function argument to the plugin instance
        populate(plugin, function.getArgumentList());

        // evaluate the function
        SilkEnv env = parseContext.newEnvFor(function);
        plugin.init(env);
        plugin.eval(env, handler);
    }

    /**
     * Has finished reading the stream?
     */
    private final boolean hasFinished = false;

    private void walkMicroFormatRoot(SilkNode schemaNode, JSONArray value) throws Exception {
        // e.g., exon(start, name)

        if (schemaNode.hasManyOccurrences()) {
            if (schemaNode.hasChildren()) {
                // e.g., exon(start, name)*
                // multiple occurrences: [[start, end], [start, end], ... ] 
                for (int i = 0; i < value.size(); i++) {
                    JSONArray eachElement = value.getJSONArray(i);
                    if (eachElement == null)
                        continue;

                    visit(schemaNode.getName(), null);
                    int index = 0;
                    for (SilkNode eachSubSchema : schemaNode.getChildNodes()) {
                        walkMicroFormatElement(eachSubSchema, eachElement.get(index++));
                    }
                    leave(schemaNode.getName());
                }
            }
            else {
                // e.g. QV*: [20, 50, 50]
                for (int i = 0; i < value.size(); i++) {
                    visit(schemaNode.getName(), value.get(i).toString());
                    leave(schemaNode.getName());
                }
            }
        }
        else {
            // [e1, e2, ...]
            visit(schemaNode.getName(), null);
            int index = 0;
            if (schemaNode.getChildNodes().size() != value.size()) {
                throw new XerialException(XerialErrorCode.INVALID_INPUT, String.format(
                        "data format doesn't match: schema=%s, value=%s", schemaNode, value));
            }
            for (SilkNode each : schemaNode.getChildNodes()) {
                walkMicroFormatElement(each, value.get(index++));
            }
            leave(schemaNode.getName());
        }
    }

    private void walkMicroFormatElement(SilkNode schemaNode, JSONValue value) throws Exception {
        if (schemaNode.hasChildren()) {
            JSONArray array = value.getJSONArray();
            if (array != null)
                walkMicroFormatRoot(schemaNode, array);
            else
                throw new XerialException(XerialErrorCode.INVALID_INPUT, String.format(
                        "data format doesn't match: schema=%s, value=%s", schemaNode, value));
        }
        else {
            visit(schemaNode.getName(), value.toString());
            leave(schemaNode.getName());
        }
    }

    private void evalDatalineColumn(SilkNode node, String columnData) throws Exception {
        // 7600 lines/sec

        if (node.hasChildren()) {
            JSONArray array = new JSONArray(columnData);
            walkMicroFormatRoot(node, array);
            return;
        }

        switch (node.getOccurrence()) {
        case ONE:
            evalColumnData(node, columnData);
            break;
        case ZERO_OR_MORE:
        case ONE_OR_MORE:
            if (columnData.startsWith("[")) {
                // process JSON array

                // 500,080 nodes/s, 2.49 MB/s
                // 591,824 nodes/s, 2.95 MB/s (when using JSONPullParser.getValueAsText())
                new EvalJSON(columnData).parseJSONArray(node);

                // 431,197 nodes/s, 2.15 MB/s
                //JSONArray array = new JSONArray(columnData);
                //walkMicroFormatRoot(node, array);
                return;
            }
            else {
                ArrayList<String> csv = StringUtil.splitCSV(columnData);
                for (String each : csv) {
                    String value = each.trim();
                    evalColumnData(node, value);
                }
                return;
            }
        default:
            evalColumnData(node, columnData);
            return;
        }

    }

    private class EvalJSON
    {
        private final String         json;
        private final JSONPullParser parser;

        EvalJSON(String json) {
            this.json = json;
            this.parser = new JSONPullParser(json);
        }

        void parseJSONArray(SilkNode schemaNode) throws Exception {
            if (schemaNode.hasManyOccurrences()) {
                JSONEvent e = parser.next();
                if (e != JSONEvent.StartArray)
                    throw new XerialException(XerialErrorCode.PARSE_ERROR, "expected [ but " + e);

                if (schemaNode.hasChildren()) {
                    // e.g., exon(start, name)*
                    // multiple occurrences: [[start, end], [start, end], ... ]

                    while ((e = parser.next()) != JSONEvent.EndArray) {
                        JSONValue v = parser.getValue();
                        JSONArray eachElement = v.getJSONArray();
                        if (eachElement == null) {
                            _logger.warn("not an JSONArray: " + v);
                            continue;
                        }

                        visit(schemaNode.getName(), null);
                        int index = 0;
                        for (SilkNode eachSubSchema : schemaNode.getChildNodes()) {
                            walkMicroFormatElement(eachSubSchema, eachElement.get(index++));
                        }
                        leave(schemaNode.getName());

                    }

                }
                else {
                    // e.g. QV*: [20, 50, 50]
                    while ((e = parser.next()) != JSONEvent.EndArray) {
                        visit(schemaNode.getName(), parser.getValueAsText());
                        leave(schemaNode.getName());
                    }

                }
            }
            else {

                // [e1, e2, ...]
                visit(schemaNode.getName(), null);
                int index = 0;

                for (SilkNode each : schemaNode.getChildNodes()) {
                    if (parser.next() == JSONEvent.EndArray) {
                        throw new XerialException(XerialErrorCode.INVALID_INPUT, String.format(
                                "data format doesn't match: schema=%s, value=%s", schemaNode, json));
                    }

                    walkMicroFormatElement(each, parser.getValue());
                }
                leave(schemaNode.getName());
            }

        }

        void parseJSONArrayOf(SilkNode parent) throws XerialException {
            JSONEvent e = parser.next();
            if (e != JSONEvent.StartArray)
                throw new XerialException(XerialErrorCode.PARSE_ERROR, "expected [ but " + e);

        }

    }

    private void evalColumnData(SilkNode node, String columnData) throws Exception {
        try {
            if (node.hasChildren()) {
                // micro-data format
                JSONArray array = new JSONArray(columnData);
                walkMicroFormatRoot(node, array);
                return;
            }

            String dataType = node.getDataType();
            if (dataType != null && dataType.equalsIgnoreCase("json")) {
                JSONValue json = JSONUtil.parseJSON(columnData);
                if (json.getJSONObject() != null) {
                    if (node.getName().equals("_")) // no name object
                    {
                        walkJSONValue(json, node.getName());
                    }
                    else {
                        visit(node.getName(), null);
                        walkJSONValue(json, node.getName());
                        leave(node.getName());
                    }
                }
                else
                    walkJSONValue(json, node.getName());
            }
            else {
                visit(node.getName(), StringUtil.unquote(columnData));
                leave(node.getName());
            }
        }
        catch (JSONException e) {
            throw new XerialException(e.getErrorCode(), String.format("line=%d: %s", numReadLine, e.getMessage()));
        }

    }

    private void walkJSONAray(JSONArray jsonArray, String parentNodeName) throws Exception {
        for (JSONValue each : jsonArray) {
            walkJSONValue(each, parentNodeName);
        }
    }

    private void walkJSONObject(JSONObject jsonObj) throws Exception {
        for (String key : jsonObj.keys()) {
            JSONValue val = jsonObj.get(key);
            walkJSONValue(val, key);
        }
    }

    private void walkJSONValue(JSONValue value, String parentNodeName) throws Exception {
        JSONValueType type = value.getValueType();
        switch (type) {
        case Array:
            walkJSONAray(value.getJSONArray(), parentNodeName);
            break;
        case Object:
            walkJSONObject(value.getJSONObject());
            break;
        case Boolean:
            visit(parentNodeName, value.toString());
            leave(parentNodeName);
            break;
        case Double:
            visit(parentNodeName, value.toString());
            leave(parentNodeName);
            break;
        case Integer:
            visit(parentNodeName, value.toString());
            leave(parentNodeName);
            break;
        case Null:
            visit(parentNodeName, value.toString());
            leave(parentNodeName);
            break;
        case String:
            visit(parentNodeName, value.toString());
            leave(parentNodeName);
            break;
        }

    }

    /**
     * Plugin holder
     */
    private static Map<String, Class< ? extends SilkFunctionPlugin>> pluginTable = null;

    /**
     * Get the plugin of the specified name
     * 
     * @param name
     *            plugin name
     * @return plugin instance or null if no corresponding plugin is found.
     */
    private SilkFunctionPlugin getPlugin(String name) {
        Class< ? extends SilkFunctionPlugin> pluginClass = getPluginTable().get(name);
        if (pluginClass == null)
            return null;

        try {
            SilkFunctionPlugin pluginInstance = pluginClass.newInstance();
            return pluginInstance;
        }
        catch (InstantiationException e) {
            _logger.warn(e);
            return null;
        }
        catch (IllegalAccessException e) {
            _logger.warn(e);
            return null;
        }
    }

    private Map<String, Class< ? extends SilkFunctionPlugin>> getPluginTable() {
        if (pluginTable == null) {
            pluginTable = new TreeMap<String, Class< ? extends SilkFunctionPlugin>>();
            // load plugins 
            ClassLoader cl = SilkParser.class.getClassLoader();

            for (Class<SilkFunctionPlugin> each : FileResource.findClasses(SilkFunctionPlugin.class.getPackage(),
                    SilkFunctionPlugin.class, cl)) {
                String functionName = each.getSimpleName().toLowerCase();
                _logger.trace("loaded " + functionName);
                pluginTable.put(functionName, each);
            }

        }

        return pluginTable;
    }

    /**
     * Information of the function (plugin) arguments (
     * {@link SilkFunctionArgument}) described in the Class definition, which
     * implements {@link SilkFunctionPlugin}.
     * 
     * @author leo
     * 
     */
    private static class PluginField
    {
        Field                field;
        SilkFunctionArgument argInfo;

        private PluginField(SilkFunctionArgument argInfo, Field field) {
            this.argInfo = argInfo;
            this.field = field;
        }
    }

    private static class PluginHolder
    {
        Class< ? extends SilkFunctionPlugin> pluginClass;
        ArrayList<PluginField>               argumentFieldList  = new ArrayList<PluginField>();
        Map<String, PluginField>             keyValueFieldTable = new HashMap<String, PluginField>();

        public PluginHolder(Class< ? extends SilkFunctionPlugin> pluginClass) {
            this.pluginClass = pluginClass;

            //ArrayList<SilkFunctionArgument> argDefs = new ArrayList<SilkFunctionArgument>();
            for (Field eachField : pluginClass.getDeclaredFields()) {
                SilkFunctionArgument argInfo = eachField.getAnnotation(SilkFunctionArgument.class);
                if (argInfo != null) {
                    PluginField pf = new PluginField(argInfo, eachField);
                    if (argInfo.name().equals(SilkFunctionArgument.NO_VALUE))
                        argumentFieldList.add(pf);
                    else
                        keyValueFieldTable.put(argInfo.name(), pf);
                }
            }

            // sort arguments in the order of their ordinal
            Collections.sort(argumentFieldList, new Comparator<PluginField>() {
                public int compare(PluginField o1, PluginField o2) {
                    return o1.argInfo.ordinal() - o2.argInfo.ordinal();
                }
            });

        }

        /**
         * Bind function arguments to the plug-in instance
         * 
         * @param plugin
         *            the instance of the plug-in
         * @param args
         *            the function arguments
         */
        public void populate(SilkFunctionPlugin plugin, List<SilkFunctionArg> args) {
            int noNameArgCount = 0;
            for (SilkFunctionArg eachArgument : args) {
                String argValue = eachArgument.getValue().toString();
                try {
                    if (eachArgument.hasName()) {
                        // key value arg
                        PluginField f = keyValueFieldTable.get(eachArgument.getName());
                        if (f == null) {
                            _logger.warn("unknown argument: " + eachArgument);
                            continue;
                        }
                        ReflectionUtil.setFieldValue(plugin, f.field, argValue);
                    }
                    else {
                        // unnamed argument
                        // matching argument order
                        if (noNameArgCount >= argumentFieldList.size()) {
                            _logger.warn(String.format("no corresponding field for the argument %s is found",
                                    eachArgument));
                            continue;
                        }
                        PluginField f = argumentFieldList.get(noNameArgCount);
                        ReflectionUtil.setFieldValue(plugin, f.field, argValue);

                        if (!TypeInfo.isCollection(f.field.getType()))
                            noNameArgCount++;
                    }
                }
                catch (XerialException e) {
                    _logger.error(e);
                }

            }
        }

    }

    /**
     * Fill the plug-in argument fields with the given arguments
     * 
     * @param plugin
     *            plug-in instance.
     * @param args
     *            function arguments.
     */
    private static void populate(SilkFunctionPlugin plugin, List<SilkFunctionArg> args) {
        PluginHolder holder = new PluginHolder(plugin.getClass());
        holder.populate(plugin, args);
    }

    public long getNumReadLine() {
        return numReadLine;
    }
}
