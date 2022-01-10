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

package org.flowable.dmn.engine.test.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntity;
import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.junit.Test;

public class DmnManagementServiceTest extends AbstractFlowableDmnTest {

    @Test
    public void testGetMetaDataForUnexistingTable() {
        TableMetaData metaData = managementService.getTableMetaData("unexistingtable");
        assertThat(metaData).isNull();
    }

    @Test
    public void testGetMetaDataNullTableName() {
        assertThatThrownBy(() -> managementService.getTableMetaData(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("tableName is null");
    }

    @Test
    public void testGetTableName() {
        String table = managementService.getTableName(HistoricDecisionExecutionEntity.class);
        assertThat(table).isEqualTo("ACT_DMN_HI_DECISION_EXECUTION");
    }

    @Test
    public void testTableCount() {
        Map<String, Long> tableCount = managementService.getTableCount();

        String tablePrefix = dmnEngineConfiguration.getDatabaseTablePrefix();

        assertThat(tableCount).containsEntry(tablePrefix + "ACT_GE_PROPERTY", 2L);
        assertThat(tableCount.get(tablePrefix + "ACT_DMN_DECISION")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_DMN_DEPLOYMENT")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_DMN_DEPLOYMENT_RESOURCE")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_DMN_HI_DECISION_EXECUTION")).isZero();
        assertThat(tableCount.get(tablePrefix + "ACT_GE_BYTEARRAY")).isZero();
    }

    @Test
    public void testGetTableMetaData() {

        String tablePrefix = dmnEngineConfiguration.getDatabaseTablePrefix();

        TableMetaData tableMetaData = managementService.getTableMetaData(tablePrefix + "ACT_DMN_HI_DECISION_EXECUTION");
        assertThat(tableMetaData.getColumnTypes()).hasSameSizeAs(tableMetaData.getColumnNames());
        assertThat(tableMetaData.getColumnNames()).hasSize(12);

        int instanceIdIndex = tableMetaData.getColumnNames().indexOf("INSTANCE_ID_");
        int startTimeIndex = tableMetaData.getColumnNames().indexOf("START_TIME_");

        assertThat(instanceIdIndex).isGreaterThanOrEqualTo(0);
        assertThat(startTimeIndex).isGreaterThanOrEqualTo(0);

        assertThat(tableMetaData.getColumnTypes().get(instanceIdIndex))
                .isIn("VARCHAR", "VARCHAR2", "NVARCHAR2", "nvarchar", "NVARCHAR", "CHARACTER VARYING");

        assertThat(tableMetaData.getColumnTypes().get(startTimeIndex))
                .isIn("TIMESTAMP", "TIMESTAMP(6)", "datetime", "DATETIME");
    }
}
