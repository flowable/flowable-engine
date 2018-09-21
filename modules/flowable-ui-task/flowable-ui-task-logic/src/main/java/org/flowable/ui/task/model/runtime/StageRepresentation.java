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
package org.flowable.ui.task.model.runtime;

import java.util.Date;

import org.flowable.ui.common.model.AbstractRepresentation;

public class StageRepresentation extends AbstractRepresentation {

    protected String name;
    protected String state;
    protected Date startedDate;
    protected Date endedDate;

    public StageRepresentation(String name, String state, Date startedDate, Date endedDate) {
        this.name = name;
        this.state = state;
        this.startedDate = startedDate;
        this.endedDate = endedDate;
    }

    public String getName() {
        return name;
    }
    public String getState() {
        return state;
    }
    public Date getStartedDate() {
        return startedDate;
    }
    public Date getEndedDate() {
        return endedDate;
    }
}
