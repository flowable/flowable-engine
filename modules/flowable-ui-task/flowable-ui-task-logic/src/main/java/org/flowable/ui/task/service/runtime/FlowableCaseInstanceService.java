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
package org.flowable.ui.task.service.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.content.api.ContentService;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.idm.api.User;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.task.model.runtime.CaseInstanceRepresentation;
import org.flowable.ui.task.model.runtime.CreateCaseInstanceRepresentation;
import org.flowable.ui.task.model.runtime.MilestoneRepresentation;
import org.flowable.ui.task.model.runtime.PlanItemInstanceRepresentation;
import org.flowable.ui.task.model.runtime.StageRepresentation;
import org.flowable.ui.task.model.runtime.UserEventListenerRepresentation;
import org.flowable.ui.task.service.api.UserCache;
import org.flowable.ui.task.service.api.UserCache.CachedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
@Service
@Transactional
public class FlowableCaseInstanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableCaseInstanceService.class);

    @Autowired
    protected CmmnRepositoryService cmmnRepositoryService;

    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;

    @Autowired
    protected CmmnHistoryService cmmnHistoryService;

    @Autowired
    protected FormService formService;

    @Autowired
    protected FormRepositoryService formRepositoryService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected ContentService contentService;

    @Autowired
    protected FlowableCommentService commentService;

    @Autowired
    protected UserCache userCache;

    public CaseInstanceRepresentation getCaseInstance(String caseInstanceId) {

        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        CaseDefinition caseDefinition = cmmnRepositoryService.getCaseDefinition(caseInstance.getCaseDefinitionId());

        User userRep = null;
        if (caseInstance.getStartUserId() != null) {
            CachedUser user = userCache.getUser(caseInstance.getStartUserId());
            if (user != null && user.getUser() != null) {
                userRep = user.getUser();
            }
        }

        CaseInstanceRepresentation caseInstanceResult = new CaseInstanceRepresentation(caseInstance, caseDefinition, 
                        caseDefinition.hasGraphicalNotation(), userRep);

        return caseInstanceResult;
    }

    public FormInfo getCaseInstanceStartForm(String caseInstanceId) {

        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (!permissionService.hasReadPermissionOnCaseInstance(SecurityUtils.getCurrentUserObject(), caseInstance, caseInstanceId)) {
            throw new NotFoundException("Case with id: " + caseInstanceId + " does not exist or is not available for this user");
        }

        return cmmnRuntimeService.getStartFormModel(caseInstance.getCaseDefinitionId(), caseInstance.getId());
    }

    public ResultListDataRepresentation getCaseInstanceActiveStages(String caseInstanceId) {

        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (!permissionService.hasReadPermissionOnCaseInstance(SecurityUtils.getCurrentUserObject(), caseInstance, caseInstanceId)) {
            throw new NotFoundException("Case with id: " + caseInstanceId + " does not exist or is not available for this user");
        }

        List<HistoricPlanItemInstance> stages = new ArrayList<>();

        //Query criteria could be improved to query for more than one state and/or a "not" condition
        //AVAILABLE stages
        stages.addAll(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstance.getId())
            .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
            .planItemInstanceState(PlanItemInstanceState.AVAILABLE)
            .list());

        //ACTIVE stages
        stages.addAll(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstance.getId())
            .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
            .planItemInstanceState(PlanItemInstanceState.ACTIVE)
            .list());

        List<StageRepresentation> stageRepresentations = stages.stream()
            .map(p -> new StageRepresentation(p.getName(), p.getState(), p.getCreateTime(), p.getEndedTime()))
            .collect(Collectors.toList());

        return new ResultListDataRepresentation(stageRepresentations);
    }

    public ResultListDataRepresentation getCaseInstanceEndedStages(String caseInstanceId) {

        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (!permissionService.hasReadPermissionOnCaseInstance(SecurityUtils.getCurrentUserObject(), caseInstance, caseInstanceId)) {
            throw new NotFoundException("Case with id: " + caseInstanceId + " does not exist or is not available for this user");
        }

        List<HistoricPlanItemInstance> stages = new ArrayList<>();

        //Query criteria could be improved to query for more than one state and/or a "not" condition
        //TERMINATED stages
        stages.addAll(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstance.getId())
            .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
            .planItemInstanceState(PlanItemInstanceState.TERMINATED)
            .list());

        //COMPLETED stages
        stages.addAll(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstance.getId())
            .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
            .planItemInstanceState(PlanItemInstanceState.COMPLETED)
            .list());

        List<StageRepresentation> stageRepresentations = stages.stream()
            .map(p -> new StageRepresentation(p.getName(), p.getState(), p.getCreateTime(), p.getEndedTime()))
            .collect(Collectors.toList());

        return new ResultListDataRepresentation(stageRepresentations);
    }

    public ResultListDataRepresentation getCaseInstanceAvailableMilestones(String caseInstanceId) {

        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (!permissionService.hasReadPermissionOnCaseInstance(SecurityUtils.getCurrentUserObject(), caseInstance, caseInstanceId)) {
            throw new NotFoundException("Case with id: " + caseInstanceId + " does not exist or is not available for this user");
        }

        List<HistoricPlanItemInstance> milestones = new ArrayList<>();

        //Available
        milestones.addAll(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstance.getId())
            .planItemInstanceDefinitionType(PlanItemDefinitionType.MILESTONE)
            .planItemInstanceState(PlanItemInstanceState.AVAILABLE)
            .list());

        List<MilestoneRepresentation> milestoneRepresentations = milestones.stream()
            .map(p -> new MilestoneRepresentation(p.getName(), p.getState(), p.getCreateTime()))
            .collect(Collectors.toList());

        return new ResultListDataRepresentation(milestoneRepresentations);
    }

    public ResultListDataRepresentation getCaseInstanceEndedMilestones(String caseInstanceId) {

        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (!permissionService.hasReadPermissionOnCaseInstance(SecurityUtils.getCurrentUserObject(), caseInstance, caseInstanceId)) {
            throw new NotFoundException("Case with id: " + caseInstanceId + " does not exist or is not available for this user");
        }

        List<HistoricPlanItemInstance> milestones = new ArrayList<>();

        //Query criteria could be improved to query for more than one state and/or a "not" condition
        //TERMINATED milestones
        milestones.addAll(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstance.getId())
            .planItemInstanceDefinitionType(PlanItemDefinitionType.MILESTONE)
            .planItemInstanceState(PlanItemInstanceState.TERMINATED)
            .list());

        //COMPLETED milestones
        milestones.addAll(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstance.getId())
            .planItemInstanceDefinitionType(PlanItemDefinitionType.MILESTONE)
            .planItemInstanceState(PlanItemInstanceState.COMPLETED)
            .list());

        List<MilestoneRepresentation> milestoneRepresentations = milestones.stream()
            .map(p -> new MilestoneRepresentation(p.getName(), p.getState(), p.getCreateTime()))
            .collect(Collectors.toList());

        return new ResultListDataRepresentation(milestoneRepresentations);
    }

    public CaseInstanceRepresentation startNewCaseInstance(CreateCaseInstanceRepresentation startRequest) {
        if (StringUtils.isEmpty(startRequest.getCaseDefinitionId())) {
            throw new BadRequestException("Case definition id is required");
        }

        CaseDefinition caseDefinition = cmmnRepositoryService.getCaseDefinition(startRequest.getCaseDefinitionId());

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionId(startRequest.getCaseDefinitionId())
            .name(startRequest.getName())
            .outcome(startRequest.getOutcome())
            .startFormVariables(startRequest.getValues())
            .startWithForm();

        User user = null;
        if (caseInstance.getStartUserId() != null) {
            CachedUser cachedUser = userCache.getUser(caseInstance.getStartUserId());
            if (cachedUser != null && cachedUser.getUser() != null) {
                user = cachedUser.getUser();
            }
        }
        return new CaseInstanceRepresentation(caseInstance, caseDefinition, caseDefinition.hasGraphicalNotation(), user);
    }

    public ResultListDataRepresentation getCaseInstanceAvailableUserEventListeners(String caseInstanceId) {
        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (!permissionService.hasReadPermissionOnCaseInstance(SecurityUtils.getCurrentUserObject(), caseInstance, caseInstanceId)) {
            throw new NotFoundException("Case with id: " + caseInstanceId + " does not exist or is not available for this user");
        }

        List<UserEventListenerInstance> userEventListenerInstances = new ArrayList<>();

        //Available UEL
        userEventListenerInstances.addAll(cmmnRuntimeService.createUserEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .stateAvailable()
            .list());

        //Suspended UEL
        userEventListenerInstances.addAll(cmmnRuntimeService.createUserEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .stateSuspended()
            .list());

        List<UserEventListenerRepresentation> collectedUserEventListenerRepresentations = userEventListenerInstances.stream()
            .map(e -> new UserEventListenerRepresentation(e.getId(), e.getName(), e.getState(), null))
            .collect(Collectors.toList());

        return new ResultListDataRepresentation(collectedUserEventListenerRepresentations);
    }

    public ResultListDataRepresentation getCaseInstanceCompletedUserEventListeners(String caseInstanceId) {
        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (!permissionService.hasReadPermissionOnCaseInstance(SecurityUtils.getCurrentUserObject(), caseInstance, caseInstanceId)) {
            throw new NotFoundException("Case with id: " + caseInstanceId + " does not exist or is not available for this user");
        }

        List<HistoricPlanItemInstance> completedUserEventListeners = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
            .planItemInstanceCaseInstanceId(caseInstance.getId())
            .planItemInstanceDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
            .planItemInstanceState(PlanItemInstanceState.COMPLETED)
            .list();

        List<UserEventListenerRepresentation> collectedUserEventListenerRepresentations = completedUserEventListeners.stream()
            .map(e -> new UserEventListenerRepresentation(e.getId(), e.getName(), e.getState(), e.getEndedTime()))
            .collect(Collectors.toList());

        return new ResultListDataRepresentation(collectedUserEventListenerRepresentations);
    }

    public void triggerUserEventListener(String caseInstanceId, String userEventListenerId) {

        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (!permissionService.hasReadPermissionOnCaseInstance(SecurityUtils.getCurrentUserObject(), caseInstance, caseInstanceId)) {
            throw new NotFoundException("Case with id: " + caseInstanceId + " does not exist or is not available for this user");
        }

        if (StringUtils.isEmpty(userEventListenerId)) {
            throw new BadRequestException("User event listener id is required");
        }

        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .id(userEventListenerId)
            .singleResult();

        if (userEventListenerInstance == null) {
            throw new NotFoundException("User event listener not found");
        }

        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

    }

    public ResultListDataRepresentation getCaseInstanceEnabledPlanItemInstances(String caseInstanceId) {
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .planItemInstanceState(PlanItemInstanceState.ENABLED)
            .list();

        List<PlanItemInstanceRepresentation> representations = new ArrayList<>(planItemInstances.size());
        for (PlanItemInstance planItemInstance : planItemInstances) {
            PlanItemInstanceRepresentation planItemInstanceRepresentation = new PlanItemInstanceRepresentation();
            planItemInstanceRepresentation.setId(planItemInstance.getId());
            planItemInstanceRepresentation.setCaseDefinitionId(planItemInstance.getCaseDefinitionId());
            planItemInstanceRepresentation.setCaseInstanceId(planItemInstance.getCaseInstanceId());
            planItemInstanceRepresentation.setStageInstanceId(planItemInstance.getStageInstanceId());
            planItemInstanceRepresentation.setStage(planItemInstance.isStage());
            planItemInstanceRepresentation.setElementId(planItemInstance.getElementId());
            planItemInstanceRepresentation.setPlanItemDefinitionId(planItemInstance.getPlanItemDefinitionId());
            planItemInstanceRepresentation.setPlanItemDefinitionType(planItemInstance.getPlanItemDefinitionType());
            planItemInstanceRepresentation.setName(planItemInstance.getName());
            planItemInstanceRepresentation.setState(planItemInstance.getState());
            planItemInstanceRepresentation.setCreateTime(planItemInstance.getCreateTime());
            representations.add(planItemInstanceRepresentation);
        }

        return new ResultListDataRepresentation(representations);
    }

    public void startEnabledPlanItemInstance(String caseInstanceId, String planItemInstanceId) {
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .planItemInstanceId(planItemInstanceId)
            .planItemInstanceState(PlanItemInstanceState.ENABLED)
            .singleResult();

        if (planItemInstance == null) {
            throw new NotFoundException("No enabled planitem instance found with id " + planItemInstanceId);
        }

        cmmnRuntimeService.startPlanItemInstance(planItemInstanceId);
    }

    public void deleteCaseInstance(String caseInstanceId) {

        User currentUser = SecurityUtils.getCurrentUserObject();

        HistoricCaseInstance caseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .startedBy(String.valueOf(currentUser.getId())) // Permission
            .singleResult();

        if (caseInstance == null) {
            throw new NotFoundException("Case with id: " + caseInstanceId + " does not exist or is not started by this user");
        }

        if (caseInstance.getEndTime() != null) {
            // Check if a hard delete of case instance is allowed
            /*if (!permissionService.canDeleteProcessInstance(currentUser, processInstance)) {
                throw new NotFoundException("Process with id: " + processInstanceId + " is already completed and can't be deleted");
            }*/

            // Delete all content related to the case instance
            contentService.deleteContentItemsByScopeIdAndScopeType(caseInstanceId, ScopeTypes.CMMN);

            // Finally, delete all history for this instance in the engine
            cmmnHistoryService.deleteHistoricCaseInstance(caseInstanceId);

        } else {
            cmmnRuntimeService.terminateCaseInstance(caseInstanceId);
        }
    }
}
