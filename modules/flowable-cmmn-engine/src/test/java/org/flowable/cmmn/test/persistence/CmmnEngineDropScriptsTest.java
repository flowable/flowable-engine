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
package org.flowable.cmmn.test.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.db.SchemaOperationsEngineBuild;
import org.flowable.common.engine.impl.db.SchemaOperationsEngineDropDbCmd;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnEngineDropScriptsTest extends FlowableCmmnTestCase {

    @Test
    public void testDropSchema() {

        // Dropping and recreating the schema should not have an impact on other tests
        long originalTableCount = countTables();
        assertThat(originalTableCount).isGreaterThan(0);
        cmmnEngineConfiguration.getCommandExecutor().execute(new SchemaOperationsEngineDropDbCmd(cmmnEngineConfiguration.getEngineScopeType()));
        assertThat(countTables()).isZero();

        cmmnEngineConfiguration.getCommandExecutor().execute(new SchemaOperationsEngineBuild(cmmnEngineConfiguration.getEngineScopeType()));
        assertThat(countTables()).isEqualTo(originalTableCount);
    }

    private long countTables() {
        return cmmnEngineConfiguration.getCmmnManagementService().getTableCounts().
                keySet().stream().filter(tableName -> tableName.toLowerCase().startsWith("act_cmmn")).count();
    }
}
