package com.maomao2.spring.beans.creation;

public class Person {

  private String name;
  private String address;
  private long phone;

  public Person(String name, Pad pad) {
    this.name = name;
    this.pad = pad;
  }

  private Pad pad;


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public long getPhone() {
    return phone;
  }

  public void setPhone(long phone) {
    this.phone = phone;
  }

  public Pad getPad() {
    return pad;
  }

  public void setPad(Pad pad) {
    this.pad = pad;
  }

  public Person() {
  }

  public Person(String name, String address, long phone) {
    super();
    this.name = name;
    this.address = address;
    this.phone = phone;
  }

  public String sayHello(String msg) {
    return (msg);
  }

  @Override
  public String toString() {
    return "Person [name=" + name + ", address=" + address + ", phone=" + phone + "]";
  }
}
