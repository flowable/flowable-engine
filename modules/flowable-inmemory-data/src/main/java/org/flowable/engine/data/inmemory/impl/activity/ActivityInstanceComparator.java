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
package org.flowable.engine.data.inmemory.impl.activity;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.common.engine.impl.db.ListQueryParameterObject.OrderBy;
import org.flowable.engine.data.inmemory.AbstractEntityComparator;
import org.flowable.engine.impl.ActivityInstanceQueryProperty;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryProperty;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class ActivityInstanceComparator extends AbstractEntityComparator implements Comparator<ActivityInstanceEntity> {

    private static final ConcurrentHashMap<String, ActivityInstanceComparator> comparators = new ConcurrentHashMap<>();

    private final List<OrderBy> order;

    private ActivityInstanceComparator(List<OrderBy> order) {
        this.order = order;
    }

    @Override
    public int compare(ActivityInstanceEntity e1, ActivityInstanceEntity e2) {
        int i = 0;
        for (OrderBy ob : order) {
            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.ACTIVITY_INSTANCE_ID.getName())) {
                i = compareString(e1.getId(), e2.getId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.PROCESS_INSTANCE_ID.getName())) {
                i = compareString(e1.getProcessInstanceId(), e2.getProcessInstanceId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.EXECUTION_ID.getName())) {
                i = compareString(e1.getExecutionId(), e2.getExecutionId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.ACTIVITY_ID.getName())) {
                i = compareString(e1.getActivityId(), e2.getActivityId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.ACTIVITY_NAME.getName())) {
                i = compareString(e1.getActivityName(), e2.getActivityName(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.ACTIVITY_TYPE.getName())) {
                i = compareString(e1.getActivityType(), e2.getActivityType(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.PROCESS_DEFINITION_ID.getName())) {
                i = compareString(e1.getProcessDefinitionId(), e2.getProcessDefinitionId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.START.getName())) {
                i = compareDate(e1.getStartTime(), e2.getStartTime(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.END.getName())) {
                i = compareDate(e1.getEndTime(), e2.getEndTime(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ActivityInstanceQueryProperty.DURATION.getName())) {
                i = compareLong(e1.getDurationInMillis(), e2.getDurationInMillis(), ob.getDirection(), ob.getNullHandlingOnOrder());
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

    public static ActivityInstanceComparator resolve(String orderBy) {
        if (orderBy == null) {
            return null;
        }
        ActivityInstanceComparator comparator = comparators.get(orderBy);
        if (comparator != null) {
            return comparator;
        }

        List<OrderBy> resolvedOrderBy = resolveOrderBy(orderBy);
        if (resolvedOrderBy == null || resolvedOrderBy.isEmpty()) {
            return null;
        }
        comparator = new ActivityInstanceComparator(resolvedOrderBy);
        comparators.put(orderBy, comparator);
        return comparator;
    }
}
