package com.maomao2.spring.exception;

import java.io.IOException;

public class ApplicationContextException extends BeansException {

  public ApplicationContextException(String msg, IOException ex) {
    super(msg, ex);
  }
}
