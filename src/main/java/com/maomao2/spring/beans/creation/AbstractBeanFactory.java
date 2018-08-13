package com.maomao2.spring.beans.creation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.maomao2.spring.beans.definition.AbstractBeanDefinition;
import com.maomao2.spring.beans.definition.BeanDefinition;
import com.maomao2.spring.beans.definition.ConstructorArgumentValues;
import com.maomao2.spring.beans.definition.MutablePropertyValues;
import com.maomao2.spring.beans.definition.PropertyValues;
import com.maomao2.spring.beans.definition.RootBeanDefinition;
import com.maomao2.spring.exception.BeanCreationException;
import com.maomao2.spring.exception.BeanDefinitionStoreException;
import com.maomao2.spring.exception.BeanNotOfRequiredTypeException;
import com.maomao2.spring.exception.BeansException;
import com.maomao2.spring.exception.CannotLoadBeanClassException;
import com.maomao2.spring.util.ClassUtils;
import com.maomao2.spring.util.ReflectionUtils;

public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry {

    Logger logger = Logger.getLogger(getClass());
    /** ClassLoader to resolve bean class names with, if necessary */
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    /** BeanPostProcessors to apply in createBean */
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();
    /** Map from bean name to merged RootBeanDefinition */
    private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<String, RootBeanDefinition>(
            256);
    /** Map between dependent bean names: bean name --> Set of dependent bean names */
    private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

    /** Map between depending bean names: bean name --> Set of bean names for the bean's dependencies */
    private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

    /** Map from scope identifier String to corresponding Scope */
    private final Map<String, Scope> scopes = new LinkedHashMap<String, Scope>(8);
    /** A custom TypeConverter to use, overriding the default PropertyEditor mechanism */
    private TypeConverter typeConverter;

    /**
     * Return the list of BeanPostProcessors that will get applied
     * to beans created with this factory.
     */
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }

    public void setTypeConverter(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    /**
     * Return the custom TypeConverter to use, if any.
     * 
     * @return the custom TypeConverter, or {@code null} if none specified
     */
    protected TypeConverter getCustomTypeConverter() {
        return this.typeConverter;
    }

    public TypeConverter getTypeConverter() {
        TypeConverter customConverter = getCustomTypeConverter();
        if (customConverter != null) {
            return customConverter;
        }
        return new SimpleTypeConverter();
    }

    public void setBeanClassLoader(ClassLoader beanClassLoader) {
        this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
    }

    public ClassLoader getBeanClassLoader() {
        return this.beanClassLoader;
    }

    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null, null, false);
    }

    /**
     * Return an instance, which may be shared or independent, of the specified bean.
     * 
     * @param name
     *            the name of the bean to retrieve
     * @param requiredType
     *            the required type of the bean to retrieve
     * @param args
     *            arguments to use when creating a bean instance using explicit arguments
     *            (only applied when creating a new instance as opposed to retrieving an existing one)
     * @param typeCheckOnly
     *            whether the instance is obtained for a type check,
     *            not for actual use
     * @return an instance of the bean
     * @throws BeansException
     *             if the bean could not be created
     */
    protected <T> T doGetBean(
            final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly) {
        Object bean;
        // return the actual bean name,主要是区分FactoryBean，这个bean特殊，以&开始
        final String beanName = transformedBeanName(name);

        // Eagerly check singleton cache for manually registered singletons.
        Object sharedInstance = getSingleton(beanName);
        // check exists bean
        if (sharedInstance != null && args == null) {
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
        } else {
            try {
                final RootBeanDefinition mbd = new RootBeanDefinition(beanName);

                // Guarantee initialization of beans that the current bean depends on.
                String[] dependsOn = mbd.getDependsOn();
                if (dependsOn != null) {
                    for (String dep : dependsOn) {
                        if (isDependent(beanName, dep)) {
                            throw new BeanCreationException(beanName,
                                    "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                        }
                        registerDependentBean(dep, beanName);
                        getBean(dep);
                    }
                }

                // if not
                // Create bean instance.

                // isSingleton
                if (mbd.isSingleton()) {
                    sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
                        public Object getObject() throws BeansException {
                            return createBean(beanName, mbd, args);
                        }
                    });
                    bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
                }

                // isPrototype
                else if (mbd.isPrototype()) {
                    // It's a prototype -> create a new instance.
                    Object prototypeInstance = createBean(beanName, mbd, args);
                    bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
                }
                // other scope
                else {
                    String scopeName = mbd.getScope();
                    final Scope scope = this.scopes.get(scopeName);
                    if (scope == null) {
                        throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                    }
                    try {
                        Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {

                            public Object getObject() throws BeansException {
                                return createBean(beanName, mbd, args);
                            }
                        });
                        bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                    } catch (IllegalStateException ex) {
                        throw new BeanCreationException(beanName,
                                "Scope '" + scopeName + "' is not active for the current thread; consider " +
                                        "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                                ex);
                    }
                }
            } catch (BeansException ex) {
                throw ex;
            }
        }

        // Check if required type matches the type of the actual bean instance.
        if (requiredType != null && bean != null && !requiredType.isAssignableFrom(bean.getClass())) {
            try {
                return getTypeConverter().convertIfNecessary(bean, requiredType);
            } catch (TypeMismatchException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to convert bean '" + name + "' to required type '" +
                            ClassUtils.getQualifiedName(requiredType) + "'", ex);
                }
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
        }
        return (T) bean;
    }

    private String transformedBeanName(String name) {
        return name;
    }

    /**
     * Get the object for the given bean instance, either the bean
     * instance itself or its created object in case of a FactoryBean.
     * 
     * @param beanInstance
     *            the shared bean instance
     * @param name
     *            name that may include factory dereference prefix
     * @param beanName
     *            the canonical bean name
     * @param mbd
     *            the merged bean definition
     * @return the object to expose for the bean
     */
    protected Object getObjectForBeanInstance(
            Object beanInstance, String name, String beanName, RootBeanDefinition mbd) {
        return beanInstance;
    }

    private boolean isDependent(String beanName, String dependentBeanName) {
        String canonicalName = transformedBeanName(beanName);
        Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
        if (dependentBeans == null) {
            return false;
        }
        if (dependentBeans.contains(dependentBeanName)) {
            return true;
        }
        return false;
    }

    /**
     * Register a dependent bean for the given bean,
     * to be destroyed before the given bean is destroyed.
     * 
     * @param beanName
     *            the name of the bean
     * @param dependentBeanName
     *            the name of the dependent bean
     */
    public void registerDependentBean(String beanName, String dependentBeanName) {
        // A quick check for an existing entry upfront, avoiding synchronization...
        String canonicalName = transformedBeanName(beanName);
        Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
        if (dependentBeans != null && dependentBeans.contains(dependentBeanName)) {
            return;
        }

        // No entry yet -> fully synchronized manipulation of the dependentBeans Set
        synchronized (this.dependentBeanMap) {
            dependentBeans = this.dependentBeanMap.get(canonicalName);
            if (dependentBeans == null) {
                dependentBeans = new LinkedHashSet<String>(8);
                this.dependentBeanMap.put(canonicalName, dependentBeans);
            }
            dependentBeans.add(dependentBeanName);
        }
        synchronized (this.dependenciesForBeanMap) {
            Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(dependentBeanName);
            if (dependenciesForBean == null) {
                dependenciesForBean = new LinkedHashSet<String>(8);
                this.dependenciesForBeanMap.put(dependentBeanName, dependenciesForBean);
            }
            dependenciesForBean.add(canonicalName);
        }
    }

    protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating instance of bean '" + beanName + "'");
        }
        RootBeanDefinition mbdToUse = mbd;

        // Make sure bean class is actually resolved at this point, and
        // clone the bean definition in case of a dynamically resolved Class
        // which cannot be stored in the shared merged bean definition.
        Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
        if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
            mbdToUse = new RootBeanDefinition(mbd);
            mbdToUse.setBeanClass(resolvedClass);
        }

        try {
            // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
            Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
            if (bean != null) {
                return bean;
            }
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName,
                    "BeanPostProcessor before instantiation of bean failed", ex);
        }

        try {
            Object beanInstance = doCreateBean(beanName, mbdToUse, args);
            logger.info("Finished creating instance of bean '" + beanName + "'");
            return beanInstance;
        } catch (BeanCreationException ex) {
            // A previously detected exception with proper bean creation context already...
            throw ex;
        }
    }

    /**
     * Resolve the bean class for the specified bean definition,
     * resolving a bean class name into a Class reference (if necessary)
     * and storing the resolved Class in the bean definition for further use.
     * 
     * @param mbd
     *            the merged bean definition to determine the class for
     * @param beanName
     *            the name of the bean (for error handling purposes)
     * @param typesToMatch
     *            the types to match in case of internal type matching purposes
     *            (also signals that the returned {@code Class} will never be exposed to application code)
     * @return the resolved bean class (or {@code null} if none)
     * @throws CannotLoadBeanClassException
     *             if we failed to load the class
     */
    protected Class<?> resolveBeanClass(final RootBeanDefinition mbd, String beanName, final Class<?>... typesToMatch)
            throws CannotLoadBeanClassException {
        try {
            if (mbd.hasBeanClass()) {
                return mbd.getBeanClass();
            } else {
                return doResolveBeanClass(mbd, typesToMatch);
            }
        } catch (ClassNotFoundException ex) {
            throw new CannotLoadBeanClassException(beanName, mbd.getBeanClassName(), ex);
        }

    }

    private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch)
            throws ClassNotFoundException {

        ClassLoader beanClassLoader = getBeanClassLoader();
        return mbd.resolveBeanClass(beanClassLoader);
    }

    /**
     * Actually create the specified bean. Pre-creation processing has already happened
     * at this point, e.g. checking {@code postProcessBeforeInstantiation} callbacks.
     * <p>
     * Differentiates between default bean instantiation, use of a
     * factory method, and autowiring a constructor.
     * 
     * @param beanName
     *            the name of the bean
     * @param mbd
     *            the merged bean definition for the bean
     * @param args
     *            explicit arguments to use for constructor or factory method invocation
     * @return a new instance of the bean
     * @throws BeanCreationException
     *             if the bean could not be created
     * @see #instantiateBean
     * @see #instantiateUsingFactoryMethod
     * @see #autowireConstructor
     */
    protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
            throws BeanCreationException {

        // Instantiate the bean.
        BeanWrapper instanceWrapper = null;

        if (instanceWrapper == null) {
            instanceWrapper = createBeanInstance(beanName, mbd, args);
        }
        final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
        Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);

        // Eagerly cache singletons to be able to resolve circular references
        // even when triggered by lifecycle interfaces like BeanFactoryAware.
        boolean earlySingletonExposure = mbd.isSingleton();

        // Initialize the bean instance.
        Object exposedObject = bean;
        try {
            populateBean(beanName, mbd, instanceWrapper);
            if (exposedObject != null) {
                exposedObject = initializeBean(beanName, exposedObject, mbd);
            }
        } catch (Throwable ex) {
            if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
                throw (BeanCreationException) ex;
            } else {
                throw new BeanCreationException(
                        beanName, "Initialization of bean failed", ex);
            }
        }

        if (earlySingletonExposure) {
            Object earlySingletonReference = getSingleton(beanName);
            if (earlySingletonReference != null) {
                if (exposedObject == bean) {
                    exposedObject = earlySingletonReference;
                }
            }
        }

        return exposedObject;
    }

    /**
     * Populate the bean instance in the given BeanWrapper with the property values
     * from the bean definition.
     * 
     * @param beanName
     *            the name of the bean
     * @param mbd
     *            the bean definition for the bean
     * @param bw
     *            BeanWrapper with bean instance
     */
    protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
        PropertyValues pvs = mbd.getPropertyValues();

        if (bw == null) {
            if (!pvs.isEmpty()) {
                throw new BeanCreationException(
                        beanName, "Cannot apply property values to null instance");
            } else {
                // Skip property population phase for null instance.
                return;
            }
        }

        // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
        // state of the bean before properties are set. This can be used, for example,
        // to support styles of field injection.
        boolean continueWithPropertyPopulation = true;

        if (!continueWithPropertyPopulation) {
            return;
        }

        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
                mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
            MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

            // Add property values based on autowire by name if applicable.
            if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
                autowireByName(beanName, mbd, bw, newPvs);
            }

            // Add property values based on autowire by type if applicable.
            if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
                autowireByType(beanName, mbd, bw, newPvs);
            }

            pvs = newPvs;
        }

        applyPropertyValues(beanName, mbd, bw, pvs);
    }

    public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
        BeanDefinition bd = new RootBeanDefinition(beanName);
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        applyPropertyValues(beanName, bd, bw, bd.getPropertyValues());
    }

    /**
     * Apply the given property values, resolving any runtime references
     * to other beans in this bean factory. Must use deep copy, so we
     * don't permanently modify this property.
     * 
     * @param beanName
     *            the bean name passed for better exception information
     * @param mbd
     *            the merged bean definition
     * @param bw
     *            the BeanWrapper wrapping the target object
     * @param pvs
     *            the new property values
     */
    protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
        if (pvs == null || pvs.isEmpty()) {
            return;
        }

        MutablePropertyValues mpvs = null;
        List<PropertyValues> original;

        if (pvs instanceof MutablePropertyValues) {
            mpvs = (MutablePropertyValues) pvs;
            // if (mpvs.isConverted()) {
            // // Shortcut: use the pre-converted values as-is.
            // try {
            // bw.setPropertyValues(mpvs);
            // return;
            // } catch (BeansException ex) {
            // throw new BeanCreationException(
            // mbd.getResourceDescription(), beanName, "Error setting property values", ex);
            // }
            // }
            // original = mpvs.getPropertyValueList();
            // } else {
            // original = Arrays.asList(pvs.getPropertyValues());
            // }
            //
            // TypeConverter converter = getCustomTypeConverter();
            // if (converter == null) {
            // converter = bw;
            // }
            // BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd,
            // converter);
            //
            // // Create a deep copy, resolving any references for values.
            // List<PropertyValue> deepCopy = new ArrayList<>(original.size());
            // boolean resolveNecessary = false;
            // for (PropertyValue pv : original) {
            // if (pv.isConverted()) {
            // deepCopy.add(pv);
            // } else {
            // String propertyName = pv.getName();
            // Object originalValue = pv.getValue();
            // Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
            // Object convertedValue = resolvedValue;
            // boolean convertible = bw.isWritableProperty(propertyName) &&
            // !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
            // if (convertible) {
            // convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
            // }
            // // Possibly store converted value in merged bean definition,
            // // in order to avoid re-conversion for every created bean instance.
            // if (resolvedValue == originalValue) {
            // if (convertible) {
            // pv.setConvertedValue(convertedValue);
            // }
            // deepCopy.add(pv);
            // } else if (convertible && originalValue instanceof TypedStringValue &&
            // !((TypedStringValue) originalValue).isDynamic() &&
            // !(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
            // pv.setConvertedValue(convertedValue);
            // deepCopy.add(pv);
            // } else {
            // resolveNecessary = true;
            // deepCopy.add(new PropertyValue(pv, convertedValue));
            // }
            // }
            // }
            // if (mpvs != null && !resolveNecessary) {
            // mpvs.setConverted();
            // }
            //
            // // Set our (possibly massaged) deep copy.
            // try {
            // bw.setPropertyValues(new MutablePropertyValues(deepCopy));
            // } catch (BeansException ex) {
            // throw new BeanCreationException(
            // mbd.getResourceDescription(), beanName, "Error setting property values", ex);
            // }
        }
    }

    protected void initBeanWrapper(BeanWrapper bw) {
    }

    /**
     * Fill in any missing property values with references to
     * other beans in this factory if autowire is set to "byName".
     * 
     * @param beanName
     *            the name of the bean we're wiring up.
     *            Useful for debugging messages; not used functionally.
     * @param mbd
     *            bean definition to update through autowiring
     * @param bw
     *            BeanWrapper from which we can obtain information about the bean
     * @param pvs
     *            the PropertyValues to register wired objects with
     */
    protected void autowireByName(
            String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

        // String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        // for (String propertyName : propertyNames) {
        // if (containsBean(propertyName)) {
        // Object bean = getBean(propertyName);
        // pvs.add(propertyName, bean);
        // registerDependentBean(propertyName, beanName);
        // if (logger.isDebugEnabled()) {
        // logger.debug("Added autowiring by name from bean name '" + beanName +
        // "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
        // }
        // } else {
        // if (logger.isTraceEnabled()) {
        // logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
        // "' by name: no matching bean found");
        // }
        // }
        // }
    }

    /**
     * Abstract method defining "autowire by type" (bean properties by type) behavior.
     * <p>
     * This is like PicoContainer default, in which there must be exactly one bean
     * of the property type in the bean factory. This makes bean factories simple to
     * configure for small namespaces, but doesn't work as well as standard Spring
     * behavior for bigger applications.
     * 
     * @param beanName
     *            the name of the bean to autowire by type
     * @param mbd
     *            the merged bean definition to update through autowiring
     * @param bw
     *            BeanWrapper from which we can obtain information about the bean
     * @param pvs
     *            the PropertyValues to register wired objects with
     */
    protected void autowireByType(
            String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

        TypeConverter converter = getCustomTypeConverter();
        // if (converter == null) {
        // converter = bw;
        // }

        Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
        // String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        // for (String propertyName : propertyNames) {
        // try {
        // PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
        // // Don't try autowiring by type for type Object: never makes sense,
        // // even if it technically is a unsatisfied, non-simple property.
        // if (Object.class != pd.getPropertyType()) {
        // MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
        // // Do not allow eager init for type matching in case of a prioritized post-processor.
        // boolean eager = !PriorityOrdered.class.isAssignableFrom(bw.getWrappedClass());
        // DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
        // Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
        // if (autowiredArgument != null) {
        // pvs.add(propertyName, autowiredArgument);
        // }
        // for (String autowiredBeanName : autowiredBeanNames) {
        // registerDependentBean(autowiredBeanName, beanName);
        // if (logger.isDebugEnabled()) {
        // logger.debug("Autowiring by type from bean name '" + beanName + "' via property '" +
        // propertyName + "' to bean named '" + autowiredBeanName + "'");
        // }
        // }
        // autowiredBeanNames.clear();
        // }
        // } catch (BeansException ex) {
        // throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
        // }
        // }
    }

    /**
     * Initialize the given bean instance, applying factory callbacks
     * as well as init methods and bean post processors.
     * <p>
     * Called from {@link #createBean} for traditionally defined beans,
     * and from {@link #initializeBean} for existing bean instances.
     * 
     * @param beanName
     *            the bean name in the factory (for debugging purposes)
     * @param bean
     *            the new bean instance we may need to initialize
     * @param mbd
     *            the bean definition that the bean was created with
     *            (can also be {@code null}, if given an existing bean instance)
     * @return the initialized bean instance (potentially wrapped)
     * @see BeanNameAware
     * @see BeanClassLoaderAware
     * @see BeanFactoryAware
     * @see #applyBeanPostProcessorsBeforeInitialization
     * @see #invokeInitMethods
     * @see #applyBeanPostProcessorsAfterInitialization
     */
    protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {

        Object wrappedBean = bean;
        if (mbd == null) {
            wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
        }

        try {
            invokeInitMethods(beanName, wrappedBean, mbd);
        } catch (Throwable ex) {
            throw new BeanCreationException(
                    beanName, "Invocation of init method failed", ex);
        }

        if (mbd == null) {
            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        }
        return wrappedBean;
    }

    /**
     * Give a bean a chance to react now all its properties are set,
     * and a chance to know about its owning bean factory (this object).
     * This means checking whether the bean implements InitializingBean or defines
     * a custom init method, and invoking the necessary callback(s) if it does.
     * 
     * @param beanName
     *            the bean name in the factory (for debugging purposes)
     * @param bean
     *            the new bean instance we may need to initialize
     * @param mbd
     *            the merged bean definition that the bean was created with
     *            (can also be {@code null}, if given an existing bean instance)
     * @throws Throwable
     *             if thrown by init methods or by the invocation process
     * @see #invokeCustomInitMethod
     */
    protected void invokeInitMethods(String beanName, final Object bean, RootBeanDefinition mbd)
            throws Throwable {

        boolean isInitializingBean = (bean instanceof InitializingBean);
        if (isInitializingBean && (mbd == null)) {
            logger.info("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
            ((InitializingBean) bean).afterPropertiesSet();
        }

        if (mbd != null) {
            String initMethodName = mbd.getInitMethodName();
            if (initMethodName != null && !(isInitializingBean && "afterPropertiesSet".equals(initMethodName))) {
                invokeCustomInitMethod(beanName, bean, mbd);
            }
        }
    }

    /**
     * Invoke the specified custom init method on the given bean.
     * Called by invokeInitMethods.
     * <p>
     * Can be overridden in subclasses for custom resolution of init
     * methods with arguments.
     * 
     * @see #invokeInitMethods
     */
    protected void invokeCustomInitMethod(String beanName, final Object bean, RootBeanDefinition mbd)
            throws Throwable {

        String initMethodName = mbd.getInitMethodName();
        final Method initMethod = bean.getClass().getMethod(initMethodName, null);
        if (initMethod == null) {
            return;
        }

        logger.info("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");

        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    ReflectionUtils.makeAccessible(initMethod);
                    return null;
                }
            });
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        initMethod.invoke(bean);
                        return null;
                    }
                }, null);
            } catch (PrivilegedActionException pae) {
                InvocationTargetException ex = (InvocationTargetException) pae.getException();
                throw ex.getTargetException();
            }
        } else {
            try {
                ReflectionUtils.makeAccessible(initMethod);
                initMethod.invoke(bean);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

    public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
            throws BeansException {

        Object result = existingBean;
        for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
            result = beanProcessor.postProcessBeforeInitialization(result, beanName);
            if (result == null) {
                return result;
            }
        }
        return result;
    }

    /**
     * Create a new instance for the specified bean, using an appropriate instantiation strategy:
     * factory method, constructor autowiring, or simple instantiation.
     * 
     * @param beanName
     *            the name of the bean
     * @param mbd
     *            the bean definition for the bean
     * @param args
     *            explicit arguments to use for constructor or factory method invocation
     * @return BeanWrapper for the new instance
     * @see #instantiateUsingFactoryMethod
     * @see #autowireConstructor
     * @see #instantiateBean
     */
    protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
        // Make sure bean class is actually resolved at this point.
        Class<?> beanClass = resolveBeanClass(mbd, beanName);

        if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers())) {
            throw new BeanCreationException(beanName,
                    "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
        }

        if (mbd.getFactoryMethodName() != null) {
            return instantiateUsingFactoryMethod(beanName, mbd, args);
        }

        // Shortcut when re-creating the same bean...
        boolean resolved = false;
        boolean autowireNecessary = false;
        if (args == null) {
            return autowireConstructor(beanName, mbd, null, null);
        } else {
            Constructor<?>[] ctors = null;
            if (ctors != null) {
                return autowireConstructor(beanName, mbd, ctors, args);
            }
        }

        // No special handling: simply use no-arg constructor.
        return instantiateBean(beanName, mbd);
    }

    public BeanWrapper instantiateUsingFactoryMethod(
            final String beanName, final RootBeanDefinition mbd, final Object[] explicitArgs) {

        BeanWrapperImpl bw = new BeanWrapperImpl();
        this.initBeanWrapper(bw);

        Object factoryBean;
        Class<?> factoryClass;
        boolean isStatic;

        String factoryBeanName = mbd.getFactoryBeanName();
        if (factoryBeanName != null) {
            if (factoryBeanName.equals(beanName)) {
                throw new BeanDefinitionStoreException(beanName,
                        "factory-bean reference points back to the same bean definition");
            }
            factoryBean = this.getBean(factoryBeanName);
            if (factoryBean == null) {
                throw new BeanCreationException(beanName,
                        "factory-bean '" + factoryBeanName + "' (or a BeanPostProcessor involved) returned null");
            }
            // if (mbd.isSingleton() && this.containsSingleton(beanName)) {
            // throw new ImplicitlyAppearedSingletonException();
            // }
            factoryClass = factoryBean.getClass();
            isStatic = false;
        } else {
            // It's a static factory method on the bean class.
            if (!mbd.hasBeanClass()) {
                throw new BeanDefinitionStoreException(beanName,
                        "bean definition declares neither a bean class nor a factory-bean reference");
            }
            factoryBean = null;
            factoryClass = mbd.getBeanClass();
            isStatic = true;
        }

        Method factoryMethodToUse = null;
        // ArgumentsHolder argsHolderToUse = null;
        Object[] argsToUse = null;

        if (explicitArgs != null) {
            argsToUse = explicitArgs;
        } else {
            Object[] argsToResolve = null;
            // synchronized (mbd.constructorArgumentLock) {
            // factoryMethodToUse = (Method) mbd.resolvedConstructorOrFactoryMethod;
            // if (factoryMethodToUse != null && mbd.constructorArgumentsResolved) {
            // // Found a cached factory method...
            // argsToUse = mbd.resolvedConstructorArguments;
            // if (argsToUse == null) {
            // argsToResolve = mbd.preparedConstructorArguments;
            // }
            // }
            // }
            // if (argsToResolve != null) {
            // argsToUse = resolvePreparedArguments(beanName, mbd, bw, factoryMethodToUse, argsToResolve);
            // }
        }

        if (factoryMethodToUse == null || argsToUse == null) {
            // Need to determine the factory method...
            // Try all methods with this name to see if they match the given arguments.
            factoryClass = ClassUtils.getUserClass(factoryClass);

            // Method[] rawCandidates = getCandidateMethods(factoryClass, mbd);
            List<Method> candidateSet = new ArrayList<>();
            // for (Method candidate : rawCandidates) {
            // if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
            // candidateSet.add(candidate);
            // }
            // }
            Method[] candidates = candidateSet.toArray(new Method[candidateSet.size()]);
            // AutowireUtils.sortFactoryMethods(candidates);

            ConstructorArgumentValues resolvedValues = null;
            boolean autowiring = (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            int minTypeDiffWeight = Integer.MAX_VALUE;
            Set<Method> ambiguousFactoryMethods = null;

            int minNrOfArgs;
            // if (explicitArgs != null) {
            // minNrOfArgs = explicitArgs.length;
            // } else {
            // // We don't have arguments passed in programmatically, so we need to resolve the
            // // arguments specified in the constructor arguments held in the bean definition.
            // ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
            // resolvedValues = new ConstructorArgumentValues();
            // minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
            // }
            //
            // LinkedList<UnsatisfiedDependencyException> causes = null;
            //
            // for (Method candidate : candidates) {
            // Class<?>[] paramTypes = candidate.getParameterTypes();
            //
            // if (paramTypes.length >= minNrOfArgs) {
            // ArgumentsHolder argsHolder;
            //
            // if (resolvedValues != null) {
            // // Resolved constructor arguments: type conversion and/or autowiring necessary.
            // try {
            // String[] paramNames = null;
            // ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
            // if (pnd != null) {
            // paramNames = pnd.getParameterNames(candidate);
            // }
            // argsHolder = createArgumentArray(
            // beanName, mbd, resolvedValues, bw, paramTypes, paramNames, candidate, autowiring);
            // } catch (UnsatisfiedDependencyException ex) {
            // if (this.beanFactory.logger.isTraceEnabled()) {
            // this.beanFactory.logger.trace("Ignoring factory method [" + candidate +
            // "] of bean '" + beanName + "': " + ex);
            // }
            // // Swallow and try next overloaded factory method.
            // if (causes == null) {
            // causes = new LinkedList<>();
            // }
            // causes.add(ex);
            // continue;
            // }
            // }
            //
            // else {
            // // Explicit arguments given -> arguments length must match exactly.
            // if (paramTypes.length != explicitArgs.length) {
            // continue;
            // }
            // argsHolder = new ArgumentsHolder(explicitArgs);
            // }
            //
            // int typeDiffWeight = (mbd.isLenientConstructorResolution()
            // ? argsHolder.getTypeDifferenceWeight(paramTypes)
            // : argsHolder.getAssignabilityWeight(paramTypes));
            // // Choose this factory method if it represents the closest match.
            // if (typeDiffWeight < minTypeDiffWeight) {
            // factoryMethodToUse = candidate;
            // argsHolderToUse = argsHolder;
            // argsToUse = argsHolder.arguments;
            // minTypeDiffWeight = typeDiffWeight;
            // ambiguousFactoryMethods = null;
            // }
            // // Find out about ambiguity: In case of the same type difference weight
            // // for methods with the same number of parameters, collect such candidates
            // // and eventually raise an ambiguity exception.
            // // However, only perform that check in non-lenient constructor resolution mode,
            // // and explicitly ignore overridden methods (with the same parameter signature).
            // else if (factoryMethodToUse != null && typeDiffWeight == minTypeDiffWeight &&
            // !mbd.isLenientConstructorResolution() &&
            // paramTypes.length == factoryMethodToUse.getParameterCount() &&
            // !Arrays.equals(paramTypes, factoryMethodToUse.getParameterTypes())) {
            // if (ambiguousFactoryMethods == null) {
            // ambiguousFactoryMethods = new LinkedHashSet<>();
            // ambiguousFactoryMethods.add(factoryMethodToUse);
            // }
            // ambiguousFactoryMethods.add(candidate);
            // }
            // }
            // }
            //
            // if (factoryMethodToUse == null) {
            // if (causes != null) {
            // UnsatisfiedDependencyException ex = causes.removeLast();
            // for (Exception cause : causes) {
            // this.beanFactory.onSuppressedException(cause);
            // }
            // throw ex;
            // }
            // List<String> argTypes = new ArrayList<>(minNrOfArgs);
            // if (explicitArgs != null) {
            // for (Object arg : explicitArgs) {
            // argTypes.add(arg != null ? arg.getClass().getSimpleName() : "null");
            // }
            // } else {
            // Set<ValueHolder> valueHolders = new LinkedHashSet<>(resolvedValues.getArgumentCount());
            // valueHolders.addAll(resolvedValues.getIndexedArgumentValues().values());
            // valueHolders.addAll(resolvedValues.getGenericArgumentValues());
            // for (ValueHolder value : valueHolders) {
            // String argType = (value.getType() != null ? ClassUtils.getShortName(value.getType())
            // : (value.getValue() != null ? value.getValue().getClass().getSimpleName() : "null"));
            // argTypes.add(argType);
            // }
            // }
            // String argDesc = StringUtils.collectionToCommaDelimitedString(argTypes);
            // throw new BeanCreationException(mbd.getResourceDescription(), beanName,
            // "No matching factory method found: " +
            // (mbd.getFactoryBeanName() != null ? "factory bean '" + mbd.getFactoryBeanName() + "'; "
            // : "")
            // +
            // "factory method '" + mbd.getFactoryMethodName() + "(" + argDesc + ")'. " +
            // "Check that a method with the specified name " +
            // (minNrOfArgs > 0 ? "and arguments " : "") +
            // "exists and that it is " +
            // (isStatic ? "static" : "non-static") + ".");
            // } else if (void.class == factoryMethodToUse.getReturnType()) {
            // throw new BeanCreationException(mbd.getResourceDescription(), beanName,
            // "Invalid factory method '" + mbd.getFactoryMethodName() +
            // "': needs to have a non-void return type!");
            // } else if (ambiguousFactoryMethods != null) {
            // throw new BeanCreationException(mbd.getResourceDescription(), beanName,
            // "Ambiguous factory method matches found in bean '" + beanName + "' " +
            // "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): "
            // +
            // ambiguousFactoryMethods);
            // }
            //
            // if (explicitArgs == null && argsHolderToUse != null) {
            // argsHolderToUse.storeCache(mbd, factoryMethodToUse);
            // }
        }

        try {
            Object beanInstance;

            beanInstance = this.instantiate(
                    mbd, beanName, this, factoryBean, factoryMethodToUse, argsToUse);

            if (beanInstance == null) {
                return null;
            }
            bw.setBeanInstance(beanInstance);
            return bw;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName,
                    "Bean instantiation via factory method failed", ex);
        }
    }

    private Object instantiate(RootBeanDefinition mbd, String beanName, AbstractBeanFactory abstractBeanFactory,
            Object factoryBean, Method factoryMethodToUse, Object[] argsToUse) {
        // TODO Auto-generated method stub
        return null;
    }

    private BeanWrapper autowireConstructor(final String beanName, final RootBeanDefinition mbd,
            Constructor<?>[] chosenCtors, final Object[] explicitArgs) {
        BeanWrapperImpl bw = new BeanWrapperImpl();
        this.initBeanWrapper(bw);

        Constructor<?> constructorToUse = null;
        // ArgumentsHolder argsHolderToUse = null;
        Object[] argsToUse = null;

        if (explicitArgs != null) {
            argsToUse = explicitArgs;
        } else {
            Object[] argsToResolve = null;
            // synchronized (mbd.constructorArgumentLock) {
            // constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
            // if (constructorToUse != null && mbd.constructorArgumentsResolved) {
            // // Found a cached constructor...
            // argsToUse = mbd.resolvedConstructorArguments;
            // if (argsToUse == null) {
            // argsToResolve = mbd.preparedConstructorArguments;
            // }
            // }
            // }
            // if (argsToResolve != null) {
            // argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve);
            // }
        }

        if (constructorToUse == null) {
            // Need to resolve the constructor.
            boolean autowiring = (chosenCtors != null ||
                    mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            ConstructorArgumentValues resolvedValues = null;

            int minNrOfArgs;
            if (explicitArgs != null) {
                minNrOfArgs = explicitArgs.length;
            } else {
                ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
                resolvedValues = new ConstructorArgumentValues();
                // minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
            }

            // Take specified constructors, if any.
            Constructor<?>[] candidates = chosenCtors;
            // if (candidates == null) {
            // Class<?> beanClass = mbd.getBeanClass();
            // try {
            // candidates = (mbd.isNonPublicAccessAllowed() ? beanClass.getDeclaredConstructors()
            // : beanClass.getConstructors());
            // } catch (Throwable ex) {
            // throw new BeanCreationException(mbd.getResourceDescription(), beanName,
            // "Resolution of declared constructors on bean Class [" + beanClass.getName() +
            // "] from ClassLoader [" + beanClass.getClassLoader() + "] failed",
            // ex);
            // }
            // }
            // AutowireUtils.sortConstructors(candidates);
            // int minTypeDiffWeight = Integer.MAX_VALUE;
            // Set<Constructor<?>> ambiguousConstructors = null;
            // LinkedList<UnsatisfiedDependencyException> causes = null;

            // for (Constructor<?> candidate : candidates) {
            // Class<?>[] paramTypes = candidate.getParameterTypes();
            //
            // if (constructorToUse != null && argsToUse.length > paramTypes.length) {
            // // Already found greedy constructor that can be satisfied ->
            // // do not look any further, there are only less greedy constructors left.
            // break;
            // }
            // if (paramTypes.length < minNrOfArgs) {
            // continue;
            // }

            // ArgumentsHolder argsHolder;
            // if (resolvedValues != null) {
            // try {
            // String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, paramTypes.length);
            // if (paramNames == null) {
            // ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
            // if (pnd != null) {
            // paramNames = pnd.getParameterNames(candidate);
            // }
            // }
            // argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
            // getUserDeclaredConstructor(candidate), autowiring);
            // } catch (UnsatisfiedDependencyException ex) {
            // if (this.beanFactory.logger.isTraceEnabled()) {
            // this.beanFactory.logger.trace(
            // "Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
            // }
            // // Swallow and try next constructor.
            // if (causes == null) {
            // causes = new LinkedList<>();
            // }
            // causes.add(ex);
            // continue;
            // }
            // } else {
            // // Explicit arguments given -> arguments length must match exactly.
            // if (paramTypes.length != explicitArgs.length) {
            // continue;
            // }
            // argsHolder = new ArgumentsHolder(explicitArgs);
            // }
            //
            // int typeDiffWeight = (mbd.isLenientConstructorResolution()
            // ? argsHolder.getTypeDifferenceWeight(paramTypes)
            // : argsHolder.getAssignabilityWeight(paramTypes));
            // // Choose this constructor if it represents the closest match.
            // if (typeDiffWeight < minTypeDiffWeight) {
            // constructorToUse = candidate;
            // argsHolderToUse = argsHolder;
            // argsToUse = argsHolder.arguments;
            // minTypeDiffWeight = typeDiffWeight;
            // ambiguousConstructors = null;
            // } else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
            // if (ambiguousConstructors == null) {
            // ambiguousConstructors = new LinkedHashSet<>();
            // ambiguousConstructors.add(constructorToUse);
            // }
            // ambiguousConstructors.add(candidate);
            // }
            // }
            //
            // if (constructorToUse == null) {
            // if (causes != null) {
            // UnsatisfiedDependencyException ex = causes.removeLast();
            // for (Exception cause : causes) {
            // this.beanFactory.onSuppressedException(cause);
            // }
            // throw ex;
            // }
            // throw new BeanCreationException(mbd.getResourceDescription(), beanName,
            // "Could not resolve matching constructor " +
            // "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
            // } else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
            // throw new BeanCreationException(mbd.getResourceDescription(), beanName,
            // "Ambiguous constructor matches found in bean '" + beanName + "' " +
            // "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): "
            // +
            // ambiguousConstructors);
            // }
            //
            // if (explicitArgs == null) {
            // argsHolderToUse.storeCache(mbd, constructorToUse);
            // }
        }

        try {
            Object beanInstance;

            beanInstance = instantiate(
                    mbd, beanName, this, constructorToUse, argsToUse);

            bw.setBeanInstance(beanInstance);
            return bw;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName,
                    "Bean instantiation via constructor failed", ex);
        }
    }

    private Object instantiate(RootBeanDefinition mbd, String beanName, AbstractBeanFactory abstractBeanFactory,
            Constructor<?> constructorToUse, Object[] argsToUse) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Instantiate the given bean using its default constructor.
     * 
     * @param beanName
     *            the name of the bean
     * @param mbd
     *            the bean definition for the bean
     * @return BeanWrapper for the new instance
     */
    protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
        try {
            Object beanInstance;
            final BeanFactory parent = (BeanFactory) this;
            beanInstance = instantiate(mbd, beanName, parent);
            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
            initBeanWrapper(bw);
            return bw;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Instantiation of bean failed", ex);
        }
    }

    private Object instantiate(RootBeanDefinition mbd, String beanName, BeanFactory parent)
            throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        final Class<?> clazz = mbd.getBeanClass();
        Constructor<?> constructorToUse = clazz.getDeclaredConstructor((Class[]) null);
        return constructorToUse.newInstance(null);
    }

    /**
     * Apply before-instantiation post-processors, resolving whether there is a
     * before-instantiation shortcut for the specified bean.
     * 
     * @param beanName
     *            the name of the bean
     * @param mbd
     *            the bean definition for the bean
     * @return the shortcut-determined bean instance, or {@code null} if none
     */
    protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
        Object bean = null;
        // Make sure bean class is actually resolved at this point.
        Class<?> targetType = determineTargetType(beanName, mbd);
        if (targetType != null) {
            bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
            if (bean != null) {
                bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
            }

        }
        return bean;

    }

    /**
     * Determine the target type for the given bean definition.
     * 
     * @param beanName
     *            the name of the bean (for error handling purposes)
     * @param mbd
     *            the merged bean definition for the bean
     * @param typesToMatch
     *            the types to match in case of internal type matching purposes
     *            (also signals that the returned {@code Class} will never be exposed to application code)
     * @return the type for the bean if determinable, or {@code null} otherwise
     */
    protected Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = resolveBeanClass(mbd, beanName, typesToMatch);
        return targetType;
    }

    /**
     * Apply InstantiationAwareBeanPostProcessors to the specified bean definition
     * (by class and name), invoking their {@code postProcessBeforeInstantiation} methods.
     * <p>
     * Any returned object will be used as the bean instead of actually instantiating
     * the target bean. A {@code null} return value from the post-processor will
     * result in the target bean being instantiated.
     * 
     * @param beanClass
     *            the class of the bean to be instantiated
     * @param beanName
     *            the name of the bean
     * @return the bean object to use instead of a default instance of the target bean, or {@code null}
     * @see InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
     */
    protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
            throws BeansException {

        Object result = existingBean;
        for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
            result = beanProcessor.postProcessAfterInitialization(result, beanName);
            if (result == null) {
                return result;
            }
        }
        return result;
    }

}
