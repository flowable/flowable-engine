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
package org.activiti.rest.conf;

import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.api.FormService;
import org.activiti.form.engine.FormEngine;
import org.activiti.form.engine.FormEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yvo Swillens
 */
@Configuration
public class FlowableFormEngineConfiguration extends BaseEngineConfiguration {

  @Bean
  public FormEngine formEngine() {
    FormEngineConfiguration formEngineConfiguration = new FormEngineConfiguration();
    formEngineConfiguration.setDataSource(dataSource());
    formEngineConfiguration.setDatabaseSchemaUpdate(environment.getProperty("engine.form.schema.update", "true"));

    return formEngineConfiguration.buildFormEngine();
  }

  @Bean
  public FormRepositoryService formRepositoryService() {
    return formEngine().getFormRepositoryService();
  }

  @Bean
  public FormService dmnFormService() {
    return formEngine().getFormService();
  }
}
