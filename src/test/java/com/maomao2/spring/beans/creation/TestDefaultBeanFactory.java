package com.maomao2.spring.beans.creation;

import static org.junit.Assert.assertEquals;

import com.maomao2.spring.beans.definition.BeanDefinition;
import com.maomao2.spring.beans.definition.ConstructorArgumentValues;
import com.maomao2.spring.beans.definition.PropertyValue;
import com.maomao2.spring.beans.definition.PropertyValues;
import com.maomao2.spring.beans.definition.RootBeanDefinition;
import org.junit.Test;

public class TestDefaultBeanFactory {
  @Test
  public void testIoC() {
    // 1. 创建beanFactory
    DefaultBeanFactory beanFactory = new DefaultBeanFactory();

    // 2. 注册bean
    BeanDefinition bd = new RootBeanDefinition("com.maomao2.spring.beans.creation.Person");

    BeanDefinitionHolder bdh = new BeanDefinitionHolder(bd,"helloWorld");
    beanFactory.registerBeanDefinition(bdh.getBeanName(), bdh.getBeanDefinition());

    // 3. 获取bean
    Person hello = (Person) beanFactory.getBean("helloWorld");
    assertEquals("Hello Spring Ioc!", hello.sayHello("Hello Spring Ioc!"));
  }

  @Test
  public void testIocProperty_constructorArgs() {
    // 1. 创建beanFactory
    DefaultBeanFactory beanFactory = new DefaultBeanFactory();

    // 2. 注册bean
    BeanDefinition bd = new RootBeanDefinition("com.maomao2.spring.beans.creation.Person");

    // 注入Property
    ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
    constructorArgumentValues.addIndexedArgumentValue(0,"zhangsan");
    constructorArgumentValues.addIndexedArgumentValue(1,"huaguoshan");
    constructorArgumentValues.addIndexedArgumentValue(2,12222222222L);
    ((RootBeanDefinition) bd).setConstructorArgumentValues(constructorArgumentValues);

    BeanDefinitionHolder bdh = new BeanDefinitionHolder( bd,"person");
    beanFactory.registerBeanDefinition(bdh.getBeanName(), bdh.getBeanDefinition());

    // 3. 获取bean
    Person person = (Person) beanFactory.getBean("person");
    Person p = new Person("zhangsan","huaguoshan",12222222222L);
    assertEquals(p.toString(), person.toString());
  }


  @Test
  public void testIocProperty_propertyArgs() {
    // 1. 创建beanFactory
    DefaultBeanFactory beanFactory = new DefaultBeanFactory();

    // 2. 注册bean
    BeanDefinition bd = new RootBeanDefinition("com.maomao2.spring.beans.creation.Pad");

    // 注入Property

    PropertyValue pv = new PropertyValue("price", 67.9);

    bd.getPropertyValues().addPropertyValue(pv);

    BeanDefinitionHolder bdh = new BeanDefinitionHolder( bd,"pad");
    beanFactory.registerBeanDefinition(bdh.getBeanName(), bdh.getBeanDefinition());

    // 3. 获取bean
    Pad pad = (Pad) beanFactory.getBean("pad");
    assertEquals(67.9,pad.getPrice(),0.1);
  }
}
