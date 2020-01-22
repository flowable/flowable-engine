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
package org.flowable.spring.boot;

import javax.persistence.EntityManagerFactory;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(type = "javax.persistence.EntityManagerFactory")
@ConditionalOnClass(SpringProcessEngineConfiguration.class)
@AutoConfigureAfter({
    HibernateJpaAutoConfiguration.class
})
public class FlowableJpaAutoConfiguration {

    @ConditionalOnMissingBean(name = "jpaProcessEngineConfigurer")
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> jpaProcessEngineConfigurer(EntityManagerFactory emf) {
        return processEngineConfiguration -> {
            processEngineConfiguration.setJpaEntityManagerFactory(emf);
            processEngineConfiguration.setJpaHandleTransaction(false);
            processEngineConfiguration.setJpaCloseEntityManager(false);
        };
    }
}
