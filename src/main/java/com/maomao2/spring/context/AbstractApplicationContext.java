package com.maomao2.spring.context;

import com.maomao2.spring.beans.creation.DefaultBeanFactory;
import com.maomao2.spring.exception.ApplicationContextException;
import com.maomao2.spring.exception.BeansException;
import java.io.IOException;

public abstract class AbstractApplicationContext implements ApplicationContext {

  private String[] configLocations;

  /**
   * Bean factory for this context
   */
  private DefaultBeanFactory beanFactory;


  public DefaultBeanFactory getBeanFactory() throws IllegalStateException {
    return this.beanFactory;
  }


  /**
   * Set the config locations for this application context.
   * <p>
   * If not set, the implementation may use a default as appropriate.
   */
  protected void setConfigLocations(String... locations) {
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
   * Return an array of resource locations, referring to the XML bean definition files that this context should be built
   * with. Can also include location patterns, which will get resolved via a ResourcePatternResolver.
   * <p>
   * The default implementation returns {@code null}. Subclasses can override this to provide a set of resource
   * locations to load bean definitions from.
   *
   * @return an array of resource locations, or {@code null} if none
   */
  protected String[] getConfigLocations() {
    return this.configLocations;
  }

  protected void refresh() {
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

  protected DefaultBeanFactory obtainFreshBeanFactory() {
    refreshBeanFactory();
    return beanFactory;
  }

  /**
   * Subclasses must implement this method to perform the actual configuration load. The method is invoked by {@link
   * #refresh()} before any other initialization work.
   * <p>A subclass will either create a new bean factory and hold a reference to it,
   * or return a single BeanFactory instance that it holds. In the latter case, it will usually throw an
   * IllegalStateException if refreshing the context more than once.
   *
   * @throws BeansException if initialization of the bean factory failed
   * @throws IllegalStateException if already initialized and multiple refresh attempts are not supported
   */
  protected void refreshBeanFactory() throws BeansException, IllegalStateException {
    DefaultBeanFactory beanFactory = new DefaultBeanFactory();
    this.beanFactory = beanFactory;
    loadBeanDefinitions(beanFactory);
  }

  /**
   * Load bean definitions into the given bean factory, typically through delegating to one or more bean definition
   * readers.
   *
   * @param beanFactory the bean factory to load bean definitions into
   * @throws BeansException if parsing of the bean definitions failed
   * @throws IOException if loading of bean definition files failed
   */
  protected abstract void loadBeanDefinitions(DefaultBeanFactory beanFactory)
      throws BeansException, ApplicationContextException;

  @Override
  public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    return beanFactory.getBean(name, requiredType);
  }

  @Override
  public <T> T getBean(Class<T> requiredType) throws BeansException {
    return beanFactory.getBean(requiredType);
  }

  @Override
  public Object getBean(String name) throws BeansException {
    return beanFactory.getBean(name);
  }
}
