package com.maomao2.spring.beans.parsing;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ApplicationContextHandler extends DefaultHandler {
    private static Map<String, Object> container = new HashMap<String, Object>();

    public static Map<String, Object> getContainer() {
        return container;
    }

    public static void setContainer(Map<String, Object> container) {
        ApplicationContextHandler.container = container;
    }

    public Object getBean(String id) {
        return container.get(id);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        // Using qualified name because we are not using xmlns prefixes here.
        if (qName.equals("bean")) {
            String id = null;
            Object clazz = null;

            for (int i = 0; i < atts.getLength(); i += 2) {
                String name = atts.getQName(i);
                String value = atts.getValue(i);
                if ("id".equals(name)) {
                    id = value;
                }

                i++;

                name = atts.getQName(i);
                value = atts.getValue(i);

                if ("class".equals(name)) {
                    try {
                        clazz = Class.forName(value).newInstance();
                    } catch (InstantiationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                container.put(id, clazz);
            }
        }
    }

}