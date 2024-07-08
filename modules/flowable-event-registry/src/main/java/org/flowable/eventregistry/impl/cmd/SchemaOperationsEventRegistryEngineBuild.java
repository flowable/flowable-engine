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
package org.flowable.eventregistry.impl.cmd;

import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class SchemaOperationsEventRegistryEngineBuild implements Command<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaOperationsEventRegistryEngineBuild.class);

    @Override
    public Void execute(CommandContext commandContext) {
        
        SchemaManager schemaManager = CommandContextUtil.getEventRegistryConfiguration(commandContext).getSchemaManager();
        String databaseSchemaUpdate = CommandContextUtil.getEventRegistryConfiguration().getDatabaseSchemaUpdate();
        
        LOGGER.debug("Executing event registry schema management with setting {}", databaseSchemaUpdate);
        if (EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
            try {
                schemaManager.schemaDrop();
            } catch (RuntimeException e) {
                // ignore
            }
        }
        if (EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)
                || EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate) || EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_CREATE.equals(databaseSchemaUpdate)) {
            schemaManager.schemaCreate();

        } else if (EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
            schemaManager.schemaCheckVersion();

        } else if (EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
            schemaManager.schemaUpdate();
        }
        
        return null;
    }
}
