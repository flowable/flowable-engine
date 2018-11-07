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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.GraphicInfo;
import org.junit.Test;

/**
 * Created by Pardo David on 21/02/2017.
 */
public class EventSubprocessSequenceFlowTest extends AbstractConverterTest {
    private static final String EVENT_SUBPROCESS_ID = "sid-3AE5DD30-CE0E-4660-871F-A515E39EECA6";
    private static final String FROM_SE_TO_TASK = "sid-45B32336-D4E3-4576-8377-2D81C0EE02C4";

    private static final double PRECISION = 0.1;

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
        assertEquals(180.5, start.getX(), PRECISION); //75.0+105.5 (parent + interception point)
        assertEquals(314.0, start.getY(), PRECISION); //230.0 + 99.0 - 15.0 (parent + lower right y - bounds y)

        GraphicInfo end = graphicInfo.get(1);
        assertEquals(225.5, end.getX(), PRECISION); //75.0 +150.5
        assertEquals(314.0, end.getY(), PRECISION); //230.0 + 44.0 + 40
    }

    @Override
    protected String getResource() {
        return "test.eventsubprocesssequenceflow.json";
    }


}
