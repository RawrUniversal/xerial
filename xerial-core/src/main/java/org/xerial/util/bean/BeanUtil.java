//--------------------------------------
// XerialJ Project
//
// BeanUtil.java
// Since: 2007/03/29
//
// $Date$
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.util.bean;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xerial.json.InvalidJSONDataException;
import org.xerial.json.JSONArray;
import org.xerial.json.JSONBoolean;
import org.xerial.json.JSONDouble;
import org.xerial.json.JSONInteger;
import org.xerial.json.JSONNull;
import org.xerial.json.JSONObject;
import org.xerial.json.JSONString;
import org.xerial.json.JSONValue;
import org.xerial.util.StringUtil;
import org.xerial.util.xml.InvalidXMLException;
import org.xerial.util.xml.XMLAttribute;
import org.xerial.util.xml.XMLException;
import org.xerial.util.xml.XMLGenerator;
import org.xerial.util.xml.dom.DOMUtil;
import org.xerial.util.xml.pullparser.DOMBuilder;
import org.xmlpull.v1.XmlPullParserException;


/**
 * BeanUtil class supports data binding between JSON data and a bean class.
 * 
 * A bean class must have the followings: - A public default (no argument)
 * constructor - To bind data object to a class, there must be public
 * getter/seter methods in the target class, prefixd with get/set. -- Allowable
 * data classes are: ---- primitive types: int/Integer, double/Double,
 * float/Float, boolean/Boolean, String ---- bean classes ---- Collection of
 * allowable data classes. e.g. List<String>, Set<Integer>, etc. ---- Map with
 * key and value classes. key and value also must be allowable classes. ------
 * to bind JSON data to map object, putSomething() method is requried. See the
 * following description.
 * 
 * For example, a Person class with integer id and string name values must have
 * <code>public int getId()</code> and <code>public String getName()</code>,
 * and <code>public void setId(int id)</code> and
 * <code>public void setName(String name)</code> methods.
 * 
 * Usage Example. <code>
 * class Person {
 *   int id;
 *   String name;
 *   public Person();
 *   public Person(int id, String name)
 *   { this.id = id; this.name = name; } 
 *   
 *   public int getId() { return id; }
 *   public String getName() { return name; }
 *   public void setId(int id) { this.id = id; }
 *   public void setName(String name) { this.name = name; }
 * }
 * 
 * Person p1 = new Person(1, "leo");
 * String json = BeanUtil.toJSON(p1); // it will give a json string { "id" : 1, "name" : "leo"}
 * 
 * Person p2 = new Person() // empty data
 * BeanUtil.populate(p2, json);	// fill p2 with a give json data
 * // p2.id = 1, p2.name = "leo"
 * 
 * </code>
 * 
 * 
 * 
 * In the GWT (Google Web Toolkit) of the current version (1.3.3, Mar 2007), the
 * generics feature of Jave (since JDK1.5) cannot be used within the client Java
 * codes. This is a problematic since GWT cannot compile the following method:
 * <code>
 * void setPersonList(List<Person> personList) { this.personList = personList; }
 * </code>
 * So you have to change the argument of the avobe method as: <code>
 * void setPersonList(List personList) { ... }
 * </code>
 * But, with this setter method, we have no information about the element type
 * contained in the List class. So given a JSON data, e.g.,{ "personList" : [
 * {"id":1, "name":"leo"}, {"id":2, "name":"taro"}] }, BeanUtil class cannot
 * instantiate any Person classes.
 * 
 * To resolve this problem, BeanUtil supports data binding via adder methods.
 * For example, instead of using setters, by using the follwing adder, you can
 * load several Person class data as a collection:
 * <code>void addPersonList(Person person) { personList.add(person); }</code>
 * 
 * Note that, in order to use adders correctly, the target of an adder, that is,
 * a collection class, must be initialized before the invocation of the adder.
 * For example, <code> 
 * public PersonList {
 *   Vector<Person> personList = new Vector<Person>();	// it must be initialized. 
 *   public PersonList() {}
 *   void addPersonList(Person p) { personList.add(p); }
 *   public getPersonList() { return personList; } 
 * }
 * </code>
 * 
 * Otherwise, BeanUtil.populate method cuases NullPointerException or some other
 * undisired effects.
 * 
 * In BeanUtil, a setter method, whose argument has no generic type parameter,
 * e.g., <code>public void setList(List l)</code> never be used to bind JSON
 * data to a class instance.
 * 
 * 
 * In order to use a Bean class with Map<K, V> objects, you must define
 * <code>public Map getSomething()</code> and
 * <code>public void putSomething(KeyType key, ValueType value)</code>
 * methods, since BeanUtil learns class types K, V from the putSomething(K key,
 * V value) methods. If a method <code>public void setSomething(Map map)</code>
 * exists, BeanUtil simply ignores it.
 * 
 * 
 * The following is an example of a bean class with Map object. <code>
 * public MapBean
 * {
 *   Map entry = new TreeMap();
 *   
 *   public MapBean(){}
 *   
 *   public void putEntry(Key key, Value v)
 *   {
 *      entry.put(key, value);
 *   }
 *   
 *   public Map getEntry()
 *   {
 *      return entry;
 *   }
 * }
 * </code>
 * 
 * @author leo
 * 
 */
public class BeanUtil {

	private static HashMap<Class, BinderSet> _beanOutputRuleRegistry = new HashMap<Class, BinderSet>();

	private static HashMap<Class, BinderSet> _beanLoadRuleRegistry = new HashMap<Class, BinderSet>();

	public static BeanBinderSet getBeanOutputRule(Class c) throws InvalidBeanException {
		if (_beanOutputRuleRegistry.containsKey(c))
			return _beanOutputRuleRegistry.get(c);
		else {
			BinderSet beanOutputRule = inspectGetters(c);
			_beanOutputRuleRegistry.put(c, beanOutputRule);
			return beanOutputRule;
		}
	}

	public static BeanBinderSet getBeanLoadRule(Class c) throws InvalidBeanException {
		if (_beanLoadRuleRegistry.containsKey(c))
			return _beanLoadRuleRegistry.get(c);
		else {
			BinderSet beanLoadRule = inspectSetter(c);
			_beanLoadRuleRegistry.put(c, beanLoadRule);
			return beanLoadRule;
		}
	}

	/**
	 * retrieve getter methods fo the form: getSomething(), then define how to
	 * use these methods according to the return type of the getter.
	 * 
	 * If the return type is: - primitiveType type: crate a rule that simply
	 * invokes getSomething() method, and recieves a value. - collection iterate
	 * for each element in the collection
	 * 
	 * @param bean
	 * @throws UTGBException
	 */
	private static BinderSet inspectGetters(Class beanClass) throws InvalidBeanException {
		BinderSet outputRuleSet = new BinderSet(beanClass);

		Method[] method = beanClass.getMethods();
		for (Method m : method) {
			if (!Modifier.isPublic(m.getModifiers())) // is public?
				continue;
			String methodName = m.getName();
			String parameterName = BeanUtil.pickPropertyName(methodName);
			if (parameterName == null || parameterName.length() == 0)
				continue;

			if (!methodName.startsWith("get") || methodName.equals("getClass"))
				continue; // Object

			Class[] parameterType = m.getParameterTypes();
			if (parameterType.length != 0) // we cannot use the getter unless
				// it requires no argument.
				continue;
			outputRuleSet.addRule(new Getter(m, parameterName));
		}

		return outputRuleSet;
	}

	public static BinderSet inspectSetter(Class beanClass) throws InvalidBeanException {
		BinderSet inputRuleSet = new BinderSet(beanClass);

		// create a rule set of binding rules from setters of the bean class
		for (Method method : beanClass.getMethods()) {
			String methodName = method.getName();

			String parameterName = BeanUtil.pickPropertyName(methodName); // retrieve
			// setter,
			// getter,
			// adder,
			// putter
			// parameter
			// name
			if (parameterName == null)
				continue;

			if (!Modifier.isPublic(method.getModifiers())) // is public
				// setter/adder/putter?
				continue;

			Class[] parameterType = method.getParameterTypes();
			if (methodName.startsWith("put")) {
				if (parameterType.length != 2)
					continue;

				Class[] mapElementType = resolveActualTypeOfMapElement(beanClass, parameterType);

				if (parameterName.length() == 0 && TypeInformation.isMap(beanClass)) {
					// bean.put(Key k, Value v)
					inputRuleSet.addRule(new MapPutter(method, "-m", mapElementType[0], mapElementType[1]));
				} else {
					// putSomething(Key k, Value v)
					inputRuleSet.addRule(new MapPutter(method, parameterName, mapElementType[0], mapElementType[1]));
				}
				continue;
			}

			if (methodName.startsWith("add")) {
				if (parameterType.length != 1)
					continue;

				Class addType = resolveActualTypeOfCollectionElement(beanClass, parameterType[0]);

				if (parameterName.length() == 0 && TypeInformation.isCollection(beanClass)) {
					// bean.add(E element)
					inputRuleSet.addRule(new CollectionAdder(method, "-c", addType));
				} else {
					// bean.addSomething(E element)
					inputRuleSet.addRule(new CollectionAdder(method, parameterName, addType));
				}
				continue;
			}

			if (methodName.startsWith("set")) {
				if (parameterType.length != 1 || parameterName.length() == 0)
					continue;

				Class inputTypeOfTheSetter = parameterType[0];

				if (inputTypeOfTheSetter.isArray()) {
				    // setSomething(int[] array) etc
					Class componentType = inputTypeOfTheSetter.getComponentType();
					inputRuleSet.addRule(new ArraySetter(method, parameterName, componentType));
				} else if (TypeInformation.isCollection(inputTypeOfTheSetter)) {
					if (method.getGenericParameterTypes()[0] instanceof ParameterizedType) {
						ParameterizedType genericSetterArgumentType = (ParameterizedType) method.getGenericParameterTypes()[0];
						Type[] actualTypeList = genericSetterArgumentType.getActualTypeArguments();
						if (actualTypeList.length > 0) {
							Class elementType = resolveRawType(actualTypeList[0]);
							inputRuleSet.addRule(new CollectionSetter(method, parameterName, inputTypeOfTheSetter, elementType));
						}
					}
					// setSomething(Collection) method wihtout any type
					// parameter cannot be used to bind JSON data, thus skip
				} else if (TypeInformation.isMap(inputTypeOfTheSetter)) {
					// setSomething(Map<K, V> map) method
					ParameterizedType genericSetterArgumentType = getParentParameterizedType(method.getGenericParameterTypes()[0], Map.class);
					if (genericSetterArgumentType != null) {
						Type[] actualTypeList = genericSetterArgumentType.getActualTypeArguments();
						if (actualTypeList.length >= 2) {
							Class keyType = resolveRawType(actualTypeList[0]);
							Class valueType = resolveRawType(actualTypeList[1]);
							inputRuleSet.addRule(new MapSetter(method, parameterName, inputTypeOfTheSetter, keyType, valueType));
							continue;
						}
					}
					// setMap(Map map) without any type parameter cannot be used
				} else {
					inputRuleSet.addRule(new Setter(method, parameterName, inputTypeOfTheSetter));
				}
			}
		}

		return inputRuleSet;
	}

	public static ParameterizedType getParameterizedType(Type t) {
		if (t == null)
			return null;

		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) t;
			return pt;
		}
		if (t instanceof Class)
			return getParameterizedType(((Class) t).getGenericSuperclass());
		else
			return null;
	}

	@SuppressWarnings("unchecked")
	public static ParameterizedType getParentParameterizedType(Type t, Class target) {
		if (t == null)
			return null;

		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) t;
			if (target.isAssignableFrom((Class) pt.getRawType())) {
				return pt;
			}
		}

		if (t instanceof Class) {
			Class c = (Class) t;
			return getParentParameterizedType(c.getGenericSuperclass(), target);
		} else
			return null;
	}

	private static Class resolveActualTypeOfCollectionElement(Type type, Class orig) {
		ParameterizedType pt = getParentParameterizedType(type, Collection.class);
		if (pt != null) {
			Type[] actualType = pt.getActualTypeArguments();
			if (actualType.length > 0)
				return resolveRawType(actualType[0], orig);
		}
		return orig;
	}

	private static Class[] resolveActualTypeOfMapElement(Type type, Class[] orig) {
		ParameterizedType pt = getParentParameterizedType(type, Map.class);
		if (pt != null) {
			Type[] actualType = pt.getActualTypeArguments();
			if (actualType.length > 0)
				return new Class[] { resolveRawType(actualType[0], orig[0]), resolveRawType(actualType[1], orig[1]) };
		}
		return orig;
	}

	private static Class resolveRawType(Type type, Class orig) {
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			return resolveRawType(pt.getRawType(), orig);
		} else if (type instanceof Class)
			return (Class) type;
		else
			return orig;
	}

	private static Class resolveRawType(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			return resolveRawType(pt.getRawType());
		} else if (type instanceof Class)
			return (Class) type;
		else
			return Object.class;
	}

	// non constructable
	private BeanUtil() {
	}

	/**
	 * @param methodName
	 *            method name of getter or setters. e.g. setSomething,
	 *            getSomething
	 * @param patternType
	 *            set(ter) or get(ter)
	 * @return ���\�b�h������擪��set/get����菜����������̐擪��lower case�ɕϊ�����String.
	 *         �؂�o���Ȃ��ꍇ�́Anull ��Ԃ�/
	 */
	static String pickPropertyName(String methodName) {
		Matcher m = null;
		m = _setGetAddMethodPattern.matcher(methodName);
		if (!m.matches())
			return null;
		else {
			if (m.group(2) != null)
				return m.group(3).toLowerCase() + m.group(4);
			else
				return "";
		}
	}

	static private Pattern _setGetAddMethodPattern = Pattern.compile("^(set|get|add|put)((\\S)(\\S*))?");

	private static class BeanToXMLProcess {
		private ByteArrayOutputStream _buffer = new ByteArrayOutputStream();

		private XMLGenerator _out = new XMLGenerator(_buffer);

		public BeanToXMLProcess() {

		}

		public String generateXML(String tagName, Object bean) throws InvalidXMLException, InvalidBeanException {
			try {
				toXML(tagName, bean);
			} catch (IllegalArgumentException e) {
				throw new InvalidBeanException(e);
			} catch (IllegalAccessException e) {
				throw new InvalidBeanException(e);
			} catch (InvocationTargetException e) {
				throw new InvalidBeanException(e);
			}
			_out.endDocument();
			_out.flush();
			return _buffer.toString();
		}

		private void toXML(String tagName, Object bean) throws InvalidXMLException, InvalidBeanException, IllegalArgumentException, IllegalAccessException,
				InvocationTargetException {
			if (bean == null)
				return;

			Class beanClass = bean.getClass();

			if (beanClass.isArray()) {
				Object[] array = (Object[]) bean;
				int i = 0;
				for (; i < array.length - 1; i++) {
					toXML(tagName, array[i]);
					_out.text(",");
				}
				toXML(tagName, array[i]);
			} else if (TypeInformation.isBasicType(beanClass)) {
				_out.element(tagName, bean.toString());
			} else {
				if (TypeInformation.isCollection(beanClass)) {
					Collection collection = (Collection) bean;
					int i = 0;
					for (Object elem : collection) {
						toXML(tagName, elem);
					}
				} 
				else if (TypeInformation.isMap(beanClass)) {
					Map map = (Map) bean;
					
					int i = 0;
					for (Object key : map.keySet()) {
						_out.startTag(tagName, new XMLAttribute("key", key.toString()));
						_out.text(map.get(key).toString());
						_out.endTag();
					}
				} 
				else {
					// return an XML elemenet
					_out.startTag(tagName);
					BeanBinderSet outputRuleSet = BeanUtil.getBeanOutputRule(beanClass);
					for (BeanBinder rule : outputRuleSet.getBindRules()) {
						Method getter = rule.getMethod();
						String parameterName = rule.getParameterName();
						
						Object parameterValue = getter.invoke(bean);
						toXML(parameterName, parameterValue);
					}
					_out.endTag();
				}
			}

		}
	}

	public static String toXML(String tagName, Object bean) throws InvalidXMLException, InvalidBeanException {
		BeanToXMLProcess bp = new BeanToXMLProcess();
		return bp.generateXML(tagName, bean);
	}

	public static String toJSON(Object bean) throws InvalidBeanException {
		return outputAsJSONValue(bean).toString();
	}

	public static JSONObject toJSONObject(Object bean) throws InvalidBeanException {
		return outputAsJSONValue(bean).getJSONObject();
	}

	public static JSONValue getValue(Object bean, String propertyName) throws InvalidBeanException {
		JSONObject json = toJSONObject(bean);
		return json.get(propertyName);
	}

	private static JSONValue outputAsJSONValue(Object bean) throws InvalidBeanException {
		if (bean == null)
			return null;
		Class beanClass = bean.getClass();

		try {
			if (beanClass.isArray()) {
				Object[] array = (Object[]) bean;
				JSONArray jsonArray = new JSONArray();
				for (Object obj : array)
					jsonArray.add(outputAsJSONValue(obj));
				return jsonArray;
			} else if (TypeInformation.isBasicType(beanClass)) {
				String jsonStr = bean.toString();
				JSONValue value = null;
				if (beanClass == String.class)
					value = new JSONString(jsonStr);
				else if (beanClass == int.class || beanClass == Integer.class)
					value = new JSONInteger(jsonStr);
				else if (beanClass == double.class || beanClass == Double.class)
					value = new JSONDouble(jsonStr);
				else if (beanClass == float.class || beanClass == Float.class)
					value = new JSONDouble(jsonStr);
				else if (beanClass == boolean.class || beanClass == Boolean.class)
					value = new JSONBoolean((Boolean) bean);
				else
					throw new InvalidBeanException(beanClass.toString() + " is not basic type");
				return value;
			} else {
				JSONObject json = new JSONObject();

				// return JSONObject
				if (TypeInformation.isCollection(beanClass)) {
					Collection collection = (Collection) bean;
					JSONArray jsonArray = new JSONArray();
					for (Object obj : collection)
						jsonArray.add(outputAsJSONValue(obj));
					json.put("-c", jsonArray);
				} else if (TypeInformation.isMap(beanClass)) {
					Map map = (Map) bean;
					JSONArray jsonArray = new JSONArray();
					for (Object key : map.keySet()) {
						JSONArray pair = new JSONArray();
						pair.add(outputAsJSONValue(key));
						pair.add(outputAsJSONValue(map.get(key)));
						jsonArray.add(pair);
					}
					json.put("-m", jsonArray);
				}

				BeanBinderSet outputRuleSet = BeanUtil.getBeanOutputRule(beanClass);
				for (BeanBinder rule : outputRuleSet.getBindRules()) {
					Method getter = rule.getMethod();
					String parameterName = rule.getParameterName();

					Object parameterValue = getter.invoke(bean);
					json.put(parameterName, outputAsJSONValue(parameterValue));
				}
				return json;
			}
		} catch (IllegalArgumentException e) {
			throw new InvalidBeanException(e);
		} catch (IllegalAccessException e) {
			throw new InvalidBeanException(e);
		} catch (InvocationTargetException e) {
			throw new InvalidBeanException(e);
		}
	}

	
	public static void populateBeanWithXML(Object bean, String xmlData) throws InvalidBeanException, XMLException
	{
		assert(bean != null);
		DOMBuilder domBuilder = new DOMBuilder();
		try {
			Element xmlElement = domBuilder.parse(new StringReader(xmlData));
			populateBeanWithXML(bean, xmlElement);
		} catch (XMLException e) {
			throw new InvalidXMLException(e);
		} catch (XmlPullParserException e) {
			throw new XMLException(e);
		} catch (IOException e) {
			throw new InvalidXMLException(e);
		}
        catch (InstantiationException e)
        {
            throw new InvalidBeanException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new InvalidBeanException(e);
        }
	}
	
	public static void populateBeanWithXML(Object bean, Element xmlElement) throws InvalidXMLException, InvalidBeanException, InstantiationException, IllegalAccessException
	{
	    if(xmlElement == null)
	        return;    // there is nothing to bind
	    
		Class beanClass = bean.getClass();
		BeanBinderSet bindRuleSet = BeanUtil.getBeanLoadRule(beanClass);
		
		// attribute data
		NamedNodeMap attributeMap = xmlElement.getAttributes();
		for(int i=0; i<attributeMap.getLength(); i++)
		{
			Node attributeNode =  attributeMap.item(i);
			if(attributeNode.getNodeType() != Node.ATTRIBUTE_NODE)
			    throw new InvalidXMLException(attributeNode.toString() + " is not an attribute node");
			
			String attributeName = attributeNode.getNodeName();
			String attributeValue = attributeNode.getNodeValue();
			BeanBinder binder = bindRuleSet.findRule(attributeName);
			if(binder != null)
			{
			    binder.invokeXMLDataSetter(bean, attributeValue);
			}
		}
		
		
		// child element data
		NodeList childNodeList = xmlElement.getChildNodes();
		for(int i=0; i<childNodeList.getLength(); i++)
		{
		    Node node = childNodeList.item(i);
		    if(node.getNodeType() == Node.ELEMENT_NODE)
		    {
		        String tagName = node.getNodeName();
		        BeanBinder binder = bindRuleSet.findRule(tagName);
		        if(binder != null)
		        {
		            binder.invokeXMLDataSetter(bean, node);
		        }
		    }
		}
	
		// text data of the xmlElement
		String textData = DOMUtil.getText(xmlElement);
		if(textData != null && textData.trim().length() > 0)
		{
		    BeanBinder binder = bindRuleSet.findRule(xmlElement.getTagName());
		    if(binder != null)
		        binder.invokeXMLDataSetter(bean, textData.trim());
		}
		
	}

	protected static void populateBeanWithXML(Object bean, Object xmlValue) throws InvalidXMLException, InvalidBeanException, InstantiationException, IllegalAccessException
	{
	    if(xmlValue == null)
	        return;
	    
	    Class beanClass = bean.getClass();
	    if(TypeInformation.isBasicType(beanClass))
	    {
	        bean = populateBasicTypeWithXML(beanClass, xmlValue);
	    }
	    else
	    {
	        if(TypeInformation.isDOMElement(xmlValue.getClass()))
	            populateBeanWithXML(bean, (Element) xmlValue);
	        else
	        {
	            throw new InvalidBeanException("unsupported value type: " + xmlValue.getClass().toString());
	        }
	    }
	}
	
	
	/**
	 * fill a bean class with a given JSON data
	 * 
	 * @param bean
	 *            a bean class
	 * @param jsonData
	 *            a string representation of a JSON data
	 * @throws InvalidJSONDataException
	 *             when the input json data is invalid (cannot interpret as a
	 *             JSON object)
	 * @throws InvalidBeanException
	 *             when a bean class has invalid structure
	 */
	public static void populateBean(Object bean, String jsonData) throws InvalidBeanException, InvalidJSONDataException {

		// parse the input JSON data
		JSONObject inputJson = new JSONObject(jsonData);
		try {
			populateBean(bean, inputJson);
		} catch (InstantiationException e) {
			throw new InvalidBeanException(e);
		} catch (IllegalAccessException e) {
			throw new InvalidBeanException(e);
		}
	}

	/**
	 * fill a bean class with a given JSONObject data
	 * 
	 * @param bean
	 *            a bean class
	 * @param json
	 *            a JSONOBject
	 * @throws InvalidBeanException
	 * @throws InvalidJSONDataException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings("unchecked")
	public static void populateBean(Object bean, JSONObject json) throws InvalidBeanException, InvalidJSONDataException, InstantiationException,
			IllegalAccessException {
		Class beanClass = bean.getClass();

		BeanBinderSet inputRuleSet = BeanUtil.getBeanLoadRule(beanClass);
		for (String parameterName : json.keys()) {
			BeanBinder binder = inputRuleSet.findRule(parameterName);
			if (binder != null) {
				JSONValue jsonData = json.get(parameterName);
				if (jsonData != null)
					binder.invokeJSONDataSetter(bean, jsonData);
			}
		}
	}

	/**
	 * Map<K, V> m m.json = { "-m": [[k1, v1], [k2, v2], ...] }
	 * 
	 * Map a = Map<A, Map<B, C>> a.json = "{ "-m" : [[a1, {"-m" : [[b1, c1],
	 * [b2, c2], ...]}], [a2, { "-m":[[b3, c3], ...]}], ...]}"
	 * 
	 * Collection { "-c" : [f1, f2, ..., ] }
	 * 
	 * Array [f1, f2, ... ]
	 * 
	 * When a JSONArray comes as an input, it is bound to Object[]
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws InvalidJSONDataException
	 */
	protected static void populateBean(Object bean, JSONArray jsonArray) throws InvalidBeanException, InstantiationException, IllegalAccessException,
			InvalidJSONDataException {
		Class beanClass = bean.getClass();
		if (beanClass.isArray()) {
			Object[] array = (Object[]) bean;
			Class componentType = beanClass.getComponentType();
			for (int i = 0; i < jsonArray.size(); i++) {
				array[i] = componentType.newInstance();
				populateBean(array[i], jsonArray.get(i));
			}
		} else
			throw new InvalidBeanException("to bind json array to a bean, it must be an instance array (e.g. Object[])");

	}

	protected static void populateBean(Object bean, Object jsonValue) throws InvalidBeanException, InvalidJSONDataException, InstantiationException,
			IllegalAccessException {
		if (jsonValue.getClass() == JSONObject.class) {
			populateBean(bean, (JSONObject) jsonValue);
		} else if (jsonValue.getClass() == JSONArray.class) {
			populateBean(bean, (JSONArray) jsonValue);
		} else {
			// the object is a JSONValue
			Class beanClass = bean.getClass();
			if (TypeInformation.isBasicType(beanClass)) {
				String jsonStr = jsonValue.toString();
				if (beanClass == String.class)
					bean = new String(jsonStr);
				else if (beanClass == int.class || beanClass == Integer.class)
					bean = new Integer(jsonStr);
				else if (beanClass == double.class || beanClass == Double.class)
					bean = new Double(jsonStr);
				else if (beanClass == float.class || beanClass == Float.class)
					bean = new Float(jsonStr);
				else if (beanClass == boolean.class || beanClass == Boolean.class)
					bean = new Boolean(jsonStr);
				else
					throw new InvalidBeanException();
			}
		}
	}

	public static Object createBean(Class beanType, Reader jsonReader) throws IOException, InvalidJSONDataException, InvalidBeanException
	{
		BufferedReader reader = new BufferedReader(jsonReader);
		StringWriter buffer = new StringWriter();
		String line;
		while((line = reader.readLine()) != null)
		{
			buffer.append(line);
			buffer.append(StringUtil.newline());
		}
		return createBean(beanType, buffer.toString());
	}
	
	public static Object createBean(Class beanType, String json) throws InvalidBeanException, InvalidJSONDataException {
		try {
			Object bean = createInstance(beanType);
			populateBean(bean, json);
			return bean;
		} catch (InstantiationException e) {
			throw new InvalidBeanException(e);
		} catch (IllegalAccessException e) {
			throw new InvalidBeanException(e);
		}
	}

	protected static Object createBean(Class valueType, Object jsonValue) throws InstantiationException, IllegalAccessException, InvalidBeanException,
			InvalidJSONDataException {

		if (jsonValue == null)
			return null;

		if (jsonValue.getClass() == JSONObject.class) {
			Object bean = createInstance(valueType);
			populateBean(bean, (JSONObject) jsonValue);
			return bean;
		} else if (jsonValue.getClass() == JSONArray.class) {
			Object bean = createInstance(valueType);
			populateBean(bean, (JSONArray) jsonValue);
			return bean;
		} else if (jsonValue.getClass() == JSONNull.class)
			return null;
		else if (jsonValue.getClass() == JSONInteger.class) {
			int value = ((JSONInteger) jsonValue).getIntValue();
			if (valueType == int.class || valueType == Integer.class)
				return new Integer(value);
			else if (valueType == double.class || valueType == Double.class)
				return new Double(value);
			else if (valueType == float.class || valueType == Float.class)
				return new Float(value);
			else {
				// if the valueType is unknown (e.g., Object class), return int
				// value since input JSON data has JSONInteger type
				return new Integer(value);
			}
		} else if (jsonValue.getClass() == JSONDouble.class) {
			double value = ((JSONDouble) jsonValue).getDoubleValue();
			if (valueType == int.class || valueType == Integer.class)
				return new Integer((int) value);
			else if (valueType == double.class || valueType == Double.class)
				return new Double(value);
			else if (valueType == float.class || valueType == Float.class)
				return new Float(value);
			else
				return new Double(value);
		} else if (jsonValue.getClass() == JSONBoolean.class) {
			boolean value = ((JSONBoolean) jsonValue).getValue();
			return new Boolean(value);
		} else if (jsonValue.getClass() == JSONString.class) {
		    String value = ((JSONString) jsonValue).getValue();
            if (valueType == int.class || valueType == Integer.class)
                return new Integer(value);
            else if (valueType == double.class || valueType == Double.class)
                return new Double(value);
            else if (valueType == float.class || valueType == Float.class)
                return new Float(value);
            else
                return value;
		}
		else {
			// input JSON data is a general Object
			Class beanClass = valueType;
			if (TypeInformation.isBasicType(beanClass)) {
				String jsonStr = jsonValue.toString();
				if (beanClass == String.class)
					return jsonStr;
				else if (beanClass == int.class || beanClass == Integer.class)
					return new Integer(jsonStr);
				else if (beanClass == double.class || beanClass == Double.class)
					return new Double(jsonStr);
				else if (beanClass == float.class || beanClass == Float.class)
					return new Float(jsonStr);
				else if (beanClass == boolean.class || beanClass == Boolean.class)
					return new Boolean(jsonStr);
			}

			
			return jsonValue;
		}
	}

	public static Object createInstance(Class c) throws InstantiationException, IllegalAccessException, InvalidBeanException {
		return TypeInformation.createInstance(c);
	}

	public static <E> E createXMLBean(Class<E> valueType, Reader xmlReader) throws XMLException, IOException, InvalidBeanException
	{
	    DOMBuilder builder = new DOMBuilder();
	    try
        {
            Element element = builder.parse(xmlReader);
            Object bean = createInstance(valueType);
            populateBeanWithXML(bean, element);
            return (E) bean;
        }
        catch (XmlPullParserException e)
        {
            throw new XMLException(e);
        }
        catch (InstantiationException e)
        {
            throw new InvalidBeanException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new InvalidBeanException(e);
        }
	}
	
	public static Object createXMLBean(Class valueType, String xmlData) throws InvalidBeanException, XMLException
	{
	    Object bean;
        try
        {
            bean = createInstance(valueType);
            populateBeanWithXML(bean, xmlData);
            return bean;
        }
        catch (InstantiationException e)
        {
            throw new InvalidBeanException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new InvalidBeanException(e);
        }
	}
	
    static Object createXMLBean(Class valueType, Object xmlValue) throws InvalidBeanException, InstantiationException, IllegalAccessException, InvalidXMLException
    {
        if (xmlValue == null)
            return null;

        // input xmlValue is a general Object
        if (TypeInformation.isBasicType(valueType)) 
            return populateBasicTypeWithXML(valueType, xmlValue);
        else
        {
            Object bean = createInstance(valueType);
            populateBeanWithXML(bean, xmlValue);
            return bean;
        }
    }
    
    public static Object populateBasicTypeWithXML(Class valueType, Object xmlValue)
    {
        assert(TypeInformation.isBasicType(valueType));
        
        String xmlValueStr;
        if(TypeInformation.isDOMElement(xmlValue.getClass()))
            xmlValueStr = DOMUtil.getText((Element) xmlValue);
        else
            xmlValueStr = xmlValue.toString();
        if (valueType == String.class)
            return xmlValueStr;
        else if (valueType == int.class || valueType == Integer.class)
            return new Integer(xmlValueStr);
        else if (valueType == double.class || valueType == Double.class)
            return new Double(xmlValueStr);
        else if (valueType == float.class || valueType == Float.class)
            return new Float(xmlValueStr);
        else if (valueType == boolean.class || valueType == Boolean.class)
            return new Boolean(xmlValueStr);
        return xmlValue;
    }
}

enum BeanParameterType {
	primitiveType, primitiveTypeCollection, primitiveTypeArray, nestedBean, beanArray, beanCollection, unknownTypeCollection, primitiveTypeAdder, beanAdder, primitiveTypeArrayAdder, beanArrayAdder, map
}





/**
 * TODO A hash structure may have better performance, especially for findRule()
 * method.
 * 
 * @author leo
 * 
 */
class BinderSet implements BeanBinderSet {
	Class beanClass;

	Vector<BeanBinder> _bindRule = new Vector<BeanBinder>();

	public BinderSet(Class beanClass) {
		this.beanClass = beanClass;
	}

	// @see org.utgenome.util.BeanBinderSet#addRule(org.utgenome.util.Binder)
	public void addRule(BeanBinder binder) {
		_bindRule.add(binder);
	}

	// @see org.utgenome.util.BeanBinderSet#getBindRules()
	public Vector<BeanBinder> getBindRules() {
		return _bindRule;
	}

	// @see org.utgenome.util.BeanBinderSet#findRule(java.lang.String)
	public BeanBinder findRule(String name) {
		for (BeanBinder rule : _bindRule) {
			if (rule.getParameterName().equals(name))
				return rule;
		}
		return null;
	}
}
