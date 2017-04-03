package org.flowable.standalone.parsing;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.common.impl.util.io.InputStreamSource;
import org.flowable.engine.common.impl.util.io.StreamSource;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.repository.Deployment;

public class ChineseConverterTest extends ResourceFlowableTestCase {

    public ChineseConverterTest() {
        super("org/flowable/standalone/parsing/encoding.flowable.cfg.xml");
    }

    public void testConvertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        bpmnModel = exportAndReadXMLFile(bpmnModel);
        deployProcess(bpmnModel);
    }

    protected String getResource() {
        return "org/flowable/standalone/parsing/chinese.bpmn";
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

    protected void deployProcess(BpmnModel bpmnModel) {
        byte[] xml = new BpmnXMLConverter().convertToXML(bpmnModel);
        try {
            Deployment deployment = processEngine.getRepositoryService().createDeployment().name("test").addString("test.bpmn20.xml", new String(xml)).deploy();
            processEngine.getRepositoryService().deleteDeployment(deployment.getId());
        } finally {
            processEngine.close();
        }
    }
}
