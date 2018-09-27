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
package org.flowable.image;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;

/**
 * This interface declares methods to generate process diagram
 * 
 * @author martin.grofcik
 * @author Tijs Rademakers
 */
public interface ProcessDiagramGenerator {

    /**
     * Generates a diagram of the given process definition, using the diagram interchange information of the process.
     * 
     * @param bpmnModel
     *            bpmn model to get diagram for
     * @param imageType
     *            type of the image to generate.
     * @param highLightedActivities
     *            activities to highlight
     * @param highLightedFlows
     *            flows to highlight
     * @param activityFontName
     *            override the default activity font
     * @param labelFontName
     *            override the default label font
     * @param customClassLoader
     *            provide a custom classloader for retrieving icon images
     * @param drawSequenceFlowNameWithNoLabelDI
     *            provide a option to also include the sequence flow name in case there's no Label DI
     */
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities, List<String> highLightedFlows,
            String activityFontName, String labelFontName, String annotationFontName, ClassLoader customClassLoader, double scaleFactor,boolean drawSequenceFlowNameWithNoLabelDI);

    /**
     * Generates a diagram of the given process definition, using the diagram interchange information of the process.
     * 
     * @param bpmnModel
     *            bpmn model to get diagram for
     * @param imageType
     *            type of the image to generate.
     * @param highLightedActivities
     *            activities to highlight
     * @param highLightedFlows
     *            flows to highlight
     * @param drawSequenceFlowNameWithNoLabelDI
     *            provide a option to also include the sequence flow name in case there's no Label DI
     */
    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities, List<String> highLightedFlows,boolean drawSequenceFlowNameWithNoLabelDI);

    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities, List<String> highLightedFlows, double scaleFactor,boolean drawSequenceFlowNameWithNoLabelDI);

    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities,boolean drawSequenceFlowNameWithNoLabelDI);

    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, List<String> highLightedActivities, double scaleFactor,boolean drawSequenceFlowNameWithNoLabelDI);

    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, String activityFontName, String labelFontName,
            String annotationFontName, ClassLoader customClassLoader,boolean drawSequenceFlowNameWithNoLabelDI);

    public InputStream generateDiagram(BpmnModel bpmnModel, String imageType, String activityFontName, String labelFontName,
            String annotationFontName, ClassLoader customClassLoader, double scaleFactor,boolean drawSequenceFlowNameWithNoLabelDI);

    public InputStream generatePngDiagram(BpmnModel bpmnModel,boolean drawSequenceFlowNameWithNoLabelDI);

    public InputStream generatePngDiagram(BpmnModel bpmnModel, double scaleFactor,boolean drawSequenceFlowNameWithNoLabelDI);

    public InputStream generateJpgDiagram(BpmnModel bpmnModel);

    public InputStream generateJpgDiagram(BpmnModel bpmnModel, double scaleFactor,boolean drawSequenceFlowNameWithNoLabelDI);

    public BufferedImage generatePngImage(BpmnModel bpmnModel, double scaleFactor);

}
