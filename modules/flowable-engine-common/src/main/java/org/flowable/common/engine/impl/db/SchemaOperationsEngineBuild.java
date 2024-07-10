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
package org.flowable.common.engine.impl.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class SchemaOperationsEngineBuild implements Command<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaOperationsEngineBuild.class);

    protected final String engineScopeType;

    public SchemaOperationsEngineBuild(String engineScopeType) {
        this.engineScopeType = engineScopeType;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations()
                .get(engineScopeType);
        if (engineConfiguration == null) {
            throw new FlowableIllegalArgumentException("There is no engine configuration for scope " + engineScopeType);
        }

        String databaseSchemaUpdate = engineConfiguration.getDatabaseSchemaUpdate();
        List<SchemaManager> schemaManagers = new ArrayList<>();
        schemaManagers.add(engineConfiguration.getCommonSchemaManager());
        schemaManagers.add(engineConfiguration.getSchemaManager());

        Collection<SchemaManager> additionalSchemaManagers = engineConfiguration.getAdditionalSchemaManagers();

        if (additionalSchemaManagers != null) {
            schemaManagers.addAll(additionalSchemaManagers);
        }

        executeSchemaUpdate(schemaManagers, databaseSchemaUpdate);

        return null;
    }

    protected void executeSchemaUpdate(List<SchemaManager> schemaManagers, String databaseSchemaUpdate) {
        LOGGER.debug("Executing schema management with setting {} from engine {}", databaseSchemaUpdate, engineScopeType);

        if (AbstractEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
            // The drop is executed in the reverse order
            ListIterator<SchemaManager> listIterator = schemaManagers.listIterator(schemaManagers.size());
            while (listIterator.hasPrevious()) {
                SchemaManager schemaManager = listIterator.previous();
                try {
                    schemaManager.schemaDrop();
                } catch (RuntimeException e) {
                    // ignore
                }
            }
        }

        if (AbstractEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)
                || AbstractEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)
                || AbstractEngineConfiguration.DB_SCHEMA_UPDATE_CREATE.equals(databaseSchemaUpdate)) {
            schemaManagers.forEach(SchemaManager::schemaCreate);

        } else if (AbstractEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
            schemaManagers.forEach(SchemaManager::schemaCheckVersion);

        } else if (AbstractEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
            schemaManagers.forEach(SchemaManager::schemaUpdate);
        }

    }
}
