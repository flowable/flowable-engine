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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cmd.CustomSqlExecution;
import org.flowable.engine.impl.cmd.AbstractCustomSqlExecution;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.junit.jupiter.api.Test;

/**
 * @author jbarrez
 */
public class CustomMybatisMapperTest extends ResourceFlowableTestCase {

    public CustomMybatisMapperTest() {
        super("org/flowable/standalone/cfg/custom-mybatis-mappers-flowable.cfg.xml");
    }

    @Test
    public void testSelectTaskColumns() {

        // Create test data
        for (int i = 0; i < 5; i++) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setName(String.valueOf(i));
            taskService.saveTask(task);
        }

        // Fetch the columns we're interested in
        CustomSqlExecution<MyTestMapper, List<Map<String, Object>>> customSqlExecution = new AbstractCustomSqlExecution<MyTestMapper, List<Map<String, Object>>>(
                MyTestMapper.class) {

            @Override
            public List<Map<String, Object>> execute(MyTestMapper customMapper) {
                return customMapper.selectTasks();
            }
        };

        // Verify
        List<Map<String, Object>> tasks = managementService.executeCustomSql(customSqlExecution);
        assertThat(tasks).hasSize(5);
        for (int i = 0; i < 5; i++) {
            Map<String, Object> task = tasks.get(i);
            assertThat(getValue(task, "id")).isNotNull();
            assertThat(getValue(task, "name")).isNotNull();
            assertThat(getValue(task, "createTime")).isNotNull();
        }

        // Cleanup
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.deleteTask(task.getId(), true);
        }

    }

    @Test
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
        CustomSqlExecution<MyTestMapper, List<Map<String, Object>>> customSqlExecution = new AbstractCustomSqlExecution<MyTestMapper, List<Map<String, Object>>>(
                MyTestMapper.class) {

            @Override
            public List<Map<String, Object>> execute(MyTestMapper customMapper) {
                return customMapper.selectTaskWithSpecificVariable("myVar");
            }

        };

        // Verify
        List<Map<String, Object>> results = managementService.executeCustomSql(customSqlExecution);
        assertThat(results).hasSize(5);
        for (int i = 0; i < 5; i++) {
            Map<String, Object> result = results.get(i);
            long id = Long.parseLong((String) getValue(result, "taskId"));
            long variableValue = ((Number) getValue(result, "variableValue")).longValue();
            assertThat(variableValue).isEqualTo(id * 2);
        }

        // Cleanup
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.deleteTask(task.getId(), true);
        }

    }

    protected Object getValue(Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }

        String upperCaseKey = key.toUpperCase();
        if (map.containsKey(upperCaseKey)) {
            return map.get(upperCaseKey);
        }

        String lowerCaseKey = key.toLowerCase();
        if (map.containsKey(lowerCaseKey)) {
            return map.get(lowerCaseKey);
        }

        fail("Map with keys " + map.keySet() + " does not contain key " + key);
        return null;
    }

}
