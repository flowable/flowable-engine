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
package org.flowable.spring.boot.json;

import org.flowable.common.engine.impl.json.VariableJsonMapper;
import org.flowable.common.engine.impl.json.jackson2.Jackson2VariableJsonMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "flowable", name = "variable-json-mapper", havingValue = "jackson2")
@ConditionalOnClass(ObjectMapper.class)
public class FlowableVariableJackson2JsonMapperConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public VariableJsonMapper flowableVariableJsonMapper(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new Jackson2VariableJsonMapper(objectMapper);
    }

}
