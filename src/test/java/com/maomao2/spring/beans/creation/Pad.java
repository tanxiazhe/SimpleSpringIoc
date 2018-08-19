package com.maomao2.spring.beans.creation;

public class Pad {

  private double price;

  public Pad() {
    System.out.println("default constuctor");
    this.price = 22;
  }

  public Pad(double price) {
    super();
    this.price = price;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

}
