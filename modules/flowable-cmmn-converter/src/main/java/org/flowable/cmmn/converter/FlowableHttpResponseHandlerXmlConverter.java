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
package org.flowable.cmmn.converter;

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.FlowableHttpResponseHandler;
import org.flowable.cmmn.model.HttpServiceTask;

import javax.xml.stream.XMLStreamReader;

import static org.flowable.cmmn.converter.CmmnXmlConstants.ELEMENT_HTTP_RESPONSE_HANDLER;

/**
 * @author Tijs Rademakers
 */
public class FlowableHttpResponseHandlerXmlConverter extends AbstractFlowableHttpHandlerXmlConverter {

    @Override
    public String getXMLElementName() {
        return ELEMENT_HTTP_RESPONSE_HANDLER;
    }

    @Override
    public boolean hasChildElements() {
        return false;
    }

    @Override
    protected BaseElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CmmnElement cmmnElement = conversionHelper.getCurrentCmmnElement();
        if (!(cmmnElement instanceof HttpServiceTask)) {
            return null;
        }

        FlowableHttpResponseHandler responseHandler = new FlowableHttpResponseHandler();
        setImplementation(xtr, responseHandler);

        ((HttpServiceTask) cmmnElement).setHttpResponseHandler(responseHandler);

        return null;
    }

}
