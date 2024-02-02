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
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.common.engine.impl.util.io.InputStreamSource;

public abstract class AbstractConverterTest implements BpmnXMLConstants {

    protected BpmnModel readXMLFile() throws Exception {
        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream(getResource());
        return readXMLFile(xmlStream);
    }
    
    protected BpmnModel readXMLFile(InputStream inputStream) throws Exception {
        return new BpmnXMLConverter().convertToBpmnModel(new InputStreamSource(inputStream), true, false, "UTF-8");
    }

    protected BpmnModel exportAndReadXMLFile(BpmnModel bpmnModel) throws Exception {
        byte[] xml = new BpmnXMLConverter().convertToXML(bpmnModel);
        return new BpmnXMLConverter().convertToBpmnModel(new InputStreamSource(new ByteArrayInputStream(xml)), true, false, "UTF-8");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	protected Map<String, Map> parseBPMNDI(byte[] xml) throws Exception {
    	Map<String, Map> bpmnDIMap = new HashMap<>();
    	Map<String, List<GraphicInfo>> edgesMap = null;
    	Map<String, GraphicInfo> shapesMap = null;

        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(xml), StandardCharsets.UTF_8);
        XMLStreamReader xtr = xif.createXMLStreamReader(in);

    	String diagramId = null;
    	while (xtr.hasNext() && !(xtr.isEndElement() && xtr.getLocalName().equalsIgnoreCase(ELEMENT_DEFINITIONS))) {
    		if (xtr.isStartElement() && xtr.getLocalName().equalsIgnoreCase(ELEMENT_DI_PLANE)) {
    			diagramId = xtr.getAttributeValue(null, ATTRIBUTE_DI_BPMNELEMENT);
    			
    			// retrieve diagram map
    			Map<String, Map> diagramMap = bpmnDIMap.get(diagramId);

    			// initialize diagram map if it doesn't exist yet
    			if (null == diagramMap) {
    				diagramMap = new HashMap<>();
    			}

    			// store shapes and edges for current diagram
    			while (xtr.hasNext() && !(xtr.isEndElement() && xtr.getLocalName().equalsIgnoreCase(ELEMENT_DI_PLANE))) {
    				xtr.next();
    				if (xtr.isStartElement()) {
    					String elementId = xtr.getAttributeValue(null, ATTRIBUTE_DI_BPMNELEMENT);
    					if (xtr.getLocalName().equalsIgnoreCase(ELEMENT_DI_SHAPE)) {
    						// retrieve shapes map
    						shapesMap = diagramMap.get(ELEMENT_DI_SHAPE);

    						// initialize shapes map if it doesn't exist yet
    						if (null == shapesMap) {
    							shapesMap = new HashMap<>();
    						}

    						// check for shape bounds
    						while (xtr.hasNext() && !(xtr.isEndElement() && xtr.getLocalName().equalsIgnoreCase(ELEMENT_DI_SHAPE))) {
    							if (!xtr.isCharacters() && xtr.isStartElement() && xtr.getLocalName().equalsIgnoreCase(ELEMENT_DI_BOUNDS)) {
    								// save shape info
    								GraphicInfo gInfo = new GraphicInfo();
    								gInfo.setX(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_X)));
    								gInfo.setY(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_Y)));
    								gInfo.setWidth(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_WIDTH)));
    								gInfo.setHeight(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_HEIGHT)));

    								shapesMap.put(elementId, gInfo);
    							}
    							xtr.next();
    						}
							// save the first time through
    						if (!diagramMap.containsKey(ELEMENT_DI_SHAPE)) {
    							diagramMap.put(ELEMENT_DI_SHAPE, shapesMap);
    						}
    						
    					} else if (xtr.getLocalName().equalsIgnoreCase(ELEMENT_DI_EDGE)) {
    						// retrieve edges map
    						edgesMap = diagramMap.get(ELEMENT_DI_EDGE);
    						List<GraphicInfo> waypointList = new ArrayList<>();

    						// initialize shapes map if it doesn't exist yet
    						if (null == edgesMap) {
    							edgesMap = new HashMap<>();
    						}

    						// check for edge waypoints
    						while (xtr.hasNext() && !(xtr.isEndElement() && xtr.getLocalName().equalsIgnoreCase(ELEMENT_DI_EDGE))) {
    							if (!xtr.isCharacters() && xtr.isStartElement() && xtr.getLocalName().equalsIgnoreCase(ELEMENT_DI_WAYPOINT)) {
    								// save waypoint info
    								GraphicInfo gInfo = new GraphicInfo();
    								gInfo.setX(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_X)));
    								gInfo.setY(Double.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_DI_Y)));

    								// save waypoint
    								waypointList.add(gInfo);
    							}
								xtr.next();
    						}
    						edgesMap.put(elementId, waypointList);

    						// save the first time through
    						if (!diagramMap.containsKey(ELEMENT_DI_EDGE)) {
    							diagramMap.put(ELEMENT_DI_EDGE, edgesMap);
    						}
    					}
    				}
    			}
    			bpmnDIMap.put(diagramId, diagramMap);
    		}
    		xtr.next();
    	}

        return bpmnDIMap;
    }
    
    protected void compareCollections(List<GraphicInfo> info, List<GraphicInfo> diInfo) {
		boolean foundMatch = false;
		for (GraphicInfo ginfo : info) {
			for (GraphicInfo gdInfo : diInfo) {
				// entries may not be in the same order
				if (foundMatch) {
					// found one match so reset and try next set of values
					info.remove(ginfo);
					foundMatch = false;
					continue;

				} else {
					assertThat(ginfo.equals(gdInfo)).isTrue();
					foundMatch = true;
				}
			}
		}
    }

    protected abstract String getResource();
}
