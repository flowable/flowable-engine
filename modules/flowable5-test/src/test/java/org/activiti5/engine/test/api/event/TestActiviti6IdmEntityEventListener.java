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
package org.activiti5.engine.test.api.event;

import java.util.ArrayList;
import java.util.List;

import org.activiti.idm.api.event.ActivitiIdmEntityEvent;
import org.activiti.idm.api.event.ActivitiIdmEvent;
import org.activiti.idm.api.event.ActivitiIdmEventListener;

public class TestActiviti6IdmEntityEventListener implements ActivitiIdmEventListener {

	private List<ActivitiIdmEvent> eventsReceived;
	private Class<?> entityClass;
	
	public TestActiviti6IdmEntityEventListener(Class<?> entityClass) {
		this.entityClass = entityClass;
		
		eventsReceived = new ArrayList<ActivitiIdmEvent>();
  }
	
	public List<ActivitiIdmEvent> getEventsReceived() {
	  return eventsReceived;
  }
	
	public void clearEventsReceived() {
		eventsReceived.clear();
	}
	
	@Override
	public void onEvent(ActivitiIdmEvent event) {
		if (event instanceof ActivitiIdmEntityEvent && entityClass.isAssignableFrom(((ActivitiIdmEntityEvent) event).getEntity().getClass())) {
			eventsReceived.add(event);
		}
	}

	@Override
	public boolean isFailOnException() {
		return true;
	}

}
