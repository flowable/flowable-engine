package org.flowable.engine.test.bpmn.scripttask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Calin Cerchez
 */
public class ScriptTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testNormalRun() {
        runtimeService.startProcessInstanceByKey("scriptTaskProcess");

        HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();

        assertThat(historicVariableInstance).isNotNull();
        assertThat(historicVariableInstance.getVariableName()).isEqualTo("persistentResult");
        assertThat(historicVariableInstance.getValue()).isEqualTo("success");
    }

    @Test
    @Deployment
    public void testSkipExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables.put("skipExpression", true);
        runtimeService.startProcessInstanceByKey("scriptTaskProcess", variables);

        HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery().variableName("persistentResult")
                .singleResult();

        assertThat(historicVariableInstance).isNull();
    }
}
