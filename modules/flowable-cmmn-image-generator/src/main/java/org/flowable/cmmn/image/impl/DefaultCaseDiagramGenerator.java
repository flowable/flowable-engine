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

package org.flowable.cmmn.image.impl;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.image.CaseDiagramGenerator;
import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.ExternalWorkerServiceTask;
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.SendEventServiceTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.cmmn.model.UserEventListener;
import org.flowable.cmmn.model.VariableEventListener;

/**
 * Class to generate an image based the diagram interchange information in a CMMN 1.1 case.
 *
 * @author Tijs Rademakers
 */
public class DefaultCaseDiagramGenerator implements CaseDiagramGenerator {

    protected Map<Class<? extends CmmnElement>, ActivityDrawInstruction> activityDrawInstructions = new HashMap<>();

    public DefaultCaseDiagramGenerator() {
        this(1.0);
    }

    // The instructions on how to draw a certain construct is
    // created statically and stored in a map for performance.
    public DefaultCaseDiagramGenerator(final double scaleFactor) {
        // generic event listener
        activityDrawInstructions.put(GenericEventListener.class, new ActivityDrawInstruction() {
            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawGenericEventListener(graphicInfo, scaleFactor);
            }
        });
        
        // timer event listener
        activityDrawInstructions.put(TimerEventListener.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawTimerEventListener(graphicInfo, scaleFactor);
            }
        });

        // user event listener
        activityDrawInstructions.put(UserEventListener.class, new ActivityDrawInstruction() {
            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawUserEventListener(graphicInfo, scaleFactor);
            }
        });
        
        // variable event listener
        activityDrawInstructions.put(VariableEventListener.class, new ActivityDrawInstruction() {
            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawVariableEventListener(graphicInfo, scaleFactor);
            }
        });

        // task
        activityDrawInstructions.put(Task.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawTask(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });

        // user task
        activityDrawInstructions.put(HumanTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawUserTask(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });
        
        // send event task
        activityDrawInstructions.put(SendEventServiceTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawSendEventTask(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });

        // external worker service task
        activityDrawInstructions.put(ExternalWorkerServiceTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawServiceTask(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });

        // service task
        activityDrawInstructions.put(ServiceTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawServiceTask(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });

        // case task
        activityDrawInstructions.put(CaseTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawCaseTask(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });

        // process task
        activityDrawInstructions.put(ProcessTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawProcessTask(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });

        // decision task
        activityDrawInstructions.put(DecisionTask.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawDecisionTask(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });

        // milestone
        activityDrawInstructions.put(Milestone.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawMilestone(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });

        // criterion
        activityDrawInstructions.put(Criterion.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                Criterion criterion = (Criterion) caseElement;
                if (criterion.isEntryCriterion()) {
                    caseDiagramCanvas.drawEntryCriterion(graphicInfo);
                } else if (criterion.isExitCriterion()) {
                    caseDiagramCanvas.drawExitCriterion(graphicInfo);
                }
            }
        });

        // stage
        activityDrawInstructions.put(Stage.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement) {
                GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(caseElement.getId());
                caseDiagramCanvas.drawStage(caseElement.getName(), graphicInfo, scaleFactor);
            }
        });
    }

    @Override
    public InputStream generateDiagram(CmmnModel cmmnModel, String imageType,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader, double scaleFactor) {

        return generateCaseDiagram(cmmnModel, imageType, activityFontName, labelFontName, annotationFontName, customClassLoader, scaleFactor).generateImage(imageType);
    }

    @Override
    public InputStream generateDiagram(CmmnModel cmmnModel, String imageType) {
        return generateDiagram(cmmnModel, imageType, null, null, null, null, 1.0);
    }

    @Override
    public InputStream generateDiagram(CmmnModel cmmnModel, String imageType, double scaleFactor) {
        return generateDiagram(cmmnModel, imageType, null, null, null, null, scaleFactor);
    }

    @Override
    public InputStream generateDiagram(CmmnModel cmmnModel, String imageType, String activityFontName,
            String labelFontName, String annotationFontName, ClassLoader customClassLoader) {

        return generateDiagram(cmmnModel, imageType, activityFontName, labelFontName, annotationFontName, customClassLoader, 1.0);
    }

    @Override
    public InputStream generatePngDiagram(CmmnModel cmmnModel) {
        return generatePngDiagram(cmmnModel, 1.0);
    }

    @Override
    public InputStream generatePngDiagram(CmmnModel cmmnModel, double scaleFactor) {
        return generateDiagram(cmmnModel, "png", scaleFactor);
    }

    @Override
    public InputStream generateJpgDiagram(CmmnModel cmmnModel) {
        return generateJpgDiagram(cmmnModel, 1.0);
    }

    @Override
    public InputStream generateJpgDiagram(CmmnModel cmmnModel, double scaleFactor) {
        return generateDiagram(cmmnModel, "jpg", scaleFactor);
    }

    public BufferedImage generateImage(CmmnModel cmmnModel, String imageType,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader, double scaleFactor) {

        return generateCaseDiagram(cmmnModel, imageType, activityFontName, labelFontName, annotationFontName,
                        customClassLoader, scaleFactor).generateBufferedImage(imageType);
    }

    public BufferedImage generateImage(CmmnModel cmmnModel, String imageType, double scaleFactor) {
        return generateImage(cmmnModel, imageType, null, null, null, null, scaleFactor);
    }

    @Override
    public BufferedImage generatePngImage(CmmnModel cmmnModel, double scaleFactor) {
        return generateImage(cmmnModel, "png", scaleFactor);
    }

    protected DefaultCaseDiagramCanvas generateCaseDiagram(CmmnModel cmmnModel, String imageType,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader, double scaleFactor) {

        prepareCmmnModel(cmmnModel);

        DefaultCaseDiagramCanvas caseDiagramCanvas = initCaseDiagramCanvas(cmmnModel, imageType, activityFontName, labelFontName, annotationFontName, customClassLoader);

        // Draw elements
        for (Case caseModel : cmmnModel.getCases()) {

            Stage planModel = caseModel.getPlanModel();
            GraphicInfo graphicInfo = cmmnModel.getGraphicInfo(planModel.getId());
            caseDiagramCanvas.drawStage(planModel.getName(), graphicInfo, scaleFactor);

            for (Criterion criterion : planModel.getExitCriteria()) {
                ActivityDrawInstruction criterionInstruction = activityDrawInstructions.get(criterion.getClass());
                criterionInstruction.draw(caseDiagramCanvas, cmmnModel, criterion);
            }

            for (PlanItem planItem : caseModel.getPlanModel().getPlanItems()) {
                drawActivity(caseDiagramCanvas, cmmnModel, planItem, scaleFactor);
            }
        }

        // Draw associations
        for (Association association : cmmnModel.getAssociations()) {
            drawAssociation(caseDiagramCanvas, cmmnModel, association, scaleFactor);
        }

        return caseDiagramCanvas;
    }

    protected void prepareCmmnModel(CmmnModel cmmnModel) {

        // Need to make sure all elements have positive x and y.
        // Check all graphicInfo and update the elements accordingly

        List<GraphicInfo> allGraphicInfos = new ArrayList<>();
        if (cmmnModel.getLocationMap() != null) {
            allGraphicInfos.addAll(cmmnModel.getLocationMap().values());
        }
        if (cmmnModel.getFlowLocationMap() != null) {
            for (List<GraphicInfo> flowGraphicInfos : cmmnModel.getFlowLocationMap().values()) {
                allGraphicInfos.addAll(flowGraphicInfos);
            }
        }

        if (allGraphicInfos.size() > 0) {

            boolean needsTranslationX = false;
            boolean needsTranslationY = false;

            double lowestX = 0.0;
            double lowestY = 0.0;

            // Collect lowest x and y
            for (GraphicInfo graphicInfo : allGraphicInfos) {

                double x = graphicInfo.getX();
                double y = graphicInfo.getY();

                if (x < lowestX) {
                    needsTranslationX = true;
                    lowestX = x;
                }
                if (y < lowestY) {
                    needsTranslationY = true;
                    lowestY = y;
                }

            }

            // Update all graphicInfo objects
            if (needsTranslationX || needsTranslationY) {

                double translationX = Math.abs(lowestX);
                double translationY = Math.abs(lowestY);

                for (GraphicInfo graphicInfo : allGraphicInfos) {
                    if (needsTranslationX) {
                        graphicInfo.setX(graphicInfo.getX() + translationX);
                    }
                    if (needsTranslationY) {
                        graphicInfo.setY(graphicInfo.getY() + translationY);
                    }
                }
            }

        }

    }

    protected void drawActivity(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, PlanItem planItem, double scaleFactor) {

        ActivityDrawInstruction drawInstruction = activityDrawInstructions.get(planItem.getPlanItemDefinition().getClass());
        if (drawInstruction != null) {
            drawInstruction.draw(caseDiagramCanvas, cmmnModel, planItem);
        
        } else if (planItem.getPlanItemDefinition() instanceof Task) {
            activityDrawInstructions.get(Task.class).draw(caseDiagramCanvas, cmmnModel, planItem);
        }

        // Nested elements
        if (planItem.getPlanItemDefinition() instanceof Stage) {
            for (PlanItem childPlanItem : ((Stage) planItem.getPlanItemDefinition()).getPlanItems()) {
                drawActivity(caseDiagramCanvas, cmmnModel, childPlanItem, scaleFactor);
            }
        }

        for (Criterion criterion : planItem.getEntryCriteria()) {
            ActivityDrawInstruction criterionInstruction = activityDrawInstructions.get(criterion.getClass());
            criterionInstruction.draw(caseDiagramCanvas, cmmnModel, criterion);
        }

        for (Criterion criterion : planItem.getExitCriteria()) {
            ActivityDrawInstruction criterionInstruction = activityDrawInstructions.get(criterion.getClass());
            criterionInstruction.draw(caseDiagramCanvas, cmmnModel, criterion);
        }
    }

    protected void drawAssociation(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, Association association, double scaleFactor) {
        // Outgoing transitions of activity
        String sourceRef = association.getSourceRef();
        String targetRef = association.getTargetRef();

        BaseElement sourceElement = cmmnModel.getCriterion(sourceRef);
        if (sourceElement == null) {
            sourceElement = cmmnModel.findPlanItem(sourceRef);
            if (sourceElement == null) {
                PlanItemDefinition planItemDefinition = cmmnModel.findPlanItemDefinition(sourceRef);
                if (planItemDefinition != null) {
                    sourceElement = cmmnModel.findPlanItem(planItemDefinition.getPlanItemRef());
                }
                if (sourceElement == null) {
                    sourceElement = cmmnModel.findTextAnnotation(sourceRef);
                }
            }
        }

        BaseElement targetElement = cmmnModel.getCriterion(targetRef);
        if (targetElement == null) {
            targetElement = cmmnModel.findPlanItem(targetRef);
            if (targetElement == null) {
                PlanItemDefinition planItemDefinition = cmmnModel.findPlanItemDefinition(targetRef);
                if (planItemDefinition != null) {
                    targetElement = cmmnModel.findPlanItem(planItemDefinition.getPlanItemRef());
                }
                if (targetElement == null) {
                    targetElement = cmmnModel.findTextAnnotation(targetRef);
                }
            }
        }

        List<GraphicInfo> graphicInfoList = cmmnModel.getFlowLocationGraphicInfo(association.getId());
        if (graphicInfoList != null && graphicInfoList.size() > 0) {
            graphicInfoList = connectionPerfectionizer(caseDiagramCanvas, cmmnModel, sourceElement, targetElement, graphicInfoList);
            int[] xPoints = new int[graphicInfoList.size()];
            int[] yPoints = new int[graphicInfoList.size()];

            for (int i = 1; i < graphicInfoList.size(); i++) {
                GraphicInfo graphicInfo = graphicInfoList.get(i);
                GraphicInfo previousGraphicInfo = graphicInfoList.get(i - 1);

                if (i == 1) {
                    xPoints[0] = (int) previousGraphicInfo.getX();
                    yPoints[0] = (int) previousGraphicInfo.getY();
                }
                xPoints[i] = (int) graphicInfo.getX();
                yPoints[i] = (int) graphicInfo.getY();

            }

            caseDiagramCanvas.drawAssociation(xPoints, yPoints, scaleFactor);
        }
    }

    protected static List<GraphicInfo> connectionPerfectionizer(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel,
                    BaseElement sourceElement, BaseElement targetElement, List<GraphicInfo> graphicInfoList) {

        GraphicInfo sourceGraphicInfo = cmmnModel.getGraphicInfo(sourceElement.getId());
        GraphicInfo targetGraphicInfo = cmmnModel.getGraphicInfo(targetElement.getId());

        DefaultCaseDiagramCanvas.SHAPE_TYPE sourceShapeType = getShapeType(sourceElement);
        DefaultCaseDiagramCanvas.SHAPE_TYPE targetShapeType = getShapeType(targetElement);

        return caseDiagramCanvas.connectionPerfectionizer(sourceShapeType, targetShapeType, sourceGraphicInfo, targetGraphicInfo, graphicInfoList);
    }

    /**
     * This method returns shape type of base element.<br>
     * Each element can be presented as rectangle, rhombus, or ellipse.
     *
     * @param baseElement
     * @return DefaultCaseDiagramCanvas.SHAPE_TYPE
     */
    protected static DefaultCaseDiagramCanvas.SHAPE_TYPE getShapeType(BaseElement baseElement) {
        if (baseElement instanceof Task || baseElement instanceof Stage) {
            return DefaultCaseDiagramCanvas.SHAPE_TYPE.Rectangle;
        } else if (baseElement instanceof Criterion) {
            return DefaultCaseDiagramCanvas.SHAPE_TYPE.Rhombus;
        } else if (baseElement instanceof EventListener) {
            return DefaultCaseDiagramCanvas.SHAPE_TYPE.Ellipse;
        } else {
            // unknown source element, just do not correct coordinates
        }
        return null;
    }

    protected static GraphicInfo getLineCenter(List<GraphicInfo> graphicInfoList) {
        GraphicInfo gi = new GraphicInfo();

        int[] xPoints = new int[graphicInfoList.size()];
        int[] yPoints = new int[graphicInfoList.size()];

        double length = 0;
        double[] lengths = new double[graphicInfoList.size()];
        lengths[0] = 0;
        double m;
        for (int i = 1; i < graphicInfoList.size(); i++) {
            GraphicInfo graphicInfo = graphicInfoList.get(i);
            GraphicInfo previousGraphicInfo = graphicInfoList.get(i - 1);

            if (i == 1) {
                xPoints[0] = (int) previousGraphicInfo.getX();
                yPoints[0] = (int) previousGraphicInfo.getY();
            }
            xPoints[i] = (int) graphicInfo.getX();
            yPoints[i] = (int) graphicInfo.getY();

            length += Math.sqrt(
                    Math.pow((int) graphicInfo.getX() - (int) previousGraphicInfo.getX(), 2) +
                            Math.pow((int) graphicInfo.getY() - (int) previousGraphicInfo.getY(), 2));
            lengths[i] = length;
        }
        m = length / 2;
        int p1 = 0;
        int p2 = 1;
        for (int i = 1; i < lengths.length; i++) {
            double len = lengths[i];
            p1 = i - 1;
            p2 = i;
            if (len > m) {
                break;
            }
        }

        GraphicInfo graphicInfo1 = graphicInfoList.get(p1);
        GraphicInfo graphicInfo2 = graphicInfoList.get(p2);

        double AB = (int) graphicInfo2.getX() - (int) graphicInfo1.getX();
        double OA = (int) graphicInfo2.getY() - (int) graphicInfo1.getY();
        double OB = lengths[p2] - lengths[p1];
        double ob = m - lengths[p1];
        double ab = AB * ob / OB;
        double oa = OA * ob / OB;

        double mx = graphicInfo1.getX() + ab;
        double my = graphicInfo1.getY() + oa;

        gi.setX(mx);
        gi.setY(my);
        return gi;
    }

    protected static DefaultCaseDiagramCanvas initCaseDiagramCanvas(CmmnModel cmmnModel, String imageType,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader) {

        // We need to calculate maximum values to know how big the image will be in its entirety
        double minX = Double.MAX_VALUE;
        double maxX = 0;
        double minY = Double.MAX_VALUE;
        double maxY = 0;

        for (Case caseModel : cmmnModel.getCases()) {
            Stage stage = caseModel.getPlanModel();
            GraphicInfo stageInfo = cmmnModel.getGraphicInfo(stage.getId());

            // width
            if (stageInfo.getX() + stageInfo.getWidth() > maxX) {
                maxX = stageInfo.getX() + stageInfo.getWidth();
            }
            if (stageInfo.getX() < minX) {
                minX = stageInfo.getX();
            }
            // height
            if (stageInfo.getY() + stageInfo.getHeight() > maxY) {
                maxY = stageInfo.getY() + stageInfo.getHeight();
            }
            if (stageInfo.getY() < minY) {
                minY = stageInfo.getY();
            }
        }

        return new DefaultCaseDiagramCanvas((int) maxX + 10, (int) maxY + 10, (int) minX, (int) minY,
                imageType, activityFontName, labelFontName, annotationFontName, customClassLoader);
    }

    public Map<Class<? extends CmmnElement>, ActivityDrawInstruction> getActivityDrawInstructions() {
        return activityDrawInstructions;
    }

    public void setActivityDrawInstructions(
            Map<Class<? extends CmmnElement>, ActivityDrawInstruction> activityDrawInstructions) {
        this.activityDrawInstructions = activityDrawInstructions;
    }

    protected interface ActivityDrawInstruction {
        void draw(DefaultCaseDiagramCanvas caseDiagramCanvas, CmmnModel cmmnModel, CaseElement caseElement);
    }

}
