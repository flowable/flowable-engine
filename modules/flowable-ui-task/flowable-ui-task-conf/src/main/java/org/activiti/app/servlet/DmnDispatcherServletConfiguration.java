package org.activiti.app.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@ComponentScan({ "org.activiti.rest.dmn.exception", "org.activiti.rest.dmn.service.api" })
@EnableAsync
public class DmnDispatcherServletConfiguration extends WebMvcConfigurationSupport {

  protected final Logger log = LoggerFactory.getLogger(DmnDispatcherServletConfiguration.class);

}
