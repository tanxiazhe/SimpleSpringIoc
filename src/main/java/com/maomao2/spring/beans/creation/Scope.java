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

package com.maomao2.spring.beans.creation;

/**
 * Strategy interface used by a {@link ConfigurableBeanFactory},
 * representing a target scope to hold bean instances in.
 * This allows for extending the BeanFactory's standard scopes
 * {@link ConfigurableBeanFactory#SCOPE_SINGLETON "singleton"} and
 * {@link ConfigurableBeanFactory#SCOPE_PROTOTYPE "prototype"}
 * with custom further scopes, registered for a
 * {@link ConfigurableBeanFactory#registerScope(String, Scope) specific key}.
 *
 * <p>
 * {@link org.springframework.context.ApplicationContext} implementations
 * such as a {@link org.springframework.web.context.WebApplicationContext}
 * may register additional standard scopes specific to their environment,
 * e.g. {@link org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST "request"}
 * and {@link org.springframework.web.context.WebApplicationContext#SCOPE_SESSION "session"},
 * based on this Scope SPI.
 *
 * <p>
 * Even if its primary use is for extended scopes in a web environment,
 * this SPI is completely generic: It provides the ability to get and put
 * objects from any underlying storage mechanism, such as an HTTP session
 * or a custom conversation mechanism. The name passed into this class's
 * {@code get} and {@code remove} methods will identify the
 * target object in the current scope.
 *
 * <p>
 * {@code Scope} implementations are expected to be thread-safe.
 * One {@code Scope} instance can be used with multiple bean factories
 * at the same time, if desired (unless it explicitly wants to be aware of
 * the containing BeanFactory), with any number of threads accessing
 * the {@code Scope} concurrently from any number of factories.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see ConfigurableBeanFactory#registerScope
 * @see CustomScopeConfigurer
 * @see org.springframework.aop.scope.ScopedProxyFactoryBean
 * @see org.springframework.web.context.request.RequestScope
 * @see org.springframework.web.context.request.SessionScope
 */
public interface Scope {

    /**
     * Return the object with the given name from the underlying scope,
     * {@link org.springframework.beans.factory.ObjectFactory#getObject() creating it}
     * if not found in the underlying storage mechanism.
     * <p>
     * This is the central operation of a Scope, and the only operation
     * that is absolutely required.
     * 
     * @param name
     *            the name of the object to retrieve
     * @param objectFactory
     *            the {@link ObjectFactory} to use to create the scoped
     *            object if it is not present in the underlying storage mechanism
     * @return the desired object (never {@code null})
     * @throws IllegalStateException
     *             if the underlying scope is not currently active
     */
    Object get(String name, ObjectFactory<?> objectFactory);

    /**
     * Remove the object with the given {@code name} from the underlying scope.
     * <p>
     * Returns {@code null} if no object was found; otherwise
     * returns the removed {@code Object}.
     * <p>
     * Note that an implementation should also remove a registered destruction
     * callback for the specified object, if any. It does, however, <i>not</i>
     * need to <i>execute</i> a registered destruction callback in this case,
     * since the object will be destroyed by the caller (if appropriate).
     * <p>
     * <b>Note: This is an optional operation.</b> Implementations may throw
     * {@link UnsupportedOperationException} if they do not support explicitly
     * removing an object.
     * 
     * @param name
     *            the name of the object to remove
     * @return the removed object, or {@code null} if no object was present
     * @throws IllegalStateException
     *             if the underlying scope is not currently active
     * @see #registerDestructionCallback
     */
    Object remove(String name);

}
