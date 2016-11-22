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
package org.activiti.rest.content.conf.engine;

import org.activiti.content.api.ContentService;
import org.activiti.content.engine.ContentEngine;
import org.activiti.content.engine.ContentEngineConfiguration;
import org.activiti.content.engine.impl.cfg.StandaloneInMemContentEngineConfiguration;
import org.activiti.engine.AbstractEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Tijs Rademakers
 */
@Configuration
public class ActivitiContentEngineConfiguration {

  @Bean
  public ContentEngine contentEngine() {
    ContentEngineConfiguration contentEngineConfiguration = new StandaloneInMemContentEngineConfiguration();
    contentEngineConfiguration.setDatabaseSchemaUpdate(AbstractEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
    return contentEngineConfiguration.buildContentEngine();
  }

  @Bean
  public ContentService contentService() {
    return contentEngine().getContentService();
  }
}
