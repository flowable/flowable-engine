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
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CaseLocalizationManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class tests {@link CaseInstanceQueryImpl} implementation
 */
public class CaseInstanceQueryImplTest extends FlowableCmmnTestCase {

    protected String deploymentId;

    @BeforeEach
    public void createCase() {
        this.deploymentId = addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").caseInstanceId("Undefined").endOr().count())
                .isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").caseInstanceId("Undefined").endOr().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(
                cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").caseInstanceId("Undefined").endOr().singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionKeyLike() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeyLike("oneTask%").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeyLike("oneTask%").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeyLike("oneTask%").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKeyLike("oneTask%").caseInstanceId("Undefined").endOr().count())
                .isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKeyLike("oneTask%").caseInstanceId("Undefined").endOr().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(
                cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKeyLike("oneTask%").caseInstanceId("Undefined").endOr().singleResult().getId())
                .isEqualTo(caseInstance.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeyLike("none%").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKeyLike("none%").caseInstanceId("Undefined").endOr().list()).hasSize(0);
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionKeyLikeIgnoreCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeyLikeIgnoreCase("onetask%").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeyLikeIgnoreCase("onetask%").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeyLikeIgnoreCase("onetask%").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKeyLikeIgnoreCase("onetask%").caseInstanceId("Undefined").endOr().count())
                .isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKeyLikeIgnoreCase("onetask%").caseInstanceId("Undefined").endOr().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(
                cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKeyLikeIgnoreCase("onetask%").caseInstanceId("Undefined").endOr().singleResult().getId())
                .isEqualTo(caseInstance.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeyLikeIgnoreCase("none%").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKeyLikeIgnoreCase("none%").caseInstanceId("Undefined").endOr().list()).hasSize(0);
    }

    @Test
    public void getCaseInstanceByCaseDefinitionKeys() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .caseDefinitionKeys(Collections.singleton("oneTaskCase"))
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .caseDefinitionKeys(Collections.singleton("oneTaskCase"))
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or()
                .caseInstanceId("undefined")
                .caseDefinitionKeys(Collections.singleton("oneTaskCase"))
                .endOr().singleResult().getId()).isEqualTo(caseInstance.getId());
    }
    
    @Test
    public void getCaseInstanceByExcludeCaseDefinitionKeys() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Set<String> excludeKeys = new HashSet<>();
        excludeKeys.add("oneTaskCase");
        excludeKeys.add("myCase");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).count()).isEqualTo(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).list()).hasSize(0);
        
        excludeKeys = new HashSet<>();
        excludeKeys.add("myCase");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().excludeCaseDefinitionKeys(excludeKeys).singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .excludeCaseDefinitionKeys(excludeKeys)
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .excludeCaseDefinitionKeys(excludeKeys)
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or()
                .caseInstanceId("undefined")
                .excludeCaseDefinitionKeys(excludeKeys)
                .endOr().singleResult().getId()).isEqualTo(caseInstance.getId());
        
        excludeKeys = new HashSet<>();
        excludeKeys.add("oneTaskCase");
        excludeKeys.add("myCase");
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .excludeCaseDefinitionKeys(excludeKeys)
                .endOr()
                .count()).isEqualTo(0);
    }

    @Test
    public void getCaseInstanceByCaseDefinitionCategory() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").list()).hasSize(2);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionCategory("http://flowable.org/cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionCategory("http://flowable.org/cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .list()).hasSize(2);
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionCategoryLike() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategoryLike("%cmmn").count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategoryLike("%cmmn").list()).hasSize(2);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionCategoryLike("%cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionCategoryLike("%cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .list()).hasSize(2);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategoryLike("%none").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionCategoryLike("%none").caseInstanceId("undefined").endOr().list()).hasSize(0);
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionCategoryLikeIgnoreCase() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategoryLikeIgnoreCase("%cmmn").count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategoryLikeIgnoreCase("%cmmn").list()).hasSize(2);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionCategoryLikeIgnoreCase("%cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionCategoryLikeIgnoreCase("%cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .list()).hasSize(2);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategoryLikeIgnoreCase("%none").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionCategoryLikeIgnoreCase("%none").caseInstanceId("undefined").endOr().list()).hasSize(0);
    }

    @Test
    public void getCaseInstanceByCaseDefinitionName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionName("oneTaskCaseName")
                .caseInstanceId("undefined")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionName("oneTaskCaseName")
                .caseInstanceId("undefined")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionName("oneTaskCaseName")
                .caseInstanceId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionNameLike() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionNameLike("oneTask%").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionNameLike("oneTask%").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionNameLike("oneTask%").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionNameLike("oneTask%")
                .caseInstanceId("undefined")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionNameLike("oneTask%")
                .caseInstanceId("undefined")
                .endOr()
                .list()).hasSize(1);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionNameLike("none%").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionNameLike("none%").caseInstanceId("undefined").endOr().list()).hasSize(0);
    }
    
    @Test
    public void getCaseInstanceByCaseDefinitionNameLikeIgnoreCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionNameLikeIgnoreCase("onetask%").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionNameLikeIgnoreCase("onetask%").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionNameLikeIgnoreCase("onetask%").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionNameLikeIgnoreCase("onetask%")
                .caseInstanceId("undefined")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionNameLikeIgnoreCase("onetask%")
                .caseInstanceId("undefined")
                .endOr()
                .list()).hasSize(1);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionNameLikeIgnoreCase("none%").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionNameLikeIgnoreCase("none%").caseInstanceId("undefined").endOr().list()).hasSize(0);
    }

    @Test
    public void getCaseInstanceByCaseDefinitionId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).count()).isEqualTo(1);
        assertThat(
                cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).caseInstanceId(caseInstance.getId()).count())
                .isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId("undefinedId")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByCaseDefinitionIds() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionIds(Set.of(caseInstance1.getCaseDefinitionId(), caseInstance2.getCaseDefinitionId()))
                .list())
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(caseInstance1.getId(), caseInstance2.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionIds(Set.of(caseInstance1.getCaseDefinitionId(), caseInstance2.getCaseDefinitionId()))
                .caseInstanceId(caseInstance1.getId())
                .list())
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(caseInstance1.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionIds(Set.of(caseInstance1.getCaseDefinitionId(), caseInstance2.getCaseDefinitionId()))
                .caseInstanceId("undefinedId")
                .endOr()
                .list())
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(caseInstance1.getId(), caseInstance2.getId());
    }

    @Test
    public void getCaseInstanceByCaseDefinitionVersion() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionVersion(1).count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionVersion(1).list()).hasSize(2);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionVersion(1)
                .caseInstanceId("undefinedId")
                .endOr()
                .count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseDefinitionVersion(1)
                .caseInstanceId("undefinedId")
                .endOr()
                .list()).hasSize(2);
    }

    @Test
    public void getCaseInstanceByCaseId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceId(caseInstance.getId())
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceId(caseInstance.getId())
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceId(caseInstance.getId())
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByBusinessKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("businessKey")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKey("businessKey").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKey("businessKey").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKey("businessKey").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKey("businessKey")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKey("businessKey")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKey("businessKey")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
    }
    
    @Test
    public void getCaseInstanceByBusinessKeyLike() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("businessKey")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKeyLike("business%").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKeyLike("business%").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKeyLike("business%").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKeyLike("business%")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKeyLike("business%")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).hasSize(1);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKeyLike("none%").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseInstanceBusinessKeyLike("none%").caseDefinitionName("undefinedId").endOr().list()).hasSize(0);
    }
    
    @Test
    public void getCaseInstanceByBusinessKeyLikeIgnoreCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("businessKey")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKeyLikeIgnoreCase("%key").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKeyLikeIgnoreCase("%key").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKeyLikeIgnoreCase("%key").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKeyLikeIgnoreCase("%key")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKeyLikeIgnoreCase("%key")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).hasSize(1);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKeyLikeIgnoreCase("%none").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseInstanceBusinessKeyLikeIgnoreCase("%none").caseDefinitionName("undefinedId").endOr().list()).hasSize(0);
    }
    
    @Test
    public void getCaseInstanceByBusinessStatus() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        
        cmmnRuntimeService.updateBusinessStatus(caseInstance.getId(), "businessStatus");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatus("businessStatus").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatus("businessStatus").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatus("businessStatus").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatus("businessStatus")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatus("businessStatus")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatus("undefined")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult()).isNull();
    }
    
    @Test
    public void getCaseInstanceByBusinessStatusLike() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        
        cmmnRuntimeService.updateBusinessStatus(caseInstance.getId(), "businessStatus");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatusLike("%Status").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatusLike("%Status").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatusLike("%Status").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatusLike("%Status")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatusLike("%Status")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).hasSize(1);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatusLike("%none").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseInstanceBusinessStatusLike("%none").caseDefinitionName("undefinedId").endOr().list()).hasSize(0);
    }
    
    @Test
    public void getCaseInstanceByBusinessStatusLikeIgnoreCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        
        cmmnRuntimeService.updateBusinessStatus(caseInstance.getId(), "businessStatus");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatusLikeIgnoreCase("%status").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatusLikeIgnoreCase("%status").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatusLikeIgnoreCase("%status").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatusLikeIgnoreCase("%status")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceBusinessStatusLikeIgnoreCase("%status")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).hasSize(1);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessStatusLikeIgnoreCase("%none").list()).hasSize(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseInstanceBusinessStatusLikeIgnoreCase("%none").caseDefinitionName("undefinedId").endOr().list()).hasSize(0);
    }

    @Test
    public void getCaseInstanceByStartedBefore() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Calendar todayCal = new GregorianCalendar();
        Calendar dateCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) + 1, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_YEAR));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBefore(dateCal.getTime()).count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBefore(dateCal.getTime()).list()).hasSize(2);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceStartedBefore(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceStartedBefore(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).hasSize(2);
    }

    @Test
    public void getCaseInstanceByStartedAfter() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Calendar todayCal = new GregorianCalendar();
        Calendar dateCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) - 1, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_YEAR));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedAfter(dateCal.getTime()).count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedAfter(dateCal.getTime()).list()).hasSize(2);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceStartedAfter(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceStartedAfter(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).hasSize(2);
    }

    @Test
    public void getCaseInstanceByStartedBy() {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("kermit");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .start();

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBy("kermit").count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBy("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBy("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceStartedBy("kermit")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceStartedBy("kermit")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceStartedBy("kermit")
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId()).isEqualTo(caseInstance.getId());
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceState(CaseInstanceState.ACTIVE).count()).isEqualTo(2);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase").caseInstanceState(CaseInstanceState.ACTIVE).list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase").caseInstanceState(CaseInstanceState.ACTIVE).singleResult().getId()).isEqualTo(caseInstance.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceState(CaseInstanceState.ACTIVE)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count()).isEqualTo(2);
            
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceState(CaseInstanceState.COMPLETED)
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count()).isZero();
            
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId("callBackId").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId("callBackId").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId("callBackId").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackId("callBackId")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackId("callBackId")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackId("callBackId")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
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

        Set<String> callBackIdParameter = new HashSet<>();
        callBackIdParameter.add("callBackId");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackIds(callBackIdParameter)
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackIds(callBackIdParameter)
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackIds(callBackIdParameter)
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());

        callBackIdParameter.add("otherCallBackId");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackIds(callBackIdParameter).list().get(1).getId()).isEqualTo(otherCaseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackIds(callBackIdParameter)
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackIds(callBackIdParameter)
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).extracting(CaseInstance::getId).containsExactlyInAnyOrder(
                caseInstance.getId(),
                otherCaseInstance.getId()
        );
    }

    @Test
    public void getCaseInstanceByCallBackType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .callbackType("callBackType")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackType("callBackType").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackType("callBackType").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackType("callBackType").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackType("callBackType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackType("callBackType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceCallbackType("callBackType")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByReferenceId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .referenceId("testReferenceId")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceReferenceId("testReferenceId").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceReferenceId("testReferenceId").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceReferenceId("testReferenceId").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceReferenceId("testReferenceId")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceReferenceId("testReferenceId")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceReferenceId("testReferenceId")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByReferenceType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .referenceType("testReferenceType")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceReferenceType("testReferenceType").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceReferenceType("testReferenceType").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceReferenceType("testReferenceType").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceReferenceType("testReferenceType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceReferenceType("testReferenceType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .caseInstanceReferenceType("testReferenceType")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByReferenceIdAndType() {
        for (int i = 0; i < 4; i++) {
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .referenceId("testReferenceId")
                    .referenceType("testReferenceType")
                    .start();
        }

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceReferenceId("testReferenceId")
                .caseInstanceReferenceType("testReferenceType").count()).isEqualTo(4);
    }

    @Test
    public void updateAndGetCaseInstanceByReferenceIdAndType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        // Test update for referenceId and referenceType
        CommandExecutor commandExecutor = cmmnEngineConfiguration.getCommandExecutor();
        commandExecutor.execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                CaseInstanceEntity caseInstanceEntity = (CaseInstanceEntity) caseInstance;
                caseInstanceEntity.setReferenceId("testReferenceId");
                caseInstanceEntity.setReferenceType("testReferenceType");
                cmmnEngineConfiguration.getCaseInstanceEntityManager().update(caseInstanceEntity);

                return null;
            }
        });

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceReferenceId("testReferenceId")
                .caseInstanceReferenceType("testReferenceType").count()).isEqualTo(1);
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("tenantId").count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("tenantId").list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("tenantId").singleResult().getId()).isEqualTo(caseInstance.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantId("tenantId")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantId("tenantId")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantId("tenantId")
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId()).isEqualTo(caseInstance.getId());
        } finally {
            cmmnRepositoryService.deleteDeployment(tempDeploymentId, true);
        }
    }

    @Test
    public void getCaseInstanceByTenantIdLike() {
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLike("ten%").count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLike("ten%").list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLike("ten%").singleResult().getId()).isEqualTo(caseInstance.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantIdLike("ten%")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantIdLike("ten%")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantIdLike("ten%")
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId()).isEqualTo(caseInstance.getId());
        } finally {
            cmmnRepositoryService.deleteDeployment(tempDeploymentId, true);
        }
    }
    
    @Test
    public void getCaseInstanceByTenantIdLikeIgnoreCase() {
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLikeIgnoreCase("%id").count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLikeIgnoreCase("%id").list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLikeIgnoreCase("%id").singleResult().getId()).isEqualTo(caseInstance.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantIdLikeIgnoreCase("%id")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantIdLikeIgnoreCase("%id")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantIdLikeIgnoreCase("%id")
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId()).isEqualTo(caseInstance.getId());
            
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLikeIgnoreCase("%none").list()).hasSize(0);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseInstanceTenantIdLikeIgnoreCase("%none").caseDefinitionId("undefined").endOr().list()).hasSize(0);
            
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceWithoutTenantId().count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceWithoutTenantId().list().get(0).getId()).isNotEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceWithoutTenantId().singleResult().getId()).isNotEqualTo(caseInstance.getId());

            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceWithoutTenantId()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceWithoutTenantId()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId()).isNotEqualTo(caseInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                    .or()
                    .caseInstanceWithoutTenantId()
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId()).isNotEqualTo(caseInstance.getId());
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
        
        List<String> queryIds = cmmnRuntimeService.createCaseInstanceQuery().activePlanItemDefinitionId("humanTask3").list().stream()
            .map(CaseInstance::getId)
            .collect(Collectors.toList());
        
        assertThat(queryIds.size()).isEqualTo(1);
        assertThat(queryIds.contains(caseInstance2.getId())).isTrue();
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).activePlanItemDefinitionId("humanTask1").count()).isZero();
        
        queryIds = cmmnRuntimeService.createCaseInstanceQuery().activePlanItemDefinitionId("humanTask1").list().stream()
                .map(CaseInstance::getId)
                .collect(Collectors.toList());
        
        assertThat(queryIds.size()).isEqualTo(2);
        assertThat(queryIds.contains(caseInstance2.getId())).isTrue();
        assertThat(queryIds.contains(caseInstance3.getId())).isTrue();
        
        Set<String> planItemDefinitionIds = new HashSet<>();
        planItemDefinitionIds.add("humanTask1");
        planItemDefinitionIds.add("humanTask3");
        
        queryIds = cmmnRuntimeService.createCaseInstanceQuery().activePlanItemDefinitionIds(planItemDefinitionIds).list().stream()
                .map(CaseInstance::getId)
                .collect(Collectors.toList());
     
        assertThat(queryIds.size()).isEqualTo(2);
        assertThat(queryIds.contains(caseInstance2.getId())).isTrue();
        assertThat(queryIds.contains(caseInstance3.getId())).isTrue();
        
        planItemDefinitionIds = new HashSet<>();
        planItemDefinitionIds.add("humanTask2");
        planItemDefinitionIds.add("humanTask4");
        
        queryIds = cmmnRuntimeService.createCaseInstanceQuery().activePlanItemDefinitionIds(planItemDefinitionIds).list().stream()
                .map(CaseInstance::getId)
                .collect(Collectors.toList());
        
        assertThat(queryIds.size()).isEqualTo(1);
        assertThat(queryIds.contains(caseInstance1.getId())).isTrue();
        
        planItemDefinitionIds = new HashSet<>();
        planItemDefinitionIds.add("humanTask88");
        planItemDefinitionIds.add("humanTask99");
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().activePlanItemDefinitionIds(planItemDefinitionIds).count()).isZero();
    }

    @Test
    public void getCaseInstanceByInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedUser("kermit")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
    }
    
    @Test
    public void getCaseInstanceByInvolvedUserIdentityLink() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", "specialLink");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit", "specialLink").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit", "specialLink").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit", "specialLink").singleResult().getId()).isEqualTo(caseInstance.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit", "wrongType").count()).isZero();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedUser("kermit", "specialLink")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedUser("kermit", "specialLink")
                .caseDefinitionKey("oneTaskCase")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedUser("kermit", "wrongType")
                .caseDefinitionKey("undefined")
                .endOr()
                .count()).isZero();
    }

    @Test
    public void getCaseInstanceByInvolvedGroup() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroups(Collections.singleton("testGroup"))
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroups(Collections.singleton("testGroup"))
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroups(Collections.singleton("testGroup"))
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId()).isEqualTo(caseInstance.getId());
    }
    
    @Test
    public void getCaseInstanceByInvolvedGroupIdentityLink() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", "specialLink");
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", "extraLink");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroup("testGroup", "specialLink").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroup("testGroup", "specialLink").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroup("testGroup", "specialLink").singleResult().getId())
                .isEqualTo(caseInstance.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroup("testGroup2", "wrongType").count()).isZero();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroup("testGroup", "specialLink")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroup("testGroup2", "extraLink")
                .caseDefinitionKey("oneTaskCase")
                .endOr()
                .count())
                .isEqualTo(1);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroup("testGroup2", "wrongType")
                .caseDefinitionKey("wrongKey")
                .endOr()
                .count())
                .isZero();
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).hasSize(2);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(3);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).hasSize(3);
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .list().get(0).getId()).isEqualTo(caseInstance2.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .singleResult().getId()).isEqualTo(caseInstance2.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count()).isEqualTo(3);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery()
                .or()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list()).hasSize(3);
    }

    @Test
    public void getCaseInstanceByIdWithoutTenant() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("caseInstance1")
                .start();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("caseInstance2")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).caseInstanceWithoutTenantId().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).caseInstanceWithoutTenantId().list()).hasSize(1);
    }

    @Test
    public void testQueryInstantVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        Instant instant1 = Instant.now();
        vars.put("instantVar", instant1);

        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        Instant instant2 = instant1.plusSeconds(1);
        vars = new HashMap<>();
        vars.put("instantVar", instant1);
        vars.put("instantVar2", instant2);
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        Instant nextYear = instant1.plus(365, ChronoUnit.DAYS);
        vars = new HashMap<>();
        vars.put("instantVar", nextYear);
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        Instant nextMonth = instant1.plus(30, ChronoUnit.DAYS);

        Instant twoYearsLater = instant1.plus(730, ChronoUnit.DAYS);

        Instant oneYearAgo = instant1.minus(365, ChronoUnit.DAYS);

        // Query on single instant variable, should result in 2 matches
        CaseInstanceQuery query = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("instantVar", instant1);
        List<CaseInstance> caseInstances = query.list();
        Assertions.assertThat(caseInstances).hasSize(2);

        // Query on two instant variables, should result in single value
        query = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("instantVar", instant1).variableValueEquals("instantVar2", instant2);
        CaseInstance caseInstance = query.singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance2.getId());

        // Query with unexisting variable value
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("instantVar", instant1.minus(1, ChronoUnit.HOURS)).singleResult();
        Assertions.assertThat(caseInstance).isNull();

        // Test NOT_EQUALS
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("instantVar", instant1).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        // Test GREATER_THAN
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("instantVar", nextMonth).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("instantVar", nextYear).count()).isZero();
        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("instantVar", oneYearAgo).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("instantVar", nextMonth).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("instantVar", nextYear).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("instantVar", oneYearAgo).count()).isEqualTo(3);

        // Test LESS_THAN
        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("instantVar", nextYear).list();
        Assertions.assertThat(caseInstances)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        caseInstance1.getId(),
                        caseInstance2.getId()
                );

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("instantVar", instant1).count()).isZero();
        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("instantVar", twoYearsLater).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("instantVar", nextYear).list();
        Assertions.assertThat(caseInstances).hasSize(3);

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("instantVar", oneYearAgo).count()).isZero();

        // Test value-only matching
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(nextYear).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(instant1).list();
        Assertions.assertThat(caseInstances)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        caseInstance1.getId(),
                        caseInstance2.getId()
                );

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(twoYearsLater).singleResult();
        Assertions.assertThat(caseInstance).isNull();
    }

    @Test
    public void testQueryLocalDateVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        LocalDate localDate = LocalDate.now();
        vars.put("localDateVar", localDate);

        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        LocalDate localDate2 = localDate.plusDays(1);
        vars = new HashMap<>();
        vars.put("localDateVar", localDate);
        vars.put("localDateVar2", localDate2);
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        LocalDate nextYear = localDate.plusYears(1);
        vars = new HashMap<>();
        vars.put("localDateVar", nextYear);
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        LocalDate nextMonth = localDate.plusMonths(1);

        LocalDate twoYearsLater = localDate.plusYears(2);

        LocalDate oneYearAgo = localDate.minusYears(2);

        // Query on single localDate variable, should result in 2 matches
        CaseInstanceQuery query = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("localDateVar", localDate);
        List<CaseInstance> caseInstances = query.list();
        Assertions.assertThat(caseInstances).hasSize(2);

        // Query on two instant variables, should result in single value
        query = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("localDateVar", localDate).variableValueEquals("localDateVar2", localDate2);
        CaseInstance caseInstance = query.singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance2.getId());

        // Query with unexisting variable value
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("localDateVar", localDate.minusDays(1)).singleResult();
        Assertions.assertThat(caseInstance).isNull();

        // Test NOT_EQUALS
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("localDateVar", localDate).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        // Test GREATER_THAN
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("localDateVar", nextMonth).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("localDateVar", nextYear).count()).isZero();
        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("localDateVar", oneYearAgo).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("localDateVar", nextMonth).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("localDateVar", nextYear).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("localDateVar", oneYearAgo).count()).isEqualTo(3);

        // Test LESS_THAN
        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("localDateVar", nextYear).list();
        Assertions.assertThat(caseInstances)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        caseInstance1.getId(),
                        caseInstance2.getId()
                );

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("localDateVar", localDate).count()).isZero();
        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("localDateVar", twoYearsLater).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("localDateVar", nextYear).list();
        Assertions.assertThat(caseInstances).hasSize(3);

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("localDateVar", oneYearAgo).count()).isZero();

        // Test value-only matching
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(nextYear).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(localDate).list();
        Assertions.assertThat(caseInstances)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        caseInstance1.getId(),
                        caseInstance2.getId()
                );

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(twoYearsLater).singleResult();
        Assertions.assertThat(caseInstance).isNull();
    }

    @Test
    public void testQueryLocalDateTimeVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        LocalDateTime localDateTime = LocalDateTime.now();
        vars.put("localDateTimeVar", localDateTime);

        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        LocalDateTime localDateTime2 = localDateTime.plusDays(1);
        vars = new HashMap<>();
        vars.put("localDateTimeVar", localDateTime);
        vars.put("localDateTimeVar2", localDateTime2);
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        LocalDateTime nextYear = localDateTime.plusYears(1);
        vars = new HashMap<>();
        vars.put("localDateTimeVar", nextYear);
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        LocalDateTime nextMonth = localDateTime.plusMonths(1);

        LocalDateTime twoYearsLater = localDateTime.plusYears(2);

        LocalDateTime oneYearAgo = localDateTime.minusYears(2);

        // Query on single localDateTime variable, should result in 2 matches
        CaseInstanceQuery query = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("localDateTimeVar", localDateTime);
        List<CaseInstance> caseInstances = query.list();
        Assertions.assertThat(caseInstances).hasSize(2);

        // Query on two localDateTime variables, should result in single value
        query = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("localDateTimeVar", localDateTime)
                .variableValueEquals("localDateTimeVar2", localDateTime2);
        CaseInstance caseInstance = query.singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance2.getId());

        // Query with unexisting variable value
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("localDateTimeVar", localDateTime.minusSeconds(1)).singleResult();
        Assertions.assertThat(caseInstance).isNull();

        // Test NOT_EQUALS
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("localDateTimeVar", localDateTime).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        // Test GREATER_THAN
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("localDateTimeVar", nextMonth).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("localDateTimeVar", nextYear).count()).isZero();
        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("localDateTimeVar", oneYearAgo).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("localDateTimeVar", nextMonth).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("localDateTimeVar", nextYear).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("localDateTimeVar", oneYearAgo).count())
                .isEqualTo(3);

        // Test LESS_THAN
        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("localDateTimeVar", nextYear).list();
        Assertions.assertThat(caseInstances)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        caseInstance1.getId(),
                        caseInstance2.getId()
                );

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("localDateTimeVar", localDateTime).count()).isZero();
        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("localDateTimeVar", twoYearsLater).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("localDateTimeVar", nextYear).list();
        Assertions.assertThat(caseInstances).hasSize(3);

        Assertions.assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("localDateTimeVar", oneYearAgo).count()).isZero();

        // Test value-only matching
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(nextYear).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(localDateTime).list();
        Assertions.assertThat(caseInstances)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        caseInstance1.getId(),
                        caseInstance2.getId()
                );

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(twoYearsLater).singleResult();
        Assertions.assertThat(caseInstance).isNull();
    }

    @Test
    public void testQueryUUIDVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        UUID someUUID = UUID.randomUUID();
        vars.put("uuidVar", someUUID);

        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        UUID someUUID2 = UUID.randomUUID();
        vars = new HashMap<>();
        vars.put("uuidVar", someUUID);
        vars.put("uuidVar2", someUUID2);
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        UUID someUUID3 = UUID.randomUUID();
        vars = new HashMap<>();
        vars.put("uuidVar", someUUID3);
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        // Query on single uuid variable, should result in 2 matches
        CaseInstanceQuery query = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("uuidVar", someUUID);
        List<CaseInstance> caseInstances = query.list();
        Assertions.assertThat(caseInstances).hasSize(2);

        // Query on two uuid variables, should result in single value
        query = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("uuidVar", someUUID)
                .variableValueEquals("uuidVar2", someUUID2);
        CaseInstance caseInstance = query.singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance2.getId());

        UUID unexistingUUID = UUID.randomUUID();
        // Query with unexisting variable value
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("uuidVar", unexistingUUID).singleResult();
        Assertions.assertThat(caseInstance).isNull();

        // Test NOT_EQUALS
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("uuidVar", someUUID).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        // Test value-only matching
        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(someUUID3).singleResult();
        Assertions.assertThat(caseInstance).isNotNull();
        Assertions.assertThat(caseInstance.getId()).isEqualTo(caseInstance3.getId());

        caseInstances = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(someUUID).list();
        Assertions.assertThat(caseInstances)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        caseInstance1.getId(),
                        caseInstance2.getId()
                );

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals(unexistingUUID).singleResult();
        Assertions.assertThat(caseInstance).isNull();
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
                if ("pt".equals(locale)) {
                    caseInstance.setLocalizedName("Caso 1");
                }
            }

            @Override
            public void localize(HistoricCaseInstance historicCaseInstance, String locale, boolean withLocalizationFallback) {

            }
        });

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(createdCase.getId()).singleResult();
        assertThat(caseInstance.getName()).isEqualTo("Default name");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(createdCase.getId()).locale("pt").singleResult();
        assertThat(caseInstance.getName()).isEqualTo("Caso 1");
    }

    @Test
    public void testQueryCaseInstanceReturnsCaseDefinitionInformation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("stringVar", "test")
                .start();

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getCaseDefinitionKey()).isEqualTo("oneTaskCase");
        assertThat(caseInstance.getCaseDefinitionName()).isEqualTo("oneTaskCaseName");
        assertThat(caseInstance.getCaseDefinitionVersion()).isEqualTo(1);
        assertThat(caseInstance.getCaseDefinitionDeploymentId()).isEqualTo(deploymentId);
        assertThat(caseInstance.getCaseVariables()).isEmpty();
    }

    @Test
    public void testQueryCaseInstanceWithVariablesReturnsCaseDefinitionInformation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("stringVar", "test")
                .start();

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables()
                .singleResult();

        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getCaseDefinitionKey()).isEqualTo("oneTaskCase");
        assertThat(caseInstance.getCaseDefinitionName()).isEqualTo("oneTaskCaseName");
        assertThat(caseInstance.getCaseDefinitionVersion()).isEqualTo(1);
        assertThat(caseInstance.getCaseDefinitionDeploymentId()).isEqualTo(deploymentId);
        assertThat(caseInstance.getCaseVariables()).containsOnly(
                entry("stringVar","test")
        );
    }


    @Test
    public void testQueryVariableValueEqualsAndNotEquals() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With null value")
                .variable("var", null)
                .start();

        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With long value")
                .variable("var", 100L)
                .start();

        CaseInstance caseInstance4 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", "TEST").list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", caseInstance2.getId()),
                        tuple("With long value", caseInstance3.getId()),
                        tuple("With double value", caseInstance4.getId())
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", "TEST").list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseInstance1.getId())
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", 100L).list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseInstance1.getId()),
                        tuple("With null value", caseInstance2.getId()),
                        tuple("With double value", caseInstance4.getId())
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", 100L).list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With long value", caseInstance3.getId())
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", 45.55).list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseInstance1.getId()),
                        tuple("With null value", caseInstance2.getId()),
                        tuple("With long value", caseInstance3.getId())
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", 45.55).list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With double value", caseInstance4.getId())
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", "test").list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseInstance1.getId()),
                        tuple("With null value", caseInstance2.getId()),
                        tuple("With long value", caseInstance3.getId()),
                        tuple("With double value", caseInstance4.getId())
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEqualsIgnoreCase("var", "test").list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", caseInstance2.getId()),
                        tuple("With long value", caseInstance3.getId()),
                        tuple("With double value", caseInstance4.getId())
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", "test").list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .isEmpty();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "test").list())
                .extracting(CaseInstance::getName, CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseInstance1.getId())
                );
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
        
        List<CaseInstance> result = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceRootScopeId(caseInstance.getId()).list();
        assertThat(result)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        oneTaskCasePlanItemInstance.getReferenceId(),
                        caseTaskWithHumanTasksPlanItemInstance.getReferenceId(),
                        caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId(),
                        oneTaskCase2PlanItemInstance.getReferenceId()
                );

        result = cmmnRuntimeService.createCaseInstanceQuery().or().caseInstanceRootScopeId(caseInstance.getId()).endOr().list();
        assertThat(result)
                .extracting(CaseInstance::getId)
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
        
        List<CaseInstance> result = cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).list();
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        oneTaskCasePlanItemInstance.getReferenceId(),
                        caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId()
                );
        
        result = cmmnRuntimeService.createCaseInstanceQuery().or().parentCaseInstanceId(caseInstance.getId()).caseInstanceBusinessKey("unexisting").endOr().list();
        assertThat(result).hasSize(2);
        
        result = cmmnRuntimeService.createCaseInstanceQuery().or().parentCaseInstanceId(caseInstance.getId()).caseDefinitionKey("oneTaskCase").endOr().list();
        assertThat(result).hasSize(5);
        
        result = cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(oneTaskCasePlanItemInstance.getReferenceId()).list();
        assertThat(result).isEmpty();
        
        result = cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseTaskSimpleCaseWithCaseTasksPlanItemInstance.getReferenceId()).list();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(caseTaskWithHumanTasksPlanItemInstance.getReferenceId());
        
        result = cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId()).list();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(oneTaskCase2PlanItemInstance.getReferenceId());
        
        result = cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(oneTaskCase2PlanItemInstance.getReferenceId()).list();
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

        List<CaseInstance> result = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceParentScopeId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId()).list();

        assertThat(result)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        oneTaskCase2PlanItemInstance.getReferenceId()
                );

        result = cmmnRuntimeService.createCaseInstanceQuery().or().caseInstanceParentScopeId(caseTaskWithHumanTasksPlanItemInstance.getReferenceId()).endOr()
                .list();

        assertThat(result)
                .extracting(CaseInstance::getId)
                .containsExactlyInAnyOrder(
                        oneTaskCase2PlanItemInstance.getReferenceId()
                );
    }
}
