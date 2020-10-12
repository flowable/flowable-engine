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
package org.flowable.eventregistry.impl.keydetector;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.InboundEventKeyDetector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author Joram Barrez
 */
public class XpathBasedInboundEventKeyDetector implements InboundEventKeyDetector<Document> {

    protected String xpathExpression;

    public XpathBasedInboundEventKeyDetector(String xpathExpression) {
        this.xpathExpression = xpathExpression;
    }

    @Override
    public String detectEventDefinitionKey(Document document) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node result = (Node) xPath.compile(xpathExpression).evaluate(document, XPathConstants.NODE);
            return result.getTextContent();
        } catch (Exception e) {
            throw new FlowableException("Could not evaluate xpath expression ", e);
        }
    }

}
