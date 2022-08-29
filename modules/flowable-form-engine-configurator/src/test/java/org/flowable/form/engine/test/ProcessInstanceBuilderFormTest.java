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
package org.flowable.form.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormInfo;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class ProcessInstanceBuilderFormTest extends AbstractFlowableFormEngineConfiguratorTest {

    @Test
    @Deployment(resources = {
            "org/flowable/form/engine/test/deployment/oneTaskProcess.bpmn20.xml",
            "org/flowable/form/engine/test/deployment/simpleInt.form"
    })
    public void startProcessInstanceWithFormVariables() {
        RuntimeService runtimeService = flowableRule.getProcessEngine().getRuntimeService();
        FormInfo formInfo = formRepositoryService.getFormModelByKey("simpleIntForm");
        String procId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .formVariables(Collections.singletonMap("intVar", "42"), formInfo, "simple")
                .start()
                .getId();

        assertThat(runtimeService.getVariables(procId))
                .containsOnly(
                        entry("intVar", 42L),
                        entry("form_simpleIntForm_outcome", "simple")
                );
    }

    @Test
    @Deployment(resources = {
            "org/flowable/form/engine/test/deployment/oneTaskProcess.bpmn20.xml",
            "org/flowable/form/engine/test/deployment/simpleInt.form"
    })
    public void startProcessInstanceWithFormVariablesAndOwner() {
        RuntimeService runtimeService = flowableRule.getProcessEngine().getRuntimeService();
        FormInfo formInfo = formRepositoryService.getFormModelByKey("simpleIntForm");
        String procId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .formVariables(Collections.singletonMap("intVar", "42"), formInfo, "simple")
                .owner("kermit")
                .start()
                .getId();

        assertThat(runtimeService.getVariables(procId))
                .containsOnly(
                        entry("intVar", 42L),
                        entry("form_simpleIntForm_outcome", "simple")
                );

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(procId);
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactly(
                tuple(IdentityLinkType.OWNER, "kermit", null, procId)
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, getProcessEngineConfiguration())) {
            List<HistoricIdentityLink> historicIdentityLinks = getProcessEngineConfiguration().getHistoryService().getHistoricIdentityLinksForProcessInstance(procId);
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactly(
                    tuple(IdentityLinkType.OWNER, "kermit", null, procId)
                );
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/form/engine/test/deployment/oneTaskProcess.bpmn20.xml",
            "org/flowable/form/engine/test/deployment/simpleInt.form"
    })
    public void startProcessInstanceWithFormVariablesAndAssignee() {
        RuntimeService runtimeService = flowableRule.getProcessEngine().getRuntimeService();
        FormInfo formInfo = formRepositoryService.getFormModelByKey("simpleIntForm");
        String procId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .formVariables(Collections.singletonMap("intVar", "42"), formInfo, "simple")
                .assignee("kermit")
                .start()
                .getId();

        assertThat(runtimeService.getVariables(procId))
                .containsOnly(
                        entry("intVar", 42L),
                        entry("form_simpleIntForm_outcome", "simple")
                );

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(procId);
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactly(
                tuple(IdentityLinkType.ASSIGNEE, "kermit", null, procId)
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, getProcessEngineConfiguration())) {
            List<HistoricIdentityLink> historicIdentityLinks = getProcessEngineConfiguration().getHistoryService().getHistoricIdentityLinksForProcessInstance(procId);
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactly(
                    tuple(IdentityLinkType.ASSIGNEE, "kermit", null, procId)
                );
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/form/engine/test/deployment/oneTaskProcess.bpmn20.xml",
            "org/flowable/form/engine/test/deployment/simpleInt.form"
    })
    public void startProcessInstanceWithFormVariablesAndOwnerAndAssignee() {
        RuntimeService runtimeService = flowableRule.getProcessEngine().getRuntimeService();
        FormInfo formInfo = formRepositoryService.getFormModelByKey("simpleIntForm");
        String procId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .formVariables(Collections.singletonMap("intVar", "42"), formInfo, "simple")
                .owner("kermit")
                .assignee("denise")
                .start()
                .getId();

        assertThat(runtimeService.getVariables(procId))
                .containsOnly(
                        entry("intVar", 42L),
                        entry("form_simpleIntForm_outcome", "simple")
                );

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(procId);
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactlyInAnyOrder(
                tuple(IdentityLinkType.OWNER, "kermit", null, procId),
                tuple(IdentityLinkType.ASSIGNEE, "denise", null, procId)
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, getProcessEngineConfiguration())) {
            List<HistoricIdentityLink> historicIdentityLinks = getProcessEngineConfiguration().getHistoryService().getHistoricIdentityLinksForProcessInstance(procId);
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.OWNER, "kermit", null, procId),
                    tuple(IdentityLinkType.ASSIGNEE, "denise", null, procId)
                );
        }
    }

    @Test
    public void startProcessInstanceWithInvalidFormVariables() {
        RuntimeService runtimeService = flowableRule.getProcessEngine().getRuntimeService();
        assertThatThrownBy(() -> runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .formVariables(Collections.singletonMap("intVar", "42"), null, "simple"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("formInfo is null");
    }
}
