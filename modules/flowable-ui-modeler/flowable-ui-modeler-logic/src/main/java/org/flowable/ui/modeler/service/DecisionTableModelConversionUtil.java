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
package org.flowable.ui.modeler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.flowable.editor.dmn.converter.DmnJsonConverterUtil;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.modeler.domain.Model;

/**
 * @author Yvo Swillens
 */
public class DecisionTableModelConversionUtil {

    public static Model convertModel(Model decisionTableModel) {

        if (StringUtils.isNotEmpty(decisionTableModel.getModelEditorJson())) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();

                JsonNode decisionTableNode = objectMapper.readTree(decisionTableModel.getModelEditorJson());
                DmnJsonConverterUtil.migrateModel(decisionTableNode, objectMapper);

                // replace editor json
                decisionTableModel.setModelEditorJson(decisionTableNode.toString());

            } catch (Exception e) {
                throw new InternalServerErrorException(String.format("Error converting decision table %s to new model version", decisionTableModel.getName()));
            }
        }

        return decisionTableModel;
    }
}
