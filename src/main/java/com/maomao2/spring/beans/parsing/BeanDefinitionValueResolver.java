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

package com.maomao2.spring.beans.parsing;

import org.apache.commons.beanutils.ConvertUtils;

import com.maomao2.spring.beans.creation.AbstractBeanFactory;
import com.maomao2.spring.beans.definition.BeanDefinition;
import com.maomao2.spring.beans.definition.RuntimeBeanReference;
import com.maomao2.spring.beans.definition.TypedStringValue;
import com.maomao2.spring.exception.BeanCreationException;
import com.maomao2.spring.exception.BeansException;

/**
 * Helper class for use in bean factory implementations,
 * resolving values contained in bean definition objects
 * into the actual values applied to the target bean instance.
 *
 * <p>
 * Operates on an {@link AbstractBeanFactory} and a plain

 * @author Juergen Hoeller
 * @since 1.2
 *
 */
public class BeanDefinitionValueResolver {

    private final AbstractBeanFactory beanFactory;

    private final String beanName;

    private final BeanDefinition beanDefinition;

    /**
     * Create a BeanDefinitionValueResolver for the given BeanFactory and BeanDefinition.
     * 
     * @param beanFactory
     *            the BeanFactory to resolve against
     * @param beanName
     *            the name of the bean that we work on
     * @param beanDefinition
     *            the BeanDefinition of the bean that we work on

     */
    public BeanDefinitionValueResolver(
            AbstractBeanFactory beanFactory, String beanName, BeanDefinition beanDefinition) {

        this.beanFactory = beanFactory;
        this.beanName = beanName;
        this.beanDefinition = beanDefinition;
    }

    /**
     * Given a PropertyValue, return a value, resolving any references to other
     * beans in the factory if necessary. The value could be:
     * <li>A BeanDefinition, which leads to the creation of a corresponding
     * new bean instance. Singleton flags and names of such "inner beans"
     * are always ignored: Inner beans are anonymous prototypes.
     * <li>A RuntimeBeanReference, which must be resolved.
     * <li>A ManagedList. This is a special collection that may contain
     * RuntimeBeanReferences or Collections that will need to be resolved.
     * <li>A ManagedSet. May also contain RuntimeBeanReferences or
     * Collections that will need to be resolved.
     * <li>A ManagedMap. In this case the value may be a RuntimeBeanReference
     * or Collection that will need to be resolved.
     * <li>An ordinary object or {@code null}, in which case it's left alone.
     * 
     * @param argName
     *            the name of the argument that the value is defined for
     * @param value
     *            the value object to resolve
     * @return the resolved object
     */
    public Object resolveValueIfNecessary(Object argName, Object value) {
        // We must check each value to see whether it requires a runtime reference
        // to another bean to be resolved.
        if (value instanceof RuntimeBeanReference) {
            RuntimeBeanReference ref = (RuntimeBeanReference) value;
            return resolveReference(argName, ref);

        } else if (value instanceof TypedStringValue) {
            // Convert value to target type here.
            TypedStringValue typedStringValue = (TypedStringValue) value;
            Object valueObject = evaluate(typedStringValue);
            try {
                Class<?> resolvedTargetType = resolveTargetType(typedStringValue);
                if (resolvedTargetType != null) {
                    return ConvertUtils.convert(valueObject, resolvedTargetType);
                } else {
                    return valueObject;
                }
            } catch (Throwable ex) {
                // Improve the message by showing the context.
                throw new BeanCreationException(
                        this.beanName,
                        "Error converting typed String value for " + argName, ex);
            }
        } else {
            return evaluate(value);
        }
    }

    private Object evaluate(Object value) {
        return value;
    }

    private Object evaluate(TypedStringValue typedStringValue) {
        return typedStringValue.getValue();
    }

    /**
     * Resolve the target type in the given TypedStringValue.
     * 
     * @param value
     *            the TypedStringValue to resolve
     * @return the resolved target type (or {@code null} if none specified)
     * @throws ClassNotFoundException
     *             if the specified type cannot be resolved
     * @see TypedStringValue#resolveTargetType
     */
    protected Class<?> resolveTargetType(TypedStringValue value) throws ClassNotFoundException {
        if (value.hasTargetType()) {
            return value.getTargetType();
        }
        return value.resolveTargetType(this.beanFactory.getBeanClassLoader());
    }

    /**
     * Resolve a reference to another bean in the factory.
     */
    private Object resolveReference(Object argName, RuntimeBeanReference ref) {
        try {
            String refName = ref.getBeanName();

            Object bean = this.beanFactory.getBean(refName);
            this.beanFactory.registerDependentBean(refName, this.beanName);
            return bean;

        } catch (BeansException ex) {
            throw new BeanCreationException(
                    this.beanName,
                    "Cannot resolve reference to bean '" + ref.getBeanName() + "' while setting " + argName, ex);
        }
    }

}
