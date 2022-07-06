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
package org.flowable.eventregistry.impl.tenantdetector;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.InboundEventTenantDetector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Joram Barrez
 */
public class XpathBasedInboundEventTenantDetector implements InboundEventTenantDetector<Document> {

    protected String xpathExpression;

    public XpathBasedInboundEventTenantDetector(String xpathExpression) {
        this.xpathExpression = xpathExpression;
    }

    @Override
    public String detectTenantId(Document payload) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node result = (Node) xPath.compile(xpathExpression).evaluate(payload, XPathConstants.NODE);
            return result.getTextContent();
        } catch (Exception e) {
            throw new FlowableException("Could not evaluate xpath expression ", e);
        }
    }

    public String getXpathExpression() {
        return xpathExpression;
    }
    public void setXpathExpression(String xpathExpression) {
        this.xpathExpression = xpathExpression;
    }
}
