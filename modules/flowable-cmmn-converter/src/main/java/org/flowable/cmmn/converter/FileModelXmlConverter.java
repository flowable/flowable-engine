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

import javax.xml.stream.XMLStreamReader;

import org.flowable.cmmn.model.CaseFileModel;
import org.flowable.cmmn.model.CmmnElement;

/**
 * @author Joram Barrez
 */
public class FileModelXmlConverter extends CaseElementXmlConverter {

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_FILE_MODEL;
    }

    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        CaseFileModel fileModel = new CaseFileModel();

        conversionHelper.setCurrentFileItemContainer(fileModel);
        conversionHelper.getCurrentCase().setFileModel(fileModel);

        return fileModel;
    }

    @Override
    protected void elementEnd(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        conversionHelper.removeCurrentFileItemContainer();
        super.elementEnd(xtr, conversionHelper);
    }

}
