package org.activiti.rest.conf;

import org.activiti.rest.common.application.ContentTypeResolver;
import org.activiti.rest.common.application.DefaultContentTypeResolver;
import org.activiti.rest.dmn.service.api.DmnRestResponseFactory;
import org.activiti.rest.form.FormRestResponseFactory;
import org.activiti.rest.service.api.RestResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Joram Barrez
 */
@Configuration
public class RestConfiguration {

  @Bean()
  public RestResponseFactory restResponseFactory() {
    RestResponseFactory restResponseFactory = new RestResponseFactory();
    return restResponseFactory;
  }

  @Bean()
  public ContentTypeResolver contentTypeResolver() {
    ContentTypeResolver resolver = new DefaultContentTypeResolver();
    return resolver;
  }

  @Bean()
  public DmnRestResponseFactory restDmnResponseFactory() {
    DmnRestResponseFactory restResponseFactory = new DmnRestResponseFactory();
    return restResponseFactory;
  }

  @Bean()
  public org.activiti.rest.dmn.common.ContentTypeResolver dmnContentTypeResolver() {
    org.activiti.rest.dmn.common.ContentTypeResolver resolver = new org.activiti.rest.dmn.common.DefaultContentTypeResolver();
    return resolver;
  }

  @Bean()
  public FormRestResponseFactory formResponseFactory() {
    FormRestResponseFactory restResponseFactory = new FormRestResponseFactory();
    return restResponseFactory;
  }

  @Bean()
  public org.activiti.rest.form.common.ContentTypeResolver formContentTypeResolver() {
    org.activiti.rest.form.common.ContentTypeResolver resolver = new org.activiti.rest.form.common.DefaultContentTypeResolver();
    return resolver;
  }


}
