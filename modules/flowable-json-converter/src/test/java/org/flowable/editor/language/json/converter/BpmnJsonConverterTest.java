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
package org.flowable.editor.language.json.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;

/**
 * This class tests {@link BpmnJsonConverter} implementation
 */
public class BpmnJsonConverterTest {

    private static final double SMALL_DELTA = 0.000001;
    private static final double PRECISION = 0.0002;

    @Test
    public void testLineCircleIntersections() {
        // Arrange
        Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO, 3);
        line.moveTo(1, 10);
        line.lineTo(20 - 1, 10);
        line.lineTo(20 - 1 + SMALL_DELTA, 10 + SMALL_DELTA);
        line.closePath();
        Ellipse2D.Double circle = new Ellipse2D.Double(4, 8, 4, 4);

        // Act
        Area intersectionArea = new Area(line);
        intersectionArea.intersect(new Area(circle));

        // Assert
        assertThat(intersectionArea.isEmpty()).isFalse();
        Rectangle2D bounds2D = intersectionArea.getBounds2D();
        assertThat(bounds2D.getX()).isCloseTo(4d, offset(PRECISION));
        assertThat(bounds2D.getY()).isCloseTo(10d, offset(PRECISION));
        assertThat(bounds2D.getX() + bounds2D.getWidth()).isCloseTo(8d, offset(PRECISION));
        assertThat(bounds2D.getY() + bounds2D.getHeight()).isCloseTo(10d, offset(PRECISION));
    }

    @Test
    public void testLineRectangleIntersections() {
        // Arrange
        Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO, 3);
        line.moveTo(1, 10);
        line.lineTo(20 - 1, 10);
        line.lineTo(20 - 1 + SMALL_DELTA, 10 + SMALL_DELTA);
        line.closePath();
        Rectangle2D.Double rectangle = new Rectangle2D.Double(4, 8, 4, 4);

        // Act
        Area intersectionArea = new Area(line);
        intersectionArea.intersect(new Area(rectangle));

        // Assert
        assertThat(intersectionArea.isEmpty()).isFalse();
        Rectangle2D bounds2D = intersectionArea.getBounds2D();
        assertThat(bounds2D.getX()).isCloseTo(4d, offset(PRECISION));
        assertThat(bounds2D.getY()).isCloseTo(10d, offset(PRECISION));
        assertThat(bounds2D.getX() + bounds2D.getWidth()).isCloseTo(8d, offset(PRECISION));
        assertThat(bounds2D.getY() + bounds2D.getHeight()).isCloseTo(10d, offset(PRECISION));
    }
}
