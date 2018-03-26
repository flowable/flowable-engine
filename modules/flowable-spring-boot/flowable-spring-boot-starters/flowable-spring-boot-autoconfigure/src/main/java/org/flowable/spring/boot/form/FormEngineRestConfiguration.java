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
package org.flowable.spring.boot.form;

import org.flowable.form.rest.FormRestResponseFactory;
import org.flowable.spring.boot.DispatcherServletConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Component scan for the Form Rest API Configuration.
 *
 * @author Filip Hrisafov
 */
@Import(DispatcherServletConfiguration.class)
@ComponentScan("org.flowable.form.rest.service.api")
public class FormEngineRestConfiguration {

    @Bean
    public FormRestResponseFactory formRestResponseFactory() {
        return new FormRestResponseFactory();
    }
}
