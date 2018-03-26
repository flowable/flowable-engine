package org.flowable.dmn.rest.conf;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.flowable.dmn.rest.conf.common", "org.flowable.dmn.rest.conf.engine" })
public class ApplicationConfiguration {

}
