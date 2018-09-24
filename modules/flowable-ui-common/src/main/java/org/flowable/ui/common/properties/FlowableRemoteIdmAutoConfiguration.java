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
package org.flowable.ui.common.properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.core.GrantedAuthorityDefaults;

/**
 * @author Filip Hrisafov
 */
@EnableConfigurationProperties({
    FlowableCommonAppProperties.class,
    FlowableRestAppProperties.class
})
@Configuration
public class FlowableRemoteIdmAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults(FlowableCommonAppProperties commonAppProperties) {
        return new GrantedAuthorityDefaults(commonAppProperties.getRolePrefix());
    }
}
