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
package flowable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication(proxyBeanMethods = false)
public class Application {

    @Bean
    CommandLineRunner init(final PhotoService photoService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... strings) throws Exception {
                photoService.launchPhotoProcess("one", "two", "three");
            }
        };

    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Service
@Transactional
class PhotoService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final PhotoRepository photoRepository;

    @Autowired
    public PhotoService(RuntimeService runtimeService, TaskService taskService, PhotoRepository photoRepository) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.photoRepository = photoRepository;
    }

    public void processPhoto(Long photoId) {
        System.out.println("processing photo#" + photoId);
    }

    public void launchPhotoProcess(String... photoLabels) {
        List<Photo> photos = new ArrayList<>();
        for (String l : photoLabels) {
            Photo x = this.photoRepository.save(new Photo(l));
            photos.add(x);
        }

        Map<String, Object> procVars = new HashMap<>();
        procVars.put("photos", photos);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dogeProcess", procVars);

        List<Execution> waitingExecutions = runtimeService.createExecutionQuery().activityId("wait").list();
        System.out.println("--> # executions = " + waitingExecutions.size());

        for (Execution execution : waitingExecutions) {
            runtimeService.trigger(execution.getId());
        }

        Task reviewTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(reviewTask.getId(), Collections.singletonMap("approved", (Object) true));

        long count = runtimeService.createProcessInstanceQuery().count();
        System.out.println("Proc count " + count);

    }
}

interface PhotoRepository extends JpaRepository<Photo, Long> {
}

@Entity
class Photo {

    @Id
    @GeneratedValue
    private Long id;

    Photo() {
    }

    Photo(String username) {
        this.username = username;
    }

    private String username;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
