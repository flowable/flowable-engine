package org.flowable.rest.conf;

import org.flowable.rest.conf.engine.EngineConfigurationWithTaskLogging;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = { "org.flowable.rest.conf.common" })
@Import(EngineConfigurationWithTaskLogging.class)
public class ApplicationWithTaskLogging {

}
