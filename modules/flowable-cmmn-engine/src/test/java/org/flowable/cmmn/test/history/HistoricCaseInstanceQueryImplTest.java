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
package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CaseLocalizationManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class tests {@link CaseInstanceQueryImpl} implementation
 */
public class HistoricCaseInstanceQueryImplTest extends FlowableCmmnTestCase {

    protected String deploymentId;

    @BeforeEach
    public void createCase() {

        deploymentId = addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
                .deploy());

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
    }

    @Test
    public void getCaseInstanceByCaseDefinitionKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionKeyLike() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeyLike("oneTask%").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeyLike("oneTask%").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeyLike("oneTask%").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKeyLike("oneTask%")
                .caseInstanceId("Undefined")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKeyLike("oneTask%")
                .caseInstanceId("Undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeyLike("none%").list()).hasSize(0);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKeyLike("none%").caseInstanceId("Undefined").endOr().list()).hasSize(0);
        }
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionKeyLikeIgnoreCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeyLikeIgnoreCase("onetask%").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeyLikeIgnoreCase("onetask%").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeyLikeIgnoreCase("onetask%").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKeyLikeIgnoreCase("onetask%")
                .caseInstanceId("Undefined")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKeyLikeIgnoreCase("onetask%")
                .caseInstanceId("Undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeyLikeIgnoreCase("none%").list()).hasSize(0);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKeyLikeIgnoreCase("none%").caseInstanceId("Undefined").endOr().list()).hasSize(0);
        }
    }

    @Test
    public void getCaseInstanceByCaseInstanceName() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("taskName1")
                .start();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("taskName2")
                .start();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("nameTask3")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceNameLikeIgnoreCase("taskName%").count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceNameLikeIgnoreCase("%TASK3").count()).isEqualTo(1);
        }
    }

    public void getCaseInstanceByCaseDefinitionKeyIncludingVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").includeCaseVariables().count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").includeCaseVariables().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").includeCaseVariables().singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .includeCaseVariables().count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .includeCaseVariables().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .includeCaseVariables().singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByCaseDefinitionKeys() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("dummy")).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("dummy")).list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("dummy")).singleResult()).isNull();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .caseDefinitionKeys(Collections.singleton("oneTaskCase"))
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .caseDefinitionKeys(Collections.singleton("oneTaskCase"))
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .caseDefinitionKeys(Collections.singleton("oneTaskCase"))
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }
    
    @Test
    public void getCaseInstanceByExcludeCaseDefinitionKeys() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            Set<String> excludeKeys = new HashSet<>();
            excludeKeys.add("oneTaskCase");
            excludeKeys.add("myCase");
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).count()).isEqualTo(0);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).list()).hasSize(0);
            
            excludeKeys = new HashSet<>();
            excludeKeys.add("myCase");
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).singleResult().getId())
                .isEqualTo(caseInstance.getId());

            excludeKeys = new HashSet<>();
            excludeKeys.add("dummy");
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).list()).hasSize(2);
            
            excludeKeys = new HashSet<>();
            excludeKeys.add("myCase");
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .excludeCaseDefinitionKeys(excludeKeys)
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .excludeCaseDefinitionKeys(excludeKeys)
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .excludeCaseDefinitionKeys(excludeKeys)
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
            
            excludeKeys = new HashSet<>();
            excludeKeys.add("oneTaskCase");
            excludeKeys.add("myCase");
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceId("undefined")
                    .excludeCaseDefinitionKeys(excludeKeys)
                    .endOr()
                    .count()).isEqualTo(0);
        }
    }

    @Test
    public void getCaseInstanceByCaseDefinitionCategory() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").list()).hasSize(2);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionCategory("http://flowable.org/cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .count())
                .isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionCategory("http://flowable.org/cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .list())
                .hasSize(2);
        }
    }

    @Test
    public void getCaseInstanceByCaseDefinitionName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionName("oneTaskCaseName")
                .caseInstanceId("undefined")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionName("oneTaskCaseName")
                .caseInstanceId("undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionName("oneTaskCaseName")
                .caseInstanceId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionNameLike() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionNameLike("oneTask%").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionNameLike("oneTask%").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionNameLike("oneTask%").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionNameLike("oneTask%")
                .caseInstanceId("undefined")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionNameLike("oneTask%")
                .caseInstanceId("undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionNameLike("none%").list()).hasSize(0);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionNameLike("none%").caseInstanceId("undefined").endOr().list()).hasSize(0);
        }
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionNameLikeIgnoreCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionNameLikeIgnoreCase("onetask%").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionNameLikeIgnoreCase("onetask%").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionNameLikeIgnoreCase("onetask%").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionNameLikeIgnoreCase("onetask%")
                .caseInstanceId("undefined")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionNameLikeIgnoreCase("onetask%")
                .caseInstanceId("undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionNameLikeIgnoreCase("none%").list()).hasSize(0);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionNameLikeIgnoreCase("none%").caseInstanceId("undefined").endOr().list()).hasSize(0);
        }
    }

    @Test
    public void getCaseInstanceByCaseDefinitionId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId("undefinedId")
                .endOr().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId("undefinedId")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByCaseDefinitionIds() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            Set<String> definitionIds = new HashSet<>(Arrays.asList(caseInstance.getCaseDefinitionId(), "dummy"));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionIds(definitionIds).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionIds(definitionIds).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionIds(definitionIds).singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionIds(Collections.singleton("unknown")).count()).isEqualTo(0);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionIds(Collections.singleton("unknown")).list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionIds(Collections.singleton("unknown")).singleResult()).isNull();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionIds(definitionIds)
                .caseInstanceId("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionIds(definitionIds)
                .caseInstanceId("undefinedId")
                .endOr().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionIds(definitionIds)
                .caseInstanceId("undefinedId")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByCaseDefinitionVersion() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionVersion(1).count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionVersion(1).list()).hasSize(2);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionVersion(1)
                .caseInstanceId("undefinedId")
                .endOr()
                .count())
                .isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionVersion(1)
                .caseInstanceId("undefinedId")
                .endOr()
                .list())
                .hasSize(2);
        }
    }

    @Test
    public void getCaseInstanceByCaseId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId(caseInstance.getId())
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId(caseInstance.getId())
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId(caseInstance.getId())
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByCaseIds() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceIds(Collections.singleton(caseInstance.getId())).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceIds(Collections.singleton(caseInstance.getId())).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceIds(Collections.singleton(caseInstance.getId())).singleResult().getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceIds(Collections.singleton("dummy")).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceIds(Collections.singleton("dummy")).list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceIds(Collections.singleton("dummy")).singleResult()).isNull();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceIds(Collections.singleton(caseInstance.getId()))
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceIds(Collections.singleton(caseInstance.getId()))
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceIds(Collections.singleton(caseInstance.getId()))
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByBusinessKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("businessKey")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey("businessKey").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey("businessKey").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey("businessKey").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKey("businessKey")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKey("businessKey")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKey("businessKey")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }
    
    @Test
    public void getCaseInstanceByBusinessKeyLike() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("businessKey")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKeyLike("%Key").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKeyLike("%Key").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKeyLike("%Key").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKeyLike("%Key")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKeyLike("%Key")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKeyLike("%none").list()).hasSize(0);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseInstanceBusinessKeyLike("%none").caseDefinitionName("undefinedId").endOr().list()).hasSize(0);
        }
    }
    
    @Test
    public void getCaseInstanceByBusinessKeyLikeIgnoreCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("businessKey")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKeyLikeIgnoreCase("%key").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKeyLikeIgnoreCase("%key").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKeyLikeIgnoreCase("%key").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKeyLikeIgnoreCase("%key")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKeyLikeIgnoreCase("%key")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKeyLikeIgnoreCase("%none").list()).hasSize(0);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseInstanceBusinessKeyLikeIgnoreCase("%none").caseDefinitionName("undefinedId").endOr().list()).hasSize(0);
        }
    }
    
    @Test
    public void getCaseInstanceByBusinessStatus() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        
        cmmnRuntimeService.updateBusinessStatus(caseInstance.getId(), "businessStatus");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessStatus("businessStatus").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessStatus("businessStatus").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessStatus("businessStatus").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatus("businessStatus")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatus("businessStatus")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatus("undefined")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult())
                .isNull();
        }
    }
    
    @Test
    public void getCaseInstanceByBusinessStatusLike() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        
        cmmnRuntimeService.updateBusinessStatus(caseInstance.getId(), "businessStatus");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessStatusLike("%Status").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessStatusLike("%Status").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessStatusLike("%Status").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatusLike("%Status")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatusLike("%Status")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessStatusLike("%none").list()).hasSize(0);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseInstanceBusinessStatusLike("%none").caseDefinitionName("undefinedId").endOr().list()).hasSize(0);
        }
    }

    @Test
    public void getCaseInstanceByParentId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .parentId("parentId")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceParentId("parentId").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceParentId("parentId").list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceParentId("parentId").singleResult().getId())
                    .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceParentId("parentId")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceParentId("parentId")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceParentId("parentId")
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByWithoutParentId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .parentId("parentId")
                .start();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("myCase").singleResult();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().withoutCaseInstanceParent().count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().withoutCaseInstanceParent().list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().withoutCaseInstanceParent().singleResult().getId())
                    .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .withoutCaseInstanceParent()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .withoutCaseInstanceParent()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .withoutCaseInstanceParent()
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByStartedBefore() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Calendar todayCal = new GregorianCalendar();
        Calendar dateCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) + 1, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_YEAR));

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBefore(dateCal.getTime()).count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBefore(dateCal.getTime()).list()).hasSize(2);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .startedBefore(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .startedBefore(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(2);
        }
    }

    @Test
    public void getCaseInstanceByStartedAfter() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            Calendar todayCal = new GregorianCalendar();
            Calendar dateCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) - 1, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_YEAR));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedAfter(dateCal.getTime()).count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedAfter(dateCal.getTime()).list()).hasSize(2);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .startedAfter(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .startedAfter(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(2);
        }
    }

    @Test
    public void getCaseInstanceByStartedBy() {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("kermit");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .start();

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBy("kermit").count()).isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBy("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBy("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .startedBy("kermit")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .startedBy("kermit")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .startedBy("kermit")
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isEqualTo(caseInstance.getId());
            }
        } finally {
            Authentication.setAuthenticatedUserId(authenticatedUserId);
        }
    }

    @Test
    public void getCaseInstanceByEndedBy() {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("elmo");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneHumanTaskCase")
                    .start();

            Authentication.setAuthenticatedUserId("kermit");
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finishedBy("elmo").count()).isEqualTo(0);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finishedBy("kermit").count()).isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finishedBy("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finishedBy("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                        .or()
                        .finishedBy("kermit")
                        .caseDefinitionName("undefinedId")
                        .endOr()
                        .count())
                        .isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                        .or()
                        .finishedBy("kermit")
                        .caseDefinitionName("undefinedId")
                        .endOr()
                        .list().get(0).getId())
                        .isEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                        .or()
                        .finishedBy("kermit")
                        .caseDefinitionId("undefined")
                        .endOr()
                        .singleResult().getId())
                        .isEqualTo(caseInstance.getId());
            }
        } finally {
            Authentication.setAuthenticatedUserId(authenticatedUserId);
        }
    }
    
    @Test
    public void getCaseInstanceByState() {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("kermit");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .start();

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().state(CaseInstanceState.ACTIVE).count()).isEqualTo(2);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").state(CaseInstanceState.ACTIVE).list().get(0).getId()).isEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").state(CaseInstanceState.ACTIVE).singleResult().getId()).isEqualTo(caseInstance.getId());

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .state(CaseInstanceState.ACTIVE)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(2);
                
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .state(CaseInstanceState.COMPLETED)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isZero();
            }
            
            cmmnRuntimeService.triggerPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
            
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().state(CaseInstanceState.COMPLETED).count()).isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().state(CaseInstanceState.COMPLETED).list().get(0).getId()).isEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().state(CaseInstanceState.COMPLETED).singleResult().getId()).isEqualTo(caseInstance.getId());

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .state(CaseInstanceState.COMPLETED)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
                
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .state(CaseInstanceState.TERMINATED)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isZero();
            }
            
            CaseInstance secondCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .start();
            
            cmmnRuntimeService.terminateCaseInstance(secondCaseInstance.getId());
            
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().state(CaseInstanceState.TERMINATED).count()).isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().state(CaseInstanceState.TERMINATED).list().get(0).getId()).isEqualTo(secondCaseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().state(CaseInstanceState.TERMINATED).singleResult().getId()).isEqualTo(secondCaseInstance.getId());

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .state(CaseInstanceState.TERMINATED)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
                
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .state(CaseInstanceState.SUSPENDED)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isZero();
            }
            
        } finally {
            Authentication.setAuthenticatedUserId(authenticatedUserId);
        }
    }

    @Test
    public void getCaseInstanceByCallBackId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .callbackId("callBackId")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackId("callBackId").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackId("callBackId").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackId("callBackId").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackId("callBackId")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackId("callBackId")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackId("callBackId")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByCallBackIds() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .callbackId("callBackId")
                .start();

        CaseInstance otherCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .callbackId("otherCallBackId")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

            Set<String> callBackIdParameter = new HashSet<>();
            callBackIdParameter.add("callBackId");
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).singleResult().getId()).isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceCallbackIds(callBackIdParameter)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceCallbackIds(callBackIdParameter)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceCallbackIds(callBackIdParameter)
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId()).isEqualTo(caseInstance.getId());

            callBackIdParameter.add("otherCallBackId");
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).list().get(1).getId()).isEqualTo(otherCaseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceCallbackIds(callBackIdParameter)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceCallbackIds(callBackIdParameter)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list()).extracting(HistoricCaseInstance::getId).containsExactlyInAnyOrder(
                    caseInstance.getId(),
                    otherCaseInstance.getId()
            );
        }
    }

    @Test
    public void getCaseInstanceByCallBackType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .callbackType("callBackType")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackType("callBackType").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackType("callBackType").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackType("callBackType").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackType("callBackType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackType("callBackType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackType("callBackType")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceWithoutCallbackId() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .callbackId("callbackId")
                .start();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("myCase")
                .singleResult();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().withoutCaseInstanceCallbackId().count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().withoutCaseInstanceCallbackId().list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().withoutCaseInstanceCallbackId().singleResult().getId())
                    .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .withoutCaseInstanceCallbackId()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .withoutCaseInstanceCallbackId()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .withoutCaseInstanceCallbackId()
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByReferenceId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .referenceId("testReferenceId")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceId("testReferenceId").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceId("testReferenceId").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceId("testReferenceId").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceId("testReferenceId")
                .caseDefinitionName("undefined")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceId("testReferenceId")
                .caseDefinitionName("undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceId("testReferenceId")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByReferenceType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .referenceType("testReferenceType")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceType("testReferenceType").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceType("testReferenceType").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceType("testReferenceType").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceType("testReferenceType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceType("testReferenceType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceType("testReferenceType")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceByReferenceIdAndType() {
        for (int i = 0; i < 5; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .referenceId("testReferenceId")
                    .referenceType("testReferenceType")
                    .start();
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceId("testReferenceId")
                .caseInstanceReferenceType("testReferenceType").count()).isEqualTo(5);
        }
    }

    @Test
    public void getCaseInstanceByTenantId() {
        String tempDeploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
                .tenantId("tenantId")
                .deploy()
                .getId();
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .tenantId("tenantId")
                    .start();

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceTenantId("tenantId").count()).isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceTenantId("tenantId").list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceTenantId("tenantId").singleResult().getId())
                    .isEqualTo(caseInstance.getId());

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantId("tenantId")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantId("tenantId")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantId("tenantId")
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isEqualTo(caseInstance.getId());
            }
        } finally {
            cmmnRepositoryService.deleteDeployment(tempDeploymentId, true);
        }
    }

    @Test
    public void getCaseInstanceWithoutTenantId() {
        String tempDeploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
                .tenantId("tenantId")
                .deploy()
                .getId();
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .tenantId("tenantId")
                    .start();

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceWithoutTenantId().count()).isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceWithoutTenantId().list().get(0).getId())
                    .isNotEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceWithoutTenantId().singleResult().getId())
                    .isNotEqualTo(caseInstance.getId());

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceWithoutTenantId()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceWithoutTenantId()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isNotEqualTo(caseInstance.getId());
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceWithoutTenantId()
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isNotEqualTo(caseInstance.getId());
            }
        } finally {
            cmmnRepositoryService.deleteDeployment(tempDeploymentId, true);
        }
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/three-task.cmmn")
    public void getCaseInstanceByActivePlanItemDefinitionId() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("activePlanItemDefinition")
                .start();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).taskDefinitionKey("humanTask3").singleResult();
        cmmnTaskService.complete(task.getId());
        
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("activePlanItemDefinition")
                .start();
        
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("activePlanItemDefinition")
                .start();
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance3.getId()).taskDefinitionKey("humanTask3").singleResult();
        cmmnTaskService.complete(task.getId());
        
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<String> queryIds = cmmnHistoryService.createHistoricCaseInstanceQuery().activePlanItemDefinitionId("humanTask3").list().stream()
                .map(HistoricCaseInstance::getId)
                .collect(Collectors.toList());
            
            assertThat(queryIds.size()).isEqualTo(1);
            assertThat(queryIds.contains(caseInstance2.getId())).isTrue();
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).activePlanItemDefinitionId("humanTask1").count()).isZero();
            
            queryIds = cmmnHistoryService.createHistoricCaseInstanceQuery().activePlanItemDefinitionId("humanTask1").list().stream()
                    .map(HistoricCaseInstance::getId)
                    .collect(Collectors.toList());
            
            assertThat(queryIds.size()).isEqualTo(2);
            assertThat(queryIds.contains(caseInstance2.getId())).isTrue();
            assertThat(queryIds.contains(caseInstance3.getId())).isTrue();
            
            Set<String> planItemDefinitionIds = new HashSet<>();
            planItemDefinitionIds.add("humanTask1");
            planItemDefinitionIds.add("humanTask3");
            
            queryIds = cmmnHistoryService.createHistoricCaseInstanceQuery().activePlanItemDefinitionIds(planItemDefinitionIds).list().stream()
                    .map(HistoricCaseInstance::getId)
                    .collect(Collectors.toList());
         
            assertThat(queryIds.size()).isEqualTo(2);
            assertThat(queryIds.contains(caseInstance2.getId())).isTrue();
            assertThat(queryIds.contains(caseInstance3.getId())).isTrue();
            
            planItemDefinitionIds = new HashSet<>();
            planItemDefinitionIds.add("humanTask2");
            planItemDefinitionIds.add("humanTask4");
            
            queryIds = cmmnHistoryService.createHistoricCaseInstanceQuery().activePlanItemDefinitionIds(planItemDefinitionIds).list().stream()
                    .map(HistoricCaseInstance::getId)
                    .collect(Collectors.toList());
            
            assertThat(queryIds.size()).isEqualTo(1);
            assertThat(queryIds.contains(caseInstance1.getId())).isTrue();
            
            planItemDefinitionIds = new HashSet<>();
            planItemDefinitionIds.add("humanTask88");
            planItemDefinitionIds.add("humanTask99");
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().activePlanItemDefinitionIds(planItemDefinitionIds).count()).isZero();
        }
    }

    @Test
    public void getCaseInstanceByInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }
    
    @Test
    public void getCaseInstanceByInvolvedUserIdentityLink() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", "specialLink");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit", "specialLink").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit", "specialLink").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit", "specialLink").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit", "wrongType").count()).isZero();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit", "specialLink")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit", "specialLink")
                .caseDefinitionKey("oneTaskCase")
                .endOr()
                .count())
                .isEqualTo(1);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit", "wrongType")
                .caseDefinitionKey("wrongKey")
                .endOr()
                .count())
                .isZero();
        }
    }

    @Test
    public void getCaseInstanceByInvolvedGroup() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Collections.singleton("testGroup"))
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Collections.singleton("testGroup"))
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Collections.singleton("testGroup"))
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }
    
    @Test
    public void getCaseInstanceByInvolvedGroupIdentityLink() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", "specialLink");
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", "extraLink");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroup("testGroup", "specialLink").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroup("testGroup", "specialLink").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroup("testGroup", "specialLink").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroup("testGroup2", "wrongType").count()).isZero();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroup("testGroup", "specialLink")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroup("testGroup2", "extraLink")
                .caseDefinitionKey("oneTaskCase")
                .endOr()
                .count())
                .isEqualTo(1);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroup("testGroup2", "wrongType")
                .caseDefinitionKey("wrongKey")
                .endOr()
                .count())
                .isZero();
        }
    }

    @Test
    public void getCaseInstanceByInvolvedGroupOrUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.CANDIDATE);
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance2.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance3.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").singleResult().getId())
                .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(2);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(3);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(3);
        }
    }

    @Test
    public void getCaseInstanceByVariable() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("queryVariable", "queryVariableValue")
                .start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("queryVariable", "queryVariableValue")
                .variable("queryVariable2", "queryVariableValue2")
                .start();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("queryVariable", "queryVariableValue")
                .variable("queryVariable3", "queryVariableValue3")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .list().get(0).getId())
                .isEqualTo(caseInstance2.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .singleResult().getId())
                .isEqualTo(caseInstance2.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(3);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(3);
        }
    }

    @Test
    public void getCaseInstanceByIdWithoutTenant() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("testCaseInstance1")
                .start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("testCaseInstance2")
                .start();

        try {
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).caseInstanceWithoutTenantId().count())
                    .isEqualTo(1);
            }
        } finally {
            cmmnRuntimeService.terminateCaseInstance(caseInstance2.getId());
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                cmmnHistoryService.deleteHistoricCaseInstance(caseInstance2.getId());
            }
            cmmnRuntimeService.terminateCaseInstance(caseInstance1.getId());
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                cmmnHistoryService.deleteHistoricCaseInstance(caseInstance1.getId());
            }
        }

    }

    @Test
    public void testLocalization() {
        CaseInstance createdCase = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("Default name")
                .start();

        cmmnEngineConfiguration.setCaseLocalizationManager(new CaseLocalizationManager() {
            @Override
            public void localize(CaseInstance caseInstance, String locale, boolean withLocalizationFallback) {

            }

            @Override
            public void localize(HistoricCaseInstance historicCaseInstance, String locale, boolean withLocalizationFallback) {
                if ("pt".equals(locale)) {
                    historicCaseInstance.setLocalizedName("Caso 1");
                }
            }
        });

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(createdCase.getId()).singleResult();
            assertThat(caseInstance.getName()).isEqualTo("Default name");
    
            caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(createdCase.getId()).locale("pt").singleResult();
            assertThat(caseInstance.getName()).isEqualTo("Caso 1");
        }
    }
  
    @Test
    public void testQueryCaseInstanceReturnsCaseDefinitionInformation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("stringVar", "test")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();

            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getCaseDefinitionKey()).isEqualTo("oneTaskCase");
            assertThat(historicCaseInstance.getCaseDefinitionName()).isEqualTo("oneTaskCaseName");
            assertThat(historicCaseInstance.getCaseDefinitionVersion()).isEqualTo(1);
            assertThat(historicCaseInstance.getCaseDefinitionDeploymentId()).isEqualTo(deploymentId);
            assertThat(historicCaseInstance.getCaseVariables()).isEmpty();
        }
    }

    @Test
    public void testQueryCaseInstanceWithVariablesReturnsCaseDefinitionInformation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("stringVar", "test")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables()
                .singleResult();

            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getCaseDefinitionKey()).isEqualTo("oneTaskCase");
            assertThat(historicCaseInstance.getCaseDefinitionName()).isEqualTo("oneTaskCaseName");
            assertThat(historicCaseInstance.getCaseDefinitionVersion()).isEqualTo(1);
            assertThat(historicCaseInstance.getCaseDefinitionDeploymentId()).isEqualTo(deploymentId);
            assertThat(historicCaseInstance.getCaseVariables()).containsOnly(
                entry("stringVar", "test")
            );
        }
    }

    @Test
    public void testQueryVariableValueEqualsAndNotEquals() {
        CaseInstance caseWithStringValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        CaseInstance caseWithNullValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With null value")
                .variable("var", null)
                .start();

        CaseInstance caseWithLongValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With long value")
                .variable("var", 100L)
                .start();

        CaseInstance caseWithDoubleValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "TEST").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                    tuple("With null value", caseWithNullValue.getId()),
                    tuple("With long value", caseWithLongValue.getId()),
                    tuple("With double value", caseWithDoubleValue.getId())
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "TEST").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                    tuple("With string value", caseWithStringValue.getId())
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", 100L).list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                    tuple("With string value", caseWithStringValue.getId()),
                    tuple("With null value", caseWithNullValue.getId()),
                    tuple("With double value", caseWithDoubleValue.getId())
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", 100L).list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                    tuple("With long value", caseWithLongValue.getId())
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", 45.55).list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                    tuple("With string value", caseWithStringValue.getId()),
                    tuple("With null value", caseWithNullValue.getId()),
                    tuple("With long value", caseWithLongValue.getId())
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", 45.55).list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                    tuple("With double value", caseWithDoubleValue.getId())
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "test").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                    tuple("With string value", caseWithStringValue.getId()),
                    tuple("With null value", caseWithNullValue.getId()),
                    tuple("With long value", caseWithLongValue.getId()),
                    tuple("With double value", caseWithDoubleValue.getId())
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "test").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .isEmpty();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "test").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                    tuple("With string value", caseWithStringValue.getId())
                );
        }
    }

    @Test
    public void queryCaseInstanceByDeploymentId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentId(deploymentId).count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentId(deploymentId).list()).hasSize(2);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentId("dummy").count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentId("dummy").list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentId("dummy").singleResult()).isNull();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .deploymentId(deploymentId)
                    .caseInstanceId("Undefined")
                    .endOr()
                    .count())
                    .isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .deploymentId(deploymentId)
                    .caseInstanceId("Undefined")
                    .endOr()
                    .list())
                    .hasSize(2);
        }
    }

    @Test
    public void queryCaseInstanceByDeploymentIds() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentIds(Arrays.asList(deploymentId, "dummy")).count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentIds(Arrays.asList(deploymentId, "dummy")).list())
                    .hasSize(2);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentIds(Arrays.asList("dummy1", "dummy2")).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentIds(Arrays.asList("dummy1", "dummy2")).list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().deploymentIds(Arrays.asList("dummy1", "dummy2")).singleResult()).isNull();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .deploymentIds(Arrays.asList(deploymentId, "dummy"))
                    .caseInstanceId("Undefined")
                    .endOr()
                    .count())
                    .isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .deploymentIds(Arrays.asList(deploymentId, "dummy"))
                    .caseInstanceId("Undefined")
                    .endOr()
                    .list())
                    .hasSize(2);
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/simpleCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/test/runtime/simpleInnerCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/test/runtime/simpleInnerCaseWithHumanTasksAndCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn"
    })
    public void testQueryByRootScopeId() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();

        PlanItemInstance oneTaskCasePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        PlanItemInstance caseTaskSimpleCaseWithCaseTasksPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskSimpleCaseWithCaseTasks").singleResult();

        PlanItemInstance caseTaskWithHumanTasksPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskCaseWithHumanTasks").singleResult();

        PlanItemInstance oneTaskCase2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        cmmnTaskService.createTaskQuery().list().forEach(task -> cmmnTaskService.complete(task.getId()));

        List<HistoricCaseInstance> result = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceRootScopeId(caseInstance.getId()).list();
        assertThat(result)
                .extracting(HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        oneTaskCasePlanItemInstance.getReferenceId(),
                        caseTaskWithHumanTasksPlanItemInstance.getReferenceId(),
                        caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId(),
                        oneTaskCase2PlanItemInstance.getReferenceId()
                );

        result = cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseInstanceRootScopeId(caseInstance.getId()).endOr().list();
        assertThat(result)
                .extracting(HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        oneTaskCasePlanItemInstance.getReferenceId(),
                        caseTaskWithHumanTasksPlanItemInstance.getReferenceId(),
                        caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId(),
                        oneTaskCase2PlanItemInstance.getReferenceId()
                );
    }
    
    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/simpleCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/test/runtime/simpleInnerCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/test/runtime/simpleInnerCaseWithHumanTasksAndCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn"
    })
    public void testQueryByParentCaseInstanceId() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();

        PlanItemInstance oneTaskCasePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        PlanItemInstance caseTaskSimpleCaseWithCaseTasksPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskSimpleCaseWithCaseTasks").singleResult();

        PlanItemInstance caseTaskWithHumanTasksPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskCaseWithHumanTasks").singleResult();

        PlanItemInstance oneTaskCase2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        cmmnTaskService.createTaskQuery().list().forEach(task -> cmmnTaskService.complete(task.getId()));

        List<HistoricCaseInstance> result = cmmnHistoryService.createHistoricCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).list();
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        oneTaskCasePlanItemInstance.getReferenceId(),
                        caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId()
                );
        
        result = cmmnHistoryService.createHistoricCaseInstanceQuery().or().parentCaseInstanceId(caseInstance.getId()).caseInstanceBusinessKey("unexisting").endOr().list();
        assertThat(result).hasSize(2);
        
        result = cmmnHistoryService.createHistoricCaseInstanceQuery().or().parentCaseInstanceId(caseInstance.getId()).caseDefinitionKey("oneTaskCase").endOr().list();
        assertThat(result).hasSize(5);
        
        result = cmmnHistoryService.createHistoricCaseInstanceQuery().parentCaseInstanceId(oneTaskCasePlanItemInstance.getReferenceId()).list();
        assertThat(result).isEmpty();
        
        result = cmmnHistoryService.createHistoricCaseInstanceQuery().parentCaseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId()).list();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(caseTaskWithHumanTasksPlanItemInstance.getReferenceId());
        
        result = cmmnHistoryService.createHistoricCaseInstanceQuery().parentCaseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId()).list();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(oneTaskCase2PlanItemInstance.getReferenceId());
        
        result = cmmnHistoryService.createHistoricCaseInstanceQuery().parentCaseInstanceId(oneTaskCase2PlanItemInstance.getReferenceId()).list();
        assertThat(result).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/simpleCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/test/runtime/simpleInnerCaseWithCaseTasks.cmmn",
            "org/flowable/cmmn/test/runtime/simpleInnerCaseWithHumanTasksAndCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn"
    })
    public void testQueryByParentScopeId() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("simpleTestCaseWithCaseTasks").start();

        PlanItemInstance caseTaskSimpleCaseWithCaseTasksPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskSimpleCaseWithCaseTasks").singleResult();

        PlanItemInstance caseTaskWithHumanTasksPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskCaseWithHumanTasks").singleResult();

        PlanItemInstance oneTaskCase2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId())
                .planItemDefinitionId("caseTaskOneTaskCase").singleResult();

        cmmnTaskService.createTaskQuery().list().forEach(task -> cmmnTaskService.complete(task.getId()));

        List<HistoricCaseInstance> result = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceParentScopeId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId()).list();
        assertThat(result)
                .extracting(HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        oneTaskCase2PlanItemInstance.getReferenceId()
                );

        result = cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseInstanceParentScopeId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId())
                .endOr().list();
        assertThat(result)
                .extracting(HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        oneTaskCase2PlanItemInstance.getReferenceId()
                );
    }

    @Test
    public void getCaseInstanceIdByCaseDefinitionKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").withoutSorting().returnIdsOnly().singleResult();
            assertThat(historicCaseInstance.getId()).isEqualTo(caseInstance.getId());
            assertThat(historicCaseInstance.getCaseDefinitionId()).isNull();
            
            historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").withoutSorting().singleResult();
            assertThat(historicCaseInstance.getId()).isEqualTo(caseInstance.getId());
            assertThat(historicCaseInstance.getCaseDefinitionId()).isNotNull();

            historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseDefinitionKey("oneTaskCase")
                    .caseInstanceId("undefined")
                    .endOr()
                    .withoutSorting()
                    .returnIdsOnly()
                    .singleResult();
            assertThat(historicCaseInstance.getId()).isEqualTo(caseInstance.getId());
            assertThat(historicCaseInstance.getCaseDefinitionId()).isNull();
            
            historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseDefinitionKey("oneTaskCase")
                    .caseInstanceId("undefined")
                    .endOr()
                    .withoutSorting()
                    .singleResult();
            
            assertThat(historicCaseInstance.getId()).isEqualTo(caseInstance.getId());
            assertThat(historicCaseInstance.getCaseDefinitionId()).isNotNull();
        }
    }
}
