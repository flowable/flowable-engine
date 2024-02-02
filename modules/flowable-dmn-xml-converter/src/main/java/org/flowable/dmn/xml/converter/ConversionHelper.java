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
package org.flowable.dmn.xml.converter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DiEdge;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnDiDiagram;
import org.flowable.dmn.model.DmnDiEdge;
import org.flowable.dmn.model.DmnDiShape;

public class ConversionHelper {

    protected DmnDefinition dmnDefinition;
    protected Decision currentDecision;
    protected DmnDiDiagram currentDiDiagram;
    protected DmnDiShape currentDiShape;
    protected DiEdge currentDiEdge;

    protected List<DmnDiDiagram> diDiagrams = new ArrayList<>();
    protected Map<String, List<DmnDiShape>> diShapes = new LinkedHashMap<>();
    protected Map<String, List<DmnDiEdge>> diEdges = new LinkedHashMap<>();

    public DmnDefinition getDmnDefinition() {
        return dmnDefinition;
    }
    public void setDmnDefinition(DmnDefinition dmnDefinition) {
        this.dmnDefinition = dmnDefinition;
    }
    public Decision getCurrentDecision() {
        return currentDecision;
    }
    public void setCurrentDecision(Decision currentDecision) {
        this.currentDecision = currentDecision;
    }

    public void addDiDiagram(DmnDiDiagram diDiagram) {
        diDiagrams.add(diDiagram);
        setCurrentDiDiagram(diDiagram);
    }

    public void addDiShape(DmnDiShape diShape) {
        diShapes.computeIfAbsent(getCurrentDiDiagram().getId(), k -> new ArrayList<>());
        diShapes.get(getCurrentDiDiagram().getId()).add(diShape);
        setCurrentDiShape(diShape);
    }

    public void addDiEdge(DmnDiEdge diEdge) {
        diEdges.computeIfAbsent(getCurrentDiDiagram().getId(), k -> new ArrayList<>());
        diEdges.get(getCurrentDiDiagram().getId()).add(diEdge);
        setCurrentDiEdge(diEdge);
    }

    public DmnDiDiagram getCurrentDiDiagram() {
        return currentDiDiagram;
    }
    public void setCurrentDiDiagram(DmnDiDiagram currentDiDiagram) {
        this.currentDiDiagram = currentDiDiagram;
    }
    public DmnDiShape getCurrentDiShape() {
        return currentDiShape;
    }

    public void setCurrentDiShape(DmnDiShape currentDiShape) {
        this.currentDiShape = currentDiShape;
    }

    public DiEdge getCurrentDiEdge() {
        return currentDiEdge;
    }

    public void setCurrentDiEdge(DiEdge currentDiEdge) {
        this.currentDiEdge = currentDiEdge;
    }

    public List<DmnDiDiagram> getDiDiagrams() {
        return diDiagrams;
    }
    public List<DmnDiShape> getDiShapes(String diagramId) {
        return diShapes.get(diagramId);
    }
    public List<DmnDiEdge> getDiEdges(String diagramId) {
        return diEdges.get(diagramId);
    }

}
