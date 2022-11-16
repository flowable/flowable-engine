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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.slf4j.Logger;

/**
 * An abstract base class for any In memory {@link DataManager} implementations
 * capable of working with any extension of {@link Entity}.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public abstract class AbstractMemoryDataManager<T extends Entity> {

    protected final Logger logger;

    private final Map<String, T> data;

    private final IdGenerator idGenerator;

    private final AtomicLong inserts = new AtomicLong();

    private final AtomicLong updates = new AtomicLong();

    private final AtomicLong deletes = new AtomicLong();

    // Used to keep track of deletes within a single Command. As the memory data
    // managers are not transactional we need a way to reference deleted objects
    // so that eg. job retries work correctly.
    private ThreadLocal<Map<String, T>> threadDeletes = new ThreadLocal<>();

    // Used to keep track of inserts within a single Command. As the memory data
    // managers are not transactional we need a way to know which items have
    // been inserted within a command, so we can delete them on a failure
    private ThreadLocal<Map<String, T>> threadInserts = new ThreadLocal<>();

    public AbstractMemoryDataManager(Logger logger, MapProvider mapProvider, IdGenerator idGenerator) {
        data = mapProvider.create(1023, 0.6f);
        this.logger = logger;
        this.idGenerator = idGenerator;
    }

    protected void onEvent(MemoryDataManagerEvent type) {
        switch (type) {
        case FAILURE:
            onFailure();
            return;
        case COMPLETE:
            onComplete();
            break;
        }
    }

    protected void onFailure() {
        Map<String, T> localInserts = threadInserts.get();
        if (localInserts != null) {
            // delete all local inserts
            localInserts.values().stream().forEach(entity -> doDelete(entity, false));
        }
        Map<String, T> localDeletes = threadDeletes.get();
        if (localDeletes != null) {
            // insert all local deletes
            localDeletes.values().stream().forEach(entity -> doInsert(entity, false));
        }

        // failure is always also a completion
        onComplete();
    }

    protected void onComplete() {
        this.threadInserts.remove();
        this.threadDeletes.remove();
    }

    /**
     * Returns the current data for the DbSqlSession of this Memory DataManager
     * instance
     * 
     * @return Current session data
     */
    public Map<String, T> getData() {
        return data;
    }

    /**
     * @return the IdGenerator for this memory datamanager
     */
    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    /**
     * Returns the number of items currently stored
     *
     * @return current number of items
     */
    public int size() {
        return data.size();
    }

    /**
     * Returns the number of inserts performed by this DataManager instance
     */
    public long inserts() {
        return inserts.get();
    }

    /**
     * Returns the number of updates performed by this DataManager instance
     */
    public long updates() {
        return updates.get();
    }

    /**
     * Returns the number of deletes performed by this DataManager instance
     */
    public long deletes() {
        return deletes.get();
    }

    public void commitInsert(T entity) {
        data.put(entity.getId(), entity);
        inserts.incrementAndGet();
    }

    protected void doInsert(T entity) {
        doInsert(entity, true);
    }

    private void doInsert(T entity, boolean storeThreadInsert) {
        if (entity == null) {
            throw new IllegalStateException("Entity cannot be null");
        }
        if (entity.getId() == null) {
            entity.setId(idGenerator.getNextId());
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Insert {} (id={})", entity, entity.getId());
        }
        data.put(entity.getId(), entity);
        inserts.incrementAndGet();

        if (!storeThreadInsert) {
            return;
        }

        Map<String, T> localInserts = threadInserts.get();
        if (localInserts == null) {
            localInserts = new HashMap<>();
            threadInserts.set(localInserts);
        }
        localInserts.put(entity.getId(), entity);
    }

    protected T doUpdate(T entity) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalStateException("Entity or entity ID cannot be null");
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Update {}", entity);
        }
        data.put(entity.getId(), entity);
        if (entity instanceof HasRevision) {
            ((HasRevision) entity).setRevision(((HasRevision) entity).getRevision() + 1);
        }
        updates.incrementAndGet();
        return entity;
    }

    protected void doDelete(String id) {
        if (id == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("delete {}", id);
        }
        T item = data.get(id);
        doDelete(item);
    }

    protected void doDelete(String id, Consumer<T> cb) {
        if (id == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("delete {}", id);
        }
        T item = data.get(id);
        if (cb != null) {
            cb.accept(item);
        }
        doDelete(item);
    }

    protected void doDelete(T entity) {
        doDelete(entity, true);
    }

    private void doDelete(T entity, boolean storeThreadDelete) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalStateException("Entity or entity ID cannot be null");
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Delete {}", entity);
        }

        entity.setDeleted(true);
        data.remove(entity.getId());
        deletes.incrementAndGet();

        if (!storeThreadDelete) {
            return;
        }

        Map<String, T> localDeletes = threadDeletes.get();
        if (localDeletes == null) {
            localDeletes = new HashMap<>();
            threadDeletes.set(localDeletes);
        }
        localDeletes.put(entity.getId(), entity);
    }

    protected T doFindById(String id) {
        if (id == null) {
            return null;
        }
        T item = data.get(id);
        if (item != null) {
            return item;
        }

        // Check if deleted / inserted within current command stack
        Map<String, T> localDeletes = threadDeletes.get();
        if (localDeletes != null) {
            item = localDeletes.get(id);
        }
        if (item != null) {
            return item;
        }
        Map<String, T> localInserts = threadInserts.get();
        if (localInserts != null) {
            item = localInserts.get(id);
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    protected <X extends Object> List<X> sortAndPaginate(List<X> collect, AbstractEntityComparator comparator, int firstResult, int maxResults) {

        if (collect.size() > 1 && comparator != null) {
            // Cast to comparator
            Collections.sort(collect, (Comparator<X>) comparator);
        }
        if (firstResult < 0 | maxResults < 0) {
            return collect;
        }

        int end = Math.min(firstResult + maxResults, collect.size());
        if (firstResult > collect.size()) {
            return Collections.emptyList();
        }

        return collect.subList(firstResult, end);
    }

}
