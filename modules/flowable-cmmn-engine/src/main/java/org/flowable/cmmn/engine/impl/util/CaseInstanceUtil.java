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
package org.flowable.cmmn.engine.impl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Joram Barrez
 */
public class CaseInstanceUtil {

    /**
     * Returns a list of {@link PlanItemInstanceEntity} instances for the given {@link CaseInstanceEntity}, irregardless of the state.
     */
    public static List<PlanItemInstanceEntity> findChildPlanItemInstances(CaseInstanceEntity caseInstanceEntity, PlanItem planItem) {
        return getAllChildPlanItemInstances(caseInstanceEntity).stream()
            .filter(planItemInstance -> planItem.getId().equals(planItemInstance.getPlanItem().getId()))
            .collect(Collectors.toList());
    }

    /**
     * Returns a list of {@link PlanItemInstanceEntity} instances for the given {@link CaseInstanceEntity},
     * which have a plan item that matches the given plan item and which are non terminal.
     */
    public static List<PlanItemInstanceEntity> findNonTerminalChildPlanItemInstances(CaseInstanceEntity caseInstanceEntity, PlanItem planItem) {
        List<PlanItemInstanceEntity> childPlanItemInstances = getAllChildPlanItemInstances(caseInstanceEntity);
        return childPlanItemInstances.stream()
            .filter(planItemInstance -> planItem.getId().equals(planItemInstance.getPlanItem().getId()) && !PlanItemInstanceState.isInTerminalState(planItemInstance))
            .collect(Collectors.toList());
    }

    /**
     * Returns a list of {@link PlanItemInstanceEntity} instances for the given {@link CaseInstanceEntity},
     * which have a plan item that matches any of the given plan items and which are in any state.
     */
    public static List<PlanItemInstanceEntity> findChildPlanItemInstances(CaseInstanceEntity caseInstanceEntity, List<PlanItem> planItems) {
        List<String> planItemIds = planItems.stream().map(PlanItem::getId).collect(Collectors.toList());
        List<PlanItemInstanceEntity> childPlanItemInstances = getAllChildPlanItemInstances(caseInstanceEntity);
        return childPlanItemInstances.stream()
            .filter(planItemInstance -> planItemIds.contains(planItemInstance.getPlanItem().getId()))
            .collect(Collectors.toList());
    }

    /**
     * Similar to {@link #findChildPlanItemInstances(CaseInstanceEntity, List)}, but returns a map planItemId, List
     */
    public static Map<String, List<PlanItemInstanceEntity>> findChildPlanItemInstancesMap(CaseInstanceEntity caseInstanceEntity, List<PlanItem> planItems) {

        Map<String, List<PlanItemInstanceEntity>> result = new HashMap<>();

        List<String> planItemIds = planItems.stream().map(PlanItem::getId).collect(Collectors.toList());
        List<PlanItemInstanceEntity> childPlanItemInstances = getAllChildPlanItemInstances(caseInstanceEntity);

        for (PlanItemInstanceEntity childPlanItemInstance : childPlanItemInstances) {
            PlanItem childPlanItem = childPlanItemInstance.getPlanItem();
            if (planItemIds.contains(childPlanItem.getId())) {
                if (!result.containsKey(childPlanItem.getId())) {
                    result.put(childPlanItem.getId(), new ArrayList<>());
                }
                result.get(childPlanItem.getId()).add(childPlanItemInstance);
            }
        }

        return result;
    }

    /**
     * Returns a list of {@link PlanItemInstanceEntity} instances for the given {@link CaseInstanceEntity}, without any filtering.
     */
    public static List<PlanItemInstanceEntity> getAllChildPlanItemInstances(CaseInstanceEntity caseInstanceEntity) {
        if (caseInstanceEntity == null) {
            throw new FlowableException("Programmatic error: case instance entity is null");
        }

        // Typically, this comes out of the cache as the child plan item instance are cached on case instance fetch
        List<PlanItemInstanceEntity> planItemInstances = new ArrayList<>();
        internalCollectPlanItemInstances(caseInstanceEntity, planItemInstances);
        return planItemInstances;
    }

    protected static void internalCollectPlanItemInstances(PlanItemInstanceContainer planItemInstanceContainer, List<PlanItemInstanceEntity> planItemInstances) {
        List<PlanItemInstanceEntity> childPlanItemInstances = planItemInstanceContainer.getChildPlanItemInstances();
        if (childPlanItemInstances != null && !childPlanItemInstances.isEmpty()) {
            for (PlanItemInstanceEntity childPlanItemInstanceEntity : childPlanItemInstances) {
                planItemInstances.add(childPlanItemInstanceEntity);
                internalCollectPlanItemInstances(childPlanItemInstanceEntity, planItemInstances);
            }
        }
    }

}