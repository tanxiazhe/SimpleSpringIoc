package com.maomao2.spring.beans.creation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;

import com.maomao2.spring.beans.definition.BeanDefinition;
import com.maomao2.spring.beans.definition.PropertyValue;
import com.maomao2.spring.beans.definition.PropertyValues;
import com.maomao2.spring.beans.definition.RootBeanDefinition;
import com.maomao2.spring.beans.parsing.BeanDefinitionValueResolver;
import com.maomao2.spring.exception.BeanCreationException;
import com.maomao2.spring.exception.BeanInstantiationException;
import com.maomao2.spring.exception.BeansException;
import com.maomao2.spring.exception.CannotLoadBeanClassException;
import com.maomao2.spring.util.ClassUtils;
import com.maomao2.spring.util.ReflectionUtils;

public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry {

  Logger logger = Logger.getLogger(getClass());
  /**
   * ClassLoader to resolve bean class names with, if necessary
   */
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  /**
   * BeanPostProcessors to apply in createBean
   */
  private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();
  /**
   * Map from bean name to merged RootBeanDefinition
   */
  private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<String, RootBeanDefinition>(
      256);
  /**
   * Map between dependent bean names: bean name --> Set of dependent bean names
   */
  private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

  /**
   * Map between depending bean names: bean name --> Set of bean names for the bean's dependencies
   */
  private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

  /**
   * Map from scope identifier String to corresponding Scope
   */
  private final Map<String, Scope> scopes = new LinkedHashMap<String, Scope>(8);

  /**
   * Return the list of BeanPostProcessors that will get applied to beans created with this factory.
   */
  public List<BeanPostProcessor> getBeanPostProcessors() {
    return this.beanPostProcessors;
  }

  public void setBeanClassLoader(ClassLoader beanClassLoader) {
    this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
  }

  public ClassLoader getBeanClassLoader() {
    return this.beanClassLoader;
  }

  protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

  public Object getBean(String name) throws BeansException {
    return doGetBean(name, null, (Object[]) null);
  }

  public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    return doGetBean(name, requiredType, (Object[]) null);
  }

  /**
   * Return an instance, which may be shared or independent, of the specified bean.
   *
   * @param name the name of the bean to retrieve
   * @param requiredType the required type of the bean to retrieve
   * @param args arguments to use when creating a bean instance using explicit arguments (only applied when creating a
   * new instance as opposed to retrieving an existing one)
   * @return an instance of the bean
   * @throws BeansException if the bean could not be created
   */
  protected <T> T doGetBean(
      final String name, final Class<T> requiredType, final Object[] args) {
    Object bean = null;
    // return the actual bean name
    final String beanName = transformedBeanName(name);

    // Eagerly check singleton cache for manually registered singletons.
    Object sharedInstance = getSingleton(beanName);
    // check exists bean
    if (sharedInstance != null && args == null) {
      bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    } else {
      try {
        final RootBeanDefinition mbd = (RootBeanDefinition) getBeanDefinition(beanName);

        // TODO
        // Guarantee initialization of beans that the current bean depends on.

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
          // TODO
          /*
           * // It's a prototype -> create a new instance.
           * Object prototypeInstance = createBean(beanName, mbd, args);
           * bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
           */
        }
        // other scope
        else {
          // TODO
          /*
           * String scopeName = mbd.getScope();
           * final Scope scope = this.scopes.get(scopeName);
           * if (scope == null) {
           * throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
           * }
           * try {
           * Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {
           *
           * public Object getObject() throws BeansException {
           * return createBean(beanName, mbd, args);
           * }
           * });
           * bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
           * } catch (IllegalStateException ex) {
           * throw new BeanCreationException(beanName,
           * "Scope '" + scopeName + "' is not active for the current thread; consider " +
           * "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
           * ex);
           * }
           */
        }
      } catch (BeansException ex) {
        throw ex;
      }
    }

    // Check if required type matches the type of the actual bean instance.
    if (requiredType != null && bean != null && !requiredType.isAssignableFrom(bean.getClass())) {
      return (T) ConvertUtils.convert(bean, requiredType);
    }
    return (T) bean;
  }

  private String transformedBeanName(String name) {
    return name;
  }

  /**
   * Get the object for the given bean instance, either the bean instance itself or its created object in case of a
   * FactoryBean.
   *
   * @param beanInstance the shared bean instance
   * @param name name that may include factory dereference prefix
   * @param beanName the canonical bean name
   * @param mbd the merged bean definition
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
   * Register a dependent bean for the given bean, to be destroyed before the given bean is destroyed.
   *
   * @param beanName the name of the bean
   * @param dependentBeanName the name of the dependent bean
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

    logger.info("Creating instance of bean '" + beanName + "'");

    RootBeanDefinition mbdToUse = mbd;

    // Make sure bean class is actually resolved at this point, and
    // clone the bean definition in case of a dynamically resolved Class
    // which cannot be stored in the shared merged bean definition.
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
    if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
      mbdToUse = new RootBeanDefinition(mbd);
      mbdToUse.setBeanClass(resolvedClass);
    }

    // TODO
    /*
     * try {
     * // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
     * Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
     * if (bean != null) {
     * return bean;
     * }
     * } catch (Throwable ex) {
     * throw new BeanCreationException(beanName,
     * "BeanPostProcessor before instantiation of bean failed", ex);
     * }
     */

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
   * Resolve the bean class for the specified bean definition, resolving a bean class name into a Class reference (if
   * necessary) and storing the resolved Class in the bean definition for further use.
   *
   * @param mbd the merged bean definition to determine the class for
   * @param beanName the name of the bean (for error handling purposes)
   * @param typesToMatch the types to match in case of internal type matching purposes (also signals that the returned
   * {@code Class} will never be exposed to application code)
   * @return the resolved bean class (or {@code null} if none)
   * @throws CannotLoadBeanClassException if we failed to load the class
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
   * Actually create the specified bean. Pre-creation processing has already happened at this point, e.g. checking
   * {@code postProcessBeforeInstantiation} callbacks.
   * <p>
   * Differentiates between default bean instantiation, use of a factory method, and autowiring a constructor.
   *
   * @param beanName the name of the bean
   * @param mbd the merged bean definition for the bean
   * @param args explicit arguments to use for constructor or factory method invocation
   * @return a new instance of the bean
   * @throws BeanCreationException if the bean could not be created
   * @see #instantiateBean
   * @see #instantiateUsingFactoryMethod
   * @see #autowireConstructor
   */
  protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
      throws BeanCreationException {

    // Instantiate the bean.
    final Object bean = createBeanInstance(beanName, mbd, args);

    // Initialize the bean instance.
    Object exposedObject = bean;
    try {
      populateBean(exposedObject, beanName, mbd);
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

    return exposedObject;
  }

  /**
   * Populate the bean instance in the given BeanWrapper with the property values from the bean definition.
   *
   * @param exposedObject the bean instance
   * @param beanName the name of the bean
   * @param mbd the bean definition for the bean
   */
  protected void populateBean(Object exposedObject, String beanName, RootBeanDefinition mbd) {
    PropertyValues pvs = mbd.getPropertyValues();

    if (exposedObject == null) {
      if (!pvs.isEmpty()) {
        throw new BeanCreationException(
            beanName, "Cannot apply property values to null instance");
      } else {
        return;
      }

    }

    applyPropertyValues(exposedObject, beanName, pvs, mbd);
  }

  /**
   * Apply the given property values, resolving any runtime references to other beans in this bean factory. Must use
   * deep copy, so we don't permanently modify this property.
   *
   * @param bean the bean
   * @param pvs the new property values
   */
  protected void applyPropertyValues(Object bean, String beanName, PropertyValues pvs, RootBeanDefinition mbd) {
    if (pvs == null || pvs.isEmpty()) {
      return;
    }
    BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd);

    List<PropertyValue> original = Arrays.asList(pvs.getPropertyValues());

    for (PropertyValue pv : original) {

      String propertyName = pv.getName();
      Object originalValue = pv.getValue();
      Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
      Field field = null;
      try {
        field = bean.getClass().getDeclaredField(propertyName);
        field.setAccessible(true);
        field.set(bean, resolvedValue);
      } catch (NoSuchFieldException e) {
        e.printStackTrace();
      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

  }

  /**
   * Initialize the given bean instance, applying factory callbacks as well as init methods and bean post processors.
   * <p>
   * Called from {@link #createBean} for traditionally defined beans, and from {@link #initializeBean} for existing bean
   * instances.
   *
   * @param beanName the bean name in the factory (for debugging purposes)
   * @param bean the new bean instance we may need to initialize
   * @param mbd the bean definition that the bean was created with (can also be {@code null}, if given an existing bean
   * instance)
   * @return the initialized bean instance (potentially wrapped)
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
   * Give a bean a chance to react now all its properties are set, and a chance to know about its owning bean factory
   * (this object). This means checking whether the bean implements InitializingBean or defines a custom init method,
   * and invoking the necessary callback(s) if it does.
   *
   * @param beanName the bean name in the factory (for debugging purposes)
   * @param bean the new bean instance we may need to initialize
   * @param mbd the merged bean definition that the bean was created with (can also be {@code null}, if given an
   * existing bean instance)
   * @throws Throwable if thrown by init methods or by the invocation process
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
   * Invoke the specified custom init method on the given bean. Called by invokeInitMethods.
   * <p>
   * Can be overridden in subclasses for custom resolution of init methods with arguments.
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
        public Object run() throws Exception {
          ReflectionUtils.makeAccessible(initMethod);
          return null;
        }
      });
      try {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
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
   * Create a new instance for the specified bean, using an appropriate instantiation strategy: factory method,
   * constructor autowiring, or simple instantiation.
   *
   * @param beanName the name of the bean
   * @param mbd the bean definition for the bean
   * @param args explicit arguments to use for constructor or factory method invocation
   * @return BeanWrapper for the new instance
   * @see #instantiateUsingFactoryMethod
   * @see #autowireConstructor
   * @see #instantiateBean
   */
  protected Object createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
    // Make sure bean class is actually resolved at this point.
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

    if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers())) {
      throw new BeanCreationException(beanName,
          "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
    }

    if (mbd.getFactoryMethodName() != null) {
      return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    // TODO
    if (args != null) {
      Constructor<?>[] ctors = null;
      return autowireConstructor(beanName, mbd, ctors, args);
    }

    // No special handling: simply use no-arg constructor.
    return instantiateBean(beanName, mbd);
  }

  public Object instantiateUsingFactoryMethod(
      final String beanName, final RootBeanDefinition mbd, final Object[] explicitArgs) {
    try {
      Object beanInstance;
      Method factoryMethodToUse = null;
      Object factoryBean = null;
      Object[] argsToUse = null;
      beanInstance = this.instantiate(
          mbd, beanName, this, factoryBean, factoryMethodToUse, argsToUse);
      return beanInstance;
    } catch (Throwable ex) {
      throw new BeanCreationException(beanName,
          "Bean instantiation via factory method failed", ex);
    }
  }

  private Object instantiate(RootBeanDefinition mbd, String beanName, AbstractBeanFactory abstractBeanFactory,
      Object factoryBean, Method factoryMethod, Object[] args) {
    try {
      return factoryMethod.invoke(factoryBean, args);
    } catch (IllegalArgumentException ex) {
      throw new BeanInstantiationException(factoryMethod,
          "Illegal arguments to factory method '" + factoryMethod.getName() + "'; ", ex);
    } catch (IllegalAccessException ex) {
      throw new BeanInstantiationException(factoryMethod,
          "Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
    } catch (InvocationTargetException ex) {
      String msg = "Factory method '" + factoryMethod.getName() + "' threw exception";
      throw new BeanInstantiationException(factoryMethod, msg, ex.getTargetException());
    }
  }

  private Object autowireConstructor(final String beanName, final RootBeanDefinition mbd,
      Constructor<?>[] chosenCtors, final Object[] explicitArgs) {

    Constructor<?> constructorToUse = null;
    Object[] argsToUse = null;

    try {
      Object beanInstance;
      beanInstance = instantiate(
          mbd, beanName, this, constructorToUse, argsToUse);

      return beanInstance;
    } catch (Throwable ex) {
      throw new BeanCreationException(beanName,
          "Bean instantiation via constructor failed", ex);
    }
  }

  private Object instantiate(RootBeanDefinition mbd, String beanName, AbstractBeanFactory abstractBeanFactory,
      Constructor<?> ctor, Object[] args) {
    try {
      ReflectionUtils.makeAccessible(ctor);
      return ctor.newInstance(args);
    } catch (InstantiationException ex) {
      throw new BeanInstantiationException(ctor.getDeclaringClass(),
          "Is it an abstract class?", ex);
    } catch (IllegalAccessException ex) {
      throw new BeanInstantiationException(ctor.getDeclaringClass(),
          "Is the constructor accessible?", ex);
    } catch (IllegalArgumentException ex) {
      throw new BeanInstantiationException(ctor.getDeclaringClass(),
          "Illegal arguments for constructor", ex);
    } catch (InvocationTargetException ex) {
      throw new BeanInstantiationException(ctor.getDeclaringClass(),
          "Constructor threw exception", ex.getTargetException());
    }
  }

  /**
   * Instantiate the given bean using its default constructor.
   *
   * @param beanName the name of the bean
   * @param mbd the bean definition for the bean
   * @return BeanWrapper for the new instance
   */
  protected Object instantiateBean(final String beanName, final RootBeanDefinition mbd) {
    final Class<?> clazz = mbd.getBeanClass();
    Constructor<?> constructorToUse = null;
    try {
      constructorToUse = clazz.getDeclaredConstructor((Class[]) null);

      return constructorToUse.newInstance(null);
    } catch (Exception ex) {
      throw new BeanInstantiationException(clazz, "No default constructor found", ex);
    }

  }

  /**
   * Apply before-instantiation post-processors, resolving whether there is a before-instantiation shortcut for the
   * specified bean.
   *
   * @param beanName the name of the bean
   * @param mbd the bean definition for the bean
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
   * @param beanName the name of the bean (for error handling purposes)
   * @param mbd the merged bean definition for the bean
   * @param typesToMatch the types to match in case of internal type matching purposes (also signals that the returned
   * {@code Class} will never be exposed to application code)
   * @return the type for the bean if determinable, or {@code null} otherwise
   */
  protected Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
    Class<?> targetType = resolveBeanClass(mbd, beanName, typesToMatch);
    return targetType;
  }

  /**
   * Apply InstantiationAwareBeanPostProcessors to the specified bean definition (by class and name), invoking their
   * {@code postProcessBeforeInstantiation} methods.
   * <p>
   * Any returned object will be used as the bean instead of actually instantiating the target bean. A {@code null}
   * return value from the post-processor will result in the target bean being instantiated.
   *
   * @param beanClass the class of the bean to be instantiated
   * @param beanName the name of the bean
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
