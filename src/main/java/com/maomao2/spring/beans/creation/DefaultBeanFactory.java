package com.maomao2.spring.beans.creation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.maomao2.spring.beans.definition.AbstractBeanDefinition;
import com.maomao2.spring.beans.definition.BeanDefinition;
import com.maomao2.spring.beans.parsing.ApplicationContextHandler;
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

    public DefaultBeanFactory(String[] strings) {
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
     * @see #getResources
     * @see #getResourcePatternResolver
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
            loadBeanDefinitions(configLocations);
        }
        return this;
    }

    private void loadBeanDefinitions(String[] configLocations) {
        // Create a new XmlBeanDefinitionReader for the given BeanFactory.
        // XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        //
        // // Configure the bean definition reader with this context's
        // // resource loading environment.
        // beanDefinitionReader.setEnvironment(this.getEnvironment());
        // beanDefinitionReader.setResourceLoader(this);
        // beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
        //
        // // Allow a subclass to provide custom initialization of the reader,
        // // then proceed with actually loading the bean definitions.
        // initBeanDefinitionReader(beanDefinitionReader);
        // loadBeanDefinitions(beanDefinitionReader);
        //
        int counter = 0;
        for (String location : configLocations) {
            counter += loadBeanDefinitions(location);
        }

    }

    private int loadBeanDefinitions(String location) {
        doLoadBeanDefinitions(location);
        return 0;
    }

    protected void doLoadBeanDefinitions(String location) {
        // XMLReader parser = XMLReaderFactory.createXMLReader();
        ApplicationContextHandler applicationContextHandler = new ApplicationContextHandler();
        // parser.setContentHandler(applicationContextHandler);
        // parser.parse(location);
        Map<String, Object> container = applicationContextHandler.getContainer();
        for (String beanName : container.keySet()) {
            BeanDefinition beanDefinition = null;
            // TODO
            // beanDefinition= parseBeanDefinitionElement(ele, beanName, containingBean);
            registerBeanDefinition(beanName, beanDefinition);
        }
    }

    /**
     * Parse the bean definition itself, without regard to name or aliases. May return
     * {@code null} if problems occurred during the parsing of the bean definition.
     */
    public AbstractBeanDefinition parseBeanDefinitionElement(
            Element ele, String beanName, BeanDefinition containingBean) {

        // this.parseState.push(new BeanEntry(beanName));
        //
        // String className = null;
        // if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
        // className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
        // }
        //
        // try {
        // String parent = null;
        // if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
        // parent = ele.getAttribute(PARENT_ATTRIBUTE);
        // }
        // AbstractBeanDefinition bd = createBeanDefinition(className, parent);
        //
        // parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
        // bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));
        //
        // parseMetaElements(ele, bd);
        // parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
        // parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
        //
        // parseConstructorArgElements(ele, bd);
        // parsePropertyElements(ele, bd);
        // parseQualifierElements(ele, bd);
        //
        // bd.setResource(this.readerContext.getResource());
        // bd.setSource(extractSource(ele));
        //
        // return bd;
        // } catch (ClassNotFoundException ex) {
        // error("Bean class [" + className + "] not found", ele, ex);
        // } catch (NoClassDefFoundError err) {
        // error("Class that bean class [" + className + "] depends on not found", ele, err);
        // } catch (Throwable ex) {
        // error("Unexpected failure during bean definition parsing", ele, ex);
        // } finally {
        // this.parseState.pop();
        // }

        return null;
    }

    // private AbstractBeanDefinition createBeanDefinition(String className,
    // String parent) {
    // GenericBeanDefinition bd = new GenericBeanDefinition();
    // bd.setParentName(parentName);
    // if (className != null) {
    // if (classLoader != null) {
    // bd.setBeanClass(ClassUtils.forName(className, classLoader));
    // } else {
    // bd.setBeanClassName(className);
    // }
    // }
    // return bd;
    // }

    @Override
    public Object getBean(String name) throws BeansException {
        return super.getBean(name);
    }

    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
            throws BeanDefinitionStoreException {
        BeanDefinition oldBeanDefinition = this.beanDefinitionMap.get(beanName);
        if (oldBeanDefinition != null) {
            if (!oldBeanDefinition.equals(beanDefinition)) {
                this.logger.info("Overriding bean definition for bean '" + beanName +
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

    public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        return this.beanDefinitionMap.get(beanName);
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
}
