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
package org.flowable.bpm.model.xml.testmodel;

import org.flowable.bpm.model.xml.ModelInstance;
import org.flowable.bpm.model.xml.ModelValidationException;
import org.flowable.bpm.model.xml.impl.ModelImpl;
import org.flowable.bpm.model.xml.impl.ModelInstanceImpl;
import org.flowable.bpm.model.xml.impl.parser.AbstractModelParser;
import org.flowable.bpm.model.xml.impl.util.ReflectUtil;
import org.flowable.bpm.model.xml.instance.DomDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.SchemaFactory;

public class TestModelParser
        extends AbstractModelParser {

    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    private static final String SCHEMA_LOCATION = "org/flowable/bpm/model/xml/testmodel/Testmodel.xsd";
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static final String TEST_NS = "http://flowable.org/animals";

    public TestModelParser() {
        this.schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA);
        try {
            addSchema(TEST_NS, schemaFactory.newSchema(ReflectUtil.getResource(SCHEMA_LOCATION)));
        }
        catch (SAXException e) {
            throw new ModelValidationException("Unable to parse schema:" + ReflectUtil.getResource(SCHEMA_LOCATION), e);
        }
    }

    @Override
    protected void configureFactory(DocumentBuilderFactory dbf) {
        dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        dbf.setAttribute(JAXP_SCHEMA_SOURCE, ReflectUtil.getResource(SCHEMA_LOCATION).toString());
        super.configureFactory(dbf);
    }

    @Override
    protected ModelInstance createModelInstance(DomDocument document) {
        return new ModelInstanceImpl((ModelImpl) TestModel.getTestModel(), TestModel.getModelBuilder(), document);
    }

}
