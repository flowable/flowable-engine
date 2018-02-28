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
package org.activiti.engine.impl.bpmn.behavior;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.activiti.engine.impl.pvm.PvmProcessDefinition;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.rules.RulesAgendaFilter;
import org.activiti.engine.impl.rules.RulesHelper;
import org.flowable.engine.delegate.BusinessRuleTaskDelegate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.common.api.delegate.Expression;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

/**
 * activity implementation of the BPMN 2.0 business rule task.
 *
 * @author Tijs Rademakers
 */
public class BusinessRuleTaskActivityBehavior extends TaskActivityBehavior implements BusinessRuleTaskDelegate {

    private static final long serialVersionUID = 1L;

    protected Set<Expression> variablesInputExpressions = new HashSet<>();
    protected Set<Expression> rulesExpressions = new HashSet<>();
    protected boolean exclude;
    protected String resultVariable;

    public BusinessRuleTaskActivityBehavior() {
    }

    @Override
    public void execute(DelegateExecution execution) {
        ActivityExecution activityExecution = (ActivityExecution) execution;
        PvmProcessDefinition processDefinition = activityExecution.getActivity().getProcessDefinition();
        String deploymentId = processDefinition.getDeploymentId();

        KieBase knowledgeBase = RulesHelper.findKnowledgeBaseByDeploymentId(deploymentId);
        KieSession ksession = knowledgeBase.newKieSession();

        if (variablesInputExpressions != null) {
            Iterator<Expression> itVariable = variablesInputExpressions.iterator();
            while (itVariable.hasNext()) {
                Expression variable = itVariable.next();
                ksession.insert(variable.getValue(execution));
            }
        }

        if (!rulesExpressions.isEmpty()) {
            RulesAgendaFilter filter = new RulesAgendaFilter();
            Iterator<Expression> itRuleNames = rulesExpressions.iterator();
            while (itRuleNames.hasNext()) {
                Expression ruleName = itRuleNames.next();
                filter.addSuffic(ruleName.getValue(execution).toString());
            }
            filter.setAccept(!exclude);
            ksession.fireAllRules(filter);

        } else {
            ksession.fireAllRules();
        }

        Collection<? extends Object> ruleOutputObjects = ksession.getObjects();
        if (ruleOutputObjects != null && !ruleOutputObjects.isEmpty()) {
            Collection<Object> outputVariables = new ArrayList<>();
            outputVariables.addAll(ruleOutputObjects);
            execution.setVariable(resultVariable, outputVariables);
        }
        ksession.dispose();
        leave(activityExecution);
    }

    @Override
    public void addRuleVariableInputIdExpression(Expression inputId) {
        this.variablesInputExpressions.add(inputId);
    }

    @Override
    public void addRuleIdExpression(Expression inputId) {
        this.rulesExpressions.add(inputId);
    }

    @Override
    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    @Override
    public void setResultVariable(String resultVariableName) {
        this.resultVariable = resultVariableName;
    }

}
