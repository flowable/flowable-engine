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
package org.flowable.engine.impl.bpmn.data;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * A simple data input association between a source and a target with assignments
 * 
 * @author Esteban Robles Luna
 */
public class SimpleDataInputAssociation extends AbstractDataAssociation {

    private static final long serialVersionUID = 1L;

    protected List<Assignment> assignments = new ArrayList<>();

    public SimpleDataInputAssociation(Expression sourceExpression, String target) {
        super(sourceExpression, target);
    }

    public SimpleDataInputAssociation(String source, String target) {
        super(source, target);
    }

    public void addAssignment(Assignment assignment) {
        this.assignments.add(assignment);
    }

    @Override
    public void evaluate(DelegateExecution execution) {
        for (Assignment assignment : this.assignments) {
            assignment.evaluate(execution);
        }
    }
}
