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
package org.flowable.bpmn.model;

/**
 * @author Tijs Rademakers
 */
public class GraphicInfo {

    protected double x;
    protected double y;
    protected double height;
    protected double width;
    protected BaseElement element;
    protected Boolean expanded;
    protected int xmlRowNumber;
    protected int xmlColumnNumber;
    protected double rotation;
    
    public GraphicInfo() {}

    public GraphicInfo(double x, double y, double height, double width) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public GraphicInfo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public Boolean getExpanded() {
        return expanded;
    }

    public void setExpanded(Boolean expanded) {
        this.expanded = expanded;
    }

    public BaseElement getElement() {
        return element;
    }

    public void setElement(BaseElement element) {
        this.element = element;
    }

    public int getXmlRowNumber() {
        return xmlRowNumber;
    }

    public void setXmlRowNumber(int xmlRowNumber) {
        this.xmlRowNumber = xmlRowNumber;
    }

    public int getXmlColumnNumber() {
        return xmlColumnNumber;
    }

    public void setXmlColumnNumber(int xmlColumnNumber) {
        this.xmlColumnNumber = xmlColumnNumber;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public boolean equals(GraphicInfo ginfo) {
    	if (this.getX() != ginfo.getX()) {
    		return false;
    	}
    	if (this.getY() != ginfo.getY()) {
    		return false;
    	}
    	if (this.getHeight() != ginfo.getHeight()) {
    		return false;
    	}
    	if (this.getWidth() != ginfo.getWidth()) {
    		return false;
    	}
        if (this.getRotation() != ginfo.getRotation()) {
            return false;
        }

    	// check for zero value in case we are comparing model value to BPMN DI value
    	// model values do not have xml location information
    	if (0 != this.getXmlColumnNumber() && 0 != ginfo.getXmlColumnNumber() && this.getXmlColumnNumber() != ginfo.getXmlColumnNumber()) {
    		return false;
    	}
    	if (0 != this.getXmlRowNumber() && 0 != ginfo.getXmlRowNumber() && this.getXmlRowNumber() != ginfo.getXmlRowNumber()) {
    		return false;
    	}

    	// only check for elements that support this value
    	if (null != this.getExpanded() && null != ginfo.getExpanded() && !this.getExpanded().equals(ginfo.getExpanded())) {
    		return false;
    	}
    	return true;
    }
}
