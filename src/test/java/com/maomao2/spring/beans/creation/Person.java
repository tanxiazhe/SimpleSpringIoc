package com.maomao2.spring.beans.creation;

public class Person {

  private String name;
  private String address;
  private int phone;

  public Person() {
  }

  public Person(String name, String address, int phone) {
    super();
    this.name = name;
    this.address = address;
    this.phone = phone;
  }

  @Override
  public String toString() {
    return "Person [name=" + name + ", address=" + address + ", phone=" + phone + "]";
  }
}
