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
package org.flowable.engine.impl.bpmn.parser.factory;

import org.flowable.bpmn.model.Import;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.bpmn.parser.XMLImporter;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * Default implementation of the {@link XMLImporterFactory}. Used when no custom {@link XMLImporterFactory} is injected
 * on the {@link ProcessEngineConfigurationImpl}.
 * 
 * @author Christophe DENEUX
 */
public class DefaultXMLImporterFactory implements XMLImporterFactory {

    // Name of the default XML Importer, provided by flowable-cxf
    private static final String DEFAULT_XML_IMPORTER_FACTORY_CLASSNAME = "org.flowable.engine.impl.webservice.CxfWSDLImporter";

    @Override
    public XMLImporter createXMLImporter(Import theImport) throws FlowableException {

        try {
            Class<?> wsdlImporterClass = Class.forName(DEFAULT_XML_IMPORTER_FACTORY_CLASSNAME, true,
                    Thread.currentThread().getContextClassLoader());
            return (XMLImporter) wsdlImporterClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new FlowableException("Could not find importer class for type " + theImport.getImportType(), e);
        } catch (Exception e) {
            throw new FlowableException(String.format("Error instantiating XML importer '%s' for type '%s'",
                    DEFAULT_XML_IMPORTER_FACTORY_CLASSNAME, theImport.getImportType()), e);
        }
    }
}
