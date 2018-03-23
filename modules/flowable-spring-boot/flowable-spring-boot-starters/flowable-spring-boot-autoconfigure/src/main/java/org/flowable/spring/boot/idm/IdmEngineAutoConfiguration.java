/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.spring.boot.idm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.flowable.engine.impl.cfg.IdmEngineConfigurator;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.idm.spring.authentication.SpringEncoder;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.AbstractEngineAutoConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnIdmEngine;
import org.flowable.spring.boot.condition.ConditionalOnProcessEngine;
import org.flowable.spring.configurator.SpringIdmEngineConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} for the Idm engine
 *
 * @author Filip Hrisafov
 */
@Configuration
@ConditionalOnIdmEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableIdmProperties.class
})
@AutoConfigureAfter({
    FlowableTransactionAutoConfiguration.class,
})
@AutoConfigureBefore({
    ProcessEngineAutoConfiguration.class
})
public class IdmEngineAutoConfiguration extends AbstractEngineAutoConfiguration {

    protected final FlowableIdmProperties idmProperties;
    protected List<EngineConfigurationConfigurer<SpringIdmEngineConfiguration>> engineConfigurers = new ArrayList<>();

    public IdmEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableIdmProperties idmProperties) {
        super(flowableProperties);
        this.idmProperties = idmProperties;
    }

    @ConditionalOnClass(PasswordEncoder.class)
    @Configuration
    @ConditionalOnMissingClass("org.flowable.ldap.LDAPConfiguration")
    @ConditionalOnProperty(prefix = "flowable.idm.ldap", name = "enabled", havingValue = "false", matchIfMissing = true)
    public static class PasswordEncoderConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public PasswordEncoder passwordEncoder(FlowableIdmProperties idmProperties) {
            PasswordEncoder encoder;
            String encoderType = idmProperties.getPasswordEncoder();
            if (Objects.equals("spring_bcrypt", encoderType)) {
                encoder = new BCryptPasswordEncoder();
            } else {
                encoder = NoOpPasswordEncoder.getInstance();
            }

            return encoder;
        }

        @Bean
        @ConditionalOnBean(PasswordEncoder.class)
        @ConditionalOnMissingBean(name = "passwordEncoderIdmEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringIdmEngineConfiguration> passwordEncoderIdmEngineConfigurationConfigurer(PasswordEncoder passwordEncoder) {
            return idmEngineConfiguration -> idmEngineConfiguration.setPasswordEncoder(new SpringEncoder(passwordEncoder));
        }

    }

    @Bean
    @ConditionalOnMissingBean
    public SpringIdmEngineConfiguration idmEngineConfiguration(DataSource dataSource, PlatformTransactionManager platformTransactionManager) {
        SpringIdmEngineConfiguration configuration = new SpringIdmEngineConfiguration();

        configuration.setTransactionManager(platformTransactionManager);
        configureEngine(configuration, dataSource);

        engineConfigurers.forEach(configurer -> configurer.configure(configuration));

        return configuration;
    }

    @Configuration
    @ConditionalOnProcessEngine
    public static class IdmEngineProcessConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "idmProcessEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> idmProcessEngineConfigurationConfigurer(
            IdmEngineConfigurator idmEngineConfigurator
        ) {
            return processEngineConfiguration -> processEngineConfiguration.setIdmEngineConfigurator(idmEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public IdmEngineConfigurator idmEngineConfigurator(IdmEngineConfiguration configuration) {
            SpringIdmEngineConfigurator idmEngineConfigurator = new SpringIdmEngineConfigurator();
            idmEngineConfigurator.setIdmEngineConfiguration(configuration);
            return idmEngineConfigurator;
        }
    }

    @Autowired(required = false)
    public void setEngineConfigurers(List<EngineConfigurationConfigurer<SpringIdmEngineConfiguration>> engineConfigurers) {
        this.engineConfigurers = engineConfigurers;
    }
}

