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
package org.flowable.cmmn.image;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.flowable.cmmn.model.CmmnModel;

/**
 * This interface declares methods to generate case diagram
 * 
 * @author Tijs Rademakers
 */
public interface CaseDiagramGenerator {

    /**
     * Generates a diagram of the given case definition, using the diagram interchange information of the case.
     * 
     * @param cmmnModel
     *            cmmn model to get diagram for
     * @param imageType
     *            type of the image to generate.
     * @param activityFontName
     *            override the default activity font
     * @param labelFontName
     *            override the default label font
     * @param customClassLoader
     *            provide a custom classloader for retrieving icon images
     */
    public InputStream generateDiagram(CmmnModel cmmnModel, String imageType, String activityFontName, String labelFontName, 
                    String annotationFontName, ClassLoader customClassLoader, double scaleFactor);

    /**
     * Generates a diagram of the given process definition, using the diagram interchange information of the process.
     * 
     * @param cmmnModel
     *            cmmn model to get diagram for
     * @param imageType
     *            type of the image to generate.
     */
    public InputStream generateDiagram(CmmnModel cmmnModel, String imageType);

    public InputStream generateDiagram(CmmnModel cmmnModel, String imageType, double scaleFactor);

    public InputStream generateDiagram(CmmnModel cmmnModel, String imageType, String activityFontName, String labelFontName,
            String annotationFontName, ClassLoader customClassLoader);

    public InputStream generatePngDiagram(CmmnModel cmmnModel);

    public InputStream generatePngDiagram(CmmnModel cmmnModel, double scaleFactor);

    public InputStream generateJpgDiagram(CmmnModel cmmnModel);

    public InputStream generateJpgDiagram(CmmnModel cmmnModel, double scaleFactor);

    public BufferedImage generatePngImage(CmmnModel cmmnModel, double scaleFactor);

}
