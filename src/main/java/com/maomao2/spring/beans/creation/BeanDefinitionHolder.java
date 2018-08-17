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

package com.maomao2.spring.beans.creation;

import com.maomao2.spring.beans.definition.BeanDefinition;

/**
 * Holder for a BeanDefinition with name and aliases. Can be registered as a placeholder for an inner bean.
 *
 * <p>
 * Can also be used for programmatic registration of inner bean definitions. If you don't care about BeanNameAware and
 * the like, registering RootBeanDefinition or ChildBeanDefinition is good enough.
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 */
public class BeanDefinitionHolder {

  private final BeanDefinition beanDefinition;

  private final String beanName;

  /**
   * Create a new BeanDefinitionHolder.
   *
   * @param beanDefinition the BeanDefinition to wrap
   * @param beanName the name of the bean, as specified for the bean definition
   */
  public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName) {
    this.beanDefinition = beanDefinition;
    this.beanName = beanName;
  }

  /**
   * Copy constructor: Create a new BeanDefinitionHolder with the same contents as the given BeanDefinitionHolder
   * instance.
   * <p>
   * Note: The wrapped BeanDefinition reference is taken as-is; it is {@code not} deeply copied.
   *
   * @param beanDefinitionHolder the BeanDefinitionHolder to copy
   */
  public BeanDefinitionHolder(BeanDefinitionHolder beanDefinitionHolder) {
    this.beanDefinition = beanDefinitionHolder.getBeanDefinition();
    this.beanName = beanDefinitionHolder.getBeanName();
  }

  /**
   * Return the wrapped BeanDefinition.
   */
  public BeanDefinition getBeanDefinition() {
    return this.beanDefinition;
  }

  /**
   * Return the primary name of the bean, as specified for the bean definition.
   */
  public String getBeanName() {
    return this.beanName;
  }

}
