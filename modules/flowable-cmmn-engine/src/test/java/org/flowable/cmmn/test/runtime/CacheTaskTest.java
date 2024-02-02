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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CacheTaskTest extends FlowableCmmnTestCase {

    @After
    public void tearDown() {
        ServiceCacheTask.reset();
        CacheTaskListener.reset();
        CacheMilestoneListener.reset();
        TestQueryCaseInstanceWithIncludeVariablesDelegate.reset();
    }

    @Test
    @CmmnDeployment
    public void testCaseCache() {
        assertThat(ServiceCacheTask.caseInstanceId).isNull();
        assertThat(ServiceCacheTask.historicCaseInstanceId).isNull();
        assertThat(ServiceCacheTask.planItemInstanceId).isNull();
        assertThat(ServiceCacheTask.historicPlanItemInstanceId).isNull();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricPlanItemInstance planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).singleResult();

            assertThat(ServiceCacheTask.caseInstanceId).isEqualTo(caseInstance.getId());
            assertThat(ServiceCacheTask.historicCaseInstanceId).isEqualTo(caseInstance.getId());
            assertThat(ServiceCacheTask.planItemInstanceId).isEqualTo(planItemInstance.getId());
            assertThat(ServiceCacheTask.historicPlanItemInstanceId).isEqualTo(planItemInstance.getId());
        }
    }

    @Test
    @CmmnDeployment
    public void testTaskListenerCache() {
        assertThat(CacheTaskListener.taskId).isNull();
        assertThat(CacheTaskListener.historicTaskId).isNull();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        assertThat(CacheTaskListener.taskId).isEqualTo(task.getId());
        assertThat(CacheTaskListener.historicTaskId).isEqualTo(task.getId());
    }

    @Test
    @CmmnDeployment
    public void testMilestoneListenerCache() {
        assertThat(CacheMilestoneListener.milestoneInstanceId).isNull();
        assertThat(CacheMilestoneListener.historicMilestoneInstanceId).isNull();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricMilestoneInstance milestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId()).singleResult();
            assertThat(milestoneInstance).isNotNull();

            assertThat(CacheMilestoneListener.milestoneInstanceId).isEqualTo(milestoneInstance.getId());
            assertThat(CacheMilestoneListener.historicMilestoneInstanceId).isEqualTo(milestoneInstance.getId());
        }
    }

    @Test
    @CmmnDeployment
    public void testCaseInstanceQueryWithIncludeVariables() {
        assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.VARIABLES).isNull();
        assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.HISTORIC_VARIABLES).isNull();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCaseInstanceQueryWithIncludeVariables")
                .variable("myVar1", "Hello")
                .variable("myVar2", "World")
                .variable("myVar3", 123)
                .start();

        Map.Entry[] entries = {
                entry("myVar1", "Hello"),
                entry("myVar2", "World"),
                entry("myVar3", 123),
                entry("varFromTheServiceTask", "valueFromTheServiceTask")
        };

        assertThat(caseInstance.getCaseVariables()).containsOnly(entries);
        assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.VARIABLES).containsOnly(entries);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.HISTORIC_VARIABLES).containsOnly(entries);
        }
    }

    @Test
    @CmmnDeployment
    public void testCaseInstanceQueryWithIncludeVariablesAfterWaitState() {
        assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.VARIABLES).isNull();
        assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.HISTORIC_VARIABLES).isNull();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCaseInstanceQueryWithIncludeVariables")
                .variable("var1", "Hello")
                .variable("var2", "World")
                .variable("var3", 123)
                .start();

        assertThat(caseInstance.getCaseVariables()).containsOnly(
                entry("var1", "Hello"),
                entry("var2", "World"),
                entry("var3", 123)
        );

        assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.VARIABLES).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.HISTORIC_VARIABLES).isNull();
        }

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        cmmnTaskService.complete(task.getId());

        Map.Entry[] entries = {
                entry("var1", "Hello"),
                entry("var2", "World"),
                entry("var3", 123),
                entry("varFromTheServiceTask", "valueFromTheServiceTask")
        };

        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            // Make sure that it is loaded in the cache
            CaseInstance queriedCaseInstance = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstance.getId());
            assertThat(queriedCaseInstance.getCaseVariables()).isEmpty();

            queriedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();

            assertThat(queriedCaseInstance.getCaseVariables()).isEmpty();

            queriedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .includeCaseVariables()
                    .singleResult();
            assertThat(queriedCaseInstance.getCaseVariables()).containsOnly(entries);

            // Make sure that it is loaded in the cache
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                HistoricCaseInstance historicQueriedCaseInstance = CommandContextUtil.getHistoricCaseInstanceEntityManager(commandContext)
                    .findById(caseInstance.getId());
                assertThat(historicQueriedCaseInstance.getCaseVariables()).isEmpty();

                historicQueriedCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();

                assertThat(historicQueriedCaseInstance.getCaseVariables()).isEmpty();

                historicQueriedCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .includeCaseVariables()
                    .singleResult();
                assertThat(historicQueriedCaseInstance.getCaseVariables()).containsOnly(entries);
            }

            return null;
        });

        assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.VARIABLES).containsOnly(entries);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.HISTORIC_VARIABLES).containsOnly(entries);
        }
    }
}
