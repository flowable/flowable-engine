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
package org.flowable.cmmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tijs Rademakers
 */
public class CmmnDiEdge extends BaseElement {

    protected String cmmnElementRef;
    protected String targetCmmnElementRef;
    protected GraphicInfo sourceDockerInfo;
    protected GraphicInfo targetDockerInfo;
    protected List<GraphicInfo> waypoints = new ArrayList<>();
    
    public String getCmmnElementRef() {
        return cmmnElementRef;
    }
    
    public void setCmmnElementRef(String cmmnElementRef) {
        this.cmmnElementRef = cmmnElementRef;
    }

    public String getTargetCmmnElementRef() {
        return targetCmmnElementRef;
    }

    public void setTargetCmmnElementRef(String targetCmmnElementRef) {
        this.targetCmmnElementRef = targetCmmnElementRef;
    }
    
    public GraphicInfo getSourceDockerInfo() {
        return sourceDockerInfo;
    }

    public void setSourceDockerInfo(GraphicInfo sourceDockerInfo) {
        this.sourceDockerInfo = sourceDockerInfo;
    }

    public GraphicInfo getTargetDockerInfo() {
        return targetDockerInfo;
    }

    public void setTargetDockerInfo(GraphicInfo targetDockerInfo) {
        this.targetDockerInfo = targetDockerInfo;
    }

    public void addWaypoint(GraphicInfo graphicInfo) {
        this.waypoints.add(graphicInfo);
    }

    public List<GraphicInfo> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<GraphicInfo> waypoints) {
        this.waypoints = waypoints;
    }
}
