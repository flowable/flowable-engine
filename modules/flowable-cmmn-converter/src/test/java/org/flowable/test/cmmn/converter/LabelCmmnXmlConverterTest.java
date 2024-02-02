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
package org.flowable.test.cmmn.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

public class LabelCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/labelCase.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel.getLabelLocationMap().size()).isEqualTo(2);

        // Test start event label
        GraphicInfo labelGraphicInfo = cmmnModel.getLabelGraphicInfo("planItemcmmnEventListener_16");
        assertThat(labelGraphicInfo).isNotNull();
        assertThat(labelGraphicInfo.getX()).isEqualTo(350.0);
        assertThat(labelGraphicInfo.getY()).isEqualTo(250.0);
        assertThat(labelGraphicInfo.getWidth()).isEqualTo(85.0);
        assertThat(labelGraphicInfo.getHeight()).isEqualTo(18.0);
        assertThat(labelGraphicInfo.getRotation()).isEqualTo(30.0);

        // Test association label
        GraphicInfo labelGraphicInfo2 = cmmnModel.getLabelGraphicInfo("CMMNEdge_cmmnConnector_19");
        assertThat(labelGraphicInfo2).isNotNull();
        assertThat(labelGraphicInfo2.getX()).isEqualTo(500.0);
        assertThat(labelGraphicInfo2.getY()).isEqualTo(250.0);
        assertThat(labelGraphicInfo2.getWidth()).isEqualTo(85.0);
        assertThat(labelGraphicInfo2.getHeight()).isEqualTo(18.0);
        assertThat(labelGraphicInfo2.getRotation()).isEqualTo(45.0);
    }

}
