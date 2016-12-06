package org.activiti.rest.conf.common;

import org.activiti.rest.service.api.RestResponseFactory;
import org.flowable.rest.application.ContentTypeResolver;
import org.flowable.rest.application.DefaultContentTypeResolver;
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
}
