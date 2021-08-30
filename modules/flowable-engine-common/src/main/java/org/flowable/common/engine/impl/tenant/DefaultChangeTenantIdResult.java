package org.flowable.common.engine.impl.tenant;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;

public class DefaultChangeTenantIdResult implements ChangeTenantIdResult {

    private final Map<String,Long> resultMap;

    public DefaultChangeTenantIdResult() {
        this.resultMap = new HashMap<>();
    }

    public DefaultChangeTenantIdResult(Map<String,Long> resultMap) {
        this.resultMap = resultMap;
    }

    @Override
    public long getChangedInstances(String entityType) {
        return resultMap.containsKey(entityType) ? resultMap.get(entityType) : 0L;
    }

    @Override
    public long putResult(String key, Long value) {
        return this.resultMap.put(key, value);
    }

    @Override
    public void putAllResults(Map<String,Long> resultMap) {
        this.resultMap.putAll(resultMap);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resultMap == null) ? 0 : resultMap.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
            DefaultChangeTenantIdResult other = (DefaultChangeTenantIdResult) obj;
        if (resultMap == null) {
            if (other.resultMap != null)
                return false;
        } else if (!resultMap.equals(other.resultMap))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ChangeTenantIdResult [resultMap=" + resultMap + "]";
    }

}
