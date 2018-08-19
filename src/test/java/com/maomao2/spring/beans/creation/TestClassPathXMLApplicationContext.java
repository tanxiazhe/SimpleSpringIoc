package com.maomao2.spring.beans.creation;

import static org.junit.Assert.assertEquals;

import com.maomao2.spring.context.ClassPathXMLApplicationContext;
import org.junit.Test;

public class TestClassPathXMLApplicationContext {

  @Test
  public void test_constructorInject() {
    ClassPathXMLApplicationContext applicationContext = new ClassPathXMLApplicationContext("applicationContext.xml");

    Pad p1 = applicationContext.getBean("pad1", Pad.class);
    assertEquals(p1.getPrice(),1999.9,0.1);
  }

  @Test
  public void test_constructorInject_lazyInit() {
    ClassPathXMLApplicationContext applicationContext = new ClassPathXMLApplicationContext("applicationContext.xml");

    Pad p2 = applicationContext.getBean("pad2", Pad.class);
    assertEquals(p2.getPrice(),22,0.1);
  }

  @Test
  public void test_propertyInject() {
    ClassPathXMLApplicationContext applicationContext = new ClassPathXMLApplicationContext("applicationContext.xml");

    Person person = applicationContext.getBean("person", Person.class);
    System.out.println(person);

  }

  @Test
  public void test_constructorInject_ref() {
    ClassPathXMLApplicationContext applicationContext = new ClassPathXMLApplicationContext("applicationContext.xml");

    Person person2 = applicationContext.getBean("person2", Person.class);
    assertEquals(person2.getPad().getPrice(),1999.9,0.1);
  }


  @Test
  public void test_propertyInject_ref() {
    ClassPathXMLApplicationContext applicationContext = new ClassPathXMLApplicationContext("applicationContext.xml");

    Person person3 = applicationContext.getBean("person3", Person.class);
    assertEquals(person3.getPad().getPrice(),22,0.1);
  }





}
