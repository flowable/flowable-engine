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
package org.flowable.spring.boot.job;

import java.util.stream.Collectors;

import org.flowable.common.rest.variable.RestVariableConverter;
import org.flowable.external.job.rest.service.api.ExternalJobRestResponseFactory;
import org.flowable.spring.boot.DispatcherServletConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Component scan for the External Job REST API Configuration.
 *
 * @author Filip Hrisafov
 */
@Import(DispatcherServletConfiguration.class)
@ComponentScan("org.flowable.external.job.rest.service.api")
public class ExternalJobRestConfiguration {

    @Autowired
    protected ObjectMapper objectMapper;

    @Bean
    @ConditionalOnMissingBean //If we don't include this annotation, we cannot override the RestResponseFactory bean
    public ExternalJobRestResponseFactory restResponseFactory(ObjectProvider<RestVariableConverter> variableConverters) {
        return new ExternalJobRestResponseFactory(objectMapper, variableConverters.orderedStream().collect(Collectors.toList()));
    }
}
