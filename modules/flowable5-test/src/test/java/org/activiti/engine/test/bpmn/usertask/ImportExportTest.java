package org.activiti.engine.test.bpmn.usertask;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.flowable.common.engine.impl.util.io.StreamSource;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.runtime.Execution;

/**
 * Created by p3700487 on 23/02/15.
 */
public class ImportExportTest extends ResourceFlowableTestCase {

    public ImportExportTest() {
        super("org/activiti/standalone/parsing/encoding.flowable.cfg.xml");
    }

    public void testConvertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        bpmnModel = exportAndReadXMLFile(bpmnModel);

        byte[] xml = new BpmnXMLConverter().convertToXML(bpmnModel);

        processEngine.getRepositoryService().createDeployment().name("test1").addString("test1.bpmn20.xml", new String(xml))
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy();

        String processInstanceKey = runtimeService.startProcessInstanceByKey("process").getId();
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceKey).messageEventSubscriptionName("InterruptMessage").singleResult();

        assertNotNull(execution);
    }

    protected void tearDown() throws Exception {
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
        super.tearDown();
    }

    protected String getResource() {
        return "org/activiti/engine/test/bpmn/usertask/ImportExportTest.testImportExport.bpmn20.xml";
    }

    protected BpmnModel readXMLFile() throws Exception {
        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream(getResource());
        StreamSource xmlSource = new InputStreamSource(xmlStream);
        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xmlSource, false, false, processEngineConfiguration.getXmlEncoding());
        return bpmnModel;
    }

    protected BpmnModel exportAndReadXMLFile(BpmnModel bpmnModel) throws Exception {
        byte[] xml = new BpmnXMLConverter().convertToXML(bpmnModel, processEngineConfiguration.getXmlEncoding());
        StreamSource xmlSource = new InputStreamSource(new ByteArrayInputStream(xml));
        BpmnModel parsedModel = new BpmnXMLConverter().convertToBpmnModel(xmlSource, false, false, processEngineConfiguration.getXmlEncoding());
        return parsedModel;
    }

}
