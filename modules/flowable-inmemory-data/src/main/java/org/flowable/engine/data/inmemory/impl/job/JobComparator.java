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
package org.flowable.engine.data.inmemory.impl.job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.common.engine.impl.db.ListQueryParameterObject.OrderBy;
import org.flowable.engine.data.inmemory.AbstractEntityComparator;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.JobQueryProperty;

/**
 * Comparator for any {@link Job}
 *
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class JobComparator extends AbstractEntityComparator implements Comparator<Job> {

    private static final ConcurrentHashMap<String, JobComparator> comparators = new ConcurrentHashMap<>();

    private static final JobComparator DEFAULT_COMPARATOR = new JobComparator(new OrderBy("RES.ID_", "asc", null));

    private final List<OrderBy> order;

    private JobComparator(OrderBy order) {
        this.order = new ArrayList<>();
        this.order.add(order);
    }

    private JobComparator(List<OrderBy> order) {
        this.order = order;
    }

    @Override
    public int compare(Job j1, Job j2) {

        int i = 0;
        for (OrderBy ob : order) {
            if (ob.getColumnName().equals(JobQueryProperty.JOB_ID.getName())) {
                i = compareString(j1.getId(), j2.getId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(JobQueryProperty.PROCESS_INSTANCE_ID.getName())) {
                i = compareString(j1.getProcessInstanceId(), j2.getProcessInstanceId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(JobQueryProperty.EXECUTION_ID.getName())) {
                i = compareString(j1.getExecutionId(), j2.getExecutionId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(JobQueryProperty.CREATE_TIME.getName())) {
                i = compareDate(j1.getCreateTime(), j2.getCreateTime(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(JobQueryProperty.DUEDATE.getName())) {
                i = compareDate(j1.getDuedate(), j2.getDuedate(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(JobQueryProperty.TENANT_ID.getName())) {
                i = compareString(j1.getTenantId(), j2.getTenantId(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }

            if (ob.getColumnName().equals(JobQueryProperty.RETRIES.getName())) {
                i = compareInt(j1.getRetries(), j2.getRetries(), ob.getDirection(), ob.getNullHandlingOnOrder());
                if (i != 0) {
                    return i;
                }
            }
        }

        return j1.getId().compareTo(j2.getId());
    }

    public static JobComparator resolve(String orderBy) {
        if (orderBy == null) {
            return null;
        }
        JobComparator comparator = comparators.get(orderBy);
        if (comparator != null) {
            return comparator;
        }

        List<OrderBy> resolvedOrderBy = resolveOrderBy(orderBy);
        if (resolvedOrderBy == null || resolvedOrderBy.isEmpty()) {
            return null;
        }
        comparator = new JobComparator(resolvedOrderBy);
        comparators.put(orderBy, comparator);
        return comparator;
    }

    public static JobComparator getDefault() {

        return DEFAULT_COMPARATOR;
    }
}
