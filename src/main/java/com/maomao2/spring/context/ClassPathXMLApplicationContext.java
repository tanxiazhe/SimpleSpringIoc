package com.maomao2.spring.context;

import com.maomao2.spring.beans.creation.DefaultBeanFactory;
import com.maomao2.spring.beans.parsing.XmlBeanDefinitionReader;
import com.maomao2.spring.exception.BeansException;

public class ClassPathXMLApplicationContext extends AbstractApplicationContext {

  public ClassPathXMLApplicationContext(String configLocation) throws BeansException {
    this(new String[]{configLocation});
  }

  public ClassPathXMLApplicationContext(String[] configLocations) {
    setConfigLocations(configLocations);
    refresh();
  }

  /**
   * Loads the bean definitions via an XmlBeanDefinitionReader.
   */

  protected void loadBeanDefinitions(DefaultBeanFactory beanFactory) {
    String[] configLocations = getConfigLocations();

    if (configLocations != null) {
      // Create a new XmlBeanDefinitionReader for the given BeanFactory.
      XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
      beanDefinitionReader.loadBeanDefinitions(configLocations);
    }

  }

}
