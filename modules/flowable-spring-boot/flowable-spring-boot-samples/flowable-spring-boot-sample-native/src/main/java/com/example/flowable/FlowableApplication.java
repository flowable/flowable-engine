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
package com.example.flowable;

import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

@SpringBootApplication
public class FlowableApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowableApplication.class, args);
    }

    @Bean
    ApplicationRunner demo(ProcessEngine processEngine, CmmnEngine cmmnEngine, DmnEngine dmnEngine, EmailService emailService) {
        return args -> {
            startProcessInstance(processEngine, emailService);
            startCaseInstance(cmmnEngine, emailService);
            executeRule(dmnEngine);
        };
    }

    protected void startProcessInstance(ProcessEngine processEngine, EmailService emailService) {
        String customerId = "1";
        String email = "email@email.com";

        RuntimeService runtimeService = processEngine.getRuntimeService();
        TaskService taskService = processEngine.getTaskService();

        Map<String, Object> vars = Map.of("customerId", customerId, "email", email);
        String processInstanceId = runtimeService.startProcessInstanceByKey("signup-process", vars).getId();

        System.out.println("process instance ID: " + processInstanceId);
        Assert.notNull(processInstanceId, "the process instance ID should not be null");
        List<Task> tasks = taskService
                .createTaskQuery()
                .taskName("confirm-email-task")
                .includeProcessVariables()
                .processVariableValueEquals("customerId", customerId)
                .list();
        Assert.state(!tasks.isEmpty(), "there should be one outstanding task");
        tasks.forEach(task -> {
            taskService.claim(task.getId(), "jlong");
            taskService.complete(task.getId());
        });
        Assert.isTrue(emailService.getSendCount(email).get() == 1, "there should be 1 email sent");
    }

    protected void startCaseInstance(CmmnEngine cmmnEngine, EmailService emailService) {
        String customerId = "2";
        String email = "email@email.com";

        CmmnRuntimeService cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
        CmmnTaskService cmmnTaskService = cmmnEngine.getCmmnTaskService();

        String caseInstanceId = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("signupCase")
                .variable("customerId", customerId)
                .variable("email", email)
                .start()
                .getId();

        System.out.println("case instance ID: " + caseInstanceId);
        Assert.notNull(caseInstanceId, "the case instance ID should not be null");
        List<Task> tasks = cmmnTaskService
                .createTaskQuery()
                .taskName("Confirm email task")
                .includeProcessVariables()
                .caseVariableValueEquals("customerId", customerId)
                .list();
        Assert.state(!tasks.isEmpty(), "there should be one outstanding task");
        tasks.forEach(task -> {
            cmmnTaskService.claim(task.getId(), "jbarrez");
            cmmnTaskService.complete(task.getId());
        });
        Assert.isTrue(emailService.getSendCount(email).get() == 2, "there should be 2 emails sent");
    }

    protected void executeRule(DmnEngine dmnEngine) {
        Map<String, Object> result = dmnEngine.getDmnDecisionService().createExecuteDecisionBuilder()
                .decisionKey("myDecisionTable")
                .variable("customerTotalOrderPrice", 99999)
                .executeWithSingleResult();

        Assert.isTrue(result.size() == 1, "Expected one result");
        Object tier = result.get("tier");
        Assert.isTrue(tier.equals("SILVER"), "Expected SILVER as output, but was " + tier);
        System.out.println("Executed DMN rule correctly");
    }

}
