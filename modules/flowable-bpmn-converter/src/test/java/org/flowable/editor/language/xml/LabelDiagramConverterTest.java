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
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class LabelDiagramConverterTest {

    @BpmnXmlConverterTest("labelProcess.bpmn")
    void validateModel(BpmnModel bpmnModel) {
        assertThat(bpmnModel.getLabelLocationMap().size()).isEqualTo(2);

        // Test start event label
        GraphicInfo labelGraphicInfo = bpmnModel.getLabelGraphicInfo("startnoneevent1");
        assertThat(labelGraphicInfo).isNotNull();
        assertThat(labelGraphicInfo.getX()).isEqualTo(419.0);
        assertThat(labelGraphicInfo.getY()).isEqualTo(250.0);
        assertThat(labelGraphicInfo.getWidth()).isEqualTo(41.0);
        assertThat(labelGraphicInfo.getHeight()).isEqualTo(18.0);
        assertThat(labelGraphicInfo.getRotation()).isEqualTo(0);

        // Test sequence flow label
        GraphicInfo labelGraphicInfo2 = bpmnModel.getLabelGraphicInfo("bpmnSequenceFlow_2");
        assertThat(labelGraphicInfo2).isNotNull();
        assertThat(labelGraphicInfo2.getX()).isEqualTo(845.0);
        assertThat(labelGraphicInfo2.getY()).isEqualTo(181.0);
        assertThat(labelGraphicInfo2.getWidth()).isEqualTo(85.0);
        assertThat(labelGraphicInfo2.getHeight()).isEqualTo(18.0);
        assertThat(labelGraphicInfo2.getRotation()).isEqualTo(90.0);

    }
}
