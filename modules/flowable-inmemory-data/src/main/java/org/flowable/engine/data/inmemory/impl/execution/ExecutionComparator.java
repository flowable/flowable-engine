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
package org.flowable.engine.data.inmemory.impl.execution;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.common.engine.impl.db.ListQueryParameterObject.OrderBy;
import org.flowable.engine.data.inmemory.AbstractEntityComparator;
import org.flowable.engine.impl.ProcessInstanceQueryProperty;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * A comparator for {@link Execution} instances.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class ExecutionComparator extends AbstractEntityComparator implements Comparator<ExecutionEntity> {

    private static final ConcurrentHashMap<String, ExecutionComparator> comparators = new ConcurrentHashMap<>();

    private final List<OrderBy> order;

    private ExecutionComparator(List<OrderBy> order) {
        this.order = order;
    }

    @Override
    public int compare(ExecutionEntity e1, ExecutionEntity e2) {
        int i = 0;
        for (OrderBy ob : order) {
            if (ob.getColumnName().equals(ProcessInstanceQueryProperty.PROCESS_START_TIME.getName())) {
                i = compareDate(e1.getStartTime(), e2.getStartTime(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID.getName())) {
                i = compareString(e1.getId(), e2.getId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ProcessInstanceQueryProperty.PROCESS_DEFINITION_ID.getName())) {
                i = compareString(e1.getProcessDefinitionId(), e2.getProcessDefinitionId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY.getName())) {
                i = compareString(e1.getProcessDefinitionKey(), e2.getProcessDefinitionKey(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(ProcessInstanceQueryProperty.TENANT_ID.getName())) {
                i = compareString(e1.getTenantId(), e2.getTenantId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }
        }

        return e1.getId().compareTo(e2.getId());
    }

    public static ExecutionComparator resolve(String orderBy) {
        if (orderBy == null) {
            return null;
        }
        ExecutionComparator comparator = comparators.get(orderBy);
        if (comparator != null) {
            return comparator;
        }

        List<OrderBy> resolvedOrderBy = resolveOrderBy(orderBy);
        if (resolvedOrderBy == null || resolvedOrderBy.isEmpty()) {
            return null;
        }
        comparator = new ExecutionComparator(resolvedOrderBy);
        comparators.put(orderBy, comparator);
        return comparator;
    }
}
