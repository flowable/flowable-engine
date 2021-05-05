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
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cmd.CustomSqlExecution;
import org.flowable.engine.impl.cmd.AbstractCustomSqlExecution;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.junit.jupiter.api.Test;

/**
 * @author jbarrez
 * @author Filip Hrisafov
 */
public class CustomMybatisMapperTest extends ResourceFlowableTestCase {

    public CustomMybatisMapperTest() {
        super("org/flowable/standalone/cfg/custom-mybatis-mappers-flowable.cfg.xml");
    }

    @Test
    public void testSelectTaskColumns() {

        // Create test data
        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        for (int i = 0; i < 5; i++) {
            processEngineConfiguration.getClock().setCurrentTime(Date.from(start.plus(i, ChronoUnit.MINUTES)));
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
        assertThat(tasks)
                .extracting(task -> getValue(task, "name"), task -> getValue(task, "createTime"))
                .containsExactlyInAnyOrder(
                        tuple("0", Date.from(start)),
                        tuple("1", Date.from(start.plus(1, ChronoUnit.MINUTES))),
                        tuple("2", Date.from(start.plus(2, ChronoUnit.MINUTES))),
                        tuple("3", Date.from(start.plus(3, ChronoUnit.MINUTES))),
                        tuple("4", Date.from(start.plus(4, ChronoUnit.MINUTES)))
                );

        assertThat(tasks)
                .extracting(task -> getValue(task, "id"))
                .doesNotContainNull();

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
