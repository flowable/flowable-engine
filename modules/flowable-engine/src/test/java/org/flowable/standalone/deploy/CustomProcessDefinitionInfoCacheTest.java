package org.flowable.standalone.deploy;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class CustomProcessDefinitionInfoCacheTest extends ResourceFlowableTestCase {
    public CustomProcessDefinitionInfoCacheTest() {
        super("org/flowable/standalone/deploy/custom.procedefinitioninfo.cache.test.flowable.cfg.xml");
    }

    @Test
    public void testCustomProcessDefinitionInfoCache() throws IOException {
        final CustomProcessDefinitionInfoCache processDefinitionInfoCache =
                (CustomProcessDefinitionInfoCache)processEngineConfiguration.getProcessDefinitionInfoCache();
        assertEquals(0, processDefinitionInfoCache.size());
        String processDefinitionTemplate = DeploymentCacheTestUtil.readTemplateFile("/org/flowable/standalone/deploy/processDefinitionInfoCacheTest.bpmn20.xml");
        repositoryService.createDeployment().addString("Process 1.bpmn20.xml", processDefinitionTemplate).deploy();
        assertTrue(processDefinitionInfoCache.size() > 0);
        // Cleanup
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
}
