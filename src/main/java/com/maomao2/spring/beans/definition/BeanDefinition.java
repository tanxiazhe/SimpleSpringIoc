package com.maomao2.spring.beans.definition;

import com.maomao2.spring.beans.creation.ConfigureBeanFactory;

public interface BeanDefinition {

    /**
     * Scope identifier for the standard singleton scope: "singleton".
     * <p>
     * Note that extended bean factories might support further scopes.
     * 
     * @see #setScope
     */
    String SCOPE_SINGLETON = ConfigureBeanFactory.SCOPE_SINGLETON;

    /**
     * Scope identifier for the standard prototype scope: "prototype".
     * <p>
     * Note that extended bean factories might support further scopes.
     * 
     * @see #setScope
     */
    String SCOPE_PROTOTYPE = ConfigureBeanFactory.SCOPE_PROTOTYPE;

    /**
     * Return the current bean class name of this bean definition.
     * <p>
     * Note that this does not have to be the actual class name used at runtime, in
     * case of a child definition overriding/inheriting the class name from its parent.
     * Hence, do <i>not</i> consider this to be the definitive bean type at runtime but
     * rather only use it for parsing purposes at the individual bean definition level.
     */
    String getBeanClassName();

    /**
     * Override the bean class name of this bean definition.
     * <p>
     * The class name can be modified during bean factory post-processing,
     * typically replacing the original class name with a parsed variant of it.
     */
    void setBeanClassName(String beanClassName);

    /**
     * Return the factory bean name, if any.
     */
    String getFactoryBeanName();

    /**
     * Specify the factory bean to use, if any.
     */
    void setFactoryBeanName(String factoryBeanName);

    /**
     * Return a factory method, if any.
     */
    String getFactoryMethodName();

    /**
     * Specify a factory method, if any. This method will be invoked with
     * constructor arguments, or with no arguments if none are specified.
     * The method will be invoked on the specified factory bean, if any,
     * or otherwise as a static method on the local bean class.
     * 
     * @param factoryMethodName
     *            static factory method name,
     *            or {@code null} if normal constructor creation should be used
     * @see #getBeanClassName()
     */
    void setFactoryMethodName(String factoryMethodName);

    /**
     * Return the name of the current target scope for this bean,
     * or {@code null} if not known yet.
     */
    String getScope();

    /**
     * Override the target scope of this bean, specifying a new scope name.
     * 
     * @see #SCOPE_SINGLETON
     * @see #SCOPE_PROTOTYPE
     */
    void setScope(String scope);

    /**
     * Return whether this bean should be lazily initialized, i.e. not
     * eagerly instantiated on startup. Only applicable to a singleton bean.
     */
    boolean isLazyInit();

    /**
     * Set whether this bean should be lazily initialized.
     * <p>
     * If {@code false}, the bean will get instantiated on startup by bean
     * factories that perform eager initialization of singletons.
     */
    void setLazyInit(boolean lazyInit);

    /**
     * Return the bean names that this bean depends on.
     */
    String[] getDependsOn();

    /**
     * Set the names of the beans that this bean depends on being initialized.
     * The bean factory will guarantee that these beans get initialized first.
     */
    void setDependsOn(String... dependsOn);

    /**
     * Return whether this bean is a candidate for getting autowired into some other bean.
     */
    boolean isAutowireCandidate();

    /**
     * Set whether this bean is a candidate for getting autowired into some other bean.
     */
    void setAutowireCandidate(boolean autowireCandidate);

    /**
     * Return the constructor argument values for this bean.
     * <p>
     * The returned instance can be modified during bean factory post-processing.
     * 
     * @return the ConstructorArgumentValues object (never {@code null})
     */
    ConstructorArgumentValues getConstructorArgumentValues();

    /**
     * Return the property values to be applied to a new instance of the bean.
     * <p>
     * The returned instance can be modified during bean factory post-processing.
     * 
     * @return the MutablePropertyValues object (never {@code null})
     */
    MutablePropertyValues getPropertyValues();

    /**
     * Return whether this a <b>Singleton</b>, with a single, shared instance
     * returned on all calls.
     * 
     * @see #SCOPE_SINGLETON
     */
    boolean isSingleton();

    /**
     * Return whether this a <b>Prototype</b>, with an independent instance
     * returned for each call.
     * 
     * @see #SCOPE_PROTOTYPE
     */
    boolean isPrototype();

}
