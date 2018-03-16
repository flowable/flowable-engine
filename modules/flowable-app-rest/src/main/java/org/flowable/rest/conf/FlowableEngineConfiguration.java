package org.flowable.rest.conf;

import org.apache.commons.lang3.StringUtils;
import org.flowable.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class FlowableEngineConfiguration {

    @Autowired
    protected Environment environment;

    @Bean
    public ProcessEngineConfigurationConfigurer processEngineConfigurer() {
        return processEngineConfiguration -> {
            processEngineConfiguration.setDatabaseSchemaUpdate(environment.getProperty("engine.process.schema.update", "true"));
            processEngineConfiguration.setAsyncExecutorActivate(Boolean.valueOf(environment.getProperty("engine.process.asyncexecutor.activate", "true")));
            processEngineConfiguration.setHistory(environment.getProperty("engine.process.history.level", "full"));

            String emailHost = environment.getProperty("email.host");
            if (StringUtils.isNotEmpty(emailHost)) {
                processEngineConfiguration.setMailServerHost(emailHost);
                processEngineConfiguration.setMailServerPort(environment.getRequiredProperty("email.port", Integer.class));

                Boolean useCredentials = environment.getProperty("email.useCredentials", Boolean.class);
                if (Boolean.TRUE.equals(useCredentials)) {
                    processEngineConfiguration.setMailServerUsername(environment.getProperty("email.username"));
                    processEngineConfiguration.setMailServerPassword(environment.getProperty("email.password"));
                }

                Boolean useSSL = environment.getProperty("email.useSSL", Boolean.class);
                if (Boolean.TRUE.equals(useSSL)) {
                    processEngineConfiguration.setMailServerUseSSL(true);
                }

                Boolean useTLS = environment.getProperty("email.useTLS", Boolean.class);
                if (Boolean.TRUE.equals(useTLS)) {
                    processEngineConfiguration.setMailServerUseTLS(useTLS);
                }
            }

            // Limit process definition cache
            processEngineConfiguration.setProcessDefinitionCacheLimit(environment.getProperty("flowable.process-definitions.cache.max", Integer.class, 128));
        };

    }
}
