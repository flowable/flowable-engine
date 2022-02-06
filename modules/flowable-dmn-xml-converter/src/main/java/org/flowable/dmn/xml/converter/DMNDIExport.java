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
package org.flowable.dmn.xml.converter;

import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnDiDiagram;
import org.flowable.dmn.model.GraphicInfo;
import org.flowable.dmn.xml.constants.DmnXMLConstants;

public class DMNDIExport implements DmnXMLConstants {

    public static void writeDMNDI(DmnDefinition model, XMLStreamWriter xtw) throws Exception {
        // start DI information
        xtw.writeStartElement(DMNDI_PREFIX, ELEMENT_DI_DMN, DMNDI_NAMESPACE);

        for (Map.Entry<String, Map<String, GraphicInfo>> diagramEntry : model.getLocationByDiagramIdMap().entrySet()) {
            DmnDiDiagram diDiagram = model.getDiDiagram(diagramEntry.getKey());
            Map<String, GraphicInfo> graphicInfoMap = diagramEntry.getValue();

            // start DI diagram
            xtw.writeStartElement(DMNDI_PREFIX, ELEMENT_DI_DIAGRAM, DMNDI_NAMESPACE);
            xtw.writeAttribute(ATTRIBUTE_ID, diDiagram.getId());

            if (StringUtils.isNotEmpty(diDiagram.getName())) {
                xtw.writeAttribute(ATTRIBUTE_NAME, diDiagram.getName());
            }

            // DI size
            if (diDiagram.getGraphicInfo() != null) {
                xtw.writeStartElement(DMNDI_PREFIX, ELEMENT_DI_SIZE, DMNDI_NAMESPACE);
                xtw.writeAttribute(ATTRIBUTE_DI_HEIGHT, String.valueOf(diDiagram.getGraphicInfo().getHeight()));
                xtw.writeAttribute(ATTRIBUTE_DI_WIDTH, String.valueOf(diDiagram.getGraphicInfo().getWidth()));
                xtw.writeEndElement();
            }

            // DI shape
            for (Map.Entry<String, GraphicInfo> graphicInfoEntry : graphicInfoMap.entrySet()) {
                xtw.writeStartElement(DMNDI_PREFIX, ELEMENT_DI_SHAPE, DMNDI_NAMESPACE);
                xtw.writeAttribute(ATTRIBUTE_ID, "DMNShape_" + graphicInfoEntry.getKey());
                xtw.writeAttribute(ATTRIBUTE_DI_DMN_ELEMENT_REF, graphicInfoEntry.getKey());

                createDmnShapeBounds(graphicInfoEntry.getValue(), xtw);

                if (model.getDecisionServiceDividerLocationMapByDiagramId(diDiagram.getId()) != null
                    && model.getDecisionServiceDividerLocationMapByDiagramId(diDiagram.getId()).containsKey(graphicInfoEntry.getKey())) {
                    createDmnDecisionServiceDividerLine(model.getDecisionServiceDividerLocationMapByDiagramId(diDiagram.getId()).get(graphicInfoEntry.getKey()), xtw);
                }

                xtw.writeEndElement();
            }

            // DI edge
            Map<String, List<GraphicInfo>> flowLocationGraphicInfoMap = model.getFlowLocationMapByDiagramId(diDiagram.getId());
            if (flowLocationGraphicInfoMap != null) {
                for (Map.Entry<String, List<GraphicInfo>> flowLocationGraphicInfoEntry : flowLocationGraphicInfoMap.entrySet()) {
                    createDmnEdge(flowLocationGraphicInfoEntry.getKey(), flowLocationGraphicInfoEntry.getValue(), xtw);
                }
            }

            // end DI diagram
            xtw.writeEndElement();
        }

        xtw.writeEndElement();
        // end DI information
    }

    protected static void createDmnShapeBounds(GraphicInfo graphicInfo, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(OMGDC_PREFIX, ELEMENT_DI_BOUNDS, OMGDC_NAMESPACE);
        xtw.writeAttribute(ATTRIBUTE_DI_HEIGHT, String.valueOf(graphicInfo.getHeight()));
        xtw.writeAttribute(ATTRIBUTE_DI_WIDTH, String.valueOf(graphicInfo.getWidth()));
        xtw.writeAttribute(ATTRIBUTE_DI_X, String.valueOf(graphicInfo.getX()));
        xtw.writeAttribute(ATTRIBUTE_DI_Y, String.valueOf(graphicInfo.getY()));
        xtw.writeEndElement();
    }

    protected static void createDmnDecisionServiceDividerLine(List<GraphicInfo> graphicInfoList, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(DMNDI_PREFIX, ELEMENT_DI_DECISION_SERVICE_DIVIDER_LINE, DMNDI_NAMESPACE);

        for (GraphicInfo graphicInfo : graphicInfoList) {
            xtw.writeStartElement(OMGDI_PREFIX, ELEMENT_DI_WAYPOINT, OMGDI_NAMESPACE);
            xtw.writeAttribute(ATTRIBUTE_DI_X, String.valueOf(graphicInfo.getX()));
            xtw.writeAttribute(ATTRIBUTE_DI_Y, String.valueOf(graphicInfo.getY()));
            xtw.writeEndElement();
        }

        xtw.writeEndElement();
    }

    protected static void createDmnEdge(String elementId, List<GraphicInfo> graphicInfoList, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(DMNDI_PREFIX, ELEMENT_DI_EDGE, DMNDI_NAMESPACE);
        xtw.writeAttribute(ATTRIBUTE_ID, "DMNEdge_" + elementId);
        xtw.writeAttribute(ATTRIBUTE_DI_DMN_ELEMENT_REF, elementId);

        for (GraphicInfo graphicInfo : graphicInfoList) {
            xtw.writeStartElement(OMGDI_PREFIX, ELEMENT_DI_WAYPOINT, OMGDI_NAMESPACE);
            xtw.writeAttribute(ATTRIBUTE_DI_X, String.valueOf(graphicInfo.getX()));
            xtw.writeAttribute(ATTRIBUTE_DI_Y, String.valueOf(graphicInfo.getY()));
            xtw.writeEndElement();
        }

        xtw.writeEndElement();
    }
}
