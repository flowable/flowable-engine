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
package org.flowable.content.rest.conf.engine;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.cfg.StandaloneInMemContentEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Tijs Rademakers
 */
@Configuration(proxyBeanMethods = false)
public class FlowableContentEngineConfiguration {

    @Bean
    public ContentEngine contentEngine() {
        ContentEngineConfiguration contentEngineConfiguration = new StandaloneInMemContentEngineConfiguration();
        contentEngineConfiguration.setDatabaseSchemaUpdate(AbstractEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        return contentEngineConfiguration.buildContentEngine();
    }

    @Bean
    public ContentService contentService(ContentEngine contentEngine) {
        return contentEngine.getContentService();
    }
}
