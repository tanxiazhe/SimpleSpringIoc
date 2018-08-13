package com.maomao2.spring.beans.creation;

import java.lang.reflect.Field;

public class SimpleTypeConverter implements TypeConverter {

    public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
        return doConvert(value, requiredType, null, null);
    }

    public <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam)
            throws TypeMismatchException {
        return doConvert(value, requiredType, methodParam, null);
    }

    public <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field) throws TypeMismatchException {
        return doConvert(value, requiredType, null, field);
    }

    private <T> T doConvert(Object value, Class<T> requiredType, MethodParameter methodParam, Field field)
            throws TypeMismatchException {
        try {
            if (field != null) {
                // return this.typeConverterDelegate.convertIfNecessary(value, requiredType, field);
            } else {
                // reurn this.typeConverterDelegate.convertIfNecessary(value, requiredType, methodParam);
            }
        } catch (IllegalArgumentException ex) {
            throw new TypeMismatchException(value, requiredType, ex);
        }
        return null;
    }
}
