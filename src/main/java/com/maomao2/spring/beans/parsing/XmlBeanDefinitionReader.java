package com.maomao2.spring.beans.parsing;

import com.maomao2.spring.util.ClassUtils;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.maomao2.spring.beans.creation.BeanDefinitionHolder;
import com.maomao2.spring.beans.creation.BeanDefinitionRegistry;
import com.maomao2.spring.beans.definition.AbstractBeanDefinition;
import com.maomao2.spring.beans.definition.BeanDefinition;
import com.maomao2.spring.beans.definition.BeanDefinitionConstrants;
import com.maomao2.spring.beans.definition.ConstructorArgumentValues;
import com.maomao2.spring.beans.definition.PropertyValue;
import com.maomao2.spring.beans.definition.RootBeanDefinition;
import com.maomao2.spring.beans.definition.RuntimeBeanReference;
import com.maomao2.spring.beans.definition.TypedStringValue;
import com.maomao2.spring.util.StringUtils;

public class XmlBeanDefinitionReader implements BeanDefinitiontReader {

  Logger logger = Logger.getLogger(getClass());

  private final BeanDefinitionRegistry registry;
  private ClassLoader beanClassLoader;
  protected DocumentHolder documentHolder = new XMLDocumentHolder();

  /**
   * Stores all used bean names so we can enforce uniqueness on a per beans-element basis. Duplicate bean ids/names may
   * not exist within the same level of beans element nesting, but may be duplicated across levels.
   */
  private final Set<String> usedNames = new HashSet<String>();

  public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
    this.registry = registry;
  }

  /**
   * Set the ClassLoader to use for bean classes.
   * <p>Default is {@code null}, which suggests to not load bean classes
   * eagerly but rather to just register bean definitions with class names, with the corresponding Classes to be
   * resolved later (or never).
   *
   * @see Thread#getContextClassLoader()
   */
  public void setBeanClassLoader(ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  @Override
  public ClassLoader getBeanClassLoader() {
    return this.beanClassLoader;
  }

  @Override
  public BeanDefinitionRegistry getRegistry() {
    return this.registry;
  }

  @Override
  public void loadBeanDefinitions(String[] configLocations) {

    for (String location : configLocations) {
      doLoadBeanDefinitions(location);
    }
  }


  protected void doLoadBeanDefinitions(String location) {
    Document doc = documentHolder.loadDocument(location);
    registerBeanDefinitions(doc);
  }

  private void registerBeanDefinitions(Document doc) {
    Element root = doc.getDocumentElement();
    doRegisterBeanDefinitions(root);
  }

  private void doRegisterBeanDefinitions(Element root) {
    logger.info("Loading bean definitions");
    NodeList nl = root.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element) {
        Element ele = (Element) node;
        parseDefaultElement(ele);
      }
    }
  }

  private void parseDefaultElement(Element ele) {
    //解析xml文件的四种基本标签中的import、alias和beans标签的，bean标签。
    if (nodeNameEquals(ele, BeanDefinitionConstrants.BEAN_ELEMENT)) {
      processBeanDefinition(ele);
    } else if (nodeNameEquals(ele, BeanDefinitionConstrants.NESTED_BEANS_ELEMENT)) {
      // recurse
      doRegisterBeanDefinitions(ele);
    }
  }

  private void processBeanDefinition(Element ele) {
    //only deal with bean element
    BeanDefinitionHolder definitionHolder = getBeanDefinitionHolder(ele);

    this.registry.registerBeanDefinition(definitionHolder.getBeanName(), definitionHolder.getBeanDefinition());

  }

  /**
   * Parse the bean definition itself, without regard to name or aliases. May return {@code null} if problems occurred
   * during the parsing of the bean definition.
   */
  public BeanDefinitionHolder getBeanDefinitionHolder(Element ele) {
    String id = ele.getAttribute(BeanDefinitionConstrants.ID_ATTRIBUTE);

    String nameAttr = ele.getAttribute(BeanDefinitionConstrants.NAME_ATTRIBUTE);

    String beanName = id;

    checkNameUniqueness(beanName, ele);

    AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName);

    return new BeanDefinitionHolder(beanDefinition, beanName);

  }

  /**
   * Validate that the specified bean name and aliases have not been used already within the current level of beans
   * element nesting.
   */
  protected void checkNameUniqueness(String beanName, Element beanElement) {
    String foundName = null;

    if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
      foundName = beanName;
    }

    if (foundName != null) {
      logger.error("Bean name '" + foundName + "' is already used in this <beans> element");
    }

    this.usedNames.add(beanName);

  }

  public AbstractBeanDefinition parseBeanDefinitionElement(Element ele, String beanName) {

    String className = null;
    if (ele.hasAttribute(BeanDefinitionConstrants.CLASS_ATTRIBUTE)) {
      className = ele.getAttribute(BeanDefinitionConstrants.CLASS_ATTRIBUTE).trim();
    }

    try {
      AbstractBeanDefinition bd = createBeanDefinition(className);

      parseBeanDefinitionAttributes(ele, beanName, bd);
      parseConstructorArgElements(ele, bd);
      parsePropertyElements(ele, bd);
      return bd;
    } catch (ClassNotFoundException ex) {
      logger.error("Bean class [" + className + "] not found");
    } catch (NoClassDefFoundError err) {
      logger.error("Class that bean class [" + className + "] depends on not found");
    } catch (Throwable ex) {
      logger.error("Unexpected failure during bean definition parsing");
    }
    return null;
  }

  /**
   * Apply the attributes of the given bean element to the given bean * definition.
   *
   * @param ele bean declaration element
   * @param beanName bean name
   * @return a bean definition initialized according to the bean element attributes
   */
  public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName,
      AbstractBeanDefinition bd) {

    if (ele.hasAttribute(BeanDefinitionConstrants.SCOPE_ATTRIBUTE)) {
      bd.setScope(ele.getAttribute(BeanDefinitionConstrants.SCOPE_ATTRIBUTE));
    }

    String lazyInit = ele.getAttribute(BeanDefinitionConstrants.LAZY_INIT_ATTRIBUTE);
    bd.setLazyInit(BeanDefinitionConstrants.TRUE_VALUE.equals(lazyInit));

    String autowire = ele.getAttribute(BeanDefinitionConstrants.AUTOWIRE_ATTRIBUTE);
    bd.setAutowireMode(getAutowireMode(autowire));

    if (ele.hasAttribute(BeanDefinitionConstrants.INIT_METHOD_ATTRIBUTE)) {
      String initMethodName = ele.getAttribute(BeanDefinitionConstrants.INIT_METHOD_ATTRIBUTE);
      if (!"".equals(initMethodName)) {
        bd.setInitMethodName(initMethodName);
      }
    }

    if (ele.hasAttribute(BeanDefinitionConstrants.DESTROY_METHOD_ATTRIBUTE)) {
      String destroyMethodName = ele.getAttribute(BeanDefinitionConstrants.DESTROY_METHOD_ATTRIBUTE);
      bd.setDestroyMethodName(destroyMethodName);
    }

    if (ele.hasAttribute(BeanDefinitionConstrants.FACTORY_METHOD_ATTRIBUTE)) {
      bd.setFactoryMethodName(ele.getAttribute(BeanDefinitionConstrants.FACTORY_METHOD_ATTRIBUTE));
    }
    if (ele.hasAttribute(BeanDefinitionConstrants.FACTORY_BEAN_ATTRIBUTE)) {
      bd.setFactoryBeanName(ele.getAttribute(BeanDefinitionConstrants.FACTORY_BEAN_ATTRIBUTE));
    }

    return bd;
  }

  public int getAutowireMode(String attValue) {
    String att = attValue;

    int autowire = AbstractBeanDefinition.AUTOWIRE_NO;
    if (BeanDefinitionConstrants.AUTOWIRE_BY_NAME_VALUE.equals(att)) {
      autowire = AbstractBeanDefinition.AUTOWIRE_BY_NAME;
    } else if (BeanDefinitionConstrants.AUTOWIRE_BY_TYPE_VALUE.equals(att)) {
      autowire = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
    } else if (BeanDefinitionConstrants.AUTOWIRE_CONSTRUCTOR_VALUE.equals(att)) {
      autowire = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;
    }

    // Else leave default value.
    return autowire;
  }

  /**
   * Parse constructor-arg sub-elements of the given bean element.
   */
  public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
    NodeList nl = beanEle.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameEquals(node, BeanDefinitionConstrants.CONSTRUCTOR_ARG_ELEMENT)) {
        parseConstructorArgElement((Element) node, bd);
      }
    }
  }

  public boolean nodeNameEquals(Node node, String desiredName) {
    return desiredName.equals(node.getNodeName()) || desiredName.equals(node.getLocalName());
  }

  /**
   * Parse a constructor-arg element.
   */
  public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
    String indexAttr = ele.getAttribute(BeanDefinitionConstrants.INDEX_ATTRIBUTE);
    String typeAttr = ele.getAttribute(BeanDefinitionConstrants.TYPE_ATTRIBUTE);
    String nameAttr = ele.getAttribute(BeanDefinitionConstrants.NAME_ATTRIBUTE);

    Object value = parsePropertyValue(ele, bd, null);
    ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(
        value);
    if (StringUtils.hasLength(typeAttr)) {
      valueHolder.setType(typeAttr);
    }
    if (StringUtils.hasLength(nameAttr)) {
      valueHolder.setName(nameAttr);
    }

    if (StringUtils.hasLength(indexAttr)) {
      try {
        int index = Integer.parseInt(indexAttr);
        if (index < 0) {
          logger.error("'index' cannot be lower than 0");
        } else {
          if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
            logger.error("Ambiguous constructor-arg entries for index " + index);
          } else {
            bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
          }
        }
      } catch (NumberFormatException ex) {
        logger.error("Attribute 'index' of tag 'constructor-arg' must be an integer");
      }
    } else {
      bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
    }
  }

  /**
   * Parse a property element.
   */
  public void parsePropertyElement(Element ele, BeanDefinition bd) {
    String propertyName = ele.getAttribute(BeanDefinitionConstrants.NAME_ATTRIBUTE);
    if (!StringUtils.hasLength(propertyName)) {
      logger.error("Tag 'property' must have a 'name' attribute");
      return;
    }

    if (bd.getPropertyValues().contains(propertyName)) {
      logger.error("Multiple 'property' definitions for property '" + propertyName + "'");
      return;
    }

    Object val = parsePropertyValue(ele, bd, propertyName);

    PropertyValue pv = new PropertyValue(propertyName, val);

    bd.getPropertyValues().addPropertyValue(pv);

  }

  /**
   * Get the value of a property element. May be a list etc. Also used for constructor arguments, "propertyName" being
   * null in this case.
   */
  public Object parsePropertyValue(Element ele, BeanDefinition bd, String propertyName) {
    String elementName = (propertyName != null) ? "<property> element for property '" + propertyName + "'"
        : "<constructor-arg> element";

    // Should only have one child element: ref, value, list, etc.
    NodeList nl = ele.getChildNodes();
    Element subElement = null;
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && !nodeNameEquals(node, BeanDefinitionConstrants.DESCRIPTION_ELEMENT) &&
          !nodeNameEquals(node, BeanDefinitionConstrants.META_ELEMENT)) {
        // Child element is what we're looking for.
        if (subElement != null) {
          logger.error(elementName + " must not contain more than one sub-element");
        } else {
          subElement = (Element) node;
        }
      }
    }

    boolean hasRefAttribute = ele.hasAttribute(BeanDefinitionConstrants.REF_ATTRIBUTE);
    boolean hasValueAttribute = ele.hasAttribute(BeanDefinitionConstrants.VALUE_ATTRIBUTE);
    if ((hasRefAttribute && hasValueAttribute) ||
        ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
      logger.error(elementName +
          " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element");
    }

    if (hasRefAttribute) {
      String refName = ele.getAttribute(BeanDefinitionConstrants.REF_ATTRIBUTE);
      if (!StringUtils.hasText(refName)) {
        logger.error(elementName + " contains empty 'ref' attribute");
      }
      RuntimeBeanReference ref = new RuntimeBeanReference(refName);
      return ref;
    } else if (hasValueAttribute) {
      TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(BeanDefinitionConstrants.VALUE_ATTRIBUTE));
      return valueHolder;
    } else if (subElement != null) {
      return parsePropertySubElement(subElement, bd);
    } else {
      // Neither child element nor "ref" or "value" attribute found.
      logger.error(elementName + " must specify a ref or value");
      return null;
    }
  }

  /**
   * Parse property sub-elements of the given bean element.
   */
  public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
    NodeList nl = beanEle.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameEquals(node, BeanDefinitionConstrants.PROPERTY_ELEMENT)) {
        parsePropertyElement((Element) node, bd);
      }
    }
  }

  public Object parsePropertySubElement(Element ele, BeanDefinition bd) {
    return parsePropertySubElement(ele, bd, null);
  }

  /**
   * Parse a value, ref or collection sub-element of a property or constructor-arg element.
   *
   * @param ele subelement of property element; we don't know which yet
   * @param defaultValueType the default type (class name) for any {@code <value>} tag that might be created
   */
  public Object parsePropertySubElement(Element ele, BeanDefinition bd, String defaultValueType) {
//     if (nodeNameEquals(ele, BeanDefinitionConstrants.BEAN_ELEMENT)) {
//      BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
//      if (nestedBd != null) {
//        nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
//      }
//      return nestedBd;
//    } else if (nodeNameEquals(ele, BeanDefinitionConstrants.REF_ELEMENT)) {
//      // A generic reference to any name of any bean.
//      String refName = ele.getAttribute(BeanDefinitionConstrants.BEAN_REF_ATTRIBUTE);
//      boolean toParent = false;
//      if (!StringUtils.hasLength(refName)) {
//        // A reference to the id of another bean in a parent context.
//        refName = ele.getAttribute(BeanDefinitionConstrants.PARENT_REF_ATTRIBUTE);
//        toParent = true;
//        if (!StringUtils.hasLength(refName)) {
//          error("'bean' or 'parent' is required for <ref> element", ele);
//          return null;
//        }
//      }
//      if (!StringUtils.hasText(refName)) {
//        error("<ref> element contains empty target attribute", ele);
//        return null;
//      }
//      RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
//      ref.setSource(extractSource(ele));
//      return ref;
//    } else if (nodeNameEquals(ele, BeanDefinitionConstrants.IDREF_ELEMENT)) {
//      return parseIdRefElement(ele);
//    } else
    if (nodeNameEquals(ele, BeanDefinitionConstrants.VALUE_ELEMENT)) {
      return parseValueElement(ele, defaultValueType);
//    } else if (nodeNameEquals(ele, NULL_ELEMENT)) {
//      // It's a distinguished null value. Let's wrap it in a TypedStringValue
//      // object in order to preserve the source location.
//      TypedStringValue nullHolder = new TypedStringValue(null);
//      nullHolder.setSource(extractSource(ele));
//      return nullHolder;
//    } else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
//      return parseArrayElement(ele, bd);
//    } else if (nodeNameEquals(ele, LIST_ELEMENT)) {
//      return parseListElement(ele, bd);
//    } else if (nodeNameEquals(ele, SET_ELEMENT)) {
//      return parseSetElement(ele, bd);
//    } else if (nodeNameEquals(ele, MAP_ELEMENT)) {
//      return parseMapElement(ele, bd);
//    } else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
//      return parsePropsElement(ele);
    } else {
      logger.error("Unknown property sub-element: [" + ele.getNodeName() + "]");
      return null;
    }
  }

//  /**
//   * Return a typed String value Object for the given 'idref' element.
//   */
//  public Object parseIdRefElement(Element ele) {
//    // A generic reference to any name of any bean.
//    String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
//    if (!StringUtils.hasLength(refName)) {
//      error("'bean' is required for <idref> element", ele);
//      return null;
//    }
//    if (!StringUtils.hasText(refName)) {
//      error("<idref> element contains empty target attribute", ele);
//      return null;
//    }
//    RuntimeBeanNameReference ref = new RuntimeBeanNameReference(refName);
//    ref.setSource(extractSource(ele));
//    return ref;
//  }

  /**
   * Return a typed String value Object for the given value element.
   */
  public Object parseValueElement(Element ele, String defaultTypeName) {
    // It's a literal value.
    String value = getTextValue(ele);
    String specifiedTypeName = ele.getAttribute(BeanDefinitionConstrants.TYPE_ATTRIBUTE);
    String typeName = specifiedTypeName;
    if (!StringUtils.hasText(typeName)) {
      typeName = defaultTypeName;
    }
    try {
      TypedStringValue typedValue = buildTypedStringValue(value, typeName);

      typedValue.setSpecifiedTypeName(specifiedTypeName);
      return typedValue;
    } catch (ClassNotFoundException ex) {
      logger.error("Type class [" + typeName + "] not found for <value> element");
      return value;
    }
  }

  /**
   * Extracts the text value from the given DOM element, ignoring XML comments.
   * <p>Appends all CharacterData nodes and EntityReference nodes into a single
   * String value, excluding Comment nodes. Only exposes actual user-specified text, no default values of any kind.
   *
   * @see CharacterData
   * @see EntityReference
   * @see Comment
   */
  public static String getTextValue(Element valueEle) {

    StringBuilder sb = new StringBuilder();
    NodeList nl = valueEle.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node item = nl.item(i);
      if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
        sb.append(item.getNodeValue());
      }
    }
    return sb.toString();
  }

  /**
   * Build a typed String value Object for the given raw value.
   */
  protected TypedStringValue buildTypedStringValue(String value, String targetTypeName)
      throws ClassNotFoundException {

    ClassLoader classLoader = this.getBeanClassLoader();
    TypedStringValue typedValue;
    if (!StringUtils.hasText(targetTypeName)) {
      typedValue = new TypedStringValue(value);
    } else if (classLoader != null) {
      Class<?> targetType = ClassUtils.forName(targetTypeName, classLoader);
      typedValue = new TypedStringValue(value, targetType);
    } else {
      typedValue = new TypedStringValue(value, targetTypeName);
    }
    return typedValue;
  }

//  /**
//   * Parse an array element.
//   */
//  public Object parseArrayElement(Element arrayEle, BeanDefinition bd) {
//    String elementType = arrayEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
//    NodeList nl = arrayEle.getChildNodes();
//    ManagedArray target = new ManagedArray(elementType, nl.getLength());
//    target.setSource(extractSource(arrayEle));
//    target.setElementTypeName(elementType);
//    target.setMergeEnabled(parseMergeAttribute(arrayEle));
//    parseCollectionElements(nl, target, bd, elementType);
//    return target;
//  }
//
//  /**
//   * Parse a list element.
//   */
//  public List<Object> parseListElement(Element collectionEle, BeanDefinition bd) {
//    String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
//    NodeList nl = collectionEle.getChildNodes();
//    ManagedList<Object> target = new ManagedList<>(nl.getLength());
//    target.setSource(extractSource(collectionEle));
//    target.setElementTypeName(defaultElementType);
//    target.setMergeEnabled(parseMergeAttribute(collectionEle));
//    parseCollectionElements(nl, target, bd, defaultElementType);
//    return target;
//  }
//
//  /**
//   * Parse a set element.
//   */
//  public Set<Object> parseSetElement(Element collectionEle, BeanDefinition bd) {
//    String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
//    NodeList nl = collectionEle.getChildNodes();
//    ManagedSet<Object> target = new ManagedSet<>(nl.getLength());
//    target.setSource(extractSource(collectionEle));
//    target.setElementTypeName(defaultElementType);
//    target.setMergeEnabled(parseMergeAttribute(collectionEle));
//    parseCollectionElements(nl, target, bd, defaultElementType);
//    return target;
//  }
//
//  protected void parseCollectionElements(
//      NodeList elementNodes, Collection<Object> target, BeanDefinition bd, String defaultElementType) {
//
//    for (int i = 0; i < elementNodes.getLength(); i++) {
//      Node node = elementNodes.item(i);
//      if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)) {
//        target.add(parsePropertySubElement((Element) node, bd, defaultElementType));
//      }
//    }
//  }
//
//  /**
//   * Parse a map element.
//   */
//  public Map<Object, Object> parseMapElement(Element mapEle, BeanDefinition bd) {
//    String defaultKeyType = mapEle.getAttribute(KEY_TYPE_ATTRIBUTE);
//    String defaultValueType = mapEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
//
//    List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
//    ManagedMap<Object, Object> map = new ManagedMap<>(entryEles.size());
//    map.setSource(extractSource(mapEle));
//    map.setKeyTypeName(defaultKeyType);
//    map.setValueTypeName(defaultValueType);
//    map.setMergeEnabled(parseMergeAttribute(mapEle));
//
//    for (Element entryEle : entryEles) {
//      // Should only have one value child element: ref, value, list, etc.
//      // Optionally, there might be a key child element.
//      NodeList entrySubNodes = entryEle.getChildNodes();
//      Element keyEle = null;
//      Element valueEle = null;
//      for (int j = 0; j < entrySubNodes.getLength(); j++) {
//        Node node = entrySubNodes.item(j);
//        if (node instanceof Element) {
//          Element candidateEle = (Element) node;
//          if (nodeNameEquals(candidateEle, KEY_ELEMENT)) {
//            if (keyEle != null) {
//              error("<entry> element is only allowed to contain one <key> sub-element", entryEle);
//            } else {
//              keyEle = candidateEle;
//            }
//          } else {
//            // Child element is what we're looking for.
//            if (nodeNameEquals(candidateEle, DESCRIPTION_ELEMENT)) {
//              // the element is a <description> -> ignore it
//            } else if (valueEle != null) {
//              error("<entry> element must not contain more than one value sub-element", entryEle);
//            } else {
//              valueEle = candidateEle;
//            }
//          }
//        }
//      }
//
//      // Extract key from attribute or sub-element.
//      Object key = null;
//      boolean hasKeyAttribute = entryEle.hasAttribute(KEY_ATTRIBUTE);
//      boolean hasKeyRefAttribute = entryEle.hasAttribute(KEY_REF_ATTRIBUTE);
//      if ((hasKeyAttribute && hasKeyRefAttribute) ||
//          ((hasKeyAttribute || hasKeyRefAttribute)) && keyEle != null) {
//        error("<entry> element is only allowed to contain either " +
//            "a 'key' attribute OR a 'key-ref' attribute OR a <key> sub-element", entryEle);
//      }
//      if (hasKeyAttribute) {
//        key = buildTypedStringValueForMap(entryEle.getAttribute(KEY_ATTRIBUTE), defaultKeyType, entryEle);
//      } else if (hasKeyRefAttribute) {
//        String refName = entryEle.getAttribute(KEY_REF_ATTRIBUTE);
//        if (!StringUtils.hasText(refName)) {
//          error("<entry> element contains empty 'key-ref' attribute", entryEle);
//        }
//        RuntimeBeanReference ref = new RuntimeBeanReference(refName);
//        ref.setSource(extractSource(entryEle));
//        key = ref;
//      } else if (keyEle != null) {
//        key = parseKeyElement(keyEle, bd, defaultKeyType);
//      } else {
//        error("<entry> element must specify a key", entryEle);
//      }
//
//      // Extract value from attribute or sub-element.
//      Object value = null;
//      boolean hasValueAttribute = entryEle.hasAttribute(VALUE_ATTRIBUTE);
//      boolean hasValueRefAttribute = entryEle.hasAttribute(VALUE_REF_ATTRIBUTE);
//      boolean hasValueTypeAttribute = entryEle.hasAttribute(VALUE_TYPE_ATTRIBUTE);
//      if ((hasValueAttribute && hasValueRefAttribute) ||
//          ((hasValueAttribute || hasValueRefAttribute)) && valueEle != null) {
//        error("<entry> element is only allowed to contain either " +
//            "'value' attribute OR 'value-ref' attribute OR <value> sub-element", entryEle);
//      }
//      if ((hasValueTypeAttribute && hasValueRefAttribute) ||
//          (hasValueTypeAttribute && !hasValueAttribute) ||
//          (hasValueTypeAttribute && valueEle != null)) {
//        error("<entry> element is only allowed to contain a 'value-type' " +
//            "attribute when it has a 'value' attribute", entryEle);
//      }
//      if (hasValueAttribute) {
//        String valueType = entryEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
//        if (!StringUtils.hasText(valueType)) {
//          valueType = defaultValueType;
//        }
//        value = buildTypedStringValueForMap(entryEle.getAttribute(VALUE_ATTRIBUTE), valueType, entryEle);
//      } else if (hasValueRefAttribute) {
//        String refName = entryEle.getAttribute(VALUE_REF_ATTRIBUTE);
//        if (!StringUtils.hasText(refName)) {
//          error("<entry> element contains empty 'value-ref' attribute", entryEle);
//        }
//        RuntimeBeanReference ref = new RuntimeBeanReference(refName);
//        ref.setSource(extractSource(entryEle));
//        value = ref;
//      } else if (valueEle != null) {
//        value = parsePropertySubElement(valueEle, bd, defaultValueType);
//      } else {
//        error("<entry> element must specify a value", entryEle);
//      }
//
//      // Add final key and value to the Map.
//      map.put(key, value);
//    }
//
//    return map;
//  }
//
//  /**
//   * Build a typed String value Object for the given raw value.
//   *
//   * @see org.springframework.beans.factory.config.TypedStringValue
//   */
//  protected final Object buildTypedStringValueForMap(String value, String defaultTypeName, Element entryEle) {
//    try {
//      TypedStringValue typedValue = buildTypedStringValue(value, defaultTypeName);
//      typedValue.setSource(extractSource(entryEle));
//      return typedValue;
//    } catch (ClassNotFoundException ex) {
//      error("Type class [" + defaultTypeName + "] not found for Map key/value type", entryEle, ex);
//      return value;
//    }
//  }
//
//  /**
//   * Parse a key sub-element of a map element.
//   */
//  protected Object parseKeyElement(Element keyEle, BeanDefinition bd, String defaultKeyTypeName) {
//    NodeList nl = keyEle.getChildNodes();
//    Element subElement = null;
//    for (int i = 0; i < nl.getLength(); i++) {
//      Node node = nl.item(i);
//      if (node instanceof Element) {
//        // Child element is what we're looking for.
//        if (subElement != null) {
//          error("<key> element must not contain more than one value sub-element", keyEle);
//        } else {
//          subElement = (Element) node;
//        }
//      }
//    }
//    return parsePropertySubElement(subElement, bd, defaultKeyTypeName);
//  }
//
//  /**
//   * Parse a props element.
//   */
//  public Properties parsePropsElement(Element propsEle) {
//    ManagedProperties props = new ManagedProperties();
//    props.setSource(extractSource(propsEle));
//    props.setMergeEnabled(parseMergeAttribute(propsEle));
//
//    List<Element> propEles = DomUtils.getChildElementsByTagName(propsEle, PROP_ELEMENT);
//    for (Element propEle : propEles) {
//      String key = propEle.getAttribute(KEY_ATTRIBUTE);
//      // Trim the text value to avoid unwanted whitespace
//      // caused by typical XML formatting.
//      String value = DomUtils.getTextValue(propEle).trim();
//      TypedStringValue keyHolder = new TypedStringValue(key);
//      keyHolder.setSource(extractSource(propEle));
//      TypedStringValue valueHolder = new TypedStringValue(value);
//      valueHolder.setSource(extractSource(propEle));
//      props.put(keyHolder, valueHolder);
//    }
//
//    return props;
//  }
//
//  /**
//   * Parse the merge attribute of a collection element, if any.
//   */
//  public boolean parseMergeAttribute(Element collectionElement) {
//    String value = collectionElement.getAttribute(MERGE_ATTRIBUTE);
//    if (DEFAULT_VALUE.equals(value)) {
//      value = this.defaults.getMerge();
//    }
//    return TRUE_VALUE.equals(value);
//  }
//

  /**
   * Create a new GenericBeanDefinition for the given class name *
   *
   * @param className the name of the bean class
   * @return the bean definition
   * @throws ClassNotFoundException if the bean class could not be loaded
   */
  public static AbstractBeanDefinition createBeanDefinition(
      String className) throws ClassNotFoundException {

    RootBeanDefinition bd = new RootBeanDefinition(className);

    return bd;
  }

}
