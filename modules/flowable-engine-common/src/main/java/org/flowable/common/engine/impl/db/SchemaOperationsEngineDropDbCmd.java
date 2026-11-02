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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Filip Hrisafov
 */
public class SchemaOperationsEngineDropDbCmd implements Command<Void> {

    protected final String engineScopeType;

    public SchemaOperationsEngineDropDbCmd(String engineScopeType) {
        this.engineScopeType = engineScopeType;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations()
                .get(engineScopeType);
        if (engineConfiguration == null) {
            throw new FlowableIllegalArgumentException("There is no engine configuration for scope " + engineScopeType);
        }

        List<SchemaManager> schemaManagers = new ArrayList<>();
        schemaManagers.add(engineConfiguration.getCommonSchemaManager());
        schemaManagers.add(engineConfiguration.getSchemaManager());

        Map<String, SchemaManager> additionalSchemaManagers = engineConfiguration.getAdditionalSchemaManagers();

        if (additionalSchemaManagers != null) {
            schemaManagers.addAll(additionalSchemaManagers.values());
        }

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

        return null;
    }

}
