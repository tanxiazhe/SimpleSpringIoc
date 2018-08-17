/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maomao2.spring.beans.definition;

/**
 * Immutable placeholder class used for a property value object when it's a reference to another bean in the factory, to
 * be resolved at runtime.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.BeanFactory#getBean
 */
public class RuntimeBeanReference {

  private final String beanName;

  /**
   * Create a new RuntimeBeanReference to the given bean name, without explicitly marking it as reference to a bean in
   * the parent factory.
   *
   * @param beanName name of the target bean
   */
  public RuntimeBeanReference(String beanName) {
    this.beanName = beanName;
  }

  public String getBeanName() {
    return this.beanName;
  }

}
