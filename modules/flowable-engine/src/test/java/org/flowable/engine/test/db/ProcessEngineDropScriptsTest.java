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
package org.flowable.engine.test.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.impl.db.SchemaOperationsEngineBuild;
import org.flowable.common.engine.impl.db.SchemaOperationsEngineDropDbCmd;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class ProcessEngineDropScriptsTest extends PluggableFlowableTestCase {

    @Test
    public void testDropSchema() {

        // Dropping and recreating the schema should not have an impact on other tests
        long originalTableCount = countTables();
        assertThat(originalTableCount).isGreaterThan(0);
        processEngineConfiguration.getCommandExecutor().execute(new SchemaOperationsEngineDropDbCmd(processEngineConfiguration.getEngineScopeType()));
        assertThat(countTables()).isZero();

        processEngineConfiguration.getCommandExecutor().execute(new SchemaOperationsEngineBuild(processEngineConfiguration.getEngineScopeType()));
        assertThat(countTables()).isEqualTo(originalTableCount);
    }

    private long countTables() {
        return processEngineConfiguration.getManagementService().getTableCount().
                keySet().stream().filter(tableName -> tableName.toLowerCase().startsWith("act_")).count();
    }

}
