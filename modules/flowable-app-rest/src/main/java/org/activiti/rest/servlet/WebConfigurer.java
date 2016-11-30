package org.activiti.rest.servlet;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import org.activiti.rest.conf.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
public class WebConfigurer implements ServletContextListener {

  private final Logger log = LoggerFactory.getLogger(WebConfigurer.class);

  public AnnotationConfigWebApplicationContext context;

  public void setContext(AnnotationConfigWebApplicationContext context) {
    this.context = context;
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServletContext servletContext = sce.getServletContext();

    log.debug("Configuring Spring root application context");

    AnnotationConfigWebApplicationContext rootContext = null;

    if (context == null) {
      rootContext = new AnnotationConfigWebApplicationContext();
      rootContext.register(ApplicationConfiguration.class);
      rootContext.refresh();
    } else {
      rootContext = context;
    }

    servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootContext);

    EnumSet<DispatcherType> disps = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);

    initSpringProcessRest(servletContext, rootContext);
    initSpringDMNRest(servletContext, rootContext);
    initSpringFormRest(servletContext, rootContext);
    initSpringContentRest(servletContext, rootContext);

    initSpringSecurity(servletContext, disps);

    log.debug("Web application fully configured");
  }

  /**
   * Initializes Process Spring and Spring MVC .
   */
  private ServletRegistration.Dynamic initSpringProcessRest(ServletContext servletContext, AnnotationConfigWebApplicationContext rootContext) {
    return initSpringRestComponent(servletContext, rootContext, "process", "/service", ProcessDispatcherServletConfiguration.class);
  }

  /**
   * Initializes DMN Spring and Spring MVC.
   */
  protected ServletRegistration.Dynamic initSpringDMNRest(ServletContext servletContext, AnnotationConfigWebApplicationContext rootContext) {
    return initSpringRestComponent(servletContext, rootContext, "dmn", "/dmn-api", DmnDispatcherServletConfiguration.class);
  }

  /**
   * Initializes Form Spring and Spring MVC.
   */
  protected ServletRegistration.Dynamic initSpringFormRest(ServletContext servletContext, AnnotationConfigWebApplicationContext rootContext) {
    return initSpringRestComponent(servletContext, rootContext, "form", "/form-api", FormDispatcherServletConfiguration.class);
  }

  /**
   * Initializes Content Spring and Spring MVC.
   */
  protected ServletRegistration.Dynamic initSpringContentRest(ServletContext servletContext, AnnotationConfigWebApplicationContext rootContext) {
    return initSpringRestComponent(servletContext, rootContext, "content", "/content-api", ContentDispatcherServletConfiguration.class);
  }
  
  protected ServletRegistration.Dynamic initSpringRestComponent(ServletContext servletContext, AnnotationConfigWebApplicationContext rootContext,
      String name, String restContextRoot, Class<? extends WebMvcConfigurationSupport> webConfigClass) {
    
    log.debug("Configuring Spring Web application context - " + name + " REST");
    AnnotationConfigWebApplicationContext dispatcherServletConfiguration = new AnnotationConfigWebApplicationContext();
    dispatcherServletConfiguration.setParent(rootContext);
    dispatcherServletConfiguration.register(webConfigClass);

    log.debug("Registering Spring MVC Servlet - " + name + " REST");
    ServletRegistration.Dynamic dispatcherServlet = servletContext.addServlet(name + "-dispatcher", new DispatcherServlet(dispatcherServletConfiguration));
    dispatcherServlet.addMapping(restContextRoot + "/*");
    dispatcherServlet.setLoadOnStartup(1);
    dispatcherServlet.setAsyncSupported(true);

    return dispatcherServlet;
  }

  /**
   * Initializes Spring Security.
   */
  protected void initSpringSecurity(ServletContext servletContext, EnumSet<DispatcherType> disps) {
    log.debug("Registering Spring Security Filter");
    FilterRegistration.Dynamic springSecurityFilter = servletContext.addFilter("springSecurityFilterChain", new DelegatingFilterProxy());

    springSecurityFilter.addMappingForUrlPatterns(disps, false, "/*");
    springSecurityFilter.setAsyncSupported(true);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    log.info("Destroying Web application");
    WebApplicationContext ac = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext());
    AnnotationConfigWebApplicationContext gwac = (AnnotationConfigWebApplicationContext) ac;
    gwac.close();
    log.debug("Web application destroyed");
  }
}
