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
package org.flowable.test.spring.boot.util;

import java.util.ArrayList;
import java.util.List;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.content.spring.SpringContentEngineConfiguration;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Filip Hrisafov
 */
@Configuration
public class CustomUserEngineConfigurerConfiguration {

    protected final List<Class<?>> invokedConfigurations = new ArrayList<>();

    @Bean
    public EngineConfigurationConfigurer<SpringAppEngineConfiguration> customUserSpringAppEngineConfigurer() {
        return this::configurationInvoked;
    }

    @Bean
    public EngineConfigurationConfigurer<SpringCmmnEngineConfiguration> customUserSpringCmmnEngineConfigurer() {
        return this::configurationInvoked;
    }

    @Bean
    public EngineConfigurationConfigurer<SpringContentEngineConfiguration> customUserSpringContentEngineConfigurer() {
        return this::configurationInvoked;
    }

    @Bean
    public EngineConfigurationConfigurer<SpringDmnEngineConfiguration> customUserSpringDmnEngineConfigurer() {
        return this::configurationInvoked;
    }

    @Bean
    public EngineConfigurationConfigurer<SpringFormEngineConfiguration> customUserSpringFormEngineConfigurer() {
        return this::configurationInvoked;
    }

    @Bean
    public EngineConfigurationConfigurer<SpringIdmEngineConfiguration> customUserSpringIdmEngineConfigurer() {
        return this::configurationInvoked;
    }

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customUserSpringProcessEngineConfigurer() {
        return this::configurationInvoked;
    }

    private void configurationInvoked(AbstractEngineConfiguration engineConfiguration) {
        invokedConfigurations.add(AopUtils.getTargetClass(engineConfiguration));
    }

    public List<Class<?>> getInvokedConfigurations() {
        return invokedConfigurations;
    }
}
