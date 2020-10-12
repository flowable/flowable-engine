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
package org.flowable.dmn.image;

import org.flowable.dmn.model.DmnDefinition;

import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * This interface declares methods to generate decision requirements diagram
 * 
 * @author Yvo Swilens
 */
public interface DecisionRequirementsDiagramGenerator {

    /**
     * Generates a diagram of the given decision definition, using the diagram interchange information of the decision.
     * 
     * @param dmnDefinition
     *            dmn model to get diagram for
     * @param imageType
     *            type of the image to generate.
     * @param activityFontName
     *            override the default activity font
     * @param labelFontName
     *            override the default label font
     * @param customClassLoader
     *            provide a custom classloader for retrieving icon images
     */
    InputStream generateDiagram(DmnDefinition dmnDefinition, String imageType, String activityFontName, String labelFontName,
                                String annotationFontName, ClassLoader customClassLoader, double scaleFactor);

    /**
     * Generates a diagram of the given process definition, using the diagram interchange information of the process.
     * 
     * @param dmnDefinition
     *            dmn model to get diagram for
     * @param imageType
     *            type of the image to generate.
     */
    InputStream generateDiagram(DmnDefinition dmnDefinition, String imageType);

    InputStream generateDiagram(DmnDefinition dmnDefinition, String imageType, double scaleFactor);

    InputStream generateDiagram(DmnDefinition dmnDefinition, String imageType, String activityFontName, String labelFontName,
                                String annotationFontName, ClassLoader customClassLoader);

    InputStream generatePngDiagram(DmnDefinition dmnDefinition);

    InputStream generatePngDiagram(DmnDefinition dmnDefinition, double scaleFactor);

    InputStream generateJpgDiagram(DmnDefinition dmnDefinition);

    InputStream generateJpgDiagram(DmnDefinition dmnDefinition, double scaleFactor);

    BufferedImage generatePngImage(DmnDefinition dmnDefinition, double scaleFactor);

}
