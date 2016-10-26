/**
 * Activiti app component part of the Activiti project
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.activiti.rest.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.rest.service.api.PutAwareCommonsMultipartResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

/**
 * @author Yvo Swillens
 */
public class BaseDispatcherServletConfiguration extends WebMvcConfigurationSupport {

  protected final Logger log = LoggerFactory.getLogger(BaseDispatcherServletConfiguration.class);

  @Autowired
  protected ObjectMapper objectMapper;

  @Bean
  public SessionLocaleResolver localeResolver() {
    return new SessionLocaleResolver();
  }

  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    log.debug("Configuring localeChangeInterceptor");
    LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
    localeChangeInterceptor.setParamName("language");
    return localeChangeInterceptor;
  }

  @Bean
  public MultipartResolver multipartResolver() {
    PutAwareCommonsMultipartResolver multipartResolver = new PutAwareCommonsMultipartResolver();
    return multipartResolver;
  }

  @Bean
  public RequestMappingHandlerMapping requestMappingHandlerMapping() {
    log.debug("Creating requestMappingHandlerMapping");
    RequestMappingHandlerMapping requestMappingHandlerMapping = new RequestMappingHandlerMapping();
    requestMappingHandlerMapping.setUseSuffixPatternMatch(false);
    Object[] interceptors = { localeChangeInterceptor() };
    requestMappingHandlerMapping.setInterceptors(interceptors);
    return requestMappingHandlerMapping;
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    addDefaultHttpMessageConverters(converters);
    for (HttpMessageConverter<?> converter : converters) {
      if (converter instanceof MappingJackson2HttpMessageConverter) {
        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = (MappingJackson2HttpMessageConverter) converter;
        jackson2HttpMessageConverter.setObjectMapper(objectMapper);
        break;
      }
    }
  }

  @Override
  protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.favorPathExtension(false);
  }
}
