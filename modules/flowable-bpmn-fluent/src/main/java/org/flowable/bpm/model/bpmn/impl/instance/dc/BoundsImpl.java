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
package org.flowable.bpm.model.bpmn.impl.instance.dc;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DC_ATTRIBUTE_HEIGHT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DC_ATTRIBUTE_WIDTH;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DC_ATTRIBUTE_X;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DC_ATTRIBUTE_Y;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DC_ELEMENT_BOUNDS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DC_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.dc.Bounds;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The DC bounds element.
 */
public class BoundsImpl
        extends BpmnModelElementInstanceImpl
        implements Bounds {

    protected static Attribute<Double> xAttribute;
    protected static Attribute<Double> yAttribute;
    protected static Attribute<Double> widthAttribute;
    protected static Attribute<Double> heightAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Bounds.class, DC_ELEMENT_BOUNDS)
                .namespaceUri(DC_NS)
                .instanceProvider(new ModelTypeInstanceProvider<Bounds>() {
                    public Bounds newInstance(ModelTypeInstanceContext instanceContext) {
                        return new BoundsImpl(instanceContext);
                    }
                });

        xAttribute = typeBuilder.doubleAttribute(DC_ATTRIBUTE_X)
                .required()
                .build();

        yAttribute = typeBuilder.doubleAttribute(DC_ATTRIBUTE_Y)
                .required()
                .build();

        widthAttribute = typeBuilder.doubleAttribute(DC_ATTRIBUTE_WIDTH)
                .required()
                .build();

        heightAttribute = typeBuilder.doubleAttribute(DC_ATTRIBUTE_HEIGHT)
                .required()
                .build();

        typeBuilder.build();
    }

    public BoundsImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public Double getX() {
        return xAttribute.getValue(this);
    }

    public void setX(double x) {
        xAttribute.setValue(this, x);
    }

    public Double getY() {
        return yAttribute.getValue(this);
    }

    public void setY(double y) {
        yAttribute.setValue(this, y);
    }

    public Double getWidth() {
        return widthAttribute.getValue(this);
    }

    public void setWidth(double width) {
        widthAttribute.setValue(this, width);
    }

    public Double getHeight() {
        return heightAttribute.getValue(this);
    }

    public void setHeight(double height) {
        heightAttribute.setValue(this, height);
    }
}
