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
package org.flowable.cmmn.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.Stage;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class SentryConverterTest extends AbstractConverterTest {

    protected static final String SENTRY_NODE_ID = "sid-FB0DD4A6-0EC0-46F9-9A22-72A76CCECA9D";

    @Test
    public void dockerInfoShouldRemainIntact() throws Exception {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(getResource());
        JsonNode model = new ObjectMapper().readTree(stream);
        CmmnModel cmmnModel = new CmmnJsonConverter().convertToCmmnModel(model);
        model = new CmmnJsonConverter().convertToJson(cmmnModel);
        validate(model);
    }

    @Override
    protected String getResource() {
        return "test.sentryIfpartmodel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        Stage planModelStage = caseModel.getPlanModel();
        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");

        List<Sentry> sentries = planModelStage.getSentries();
        assertThat(sentries).hasSize(1);

        Sentry sentry = sentries.get(0);

        Criterion criterion = planItem.getEntryCriteria().get(0);
        assertThat(criterion.getSentryRef()).isEqualTo(sentry.getId());

        SentryIfPart ifPart = sentry.getSentryIfPart();
        assertThat(ifPart).isNotNull();
        assertThat(ifPart.getCondition()).isEqualTo("${true}");

        assertThat(sentry.getName()).isEqualTo("sentry name");
        assertThat(sentry.getDocumentation()).isEqualTo("sentry doc");

        GraphicInfo sentryGraphicInfo = model.getGraphicInfo(criterion.getId());
        assertThat(sentryGraphicInfo.getX()).isEqualTo(400.73441809224767);
        assertThat(sentryGraphicInfo.getY()).isEqualTo(110.88085470555188);
    }

    protected void validate(JsonNode model) {
        ArrayNode node = (ArrayNode) model.path("childShapes").get(0).path("childShapes");
        JsonNode sentryNode = null;

        for (JsonNode shape : node) {
            String resourceId = shape.path("resourceId").asText();
            if (SENTRY_NODE_ID.equals(resourceId)) {
                sentryNode = shape;
            }
        }

        //validate docker nodes
        Double x = sentryNode.path("dockers").get(0).path("x").asDouble();
        Double y = sentryNode.path("dockers").get(0).path("y").asDouble();

        //the modeler does not store a mathematical correct docker point.
        assertThat(x).isEqualTo(-1.0);
        assertThat(y).isEqualTo(34.0);
    }

}
