package com.maomao2.spring.beans.definition;

import com.maomao2.spring.beans.creation.ConfigureBeanFactory;
import com.maomao2.spring.util.ClassUtils;

public abstract class AbstractBeanDefinition implements BeanDefinition {

  /**
   * Constant for the default scope name: {@code ""}, equivalent to singleton status unless overridden from a parent
   * bean definition (if applicable).
   */
  public static final String SCOPE_DEFAULT = "";

  /**
   * Constant that indicates no autowiring at all.
   *
   * @see #setAutowireMode
   */
  public static final int AUTOWIRE_NO = ConfigureBeanFactory.AUTOWIRE_NO;

  /**
   * Constant that indicates autowiring bean properties by name.
   *
   * @see #setAutowireMode
   */
  public static final int AUTOWIRE_BY_NAME = ConfigureBeanFactory.AUTOWIRE_BY_NAME;

  /**
   * Constant that indicates autowiring bean properties by type.
   *
   * @see #setAutowireMode
   */
  public static final int AUTOWIRE_BY_TYPE = ConfigureBeanFactory.AUTOWIRE_BY_TYPE;

  /**
   * Constant that indicates autowiring a constructor.
   *
   * @see #setAutowireMode
   */
  public static final int AUTOWIRE_CONSTRUCTOR = ConfigureBeanFactory.AUTOWIRE_CONSTRUCTOR;

  /**
   * Constant that indicates no dependency check at all.
   *
   * @see #setDependencyCheck
   */
  public static final int DEPENDENCY_CHECK_NONE = 0;

  /**
   * Constant that indicates dependency checking for object references.
   *
   * @see #setDependencyCheck
   */
  public static final int DEPENDENCY_CHECK_OBJECTS = 1;

  /**
   * Constant that indicates dependency checking for "simple" properties.
   *
   * @see #setDependencyCheck
   * @see org.springframework.beans.BeanUtils#isSimpleProperty
   */
  public static final int DEPENDENCY_CHECK_SIMPLE = 2;

  /**
   * Constant that indicates dependency checking for all properties (object references as well as "simple" properties).
   *
   * @see #setDependencyCheck
   */
  public static final int DEPENDENCY_CHECK_ALL = 3;

  private volatile Object beanClass;

  private String scope = SCOPE_DEFAULT;

  private boolean lazyInit = false;

  private int autowireMode = AUTOWIRE_NO;

  private int dependencyCheck = DEPENDENCY_CHECK_NONE;

  private String[] dependsOn;

  private boolean autowireCandidate = true;

  private ConstructorArgumentValues constructorArgumentValues;

  private MutablePropertyValues propertyValues;

  private String factoryBeanName;

  private String factoryMethodName;

  private String initMethodName;

  private String destroyMethodName;

  private boolean enforceInitMethod = true;

  /**
   * Create a new AbstractBeanDefinition with default settings.
   */
  protected AbstractBeanDefinition() {
    this(null, null);
  }

  /**
   * Create a new AbstractBeanDefinition as a deep copy of the given bean definition.
   *
   * @param original the original bean definition to copy from
   */
  protected AbstractBeanDefinition(BeanDefinition original) {
    setBeanClassName(original.getBeanClassName());
    setFactoryBeanName(original.getFactoryBeanName());
    setFactoryMethodName(original.getFactoryMethodName());
    setScope(original.getScope());
    setLazyInit(original.isLazyInit());

    if (original instanceof AbstractBeanDefinition) {
      AbstractBeanDefinition originalAbd = (AbstractBeanDefinition) original;
      if (originalAbd.hasBeanClass()) {
        setBeanClass(originalAbd.getBeanClass());
      }
      setAutowireMode(originalAbd.getAutowireMode());
      setDependencyCheck(originalAbd.getDependencyCheck());
      setDependsOn(originalAbd.getDependsOn());
      setAutowireCandidate(originalAbd.isAutowireCandidate());
      setInitMethodName(originalAbd.getInitMethodName());
      setEnforceInitMethod(originalAbd.isEnforceInitMethod());
      setDestroyMethodName(originalAbd.getDestroyMethodName());
    }
  }

  /**
   * Create a new AbstractBeanDefinition with the given constructor argument values and property values.
   */
  protected AbstractBeanDefinition(ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
    setConstructorArgumentValues(cargs);
    setPropertyValues(pvs);
  }

  /**
   * Return whether this definition specifies a bean class.
   */
  public boolean hasBeanClass() {
    return (this.beanClass instanceof Class);
  }

  /**
   * Specify the class for this bean.
   */
  public void setBeanClass(Class<?> beanClass) {
    this.beanClass = beanClass;
  }

  /**
   * Return the class of the wrapped bean, if already resolved.
   *
   * @return the bean class, or {@code null} if none defined
   * @throws IllegalStateException if the bean definition does not define a bean class, or a specified bean class name
   * has not been resolved into an actual Class
   */
  public Class<?> getBeanClass() throws IllegalStateException {
    Object beanClassObject = this.beanClass;
    if (beanClassObject == null) {
      throw new IllegalStateException("No bean class specified on bean definition");
    }
    if (!(beanClassObject instanceof Class)) {
      throw new IllegalStateException(
          "Bean class name [" + beanClassObject + "] has not been resolved into an actual Class");
    }
    return (Class<?>) beanClassObject;
  }

  @Override
  public void setBeanClassName(String beanClassName) {
    this.beanClass = beanClassName;
  }

  @Override
  public String getBeanClassName() {
    Object beanClassObject = this.beanClass;
    if (beanClassObject instanceof Class) {
      return ((Class<?>) beanClassObject).getName();
    } else {
      return (String) beanClassObject;
    }
  }

  /**
   * Determine the class of the wrapped bean, resolving it from a specified class name if necessary. Will also reload a
   * specified Class from its name when called with the bean class already resolved.
   *
   * @param classLoader the ClassLoader to use for resolving a (potential) class name
   * @return the resolved bean class
   * @throws ClassNotFoundException if the class name could be resolved
   */
  public Class<?> resolveBeanClass(ClassLoader classLoader) throws ClassNotFoundException {
    String className = getBeanClassName();
    if (className == null) {
      return null;
    }
    Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
    this.beanClass = resolvedClass;
    return resolvedClass;
  }

  /**
   * Set the name of the target scope for the bean.
   * <p>
   * The default is singleton status, although this is only applied once a bean definition becomes active in the
   * containing factory. A bean definition may eventually inherit its scope from a parent bean definition. For this
   * reason, the default scope name is an empty string (i.e., {@code ""}), with singleton status being assumed until a
   * resolved scope is set.
   *
   * @see #SCOPE_SINGLETON
   * @see #SCOPE_PROTOTYPE
   */
  @Override
  public void setScope(String scope) {
    this.scope = scope;
  }

  /**
   * Return the name of the target scope for the bean.
   */
  @Override
  public String getScope() {
    return this.scope;
  }

  /**
   * Return whether this a <b>Singleton</b>, with a single shared instance returned from all calls.
   *
   * @see #SCOPE_SINGLETON
   */
  @Override
  public boolean isSingleton() {
    return SCOPE_SINGLETON.equals(scope) || SCOPE_DEFAULT.equals(scope);
  }

  /**
   * Return whether this a <b>Prototype</b>, with an independent instance returned for each call.
   *
   * @see #SCOPE_PROTOTYPE
   */
  @Override
  public boolean isPrototype() {
    return SCOPE_PROTOTYPE.equals(scope);
  }

  /**
   * Set whether this bean should be lazily initialized.
   * <p>
   * If {@code false}, the bean will get instantiated on startup by bean factories that perform eager initialization of
   * singletons.
   */
  @Override
  public void setLazyInit(boolean lazyInit) {
    this.lazyInit = lazyInit;
  }

  /**
   * Return whether this bean should be lazily initialized, i.e. not eagerly instantiated on startup. Only applicable to
   * a singleton bean.
   */
  @Override
  public boolean isLazyInit() {
    return this.lazyInit;
  }

  /**
   * Set the autowire mode. This determines whether any automagical detection and setting of bean references will
   * happen. Default is AUTOWIRE_NO, which means there's no autowire.
   *
   * @param autowireMode the autowire mode to set. Must be one of the constants defined in this class.
   * @see #AUTOWIRE_NO
   * @see #AUTOWIRE_BY_NAME
   * @see #AUTOWIRE_BY_TYPE
   * @see #AUTOWIRE_CONSTRUCTOR
   * @see #AUTOWIRE_AUTODETECT
   */
  public void setAutowireMode(int autowireMode) {
    this.autowireMode = autowireMode;
  }

  /**
   * Return the autowire mode as specified in the bean definition.
   */
  public int getAutowireMode() {
    return this.autowireMode;
  }

  /**
   * Set the dependency check code.
   *
   * @param dependencyCheck the code to set. Must be one of the four constants defined in this class.
   * @see #DEPENDENCY_CHECK_NONE
   * @see #DEPENDENCY_CHECK_OBJECTS
   * @see #DEPENDENCY_CHECK_SIMPLE
   * @see #DEPENDENCY_CHECK_ALL
   */
  public void setDependencyCheck(int dependencyCheck) {
    this.dependencyCheck = dependencyCheck;
  }

  /**
   * Return the dependency check code.
   */
  public int getDependencyCheck() {
    return this.dependencyCheck;
  }

  /**
   * Set the names of the beans that this bean depends on being initialized. The bean factory will guarantee that these
   * beans get initialized first.
   * <p>
   * Note that dependencies are normally expressed through bean properties or constructor arguments. This property
   * should just be necessary for other kinds of dependencies like statics (*ugh*) or database preparation on startup.
   */
  @Override
  public void setDependsOn(String... dependsOn) {
    this.dependsOn = dependsOn;
  }

  /**
   * Return the bean names that this bean depends on.
   */
  @Override
  public String[] getDependsOn() {
    return this.dependsOn;
  }

  /**
   * Set whether this bean is a candidate for getting autowired into some other bean.
   */
  @Override
  public void setAutowireCandidate(boolean autowireCandidate) {
    this.autowireCandidate = autowireCandidate;
  }

  /**
   * Return whether this bean is a candidate for getting autowired into some other bean.
   */
  @Override
  public boolean isAutowireCandidate() {
    return this.autowireCandidate;
  }

  /**
   * Specify constructor argument values for this bean.
   */
  public void setConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) {
    this.constructorArgumentValues = (constructorArgumentValues != null ? constructorArgumentValues
        : new ConstructorArgumentValues());
  }

  /**
   * Return constructor argument values for this bean (never {@code null}).
   */
  @Override
  public ConstructorArgumentValues getConstructorArgumentValues() {
    return this.constructorArgumentValues;
  }

  /**
   * Return if there are constructor argument values defined for this bean.
   */
  public boolean hasConstructorArgumentValues() {
    return !this.constructorArgumentValues.isEmpty();
  }

  /**
   * Specify property values for this bean, if any.
   */
  public void setPropertyValues(MutablePropertyValues propertyValues) {
    this.propertyValues = (propertyValues != null ? propertyValues : new MutablePropertyValues());
  }

  /**
   * Return property values for this bean (never {@code null}).
   */
  @Override
  public MutablePropertyValues getPropertyValues() {
    return this.propertyValues;
  }

  @Override
  public void setFactoryBeanName(String factoryBeanName) {
    this.factoryBeanName = factoryBeanName;
  }

  @Override
  public String getFactoryBeanName() {
    return this.factoryBeanName;
  }

  @Override
  public void setFactoryMethodName(String factoryMethodName) {
    this.factoryMethodName = factoryMethodName;
  }

  @Override
  public String getFactoryMethodName() {
    return this.factoryMethodName;
  }

  /**
   * Set the name of the initializer method. The default is {@code null} in which case there is no initializer method.
   */
  public void setInitMethodName(String initMethodName) {
    this.initMethodName = initMethodName;
  }

  /**
   * Return the name of the initializer method.
   */
  public String getInitMethodName() {
    return this.initMethodName;
  }

  /**
   * Specify whether or not the configured init method is the default. Default value is {@code false}.
   *
   * @see #setInitMethodName
   */
  public void setEnforceInitMethod(boolean enforceInitMethod) {
    this.enforceInitMethod = enforceInitMethod;
  }

  /**
   * Indicate whether the configured init method is the default.
   *
   * @see #getInitMethodName()
   */
  public boolean isEnforceInitMethod() {
    return this.enforceInitMethod;
  }

  /**
   * Set the name of the destroy method. The default is {@code null} in which case there is no destroy method.
   */
  public void setDestroyMethodName(String destroyMethodName) {
    this.destroyMethodName = destroyMethodName;
  }

  /**
   * Return the name of the destroy method.
   */
  public String getDestroyMethodName() {
    return this.destroyMethodName;
  }

  /**
   * Return the resolved autowire code, (resolving AUTOWIRE_AUTODETECT to AUTOWIRE_CONSTRUCTOR or AUTOWIRE_BY_TYPE).
   *
   * @see #AUTOWIRE_AUTODETECT
   * @see #AUTOWIRE_CONSTRUCTOR
   * @see #AUTOWIRE_BY_TYPE
   */
  public int getResolvedAutowireMode() {

    return this.autowireMode;
  }
}
