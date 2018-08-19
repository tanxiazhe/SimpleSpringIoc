package com.maomao2.spring.beans.definition;

import com.sun.corba.se.impl.io.TypeMismatchException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.maomao2.spring.beans.creation.AbstractBeanFactory;
import com.maomao2.spring.beans.creation.MethodParameter;
import com.maomao2.spring.beans.definition.ConstructorArgumentValues.ValueHolder;
import com.maomao2.spring.beans.parsing.BeanDefinitionValueResolver;
import com.maomao2.spring.exception.BeanCreationException;
import com.maomao2.spring.exception.BeanInstantiationException;
import com.maomao2.spring.exception.BeansException;
import com.maomao2.spring.util.MethodInvoker;
import com.maomao2.spring.util.ReflectionUtils;
import org.apache.commons.beanutils.ConvertUtils;

public class ConstructorResolver {

  private final AbstractBeanFactory beanFactory;

  /**
   * Create a new ConstructorResolver for the given factory and instantiation strategy.
   */
  public ConstructorResolver(AbstractBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  public Object autowireConstructor(String beanName, RootBeanDefinition mbd, Object[] explicitArgs) {
    Constructor<?> constructorToUse = null;
    Object[] argsToUse = null;
    ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
    ConstructorArgumentValues resolvedValues = new ConstructorArgumentValues();
    int minNrOfArgs = resolveConstructorArguments(beanName, mbd, cargs, resolvedValues);

    // Take specified constructors, if any.
    Constructor<?>[] candidates = null;
    Class<?> beanClass = mbd.getBeanClass();
    try {
      candidates = beanClass.getConstructors();
    } catch (Throwable ex) {
      throw new BeanCreationException("Resolution of declared constructors on bean Class [" + beanClass.getName()
          + "] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
    }

    int minTypeDiffWeight = Integer.MAX_VALUE;
    Set<Constructor<?>> ambiguousConstructors = null;

    for (Constructor<?> candidate : candidates) {

      Class<?>[] paramTypes = candidate.getParameterTypes();

      if (constructorToUse != null && argsToUse.length > paramTypes.length) {
        // Already found greedy constructor that can be satisfied ->
        // do not look any further, there are only less greedy
        // constructors left.
        break;
      }
      if (paramTypes.length < minNrOfArgs) {
        continue;
      }

      String[] paramNames = getParameterNames(candidate);
      ArgumentsHolder argsHolder;
      argsHolder = createArgumentArray(beanName, mbd, resolvedValues, paramTypes, paramNames, candidate);

      int typeDiffWeight = argsHolder.getTypeDifferenceWeight(paramTypes);
      // Choose this constructor if it represents the closest match.
      if (typeDiffWeight < minTypeDiffWeight) {
        constructorToUse = candidate;
        argsToUse = argsHolder.arguments;

      }

      if (constructorToUse == null) {
        throw new BeanCreationException("无法找到符合条件的构造函数"
            + "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
      }
    }

    try {
      Object beanInstance;
      beanInstance = instantiate(
          mbd, beanName, this.beanFactory, constructorToUse, argsToUse);

      return beanInstance;
    } catch (Throwable ex) {
      throw new BeanCreationException(beanName,
          "Bean instantiation via constructor failed", ex);
    }
  }


  public String[] getParameterNames(Constructor<?> ctor) {
    Parameter[] parameters = ctor.getParameters();
    String[] parameterNames = new String[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      Parameter param = parameters[i];
      if (!param.isNamePresent()) {
        return null;
      }
      parameterNames[i] = param.getName();
    }
    return parameterNames;
  }

  private ArgumentsHolder createArgumentArray(String beanName, RootBeanDefinition mbd,
      ConstructorArgumentValues resolvedValues, Class<?>[] paramTypes, String[] paramNames,
      Constructor<?> candidate) {

    ArgumentsHolder args = new ArgumentsHolder(paramTypes.length);
    Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<ValueHolder>(paramTypes.length);
    Set<String> autowiredBeanNames = new LinkedHashSet<String>(4);

    for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
      Class<?> paramType = paramTypes[paramIndex];
      String paramName = (paramNames != null ? paramNames[paramIndex] : "");
      // Try to find matching constructor argument value, either indexed or generic.
      ConstructorArgumentValues.ValueHolder valueHolder = resolvedValues.getArgumentValue(paramIndex, paramType,
          paramName, usedValueHolders);
      // If we couldn't find a direct match and are not supposed to autowire,
      // let's try the next generic, untyped argument value as fallback:
      // it could match after type conversion (for example, String -> int).
      if (valueHolder == null && (paramTypes.length == resolvedValues.getArgumentCount())) {
        valueHolder = resolvedValues.getGenericArgumentValue(null, null, usedValueHolders);
      }
      if (valueHolder != null) {
        // We found a potential match - let's give it a try.
        // Do not consider the same value definition multiple times!
        usedValueHolders.add(valueHolder);
        Object originalValue = valueHolder.getValue();
        Object convertedValue = ConvertUtils.convert(originalValue, paramType);

        args.arguments[paramIndex] = convertedValue;

        args.resolveNecessary = true;
        args.rawArguments[paramIndex] = originalValue;
      }
    }

    return args;
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
   * Resolve the constructor arguments for this bean into the resolvedValues object. This may involve looking up other
   * beans.
   * <p>
   * This method is also used for handling invocations of static factory methods.
   */
  private int resolveConstructorArguments(String beanName, RootBeanDefinition mbd,
      ConstructorArgumentValues cargs, ConstructorArgumentValues resolvedValues) {

    BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd);

    int minNrOfArgs = cargs.getArgumentCount();

    for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : cargs.getIndexedArgumentValues()
        .entrySet()) {
      int index = entry.getKey();
      if (index < 0) {
        throw new BeanCreationException(beanName,
            "Invalid constructor argument index: ");
      }
      if (index > minNrOfArgs) {
        minNrOfArgs = index + 1;
      }
      ConstructorArgumentValues.ValueHolder valueHolder = entry.getValue();

      Object resolvedValue = valueResolver.resolveValueIfNecessary("constructor argument",
          valueHolder.getValue());
      ConstructorArgumentValues.ValueHolder resolvedValueHolder = new ConstructorArgumentValues.ValueHolder(
          resolvedValue, valueHolder.getType(), valueHolder.getName());

      resolvedValues.addIndexedArgumentValue(index, resolvedValueHolder);
    }

    for (ConstructorArgumentValues.ValueHolder valueHolder : cargs.getGenericArgumentValues()) {

      Object resolvedValue = valueResolver.resolveValueIfNecessary("constructor argument",
          valueHolder.getValue());
      ConstructorArgumentValues.ValueHolder resolvedValueHolder = new ConstructorArgumentValues.ValueHolder(
          resolvedValue, valueHolder.getType(), valueHolder.getName());

      resolvedValues.addGenericArgumentValue(resolvedValueHolder);

    }

    return minNrOfArgs;
  }

  /**
   * Instantiate the given bean using its default constructor.
   *
   * @param beanName the name of the bean
   * @param mbd the bean definition for the bean
   * @return BeanWrapper for the new instance
   */
  public Object instantiateBeanUsingNoArgs(final String beanName, final RootBeanDefinition mbd) {
    final Class<?> clazz = mbd.getBeanClass();
    Constructor<?> constructorToUse = null;
    try {
      constructorToUse = clazz.getDeclaredConstructor((Class[]) null);

      return constructorToUse.newInstance(null);
    } catch (Exception ex) {
      throw new BeanInstantiationException(clazz, "No default constructor found", ex);
    }

  }

  public Object instantiateUsingFactoryMethod(
      final String beanName, final RootBeanDefinition mbd, final Object[] explicitArgs) {
    try {
      Object beanInstance;
      Method factoryMethodToUse = null;
      Object factoryBean = null;
      Object[] argsToUse = null;
      beanInstance = this.instantiate(
          mbd, beanName, this.beanFactory, factoryBean, factoryMethodToUse, argsToUse);
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

  /**
   * 内部类，用于保存参数组合
   */
  private static class ArgumentsHolder {

    public final Object rawArguments[];

    public final Object arguments[];

    public final Object preparedArguments[];

    public boolean resolveNecessary = false;

    public ArgumentsHolder(int size) {
      this.rawArguments = new Object[size];
      this.arguments = new Object[size];
      this.preparedArguments = new Object[size];
    }

    public int getTypeDifferenceWeight(Class<?>[] paramTypes) {
      // If valid arguments found, determine type difference weight.
      // Try type difference weight on both the converted arguments and
      // the raw arguments. If the raw weight is better, use it.
      // Decrease raw weight by 1024 to prefer it over equal converted weight.
      int typeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.arguments);
      int rawTypeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.rawArguments) - 1024;
      return (rawTypeDiffWeight < typeDiffWeight ? rawTypeDiffWeight : typeDiffWeight);
    }

  }
}
