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

package org.flowable.dmn.image.impl;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.image.DecisionRequirementsDiagramGenerator;
import org.flowable.dmn.image.exception.FlowableImageException;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.InformationRequirement;
import org.flowable.dmn.model.NamedElement;
import org.flowable.dmn.model.DmnElementReference;
import org.flowable.dmn.model.GraphicInfo;

/**
 * Class to generate an image based the diagram interchange information in a DMN 1.2 definition.
 *
 * @author Yvo Swillens
 */
public class DefaultDecisionRequirementsDiagramGenerator implements DecisionRequirementsDiagramGenerator {

    protected Map<Class<? extends NamedElement>, ActivityDrawInstruction> elementDrawInstructions = new HashMap<>();

    public DefaultDecisionRequirementsDiagramGenerator() {
        this(1.0);
    }

    // The instructions on how to draw a certain construct is
    // created statically and stored in a map for performance.
    public DefaultDecisionRequirementsDiagramGenerator(final double scaleFactor) {
        elementDrawInstructions.put(Decision.class, new ActivityDrawInstruction() {

            @Override
            public void draw(DefaultDecisionRequirementsDiagramCanvas decisionRequirementsDiagramCanvas, DmnDefinition dmnDefinition,
                    NamedElement NamedElement) {
                GraphicInfo graphicInfo = dmnDefinition.getGraphicInfo(NamedElement.getId());
                decisionRequirementsDiagramCanvas.drawDecision(NamedElement.getName(), graphicInfo, scaleFactor);
            }
        });
    }

    @Override
    public InputStream generateDiagram(DmnDefinition dmnDefinition, String imageType,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader, double scaleFactor) {

        return generateDecisionRequirementsDiagram(dmnDefinition, imageType, activityFontName, labelFontName, annotationFontName, customClassLoader,
                scaleFactor).generateImage(imageType);
    }

    @Override
    public InputStream generateDiagram(DmnDefinition dmnDefinition, String imageType) {
        return generateDiagram(dmnDefinition, imageType, null, null, null, null, 1.0);
    }

    @Override
    public InputStream generateDiagram(DmnDefinition dmnDefinition, String imageType, double scaleFactor) {
        return generateDiagram(dmnDefinition, imageType, null, null, null, null, scaleFactor);
    }

    @Override
    public InputStream generateDiagram(DmnDefinition dmnDefinition, String imageType, String activityFontName,
            String labelFontName, String annotationFontName, ClassLoader customClassLoader) {

        return generateDiagram(dmnDefinition, imageType, activityFontName, labelFontName, annotationFontName, customClassLoader, 1.0);
    }

    @Override
    public InputStream generatePngDiagram(DmnDefinition dmnDefinition) {
        return generatePngDiagram(dmnDefinition, 1.0);
    }

    @Override
    public InputStream generatePngDiagram(DmnDefinition dmnDefinition, double scaleFactor) {
        return generateDiagram(dmnDefinition, "png", scaleFactor);
    }

    @Override
    public InputStream generateJpgDiagram(DmnDefinition dmnDefinition) {
        return generateJpgDiagram(dmnDefinition, 1.0);
    }

    @Override
    public InputStream generateJpgDiagram(DmnDefinition dmnDefinition, double scaleFactor) {
        return generateDiagram(dmnDefinition, "jpg", scaleFactor);
    }

    public BufferedImage generateImage(DmnDefinition dmnDefinition, String imageType,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader, double scaleFactor) {

        return generateDecisionRequirementsDiagram(dmnDefinition, imageType, activityFontName, labelFontName, annotationFontName,
                customClassLoader, scaleFactor).generateBufferedImage(imageType);
    }

    public BufferedImage generateImage(DmnDefinition dmnDefinition, String imageType, double scaleFactor) {
        return generateImage(dmnDefinition, imageType, null, null, null, null, scaleFactor);
    }

    @Override
    public BufferedImage generatePngImage(DmnDefinition dmnDefinition, double scaleFactor) {
        return generateImage(dmnDefinition, "png", scaleFactor);
    }

    protected DefaultDecisionRequirementsDiagramCanvas generateDecisionRequirementsDiagram(DmnDefinition dmnDefinition, String imageType,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader, double scaleFactor) {

        prepareDmnDefinition(dmnDefinition);

        DefaultDecisionRequirementsDiagramCanvas decisionRequirementsDiagramCanvas = initDecisionRequirementsDiagramCanvas(dmnDefinition, imageType,
                activityFontName, labelFontName, annotationFontName, customClassLoader);

        // Draw elements
        for (DecisionService decisionService : dmnDefinition.getDecisionServices()) {

            GraphicInfo decisionServiceInfo = dmnDefinition.getGraphicInfo(decisionService.getId());

            if (decisionServiceInfo == null) {
                throw new FlowableImageException("Could not find graphic info for decision service: " + decisionService.getId());
            }

            List<GraphicInfo> decisionServiceDividerInfos = dmnDefinition.getDecisionServiceDividerGraphicInfo(decisionService.getId());
            decisionRequirementsDiagramCanvas.drawDecisionService(decisionService.getName(), decisionServiceInfo, decisionServiceDividerInfos, scaleFactor);

            for (DmnElementReference decisionRef : decisionService.getOutputDecisions()) {
                Decision decision = dmnDefinition.getDecisionById(decisionRef.getParsedId());

                // draw decisions
                drawDecision(decisionRequirementsDiagramCanvas, dmnDefinition, decision, scaleFactor);

                // draw information requirements
                for (InformationRequirement informationRequirement : decision.getRequiredDecisions()) {
                    drawInformationRequirement(decisionRequirementsDiagramCanvas, dmnDefinition, informationRequirement, decision, scaleFactor);
                }
            }

            for (DmnElementReference decisionRef : decisionService.getEncapsulatedDecisions()) {
                Decision decision = dmnDefinition.getDecisionById(decisionRef.getParsedId());
                drawDecision(decisionRequirementsDiagramCanvas, dmnDefinition, decision, scaleFactor);

                // draw information requirements
                for (InformationRequirement informationRequirement : decision.getRequiredDecisions()) {
                    drawInformationRequirement(decisionRequirementsDiagramCanvas, dmnDefinition, informationRequirement, decision, scaleFactor);
                }
            }
        }

        return decisionRequirementsDiagramCanvas;
    }

    protected void prepareDmnDefinition(DmnDefinition dmnDefinition) {
        // Need to make sure all elements have positive x and y.
        // Check all graphicInfo and update the elements accordingly

        List<GraphicInfo> allGraphicInfos = new ArrayList<>();
        if (dmnDefinition.getLocationMap() != null) {
            allGraphicInfos.addAll(dmnDefinition.getLocationMap().values());
        }
        if (dmnDefinition.getFlowLocationMap() != null) {
            for (List<GraphicInfo> flowGraphicInfos : dmnDefinition.getFlowLocationMap().values()) {
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

    protected void drawDecision(DefaultDecisionRequirementsDiagramCanvas decisionRequirementsDiagramCanvas, DmnDefinition dmnDefinition, Decision decision,
            double scaleFactor) {

        ActivityDrawInstruction drawInstruction = elementDrawInstructions.get(decision.getClass());
        if (drawInstruction != null) {
            drawInstruction.draw(decisionRequirementsDiagramCanvas, dmnDefinition, decision);
        }
    }

    protected void drawInformationRequirement(DefaultDecisionRequirementsDiagramCanvas decisionRequirementsDiagramCanvas, DmnDefinition dmnDefinition,
            InformationRequirement informationRequirement, Decision targetDecision, double scaleFactor) {

        Decision sourceDecision = dmnDefinition.getDecisionById(informationRequirement.getRequiredDecision().getParsedId());
        List<GraphicInfo> graphicInfoList = dmnDefinition.getFlowLocationGraphicInfo(informationRequirement.getId());

        if (graphicInfoList != null && graphicInfoList.size() > 0) {
            graphicInfoList = connectionPerfectionizer(decisionRequirementsDiagramCanvas, dmnDefinition, sourceDecision, targetDecision, graphicInfoList);
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

            decisionRequirementsDiagramCanvas.drawInformationRequirement(xPoints, yPoints, scaleFactor);
        }
    }

    protected static List<GraphicInfo> connectionPerfectionizer(DefaultDecisionRequirementsDiagramCanvas processDiagramCanvas, DmnDefinition dmnDefinition,
            DmnElement sourceElement, DmnElement targetElement, List<GraphicInfo> graphicInfoList) {

        GraphicInfo sourceGraphicInfo = dmnDefinition.getGraphicInfo(sourceElement.getId());
        GraphicInfo targetGraphicInfo = dmnDefinition.getGraphicInfo(targetElement.getId());

        DefaultDecisionRequirementsDiagramCanvas.SHAPE_TYPE sourceShapeType = getShapeType(sourceElement);
        DefaultDecisionRequirementsDiagramCanvas.SHAPE_TYPE targetShapeType = getShapeType(targetElement);

        return processDiagramCanvas.connectionPerfectionizer(sourceShapeType, targetShapeType, sourceGraphicInfo, targetGraphicInfo, graphicInfoList);
    }

    /**
     * This method returns shape type of base element.<br>
     * Each element can be presented as rectangle, rhombus, or ellipse.
     *
     * @param baseElement
     * @return DefaultProcessDiagramCanvas.SHAPE_TYPE
     */
    protected static DefaultDecisionRequirementsDiagramCanvas.SHAPE_TYPE getShapeType(DmnElement baseElement) {
        if (baseElement instanceof Decision) {
            return DefaultDecisionRequirementsDiagramCanvas.SHAPE_TYPE.Rectangle;
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

    protected static DefaultDecisionRequirementsDiagramCanvas initDecisionRequirementsDiagramCanvas(DmnDefinition dmnDefinition, String imageType,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader) {

        // We need to calculate maximum values to know how big the image will be in its entirety
        double minX = Double.MAX_VALUE;
        double maxX = 0;
        double minY = Double.MAX_VALUE;
        double maxY = 0;

        for (DecisionService decisionService : dmnDefinition.getDecisionServices()) {
            GraphicInfo decisionServiceInfo = dmnDefinition.getGraphicInfo(decisionService.getId());

            if (decisionServiceInfo == null) {
                throw new FlowableImageException("Could not find graphic info for decision service: " + decisionService.getId());
            }

            // width
            if (decisionServiceInfo.getX() + decisionServiceInfo.getWidth() > maxX) {
                maxX = decisionServiceInfo.getX() + decisionServiceInfo.getWidth();
            }
            if (decisionServiceInfo.getX() < minX) {
                minX = decisionServiceInfo.getX();
            }
            // height
            if (decisionServiceInfo.getY() + decisionServiceInfo.getHeight() > maxY) {
                maxY = decisionServiceInfo.getY() + decisionServiceInfo.getHeight();
            }
            if (decisionServiceInfo.getY() < minY) {
                minY = decisionServiceInfo.getY();
            }
        }

        return new DefaultDecisionRequirementsDiagramCanvas((int) maxX + 10, (int) maxY + 10, (int) minX, (int) minY,
                imageType, activityFontName, labelFontName, annotationFontName, customClassLoader);
    }

    public Map<Class<? extends NamedElement>, ActivityDrawInstruction> getElementDrawInstructions() {
        return elementDrawInstructions;
    }

    public void setElementDrawInstructions(
            Map<Class<? extends NamedElement>, ActivityDrawInstruction> elementDrawInstructions) {
        this.elementDrawInstructions = elementDrawInstructions;
    }

    protected interface ActivityDrawInstruction {

        void draw(DefaultDecisionRequirementsDiagramCanvas decisionRequirementsDiagramCanvas, DmnDefinition dmnDefinition, NamedElement NamedElement);
    }

}
