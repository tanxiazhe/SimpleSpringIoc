package com.maomao2.spring.beans.creation;

public class TestDefaultBeanFactory {

  public static void main(String[] args) {
    System.out.println("现在开始初始化容器");
    BeanFactory factory = new DefaultBeanFactory("applicationContext.xml");
    System.out.println("容器初始化成功");
    // 得到Person，并使用
    Person person = factory.getBean("person", Person.class);
    System.out.println(person);

    Pad p1 = factory.getBean("pad1", Pad.class);
    System.out.println(p1.getPrice());
  }
}
