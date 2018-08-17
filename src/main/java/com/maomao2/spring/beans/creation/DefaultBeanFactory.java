package com.maomao2.spring.beans.creation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.maomao2.spring.beans.definition.BeanDefinition;
import com.maomao2.spring.beans.parsing.XmlBeanDefinitionReader;
import com.maomao2.spring.exception.BeanDefinitionStoreException;
import com.maomao2.spring.exception.BeansException;
import com.maomao2.spring.exception.NoSuchBeanDefinitionException;
import com.maomao2.spring.util.StringUtils;

public class DefaultBeanFactory extends AbstractBeanFactory implements ConfigureBeanFactory, BeanDefinitionRegistry {
    Logger logger = Logger.getLogger(getClass());

    /** Map of bean definition objects, keyed by bean name */
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>(256);

    /** List of bean definition names, in registration order */
    private volatile List<String> beanDefinitionNames = new ArrayList<String>(256);

    private String[] configLocations;

    public DefaultBeanFactory(String configLocation) throws BeansException {
        this(new String[] { configLocation });
    }

    public DefaultBeanFactory(String[] configLocations) {
        setConfigLocations(configLocations);
        refresh();
    }

    /**
     * Set the config locations for this application context.
     * <p>
     * If not set, the implementation may use a default as appropriate.
     */
    public void setConfigLocations(String... locations) {
        if (locations != null) {
            this.configLocations = new String[locations.length];
            for (int i = 0; i < locations.length; i++) {
                this.configLocations[i] = (locations[i]).trim();
            }
        } else {
            this.configLocations = null;
        }
    }

    /**
     * Return an array of resource locations, referring to the XML bean definition
     * files that this context should be built with. Can also include location
     * patterns, which will get resolved via a ResourcePatternResolver.
     * <p>
     * The default implementation returns {@code null}. Subclasses can override
     * this to provide a set of resource locations to load bean definitions from.
     * 
     * @return an array of resource locations, or {@code null} if none
     */
    protected String[] getConfigLocations() {
        return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
    }

    /**
     * Return the default config locations to use, for the case where no
     * explicit config locations have been specified.
     * <p>
     * The default implementation returns {@code null},
     * requiring explicit config locations.
     * 
     * @return an array of default config locations, if any
     * @see #setConfigLocations
     */
    protected String[] getDefaultConfigLocations() {
        return null;
    }

    private void refresh() {
        // Prepare this context for refreshing.
        // prepareRefresh();

        // Tell the subclass to refresh the internal bean factory.
        DefaultBeanFactory beanFactory = obtainFreshBeanFactory();

        // Prepare the bean factory for use in this context.
        // prepareBeanFactory(beanFactory);
        //
        // try {
        // // Allows post-processing of the bean factory in context subclasses.
        // postProcessBeanFactory(beanFactory);
        //
        // // Invoke factory processors registered as beans in the context.
        // invokeBeanFactoryPostProcessors(beanFactory);
        //
        // // Register bean processors that intercept bean creation.
        // registerBeanPostProcessors(beanFactory);
        //
        // // Initialize message source for this context.
        // initMessageSource();
        //
        // // Initialize event multicaster for this context.
        // initApplicationEventMulticaster();
        //
        // // Initialize other special beans in specific context subclasses.
        // onRefresh();
        //
        // // Check for listener beans and register them.
        // registerListeners();
        //
        // // Instantiate all remaining (non-lazy-init) singletons.
        // finishBeanFactoryInitialization(beanFactory);
        //
        // // Last step: publish corresponding event.
        // finishRefresh();

    }

    private DefaultBeanFactory obtainFreshBeanFactory() {
        String[] configLocations = getConfigLocations();
        if (configLocations != null) {
            // Create a new XmlBeanDefinitionReader for the given BeanFactory.
            XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(this);
            beanDefinitionReader.loadBeanDefinitions(configLocations);
        }
        return this;
    }

    @Override
    public Object getBean(String name) throws BeansException {
        return super.getBean(name, null);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return super.getBean(null, requiredType);
    }

    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
            throws BeanDefinitionStoreException {
        BeanDefinition oldBeanDefinition = this.beanDefinitionMap.get(beanName);
        if (oldBeanDefinition != null) {
            if (!oldBeanDefinition.equals(beanDefinition)) {
                this.logger.error("Overriding bean definition for bean '" + beanName +
                        "' with a different definition: replacing [" + oldBeanDefinition +
                        "] with [" + beanDefinition + "]");
            }
        }

        this.beanDefinitionMap.put(beanName, beanDefinition);
        this.beanDefinitionNames.add(beanName);
    }

    public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        this.beanDefinitionMap.remove(beanName);
    }

    public boolean containsBeanDefinition(String beanName) {
        return this.beanDefinitionMap.containsKey(beanName);
    }

    public String[] getBeanDefinitionNames() {

        return StringUtils.toStringArray(this.beanDefinitionNames);
    }

    public int getBeanDefinitionCount() {
        return this.beanDefinitionMap.size();
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        BeanDefinition bd = this.beanDefinitionMap.get(beanName);
        if (bd == null) {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace("No bean named '" + beanName + "' found in " + this);
            }
            throw new NoSuchBeanDefinitionException(beanName);
        }
        return bd;
    }
}
