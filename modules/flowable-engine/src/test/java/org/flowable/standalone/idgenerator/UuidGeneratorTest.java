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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class UuidGeneratorTest extends ResourceFlowableTestCase {

    public UuidGeneratorTest() throws Exception {
        super("org/flowable/standalone/idgenerator/uuidgenerator.test.flowable.cfg.xml");
    }

    @Test
    @Deployment
    public void testUuidGeneratorUsage() {

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // Start processes
        for (int i = 0; i < 50; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        runtimeService.startProcessInstanceByKey("simpleProcess");
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail();
                    }
                }
            });
        }

        // Complete tasks
        executorService.execute(new Runnable() {

            @Override
            public void run() {
                boolean tasksFound = true;
                while (tasksFound) {

                    List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
                    for (org.flowable.task.api.Task task : tasks) {
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
            }
        });

        try {
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail();
        }

        assertEquals(50, historyService.createHistoricProcessInstanceQuery().count());
    }

}
