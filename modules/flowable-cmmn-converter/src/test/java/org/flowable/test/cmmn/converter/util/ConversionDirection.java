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
package org.flowable.test.cmmn.converter.util;

import java.util.function.Function;
import java.util.function.Supplier;

import org.flowable.cmmn.model.CmmnModel;

/**
 * @author Filip Hrisafov
 */
public enum ConversionDirection {
    xmlToModel(XmlTestUtils::readXMLFile),
    xmlToModelAndBack(XmlTestUtils::readXmlExportAndReadAgain),
    ;

    private final Function<String, CmmnModel> modelProvider;

    ConversionDirection(Function<String, CmmnModel> modelProvider) {
        this.modelProvider = modelProvider;
    }

    public Supplier<CmmnModel> supplyCmmnModel(String resource) {
        return () -> modelProvider.apply(resource);
    }
}
