package org.flowable.rest.dmn.conf;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "org.flowable.rest.dmn.conf.common", "org.flowable.rest.dmn.conf.engine" })
public class ApplicationConfiguration {

}
