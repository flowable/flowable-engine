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

package org.flowable.content.engine.test.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.content.engine.impl.persistence.entity.ContentItemEntity;
import org.flowable.content.engine.test.AbstractFlowableContentTest;
import org.junit.Test;

public class ContentManagementServiceTest extends AbstractFlowableContentTest {

    @Test
    public void testGetMetaDataForUnexistingTable() {
        TableMetaData metaData = contentManagementService.getTableMetaData("unexistingtable");
        assertThat(metaData).isNull();
    }

    @Test
    public void testGetMetaDataNullTableName() {
        assertThatThrownBy(() -> contentManagementService.getTableMetaData(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("tableName is null");
    }

    @Test
    public void testGetTableName() {
        String table = contentManagementService.getTableName(ContentItemEntity.class);
        assertThat(table).isEqualTo("ACT_CO_CONTENT_ITEM");
    }

    @Test
    public void testTableCount() {
        Map<String, Long> tableCount = contentManagementService.getTableCount();

        String tablePrefix = contentEngineConfiguration.getDatabaseTablePrefix();

        assertThat(tableCount.get(tablePrefix + "ACT_CO_CONTENT_ITEM")).isZero();
    }

    @Test
    public void testGetTableMetaData() {

        String tablePrefix = contentEngineConfiguration.getDatabaseTablePrefix();

        TableMetaData tableMetaData = contentManagementService.getTableMetaData(tablePrefix + "ACT_CO_CONTENT_ITEM");
        assertThat(tableMetaData.getColumnTypes()).hasSameSizeAs(tableMetaData.getColumnNames());
        assertThat(tableMetaData.getColumnNames()).hasSize(17);

        int createdByIndex = tableMetaData.getColumnNames().indexOf("CREATED_BY_");
        int createdIndex = tableMetaData.getColumnNames().indexOf("CREATED_");

        assertThat(createdByIndex).isGreaterThanOrEqualTo(0);
        assertThat(createdIndex).isGreaterThanOrEqualTo(0);

        assertThat(tableMetaData.getColumnTypes().get(createdByIndex))
                .isIn("VARCHAR", "VARCHAR2", "NVARCHAR2", "nvarchar", "NVARCHAR", "CHARACTER VARYING");

        assertThat(tableMetaData.getColumnTypes().get(createdIndex))
                .isIn("TIMESTAMP", "TIMESTAMP(6)", "datetime", "DATETIME");
    }
}
