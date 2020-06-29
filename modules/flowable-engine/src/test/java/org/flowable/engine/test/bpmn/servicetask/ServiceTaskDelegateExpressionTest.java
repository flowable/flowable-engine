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

package org.flowable.engine.test.bpmn.servicetask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntityManager;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class ServiceTaskDelegateExpressionTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testDelegateExpression() {
        String processId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .transientVariable("delegateBean", new DummyTestDelegateBean())
                .start()
                .getProcessInstanceId();

        assertThat(runtimeService.getVariables(processId))
                .containsOnly(entry("executed", true));

        assertThat(taskService.createTaskQuery().singleResult()).isNotNull();
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/servicetask/ServiceTaskDelegateExpressionTest.testDelegateExpression.bpmn20.xml")
    public void testDelegateExpressionActivityInstanceQuery() {
        String processId = managementService.executeCommand(new Command<String>() {

            @Override
            public String execute(CommandContext commandContext) {
                String processId = runtimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("process")
                        .transientVariable("delegateBean", new DummyTestDelegateBean())
                        .start()
                        .getProcessInstanceId();
                
                ActivityInstanceEntityManager activityInstanceEntityManager = processEngineConfiguration.getActivityInstanceEntityManager();
                
                List<ActivityInstanceEntity> activityInstances = activityInstanceEntityManager.findActivityInstancesByProcessInstanceId(processId, true);
                assertThat(activityInstances).hasSize(5);
                Map<String, List<ActivityInstanceEntity>> activityInstanceMap = activityInstances.stream().collect(Collectors.groupingBy(ActivityInstanceEntity::getActivityId));
                assertThat(activityInstanceMap).containsKey("theStart");
                assertThat(activityInstanceMap.get("theStart")).hasSize(1);
                assertThat(activityInstanceMap.get("theStart").get(0).getEndTime()).isNotNull();
                assertThat(activityInstanceMap.get("theStart").get(0).getTransactionOrder()).isEqualTo(1);
                
                assertThat(activityInstanceMap).containsKey("service1");
                assertThat(activityInstanceMap.get("service1")).hasSize(1);
                assertThat(activityInstanceMap.get("service1").get(0).getEndTime()).isNotNull();
                assertThat(activityInstanceMap.get("service1").get(0).getTransactionOrder()).isEqualTo(3);
                
                assertThat(activityInstanceMap).containsKey("usertask1");
                assertThat(activityInstanceMap.get("usertask1")).hasSize(1);
                assertThat(activityInstanceMap.get("usertask1").get(0).getEndTime()).isNull();
                assertThat(activityInstanceMap.get("usertask1").get(0).getTransactionOrder()).isEqualTo(5);
                
                return processId;
            }
            
        });

        assertThat(runtimeService.getVariables(processId))
                .containsOnly(entry("executed", true));

        Task task = taskService.createTaskQuery().processInstanceId(processId).singleResult();
        assertThat(task).isNotNull();
        
        managementService.executeCommand(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                taskService.complete(task.getId());
                
                ActivityInstanceEntityManager activityInstanceEntityManager = processEngineConfiguration.getActivityInstanceEntityManager();
                
                List<ActivityInstanceEntity> activityInstances = activityInstanceEntityManager.findActivityInstancesByProcessInstanceId(processId, true);
                assertThat(activityInstances).hasSize(7);
                Map<String, List<ActivityInstanceEntity>> activityInstanceMap = activityInstances.stream().collect(Collectors.groupingBy(ActivityInstanceEntity::getActivityId));
                assertThat(activityInstanceMap).containsKey("usertask1");
                assertThat(activityInstanceMap.get("usertask1")).hasSize(1);
                assertThat(activityInstanceMap.get("usertask1").get(0).getEndTime()).isNotNull();
                assertThat(activityInstanceMap.get("usertask1").get(0).getTransactionOrder()).isEqualTo(5);
                
                assertThat(activityInstanceMap).containsKey("theEnd");
                assertThat(activityInstanceMap.get("theEnd")).hasSize(1);
                assertThat(activityInstanceMap.get("theEnd").get(0).getEndTime()).isNotNull();
                assertThat(activityInstanceMap.get("theEnd").get(0).getTransactionOrder()).isEqualTo(2);
                
                return null;
            }
            
        });
    }
    
    @Test
    @Deployment
    public void testDelegateExpressionPassThrough() {
        String processId = managementService.executeCommand(new Command<String>() {

            @Override
            public String execute(CommandContext commandContext) {
                String processId = runtimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("process")
                        .transientVariable("delegateBean", new DummyTestDelegateBean())
                        .start()
                        .getProcessInstanceId();
                
                ActivityInstanceEntityManager activityInstanceEntityManager = processEngineConfiguration.getActivityInstanceEntityManager();
                
                List<ActivityInstanceEntity> activityInstances = activityInstanceEntityManager.findActivityInstancesByProcessInstanceId(processId, true);
                assertThat(activityInstances).hasSize(5);
                
                ActivityInstanceEntity activityInstance = activityInstances.get(0);
                assertThat(activityInstance.getActivityId()).isEqualTo("theStart");
                assertThat(activityInstance.getEndTime()).isNotNull();
                assertThat(activityInstance.getTransactionOrder()).isEqualTo(1);
                
                activityInstance = activityInstances.get(1);
                assertThat(activityInstance.getActivityType()).isEqualTo("sequenceFlow");
                assertThat(activityInstance.getActivityId()).isEqualTo("_flow_theStart__service1");
                assertThat(activityInstance.getTransactionOrder()).isEqualTo(2);
                
                activityInstance = activityInstances.get(2);
                assertThat(activityInstance.getActivityId()).isEqualTo("service1");
                assertThat(activityInstance.getEndTime()).isNotNull();
                assertThat(activityInstance.getTransactionOrder()).isEqualTo(3);
                
                activityInstance = activityInstances.get(3);
                assertThat(activityInstance.getActivityType()).isEqualTo("sequenceFlow");
                assertThat(activityInstance.getActivityId()).isEqualTo("_flow_service1__theEnd");
                assertThat(activityInstance.getTransactionOrder()).isEqualTo(4);
                
                activityInstance = activityInstances.get(4);
                assertThat(activityInstance.getActivityId()).isEqualTo("theEnd");
                assertThat(activityInstance.getEndTime()).isNotNull();
                assertThat(activityInstance.getTransactionOrder()).isEqualTo(5);
                
                return processId;
            }
            
        });
        
        managementService.executeCommand(new Command<Void>() {
            
            @Override
            public Void execute(CommandContext commandContext) {
                ActivityInstanceEntityManager activityInstanceEntityManager = processEngineConfiguration.getActivityInstanceEntityManager();
                
                List<ActivityInstanceEntity> activityInstances = activityInstanceEntityManager.findActivityInstancesByProcessInstanceId(processId, true);
                assertThat(activityInstances).isEmpty();
                
                return null;
            }
        });
    }

    @Test
    @Deployment
    public void testDelegateExpressionWithSkipExpression() {
        // Global property enabled, but local says false -> do not skip
        String processId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .transientVariable("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true)
                .transientVariable("delegateBean", new DummyTestDelegateBean())
                .transientVariable("shouldSkip", false)
                .start()
                .getProcessInstanceId();

        assertThat(runtimeService.getVariables(processId))
                .containsOnly(
                        entry("executed", true)
                );

        assertThat(taskService.createTaskQuery().processInstanceId(processId).singleResult()).isNotNull();

        // Global property enabled, but local says true -> do skip
        processId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .transientVariable("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true)
                .transientVariable("delegateBean", new DummyTestDelegateBean())
                .transientVariable("shouldSkip", true)
                .start()
                .getProcessInstanceId();

        assertThat(runtimeService.getVariables(processId)).isEmpty();

        assertThat(taskService.createTaskQuery().processInstanceId(processId).singleResult()).isNotNull();

        // Global property not enabled, but local says true -> do not skip
        processId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .transientVariable("delegateBean", new DummyTestDelegateBean())
                .transientVariable("shouldSkip", true)
                .start()
                .getProcessInstanceId();

        assertThat(runtimeService.getVariables(processId))
                .containsOnly(
                        entry("executed", true)
                );

        assertThat(taskService.createTaskQuery().processInstanceId(processId).singleResult()).isNotNull();
    }
}
