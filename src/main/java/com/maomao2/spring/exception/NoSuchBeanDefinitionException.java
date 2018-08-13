package com.maomao2.spring.exception;

public class NoSuchBeanDefinitionException extends RuntimeException {

    /**
     * TODO Comment.
     *
     * @since v1.0
     */
    private static final long serialVersionUID = 1L;

    private String beanName;

    public NoSuchBeanDefinitionException(String name) {
        super("No bean named '" + name + "' available");
        this.setBeanName(name);
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}
