package org.flowable.engine.test.bpmn.event.variable;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.util.CommandContextUtil;

public class SetVariableExpression {

    public void setVariable(String variable, Object value, DelegateExecution execution) {
        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();
        Expression expression = expressionManager.createExpression("${" + variable + "}");
        expression.setValue(value, execution);
    }
}
