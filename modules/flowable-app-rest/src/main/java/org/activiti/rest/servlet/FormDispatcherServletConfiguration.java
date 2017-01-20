package org.activiti.rest.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@ComponentScan({ "org.activiti.rest.form.exception", "org.activiti.rest.form.service.api" })
@EnableAsync
public class FormDispatcherServletConfiguration extends WebMvcConfigurationSupport {

  protected final Logger log = LoggerFactory.getLogger(FormDispatcherServletConfiguration.class);

}
