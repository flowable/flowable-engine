package org.flowable.dmn.engine.test.runtime;

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

import java.util.HashMap;
import java.util.Map;

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
        // Arrange
        // Act
        Map<String, Object> result = executeDecision(TEST_TENANT_ID, TEST_PARENT_DEPLOYMENT_ID);

        // Assert
        Assert.assertEquals("result2", result.get("outputVariable1"));
    }


    @Test
    public void fallBackDecisionKeyDeploymentIdTenantId_wrongDeploymentId() {
        // Act
        Map<String, Object> result = executeDecision(TEST_TENANT_ID, "WRONG_PARENT_DEPLOYMENT_ID");

        // Assert
        Assert.assertEquals("result2", result.get("outputVariable1"));
    }

    @Test
    public void decisionKeyDeploymentIdTenantId_wrongTenantId_throwsException() {
        // Arrange
        expectedException.expect(FlowableObjectNotFoundException.class);
        expectedException.expectMessage("No decision found for key: decision1, parent deployment id testParentDeploymentId and tenant id: WRONG_TENANT_ID.");

        // Act
        executeDecision("WRONG_TENANT_ID", TEST_PARENT_DEPLOYMENT_ID);
    }

    @Test
    public void decisionKeyTenantId_wrongTenantId_throwsException() {
        // Arrange
        expectedException.expect(FlowableObjectNotFoundException.class);
        expectedException.expectMessage("no decisions deployed with key 'decision1' for tenant identifier 'WRONG_TENANT_ID'");

        // Act
        executeDecision("WRONG_TENANT_ID", null);
    }

    @Test
    public void decisionKeyDeploymentId() {
        // Arrange
        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDeployment localDeployment = dmnEngine.getDmnRepositoryService().createDeployment().
                addClasspathResource("org/flowable/dmn/engine/test/runtime/StandaloneRuntimeTest.ruleUsageExample.dmn").
                tenantId(null).
                parentDeploymentId(TEST_PARENT_DEPLOYMENT_ID).
                deploy();
        try {
            // Act
            Map<String, Object> result = executeDecision(null, TEST_PARENT_DEPLOYMENT_ID);

            // Assert
            Assert.assertEquals("result2", result.get("outputVariable1"));
        } finally {
            dmnEngine.getDmnRepositoryService().deleteDeployment(localDeployment.getId());
        }
    }

    @Test
    public void decisionKeyTenantId() {
        // Arrange
        // Act
        Map<String, Object> result = executeDecision(TEST_TENANT_ID, null);

        // Assert
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
            // Act
            Map<String, Object> result = executeDecision(null, "WRONG_PARENT_DEPLOYMENT_ID");

            // Assert
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
