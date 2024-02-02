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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.management.TablePage;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

/**
 * @author Joram Barrez
 */
@DisabledIfSystemProperty(named = "disableWhen", matches = "cockroachdb")
public class TablePageQueryTest extends PluggableFlowableTestCase {

    @Test
    public void testGetTablePage() {
        String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
        List<String> taskIds = generateDummyTasks(20);

        TablePage tablePage = managementService.createTablePageQuery().tableName(tablePrefix + "ACT_RU_TASK").listPage(0, 5);

        assertThat(tablePage.getFirstResult()).isZero();
        assertThat(tablePage.getSize()).isEqualTo(5);
        assertThat(tablePage.getRows()).hasSize(5);
        assertThat(tablePage.getTotal()).isEqualTo(20);

        tablePage = managementService.createTablePageQuery().tableName(tablePrefix + "ACT_RU_TASK").listPage(14, 10);

        assertThat(tablePage.getFirstResult()).isEqualTo(14);
        assertThat(tablePage.getSize()).isEqualTo(6);
        assertThat(tablePage.getRows()).hasSize(6);
        assertThat(tablePage.getTotal()).isEqualTo(20);

        taskService.deleteTasks(taskIds, true);
    }

    @Test
    public void testGetSortedTablePage() {
        String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
        List<String> taskIds = generateDummyTasks(15);

        // With an ascending sort
        TablePage tablePage = managementService.createTablePageQuery().tableName(tablePrefix + "ACT_RU_TASK").orderAsc("NAME_").listPage(1, 7);
        String[] expectedTaskNames = new String[] { "B", "C", "D", "E", "F", "G", "H" };
        verifyTaskNames(expectedTaskNames, tablePage.getRows());

        // With a descending sort
        tablePage = managementService.createTablePageQuery().tableName(tablePrefix + "ACT_RU_TASK").orderDesc("NAME_").listPage(6, 8);
        expectedTaskNames = new String[] { "I", "H", "G", "F", "E", "D", "C", "B" };
        verifyTaskNames(expectedTaskNames, tablePage.getRows());

        taskService.deleteTasks(taskIds, true);
    }

    private void verifyTaskNames(String[] expectedTaskNames, List<Map<String, Object>> rowData) {
        assertThat(rowData).hasSameSizeAs(expectedTaskNames);
        String columnKey = "NAME_";

        // mybatis will return the correct case for postgres table columns from
        // version 3.0.6 on
        if ("postgres".equals(processEngineConfiguration.getDatabaseType())) {
            columnKey = "name_";
        }

        for (int i = 0; i < expectedTaskNames.length; i++) {
            assertThat(rowData.get(i))
                    .containsEntry(columnKey, expectedTaskNames[i]);
        }
    }

    private List<String> generateDummyTasks(int nrOfTasks) {
        ArrayList<String> taskIds = new ArrayList<>();
        for (int i = 0; i < nrOfTasks; i++) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setName(String.valueOf((char) ('A' + i)));
            taskService.saveTask(task);
            taskIds.add(task.getId());
        }
        return taskIds;
    }

}
