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
import static org.flowable.test.cmmn.converter.util.XmlTestUtils.exportAndReadXMLFile;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Stage;
import org.junit.jupiter.api.Test;

class ExporterAndVersionTest {

    @Test
    public void convertModelToXML() {
        CmmnModel cmmnModel = new CmmnModel();
        Case caze = new Case();
        Stage planModel = new Stage();
        planModel.setPlanModel(true);
        planModel.setId("planModel");
        caze.setPlanModel(planModel);
        caze.setId("caseId");
        cmmnModel.addCase(caze);
        cmmnModel.setExporter("Flowable");
        cmmnModel.setExporterVersion("latest");
        
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);

        assertThat(parsedModel.getExporter()).isEqualTo("Flowable");
        assertThat(parsedModel.getExporterVersion()).isEqualTo("latest");
    }
}
