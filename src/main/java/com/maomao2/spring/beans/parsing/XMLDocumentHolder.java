package com.maomao2.spring.beans.parsing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLDocumentHolder implements DocumentHolder {

    // 建立一个HashMap用来存放字符串和文档
    private Map<String, Document> docs = new HashMap<String, Document>();

    public Document loadDocument(String filePath) {

        Document doc = this.docs.get(filePath);// 用HashMap先根据路径获取文档

        if (doc == null) {

            this.docs.put(filePath, doLoadDocument(filePath)); // 如果为空，把路径和文档放进去

        }

        return this.docs.get(filePath);
    }

    private Document doLoadDocument(String filePath) {
        // （1）创建DocumentBuilderFactory对象

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            // （2）创建DocumentBuilder对象

            DocumentBuilder db = dbf.newDocumentBuilder();

            // （3）通过DocumentBuilder对象的parse方法加载book.xml

            Document document = db.parse(filePath);
            return document;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;

    }

}