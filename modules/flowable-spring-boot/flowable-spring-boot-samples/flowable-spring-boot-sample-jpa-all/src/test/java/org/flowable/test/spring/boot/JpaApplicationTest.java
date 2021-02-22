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
package org.flowable.test.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import flowable.Application;
import flowable.Photo;
import flowable.PhotoRepository;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest(classes = Application.class)
public class JpaApplicationTest {

    @Autowired
    private SpringProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    @AfterEach
    public void tearDown() {
        runtimeService.createProcessInstanceQuery()
            .processDefinitionKey("dogeProcess")
            .list()
            .forEach(processInstance -> runtimeService.deleteProcessInstance(processInstance.getId(), "for test"));

        historyService.createHistoricProcessInstanceQuery()
            .processDefinitionKey("dogeProcess")
            .list()
            .forEach(processInstance -> historyService.deleteHistoricProcessInstance(processInstance.getId()));
        photoRepository.deleteAll();
    }

    @Test
    public void usesJpaEntityManager() {
        assertThat(processEngineConfiguration.getJpaEntityManagerFactory())
            .as("Process engine configuration jpa entity manager factory")
            .isEqualTo(entityManagerFactory)
            .isInstanceOf(EntityManagerFactory.class);
    }

    @Test
    public void photosArePersistedAsJpaEntities() {
        List<Photo> photos = photoRepository.saveAll(Arrays.asList(new Photo("one"), new Photo("two")));

        Map<String, Object> variables = new HashMap<>();
        variables.put("photos", photos);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dogeProcess", variables);

        List<Execution> waitingExecutions = runtimeService.createExecutionQuery().activityId("wait").list();
        assertThat(waitingExecutions)
            .hasSize(2);

        Map<String, Object> processInstanceVariables = runtimeService.getVariables(processInstance.getId());
        assertThat(processInstanceVariables).containsOnlyKeys("photos");
        List<Photo> processPhotos = (List<Photo>) processInstanceVariables.get("photos");
        assertThat(processPhotos)
            .extracting(Photo::getLabel)
            .containsExactly(
                "one",
                "two"
            );

        for (Execution waitingExecution : waitingExecutions) {
            Photo executionPhoto = runtimeService.getVariable(waitingExecution.getId(), "photo", Photo.class);
            assertThat(executionPhoto).isNotNull();
            assertThat(executionPhoto.getLabel()).isIn("one", "two");
            runtimeService.trigger(waitingExecution.getId());
        }

        Task reviewTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(reviewTask.getId(), Collections.singletonMap("approved", true));

        assertThat(runtimeService.createProcessInstanceQuery().count())
            .isZero();
    }
}
