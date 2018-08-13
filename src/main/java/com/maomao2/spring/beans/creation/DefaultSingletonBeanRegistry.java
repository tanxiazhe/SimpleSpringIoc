package com.maomao2.spring.beans.creation;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.maomao2.spring.exception.BeanCreationException;
import com.maomao2.spring.exception.BeansException;
import com.maomao2.spring.util.Assert;

public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {

    protected static final Object NULL_OBJECT = new Object();

    /** Cache of singleton objects: bean name --> bean instance */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(256);

    /** Set of registered singletons, containing the bean names in registration order */
    private final Set<String> registeredSingletons = new LinkedHashSet<String>(256);

    public void registerSingleton(String beanName, Object singletonObject) {
        synchronized (this.singletonObjects) {
            Object oldObject = this.singletonObjects.get(beanName);
            if (oldObject != null) {
                throw new IllegalStateException("Could not register object [" + singletonObject +
                        "] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
            }
            this.singletonObjects.put(beanName, (singletonObject != null ? singletonObject : NULL_OBJECT));
            this.registeredSingletons.add(beanName);
        }

    }

    public Object getSingleton(String beanName) {
        Object singletonObject = this.singletonObjects.get(beanName);
        return (singletonObject != NULL_OBJECT ? singletonObject : null);
    }

    /**
     * Return the (raw) singleton object registered under the given name,
     * creating and registering a new one if none registered yet.
     * 
     * @param beanName
     *            the name of the bean
     * @param singletonFactory
     *            the ObjectFactory to lazily create the singleton
     *            with, if necessary
     * @return the registered singleton object
     */
    public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
        Assert.notNull(beanName, "'beanName' must not be null");
        synchronized (this.singletonObjects) {
            Object singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject == null) {
                boolean newSingleton = false;
                try {
                    singletonObject = singletonFactory.getObject();
                    newSingleton = true;
                } catch (IllegalStateException ex) {
                    // Has the singleton object implicitly appeared in the meantime ->
                    // if yes, proceed with it since the exception indicates that state.
                    singletonObject = this.singletonObjects.get(beanName);
                    if (singletonObject == null) {
                        throw ex;
                    }
                } catch (BeanCreationException ex) {
                    throw ex;
                } catch (BeansException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (newSingleton) {
                    addSingleton(beanName, singletonObject);
                }
            }
            return (singletonObject != NULL_OBJECT ? singletonObject : null);
        }
    }

    /**
     * Add the given singleton object to the singleton cache of this factory.
     * <p>
     * To be called for eager registration of singletons.
     * 
     * @param beanName
     *            the name of the bean
     * @param singletonObject
     *            the singleton object
     */
    protected void addSingleton(String beanName, Object singletonObject) {
        synchronized (this.singletonObjects) {
            this.singletonObjects.put(beanName, (singletonObject != null ? singletonObject : NULL_OBJECT));
            this.registeredSingletons.add(beanName);
        }
    }

}
