package org.flowable.rest.conf;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.flowable.rest.conf.common", "org.flowable.rest.conf.engine" })
public class ApplicationConfiguration {

}
