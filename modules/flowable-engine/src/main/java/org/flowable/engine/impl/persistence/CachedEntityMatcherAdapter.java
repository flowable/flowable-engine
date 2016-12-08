package org.flowable.engine.impl.persistence;

import java.util.Collection;

import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.engine.impl.persistence.cache.CachedEntity;

/**
 * @author Joram Barrez
 */
public abstract class CachedEntityMatcherAdapter<EntityImpl extends Entity> implements CachedEntityMatcher<EntityImpl> {
  
  @Override
  public boolean isRetained(Collection<EntityImpl> databaseEntities, Collection<CachedEntity> cachedEntities, EntityImpl entity, Object param) {
    return isRetained(entity, param);
  }
  
  public abstract boolean isRetained(EntityImpl entity, Object param);
  
}
 
