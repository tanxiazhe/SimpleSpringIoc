package com.maomao2.spring.beans.parsing;

import org.w3c.dom.Document;

public interface DocumentHolder {

  Document loadDocument(String filePath);

}