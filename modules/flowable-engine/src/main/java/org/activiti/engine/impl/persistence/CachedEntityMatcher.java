package org.activiti.engine.impl.persistence;

import java.util.Collection;

import org.activiti.engine.common.impl.persistence.entity.Entity;
import org.activiti.engine.impl.persistence.cache.CachedEntity;

/**
 * Interface to express a condition whether or not a cached entity should be used in the return result of a query.
 * 
 * @author Joram Barrez
 */
public interface CachedEntityMatcher<EntityImpl extends Entity> {

  /**
   * Returns true if an entity from the cache should be retained (i.e. used as return result for a query).
   * 
   * Most implementations of this interface probably don't need this method,
   * and should extend the simpler {@link CachedEntityMatcherAdapter}, which hides this method.
   * 
   * Note that the databaseEntities collection can be null, in case only the cache is checked.
   */
  boolean isRetained(Collection<EntityImpl> databaseEntities, Collection<CachedEntity> cachedEntities, EntityImpl entity, Object param);

}
