package com.maomao2.spring.beans.creation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.maomao2.spring.beans.definition.BeanDefinition;
import com.maomao2.spring.beans.parsing.XmlBeanDefinitionReader;
import com.maomao2.spring.exception.BeanDefinitionStoreException;
import com.maomao2.spring.exception.BeansException;
import com.maomao2.spring.exception.NoSuchBeanDefinitionException;
import com.maomao2.spring.util.StringUtils;

public class DefaultBeanFactory extends AbstractBeanFactory implements ConfigureBeanFactory, BeanDefinitionRegistry {

  Logger logger = Logger.getLogger(getClass());

  /**
   * Map of bean definition objects, keyed by bean name
   */
  private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>(256);

  /**
   * List of bean definition names, in registration order
   */
  private volatile List<String> beanDefinitionNames = new ArrayList<String>(256);


  @Override
  public Object getBean(String name) throws BeansException {
    return super.getBean(name, null);
  }

  @Override
  public <T> T getBean(Class<T> requiredType) throws BeansException {
    return super.getBean(null, requiredType);
  }

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
      throws BeanDefinitionStoreException {
    BeanDefinition oldBeanDefinition = this.beanDefinitionMap.get(beanName);
    if (oldBeanDefinition != null) {
      if (!oldBeanDefinition.equals(beanDefinition)) {
        this.logger.error("Overriding bean definition for bean '" + beanName +
            "' with a different definition: replacing [" + oldBeanDefinition +
            "] with [" + beanDefinition + "]");
      }
    }

    this.beanDefinitionMap.put(beanName, beanDefinition);
    this.beanDefinitionNames.add(beanName);
  }

  @Override
  public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
    this.beanDefinitionMap.remove(beanName);
  }

  public boolean containsBeanDefinition(String beanName) {
    return this.beanDefinitionMap.containsKey(beanName);
  }

  public String[] getBeanDefinitionNames() {

    return StringUtils.toStringArray(this.beanDefinitionNames);
  }

  public int getBeanDefinitionCount() {
    return this.beanDefinitionMap.size();
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
    BeanDefinition bd = this.beanDefinitionMap.get(beanName);
    if (bd == null) {
      if (this.logger.isTraceEnabled()) {
        this.logger.trace("No bean named '" + beanName + "' found in " + this);
      }
      throw new NoSuchBeanDefinitionException(beanName);
    }
    return bd;
  }
}
