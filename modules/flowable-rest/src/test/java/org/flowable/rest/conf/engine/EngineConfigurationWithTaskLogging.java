package org.flowable.rest.conf.engine;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfigurationWithTaskLogging extends EngineConfiguration {

    @Override
    public ProcessEngineConfigurationImpl processEngineConfiguration() {
        ProcessEngineConfigurationImpl configuration = super.processEngineConfiguration();
        configuration.setEnableHistoricTaskLogging(true);
        return configuration;
    }
}
