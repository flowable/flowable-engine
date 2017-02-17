package org.activiti.editor.language.xml;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.junit.Test;

/**
 * Created by Pardo David on 16/01/2017.
 */
public class CollapsedSubProcessConverterTest extends AbstractConverterTest {
  
	private static final String START_EVENT = "sid-89C70A03-C51B-4185-AB85-B8476E7A4F0C";
	private static final String SEQUENCEFLOW_TO_COLLAPSEDSUBPROCESS = "sid-B80498C9-A45C-4D58-B4AA-5393A409ACAA";
	private static final String COLLAPSEDSUBPROCESS = "sid-C20D5023-C2B9-4102-AA17-7F16E49E47C1";
	private static final String IN_CSB_START_EVENT = "sid-D8198785-4F74-43A8-A4CD-AF383CEEBE04";
	private static final String IN_CSB_SEQUENCEFLOW_TO_USERTASK = "sid-C633903D-1169-42A4-933D-4D9AAB959792";
	private static final String IN_CSB_USERTASK = "sid-F64640C9-9585-4927-806B-8B0A03DB2B8B";
	private static final String IN_CSB_SEQUENCEFLOW_TO_END = "sid-C1EFE310-3B12-42DA-AEE6-5E442C2FEF19";

	@Test
	public void convertFromXmlToJava() throws Exception{
		BpmnModel bpmnModel = readXMLFile();
		validateModel(bpmnModel);
	}

	@Test
	public void convertFromJavaToXml() throws Exception{
		BpmnModel bpmnModel = readXMLFile();
		validateModel(bpmnModel);
		bpmnModel = exportAndReadXMLFile(bpmnModel);
		validateModel(bpmnModel);
	}

	private void validateModel(BpmnModel bpmnModel){
		//temp vars
		GraphicInfo gi = null;
		GraphicInfo start = null;
		GraphicInfo end = null;
		List<GraphicInfo> flowLocationGraphicInfo = null;

		//validate parent
		gi = bpmnModel.getGraphicInfo(START_EVENT);
		assertThat(gi.getX(),is(73.0));
		assertThat(gi.getY(),is(96.0));
		assertThat(gi.getWidth(),is(30.0));
		assertThat(gi.getHeight(),is(30.0));
		assertThat(gi.getExpanded(),is(nullValue()));

		flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(SEQUENCEFLOW_TO_COLLAPSEDSUBPROCESS);
		assertThat(flowLocationGraphicInfo.size(),is(2));

		gi = bpmnModel.getGraphicInfo(COLLAPSEDSUBPROCESS);
		assertThat(gi.getExpanded(),is(false));

		//intersection points traversed from xml are full points it seems...
		start = flowLocationGraphicInfo.get(0);
		assertThat(start.getX(),is(102.0));
		assertThat(start.getY(),is(111.0));

		end = flowLocationGraphicInfo.get(1);
		assertThat(end.getX(),is(165.0));
		assertThat(end.getY(),is(112.0));

		//validate graphic infos
		FlowElement flowElement = bpmnModel.getFlowElement(IN_CSB_START_EVENT);
		assertThat(flowElement,instanceOf(StartEvent.class));

		gi = bpmnModel.getGraphicInfo(IN_CSB_START_EVENT);
		assertThat(gi.getX(),is(90.0));
		assertThat(gi.getY(),is(135.0));
		assertThat(gi.getWidth(),is(30.0));
		assertThat(gi.getHeight(),is(30.0));


		flowElement = bpmnModel.getFlowElement(IN_CSB_SEQUENCEFLOW_TO_USERTASK);
		assertThat(flowElement,instanceOf(SequenceFlow.class));
		assertThat(flowElement.getName(),is("to ut"));

		flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(IN_CSB_SEQUENCEFLOW_TO_USERTASK);
		assertThat(flowLocationGraphicInfo.size(),is(2));

		start = flowLocationGraphicInfo.get(0);
		assertThat(start.getX(),is(120.0));
		assertThat(start.getY(),is(150.0));

		end = flowLocationGraphicInfo.get(1);
		assertThat(end.getX(),is(232.0));
		assertThat(end.getY(),is(150.0));

		flowElement = bpmnModel.getFlowElement(IN_CSB_USERTASK);
		assertThat(flowElement,instanceOf(UserTask.class));
		assertThat(flowElement.getName(),is("User task 1"));

		gi = bpmnModel.getGraphicInfo(IN_CSB_USERTASK);
		assertThat(gi.getX(),is(232.0));
		assertThat(gi.getY(),is(110.0));
		assertThat(gi.getWidth(),is(100.0));
		assertThat(gi.getHeight(),is(80.0));

		flowElement = bpmnModel.getFlowElement(IN_CSB_SEQUENCEFLOW_TO_END);
		assertThat(flowElement,instanceOf(SequenceFlow.class));
		assertThat(flowElement.getName(),is("to end"));

		flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(IN_CSB_SEQUENCEFLOW_TO_END);
		assertThat(flowLocationGraphicInfo.size() , is(2));

		start = flowLocationGraphicInfo.get(0);
		assertThat(start.getX(),is(332.0));
		assertThat(start.getY(),is(150.0));

		end = flowLocationGraphicInfo.get(1);
		assertThat(end.getX(),is(435.0));
		assertThat(end.getY(),is(150.0));
	}

	@Override
	protected String getResource() {
		return "collapsed-subprocess.bpmn20.xml";
	}
}
