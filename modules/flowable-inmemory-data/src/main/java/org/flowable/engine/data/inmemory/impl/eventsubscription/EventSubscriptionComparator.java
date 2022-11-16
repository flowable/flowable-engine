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
package org.flowable.engine.data.inmemory.impl.eventsubscription;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.common.engine.impl.db.ListQueryParameterObject.OrderBy;
import org.flowable.engine.data.inmemory.AbstractEntityComparator;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryProperty;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class EventSubscriptionComparator extends AbstractEntityComparator implements Comparator<EventSubscriptionEntity> {

    private static final ConcurrentHashMap<String, EventSubscriptionComparator> comparators = new ConcurrentHashMap<>();

    private final List<OrderBy> order;

    private EventSubscriptionComparator(List<OrderBy> order) {
        this.order = order;
    }

    @Override
    public int compare(EventSubscriptionEntity e1, EventSubscriptionEntity e2) {

        int i = 0;
        for (OrderBy ob : order) {
            if (ob.getColumnName().equals(EventSubscriptionQueryProperty.ID.getName())) {
                i = compareString(e1.getId(), e2.getId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(EventSubscriptionQueryProperty.CREATED.getName())) {
                i = compareDate(e1.getCreated(), e2.getCreated(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(EventSubscriptionQueryProperty.PROCESS_INSTANCE_ID.getName())) {
                i = compareString(e1.getProcessInstanceId(), e2.getProcessInstanceId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(EventSubscriptionQueryProperty.PROCESS_DEFINITION_ID.getName())) {
                i = compareString(e1.getProcessDefinitionId(), e2.getProcessDefinitionId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(EventSubscriptionQueryProperty.EVENT_NAME.getName())) {
                i = compareString(e1.getEventName(), e2.getEventName(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(EventSubscriptionQueryProperty.TENANT_ID.getName())) {
                i = compareString(e1.getTenantId(), e2.getTenantId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }
        }

        return e1.getId().compareTo(e2.getId());
    }

    public static EventSubscriptionComparator resolve(String orderBy) {
        if (orderBy == null) {
            return null;
        }
        EventSubscriptionComparator comparator = comparators.get(orderBy);
        if (comparator != null) {
            return comparator;
        }

        List<OrderBy> resolvedOrderBy = resolveOrderBy(orderBy);
        if (resolvedOrderBy == null || resolvedOrderBy.isEmpty()) {
            return null;
        }
        comparator = new EventSubscriptionComparator(resolvedOrderBy);
        comparators.put(orderBy, comparator);
        return comparator;
    }
}
