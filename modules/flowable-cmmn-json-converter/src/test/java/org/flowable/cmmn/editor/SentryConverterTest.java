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

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.Stage;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class SentryConverterTest extends AbstractConverterTest {

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
        assertEquals(1, sentries.size());

        Sentry sentry = sentries.get(0);

        Criterion criterion = planItem.getEntryCriteria().get(0);
        assertEquals(sentry.getId(), criterion.getSentryRef());

        SentryIfPart ifPart = sentry.getSentryIfPart();
        assertNotNull(ifPart);
        assertThat(ifPart.getCondition(), is("${true}"));

        assertThat(sentry.getName(), is("sentry name"));
        assertThat(sentry.getDocumentation(), is("sentry doc"));

        GraphicInfo sentryGraphicInfo = model.getGraphicInfo(criterion.getId());
        assertThat(sentryGraphicInfo.getX(), is(400.73441809224767) );
        assertThat(sentryGraphicInfo.getY(), is(110.88085470555188) );
    }
}
