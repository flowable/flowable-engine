package org.flowable.app.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@ComponentScan(value = { "org.flowable.rest.exception", "org.flowable.rest.service.api" }, excludeFilters = { @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.flowable.rest.service.api.identity.*") })
@EnableAsync
public class ProcessDispatcherServletConfiguration extends BaseDispatcherServletConfiguration {

    protected final Logger log = LoggerFactory.getLogger(ProcessDispatcherServletConfiguration.class);

}
