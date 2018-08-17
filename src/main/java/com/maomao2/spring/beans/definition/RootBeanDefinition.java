package com.maomao2.spring.beans.definition;

public class RootBeanDefinition extends AbstractBeanDefinition {

  public RootBeanDefinition(String beanClassName) {
    setBeanClassName(beanClassName);
  }

  /**
   * Create a new RootBeanDefinition as deep copy of the given bean definition.
   *
   * @param original the original bean definition to copy from
   */
  public RootBeanDefinition(RootBeanDefinition original) {
    super(original);
  }
}
