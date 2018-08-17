
package com.maomao2.spring.beans.parsing;

import org.w3c.dom.Document;


public interface BeanDefinitionDocumentReader {


  void registerBeanDefinitions(Document doc);
}
