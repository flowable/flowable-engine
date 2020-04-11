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
package org.flowable.editor.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;

/**
 * Created by Pardo David on 6/01/2017.
 */
public class CollapsebleSubprocessTest extends AbstractConverterTest {

    private static final String START_EVENT = "sid-89C70A03-C51B-4185-AB85-B8476E7A4F0C";
    private static final String SEQUENCEFLOW_TO_COLLAPSEDSUBPROCESS = "sid-B80498C9-A45C-4D58-B4AA-5393A409ACAA";
    private static final String COLLAPSEDSUBPROCESS = "sid-C20D5023-C2B9-4102-AA17-7F16E49E47C1";
    private static final String IN_CSB_START_EVENT = "sid-D8198785-4F74-43A8-A4CD-AF383CEEBE04";
    private static final String IN_CSB_SEQUENCEFLOW_TO_USERTASK = "sid-C633903D-1169-42A4-933D-4D9AAB959792";
    private static final String IN_CSB_USERTASK = "sid-F64640C9-9585-4927-806B-8B0A03DB2B8B";
    private static final String IN_CSB_SEQUENCEFLOW_TO_END = "sid-C1EFE310-3B12-42DA-AEE6-5E442C2FEF19";

    private static final double PRECISION = 0.1d;

    @Test
    public void testItShouldBePossibleToConvertModelerJsonToJava() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Test
    public void itShouldBePossibleToConvertJavaToJson() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    private void validateModel(BpmnModel bpmnModel) {
        //temp vars
        GraphicInfo gi = null;
        GraphicInfo start = null;
        GraphicInfo end = null;
        List<GraphicInfo> flowLocationGraphicInfo = null;

        //validate parent
        gi = bpmnModel.getGraphicInfo(START_EVENT);
        assertThat(gi.getX()).isEqualTo(73.0);
        assertThat(gi.getY()).isEqualTo(96.0);
        assertThat(gi.getWidth()).isEqualTo(30.0);
        assertThat(gi.getHeight()).isEqualTo(30.0);

        flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(SEQUENCEFLOW_TO_COLLAPSEDSUBPROCESS);
        assertThat(flowLocationGraphicInfo).hasSize(2);

        gi = bpmnModel.getGraphicInfo(COLLAPSEDSUBPROCESS);
        assertThat(gi.getExpanded()).isFalse();

        //the intersection points are not full values so its a strange double here...
        start = flowLocationGraphicInfo.get(0);
        assertThat(start.getX()).isCloseTo(102.99814034216989, offset(PRECISION));
        assertThat(start.getY()).isCloseTo(111.23619118649086, offset(PRECISION));

        end = flowLocationGraphicInfo.get(1);
        assertThat(end.getX()).isCloseTo(165.0, offset(PRECISION));
        assertThat(end.getY()).isCloseTo(112.21259842519686, offset(PRECISION));

        //validate graphic infos
        FlowElement flowElement = bpmnModel.getFlowElement(IN_CSB_START_EVENT);
        assertThat(flowElement).isInstanceOf(StartEvent.class);

        gi = bpmnModel.getGraphicInfo(IN_CSB_START_EVENT);
        assertThat(gi.getX()).isEqualTo(90.0);
        assertThat(gi.getY()).isEqualTo(135.0);
        assertThat(gi.getWidth()).isEqualTo(30.0);
        assertThat(gi.getHeight()).isEqualTo(30.0);

        flowElement = bpmnModel.getFlowElement(IN_CSB_SEQUENCEFLOW_TO_USERTASK);
        assertThat(flowElement).isInstanceOf(SequenceFlow.class);
        assertThat(flowElement.getName()).isEqualTo("to ut");

        flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(IN_CSB_SEQUENCEFLOW_TO_USERTASK);
        assertThat(flowLocationGraphicInfo).hasSize(2);

        start = flowLocationGraphicInfo.get(0);
        assertThat(start.getX()).isCloseTo(120.0, offset(PRECISION));
        assertThat(start.getY()).isCloseTo(150.0, offset(PRECISION));

        end = flowLocationGraphicInfo.get(1);
        assertThat(end.getX()).isCloseTo(232.0, offset(PRECISION));
        assertThat(end.getY()).isEqualTo(150.0);

        flowElement = bpmnModel.getFlowElement(IN_CSB_USERTASK);
        assertThat(flowElement).isInstanceOf(UserTask.class);
        assertThat(flowElement.getName()).isEqualTo("User task 1");

        gi = bpmnModel.getGraphicInfo(IN_CSB_USERTASK);
        assertThat(gi.getX()).isEqualTo(232.0);
        assertThat(gi.getY()).isEqualTo(110.0);
        assertThat(gi.getWidth()).isEqualTo(100.0);
        assertThat(gi.getHeight()).isEqualTo(80.0);

        flowElement = bpmnModel.getFlowElement(IN_CSB_SEQUENCEFLOW_TO_END);
        assertThat(flowElement).isInstanceOf(SequenceFlow.class);
        assertThat(flowElement.getName()).isEqualTo("to end");

        flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(IN_CSB_SEQUENCEFLOW_TO_END);
        assertThat(flowLocationGraphicInfo).hasSize(2);

        start = flowLocationGraphicInfo.get(0);
        assertThat(start.getX()).isCloseTo(332.0, offset(PRECISION));
        assertThat(start.getY()).isCloseTo(150.0, offset(PRECISION));

        end = flowLocationGraphicInfo.get(1);
        assertThat(end.getX()).isEqualTo(435.0);
        assertThat(end.getY()).isEqualTo(150.0);
    }

    @Override
    protected String getResource() {
        return "test.collapsed-subprocess.json";
    }
}
