package org.activiti.editor.language;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EventSubProcess;
import org.activiti.bpmn.model.GraphicInfo;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Pardo David on 21/02/2017.
 */
public class EventSubprocessSequenceFlowTest extends AbstractConverterTest {
	private final static String EVENT_SUBPROCESS_ID = "sid-3AE5DD30-CE0E-4660-871F-A515E39EECA6";
	private final static String FROM_SE_TO_TASK = "sid-45B32336-D4E3-4576-8377-2D81C0EE02C4";

	@Test
	public void oneWay() throws Exception{
		BpmnModel bpmnModel = readJsonFile();
		validate(bpmnModel);
	}

	@Test
	public void twoWay() throws Exception{
		BpmnModel bpmnModel = readJsonFile();
		bpmnModel = convertToJsonAndBack(bpmnModel);
		validate(bpmnModel);
	}

	private void validate(BpmnModel model){
		EventSubProcess eventSubProcess = (EventSubProcess) model.getFlowElement(EVENT_SUBPROCESS_ID);

		//assert that there where 5 sequenceflows registered.
		assertThat(model.getFlowLocationMap().size(),is(5));

		List<GraphicInfo> graphicInfo = model.getFlowLocationGraphicInfo(FROM_SE_TO_TASK);
		GraphicInfo start = graphicInfo.get(0);
		assertThat(start.getX(),is(180.5)); //75.0+105.5 (parent + interception point)
		assertThat(start.getY(),is(314.0)); //230.0 + 99.0 - 15.0 (parent + lower right y - bounds y)

		GraphicInfo end = graphicInfo.get(1);
		assertThat(end.getX(),is(225.5)); //75.0 +150.5
		assertThat(end.getY(),is(314.0)); //230.0 + 44.0 + 40
	}

	@Override
	protected String getResource() {
		return "test.eventsubprocesssequenceflow.json";
	}


}
