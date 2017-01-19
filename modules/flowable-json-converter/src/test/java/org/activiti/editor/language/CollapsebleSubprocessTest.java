package org.activiti.editor.language;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.*;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Pardo David on 6/01/2017.
 */
public class CollapsebleSubprocessTest extends AbstractConverterTest{
	private static final String IN_CSB_START_EVENT = "sid-D8198785-4F74-43A8-A4CD-AF383CEEBE04";
	private static final String IN_CSB_SEQUENCEFLOW_TO_USERTASK = "sid-C633903D-1169-42A4-933D-4D9AAB959792";
	private static final String IN_CSB_USERTASK = "sid-F64640C9-9585-4927-806B-8B0A03DB2B8B";
	private static final String IN_CSB_SEQUENCEFLOW_TO_END = "sid-C1EFE310-3B12-42DA-AEE6-5E442C2FEF19";

	@Test
	public void testItShouldBePossibleToConvertModelerJsonToJava() throws Exception{
		BpmnModel bpmnModel = readJsonFile();
		validateModel(bpmnModel);
	}

	@Test
	public void itShouldBePossibleToConvertJavaToJson() throws Exception{
		BpmnModel bpmnModel = readJsonFile();
		bpmnModel = convertToJsonAndBack(bpmnModel);
		validateModel(bpmnModel);
	}

	private void validateModel(BpmnModel bpmnModel){
		//canvas
		GraphicInfo graphicInfo = bpmnModel.getGraphicInfo("sid-C20D5023-C2B9-4102-AA17-7F16E49E47C1-canvas");
		assertThat(graphicInfo.getX(),is(0.0));
		assertThat(graphicInfo.getY(),is(0.0));
		assertThat(graphicInfo.getWidth(),is(1200.0));
		assertThat(graphicInfo.getHeight(),is(1050.0));

		//validate graphic infos
		FlowElement flowElement = bpmnModel.getFlowElement(IN_CSB_START_EVENT);
		assertThat(flowElement,instanceOf(StartEvent.class));

		GraphicInfo gi = bpmnModel.getGraphicInfo(IN_CSB_START_EVENT);
		assertThat(gi.getX(),is(90.0));
		assertThat(gi.getY(),is(135.0));
		assertThat(gi.getWidth(),is(30.0));
		assertThat(gi.getHeight(),is(30.0));


		flowElement = bpmnModel.getFlowElement(IN_CSB_SEQUENCEFLOW_TO_USERTASK);
		assertThat(flowElement,instanceOf(SequenceFlow.class));
		assertThat(flowElement.getName(),is("to ut"));

		List<GraphicInfo> flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(IN_CSB_SEQUENCEFLOW_TO_USERTASK);
		assertThat(flowLocationGraphicInfo.size(),is(2));

		GraphicInfo start = flowLocationGraphicInfo.get(0);
		assertThat(start.getX(),is(120.0));
		assertThat(start.getY(),is(150.0));

		GraphicInfo end = flowLocationGraphicInfo.get(1);
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
		return "test.collapsed-subprocess.json";
	}
}
