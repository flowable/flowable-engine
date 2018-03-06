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
package org.flowable.standalone.idgenerator;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.Deployment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class TaskIdGeneratorTest extends ResourceFlowableTestCase {

    public TaskIdGeneratorTest() throws Exception {
        super("org/flowable/standalone/idgenerator/taskidgenerator.test.flowable.cfg.xml");
    }

    @Deployment(resources = "org/flowable/standalone/idgenerator/UuidGeneratorTest.testUuidGeneratorUsage.bpmn20.xml")
    public void testUuidGeneratorUsage() {

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        // Start processes
        for (int i = 0; i < 5; i++) {
            executorService.execute(() -> {
                try {
                    runtimeService.startProcessInstanceByKey("simpleProcess");
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            });
        }

        // Complete tasks
        executorService.execute(() -> {
            boolean tasksFound = true;
            while (tasksFound) {

                List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
                for (org.flowable.task.api.Task task : tasks) {
                    assertThat(task.getId(), startsWith("TASK-"));
                    taskService.complete(task.getId());
                }

                tasksFound = taskService.createTaskQuery().count() > 0;

                if (!tasksFound) {
                    try {
                        Thread.sleep(1500L); // just to be sure
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    tasksFound = taskService.createTaskQuery().count() > 0;
                }
            }
        });

        try {
            executorService.shutdown();
            executorService.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

        assertEquals(5, historyService.createHistoricProcessInstanceQuery().count());
        historyService.createHistoricTaskInstanceQuery().list().
                forEach(historicTask -> assertThat(historicTask.getId(), startsWith("TASK-")) );
    }

}
