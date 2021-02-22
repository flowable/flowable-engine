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

import java.util.Collections;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Demonstrates the Flowable Actuator endpoints available <a href="http://localhost:8080/flowable/processes/waiter">on localhost</a> where, in this case, {@code waiter} is the name of the process
 * definition.
 *
 */
@SpringBootApplication(proxyBeanMethods = false)
public class Application {

    @Bean
    CommandLineRunner startProcess(final RuntimeService runtimeService, final TaskService taskService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... strings) throws Exception {
                for (int i = 0; i < 10; i++)
                    runtimeService.startProcessInstanceByKey("waiter", Collections.singletonMap("customerId", (Object) i));

                for (int i = 0; i < 7; i++)
                    taskService.complete(taskService.createTaskQuery().list().get(0).getId());
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
