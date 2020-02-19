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
package org.flowable.cmmn.engine.impl.delegate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.TaskWithFieldExtensions;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.el.FixedValue;

/**
 * @author Joram Barrez
 */
public class CmmnDelegateHelper {

    /**
     * Returns the {@link CmmnModel} matching the case definition cmmn model for the case definition of the passed {@link DelegatePlanItemInstance}.
     */
    public static CmmnModel getCmmnModel(DelegatePlanItemInstance  planItemInstance) {
        if (planItemInstance == null) {
            throw new  FlowableException("Null planItemInstance passed");
        }
        return CaseDefinitionUtil.getCmmnModel(planItemInstance.getCaseDefinitionId());
    }

    /**
     * Returns the current {@link CmmnElement} where the {@link DelegatePlanItemInstance} is currently at.
     */
    public static CmmnElement getCmmnElement(DelegatePlanItemInstance planItemInstance) {
        CmmnModel cmmnModel =  getCmmnModel(planItemInstance);
        CaseElement caseElement = null;
        if (planItemInstance.getPlanItem() != null) {
            caseElement = cmmnModel.getPrimaryCase().getAllCaseElements().get(planItemInstance.getPlanItem().getId());
            if (caseElement == null) {
                throw new FlowableException("Could not find a CmmnElement for id " + planItemInstance.getPlanItem().getId());
            }
        }
        return caseElement;
    }

    public static boolean isExecutingLifecycleListener(DelegatePlanItemInstance planItemInstance) {
        // Need to check the lifecycle listener, not the model listener (as it could be a lifecycle listener set on the config level)
        return planItemInstance.getCurrentLifecycleListener() != null;
    }

    public static Map<String, List<ExtensionElement>> getExtensionElements(DelegatePlanItemInstance planItemInstance) {
        if (isExecutingLifecycleListener(planItemInstance)) {
            return getListenerExtensionElements(planItemInstance);
        } else {
            return getCmmnElementExtensionElements(planItemInstance);
        }
    }

    public static Map<String, List<ExtensionElement>> getCmmnElementExtensionElements(DelegatePlanItemInstance planItemInstance) {
        return getCmmnElement(planItemInstance).getExtensionElements();
    }

    public static Map<String, List<ExtensionElement>> getListenerExtensionElements(DelegatePlanItemInstance planItemInstance) {
        if (planItemInstance.getCurrentFlowableListener() != null) {
            return planItemInstance.getCurrentFlowableListener().getExtensionElements();
        }
        return Collections.emptyMap();
    }

    public static List<FieldExtension> getFields(DelegatePlanItemInstance planItemInstance) {
        if (isExecutingLifecycleListener(planItemInstance)) {
            return getListenerFields(planItemInstance);
        } else {
            return getCmmnElementFields(planItemInstance);
        }
    }

    public static List<FieldExtension> getCmmnElementFields(DelegatePlanItemInstance planItemInstance) {
        CmmnElement cmmnElement = getCmmnElement(planItemInstance);
        if (cmmnElement instanceof TaskWithFieldExtensions) {
            return ((TaskWithFieldExtensions) cmmnElement).getFieldExtensions();
        }
        if (cmmnElement instanceof PlanItem) {
            PlanItemDefinition planItemDefinition = ((PlanItem) cmmnElement).getPlanItemDefinition();
            if (planItemDefinition instanceof TaskWithFieldExtensions) {
                return ((TaskWithFieldExtensions) planItemDefinition).getFieldExtensions();
            }
        }
        return new ArrayList<>();
    }

    public static List<FieldExtension> getListenerFields(DelegatePlanItemInstance planItemInstance) {
        if  (planItemInstance.getCurrentFlowableListener() != null) {
            return planItemInstance.getCurrentFlowableListener().getFieldExtensions();
        }
        return Collections.emptyList();
    }

    public static FieldExtension getField(DelegatePlanItemInstance planItemInstance, String fieldName) {
        if (isExecutingLifecycleListener(planItemInstance)) {
            return getListenerField(planItemInstance, fieldName);
        } else {
            return getCmmnElementField(planItemInstance, fieldName);
        }
    }

    public static FieldExtension getCmmnElementField(DelegatePlanItemInstance planItemInstance, String fieldName) {
        List<FieldExtension> fieldExtensions = getCmmnElementFields(planItemInstance);
        if (fieldExtensions == null || fieldExtensions.size() == 0) {
            return null;
        }
        for (FieldExtension fieldExtension : fieldExtensions) {
            if (fieldExtension.getFieldName() != null && fieldExtension.getFieldName().equals(fieldName)) {
                return fieldExtension;
            }
        }
        return null;
    }

    public static FieldExtension getListenerField(DelegatePlanItemInstance planItemInstance, String fieldName) {
        List<FieldExtension> fieldExtensions = getListenerFields(planItemInstance);
        if (fieldExtensions == null || fieldExtensions.size() == 0) {
            return null;
        }
        for (FieldExtension fieldExtension : fieldExtensions) {
            if (fieldExtension.getFieldName() != null && fieldExtension.getFieldName().equals(fieldName)) {
                return fieldExtension;
            }
        }
        return null;
    }

    public static Expression createExpressionForField(FieldExtension fieldExtension) {
        if (StringUtils.isNotEmpty(fieldExtension.getExpression())) {
            ExpressionManager expressionManager = CommandContextUtil.getCmmnEngineConfiguration().getExpressionManager();
            return expressionManager.createExpression(fieldExtension.getExpression());
        } else {
            return new FixedValue(fieldExtension.getStringValue());
        }
    }

    public static Expression getFieldExpression(DelegatePlanItemInstance planItemInstance, String fieldName) {
        if (isExecutingLifecycleListener(planItemInstance)) {
            return getListenerFieldExpression(planItemInstance, fieldName);
        } else {
            return getCmmnElementFieldExpression(planItemInstance, fieldName);
        }
    }

    public static Expression getCmmnElementFieldExpression(DelegatePlanItemInstance planItemInstance, String fieldName) {
        FieldExtension fieldExtension = getCmmnElementField(planItemInstance, fieldName);
        if (fieldExtension != null) {
            return createExpressionForField(fieldExtension);
        }
        return null;
    }

    public static Expression getListenerFieldExpression(DelegatePlanItemInstance planItemInstance, String fieldName) {
        FieldExtension fieldExtension = getListenerField(planItemInstance, fieldName);
        if (fieldExtension != null) {
            return createExpressionForField(fieldExtension);
        }
        return null;
    }

}
