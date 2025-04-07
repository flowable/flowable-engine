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
package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Filip Hrisafov
 */
public class ProcessInstanceQueryTest extends PluggableFlowableTestCase {

    private static final int PROCESS_DEFINITION_KEY_DEPLOY_COUNT = 4;
    private static final int PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT = 1;
    private static final int PROCESS_DEPLOY_COUNT = PROCESS_DEFINITION_KEY_DEPLOY_COUNT + PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT;
    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
    private static final String PROCESS_DEFINITION_KEY_2 = "oneTaskProcess2";
    private static final String PROCESS_DEFINITION_NAME = "oneTaskProcessName";
    private static final String PROCESS_DEFINITION_NAME_2 = "oneTaskProcess2Name";
    private static final String PROCESS_DEFINITION_CATEGORY = "org.flowable.engine.test.api.runtime.Category";
    private static final String PROCESS_DEFINITION_CATEGORY_2 = "org.flowable.engine.test.api.runtime.2Category";

    private org.flowable.engine.repository.Deployment deployment;
    private List<String> processInstanceIds;

    /**
     * Setup starts 4 process instances of oneTaskProcess and 1 instance of oneTaskProcess2
     */
    @BeforeEach
    protected void setUp() throws Exception {
        deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess2.bpmn20.xml").deploy();

        processInstanceIds = new ArrayList<>();
        for (int i = 0; i < PROCESS_DEFINITION_KEY_DEPLOY_COUNT; i++) {
            String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, String.valueOf(i)).getId();
            runtimeService.updateBusinessStatus(processInstanceId, String.valueOf(i));
            processInstanceIds.add(processInstanceId);
        }
        
        String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY_2, "1").getId();
        runtimeService.updateBusinessStatus(processInstanceId, "1");
        processInstanceIds.add(processInstanceId);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        deleteDeployments();
    }

    @Test
    public void testQueryNoSpecificsList() {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        assertThat(query.count()).isEqualTo(PROCESS_DEPLOY_COUNT);
        assertThat(query.list()).hasSize(PROCESS_DEPLOY_COUNT);
    }

    @Test
    public void testQueryNoSpecificsSingleResult() {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByProcessDefinitionKeySingleResult() {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY_2);
        assertThat(query.count()).isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(query.list()).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(query.singleResult()).isNotNull();
    }

    @Test
    public void testQueryByInvalidProcessDefinitionKey() {
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("invalid").singleResult()).isNull();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("invalid").list()).isEmpty();
    }

    @Test
    public void testQueryByProcessDefinitionKeyMultipleResults() {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY);
        assertThat(query.count()).isEqualTo(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(query.list()).hasSize(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByProcessDefinitionKeys() {
        final Set<String> processDefinitionKeySet = new HashSet<>(2);
        processDefinitionKeySet.add(PROCESS_DEFINITION_KEY);
        processDefinitionKeySet.add(PROCESS_DEFINITION_KEY_2);

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKeys(processDefinitionKeySet);
        assertThat(query.count()).isEqualTo(PROCESS_DEPLOY_COUNT);
        assertThat(query.list()).hasSize(PROCESS_DEPLOY_COUNT);
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByInvalidProcessDefinitionKeys() {
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().processDefinitionKeys(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().processDefinitionKeys(Collections.emptySet()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }
    
    @Test
    public void testQueryByExcludeProcessDefinitionKeys() {
        Set<String> processDefinitionKeySet = new HashSet<>(2);
        processDefinitionKeySet.add(PROCESS_DEFINITION_KEY);
        processDefinitionKeySet.add(PROCESS_DEFINITION_KEY_2);

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().excludeProcessDefinitionKeys(processDefinitionKeySet);
        assertThat(query.count()).isEqualTo(0);
        assertThat(query.list()).hasSize(0);
        
        processDefinitionKeySet = new HashSet<>(2);
        processDefinitionKeySet.add(PROCESS_DEFINITION_KEY);
        
        query = runtimeService.createProcessInstanceQuery().excludeProcessDefinitionKeys(processDefinitionKeySet);
        assertThat(query.count()).isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(query.list()).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        
        processDefinitionKeySet = new HashSet<>(2);
        processDefinitionKeySet.add(PROCESS_DEFINITION_KEY_2);
        
        query = runtimeService.createProcessInstanceQuery().excludeProcessDefinitionKeys(processDefinitionKeySet);
        assertThat(query.count()).isEqualTo(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(query.list()).hasSize(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
    }

    @Test
    public void testQueryByInvalidExcludeProcessDefinitionKeys() {
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().excludeProcessDefinitionKeys(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().excludeProcessDefinitionKeys(Collections.emptySet()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }
    
    @Test
    public void testQueryByProcessDefinitionKeyLike() {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKeyLike(PROCESS_DEFINITION_KEY_2);
        assertThat(query.count()).isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(query.list()).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(query.singleResult()).isNotNull();
        
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKeyLike("oneTask%").list()).hasSize(5);
        
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKeyLike("none%").list()).hasSize(0);
    }
    
    @Test
    public void testQueryByProcessDefinitionKeyLikeIgnoreCase() {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKeyLikeIgnoreCase(PROCESS_DEFINITION_KEY_2);
        assertThat(query.count()).isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(query.list()).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(query.singleResult()).isNotNull();
        
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKeyLikeIgnoreCase("onetask%").list()).hasSize(5);
        
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKeyLikeIgnoreCase("none%").list()).hasSize(0);
    }

    @Test
    public void testQueryByProcessInstanceId() {
        for (String processInstanceId : processInstanceIds) {
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult()).isNotNull();
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).list()).hasSize(1);
        }
    }

    @Test
    public void testQueryByProcessDefinitionCategory() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().processDefinitionCategory(PROCESS_DEFINITION_CATEGORY).list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);

        assertThat(instances)
                .extracting(ProcessInstance::getBusinessKey, ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionName,
                        ProcessInstance::getProcessDefinitionVersion, ProcessInstance::getProcessDefinitionCategory, ProcessInstance::getDeploymentId)
                .as("businessKey, processDefinitionKey, processDefinitionName, processDefinitionVersion, processDefinitionCategory, deploymentId")
                .containsExactlyInAnyOrder(
                        tuple("0", PROCESS_DEFINITION_KEY, "oneTaskProcessName", 1, PROCESS_DEFINITION_CATEGORY, deployment.getId()),
                        tuple("1", PROCESS_DEFINITION_KEY, "oneTaskProcessName", 1, PROCESS_DEFINITION_CATEGORY, deployment.getId()),
                        tuple("2", PROCESS_DEFINITION_KEY, "oneTaskProcessName", 1, PROCESS_DEFINITION_CATEGORY, deployment.getId()),
                        tuple("3", PROCESS_DEFINITION_KEY, "oneTaskProcessName", 1, PROCESS_DEFINITION_CATEGORY, deployment.getId()));

        instances = runtimeService.createProcessInstanceQuery().processDefinitionCategory(PROCESS_DEFINITION_CATEGORY_2).list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);

        assertThat(instances)
                .extracting(ProcessInstance::getBusinessKey, ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionName,
                        ProcessInstance::getProcessDefinitionVersion, ProcessInstance::getProcessDefinitionCategory, ProcessInstance::getDeploymentId)
                .as("businessKey, processDefinitionKey, processDefinitionName, processDefinitionVersion, processDefinitionCategory, deploymentId")
                .containsExactlyInAnyOrder(
                        tuple("1", PROCESS_DEFINITION_KEY_2, "oneTaskProcess2Name", 1, PROCESS_DEFINITION_CATEGORY_2, deployment.getId())
                );
    }

    @Test
    public void testOrQueryByProcessDefinitionCategory() {
        assertThat(
                runtimeService.createProcessInstanceQuery().or().processDefinitionCategory(PROCESS_DEFINITION_CATEGORY).processDefinitionId("undefined").endOr()
                        .count()).isEqualTo(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().or().processDefinitionCategory(PROCESS_DEFINITION_CATEGORY_2).processDefinitionId("undefined")
                .endOr().count()).isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
    }
    
    @Test
    public void testQueryByProcessDefinitionCategoryLike() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLike(PROCESS_DEFINITION_CATEGORY).list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);

        instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLike(PROCESS_DEFINITION_CATEGORY_2).list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);

        instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLike("%Category").list();
        assertThat(instances).hasSize(5);
        
        instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLike("%2Category").list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        
        instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLike("%none").list();
        assertThat(instances).hasSize(0);
    }
    
    @Test
    public void testOrQueryByProcessDefinitionCategoryLike() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().or().processDefinitionCategoryLike(PROCESS_DEFINITION_CATEGORY).processDefinitionId("undefined").endOr().list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);

        instances = runtimeService.createProcessInstanceQuery().or().processDefinitionCategoryLike(PROCESS_DEFINITION_CATEGORY_2).processDefinitionId("undefined").endOr().list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);

        instances = runtimeService.createProcessInstanceQuery().or().processDefinitionCategoryLike("%Category").processDefinitionId("undefined").endOr().list();
        assertThat(instances).hasSize(5);
        
        instances = runtimeService.createProcessInstanceQuery().or().processDefinitionCategoryLike("%2Category").processDefinitionId("undefined").endOr().list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        
        instances = runtimeService.createProcessInstanceQuery().or().processDefinitionCategoryLike("%none").processDefinitionId("undefined").endOr().list();
        assertThat(instances).hasSize(0);
    }
    
    @Test
    public void testQueryByProcessDefinitionCategoryLikeIgnoreCase() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLikeIgnoreCase(PROCESS_DEFINITION_CATEGORY).list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);

        instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLikeIgnoreCase(PROCESS_DEFINITION_CATEGORY_2).list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);

        instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLikeIgnoreCase("%category").list();
        assertThat(instances).hasSize(5);
        
        instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLikeIgnoreCase("%2category").list();
        assertThat(instances).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        
        instances = runtimeService.createProcessInstanceQuery().processDefinitionCategoryLikeIgnoreCase("%none").list();
        assertThat(instances).hasSize(0);
    }

    @Test
    public void testQueryByProcessInstanceName() {
        runtimeService.setProcessInstanceName(processInstanceIds.get(0), "new name");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceName("new name").singleResult()).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceName("new name").list()).hasSize(1);

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceName("unexisting").singleResult()).isNull();
    }

    @Test
    public void testOrQueryByProcessInstanceName() {
        runtimeService.setProcessInstanceName(processInstanceIds.get(0), "new name");
        assertThat(runtimeService.createProcessInstanceQuery().or().processInstanceName("new name").processDefinitionId("undefined").endOr().singleResult())
                .isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().or().processInstanceName("new name").processDefinitionId("undefined").endOr().list()).hasSize(1);

        assertThat(runtimeService.createProcessInstanceQuery()
                .or()
                .processInstanceName("new name")
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .processDefinitionId("undefined")
                .singleResult()).isNotNull();

        assertThat(runtimeService.createProcessInstanceQuery().or().processInstanceName("unexisting").processDefinitionId("undefined").endOr().singleResult())
                .isNull();

        assertThat(runtimeService.createProcessInstanceQuery()
                .or()
                .processInstanceName("unexisting")
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .processDefinitionId("undefined")
                .endOr()
                .singleResult()).isNull();
    }

    @Test
    public void testQueryByProcessInstanceNameLike() {
        runtimeService.setProcessInstanceName(processInstanceIds.get(0), "new name");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceNameLike("% name").singleResult()).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceNameLike("new name").list()).hasSize(1);

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceNameLike("%nope").singleResult()).isNull();
    }

    @Test
    public void testOrQueryByProcessInstanceNameLike() {
        runtimeService.setProcessInstanceName(processInstanceIds.get(0), "new name");
        assertThat(runtimeService.createProcessInstanceQuery().or().processInstanceNameLike("% name").processDefinitionId("undefined").endOr().singleResult())
                .isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().or().processInstanceNameLike("new name").processDefinitionId("undefined").endOr().list())
                .hasSize(1);

        assertThat(runtimeService.createProcessInstanceQuery().or().processInstanceNameLike("%nope").processDefinitionId("undefined").endOr().singleResult())
                .isNull();
    }

    @Test
    public void testOrQueryByProcessInstanceNameLikeIgnoreCase() {
        runtimeService.setProcessInstanceName(processInstanceIds.get(0), "new name");
        runtimeService.setProcessInstanceName(processInstanceIds.get(1), "other Name!");

        // Runtime
        assertThat(runtimeService.createProcessInstanceQuery().or().processInstanceNameLikeIgnoreCase("%name%").processDefinitionId("undefined").endOr().list())
                .hasSize(2);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceNameLikeIgnoreCase("%name%").list()).hasSize(2);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceNameLikeIgnoreCase("%NAME%").list()).hasSize(2);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceNameLikeIgnoreCase("%NaM%").list()).hasSize(2);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceNameLikeIgnoreCase("%the%").list()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceNameLikeIgnoreCase("new%").list()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().or().processInstanceNameLikeIgnoreCase("%nope").processDefinitionId("undefined").endOr()
                .singleResult()).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // History
            assertThat(historyService.createHistoricProcessInstanceQuery().or().processInstanceNameLikeIgnoreCase("%name%").processDefinitionId("undefined")
                    .endOr().list()).hasSize(2);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceNameLikeIgnoreCase("%name%").list()).hasSize(2);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceNameLikeIgnoreCase("%NAME%").list()).hasSize(2);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceNameLikeIgnoreCase("%NaM%").list()).hasSize(2);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceNameLikeIgnoreCase("%the%").list()).hasSize(1);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceNameLikeIgnoreCase("new%").list()).hasSize(1);
            assertThat(
                    historyService.createHistoricProcessInstanceQuery().or().processInstanceNameLikeIgnoreCase("%nope").processDefinitionId("undefined").endOr()
                            .singleResult()).isNull();
        }
    }

    @Test
    public void testQueryByBusinessKeyAndProcessDefinitionKey() {
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("0", PROCESS_DEFINITION_KEY).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("1", PROCESS_DEFINITION_KEY).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("2", PROCESS_DEFINITION_KEY).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("3", PROCESS_DEFINITION_KEY).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("1", PROCESS_DEFINITION_KEY_2).count()).isEqualTo(1);
    }

    @Test
    public void testQueryByBusinessKey() {
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("0").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("1").count()).isEqualTo(2);
    }

    @Test
    public void testQueryByBusinessKeyLike() {
        processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, "1A").getId());
        processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, "A1").getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLike("%0").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLike("1%").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLike("%1").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLike("%1%").count()).isEqualTo(4);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLike("%A%").count()).isEqualTo(2);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLike("%B%").count()).isZero();
    }
    
    @Test
    public void testQueryByBusinessKeyLikeIgnoreCase() {
        processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, "1A").getId());
        processInstanceIds.add(runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, "A1").getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%0").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("1%").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%1").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%1%").count()).isEqualTo(4);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%a%").count()).isEqualTo(2);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%b%").count()).isZero();
    }

    @Test
    public void testQueryByInvalidBusinessKey() {
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("invalid").count()).isZero();

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(null).count())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }
    
    @Test
    public void testQueryByBusinessStatus() {
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatus("0").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatus("1").count()).isEqualTo(2);
    }

    @Test
    public void testQueryByBusinessStatusLike() {
        String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
        processInstanceIds.add(processInstanceId);
        runtimeService.updateBusinessStatus(processInstanceId, "1A");
        
        processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
        processInstanceIds.add(processInstanceId);
        runtimeService.updateBusinessStatus(processInstanceId, "A1");
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLike("%0").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLike("1%").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLike("%1").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLike("%1%").count()).isEqualTo(4);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLike("%A%").count()).isEqualTo(2);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLike("%B%").count()).isZero();
    }
    
    @Test
    public void testQueryByBusinessStatusLikeIgnoreCase() {
        String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
        processInstanceIds.add(processInstanceId);
        runtimeService.updateBusinessStatus(processInstanceId, "1A");
        
        processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
        processInstanceIds.add(processInstanceId);
        runtimeService.updateBusinessStatus(processInstanceId, "A1");
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLikeIgnoreCase("%0").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLikeIgnoreCase("1%").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLikeIgnoreCase("%1").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLikeIgnoreCase("%1%").count()).isEqualTo(4);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLikeIgnoreCase("%a%").count()).isEqualTo(2);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatusLikeIgnoreCase("%b%").count()).isZero();
    }

    @Test
    public void testQueryByInvalidBusinessStatus() {
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessStatus("invalid").count()).isZero();

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().processInstanceBusinessStatus(null).count())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByProcessDefinitionId() {
        final ProcessDefinition processDefinition1 = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY)
                .singleResult();
        ProcessInstanceQuery query1 = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition1.getId());
        assertThat(query1.count()).isEqualTo(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(query1.list()).hasSize(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThatThrownBy(() -> query1.singleResult())
                .isExactlyInstanceOf(FlowableException.class);

        final ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY_2)
                .singleResult();
        ProcessInstanceQuery query2 = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition2.getId());
        assertThat(query2.count()).isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(query2.list()).hasSize(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(query2.singleResult()).isNotNull();
    }

    @Test
    public void testQueryByProcessDefinitionIds() {
        final ProcessDefinition processDefinition1 = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY)
                .singleResult();
        final ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY_2)
                .singleResult();

        final Set<String> processDefinitionIdSet = new HashSet<>(2);
        processDefinitionIdSet.add(processDefinition1.getId());
        processDefinitionIdSet.add(processDefinition2.getId());

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionIds(processDefinitionIdSet);
        assertThat(query.count()).isEqualTo(PROCESS_DEPLOY_COUNT);
        assertThat(query.list()).hasSize(PROCESS_DEPLOY_COUNT);
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByInvalidProcessDefinitionIds() {
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().processDefinitionIds(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().processDefinitionIds(Collections.emptySet()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByProcessDefinitionName() {
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionName(PROCESS_DEFINITION_NAME).count())
                .isEqualTo(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionName(PROCESS_DEFINITION_NAME_2).count())
                .isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
    }

    @Test
    public void testOrQueryByProcessDefinitionName() {
        assertThat(runtimeService.createProcessInstanceQuery().or().processDefinitionName(PROCESS_DEFINITION_NAME).processDefinitionId("undefined").endOr()
                .count()).isEqualTo(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().or().processDefinitionName(PROCESS_DEFINITION_NAME_2).processDefinitionId("undefined").endOr()
                .count()).isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
    }

    @Test
    public void testQueryByInvalidProcessDefinitionName() {
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionName("invalid").singleResult()).isNull();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionName("invalid").count()).isZero();
    }
    
    @Test
    public void testQueryByProcessDefinitionNameLike() {
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionNameLike(PROCESS_DEFINITION_NAME).count())
                .isEqualTo(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionNameLike(PROCESS_DEFINITION_NAME_2).count())
                .isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionNameLike("oneTask%").count())
                .isEqualTo(5);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionNameLike("none%").count())
                .isEqualTo(0);
    }
    
    @Test
    public void testQueryByProcessDefinitionNameLikeIgnoreCase() {
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionNameLikeIgnoreCase(PROCESS_DEFINITION_NAME).count())
                .isEqualTo(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionNameLikeIgnoreCase(PROCESS_DEFINITION_NAME_2).count())
                .isEqualTo(PROCESS_DEFINITION_KEY_2_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionNameLikeIgnoreCase("onetask%").count())
                .isEqualTo(5);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionNameLikeIgnoreCase("none%").count())
                .isEqualTo(0);
    }

    @Test
    public void testQueryByDeploymentId() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().deploymentId(deployment.getId()).list();
        assertThat(instances).hasSize(PROCESS_DEPLOY_COUNT);
        ProcessInstance processInstance = instances.get(0);
        assertThat(processInstance.getDeploymentId()).isEqualTo(deployment.getId());
        assertThat(processInstance.getProcessDefinitionVersion()).isEqualTo(1);
        assertThat(processInstance.getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo("oneTaskProcessName");
        assertThat(processInstance.getProcessDefinitionCategory()).isEqualTo(PROCESS_DEFINITION_CATEGORY);
        assertThat(runtimeService.createProcessInstanceQuery().deploymentId(deployment.getId()).count()).isEqualTo(PROCESS_DEPLOY_COUNT);
    }

    @Test
    public void testQueryByDeploymentIdIn() {
        List<String> deploymentIds = Collections.singletonList(deployment.getId());
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().deploymentIdIn(deploymentIds).list();
        assertThat(instances).hasSize(PROCESS_DEPLOY_COUNT);

        assertThat(instances)
                .extracting(ProcessInstance::getBusinessKey, ProcessInstance::getProcessDefinitionKey, ProcessInstance::getProcessDefinitionName,
                        ProcessInstance::getProcessDefinitionVersion, ProcessInstance::getDeploymentId)
                .as("businessKey, processDefinitionKey, processDefinitionName, processDefinitionVersion, deploymentId")
                .containsExactlyInAnyOrder(
                        tuple("0", PROCESS_DEFINITION_KEY, "oneTaskProcessName", 1, deployment.getId()),
                        tuple("1", PROCESS_DEFINITION_KEY, "oneTaskProcessName", 1, deployment.getId()),
                        tuple("2", PROCESS_DEFINITION_KEY, "oneTaskProcessName", 1, deployment.getId()),
                        tuple("3", PROCESS_DEFINITION_KEY, "oneTaskProcessName", 1, deployment.getId()),
                        tuple("1", PROCESS_DEFINITION_KEY_2, "oneTaskProcess2Name", 1, deployment.getId())
                );

        assertThat(runtimeService.createProcessInstanceQuery().deploymentIdIn(deploymentIds).count()).isEqualTo(PROCESS_DEPLOY_COUNT);

        assertThat(runtimeService.createProcessInstanceQuery().deploymentIdIn(Collections.singletonList("dummy")).list()).isEmpty();

        assertThat(runtimeService.createProcessInstanceQuery()
                .or()
                .processInstanceId("invalid")
                .deploymentIdIn(deploymentIds)
                .endOr()
                .count()).isEqualTo(PROCESS_DEPLOY_COUNT);
    }

    @Test
    public void testOrQueryByDeploymentId() {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().or().deploymentId(deployment.getId()).processDefinitionId("undefined")
                .endOr().list();
        assertThat(instances).hasSize(PROCESS_DEPLOY_COUNT);
        ProcessInstance processInstance = instances.get(0);
        assertThat(processInstance.getDeploymentId()).isEqualTo(deployment.getId());
        assertThat(processInstance.getProcessDefinitionVersion()).isEqualTo(Integer.valueOf(1));
        assertThat(processInstance.getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo("oneTaskProcessName");

        instances = runtimeService.createProcessInstanceQuery()
                .or()
                .deploymentId(deployment.getId())
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .processDefinitionId("undefined")
                .endOr()
                .list();
        assertThat(instances).hasSize(4);

        instances = runtimeService.createProcessInstanceQuery()
                .or()
                .deploymentId(deployment.getId())
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processDefinitionKey("undefined")
                .processDefinitionId("undefined")
                .endOr()
                .list();
        assertThat(instances).isEmpty();

        assertThat(runtimeService.createProcessInstanceQuery().or().deploymentId(deployment.getId()).processDefinitionId("undefined").endOr().count())
                .isEqualTo(PROCESS_DEPLOY_COUNT);

        assertThat(runtimeService.createProcessInstanceQuery()
                .or()
                .deploymentId(deployment.getId())
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .processDefinitionId("undefined")
                .endOr()
                .count()).isEqualTo(4);

        assertThat(runtimeService.createProcessInstanceQuery()
                .or()
                .deploymentId(deployment.getId())
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processDefinitionKey("undefined")
                .processDefinitionId("undefined")
                .endOr()
                .count()).isZero();
    }

    @Test
    public void testOrQueryByDeploymentIdIn() {
        List<String> deploymentIds = new ArrayList<>();
        deploymentIds.add(deployment.getId());
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().or().deploymentIdIn(deploymentIds).processDefinitionId("undefined")
                .endOr().list();
        assertThat(instances).hasSize(PROCESS_DEPLOY_COUNT);

        ProcessInstance processInstance = instances.get(0);
        assertThat(processInstance.getDeploymentId()).isEqualTo(deployment.getId());
        assertThat(processInstance.getProcessDefinitionVersion()).isEqualTo(Integer.valueOf(1));
        assertThat(processInstance.getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
        assertThat(processInstance.getProcessDefinitionName()).isEqualTo("oneTaskProcessName");

        assertThat(runtimeService.createProcessInstanceQuery().or().deploymentIdIn(deploymentIds).processDefinitionId("undefined").endOr().count())
                .isEqualTo(PROCESS_DEPLOY_COUNT);
    }

    @Test
    public void testQueryByInvalidDeploymentId() {
        assertThat(runtimeService.createProcessInstanceQuery().deploymentId("invalid").singleResult()).isNull();
        assertThat(runtimeService.createProcessInstanceQuery().deploymentId("invalid").count()).isZero();
    }

    @Test
    public void testOrQueryByInvalidDeploymentId() {
        assertThat(runtimeService.createProcessInstanceQuery().or().deploymentId("invalid").processDefinitionId("undefined").endOr().singleResult()).isNull();
        assertThat(runtimeService.createProcessInstanceQuery().or().deploymentId("invalid").processDefinitionId("undefined").endOr().count()).isZero();
    }

    @Test
    public void testQueryByInvalidProcessInstanceId() {
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId("I do not exist").singleResult()).isNull();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId("I do not exist").list()).isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testQueryBySuperProcessInstanceId() {
        ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId());
        ProcessInstance subProcessInstance = query.singleResult();
        assertThat(subProcessInstance).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testOrQueryBySuperProcessInstanceId() {
        ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().or().superProcessInstanceId(superProcessInstance.getId())
                .processDefinitionId("undefined").endOr();
        ProcessInstance subProcessInstance = query.singleResult();
        assertThat(subProcessInstance).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/currentActivityTestProcess.bpmn20.xml" })
    public void testQueryByActiveActivityId() {
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("currentActivityProcessTest");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        taskService.complete(task.getId());
        
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("currentActivityProcessTest");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("currentActivityProcessTest");
        
        List<String> queryIds = runtimeService.createProcessInstanceQuery().activeActivityId("task1").list().stream()
            .map(ProcessInstance::getId)
            .collect(Collectors.toList());
        
        assertThat(queryIds.size()).isEqualTo(2);
        assertThat(queryIds.contains(processInstance2.getId())).isTrue();
        assertThat(queryIds.contains(processInstance3.getId())).isTrue();
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance1.getId()).activeActivityId("task1").count()).isZero();
        
        queryIds = runtimeService.createProcessInstanceQuery().activeActivityId("task3").list().stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());
        
        assertThat(queryIds.size()).isEqualTo(1);
        assertThat(queryIds.contains(processInstance1.getId())).isTrue();
        
        Set<String> activityIds = new HashSet<>();
        activityIds.add("task1");
        activityIds.add("task3");
        
        queryIds = runtimeService.createProcessInstanceQuery().activeActivityIds(activityIds).list().stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());
     
        assertThat(queryIds.size()).isEqualTo(3);
        assertThat(queryIds.contains(processInstance1.getId())).isTrue();
        assertThat(queryIds.contains(processInstance2.getId())).isTrue();
        assertThat(queryIds.contains(processInstance3.getId())).isTrue();
        
        activityIds = new HashSet<>();
        activityIds.add("task2");
        activityIds.add("task3");
        
        queryIds = runtimeService.createProcessInstanceQuery().activeActivityIds(activityIds).list().stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());
        
        assertThat(queryIds.size()).isEqualTo(1);
        assertThat(queryIds.contains(processInstance1.getId())).isTrue();
        
        activityIds = new HashSet<>();
        activityIds.add("task2");
        activityIds.add("task4");
        assertThat(runtimeService.createProcessInstanceQuery().activeActivityIds(activityIds).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testQueryByInvolvedUser() {
        ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");
        runtimeService.addUserIdentityLink(superProcessInstance.getId(), "kermit", "specialLink");

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId());
        ProcessInstance subProcessInstance = query.singleResult();
        assertThat(subProcessInstance).isNotNull();
        runtimeService.addUserIdentityLink(subProcessInstance.getId(), "kermit", "anotherLink");

        assertThat(runtimeService.createProcessInstanceQuery().involvedUser("kermit", "specialLink").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().involvedUser("kermit", "specialLink").singleResult().getId())
                .isEqualTo(superProcessInstance.getId());

        assertThat(runtimeService.createProcessInstanceQuery().involvedUser("kermit", "anotherLink").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().involvedUser("kermit", "anotherLink").singleResult().getId())
                .isEqualTo(subProcessInstance.getId());

        assertThat(runtimeService.createProcessInstanceQuery().involvedUser("kermit", "undefined").count()).isZero();

        assertThat(runtimeService.createProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("undefined").endOr().count())
                .isEqualTo(1);
        assertThat(
                runtimeService.createProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("undefined").endOr().singleResult()
                        .getId()).isEqualTo(superProcessInstance.getId());

        assertThat(runtimeService.createProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("subProcessQueryTest").endOr()
                .count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("subProcessQueryTest").endOr()
                .singleResult().getId()).isEqualTo(superProcessInstance.getId());

        assertThat(runtimeService.createProcessInstanceQuery().or().involvedUser("kermit", "undefined").processDefinitionKey("undefined").endOr().count())
                .isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testQueryByInvolvedGroup() {
        ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");
        runtimeService.addGroupIdentityLink(superProcessInstance.getId(), "sales", "specialLink");

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId());
        ProcessInstance subProcessInstance = query.singleResult();
        assertThat(subProcessInstance).isNotNull();
        runtimeService.addGroupIdentityLink(subProcessInstance.getId(), "sales", "anotherLink");

        assertThat(runtimeService.createProcessInstanceQuery().involvedGroup("sales", "specialLink").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().involvedGroup("sales", "specialLink").singleResult().getId())
                .isEqualTo(superProcessInstance.getId());

        assertThat(runtimeService.createProcessInstanceQuery().involvedGroup("sales", "anotherLink").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().involvedGroup("sales", "anotherLink").singleResult().getId())
                .isEqualTo(subProcessInstance.getId());

        assertThat(runtimeService.createProcessInstanceQuery().involvedGroup("sales", "undefined").count()).isZero();

        assertThat(runtimeService.createProcessInstanceQuery().or().involvedGroup("sales", "specialLink").processDefinitionKey("undefined").endOr().count())
                .isEqualTo(1);
        assertThat(
                runtimeService.createProcessInstanceQuery().or().involvedGroup("sales", "specialLink").processDefinitionKey("undefined").endOr().singleResult()
                        .getId()).isEqualTo(superProcessInstance.getId());

        assertThat(runtimeService.createProcessInstanceQuery().or().involvedGroup("sales", "specialLink").processDefinitionKey("subProcessQueryTest").endOr()
                .count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().or().involvedGroup("sales", "specialLink").processDefinitionKey("subProcessQueryTest").endOr()
                .singleResult().getId()).isEqualTo(superProcessInstance.getId());

        assertThat(runtimeService.createProcessInstanceQuery().or().involvedGroup("sales", "undefined").processDefinitionKey("undefined").endOr().count())
                .isZero();
    }

    @Test
    public void testQueryByInvalidSuperProcessInstanceId() {
        assertThat(runtimeService.createProcessInstanceQuery().superProcessInstanceId("invalid").singleResult()).isNull();
        assertThat(runtimeService.createProcessInstanceQuery().superProcessInstanceId("invalid").list()).isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testQueryBySubProcessInstanceId() {
        ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId())
                .isEqualTo(superProcessInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testOrQueryBySubProcessInstanceId() {
        ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("subProcessQueryTest");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().or().superProcessInstanceId(superProcessInstance.getId())
                .processDefinitionId("undefined").singleResult();
        assertThat(subProcessInstance).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().or().subProcessInstanceId(subProcessInstance.getId()).processDefinitionId("undefined")
                .singleResult()
                .getId()).isEqualTo(superProcessInstance.getId());
    }

    @Test
    public void testQueryByInvalidSubProcessInstanceId() {
        assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId("invalid").singleResult()).isNull();
        assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId("invalid").list()).isEmpty();
    }

    // Nested subprocess make the query complexer, hence this test
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcessWithNestedSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testQueryBySuperProcessInstanceIdNested() {
        ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        ProcessInstance nestedSubProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(subProcessInstance.getId())
                .singleResult();
        assertThat(nestedSubProcessInstance).isNotNull();
    }

    // Nested subprocess make the query complexer, hence this test
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcessWithNestedSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testQueryBySubProcessInstanceIdNested() {
        ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId()).singleResult();
        assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId())
                .isEqualTo(superProcessInstance.getId());

        ProcessInstance nestedSubProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(subProcessInstance.getId())
                .singleResult();
        assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(nestedSubProcessInstance.getId()).singleResult().getId())
                .isEqualTo(subProcessInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/superProcessWithNestedSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testQueryWithExcludeSubprocesses() {
        ProcessInstance superProcessInstance = runtimeService.startProcessInstanceByKey("nestedSubProcessQueryTest");
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(superProcessInstance.getId()).singleResult();
        ProcessInstance nestedSubProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(subProcessInstance.getId())
                .singleResult();

        List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().excludeSubprocesses(true).list();
        assertThat(instanceList).hasSize(6);

        boolean superProcessFound = false;
        boolean subProcessFound = false;
        boolean nestedSubProcessFound = false;
        for (ProcessInstance processInstance : instanceList) {
            if (processInstance.getId().equals(superProcessInstance.getId())) {
                superProcessFound = true;
            } else if (processInstance.getId().equals(subProcessInstance.getId())) {
                subProcessFound = true;
            } else if (processInstance.getId().equals(nestedSubProcessInstance.getId())) {
                nestedSubProcessFound = true;
            }
        }
        assertThat(superProcessFound).isTrue();
        assertThat(subProcessFound).isFalse();
        assertThat(nestedSubProcessFound).isFalse();

        instanceList = runtimeService.createProcessInstanceQuery().excludeSubprocesses(false).list();
        assertThat(instanceList).hasSize(8);
    }

    @Test
    public void testQueryPaging() {
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).count())
                .isEqualTo(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).listPage(0, 2)).hasSize(2);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).listPage(1, 3)).hasSize(3);
    }

    @Test
    public void testQuerySorting() {
        assertThat(runtimeService.createProcessInstanceQuery().orderByProcessInstanceId().asc().list()).hasSize(PROCESS_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().orderByProcessDefinitionId().asc().list()).hasSize(PROCESS_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().orderByProcessDefinitionKey().asc().list()).hasSize(PROCESS_DEPLOY_COUNT);

        assertThat(runtimeService.createProcessInstanceQuery().orderByProcessInstanceId().desc().list()).hasSize(PROCESS_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().orderByProcessDefinitionId().desc().list()).hasSize(PROCESS_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().orderByProcessDefinitionKey().desc().list()).hasSize(PROCESS_DEPLOY_COUNT);

        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).orderByProcessInstanceId().asc().list())
                .hasSize(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).orderByProcessInstanceId().desc().list())
                .hasSize(PROCESS_DEFINITION_KEY_DEPLOY_COUNT);
    }

    @Test
    public void testQueryInvalidSorting() {
        // asc - desc not called -> exception
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().orderByProcessDefinitionId().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryStringVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("stringVar", "abcdef");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("stringVar", "abcdef");
        vars.put("stringVar2", "ghijkl");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("stringVar", "azerty");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Test EQUAL on single string variable, should result in 2 matches
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("stringVar", "abcdef");
        List<ProcessInstance> processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        // Test EQUAL on two string variables, should result in single match
        query = runtimeService.createProcessInstanceQuery().variableValueEquals("stringVar", "abcdef").variableValueEquals("stringVar2", "ghijkl");
        ProcessInstance resultInstance = query.singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

        // Test NOT_EQUAL, should return only 1 resultInstance
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("stringVar", "abcdef").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN, should return only matching 'azerty'
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("stringVar", "abcdef").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("stringVar", "z").singleResult();
        assertThat(resultInstance).isNull();

        // Test GREATER_THAN_OR_EQUAL, should return 3 results
        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "abcdef").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "z").count()).isZero();

        // Test LESS_THAN, should return 2 results
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("stringVar", "abcdeg").list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("stringVar", "abcdef").count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "abcdef").list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "aa").count()).isZero();

        // Test LIKE
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "azert%").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "%y").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "%zer%").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "a%").count()).isEqualTo(3);
        assertThat(runtimeService.createProcessInstanceQuery().variableValueLike("stringVar", "%x%").count()).isZero();

        // Test value-only matching
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("azerty").singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        processInstances = runtimeService.createProcessInstanceQuery().variableValueEquals("abcdef").list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("notmatchinganyvalues").singleResult();
        assertThat(resultInstance).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryLongVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("longVar", 12345L);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("longVar", 12345L);
        vars.put("longVar2", 67890L);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("longVar", 55555L);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on single long variable, should result in 2 matches
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("longVar", 12345L);
        List<ProcessInstance> processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        // Query on two long variables, should result in single match
        query = runtimeService.createProcessInstanceQuery().variableValueEquals("longVar", 12345L).variableValueEquals("longVar2", 67890L);
        ProcessInstance resultInstance = query.singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("longVar", 999L).singleResult();
        assertThat(resultInstance).isNull();

        // Test NOT_EQUALS
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("longVar", 12345L).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("longVar", 44444L).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("longVar", 55555L).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("longVar", 1L).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar", 44444L).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar", 55555L).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("longVar", 1L).count()).isEqualTo(3);

        // Test LESS_THAN
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("longVar", 55555L).list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("longVar", 12345L).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("longVar", 66666L).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("longVar", 55555L).list();
        assertThat(processInstances).hasSize(3);

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("longVar", 12344L).count()).isZero();

        // Test value-only matching
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals(55555L).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        processInstances = runtimeService.createProcessInstanceQuery().variableValueEquals(12345L).list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals(999L).singleResult();
        assertThat(resultInstance).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryDoubleVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("doubleVar", 12345.6789);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("doubleVar", 12345.6789);
        vars.put("doubleVar2", 9876.54321);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("doubleVar", 55555.5555);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on single double variable, should result in 2 matches
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("doubleVar", 12345.6789);
        List<ProcessInstance> processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        // Query on two double variables, should result in single value
        query = runtimeService.createProcessInstanceQuery().variableValueEquals("doubleVar", 12345.6789).variableValueEquals("doubleVar2", 9876.54321);
        ProcessInstance resultInstance = query.singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("doubleVar", 9999.99).singleResult();
        assertThat(resultInstance).isNull();

        // Test NOT_EQUALS
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("doubleVar", 12345.6789).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("doubleVar", 44444.4444).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("doubleVar", 55555.5555).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("doubleVar", 1.234).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("doubleVar", 44444.4444).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("doubleVar", 55555.5555).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("doubleVar", 1.234).count()).isEqualTo(3);

        // Test LESS_THAN
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("doubleVar", 55555.5555).list();
        assertThat(processInstances).hasSize(2);

        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("doubleVar", 12345.6789).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("doubleVar", 66666.6666).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("doubleVar", 55555.5555).list();
        assertThat(processInstances).hasSize(3);

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("doubleVar", 12344.6789).count()).isZero();

        // Test value-only matching
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals(55555.5555).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        processInstances = runtimeService.createProcessInstanceQuery().variableValueEquals(12345.6789).list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals(999.999).singleResult();
        assertThat(resultInstance).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryIntegerVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("integerVar", 12345);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("integerVar", 12345);
        vars.put("integerVar2", 67890);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("integerVar", 55555);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on single integer variable, should result in 2 matches
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("integerVar", 12345);
        List<ProcessInstance> processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        // Query on two integer variables, should result in single value
        query = runtimeService.createProcessInstanceQuery().variableValueEquals("integerVar", 12345).variableValueEquals("integerVar2", 67890);
        ProcessInstance resultInstance = query.singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("integerVar", 9999).singleResult();
        assertThat(resultInstance).isNull();

        // Test NOT_EQUALS
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("integerVar", 12345).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("integerVar", 44444).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("integerVar", 55555).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("integerVar", 1).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("integerVar", 44444).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("integerVar", 55555).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("integerVar", 1).count()).isEqualTo(3);

        // Test LESS_THAN
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("integerVar", 55555).list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("integerVar", 12345).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("integerVar", 66666).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("integerVar", 55555).list();
        assertThat(processInstances).hasSize(3);

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("integerVar", 12344).count()).isZero();

        // Test value-only matching
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals(55555).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        processInstances = runtimeService.createProcessInstanceQuery().variableValueEquals(12345).list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals(9999).singleResult();
        assertThat(resultInstance).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testOrQueryIntegerVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("integerVar", 12345);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("integerVar", 12345);
        vars.put("integerVar2", 67890);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("integerVar", 55555);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on single integer variable, should result in 2 matches
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().or().variableValueEquals("integerVar", 12345).processDefinitionId("undefined")
                .endOr();
        List<ProcessInstance> processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        query = runtimeService.createProcessInstanceQuery()
                .or()
                .variableValueEquals("integerVar", 12345)
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processDefinitionKey("oneTaskProcess")
                .processDefinitionId("undefined")
                .endOr();
        processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        // Query on two integer variables, should result in single value
        query = runtimeService.createProcessInstanceQuery().variableValueEquals("integerVar", 12345).or().variableValueEquals("integerVar2", 67890)
                .processDefinitionId("undefined").endOr();
        ProcessInstance resultInstance = query.singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        resultInstance = runtimeService.createProcessInstanceQuery().or().variableValueEquals("integerVar", 9999).processDefinitionId("undefined").endOr()
                .singleResult();
        assertThat(resultInstance).isNull();

        // Test NOT_EQUALS
        resultInstance = runtimeService.createProcessInstanceQuery().or().variableValueNotEquals("integerVar", 12345).processDefinitionId("undefined").endOr()
                .singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        resultInstance = runtimeService.createProcessInstanceQuery().or().variableValueGreaterThan("integerVar", 44444).processDefinitionId("undefined").endOr()
                .singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery()
                .or()
                .variableValueGreaterThan("integerVar", 44444)
                .processDefinitionId("undefined")
                .endOr()
                .or()
                .processDefinitionKey("oneTaskProcess")
                .processDefinitionId("undefined")
                .endOr()
                .singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(
                runtimeService.createProcessInstanceQuery().or().variableValueGreaterThan("integerVar", 55555).processDefinitionId("undefined").endOr().count())
                .isZero();
        assertThat(runtimeService.createProcessInstanceQuery().or().variableValueGreaterThan("integerVar", 1).processDefinitionId("undefined").endOr().count())
                .isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        resultInstance = runtimeService.createProcessInstanceQuery().or().variableValueGreaterThanOrEqual("integerVar", 44444).processDefinitionId("undefined")
                .endOr().singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().or().variableValueGreaterThanOrEqual("integerVar", 55555).processDefinitionId("undefined")
                .endOr().singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().or().variableValueGreaterThanOrEqual("integerVar", 1).processDefinitionId("undefined").endOr()
                .count()).isEqualTo(3);

        // Test LESS_THAN
        processInstances = runtimeService.createProcessInstanceQuery().or().variableValueLessThan("integerVar", 55555).processDefinitionId("undefined").endOr()
                .list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createProcessInstanceQuery().or().variableValueLessThan("integerVar", 12345).processDefinitionId("undefined").endOr().count())
                .isZero();
        assertThat(runtimeService.createProcessInstanceQuery().or().variableValueLessThan("integerVar", 66666).processDefinitionId("undefined").endOr().count())
                .isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        processInstances = runtimeService.createProcessInstanceQuery().or().variableValueLessThanOrEqual("integerVar", 55555).processDefinitionId("undefined")
                .endOr().list();
        assertThat(processInstances).hasSize(3);

        assertThat(runtimeService.createProcessInstanceQuery().or().variableValueLessThanOrEqual("integerVar", 12344).processDefinitionId("undefined").endOr()
                .count()).isZero();

        // Test value-only matching
        resultInstance = runtimeService.createProcessInstanceQuery().or().variableValueEquals(55555).processDefinitionId("undefined").endOr().singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        processInstances = runtimeService.createProcessInstanceQuery().or().variableValueEquals(12345).processDefinitionId("undefined").endOr().list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().or().variableValueEquals(9999).processDefinitionId("undefined").endOr().singleResult();
        assertThat(resultInstance).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryShortVariable() {
        Map<String, Object> vars = new HashMap<>();
        short shortVar = 1234;
        vars.put("shortVar", shortVar);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        short shortVar2 = 6789;
        vars = new HashMap<>();
        vars.put("shortVar", shortVar);
        vars.put("shortVar2", shortVar2);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("shortVar", (short) 5555);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on single short variable, should result in 2 matches
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("shortVar", shortVar);
        List<ProcessInstance> processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        // Query on two short variables, should result in single value
        query = runtimeService.createProcessInstanceQuery().variableValueEquals("shortVar", shortVar).variableValueEquals("shortVar2", shortVar2);
        ProcessInstance resultInstance = query.singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        short unexistingValue = (short) 9999;
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("shortVar", unexistingValue).singleResult();
        assertThat(resultInstance).isNull();

        // Test NOT_EQUALS
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("shortVar", (short) 1234).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("shortVar", (short) 4444).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("shortVar", (short) 5555).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("shortVar", (short) 1).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("shortVar", (short) 4444).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("shortVar", (short) 5555).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("shortVar", (short) 1).count()).isEqualTo(3);

        // Test LESS_THAN
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("shortVar", (short) 5555).list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("shortVar", (short) 1234).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("shortVar", (short) 6666).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("shortVar", (short) 5555).list();
        assertThat(processInstances).hasSize(3);

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("shortVar", (short) 1233).count()).isZero();

        // Test value-only matching
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals((short) 5555).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        processInstances = runtimeService.createProcessInstanceQuery().variableValueEquals((short) 1234).list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals((short) 999).singleResult();
        assertThat(resultInstance).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryDateVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        Date date1 = Calendar.getInstance().getTime();
        vars.put("dateVar", date1);

        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.SECOND, 1);

        Date date2 = cal2.getTime();
        vars = new HashMap<>();
        vars.put("dateVar", date1);
        vars.put("dateVar2", date2);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 1);
        vars = new HashMap<>();
        vars.put("dateVar", nextYear.getTime());
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Calendar nextMonth = Calendar.getInstance();
        nextMonth.add(Calendar.MONTH, 1);

        Calendar twoYearsLater = Calendar.getInstance();
        twoYearsLater.add(Calendar.YEAR, 2);

        Calendar oneYearAgo = Calendar.getInstance();
        oneYearAgo.add(Calendar.YEAR, -1);

        // Query on single short variable, should result in 2 matches
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("dateVar", date1);
        List<ProcessInstance> processInstances = query.list();
        assertThat(processInstances).hasSize(2);

        // Query on two short variables, should result in single value
        query = runtimeService.createProcessInstanceQuery().variableValueEquals("dateVar", date1).variableValueEquals("dateVar2", date2);
        ProcessInstance resultInstance = query.singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        Date unexistingDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/01/1989 12:00:00");
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals("dateVar", unexistingDate).singleResult();
        assertThat(resultInstance).isNull();

        // Test NOT_EQUALS
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("dateVar", date1).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("dateVar", nextMonth.getTime()).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("dateVar", nextYear.getTime()).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThan("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextMonth.getTime()).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextYear.getTime()).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

        // Test LESS_THAN
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThan("dateVar", nextYear.getTime()).list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("dateVar", date1).count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThan("dateVar", twoYearsLater.getTime()).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        processInstances = runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", nextYear.getTime()).list();
        assertThat(processInstances).hasSize(3);

        assertThat(runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isZero();

        // Test value-only matching
        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals(nextYear.getTime()).singleResult();
        assertThat(resultInstance).isNotNull();
        assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

        processInstances = runtimeService.createProcessInstanceQuery().variableValueEquals(date1).list();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        resultInstance = runtimeService.createProcessInstanceQuery().variableValueEquals(twoYearsLater.getTime()).singleResult();
        assertThat(resultInstance).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testBooleanVariable() throws Exception {

        // TEST EQUALS
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("booleanVar", true);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("booleanVar", false);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().variableValueEquals("booleanVar", true).list();
        assertThat(instances).isNotNull();
        assertThat(instances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance1.getId());

        instances = runtimeService.createProcessInstanceQuery().variableValueEquals("booleanVar", false).list();
        assertThat(instances).isNotNull();
        assertThat(instances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance2.getId());

        // TEST NOT_EQUALS
        instances = runtimeService.createProcessInstanceQuery().variableValueNotEquals("booleanVar", true).list();
        assertThat(instances).isNotNull();
        assertThat(instances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance2.getId());

        instances = runtimeService.createProcessInstanceQuery().variableValueNotEquals("booleanVar", false).list();
        assertThat(instances).isNotNull();
        assertThat(instances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance1.getId());

        // Test value-only matching
        instances = runtimeService.createProcessInstanceQuery().variableValueEquals(true).list();
        assertThat(instances).isNotNull();
        assertThat(instances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance1.getId());

        // Test unsupported operations
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThan("booleanVar", true))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'greater than' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("booleanVar", true))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThan("booleanVar", true))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'less than' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("booleanVar", true))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'less than or equal' condition");

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");

        // Test value-only matching, no results present
        instances = runtimeService.createProcessInstanceQuery().variableValueEquals(true).list();
        assertThat(instances).isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryVariablesUpdatedToNullValue() {
        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "coca-cola");
        variables.put("dateVar", new Date());
        variables.put("booleanVar", true);
        variables.put("nullVar", null);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("longVar", null).variableValueEquals("shortVar", null)
                .variableValueEquals("integerVar", null)
                .variableValueEquals("stringVar", null).variableValueEquals("booleanVar", null).variableValueEquals("dateVar", null);

        ProcessInstanceQuery notQuery = runtimeService.createProcessInstanceQuery().variableValueNotEquals("longVar", null)
                .variableValueNotEquals("shortVar", null)
                .variableValueNotEquals("integerVar", null).variableValueNotEquals("stringVar", null).variableValueNotEquals("booleanVar", null)
                .variableValueNotEquals("dateVar", null);

        assertThat(query.singleResult()).isNull();
        assertThat(notQuery.singleResult()).isNotNull();

        // Set all existing variables values to null
        runtimeService.setVariable(processInstance.getId(), "longVar", null);
        runtimeService.setVariable(processInstance.getId(), "shortVar", null);
        runtimeService.setVariable(processInstance.getId(), "integerVar", null);
        runtimeService.setVariable(processInstance.getId(), "stringVar", null);
        runtimeService.setVariable(processInstance.getId(), "dateVar", null);
        runtimeService.setVariable(processInstance.getId(), "nullVar", null);
        runtimeService.setVariable(processInstance.getId(), "booleanVar", null);

        Execution queryResult = query.singleResult();
        assertThat(queryResult).isNotNull();
        assertThat(queryResult.getId()).isEqualTo(processInstance.getId());
        assertThat(notQuery.singleResult()).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryNullVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("nullVar", null);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("nullVar", "notnull");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("nullVarLong", "notnull");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("nullVarDouble", "notnull");
        ProcessInstance processInstance4 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("nullVarByte", "testbytes".getBytes());
        ProcessInstance processInstance5 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on null value, should return one value
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("nullVar", null);
        List<ProcessInstance> processInstances = query.list();
        assertThat(processInstances).isNotNull();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance1.getId());

        // Test NOT_EQUALS null
        assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("nullVar", null).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("nullVarLong", null).count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("nullVarDouble", null).count()).isEqualTo(1);
        // When a byte-array reference is present, the variable is not considered null
        assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("nullVarByte", null).count()).isEqualTo(1);

        // Test value-only
        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals(null).count()).isEqualTo(1);

        // All other variable queries with null should throw exception
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThan("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'greater than' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThan("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'less than' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'less than or equal' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLike("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Only string values can be used with 'like' condition");

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance4.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance5.getId(), "test");

        // Test value-only, no more null-variables exist
        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals(null).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryEqualsIgnoreCase() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mixed", "AbCdEfG");
        vars.put("upper", "ABCDEFG");
        vars.put("lower", "abcdefg");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        ProcessInstance instance = runtimeService.createProcessInstanceQuery().variableValueEqualsIgnoreCase("mixed", "abcdefg").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        instance = runtimeService.createProcessInstanceQuery().variableValueEqualsIgnoreCase("lower", "abcdefg").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        instance = runtimeService.createProcessInstanceQuery().variableValueEqualsIgnoreCase("upper", "abcdefg").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        // Pass in non-lower-case string
        instance = runtimeService.createProcessInstanceQuery().variableValueEqualsIgnoreCase("upper", "ABCdefg").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        // Pass in null-value, should cause exception
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueEqualsIgnoreCase("upper", null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("value is null");

        // Pass in null name, should cause exception
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueEqualsIgnoreCase(null, "abcdefg").singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");

        // Test NOT equals
        instance = runtimeService.createProcessInstanceQuery().variableValueNotEqualsIgnoreCase("upper", "UIOP").singleResult();
        assertThat(instance).isNotNull();

        // Should return result when using "ABCdefg" case-insensitive while
        // normal not-equals won't
        instance = runtimeService.createProcessInstanceQuery().variableValueNotEqualsIgnoreCase("upper", "ABCdefg").singleResult();
        assertThat(instance).isNull();
        instance = runtimeService.createProcessInstanceQuery().variableValueNotEquals("upper", "ABCdefg").singleResult();
        assertThat(instance).isNotNull();

        // Pass in null-value, should cause exception
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueNotEqualsIgnoreCase("upper", null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("value is null");

        // Pass in null name, should cause exception
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueNotEqualsIgnoreCase(null, "abcdefg").singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryLikeIgnoreCase() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mixed", "AbCdEfG");
        vars.put("upper", "ABCDEFG");
        vars.put("lower", "abcdefg");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        ProcessInstance instance = runtimeService.createProcessInstanceQuery().variableValueLikeIgnoreCase("mixed", "abcd%").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        instance = runtimeService.createProcessInstanceQuery().variableValueLikeIgnoreCase("lower", "abcde%").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        instance = runtimeService.createProcessInstanceQuery().variableValueLikeIgnoreCase("upper", "abcd%").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        // Pass in non-lower-case string
        instance = runtimeService.createProcessInstanceQuery().variableValueLikeIgnoreCase("upper", "ABCde%").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        // Pass in null-value, should cause exception
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLikeIgnoreCase("upper", null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("value is null");

        // Pass in null name, should cause exception
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLikeIgnoreCase(null, "abcdefg").singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryInvalidTypes() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("bytesVar", "test".getBytes());
        vars.put("serializableVar", new DummySerializable());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueEquals("bytesVar", "test".getBytes()).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Variables of type ByteArray cannot be used to query");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueEquals("serializableVar", new DummySerializable()).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Variables of type ByteArray cannot be used to query");

        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    }

    @Test
    public void testQueryVariablesNullNameArgument() {
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueEquals(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueNotEquals(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThan(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThan(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLike(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryAllVariableTypes() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("nullVar", null);
        vars.put("stringVar", "string");
        vars.put("longVar", 10L);
        vars.put("doubleVar", 1.2);
        vars.put("integerVar", 1234);
        vars.put("booleanVar", true);
        vars.put("shortVar", (short) 123);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().variableValueEquals("nullVar", null).variableValueEquals("stringVar", "string")
                .variableValueEquals("longVar", 10L)
                .variableValueEquals("doubleVar", 1.2).variableValueEquals("integerVar", 1234).variableValueEquals("booleanVar", true)
                .variableValueEquals("shortVar", (short) 123);

        List<ProcessInstance> processInstances = query.list();
        assertThat(processInstances).isNotNull();
        assertThat(processInstances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance.getId());

        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testClashingValues() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("var", 1234L);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("var", 1234);

        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars2);

        List<ProcessInstance> foundInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess")
                .variableValueEquals("var", 1234L).list();

        assertThat(foundInstances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance.getId());

        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testVariableExistsQuery() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mixed", "AbCdEfG");
        vars.put("upper", "ABCDEFG");
        vars.put("lower", "abcdefg");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        ProcessInstance instance = runtimeService.createProcessInstanceQuery().variableExists("mixed").singleResult();
        assertThat(instance).isNotNull();
        assertThat(instance.getId()).isEqualTo(processInstance1.getId());

        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().variableNotExists("lower").list();
        assertThat(instances).hasSize(5);

        instances = runtimeService.createProcessInstanceQuery().variableExists("lower").variableValueEquals("upper", "ABCDEFG").list();
        assertThat(instances).hasSize(1);

        instances = runtimeService.createProcessInstanceQuery().or().variableExists("mixed").variableValueEquals("upper", "ABCDEFG").endOr().list();
        assertThat(instances).hasSize(1);

        instances = runtimeService.createProcessInstanceQuery().or().variableNotExists("mixed").variableValueEquals("upper", "ABCDEFG").endOr().list();
        assertThat(instances).hasSize(6);

        instances = runtimeService.createProcessInstanceQuery().or().variableNotExists("mixed").endOr().or().variableValueEquals("upper", "ABCDEFG").endOr()
                .list();
        assertThat(instances).isEmpty();
    }

    @Test
    public void testQueryByProcessInstanceIds() {
        Set<String> processInstanceIds = new HashSet<>(this.processInstanceIds);

        // start an instance that will not be part of the query
        runtimeService.startProcessInstanceByKey("oneTaskProcess2", "2");

        ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceIds(processInstanceIds);
        assertThat(processInstanceQuery.count()).isEqualTo(5);

        List<ProcessInstance> processInstances = processInstanceQuery.list();
        assertThat(processInstances).hasSize(5);

        for (ProcessInstance processInstance : processInstances) {
            assertThat(processInstanceIds).contains(processInstance.getId());
        }
    }

    @Test
    public void testQueryByProcessInstanceIdsEmpty() {
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().processInstanceIds(new HashSet<>()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Set of process instance ids is empty");
    }

    @Test
    public void testQueryByProcessInstanceIdsNull() {
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().processInstanceIds(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Set of process instance ids is null");
    }

    @Test
    public void testNativeQuery() {
        // just test that the query will be constructed and executed, details
        // are tested in the TaskQueryTest
        assertThat(managementService.getTableName(ProcessInstance.class, false)).isEqualTo("ACT_RU_EXECUTION");

        long piCount = runtimeService.createProcessInstanceQuery().count();

        // There are 2 executions for each process instance
        assertThat(runtimeService.createNativeProcessInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(ProcessInstance.class)).list())
                .hasSize((int) piCount * 2);
        assertThat(
                runtimeService.createNativeProcessInstanceQuery().sql("SELECT count(*) FROM " + managementService.getTableName(ProcessInstance.class)).count())
                .isEqualTo(piCount * 2);
    }

    /**
     * Test confirming fix for ACT-1731
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testIncludeBinaryVariables() throws Exception {
        // Start process with a binary variable
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("oneTaskProcess", Collections.singletonMap("binaryVariable", (Object) "It is I, le binary".getBytes()));

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables().singleResult();
        assertThat(processInstance).isNotNull();
        // Query process, including variables
        byte[] bytes = (byte[]) processInstance.getProcessVariables().get("binaryVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");
    }

    @Test
    public void testNativeQueryPaging() {
        assertThat(
                runtimeService.createNativeProcessInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(ProcessInstance.class)).listPage(0, 5))
                .hasSize(5);
    }

    @Test
    public void testLocalizeProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<ProcessInstance> processes = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processes)
                .extracting(ProcessInstance::getName, ProcessInstance::getDescription)
                .containsExactly(tuple(null, null));

        ObjectNode infoNode = dynamicBpmnService.getProcessDefinitionInfo(processInstance.getProcessDefinitionId());
        dynamicBpmnService.changeLocalizationName("en-GB", "oneTaskProcess", "The One org.flowable.task.service.Task Process 'en-GB' localized name", infoNode);
        dynamicBpmnService
                .changeLocalizationDescription("en-GB", "oneTaskProcess", "The One org.flowable.task.service.Task Process 'en-GB' localized description",
                        infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        dynamicBpmnService.changeLocalizationName("en", "oneTaskProcess", "The One org.flowable.task.service.Task Process 'en' localized name", infoNode);
        dynamicBpmnService
                .changeLocalizationDescription("en", "oneTaskProcess", "The One org.flowable.task.service.Task Process 'en' localized description", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        processes = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processes)
                .extracting(ProcessInstance::getName, ProcessInstance::getDescription)
                .containsExactly(tuple(null, null));

        processes = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).locale("es").list();
        assertThat(processes)
                .extracting(ProcessInstance::getName, ProcessInstance::getDescription)
                .containsExactly(tuple("Nombre del proceso", "Descripción del proceso"));

        processes = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).locale("en-GB").list();
        assertThat(processes)
                .extracting(ProcessInstance::getName, ProcessInstance::getDescription)
                .containsExactly(tuple("The One org.flowable.task.service.Task Process 'en-GB' localized name",
                        "The One org.flowable.task.service.Task Process 'en-GB' localized description"));

        processes = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).listPage(0, 10);
        assertThat(processes)
                .extracting(ProcessInstance::getName, ProcessInstance::getDescription)
                .containsExactly(tuple(null, null));

        processes = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).locale("es").listPage(0, 10);
        assertThat(processes)
                .extracting(ProcessInstance::getName, ProcessInstance::getDescription)
                .containsExactly(tuple("Nombre del proceso", "Descripción del proceso"));

        processes = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).locale("en-GB").listPage(0, 10);
        assertThat(processes)
                .extracting(ProcessInstance::getName, ProcessInstance::getDescription)
                .containsExactly(tuple("The One org.flowable.task.service.Task Process 'en-GB' localized name",
                        "The One org.flowable.task.service.Task Process 'en-GB' localized description"));

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance.getName()).isNull();
        assertThat(processInstance.getDescription()).isNull();

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).locale("es").singleResult();
        assertThat(processInstance.getName()).isEqualTo("Nombre del proceso");
        assertThat(processInstance.getDescription()).isEqualTo("Descripción del proceso");

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).locale("en-GB").singleResult();
        assertThat(processInstance.getName()).isEqualTo("The One org.flowable.task.service.Task Process 'en-GB' localized name");
        assertThat(processInstance.getDescription()).isEqualTo("The One org.flowable.task.service.Task Process 'en-GB' localized description");

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance.getName()).isNull();
        assertThat(processInstance.getDescription()).isNull();

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).locale("en").singleResult();
        assertThat(processInstance.getName()).isEqualTo("The One org.flowable.task.service.Task Process 'en' localized name");
        assertThat(processInstance.getDescription()).isEqualTo("The One org.flowable.task.service.Task Process 'en' localized description");

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).locale("en-AU").withLocalizationFallback()
                .singleResult();
        assertThat(processInstance.getName()).isEqualTo("The One org.flowable.task.service.Task Process 'en' localized name");
        assertThat(processInstance.getDescription()).isEqualTo("The One org.flowable.task.service.Task Process 'en' localized description");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryStartedBefore() throws Exception {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date noon = calendar.getTime();

        processEngineConfiguration.getClock().setCurrentTime(noon);

        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date hourLater = calendar.getTime();

        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().startedBefore(hourLater).list();

        assertThat(processInstances).hasSize(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryStartedAfter() throws Exception {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2030);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date noon = calendar.getTime();

        processEngineConfiguration.getClock().setCurrentTime(noon);

        calendar.add(Calendar.HOUR_OF_DAY, -1);
        Date hourEarlier = calendar.getTime();

        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().startedAfter(hourEarlier).list();

        assertThat(processInstances).hasSize(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryStartedBy() throws Exception {
        final String authenticatedUser = "user1";
        identityService.setAuthenticatedUserId(authenticatedUser);
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().startedBy(authenticatedUser).list();

        assertThat(processInstances).hasSize(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryOrderedByStartTime() throws Exception {
        Instant now = Instant.now();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(now));
        String nowInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess", "now").getId();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(now.minus(1, ChronoUnit.HOURS)));
        String nowMinus1InstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess", "now").getId();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(now.plus(1, ChronoUnit.HOURS)));
        String nowPlus1InstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess", "now").getId();

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("now").orderByStartTime().asc().list())
                .extracting(ProcessInstance::getId)
                .as("ascending order by startTime")
                .containsExactly(nowMinus1InstanceId, nowInstanceId, nowPlus1InstanceId);

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("now").orderByStartTime().desc().list())
                .extracting(ProcessInstance::getId)
                .as("descending order by startTime")
                .containsExactly(nowPlus1InstanceId, nowInstanceId, nowMinus1InstanceId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryByInvalidCallbackId() {
        String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess", "now").getId();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).processInstanceCallbackId("foo").list()).isNullOrEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryByCallbackIds() {
        String processInstanceId = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").callbackId("processOneId").start()
                .getId();
        String processInstanceId2 = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").callbackId("someOtherCallBack").start()
                .getId();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceCallbackIds(Set.of("someId", "processOneId")).list()).extracting(
                        ProcessInstance::getId)
                .containsExactly(processInstanceId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryByInvalidCallbackType() {
        String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess", "now").getId();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).processInstanceCallbackType("foo").list()).isNullOrEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryByReferenceId() {
        String processInstanceId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .referenceId("testReferenceId")
                .start()
                .getId();

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceReferenceId("testReferenceId").list())
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstanceId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryByInvalidReferenceId() {
        String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess", "now").getId();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).processInstanceReferenceId("foo").list()).isNullOrEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryByReferenceType() {
        String processInstanceId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .referenceType("testReferenceType")
                .start()
                .getId();

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceReferenceType("testReferenceType").list())
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstanceId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryByInvalidReferenceType() {
        String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess", "now").getId();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).processInstanceReferenceType("foo").list()).isNullOrEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryByReferenceIdAndType() {
        String[] processInstanceIds = new String[6];
        for (int i = 0; i < processInstanceIds.length; i++) {
            String processInstanceId = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneTaskProcess")
                    .referenceId("testReferenceId")
                    .referenceType("testReferenceType")
                    .start()
                    .getId();
            processInstanceIds[i] = processInstanceId;
        }

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceReferenceId("testReferenceId").processInstanceReferenceType("testReferenceType")
                .list())
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstanceIds);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryVariableValueEqualsAndNotEquals() {
        ProcessInstance processWithStringValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        ProcessInstance processWithNullValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With null value")
                .variable("var", null)
                .start();

        ProcessInstance processWithLongValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With long value")
                .variable("var", 100L)
                .start();

        ProcessInstance processWithDoubleValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("var", "TEST").list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", processWithNullValue.getId()),
                        tuple("With long value", processWithLongValue.getId()),
                        tuple("With double value", processWithDoubleValue.getId())
                );

        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("var", "TEST").list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", processWithStringValue.getId())
                );

        assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("var", 100L).list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", processWithStringValue.getId()),
                        tuple("With null value", processWithNullValue.getId()),
                        tuple("With double value", processWithDoubleValue.getId())
                );

        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("var", 100L).list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With long value", processWithLongValue.getId())
                );

        assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("var", 45.55).list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", processWithStringValue.getId()),
                        tuple("With null value", processWithNullValue.getId()),
                        tuple("With long value", processWithLongValue.getId())
                );

        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("var", 45.55).list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With double value", processWithDoubleValue.getId())
                );

        assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEquals("var", "test").list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", processWithStringValue.getId()),
                        tuple("With null value", processWithNullValue.getId()),
                        tuple("With long value", processWithLongValue.getId()),
                        tuple("With double value", processWithDoubleValue.getId())
                );

        assertThat(runtimeService.createProcessInstanceQuery().variableValueNotEqualsIgnoreCase("var", "test").list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", processWithNullValue.getId()),
                        tuple("With long value", processWithLongValue.getId()),
                        tuple("With double value", processWithDoubleValue.getId())
                );

        assertThat(runtimeService.createProcessInstanceQuery().variableValueEquals("var", "test").list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .isEmpty();

        assertThat(runtimeService.createProcessInstanceQuery().variableValueEqualsIgnoreCase("var", "test").list())
                .extracting(ProcessInstance::getName, ProcessInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", processWithStringValue.getId())
                );
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/simpleParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/simpleInnerCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/simpleProcessWithUserTasks.bpmn20.xml",
            "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml"
    })
    public void testQueryByRootScopeId() {
        runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");
        List<String> validationList = runtimeService.createProcessInstanceQuery().list().stream().map(ProcessInstance::getId).toList();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");

        ActivityInstance firstLevelCallActivity1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityId("callActivity1").singleResult();

        ActivityInstance secondLevelCallActivity1_1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity1").singleResult();

        ActivityInstance thirdLevelCallActivity1_1_1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(secondLevelCallActivity1_1.getCalledProcessInstanceId())
                .activityId("callActivity1").singleResult();

        ActivityInstance secondLevelCallActivity1_2 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity2").singleResult();

        ActivityInstance firstLevelCallActivity2 = runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId())
                .activityId("callActivity2").singleResult();

        List<ProcessInstance> result = runtimeService.createProcessInstanceQuery().processInstanceRootScopeId(processInstance.getId()).list();

        assertThat(result)
                .extracting(ProcessInstance::getId, ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder(
                        tuple(firstLevelCallActivity1.getCalledProcessInstanceId(), "simpleInnerParallelCallActivity"),
                        tuple(secondLevelCallActivity1_1.getCalledProcessInstanceId(), "simpleProcessWithUserTaskAndCallActivity"),
                        tuple(thirdLevelCallActivity1_1_1.getCalledProcessInstanceId(), "oneTaskProcess"),
                        tuple(secondLevelCallActivity1_2.getCalledProcessInstanceId(), "oneTaskProcess"),
                        tuple(firstLevelCallActivity2.getCalledProcessInstanceId(), "oneTaskProcess")
                );

        assertThat(result).extracting(ProcessInstance::getId).doesNotContainAnyElementsOf(validationList);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/simpleParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/simpleInnerCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/simpleProcessWithUserTasks.bpmn20.xml",
            "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml"
    })
    public void testQueryByParentScopeId() {
        ProcessInstance validationProcessInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");
        
        List<ProcessInstance> result = runtimeService.createProcessInstanceQuery().processInstanceParentScopeId(processInstance.getId()).list();
        assertThat(result).isEmpty();

        assertThat(result).extracting(ProcessInstance::getId).doesNotContain(
                validationProcessInstance.getId()
        );

        ActivityInstance firstLevelCallActivity1 = runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId())
                .activityId("callActivity1").singleResult();

        ActivityInstance secondLevelCallActivity1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity1").singleResult();
        ActivityInstance secondLevelCallActivity2 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity2").singleResult();

        result = runtimeService.createProcessInstanceQuery().processInstanceParentScopeId(firstLevelCallActivity1.getCalledProcessInstanceId()).list();

        assertThat(result)
                .extracting(ProcessInstance::getId, ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder(
                        tuple(secondLevelCallActivity1.getCalledProcessInstanceId(), "simpleProcessWithUserTaskAndCallActivity"),
                        tuple(secondLevelCallActivity2.getCalledProcessInstanceId(), "oneTaskProcess")
                );

        assertThat(result).extracting(ProcessInstance::getId).doesNotContain(
                validationProcessInstance.getId()
        );
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testIncludeDefinedVariables() {
        ProcessInstance processInstance = runtimeService
                .createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .businessKey("testBusinessKey")
                .variable("testVar", "test value")
                .variable("intVar", 123)
                .start();

        assertThat(processInstance).isNotNull();
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("testBusinessKey").singleResult();
        assertThat(processInstance.getProcessVariables()).isEmpty();

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("testBusinessKey").includeProcessVariables().singleResult();
        assertThat(processInstance.getProcessVariables())
                .containsOnly(
                        entry("testVar", "test value"),
                        entry("intVar", 123)
                );

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("testBusinessKey")
                .includeProcessVariables(List.of("testVar", "dummy")).singleResult();
        assertThat(processInstance.getProcessVariables())
                .containsOnly(
                        entry("testVar", "test value")
                );

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("testBusinessKey")
                .includeProcessVariables(List.of("unknown", "dummy")).singleResult();
        assertThat(processInstance.getProcessVariables())
                .isEmpty();
    }

}
