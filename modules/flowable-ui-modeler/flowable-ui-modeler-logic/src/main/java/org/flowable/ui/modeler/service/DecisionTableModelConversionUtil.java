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

import org.apache.commons.lang3.StringUtils;
import org.flowable.editor.dmn.converter.DmnJsonConverterUtil;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.modeler.domain.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Yvo Swillens
 */
public class DecisionTableModelConversionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableDecisionTableService.class);

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

    public static Model convertModelToV3(Model decisionTableModel) {

        if (StringUtils.isNotEmpty(decisionTableModel.getModelEditorJson())) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();

                JsonNode decisionTableNode = objectMapper.readTree(decisionTableModel.getModelEditorJson());
                DmnJsonConverterUtil.migrateModelV3(decisionTableNode, objectMapper);

                // replace editor json
                decisionTableModel.setModelEditorJson(decisionTableNode.toString());

            } catch (Exception e) {
                throw new InternalServerErrorException(String.format("Error converting decision table %s to new model version", decisionTableModel.getName()), e);
            }
        }

        return decisionTableModel;
    }
}
