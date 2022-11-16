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
package org.flowable.engine.data.inmemory.impl.task;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.common.engine.impl.db.ListQueryParameterObject.OrderBy;
import org.flowable.engine.data.inmemory.AbstractEntityComparator;
import org.flowable.task.service.impl.TaskQueryProperty;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class TaskComparator extends AbstractEntityComparator implements Comparator<TaskEntity> {

    private static final ConcurrentHashMap<String, TaskComparator> comparators = new ConcurrentHashMap<>();

    private final List<OrderBy> order;

    private TaskComparator(List<OrderBy> order) {
        this.order = order;
    }

    @Override
    public int compare(TaskEntity e1, TaskEntity e2) {

        int i = 0;
        for (OrderBy ob : order) {
            if (ob.getColumnName().equals(TaskQueryProperty.TASK_ID.getName())) {
                i = compareString(e1.getId(), e2.getId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.TASK_ID.getName())) {
                i = compareString(e1.getId(), e2.getId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.DESCRIPTION.getName())) {
                i = compareString(e1.getDescription(), e2.getDescription(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.PRIORITY.getName())) {
                i = compareInt(e1.getPriority(), e2.getPriority(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.ASSIGNEE.getName())) {
                i = compareString(e1.getAssignee(), e2.getAssignee(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.OWNER.getName())) {
                i = compareString(e1.getOwner(), e2.getOwner(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.CREATE_TIME.getName())) {
                i = compareDate(e1.getCreateTime(), e2.getCreateTime(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.PROCESS_INSTANCE_ID.getName())) {
                i = compareString(e1.getProcessInstanceId(), e2.getProcessInstanceId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.EXECUTION_ID.getName())) {
                i = compareString(e1.getExecutionId(), e2.getExecutionId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.PROCESS_DEFINITION_ID.getName())) {
                i = compareString(e1.getProcessDefinitionId(), e2.getProcessDefinitionId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.DUE_DATE.getName())) {
                i = compareDate(e1.getDueDate(), e2.getDueDate(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.TENANT_ID.getName())) {
                i = compareString(e1.getTenantId(), e2.getTenantId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.TASK_DEFINITION_KEY.getName())) {
                i = compareString(e1.getTaskDefinitionKey(), e2.getTaskDefinitionKey(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(TaskQueryProperty.CATEGORY.getName())) {
                i = compareString(e1.getCategory(), e2.getCategory(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }
        }

        return e1.getId().compareTo(e2.getId());
    }

    public static TaskComparator resolve(String orderBy) {
        if (orderBy == null) {
            return null;
        }
        TaskComparator comparator = comparators.get(orderBy);
        if (comparator != null) {
            return comparator;
        }

        List<OrderBy> resolvedOrderBy = resolveOrderBy(orderBy);
        if (resolvedOrderBy == null || resolvedOrderBy.isEmpty()) {
            return null;
        }
        comparator = new TaskComparator(resolvedOrderBy);
        comparators.put(orderBy, comparator);
        return comparator;
    }
}
