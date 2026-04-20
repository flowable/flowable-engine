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
package org.flowable.cmmn.engine.impl.listener;

import java.util.List;

import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.DelegateExpressionUtil;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;

/**
 * @author martin.grofcik
 */
public class DelegateExpressionCaseLifecycleListener implements CaseInstanceLifecycleListener {

    protected String sourceState;
    protected String targetState;
    protected Expression expression;
    protected List<FieldExtension> fieldExtensions;

    public DelegateExpressionCaseLifecycleListener(String sourceState, String targetState, Expression expression,
        List<FieldExtension> fieldExtensions) {
        this.sourceState = sourceState;
        this.targetState = targetState;
        this.expression = expression;
        this.fieldExtensions = fieldExtensions;
    }

    @Override
    public String getSourceState() {
        return sourceState;
    }

    @Override
    public String getTargetState() {
        return targetState;
    }

    @Override
    public void stateChanged(CaseInstance caseInstance, String oldState, String newState) {
        CaseInstanceEntity caseInstanceEntity = (CaseInstanceEntity) caseInstance;
        Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, caseInstanceEntity, fieldExtensions);

        if (delegate instanceof CaseInstanceLifecycleListener listener) {
            listener.stateChanged(caseInstanceEntity, oldState, newState);
        } else {
            throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did not resolve to an implementation of " + CaseInstanceLifecycleListener.class);
        }
    }

    /**
     * returns the expression text for this CaseInstance lifecycle listener.
     */
    public String getExpressionText() {
        return expression.getExpressionText();
    }

}
