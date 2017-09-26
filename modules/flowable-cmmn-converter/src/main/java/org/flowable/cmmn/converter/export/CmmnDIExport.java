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
package org.flowable.cmmn.converter.export;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamWriter;

import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;

public class CmmnDIExport implements CmmnXmlConstants {
    
    public static void writeCmmnDI(CmmnModel model, XMLStreamWriter xtw) throws Exception {
        // CMMN DI information
        xtw.writeStartElement(CMMNDI_PREFIX, ELEMENT_DI_CMMN, CMMNDI_NAMESPACE);
        xtw.writeStartElement(CMMNDI_PREFIX, ELEMENT_DI_DIAGRAM, CMMNDI_NAMESPACE);

        String caseId = model.getPrimaryCase().getId();

        xtw.writeAttribute(ATTRIBUTE_ID, "CMMNDiagram_" + caseId);

        for (String elementId : model.getLocationMap().keySet()) {
            if (elementId.equals(model.getPrimaryCase().getPlanModel().getId())) {
                writePlanModel(model.getPrimaryCase().getPlanModel(), model, xtw);
                
            } else {
                PlanItem planItem = model.findPlanItem(elementId);
                if (planItem != null) {
                    writePlanItem(planItem, model, xtw);
                }
            }
        }
        
        if (model.getFlowLocationMap().size() > 0) {
            Map<String, Association> associationMap = new LinkedHashMap<>();
            for (Association association : model.getAssociations()) {
                associationMap.put(association.getId(), association);
            }
            
            for (String elementId : model.getFlowLocationMap().keySet()) {
                Association association = associationMap.get(elementId);
                if (association == null) continue;
                
                createCmmnEdge(model, association.getId(), association.getSourceRef(), association.getTargetRef(), xtw);
            }
        }

        // end CMMN DI elements
        xtw.writeEndElement();
        xtw.writeEndElement();
    }
    
    protected static void writePlanModel(Stage stage, CmmnModel model, XMLStreamWriter xtw) throws Exception {
        createCmmnShape(model, stage.getId(), xtw);
        for (Criterion criterion : stage.getExitCriteria()) {
            createCmmnShape(model, criterion.getId(), xtw);
        }
    }
    
    protected static void writePlanItem(PlanItem planItem, CmmnModel model, XMLStreamWriter xtw) throws Exception {
        createCmmnShape(model, planItem.getId(), xtw);
        for (Criterion criterion : planItem.getEntryCriteria()) {
            createCmmnShape(model, criterion.getId(), xtw);
        }
        
        for (Criterion criterion : planItem.getExitCriteria()) {
            createCmmnShape(model, criterion.getId(), xtw);
        }
    }
    
    protected static void createCmmnShape(CmmnModel model, String elementId, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(CMMNDI_PREFIX, ELEMENT_DI_SHAPE, CMMNDI_NAMESPACE);
        String shapeId = "CMMNShape_" + elementId;
        xtw.writeAttribute(ATTRIBUTE_ID, shapeId);
        xtw.writeAttribute(ATTRIBUTE_DI_CMMN_ELEMENT_REF, elementId);

        GraphicInfo graphicInfo = model.getGraphicInfo(elementId);
        
        xtw.writeStartElement(OMGDC_PREFIX, ELEMENT_DI_BOUNDS, OMGDC_NAMESPACE);
        xtw.writeAttribute(ATTRIBUTE_DI_HEIGHT, String.valueOf(graphicInfo.getHeight()));
        xtw.writeAttribute(ATTRIBUTE_DI_WIDTH, String.valueOf(graphicInfo.getWidth()));
        xtw.writeAttribute(ATTRIBUTE_DI_X, String.valueOf(graphicInfo.getX()));
        xtw.writeAttribute(ATTRIBUTE_DI_Y, String.valueOf(graphicInfo.getY()));
        xtw.writeEndElement();
        
        // The xsd requires a CMMNLabel to be there, even though the spec text says it's optional
        xtw.writeStartElement(CMMNDI_PREFIX, ELEMENT_DI_LABEL, CMMNDI_NAMESPACE);
        xtw.writeEndElement();

        xtw.writeEndElement();
        
    }
    
    protected static void createCmmnEdge(CmmnModel model, String associationId, String sourceElementId, String targetElementId, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(CMMNDI_PREFIX, ELEMENT_DI_EDGE, CMMNDI_NAMESPACE);
        String edgeId = associationId;
        if (!edgeId.startsWith("CMMNEdge_")) {
            edgeId = "CMMNEdge_" + associationId;
        }
        xtw.writeAttribute(ATTRIBUTE_ID, edgeId);
        xtw.writeAttribute(ATTRIBUTE_DI_CMMN_ELEMENT_REF, sourceElementId);
        xtw.writeAttribute(ATTRIBUTE_DI_TARGET_CMMN_ELEMENT_REF, targetElementId);

        List<GraphicInfo> graphicInfoList = model.getFlowLocationGraphicInfo(associationId);
        for (GraphicInfo graphicInfo : graphicInfoList) {
            xtw.writeStartElement(OMGDI_PREFIX, ELEMENT_DI_WAYPOINT, OMGDI_NAMESPACE);
            xtw.writeAttribute(ATTRIBUTE_DI_X, String.valueOf(graphicInfo.getX()));
            xtw.writeAttribute(ATTRIBUTE_DI_Y, String.valueOf(graphicInfo.getY()));
            xtw.writeEndElement();
        }
        
        // The xsd requires a CMMNLabel to be there, even though the spec text says it's optional
        xtw.writeStartElement(CMMNDI_PREFIX, ELEMENT_DI_LABEL, CMMNDI_NAMESPACE);
        xtw.writeEndElement();

        xtw.writeEndElement();
    }
}
