package org.flowable.dmn.engine.test.runtime;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.AbstractFlowableDmnTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * This class tests fallbacks in {@link org.flowable.dmn.engine.impl.cmd.AbstractExecuteDecisionCmd}
 */
public class DecisionTableExecutionFallBackTest extends AbstractFlowableDmnTest {

    public static final String TEST_TENANT_ID = "testTenantId";
    public static final String TEST_PARENT_DEPLOYMENT_ID = "testParentDeploymentId";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected DmnDeployment deployment;

    @Before
    public void createDeployment() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        deployment = dmnEngine.getDmnRepositoryService().createDeployment().
                addClasspathResource("org/flowable/dmn/engine/test/runtime/StandaloneRuntimeTest.ruleUsageExample.dmn").
                tenantId(TEST_TENANT_ID).
                parentDeploymentId(TEST_PARENT_DEPLOYMENT_ID).
                deploy();
    }

    @After
    public void cleanUpDeployment() {
        flowableDmnRule.getDmnEngine().getDmnRepositoryService().deleteDeployment(deployment.getId());
    }

    @Test
    public void decisionKeyDeploymentIdTenantId() {
        Map<String, Object> result = executeDecision(TEST_TENANT_ID, TEST_PARENT_DEPLOYMENT_ID);
        Assert.assertEquals("result2", result.get("outputVariable1"));
    }


    @Test
    public void fallBackDecisionKeyDeploymentIdTenantIdWrongDeploymentId() {
        Map<String, Object> result = executeDecision(TEST_TENANT_ID, "WRONG_PARENT_DEPLOYMENT_ID");

        Assert.assertEquals("result2", result.get("outputVariable1"));
    }

    @Test
    public void decisionKeyDeploymentIdTenantIdWrongTenantIdThrowsException() {
        expectedException.expect(FlowableObjectNotFoundException.class);
        expectedException.expectMessage("No decision found for key: decision1, parent deployment id testParentDeploymentId and tenant id: WRONG_TENANT_ID.");

        executeDecision("WRONG_TENANT_ID", TEST_PARENT_DEPLOYMENT_ID);
    }

    @Test
    public void decisionKeyTenantIdWrongTenantIdThrowsException() {
        expectedException.expect(FlowableObjectNotFoundException.class);
        expectedException.expectMessage("Decision table for key [decision1] and tenantId [WRONG_TENANT_ID] was not found");

        executeDecision("WRONG_TENANT_ID", null);
    }

    @Test
    public void decisionKeyDeploymentId() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDeployment localDeployment = dmnEngine.getDmnRepositoryService().createDeployment().
                addClasspathResource("org/flowable/dmn/engine/test/runtime/StandaloneRuntimeTest.ruleUsageExample.dmn").
                tenantId(null).
                parentDeploymentId(TEST_PARENT_DEPLOYMENT_ID).
                deploy();
        try {
            Map<String, Object> result = executeDecision(null, TEST_PARENT_DEPLOYMENT_ID);

            Assert.assertEquals("result2", result.get("outputVariable1"));
        } finally {
            dmnEngine.getDmnRepositoryService().deleteDeployment(localDeployment.getId());
        }
    }

    @Test
    public void decisionKeyTenantId() {
        Map<String, Object> result = executeDecision(TEST_TENANT_ID, null);
        Assert.assertEquals("result2", result.get("outputVariable1"));
    }


    @Test
    public void fallBackDecisionKeyDeploymentId_wrongDeploymentId() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDeployment localDeployment = dmnEngine.getDmnRepositoryService().createDeployment().
                addClasspathResource("org/flowable/dmn/engine/test/runtime/StandaloneRuntimeTest.ruleUsageExample.dmn").
                tenantId(null).
                parentDeploymentId(TEST_PARENT_DEPLOYMENT_ID).
                deploy();
        try {
            Map<String, Object> result = executeDecision(null, "WRONG_PARENT_DEPLOYMENT_ID");

            Assert.assertEquals("result2", result.get("outputVariable1"));
        } finally {
            dmnEngine.getDmnRepositoryService().deleteDeployment(localDeployment.getId());
        }
    }

    @Test
    public void fallBackDecisionKeyDeploymentId_fallbackToDefaultTenant() {
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDeployment localDeployment = dmnEngine.getDmnRepositoryService().createDeployment().
                addClasspathResource("org/flowable/dmn/engine/test/runtime/StandaloneRuntimeTest.ruleUsageExample.dmn").
                tenantId(null).
                parentDeploymentId(TEST_PARENT_DEPLOYMENT_ID).
                deploy();
        try {
            Map<String, Object> inputVariables = new HashMap<>();
            inputVariables.put("inputVariable1", 2);
            inputVariables.put("inputVariable2", "test2");

            Map<String, Object> result = flowableDmnRule.getDmnEngine().getDmnRuleService().createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .tenantId("flowable")
                .parentDeploymentId(localDeployment.getId())
                .variables(inputVariables)
                .fallbackToDefaultTenant()
                .executeWithSingleResult();

            Assert.assertEquals("result2", result.get("outputVariable1"));
        } finally {
            dmnEngine.getDmnRepositoryService().deleteDeployment(localDeployment.getId());
        }
    }

    protected Map<String, Object> executeDecision(String tenantId, String parentDeploymentId) {
        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);
        inputVariables.put("inputVariable2", "test2");

        return flowableDmnRule.getDmnEngine().getDmnRuleService().createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .tenantId(tenantId)
                .parentDeploymentId(parentDeploymentId)
                .variables(inputVariables)
                .executeWithSingleResult();
    }

}
