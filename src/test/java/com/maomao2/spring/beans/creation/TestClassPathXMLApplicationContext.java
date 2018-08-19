package com.maomao2.spring.beans.creation;

import com.maomao2.spring.context.ClassPathXMLApplicationContext;

public class TestClassPathXMLApplicationContext {

  public static void main(String[] args) {
    ClassPathXMLApplicationContext applicationContext = new ClassPathXMLApplicationContext("applicationContext.xml");

    Person person = applicationContext.getBean("person", Person.class);
    System.out.println(person);

    Pad p1 = applicationContext.getBean("pad1", Pad.class);
    System.out.println(p1.getPrice());

    Pad p2 = applicationContext.getBean("pad2", Pad.class);
    System.out.println(p2.getPrice());

  }
}
