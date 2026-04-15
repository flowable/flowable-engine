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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.jupiter.api.Test;

public class CaseInstanceUpdateBuilderTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testUpdateBusinessKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.createCaseInstanceUpdateBuilder(caseInstance.getId())
                .businessKey("newBusinessKey")
                .update();

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getBusinessKey()).isEqualTo("newBusinessKey");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getBusinessKey()).isEqualTo("newBusinessKey");
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testUpdateBusinessStatus() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.createCaseInstanceUpdateBuilder(caseInstance.getId())
                .businessStatus("newStatus")
                .update();

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getBusinessStatus()).isEqualTo("newStatus");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getBusinessStatus()).isEqualTo("newStatus");
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testUpdateName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(caseInstance.getName()).isNull();

        cmmnRuntimeService.createCaseInstanceUpdateBuilder(caseInstance.getId())
                .name("My case")
                .update();

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getName()).isEqualTo("My case");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testUpdateDueDate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Date dueDate = new Date();
        cmmnRuntimeService.createCaseInstanceUpdateBuilder(caseInstance.getId())
                .dueDate(dueDate)
                .update();

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getDueDate()).isEqualTo(dueDate);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getDueDate()).isEqualTo(dueDate);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testUpdateDueDateToNull() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.createCaseInstanceUpdateBuilder(caseInstance.getId())
                .dueDate(new Date())
                .update();

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getDueDate()).isNotNull();

        cmmnRuntimeService.createCaseInstanceUpdateBuilder(caseInstance.getId())
                .dueDate(null)
                .update();

        updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getDueDate()).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testUpdateMultipleProperties() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Date dueDate = new Date();
        cmmnRuntimeService.createCaseInstanceUpdateBuilder(caseInstance.getId())
                .businessKey("myKey")
                .businessStatus("myStatus")
                .name("My case")
                .dueDate(dueDate)
                .update();

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getBusinessKey()).isEqualTo("myKey");
        assertThat(updatedInstance.getBusinessStatus()).isEqualTo("myStatus");
        assertThat(updatedInstance.getName()).isEqualTo("My case");
        assertThat(updatedInstance.getDueDate()).isEqualTo(dueDate);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getBusinessKey()).isEqualTo("myKey");
            assertThat(historicCaseInstance.getBusinessStatus()).isEqualTo("myStatus");
            assertThat(historicCaseInstance.getName()).isEqualTo("My case");
            assertThat(historicCaseInstance.getDueDate()).isEqualTo(dueDate);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testUpdateOnlyDueDateLeavesOtherFieldsUnchanged() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("originalKey")
                .businessStatus("originalStatus")
                .name("Original name")
                .start();

        Date dueDate = new Date();
        cmmnRuntimeService.createCaseInstanceUpdateBuilder(caseInstance.getId())
                .dueDate(dueDate)
                .update();

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getBusinessKey()).isEqualTo("originalKey");
        assertThat(updatedInstance.getBusinessStatus()).isEqualTo("originalStatus");
        assertThat(updatedInstance.getName()).isEqualTo("Original name");
        assertThat(updatedInstance.getDueDate()).isEqualTo(dueDate);
    }

    @Test
    public void testUpdateWithNullId() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceUpdateBuilder(null)
                .businessKey("key")
                .update())
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testUpdateWithNonExistingId() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceUpdateBuilder("nonExistingId")
                .businessKey("key")
                .update())
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }
}
