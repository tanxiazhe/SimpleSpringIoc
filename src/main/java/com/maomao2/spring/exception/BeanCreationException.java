package com.maomao2.spring.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class BeanCreationException extends RuntimeException {

    private String beanName;

    private List<Throwable> relatedCauses;

    /**
     * Create a new BeanCreationException.
     * 
     * @param msg
     *            the detail message
     */
    public BeanCreationException(String msg) {
        super(msg);
    }

    /**
     * Create a new BeanCreationException.
     * 
     * @param msg
     *            the detail message
     * @param cause
     *            the root cause
     */
    public BeanCreationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Create a new BeanCreationException.
     * 
     * @param beanName
     *            the name of the bean requested
     * @param msg
     *            the detail message
     */
    public BeanCreationException(String beanName, String msg) {
        super("Error creating bean" + (beanName != null ? " with name '" + beanName + "'" : "") + ": " + msg);
        this.beanName = beanName;
    }

    /**
     * Create a new BeanCreationException.
     * 
     * @param beanName
     *            the name of the bean requested
     * @param msg
     *            the detail message
     * @param cause
     *            the root cause
     */
    public BeanCreationException(String beanName, String msg, Throwable cause) {
        this(beanName, msg);
        initCause(cause);
    }

    /**
     * Return the name of the bean requested, if any.
     */
    public String getBeanName() {
        return this.beanName;
    }

    /**
     * Add a related cause to this bean creation exception,
     * not being a direct cause of the failure but having occurred
     * earlier in the creation of the same bean instance.
     * 
     * @param ex
     *            the related cause to add
     */
    public void addRelatedCause(Throwable ex) {
        if (this.relatedCauses == null) {
            this.relatedCauses = new LinkedList<Throwable>();
        }
        this.relatedCauses.add(ex);
    }

    /**
     * Return the related causes, if any.
     * 
     * @return the array of related causes, or {@code null} if none
     */
    public Throwable[] getRelatedCauses() {
        if (this.relatedCauses == null) {
            return null;
        }
        return this.relatedCauses.toArray(new Throwable[this.relatedCauses.size()]);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (this.relatedCauses != null) {
            for (Throwable relatedCause : this.relatedCauses) {
                sb.append("\nRelated cause: ");
                sb.append(relatedCause);
            }
        }
        return sb.toString();
    }

    @Override
    public void printStackTrace(PrintStream ps) {
        synchronized (ps) {
            super.printStackTrace(ps);
            if (this.relatedCauses != null) {
                for (Throwable relatedCause : this.relatedCauses) {
                    ps.println("Related cause:");
                    relatedCause.printStackTrace(ps);
                }
            }
        }
    }

    @Override
    public void printStackTrace(PrintWriter pw) {
        synchronized (pw) {
            super.printStackTrace(pw);
            if (this.relatedCauses != null) {
                for (Throwable relatedCause : this.relatedCauses) {
                    pw.println("Related cause:");
                    relatedCause.printStackTrace(pw);
                }
            }
        }
    }

}
