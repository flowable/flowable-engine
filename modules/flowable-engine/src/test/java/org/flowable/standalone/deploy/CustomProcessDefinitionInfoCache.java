package org.flowable.standalone.deploy;

import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionInfoCacheObject;

import java.util.HashMap;
import java.util.Map;

public class CustomProcessDefinitionInfoCache implements DeploymentCache<ProcessDefinitionInfoCacheObject> {

    private final Map<String, ProcessDefinitionInfoCacheObject> cache = new HashMap<>();

    @Override
    public ProcessDefinitionInfoCacheObject get(String id) {
        return cache.get(id);
    }

    @Override
    public boolean contains(String id) {
        return cache.containsKey(id);
    }

    @Override
    public void add(String id, ProcessDefinitionInfoCacheObject object) {
        cache.put(id, object);
    }

    @Override
    public void remove(String id) {
        cache.remove(id);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    //for testing purpose
    public int size(){
        return cache.size();
    }
}
