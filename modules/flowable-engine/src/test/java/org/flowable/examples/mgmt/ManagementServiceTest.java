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
package org.flowable.examples.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test case for the various operations of the {@link ManagementService}
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 */
@DisabledIfSystemProperty(named = "disableWhen", matches = "cockroachdb")
public class ManagementServiceTest extends PluggableFlowableTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementServiceTest.class);

    @Test
    public void testTableCount() {
        Map<String, Long> tableCount = managementService.getTableCount();

        String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
        
        managementService.executeCommand(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                List<PropertyEntity> properties = Context.getProcessEngineConfiguration().getPropertyEntityManager().findAll();
                for (PropertyEntity propertyEntity : properties) {
                    LOGGER.info("!!!Property {} {}", propertyEntity.getName(), propertyEntity.getValue());
                }
                return null;
            }
            
        });

        assertThat(tableCount).containsEntry(tablePrefix + "ACT_GE_PROPERTY", Long.valueOf(13));
        assertThat(tableCount.get(tablePrefix + "ACT_GE_BYTEARRAY")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_RE_DEPLOYMENT")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_RU_EXECUTION")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_ID_GROUP")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_ID_MEMBERSHIP")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_ID_USER")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_RE_PROCDEF")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_RU_TASK")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_RU_IDENTITYLINK")).isZero();
    }

    @Test
    public void testGetTableMetaData() {

        String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

        TableMetaData tableMetaData = managementService.getTableMetaData(tablePrefix + "ACT_RU_TASK");
        assertThat(tableMetaData.getColumnTypes()).hasSameSizeAs(tableMetaData.getColumnNames());
        assertThat(tableMetaData.getColumnNames()).hasSize(30);
 
        int assigneeIndex = tableMetaData.getColumnNames().indexOf("ASSIGNEE_");
        int createTimeIndex = tableMetaData.getColumnNames().indexOf("CREATE_TIME_");

        assertThat(assigneeIndex).isGreaterThanOrEqualTo(0);
        assertThat(createTimeIndex).isGreaterThanOrEqualTo(0);

        assertOneOf(new String[] { "VARCHAR", "NVARCHAR2", "nvarchar", "NVARCHAR" }, tableMetaData.getColumnTypes().get(assigneeIndex));
        assertOneOf(new String[] { "TIMESTAMP", "TIMESTAMP(6)", "datetime", "DATETIME" }, tableMetaData.getColumnTypes().get(createTimeIndex));
    }

    private void assertOneOf(String[] possibleValues, String currentValue) {
        for (String value : possibleValues) {
            if (currentValue.equals(value)) {
                return;
            }
        }
        fail("Value '" + currentValue + "' should be one of: " + Arrays.deepToString(possibleValues));
    }

}
