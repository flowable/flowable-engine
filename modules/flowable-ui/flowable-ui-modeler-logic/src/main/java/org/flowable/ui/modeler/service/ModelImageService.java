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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.bpmn.model.Artifact;
import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.DataObject;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.util.ImageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
@Transactional
public class ModelImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelImageService.class);

    private static float THUMBNAIL_WIDTH = 300f;

    protected BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
    protected CmmnJsonConverter cmmnJsonConverter = new CmmnJsonConverter();

    public byte[] generateThumbnailImage(Model model, ObjectNode editorJsonNode) {
        try {

            BpmnModel bpmnModel = bpmnJsonConverter.convertToBpmnModel(editorJsonNode);

            double scaleFactor = 1.0;
            GraphicInfo diagramInfo = calculateDiagramSize(bpmnModel);
            if (diagramInfo.getWidth() > THUMBNAIL_WIDTH) {
                scaleFactor = diagramInfo.getWidth() / THUMBNAIL_WIDTH;
                scaleDiagram(bpmnModel, scaleFactor);
            }

            BufferedImage modelImage = ImageGenerator.createImage(bpmnModel, scaleFactor);
            if (modelImage != null) {
                return ImageGenerator.createByteArrayForImage(modelImage, "png");
            }
        } catch (Exception e) {
            LOGGER.error("Error creating thumbnail image {}", model.getId(), e);
        }
        return null;
    }

    public byte[] generateCmmnThumbnailImage(Model model, ObjectNode editorJsonNode) {
        try {

            CmmnModel cmmnModel = cmmnJsonConverter.convertToCmmnModel(editorJsonNode);

            double scaleFactor = 1.0;
            GraphicInfo diagramInfo = calculateDiagramSize(cmmnModel);
            if (diagramInfo.getWidth() > THUMBNAIL_WIDTH) {
                scaleFactor = diagramInfo.getWidth() / THUMBNAIL_WIDTH;
                scaleDiagram(cmmnModel, scaleFactor);
            }

            BufferedImage modelImage = ImageGenerator.createCmmnImage(cmmnModel, scaleFactor);
            if (modelImage != null) {
                return ImageGenerator.createByteArrayForImage(modelImage, "png");
            }
        } catch (Exception e) {
            LOGGER.error("Error creating thumbnail cmmn image {}", model.getId(), e);
        }
        return null;
    }

    protected GraphicInfo calculateDiagramSize(BpmnModel bpmnModel) {
        GraphicInfo diagramInfo = new GraphicInfo();

        for (Pool pool : bpmnModel.getPools()) {
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(pool.getId());
            double elementMaxX = graphicInfo.getX() + graphicInfo.getWidth();
            double elementMaxY = graphicInfo.getY() + graphicInfo.getHeight();

            if (elementMaxX > diagramInfo.getWidth()) {
                diagramInfo.setWidth(elementMaxX);
            }
            if (elementMaxY > diagramInfo.getHeight()) {
                diagramInfo.setHeight(elementMaxY);
            }
        }

        for (Process process : bpmnModel.getProcesses()) {
            calculateWidthForFlowElements(process.getFlowElements(), bpmnModel, diagramInfo);
            calculateWidthForArtifacts(process.getArtifacts(), bpmnModel, diagramInfo);
        }
        return diagramInfo;
    }

    protected GraphicInfo calculateDiagramSize(CmmnModel cmmnModel) {
        GraphicInfo diagramInfo = new GraphicInfo();

        for (Case caseModel : cmmnModel.getCases()) {
            Stage stage = caseModel.getPlanModel();
            org.flowable.cmmn.model.GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(stage.getId());
            double elementMaxX = graphicInfo.getX() + graphicInfo.getWidth();
            double elementMaxY = graphicInfo.getY() + graphicInfo.getHeight();

            if (elementMaxX > diagramInfo.getWidth()) {
                diagramInfo.setWidth(elementMaxX);
            }
            if (elementMaxY > diagramInfo.getHeight()) {
                diagramInfo.setHeight(elementMaxY);
            }
        }
        return diagramInfo;
    }

    protected void scaleDiagram(BpmnModel bpmnModel, double scaleFactor) {
        for (Pool pool : bpmnModel.getPools()) {
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(pool.getId());
            scaleGraphicInfo(graphicInfo, scaleFactor);
        }

        for (Process process : bpmnModel.getProcesses()) {
            scaleFlowElements(process.getFlowElements(), bpmnModel, scaleFactor);
            scaleArtifacts(process.getArtifacts(), bpmnModel, scaleFactor);
            for (Lane lane : process.getLanes()) {
                scaleGraphicInfo(bpmnModel.getGraphicInfo(lane.getId()), scaleFactor);
            }
        }
    }

    protected void scaleDiagram(CmmnModel cmmnModel, double scaleFactor) {
        for (Case caseModel : cmmnModel.getCases()) {
            org.flowable.cmmn.model.GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseModel.getPlanModel().getId());
            scaleCmmnGraphicInfo(graphicInfo, scaleFactor);

            for (Criterion criterion : caseModel.getPlanModel().getExitCriteria()) {
                org.flowable.cmmn.model.GraphicInfo criterionGraphicInfo = cmmnModel.getGraphicInfo(criterion.getId());
                scaleCmmnGraphicInfo(criterionGraphicInfo, scaleFactor);
            }

            scalePlanItems(caseModel.getPlanModel().getPlanItems(), cmmnModel, scaleFactor);
        }

        scaleAssociations(cmmnModel.getAssociations(), cmmnModel, scaleFactor);
    }

    protected void calculateWidthForFlowElements(Collection<FlowElement> elementList, BpmnModel bpmnModel, GraphicInfo diagramInfo) {
        for (FlowElement flowElement : elementList) {
            List<GraphicInfo> graphicInfoList = new ArrayList<>();
            if (flowElement instanceof SequenceFlow) {
                List<GraphicInfo> flowGraphics = bpmnModel.getFlowLocationGraphicInfo(flowElement.getId());
                if (flowGraphics != null && flowGraphics.size() > 0) {
                    graphicInfoList.addAll(flowGraphics);
                }
            } else {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowElement.getId());
                if (graphicInfo != null) {
                    graphicInfoList.add(graphicInfo);
                }
            }

            processGraphicInfoList(graphicInfoList, diagramInfo);
        }
    }

    protected void calculateWidthForArtifacts(Collection<Artifact> artifactList, BpmnModel bpmnModel, GraphicInfo diagramInfo) {
        for (Artifact artifact : artifactList) {
            List<GraphicInfo> graphicInfoList = new ArrayList<>();
            if (artifact instanceof Association) {
                graphicInfoList.addAll(bpmnModel.getFlowLocationGraphicInfo(artifact.getId()));
            } else {
                graphicInfoList.add(bpmnModel.getGraphicInfo(artifact.getId()));
            }

            processGraphicInfoList(graphicInfoList, diagramInfo);
        }
    }

    protected void processGraphicInfoList(List<GraphicInfo> graphicInfoList, GraphicInfo diagramInfo) {
        for (GraphicInfo graphicInfo : graphicInfoList) {
            double elementMaxX = graphicInfo.getX() + graphicInfo.getWidth();
            double elementMaxY = graphicInfo.getY() + graphicInfo.getHeight();

            if (elementMaxX > diagramInfo.getWidth()) {
                diagramInfo.setWidth(elementMaxX);
            }
            if (elementMaxY > diagramInfo.getHeight()) {
                diagramInfo.setHeight(elementMaxY);
            }
        }
    }

    protected void scaleFlowElements(Collection<FlowElement> elementList, BpmnModel bpmnModel, double scaleFactor) {
        for (FlowElement flowElement : elementList) {
            List<GraphicInfo> graphicInfoList = new ArrayList<>();
            if (flowElement instanceof SequenceFlow) {
                List<GraphicInfo> flowList = bpmnModel.getFlowLocationGraphicInfo(flowElement.getId());
                if (flowList != null) {
                    graphicInfoList.addAll(flowList);
                }

            // no graphic info for Data Objects
            } else if (!DataObject.class.isInstance(flowElement)) {
                graphicInfoList.add(bpmnModel.getGraphicInfo(flowElement.getId()));
            }

            scaleGraphicInfoList(graphicInfoList, scaleFactor);

            if (flowElement instanceof SubProcess) {
                SubProcess subProcess = (SubProcess) flowElement;
                scaleFlowElements(subProcess.getFlowElements(), bpmnModel, scaleFactor);
            }
        }
    }

    protected void scaleArtifacts(Collection<Artifact> artifactList, BpmnModel bpmnModel, double scaleFactor) {
        for (Artifact artifact : artifactList) {
            List<GraphicInfo> graphicInfoList = new ArrayList<>();
            if (artifact instanceof Association) {
                List<GraphicInfo> flowList = bpmnModel.getFlowLocationGraphicInfo(artifact.getId());
                if (flowList != null) {
                    graphicInfoList.addAll(flowList);
                }
            } else {
                graphicInfoList.add(bpmnModel.getGraphicInfo(artifact.getId()));
            }

            scaleGraphicInfoList(graphicInfoList, scaleFactor);
        }
    }

    protected void scalePlanItems(Collection<PlanItem> itemList, CmmnModel cmmnModel, double scaleFactor) {
        for (PlanItem planItem : itemList) {
            org.flowable.cmmn.model.GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(planItem.getId());
            scaleCmmnGraphicInfo(graphicInfo, scaleFactor);

            if (planItem.getPlanItemDefinition() instanceof Stage) {
                Stage stage = (Stage) planItem.getPlanItemDefinition();
                scalePlanItems(stage.getPlanItems(), cmmnModel, scaleFactor);
            }

            for (Criterion criterion : planItem.getEntryCriteria()) {
                org.flowable.cmmn.model.GraphicInfo criterionGraphicInfo = cmmnModel.getGraphicInfo(criterion.getId());
                scaleCmmnGraphicInfo(criterionGraphicInfo, scaleFactor);
            }

            for (Criterion criterion : planItem.getExitCriteria()) {
                org.flowable.cmmn.model.GraphicInfo criterionGraphicInfo = cmmnModel.getGraphicInfo(criterion.getId());
                scaleCmmnGraphicInfo(criterionGraphicInfo, scaleFactor);
            }
        }
    }

    protected void scaleAssociations(List<org.flowable.cmmn.model.Association> associationList, CmmnModel cmmnModel, double scaleFactor) {
        for (org.flowable.cmmn.model.Association association : associationList) {
            List<org.flowable.cmmn.model.GraphicInfo> flowList = cmmnModel.getFlowLocationGraphicInfo(association.getId());
            scaleCmmnGraphicInfoList(flowList, scaleFactor);
        }
    }

    protected void scaleGraphicInfoList(List<GraphicInfo> graphicInfoList, double scaleFactor) {
        for (GraphicInfo graphicInfo : graphicInfoList) {
            scaleGraphicInfo(graphicInfo, scaleFactor);
        }
    }

    protected void scaleGraphicInfo(GraphicInfo graphicInfo, double scaleFactor) {
        graphicInfo.setX(graphicInfo.getX() / scaleFactor);
        graphicInfo.setY(graphicInfo.getY() / scaleFactor);
        graphicInfo.setWidth(graphicInfo.getWidth() / scaleFactor);
        graphicInfo.setHeight(graphicInfo.getHeight() / scaleFactor);
    }

    protected void scaleCmmnGraphicInfoList(List<org.flowable.cmmn.model.GraphicInfo> graphicInfoList, double scaleFactor) {
        if (graphicInfoList != null) {
            for (org.flowable.cmmn.model.GraphicInfo graphicInfo : graphicInfoList) {
                scaleCmmnGraphicInfo(graphicInfo, scaleFactor);
            }
        }
    }

    protected void scaleCmmnGraphicInfo(org.flowable.cmmn.model.GraphicInfo graphicInfo, double scaleFactor) {
        graphicInfo.setX(graphicInfo.getX() / scaleFactor);
        graphicInfo.setY(graphicInfo.getY() / scaleFactor);
        graphicInfo.setWidth(graphicInfo.getWidth() / scaleFactor);
        graphicInfo.setHeight(graphicInfo.getHeight() / scaleFactor);
    }
}
