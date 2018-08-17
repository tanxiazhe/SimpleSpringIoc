package com.maomao2.spring.beans.creation;

import com.maomao2.spring.exception.BeansException;

public interface BeanFactory {
    <T> T getBean(String name, Class<T> requiredType) throws BeansException;

    <T> T getBean(Class<T> requiredType) throws BeansException;

    Object getBean(String name) throws BeansException;
}
