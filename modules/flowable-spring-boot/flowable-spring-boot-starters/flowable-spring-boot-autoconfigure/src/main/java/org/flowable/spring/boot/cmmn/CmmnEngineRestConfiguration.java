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
package org.flowable.spring.boot.cmmn;

import java.util.List;

import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.common.rest.variable.RestVariableConverter;
import org.flowable.spring.boot.DispatcherServletConfiguration;
import org.flowable.spring.boot.json.Jackson2JsonRestConverterConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import tools.jackson.databind.ObjectMapper;

/**
 * Component scan for the CMMN Rest API Configuration.
 *
 * @author Filip Hrisafov
 */
@Import({
        DispatcherServletConfiguration.class,
        Jackson2JsonRestConverterConfiguration.class
})
@ComponentScan("org.flowable.cmmn.rest.service.api")
public class CmmnEngineRestConfiguration {
    
    @Bean
    public CmmnRestResponseFactory cmmnRestResponseFactory(ObjectMapper objectMapper, ObjectProvider<RestVariableConverter> variableConverters) {
        CmmnRestResponseFactory restResponseFactory = new CmmnRestResponseFactory(objectMapper);
        List<RestVariableConverter> additionalVariableConverters = variableConverters.orderedStream().toList();
        restResponseFactory.getVariableConverters().addAll(additionalVariableConverters);
        return restResponseFactory;
    }
}
