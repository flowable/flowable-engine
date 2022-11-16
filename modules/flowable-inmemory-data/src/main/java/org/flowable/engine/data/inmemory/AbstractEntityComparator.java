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
package org.flowable.engine.data.inmemory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.query.Query.NullHandlingOnOrder;
import org.flowable.common.engine.impl.db.ListQueryParameterObject;
import org.flowable.common.engine.impl.db.ListQueryParameterObject.OrderBy;

/**
 * Abstract base for various {@link Entity} comparator implementations.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public abstract class AbstractEntityComparator {

    private static final String NULL_HANDLING_FIRST = "FIRST";

    private static final String ORDER_BY = "order by "; // trailing space on
                                                        // purpose

    protected int compareString(String s1, String s2, String direction, NullHandlingOnOrder nullHandling) {
        if (s1 == null && s2 == null) {
            return 0;
        }
        if (s1 == null) {
            return NullHandlingOnOrder.NULLS_FIRST == nullHandling ? -1 : 1;
        }
        if (s2 == null) {
            return NullHandlingOnOrder.NULLS_FIRST == nullHandling ? 1 : -1;
        }
        int i = s1.compareTo(s2);
        return ListQueryParameterObject.SORTORDER_ASC.equals(direction) ? i : -i;
    }

    protected static int compareDate(Date d1, Date d2, String direction, NullHandlingOnOrder nullHandling) {
        if (d1 == null && d2 == null) {
            return 0;
        }
        if (d1 == null) {
            return NullHandlingOnOrder.NULLS_FIRST == nullHandling ? -1 : 1;
        }
        if (d2 == null) {
            return NullHandlingOnOrder.NULLS_FIRST == nullHandling ? 1 : -1;
        }
        int i = d1.compareTo(d2);
        return ListQueryParameterObject.SORTORDER_ASC.equals(direction) ? i : -i;
    }

    protected static int compareLong(Long l1, Long l2, String direction, NullHandlingOnOrder nullHandling) {
        if (l1 == null && l2 == null) {
            return 0;
        }
        if (l1 == null) {
            return NullHandlingOnOrder.NULLS_FIRST == nullHandling ? -1 : 1;
        }
        if (l2 == null) {
            return NullHandlingOnOrder.NULLS_FIRST == nullHandling ? 1 : -1;
        }
        int i = l1.compareTo(l2);
        return ListQueryParameterObject.SORTORDER_ASC.equals(direction) ? i : -i;
    }

    protected static int compareInt(Integer l1, Integer l2, String direction, NullHandlingOnOrder nullHandling) {
        if (l1 == null && l2 == null) {
            return 0;
        }
        if (l1 == null) {
            return NullHandlingOnOrder.NULLS_FIRST == nullHandling ? -1 : 1;
        }
        if (l2 == null) {
            return NullHandlingOnOrder.NULLS_FIRST == nullHandling ? 1 : -1;
        }
        int i = l1.compareTo(l2);
        return ListQueryParameterObject.SORTORDER_ASC.equals(direction) ? i : -i;
    }

    protected static List<OrderBy> resolveOrderBy(String orderBy) {

        List<OrderBy> result = null;
        if (orderBy != null) {
            String[] parts = StringUtils.split(orderBy, ',');
            for (String part : parts) {
                if (part.startsWith(ORDER_BY)) {
                    part = part.substring(ORDER_BY.length());
                }
                String[] values = StringUtils.split(part, ' ');
                if (values.length != 2 && values.length != 4) {
                    throw new IllegalArgumentException(
                                    "Unsupported sort '" + part + "', expected 2 or 4 parts ('column direction') but got " + values.length + " instead!");
                }
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(new OrderBy(StringUtils.trim(values[0]), StringUtils.trim(values[1]), values.length > 3 ? resolveNullHandling(values[3]) : null));
            }
        }

        return result;
    }

    protected static NullHandlingOnOrder resolveNullHandling(String firstOrLast) {
        return NULL_HANDLING_FIRST.equalsIgnoreCase(firstOrLast) ? NullHandlingOnOrder.NULLS_FIRST : NullHandlingOnOrder.NULLS_LAST;
    }
}
