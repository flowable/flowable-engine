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
package org.flowable.dmn.model;

public class DmnDiShape extends DmnDiDiagramElement {

    protected String dmnElementRef;
    protected GraphicInfo graphicInfo;
    protected DmnDiDecisionServiceDividerLine decisionServiceDividerLine;

    public String getDmnElementRef() {
        return dmnElementRef;
    }
    public void setDmnElementRef(String dmnElementRef) {
        this.dmnElementRef = dmnElementRef;
    }
    public GraphicInfo getGraphicInfo() {
        return graphicInfo;
    }
    public void setGraphicInfo(GraphicInfo graphicInfo) {
        this.graphicInfo = graphicInfo;
    }
    public DmnDiDecisionServiceDividerLine getDecisionServiceDividerLine() {
        return decisionServiceDividerLine;
    }
    public void setDecisionServiceDividerLine(DmnDiDecisionServiceDividerLine decisionServiceDividerLine) {
        this.decisionServiceDividerLine = decisionServiceDividerLine;
    }
}
