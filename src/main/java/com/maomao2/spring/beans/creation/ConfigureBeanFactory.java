package com.maomao2.spring.beans.creation;

public interface ConfigureBeanFactory extends BeanFactory {

  String SCOPE_SINGLETON = "singleton";
  String SCOPE_PROTOTYPE = "prototype";

  /**
   * Constant that indicates no externally defined autowiring. Note that BeanFactoryAware etc and annotation-driven
   * injection will still be applied.
   */
  int AUTOWIRE_NO = 0;

  /**
   * Constant that indicates autowiring bean properties by name (applying to all bean property setters).
   *
   * @see #createBean
   * @see #autowire
   * @see #autowireBeanProperties
   */
  int AUTOWIRE_BY_NAME = 1;

  /**
   * Constant that indicates autowiring bean properties by type (applying to all bean property setters).
   *
   * @see #createBean
   * @see #autowire
   * @see #autowireBeanProperties
   */
  int AUTOWIRE_BY_TYPE = 2;

  /**
   * Constant that indicates autowiring the greediest constructor that can be satisfied (involves resolving the
   * appropriate constructor).
   *
   * @see #createBean
   * @see #autowire
   */
  int AUTOWIRE_CONSTRUCTOR = 3;

}
