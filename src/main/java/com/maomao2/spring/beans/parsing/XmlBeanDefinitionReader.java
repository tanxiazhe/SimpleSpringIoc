package com.maomao2.spring.beans.parsing;

import static com.maomao2.spring.beans.definition.BeanDefinitionConstrants.VALUE_ATTRIBUTE;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

  protected DocumentHolder documentHolder = new XMLDocumentHolder();

  /**
   * Stores all used bean names so we can enforce uniqueness on a per beans-element basis. Duplicate bean ids/names may
   * not exist within the same level of beans element nesting, but may be duplicated across levels.
   */
  private final Set<String> usedNames = new HashSet<String>();

  public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
    this.registry = registry;
  }

  public BeanDefinitionRegistry getRegistry() {
    return this.registry;
  }

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
    boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
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
      TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
      return valueHolder;
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
