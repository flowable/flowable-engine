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
package org.flowable.standalone.cfg;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cmd.CustomSqlExecution;
import org.flowable.engine.impl.cmd.AbstractCustomSqlExecution;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;

/**
 * @author jbarrez
 */
public class CustomMybatisMapperTest extends ResourceFlowableTestCase {

    public CustomMybatisMapperTest() {
        super("org/flowable/standalone/cfg/custom-mybatis-mappers-flowable.cfg.xml");
    }

    public void testSelectTaskColumns() {

        // Create test data
        for (int i = 0; i < 5; i++) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setName(String.valueOf(i));
            taskService.saveTask(task);
        }

        // Fetch the columns we're interested in
        CustomSqlExecution<MyTestMapper, List<Map<String, Object>>> customSqlExecution = new AbstractCustomSqlExecution<MyTestMapper, List<Map<String, Object>>>(MyTestMapper.class) {

            @Override
            public List<Map<String, Object>> execute(MyTestMapper customMapper) {
                return customMapper.selectTasks();
            }
        };

        // Verify
        List<Map<String, Object>> tasks = managementService.executeCustomSql(customSqlExecution);
        assertEquals(5, tasks.size());
        for (int i = 0; i < 5; i++) {
            Map<String, Object> task = tasks.get(i);
            assertNotNull(task.get("ID"));
            assertNotNull(task.get("NAME"));
            assertNotNull(task.get("CREATETIME"));
        }

        // Cleanup
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.deleteTask(task.getId());
            historyService.deleteHistoricTaskInstance(task.getId());
        }

    }

    public void testFetchTaskWithSpecificVariable() {

        // Create test data
        for (int i = 0; i < 5; i++) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setName(String.valueOf(i));
            taskService.saveTask(task);

            taskService.setVariable(task.getId(), "myVar", Long.valueOf(task.getId()) * 2);
            taskService.setVariable(task.getId(), "myVar2", "SomeOtherValue");
        }

        // Fetch data with custom query
        CustomSqlExecution<MyTestMapper, List<Map<String, Object>>> customSqlExecution = new AbstractCustomSqlExecution<MyTestMapper, List<Map<String, Object>>>(MyTestMapper.class) {

            @Override
            public List<Map<String, Object>> execute(MyTestMapper customMapper) {
                return customMapper.selectTaskWithSpecificVariable("myVar");
            }

        };

        // Verify
        List<Map<String, Object>> results = managementService.executeCustomSql(customSqlExecution);
        assertEquals(5, results.size());
        for (int i = 0; i < 5; i++) {
            Map<String, Object> result = results.get(i);
            Long id = Long.valueOf((String) result.get("TASKID"));
            Long variableValue = ((Number) result.get("VARIABLEVALUE")).longValue();
            assertEquals(id * 2, variableValue.longValue());
        }

        // Cleanup
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.deleteTask(task.getId());
            historyService.deleteHistoricTaskInstance(task.getId());
        }

    }

}
