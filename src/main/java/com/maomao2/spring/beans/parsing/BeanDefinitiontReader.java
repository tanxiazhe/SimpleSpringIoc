
package com.maomao2.spring.beans.parsing;

import com.maomao2.spring.beans.creation.BeanDefinitionRegistry;
import org.w3c.dom.Document;


public interface BeanDefinitiontReader {


  /**
   * Return the bean factory to register the bean definitions with.
   * <p>The factory is exposed through the BeanDefinitionRegistry interface,
   * encapsulating the methods that are relevant for bean definition handling.
   */
  BeanDefinitionRegistry getRegistry();

  /**
   * Load bean definitions from the specified configLocations.
   *
   * @param configLocations the resource descriptor
   */
  void loadBeanDefinitions(String[] configLocations);

}
