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

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.UserEventListener;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dennis Federico
 */
public class UserEventListenerCmmnXmlConverterTest extends AbstractConverterTest {

    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/user-event-listener.cmmn";

    @Test
    public void convertXMLToModel() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        validateModel(cmmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);
        validateModel(parsedModel);
    }

    public void validateModel(CmmnModel cmmnModel) {
        assertNotNull(cmmnModel);

        List<HumanTask> humanTasks = cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionsOfType(HumanTask.class, true);
        assertEquals(2, humanTasks.size());

        List<UserEventListener> userEventListeners = cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionsOfType(UserEventListener.class, true);
        assertEquals(1, userEventListeners.size());

        UserEventListener userEventListener = userEventListeners.get(0);
        assertEquals("myUserEventListener", userEventListener.getName());
        assertEquals("userActionListener",userEventListener.getId());
        assertEquals("UserEventListener documentation",userEventListener.getDocumentation());
        assertArrayEquals(new String[]{"aRole"}, userEventListener.getAuthorizedRoleRefs());
    }
}
