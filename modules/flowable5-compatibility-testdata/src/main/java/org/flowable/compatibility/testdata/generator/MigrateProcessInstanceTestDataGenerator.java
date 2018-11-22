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
package org.flowable.compatibility.testdata.generator;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.flowable.compatibility.testdata.Flowable5TestDataGenerator;

/**
 * @author Filip Hrisafov
 */
public class MigrateProcessInstanceTestDataGenerator implements Flowable5TestDataGenerator {

    @Override
    public void generateTestData(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment().addClasspathResource("migrationProcess.bpmn20.xml").deploy();

        RuntimeService runtimeService = processEngine.getRuntimeService();
        runtimeService.startProcessInstanceByKey("receiveTask", "activitiv5-migrate-process");
    }
}
