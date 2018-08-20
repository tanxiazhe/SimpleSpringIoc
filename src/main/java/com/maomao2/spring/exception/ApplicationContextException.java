package com.maomao2.spring.exception;

import java.io.IOException;

public class ApplicationContextException extends BeansException {

    /**
     * TODO Comment.
     *
     * @since v1.0
     */
    private static final long serialVersionUID = 3543452982543936028L;

    public ApplicationContextException(String msg, IOException ex) {
        super(msg, ex);
    }
}
