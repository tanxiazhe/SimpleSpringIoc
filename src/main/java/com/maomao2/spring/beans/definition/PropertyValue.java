/*
 * Copyright 2002-2016 the original author or authors.
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
 * Object to hold information and value for an individual bean property. Using an object here, rather than just storing
 * all properties in a map keyed by property name, allows for more flexibility, and the ability to handle indexed
 * properties etc in an optimized way.
 *
 * <p>
 * Note that the value doesn't need to be the final required type: A {@link BeanWrapper} implementation should handle
 * any necessary conversion, as this object doesn't know anything about the objects it will be applied to.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see PropertyValues
 * @see BeanWrapper
 * @since 13 May 2001
 */
@SuppressWarnings("serial")
public class PropertyValue {

  private final String name;

  private final Object value;

  /**
   * Create a new PropertyValue instance.
   *
   * @param name the name of the property (never {@code null})
   * @param value the value of the property (possibly before type conversion)
   */
  public PropertyValue(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Return the name of the property.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the value of the property.
   * <p>
   * Note that type conversion will <i>not</i> have occurred here. It is the responsibility of the BeanWrapper
   * implementation to perform type conversion.
   */
  public Object getValue() {
    return this.value;
  }

  /**
   * Copy constructor.
   *
   * @param original the PropertyValue to copy (never {@code null})
   */
  public PropertyValue(PropertyValue original) {
    this.name = original.getName();
    this.value = original.getValue();
  }
}
