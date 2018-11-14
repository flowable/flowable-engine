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
package org.flowable.cmmn.converter.util;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.ImplementationType;

/**
 * @author Joram Barrez
 */
public class ListenerXmlConverterUtil {

    public static FlowableListener convertToListener(XMLStreamReader xtr) {
        FlowableListener listener = new FlowableListener();
        CmmnXmlUtil.addXMLLocation(listener, xtr);
        if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_CLASS))) {
            listener.setImplementation(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_CLASS));
            listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_EXPRESSION))) {
            listener.setImplementation(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_EXPRESSION));
            listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_DELEGATEEXPRESSION))) {
            listener.setImplementation(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_DELEGATEEXPRESSION));
            listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        }

        listener.setEvent(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_EVENT));
        listener.setSourceState(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_SOURCE_STATE));
        listener.setTargetState(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_TARGET_STATE));
        listener.setOnTransaction(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_LISTENER_ON_TRANSACTION));

        return listener;
    }

}
