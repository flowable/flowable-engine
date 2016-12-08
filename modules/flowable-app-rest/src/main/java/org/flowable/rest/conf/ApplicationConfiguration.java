package org.flowable.rest.conf;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources({ 
  @PropertySource(value = "classpath:db.properties", ignoreResourceNotFound = true), 
  @PropertySource(value = "classpath:engine.properties", ignoreResourceNotFound = true) 
  })
@ComponentScan(basePackages = { "org.flowable.rest.conf" })
@ImportResource({ "classpath:flowable-custom-context.xml" })
public class ApplicationConfiguration {

}
