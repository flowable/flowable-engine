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
package org.flowable.cmmn.converter.export;

import org.flowable.cmmn.model.UserEventListener;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author Dennis Federico
 */
public class UserEventListenerExport extends AbstractPlanItemDefinitionExport<UserEventListener> {

    @Override
    protected Class<? extends UserEventListener> getExportablePlanItemDefinitionClass() {
        return UserEventListener.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(UserEventListener planItemDefinition) {
        return ELEMENT_USER_EVENT_LISTENER;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(UserEventListener userEventListener, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(userEventListener, xtw);
        String[] authorizedRoleRefs = userEventListener.getAuthorizedRoleRefs();
        if (authorizedRoleRefs != null && authorizedRoleRefs.length > 0) {
            xtw.writeAttribute(ATTRIBUTE_AUTHORIZED_ROLE_REFS, String.join(",", authorizedRoleRefs));
        }
    }
}
