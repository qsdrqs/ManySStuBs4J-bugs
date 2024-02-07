/* 
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hazelcast.impl;

import com.hazelcast.cluster.ClusterImpl;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.core.*;
import com.hazelcast.impl.base.DistributedLock;
import com.hazelcast.impl.base.ScheduledAction;
import com.hazelcast.impl.concurrentmap.LFUMapEntryComparator;
import com.hazelcast.impl.concurrentmap.LRUMapEntryComparator;
import com.hazelcast.impl.concurrentmap.MapStoreWrapper;
import com.hazelcast.impl.concurrentmap.MultiData;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.*;
import com.hazelcast.query.Expression;
import com.hazelcast.query.MapIndexService;
import com.hazelcast.util.SortedHashMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import static com.hazelcast.impl.ClusterOperation.*;
import static com.hazelcast.impl.Constants.Objects.OBJECT_REDO;
import static com.hazelcast.nio.IOUtil.toData;
import static com.hazelcast.nio.IOUtil.toObject;

public class CMap {

    private static final Comparator<MapEntry> LRU_COMPARATOR = new LRUMapEntryComparator();
    private static final Comparator<MapEntry> LFU_COMPARATOR = new LFUMapEntryComparator();

    enum EvictionPolicy {
        LRU,
        LFU,
        NONE
    }

    public final static int DEFAULT_MAP_SIZE = 10000;

    final ILogger logger;

    final ConcurrentMapManager concurrentMapManager;

    final Node node;

    final int PARTITION_COUNT;

    final Block[] blocks;

    final Address thisAddress;

    final ConcurrentMap<Data, Record> mapRecords = new ConcurrentHashMap<Data, Record>(10000);

    final ConcurrentMap<Long, RecordEntry> mapRecordEntries = new ConcurrentHashMap<Long, RecordEntry>(1000);

    final String name;

    final Map<Address, Boolean> mapListeners = new HashMap<Address, Boolean>(1);

    final int backupCount;

    final EvictionPolicy evictionPolicy;

    final Comparator<MapEntry> evictionComparator;

    final int maxSize;

    final float evictionRate;

    final long ttl; //ttl for entries

    final long maxIdle; //maxIdle for entries

    final Instance.InstanceType instanceType;

    final MapLoader loader;

    final MapStore store;

    final long writeDelayMillis;

    final long removeDelayMillis;

    final long evictionDelayMillis;

    final MapIndexService mapIndexService;

    final LocallyOwnedMap locallyOwnedMap;

    final MapNearCache mapNearCache;

    final long creationTime;

    volatile boolean ttlPerRecord = false;

    volatile long lastEvictionTime = 0;

    DistributedLock lockEntireMap = null;

    public CMap(ConcurrentMapManager concurrentMapManager, String name) {
        this.concurrentMapManager = concurrentMapManager;
        this.logger = concurrentMapManager.node.getLogger(CMap.class.getName());
        this.PARTITION_COUNT = concurrentMapManager.PARTITION_COUNT;
        this.blocks = concurrentMapManager.blocks;
        this.node = concurrentMapManager.node;
        this.thisAddress = concurrentMapManager.thisAddress;
        this.name = name;
        MapConfig mapConfig = null;
        String mapConfigName = name.substring(2);
        if (mapConfigName.startsWith("__hz_") || mapConfigName.startsWith("l:") || mapConfigName.startsWith("s:")) {
            mapConfig = new MapConfig();
        } else {
            mapConfig = node.getConfig().getMapConfig(mapConfigName);
        }
        this.mapIndexService = new MapIndexService(mapConfig.isValueIndexed());
        this.backupCount = mapConfig.getBackupCount();
        ttl = mapConfig.getTimeToLiveSeconds() * 1000L;
        evictionDelayMillis = mapConfig.getEvictionDelaySeconds() * 1000L;
        maxIdle = mapConfig.getMaxIdleSeconds() * 1000L;
        evictionPolicy = EvictionPolicy.valueOf(mapConfig.getEvictionPolicy());
        if (evictionPolicy == EvictionPolicy.NONE) {
            maxSize = Integer.MAX_VALUE;
            evictionComparator = null;
        } else {
            maxSize = (mapConfig.getMaxSize() == 0) ? MapConfig.DEFAULT_MAX_SIZE : mapConfig.getMaxSize();
            if (evictionPolicy == EvictionPolicy.LRU) {
                evictionComparator = LRU_COMPARATOR;
            } else {
                evictionComparator = LFU_COMPARATOR;
            }
        }
        evictionRate = mapConfig.getEvictionPercentage() / 100f;
        instanceType = ConcurrentMapManager.getInstanceType(name);
        MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
        MapStoreWrapper mapStoreWrapper = null;
        int writeDelaySeconds = -1;
        if (!node.isSuperClient() && mapStoreConfig != null && mapStoreConfig.isEnabled()) {
            try {
                Object storeInstance = mapStoreConfig.getImplementation();
                if (storeInstance == null) {
                    String mapStoreClassName = mapStoreConfig.getClassName();
                    storeInstance = Serializer.classForName(node.getConfig().getClassLoader(), mapStoreClassName).newInstance();
                }
                mapStoreWrapper = new MapStoreWrapper(storeInstance,
                        node.factory.getHazelcastInstanceProxy(),
                        mapStoreConfig.getProperties(),
                        mapConfigName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            writeDelaySeconds = mapStoreConfig.getWriteDelaySeconds();
        }
        if (!node.isSuperClient() && evictionPolicy == EvictionPolicy.NONE && instanceType == Instance.InstanceType.MAP) {
            locallyOwnedMap = new LocallyOwnedMap();
            concurrentMapManager.mapLocallyOwnedMaps.put(name, locallyOwnedMap);
        } else {
            locallyOwnedMap = null;
        }
        writeDelayMillis = (writeDelaySeconds == -1) ? -1L : writeDelaySeconds * 1000L;
        if (writeDelaySeconds > 0) {
            removeDelayMillis = concurrentMapManager.GLOBAL_REMOVE_DELAY_MILLIS + writeDelaySeconds;
        } else {
            removeDelayMillis = concurrentMapManager.GLOBAL_REMOVE_DELAY_MILLIS;
        }
        loader = (mapStoreWrapper == null || !mapStoreWrapper.isMapLoader()) ? null : mapStoreWrapper;
        store = (mapStoreWrapper == null || !mapStoreWrapper.isMapStore()) ? null : mapStoreWrapper;
        NearCacheConfig nearCacheConfig = mapConfig.getNearCacheConfig();
        if (nearCacheConfig == null) {
            mapNearCache = null;
        } else {
            mapNearCache = new MapNearCache(this,
                    SortedHashMap.getOrderingTypeByName(nearCacheConfig.getEvictionPolicy()),
                    nearCacheConfig.getMaxSize(),
                    nearCacheConfig.getTimeToLiveSeconds() * 1000L,
                    nearCacheConfig.getMaxIdleSeconds() * 1000L,
                    nearCacheConfig.isInvalidateOnChange());
            concurrentMapManager.mapCaches.put(name, mapNearCache);
        }
        this.creationTime = System.currentTimeMillis();
    }

    public boolean checkLock(Request request) {
        return (lockEntireMap == null
                || !lockEntireMap.isLocked()
                || lockEntireMap.isLockedBy(request.lockAddress, request.lockThreadId));
    }

    public RecordEntry getRecordEntry(Record record) {
        RecordEntry recordEntry = mapRecordEntries.get(record.getId());
        if (recordEntry == null) {
            recordEntry = new RecordEntry(record);
            RecordEntry found = mapRecordEntries.putIfAbsent(record.getId(), recordEntry);
            if (found != null) {
                recordEntry = found;
            }
        }
        return recordEntry;
    }

    public void invalidateRecordEntryValue(Record record) {
        mapRecordEntries.remove(record.getId());
    }

    public void lockMap(Request request) {
        if (request.operation == CONCURRENT_MAP_LOCK_MAP) {
            if (lockEntireMap == null) {
                lockEntireMap = new DistributedLock();
            }
            boolean locked = lockEntireMap.lock(request.lockAddress, request.lockThreadId);
            request.clearForResponse();
            if (!locked) {
                if (request.timeout == 0) {
                    request.response = Boolean.FALSE;
                } else {
                    request.response = OBJECT_REDO;
                }
            } else {
                request.response = Boolean.TRUE;
            }
        } else if (request.operation == CONCURRENT_MAP_UNLOCK_MAP) {
            if (lockEntireMap != null) {
                request.response = lockEntireMap.unlock(request.lockAddress, request.lockThreadId);
            } else {
                request.response = Boolean.TRUE;
            }
        }
    }

    public void addIndex(Expression expression, boolean ordered, int attributeIndex) {
        mapIndexService.addIndex(expression, ordered, attributeIndex);
    }

    public Record getRecord(Data key) {
        return mapRecords.get(key);
    }

    public int getBackupCount() {
        return backupCount;
    }

    public void own(Request req) {
        if (req.key == null || req.key.size() == 0) {
            throw new RuntimeException("Key cannot be null " + req.key);
        }
        if (req.value == null) {
            req.value = new Data();
        }
        Record record = toRecord(req);
        if (req.ttl <= 0 || req.timeout <= 0) {
            record.setInvalid();
        } else {
            record.setExpirationTime(req.ttl);
            record.setMaxIdle(req.timeout);
        }
        markAsActive(record);
        if (store != null && writeDelayMillis > 0) {
            markAsDirty(record);
        }
        updateIndexes(record);
        record.setVersion(req.version);
    }

    public boolean isMultiMap() {
        return (instanceType == Instance.InstanceType.MULTIMAP);
    }

    public boolean isSet() {
        return (instanceType == Instance.InstanceType.SET);
    }

    public boolean isList() {
        return (instanceType == Instance.InstanceType.LIST);
    }

    public boolean isMap() {
        return (instanceType == Instance.InstanceType.MAP);
    }

    public boolean backup(Request req) {
        if (req.key == null || req.key.size() == 0) {
            throw new RuntimeException("Backup key size cannot be 0: " + req.key);
        }
        if (instanceType == Instance.InstanceType.MAP || instanceType == Instance.InstanceType.SET) {
            return backupOneValue(req);
        } else {
            return backupMultiValue(req);
        }
    }

    /**
     * Map and Set have one value only so we can ignore the
     * values with old version.
     *
     * @param req
     * @return
     */
    private boolean backupOneValue(Request req) {
        Record record = getRecord(req.key);
        if (record != null && record.isActive() && req.version < record.getVersion()) {
            return false;
        }
        doBackup(req);
        if (record != null) {
            record.setVersion(req.version);
        }
        return true;
    }

    /**
     * MultiMap and List have to use versioned backup
     * because each key can have multiple values and
     * we don't want to miss backing up each one.
     *
     * @param req
     * @return
     */
    private boolean backupMultiValue(Request req) {
        Record record = getRecord(req.key);
        if (record != null) {
            record.setActive();
            if (req.version > record.getVersion() + 1) {
                Request reqCopy = req.hardCopy();
                record.addBackupOp(new VersionedBackupOp(this, reqCopy));
                return true;
            } else if (req.version <= record.getVersion()) {
                return false;
            }
        }
        doBackup(req);
        if (record != null) {
            record.setVersion(req.version);
            record.runBackupOps();
        }
        return true;
    }

    public void doBackup(Request req) {
        if (req.key == null || req.key.size() == 0) {
            throw new RuntimeException("Backup key size cannot be zero! " + req.key);
        }
        if (req.operation == CONCURRENT_MAP_BACKUP_PUT) {
            Record record = toRecord(req);
            markAsActive(record);
            record.setVersion(req.version);
            if (req.indexes != null) {
                if (req.indexTypes == null) {
                    throw new RuntimeException("index types cannot be null!");
                }
                if (req.indexes.length != req.indexTypes.length) {
                    throw new RuntimeException("index and type lengths do not match");
                }
                record.setIndexes(req.indexes, req.indexTypes);
            }
            if (req.ttl > 0) {
                record.setExpirationTime(req.ttl);
                ttlPerRecord = true;
            }
        } else if (req.operation == CONCURRENT_MAP_BACKUP_REMOVE) {
            Record record = getRecord(req.key);
            if (record != null) {
                if (record.isActive()) {
                    if (record.getCopyCount() > 0) {
                        record.decrementCopyCount();
                    }
                    markAsRemoved(record);
                }
            }
        } else if (req.operation == CONCURRENT_MAP_BACKUP_LOCK) {
            Record rec = toRecord(req);
            if (rec.getVersion() == 0) {
                rec.setVersion(req.version);
            }
        } else if (req.operation == CONCURRENT_MAP_BACKUP_ADD) {
            add(req, true);
        } else if (req.operation == CONCURRENT_MAP_BACKUP_REMOVE_MULTI) {
            Record record = getRecord(req.key);
            if (record != null) {
                if (req.value == null) {
                    markAsRemoved(record);
                } else {
                    if (record.containsValue(req.value)) {
                        if (record.getMultiValues() != null) {
                            Iterator<Data> itValues = record.getMultiValues().iterator();
                            while (itValues.hasNext()) {
                                Data value = itValues.next();
                                if (req.value.equals(value)) {
                                    itValues.remove();
                                }
                            }
                        }
                    }
                    if (record.valueCount() == 0) {
                        markAsRemoved(record);
                    }
                }
            }
        } else {
            logger.log(Level.SEVERE, "Unknown backup operation " + req.operation);
        }
    }

    private void purgeIfNotOwnedOrBackup(Collection<Record> records) {
        Map<Address, Integer> mapMemberDistances = new HashMap<Address, Integer>();
        for (Record record : records) {
            Block block = blocks[record.getBlockId()];
            boolean owned = thisAddress.equals(block.getOwner());
            if (!owned && !block.isMigrating()) {
                Address owner = (block.isMigrating()) ? block.getMigrationAddress() : block.getOwner();
                if (owner != null && !thisAddress.equals(owner)) {
                    int distance;
                    Integer d = mapMemberDistances.get(owner);
                    if (d == null) {
                        distance = concurrentMapManager.getDistance(owner, thisAddress);
                        mapMemberDistances.put(owner, distance);
                    } else {
                        distance = d;
                    }
                    if (distance > getBackupCount()) {
                        mapRecords.remove(record.getKey());
                    }
                }
            }
        }
    }

    private boolean isOwned(Record record) {
        PartitionServiceImpl partitionService = concurrentMapManager.partitionManager.partitionServiceImpl;
        PartitionServiceImpl.PartitionProxy partition = partitionService.getPartition(record.getBlockId());
        Member owner = partition.getOwner();
        return (owner != null && owner.localMember());
    }

    private boolean isBackup(Record record) {
        PartitionServiceImpl partitionService = concurrentMapManager.partitionManager.partitionServiceImpl;
        PartitionServiceImpl.PartitionProxy partition = partitionService.getPartition(record.getBlockId());
        Member ownerNow = partition.getOwner();
        Member ownerEventual = partition.getEventualOwner();
        if (ownerEventual != null && ownerNow != null && !ownerNow.localMember()) {
            int distance = node.getClusterImpl().getDistanceFrom(ownerEventual);
            return (distance != -1 && distance <= getBackupCount());
        }
        return false;
    }

    public int size() {
        if (maxIdle > 0 || ttl > 0 || ttlPerRecord || isList() || isMultiMap()) {
            long now = System.currentTimeMillis();
            int size = 0;
            Collection<Record> records = mapIndexService.getOwnedRecords();
            for (Record record : records) {
                if (record.isActive() && record.isValid(now)) {
                    size += record.valueCount();
                }
            }
            return size;
        } else {
            return mapIndexService.size();
        }
    }

    public boolean hasOwned(long blockId) {
        Collection<Record> records = mapRecords.values();
        for (Record record : records) {
            if (record.getBlockId() == blockId && record.isActive()) {
                return true;
            }
        }
        return false;
    }

    public int valueCount(Data key) {
        long now = System.currentTimeMillis();
        int count = 0;
        Record record = mapRecords.get(key);
        if (record != null && record.isValid(now)) {
            count = record.valueCount();
        }
        return count;
    }

    public boolean contains(Request req) {
        Data key = req.key;
        Data value = req.value;
        if (key != null) {
            Record record = getRecord(req.key);
            if (record == null) {
                return false;
            } else {
                if (record.isActive() && record.isValid()) {
                    if (value == null) {
                        return record.valueCount() > 0;
                    } else {
                        return record.containsValue(value);
                    }
                }
            }
        } else {
            Collection<Record> records = mapRecords.values();
            for (Record record : records) {
                long now = System.currentTimeMillis();
                if (record.isActive() && record.isValid(now)) {
                    Block block = blocks[record.getBlockId()];
                    if (thisAddress.equals(block.getOwner())) {
                        if (record.containsValue(value)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void containsValue(Request request) {
        if (isMultiMap()) {
            boolean found = false;
            Collection<Record> records = mapRecords.values();
            for (Record record : records) {
                long now = System.currentTimeMillis();
                if (record.isActive() && record.isValid(now)) {
                    Block block = blocks[record.getBlockId()];
                    if (thisAddress.equals(block.getOwner())) {
                        if (record.containsValue(request.value)) {
                            found = true;
                        }
                    }
                }
            }
            request.response = found;
        } else {
            request.response = mapIndexService.containsValue(request.value);
        }
    }

    public CMapEntry getMapEntry(Request req) {
        Record record = getRecord(req.key);
        if (record == null || !record.isActive() || !record.isValid()) {
            return null;
        }
        return new CMapEntry(record.getCost(), record.getExpirationTime(), record.getLastAccessTime(), record.getLastUpdateTime(),
                record.getCreationTime(), record.getVersion(), record.getHits(), true);
    }

    public Data get(Request req) {
        Record record = getRecord(req.key);
        if (record == null)
            return null;
        if (!record.isActive()) return null;
        if (!record.isValid()) {
            if (record.isEvictable()) {
                return null;
            }
        }
        record.setLastAccessed();
        Data data = record.getValue();
        Data returnValue = null;
        if (data != null) {
            returnValue = data;
        } else {
            if (record.getMultiValues() != null) {
                Values values = new Values(record.getMultiValues());
                returnValue = toData(values);
            }
        }
        if (req.local && locallyOwnedMap != null && returnValue != null) {
            locallyOwnedMap.offerToCache(record);
        }
        return returnValue;
    }

    public boolean add(Request req, boolean backup) {
        Record record = getRecord(req.key);
        if (record == null) {
            record = toRecord(req);
        } else {
            if (record.isActive() && req.operation == CONCURRENT_MAP_ADD_TO_SET) {
                return false;
            }
        }
        record.setActive(true);
        record.incrementVersion();
        record.incrementCopyCount();
        if (!backup) {
            updateIndexes(record);
            concurrentMapManager.fireMapEvent(mapListeners, getName(), EntryEvent.TYPE_ADDED, record, req.caller);
        }
        return true;
    }

    void lock(Request request) {
        Data reqValue = request.value;
        request.value = null;
        Record rec = concurrentMapManager.ensureRecord(request);
        if (request.operation == CONCURRENT_MAP_LOCK_AND_GET_VALUE) {
            if (reqValue == null) {
                request.value = rec.getValue();
                if (rec.getMultiValues() != null) {
                    Values values = new Values(rec.getMultiValues());
                    request.value = toData(values);
                }
            } else {
                // multimap txn put operation
                if (!rec.containsValue(reqValue)) {
                    request.value = null;
                } else {
                    request.value = reqValue;
                }
            }
        }
        rec.lock(request.lockThreadId, request.lockAddress);
        rec.incrementVersion();
        mapRecordEntries.remove(rec.getId());
        request.version = rec.getVersion();
        request.lockCount = rec.getLockCount();
        markAsActive(rec);
        request.response = Boolean.TRUE;
    }

    void unlock(Record record) {
        record.clearLock();
        fireScheduledActions(record);
    }

    void fireScheduledActions(Record record) {
        concurrentMapManager.checkServiceThread();
        if (record.getLockCount() == 0) {
            record.clearLock();
            while (record.hasScheduledAction()) {
                ScheduledAction sa = record.getScheduledActions().remove(0);
                node.clusterManager.deregisterScheduledAction(sa);
                if (!sa.expired()) {
                    sa.consume();
                    if (record.isLocked()) {
                        return;
                    }
                } else {
                    sa.onExpire();
                }
            }
        }
    }

    public void onMigrate(Record record) {
        if (record == null) return;
        List<ScheduledAction> lsScheduledActions = record.getScheduledActions();
        if (lsScheduledActions != null) {
            if (lsScheduledActions.size() > 0) {
                Iterator<ScheduledAction> it = lsScheduledActions.iterator();
                while (it.hasNext()) {
                    ScheduledAction sa = it.next();
                    if (sa.isValid() && !sa.expired()) {
                        sa.onMigrate();
                    }
                    sa.setValid(false);
                    node.clusterManager.deregisterScheduledAction(sa);
                    it.remove();
                }
            }
        }
    }

    public void onDisconnect(Record record, Address deadAddress) {
        if (record == null || deadAddress == null) return;
        List<ScheduledAction> lsScheduledActions = record.getScheduledActions();
        if (lsScheduledActions != null) {
            if (lsScheduledActions.size() > 0) {
                Iterator<ScheduledAction> it = lsScheduledActions.iterator();
                while (it.hasNext()) {
                    ScheduledAction sa = it.next();
                    if (deadAddress.equals(sa.getRequest().caller)) {
                        node.clusterManager.deregisterScheduledAction(sa);
                        sa.setValid(false);
                        it.remove();
                    }
                }
            }
        }
        if (record.getLockCount() > 0) {
            if (deadAddress.equals(record.getLockAddress())) {
                unlock(record);
            }
        }
    }

    public boolean removeMulti(Request req) {
        Record record = getRecord(req.key);
        if (record == null) return false;
        boolean removed = false;
        if (req.value == null) {
            removed = true;
            markAsRemoved(record);
        } else {
            if (record.containsValue(req.value)) {
                if (record.getMultiValues() != null) {
                    Iterator<Data> itValues = record.getMultiValues().iterator();
                    while (itValues.hasNext()) {
                        Data value = itValues.next();
                        if (req.value.equals(value)) {
                            itValues.remove();
                            removed = true;
                        }
                    }
                }
            }
        }
        if (req.txnId != -1) {
            unlock(record);
        }
        if (removed) {
            record.incrementVersion();
            concurrentMapManager.fireMapEvent(mapListeners, getName(), EntryEvent.TYPE_REMOVED, record.getKey(), req.value, record.getMapListeners(), req.caller);
            logger.log(Level.FINEST, record.getValue() + " RemoveMulti " + record.getMultiValues());
        }
        req.version = record.getVersion();
        if (record.valueCount() == 0) {
            markAsRemoved(record);
        }
        return removed;
    }

    public boolean putMulti(Request req) {
        Record record = getRecord(req.key);
        boolean added = true;
        if (record == null) {
            record = toRecord(req);
        } else {
            if (!record.isActive()) {
                markAsActive(record);
            }
            if (record.containsValue(req.value)) {
                added = false;
            }
        }
        if (added) {
            Data value = req.value;
            updateIndexes(record);
            record.addValue(value);
            record.incrementVersion();
            concurrentMapManager.fireMapEvent(mapListeners, getName(), EntryEvent.TYPE_ADDED, record.getKey(), value, record.getMapListeners(), req.caller);
        }
        if (req.txnId != -1) {
            unlock(record);
        }
        logger.log(Level.FINEST, record.getValue() + " PutMulti " + record.getMultiValues());
        req.clearForResponse();
        req.version = record.getVersion();
        return added;
    }

    public void doAtomic(Request req) {
        Record record = getRecord(req.key);
        if (record == null) {
            record = createNewRecord(req.key, toData(0L));
            mapRecords.put(req.key, record);
        }
        if (req.operation == ATOMIC_NUMBER_GET_AND_SET) {
            req.response = record.getValue();
            record.setValue(toData(req.longValue));
        } else if (req.operation == ATOMIC_NUMBER_ADD_AND_GET) {
            record.setValue(IOUtil.addDelta(record.getValue(), req.longValue));
            req.response = record.getValue();
        } else if (req.operation == ATOMIC_NUMBER_GET_AND_ADD) {
            req.response = record.getValue();
            record.setValue(IOUtil.addDelta(record.getValue(), req.longValue));
        } else if (req.operation == ATOMIC_NUMBER_COMPARE_AND_SET) {
            if (record.getValue().equals(req.value)) {
                record.setValue(toData(req.longValue));
                req.response = Boolean.TRUE;
            } else {
                req.response = Boolean.FALSE;
            }
            req.value = null;
        }
    }

    public void put(Request req) {
        long now = System.currentTimeMillis();
        if (mapRecords.size() >= maxSize) {
            //todo
        }
        if (req.value == null) {
            req.value = new Data();
        }
        Record record = getRecord(req.key);
        if (record != null && !record.isValid(now)) {
            record.setValue(null);
            record.setMultiValues(null);
        }
        if (req.operation == CONCURRENT_MAP_PUT_IF_ABSENT) {
            if (record != null && record.isActive() && record.isValid(now) && record.getValue() != null) {
                req.clearForResponse();
                req.response = record.getValue();
                return;
            }
        } else if (req.operation == CONCURRENT_MAP_REPLACE_IF_NOT_NULL) {
            if (record == null || !record.isActive() || !record.isValid(now) || record.getValue() == null) {
                return;
            }
        } else if (req.operation == CONCURRENT_MAP_REPLACE_IF_SAME) {
            if (record == null || !record.isActive() || !record.isValid(now)) {
                req.response = Boolean.FALSE;
                return;
            }
            MultiData multiData = (MultiData) toObject(req.value);
            if (multiData == null || multiData.size() != 2) {
                throw new RuntimeException("Illegal replaceIfSame argument: " + multiData);
            }
            Data expectedOldValue = multiData.getData(0);
            req.value = multiData.getData(1);
            if (!record.getValue().equals(expectedOldValue)) {
                req.response = Boolean.FALSE;
                return;
            }
        }
        Data oldValue = null;
        if (record == null) {
            record = createNewRecord(req.key, req.value);
            mapRecords.put(req.key, record);
        } else {
            markAsActive(record);
            oldValue = (record.isValid(now)) ? record.getValue() : null;
            record.setValue(req.value);
            record.incrementVersion();
            record.setLastUpdated();
        }
        if (req.ttl > 0) {
            record.setExpirationTime(req.ttl);
            ttlPerRecord = true;
        }
        if (oldValue == null) {
            concurrentMapManager.fireMapEvent(mapListeners, getName(), EntryEvent.TYPE_ADDED, record, req.caller);
        } else {
            fireInvalidation(record);
            concurrentMapManager.fireMapEvent(mapListeners, getName(), EntryEvent.TYPE_UPDATED, record, req.caller);
        }
        if (req.txnId != -1) {
            unlock(record);
        }
        record.setIndexes(req.indexes, req.indexTypes);
        updateIndexes(record);
        markAsDirty(record);
        req.clearForResponse();
        req.version = record.getVersion();
        req.longValue = record.getCopyCount();
        if (req.operation == CONCURRENT_MAP_REPLACE_IF_SAME) {
            req.response = Boolean.TRUE;
        } else {
            req.response = oldValue;
        }
    }

    private void executeStoreUpdate(final Set<Record> dirtyRecords) {
        if (dirtyRecords.size() > 0) {
            concurrentMapManager.node.executorManager.executeStoreTask(new Runnable() {
                public void run() {
                    try {
                        Set<Object> keysToDelete = new HashSet<Object>();
                        Map<Object, Object> updates = new HashMap<Object, Object>();
                        for (Record dirtyRecord : dirtyRecords) {
                            if (!dirtyRecord.isActive()) {
                                keysToDelete.add(dirtyRecord.getRecordEntry().getKey());
                            } else {
                                Map.Entry entry = dirtyRecord.getRecordEntry();
                                updates.put(entry.getKey(), entry.getValue());
                            }
                        }
                        if (keysToDelete.size() == 1) {
                            store.delete(keysToDelete.iterator().next());
                        } else if (keysToDelete.size() > 1) {
                            store.deleteAll(keysToDelete);
                        }
                        if (updates.size() == 1) {
                            Map.Entry entry = updates.entrySet().iterator().next();
                            store.store(entry.getKey(), entry.getValue());
                        } else if (updates.size() > 1) {
                            store.storeAll(updates);
                        }
                    } catch (Exception e) {
                        for (Record dirtyRecord : dirtyRecords) {
                            dirtyRecord.setDirty(true);
                        }
                    }
                }
            });
        }
    }

    LocalMapStatsImpl getLocalMapStats() {
        LocalMapStatsImpl localMapStats = new LocalMapStatsImpl();
        long now = System.currentTimeMillis();
        int ownedEntryCount = 0;
        int backupEntryCount = 0;
        int markedAsRemovedEntryCount = 0;
        int ownedEntryMemoryCost = 0;
        int backupEntryMemoryCost = 0;
        int markedAsRemovedMemoryCost = 0;
        int hits = 0;
        int lockedEntryCount = 0;
        int lockWaitCount = 0;
        ClusterImpl clusterImpl = node.getClusterImpl();
        final Collection<Record> records = mapRecords.values();
        final PartitionServiceImpl partitionService = concurrentMapManager.partitionManager.partitionServiceImpl;
        for (Record record : records) {
            if (!record.isActive() || !record.isValid(now)) {
                markedAsRemovedEntryCount++;
                markedAsRemovedMemoryCost += record.getCost();
            } else {
                PartitionServiceImpl.PartitionProxy partition = partitionService.getPartition(record.getBlockId());
                Member owner = partition.getOwner();
                if (owner != null && !partition.isMigrating()) {
                    boolean owned = owner.localMember();
                    if (owned) {
                        ownedEntryCount += record.valueCount();
                        ownedEntryMemoryCost += record.getCost();
                        localMapStats.setLastAccessTime(record.getLastAccessTime());
                        localMapStats.setLastUpdateTime(record.getLastUpdateTime());
                        hits += record.getHits();
                        if (record.isLocked()) {
                            lockedEntryCount++;
                            lockWaitCount += record.getScheduledActionCount();
                        }
                    } else {
                        Member ownerEventual = partition.getEventualOwner();
                        boolean backup = false;
                        if (ownerEventual != null && !owner.localMember()) {
                            int distance = node.getClusterImpl().getDistanceFrom(ownerEventual);
                            backup = (distance != -1 && distance <= getBackupCount());
                        }
                        if (backup && !shouldPurgeRecord(record, now)) {
                            backupEntryCount += record.valueCount();
                            backupEntryMemoryCost += record.getCost();
                        } else {
                            markedAsRemovedEntryCount++;
                            markedAsRemovedMemoryCost += record.getCost();
                        }
                    }
                }
            }
        }
        localMapStats.setMarkedAsRemovedEntryCount(zeroOrPositive(markedAsRemovedEntryCount));
        localMapStats.setMarkedAsRemovedMemoryCost(zeroOrPositive(markedAsRemovedMemoryCost));
        localMapStats.setLockWaitCount(zeroOrPositive(lockWaitCount));
        localMapStats.setLockedEntryCount(zeroOrPositive(lockedEntryCount));
        localMapStats.setHits(zeroOrPositive(hits));
        localMapStats.setOwnedEntryCount(zeroOrPositive(ownedEntryCount));
        localMapStats.setBackupEntryCount(zeroOrPositive(backupEntryCount));
        localMapStats.setOwnedEntryMemoryCost(zeroOrPositive(ownedEntryMemoryCost));
        localMapStats.setBackupEntryMemoryCost(zeroOrPositive(backupEntryMemoryCost));
        localMapStats.setLastEvictionTime(zeroOrPositive(clusterImpl.getClusterTimeFor(lastEvictionTime)));
        localMapStats.setCreationTime(zeroOrPositive(clusterImpl.getClusterTimeFor(creationTime)));
        return localMapStats;
    }

    private static long zeroOrPositive(long value) {
        return (value > 0) ? value : 0;
    }

    void startCleanup() {
        final long now = System.currentTimeMillis();
        if (locallyOwnedMap != null) {
            locallyOwnedMap.evict(now);
        }
        if (mapNearCache != null) {
            mapNearCache.evict(now, false);
        }
        final Set<Record> recordsDirty = new HashSet<Record>();
        final Set<Record> recordsUnknown = new HashSet<Record>();
        final Set<Record> recordsToPurge = new HashSet<Record>();
        final Set<Record> recordsToEvict = new HashSet<Record>();
        final Set<Record> sortedRecords = new TreeSet<Record>(evictionComparator);
        final Collection<Record> records = mapRecords.values();
        final int clusterMemberSize = node.getClusterImpl().getMembers().size();
        final int memberCount = (clusterMemberSize == 0) ? 1 : clusterMemberSize;
        final int maxSizePerJVM = maxSize / memberCount;
        final boolean evictionAware = evictionComparator != null && maxSizePerJVM > 0;
        final PartitionServiceImpl partitionService = concurrentMapManager.partitionManager.partitionServiceImpl;
        int recordsStillOwned = 0;
        int backupPurgeCount = 0;
        for (Record record : records) {
            PartitionServiceImpl.PartitionProxy partition = partitionService.getPartition(record.getBlockId());
            Member owner = partition.getOwner();
            if (owner != null && !partition.isMigrating()) {
                boolean owned = owner.localMember();
                if (owned) {
                    if (store != null && writeDelayMillis > 0 && record.isDirty()) {
                        if (now > record.getWriteTime()) {
                            recordsDirty.add(record);
                            record.setDirty(false);
                        }
                    } else if (shouldPurgeRecord(record, now)) {
                        recordsToPurge.add(record);  // removed records
                    } else if (record.isActive() && !record.isValid(now)) {
                        recordsToEvict.add(record);  // expired records
                    } else if (evictionAware && record.isActive() && record.isEvictable()) {
                        sortedRecords.add(record);   // sorting for eviction
                        recordsStillOwned++;
                    } else {
                        recordsStillOwned++;
                    }
                } else {
                    Member ownerEventual = partition.getEventualOwner();
                    boolean backup = false;
                    if (ownerEventual != null && owner != null && !owner.localMember()) {
                        int distance = node.getClusterImpl().getDistanceFrom(ownerEventual);
                        backup = (distance != -1 && distance <= getBackupCount());
                    }
                    if (backup) {
                        if (shouldPurgeRecord(record, now)) {
                            recordsToPurge.add(record);
                            backupPurgeCount++;
                        }
                    } else {
                        recordsUnknown.add(record);
                    }
                }
            }
        }
        if (evictionAware && maxSizePerJVM < recordsStillOwned) {
            int numberOfRecordsToEvict = (int) (recordsStillOwned * evictionRate);
            int evictedCount = 0;
            for (Record record : sortedRecords) {
                if (record.isActive() && record.isEvictable()) {
                    recordsToEvict.add(record);
                    if (++evictedCount >= numberOfRecordsToEvict) {
                        break;
                    }
                }
            }
        }
        Level levelLog = (concurrentMapManager.LOG_STATE) ? Level.INFO : Level.FINEST;
        logger.log(levelLog, name + " Cleanup "
                + ", dirty:" + recordsDirty.size()
                + ", purge:" + recordsToPurge.size()
                + ", evict:" + recordsToEvict.size()
                + ", unknown:" + recordsUnknown.size()
                + ", stillOwned:" + recordsStillOwned
                + ", backupPurge:" + backupPurgeCount
        );
        executeStoreUpdate(recordsDirty);
        executeEviction(recordsToEvict);
        executePurge(recordsToPurge);
        executePurgeUnknowns(recordsUnknown);
    }

    private void executePurgeUnknowns(final Set<Record> recordsUnknown) {
        if (recordsUnknown.size() > 0) {
            concurrentMapManager.enqueueAndReturn(new Processable() {
                public void process() {
                    purgeIfNotOwnedOrBackup(recordsUnknown);
                }
            });
        }
    }

    private void executePurge(final Set<Record> recordsToPurge) {
        if (recordsToPurge.size() > 0) {
            concurrentMapManager.enqueueAndReturn(new Processable() {
                public void process() {
                    final long now = System.currentTimeMillis();
                    for (Record recordToPurge : recordsToPurge) {
                        if (shouldPurgeRecord(recordToPurge, now)) {
                            removeAndPurgeRecord(recordToPurge);
                        }
                    }
                }
            });
        }
    }

    boolean shouldPurgeRecord(Record record, long now) {
        return !record.isActive() && shouldRemove(record, now);
    }

    private void executeEviction(Collection<Record> lsRecordsToEvict) {
        if (lsRecordsToEvict != null && lsRecordsToEvict.size() > 0) {
            logger.log(Level.FINEST, lsRecordsToEvict.size() + " evicting");
            for (final Record recordToEvict : lsRecordsToEvict) {
                concurrentMapManager.evictAsync(recordToEvict.getName(), recordToEvict.getKey());
            }
        }
    }

    void fireInvalidation(Record record) {
        if (mapNearCache != null && mapNearCache.shouldInvalidateOnChange()) {
            for (MemberImpl member : concurrentMapManager.lsMembers) {
                if (!member.localMember()) {
                    if (member.getAddress() != null) {
                        Packet packet = concurrentMapManager.obtainPacket();
                        packet.name = getName();
                        packet.setKey(record.getKey());
                        packet.operation = ClusterOperation.CONCURRENT_MAP_INVALIDATE;
                        boolean sent = concurrentMapManager.send(packet, member.getAddress());
                        if (!sent) {
                            concurrentMapManager.releasePacket(packet);
                        }
                    }
                }
            }
            mapNearCache.invalidate(record.getKey());
        }
    }

    Record toRecord(Request req) {
        Record record = mapRecords.get(req.key);
        if (record == null) {
            if (isMultiMap()) {
                record = createNewRecord(req.key, null);
                record.addValue(req.value);
            } else {
                record = createNewRecord(req.key, req.value);
            }
            mapRecords.put(req.key, record);
        } else {
            if (req.value != null) {
                if (isMultiMap()) {
                    record.addValue(req.value);
                } else {
                    record.setValue(req.value);
                }
            }
        }
        record.setIndexes(req.indexes, req.indexTypes);
        record.setCopyCount((int) req.longValue);
        if (req.lockCount >= 0) {
            DistributedLock lock = new DistributedLock(req.lockAddress, req.lockThreadId, req.lockCount);
            record.setLock(lock);
        }
        return record;
    }

    public boolean removeItem(Request req) {
        Record record = mapRecords.get(req.key);
        if (record == null) {
            return false;
        }
        if (req.txnId != -1) {
            unlock(record);
        }
        boolean removed = false;
        if (record.getCopyCount() > 0) {
            record.decrementCopyCount();
            removed = true;
        } else if (record.getValue() != null) {
            removed = true;
        } else if (record.getMultiValues() != null) {
            removed = true;
        }
        if (removed) {
            concurrentMapManager.fireMapEvent(mapListeners, getName(), EntryEvent.TYPE_REMOVED, record, req.caller);
            record.incrementVersion();
        }
        req.version = record.getVersion();
        req.longValue = record.getCopyCount();
        markAsRemoved(record);
        return true;
    }

    boolean evict(Request req) {
        Record record = getRecord(req.key);
        long now = System.currentTimeMillis();
        if (record != null && record.isActive() && record.valueCount() > 0) {
            fireInvalidation(record);
            concurrentMapManager.fireMapEvent(mapListeners, getName(), EntryEvent.TYPE_EVICTED, record.getKey(), record.getValue(), record.getMapListeners(), req.caller);
            record.incrementVersion();
            markAsRemoved(record);
            req.clearForResponse();
            req.version = record.getVersion();
            req.longValue = record.getCopyCount();
            lastEvictionTime = now;
            return true;
        }
        return false;
    }

    public void remove(Request req) {
        Record record = mapRecords.get(req.key);
        if (record == null) {
            req.clearForResponse();
            return;
        }
        if (req.txnId != -1) {
            unlock(record);
        }
        if (!record.isActive()) {
            return;
        }
        if (!record.isValid()) {
            if (record.isEvictable()) {
                return;
            }
        }
        if (req.value != null) {
            if (record.getValue() != null) {
                if (!record.getValue().equals(req.value)) {
                    return;
                }
            }
        }
        Data oldValue = record.getValue();
        if (oldValue == null && record.getMultiValues() != null && record.getMultiValues().size() > 0) {
            Values values = new Values(record.getMultiValues());
            oldValue = toData(values);
        }
        if (oldValue != null) {
            fireInvalidation(record);
            concurrentMapManager.fireMapEvent(mapListeners, getName(), EntryEvent.TYPE_REMOVED, record.getKey(), oldValue, record.getMapListeners(), req.caller);
            record.incrementVersion();
        }
        markAsRemoved(record);
        req.clearForResponse();
        req.version = record.getVersion();
        req.longValue = record.getCopyCount();
        req.response = oldValue;
    }

    void reset() {
        if (locallyOwnedMap != null) {
            locallyOwnedMap.reset();
        }
        mapRecords.clear();
        mapIndexService.clear();
    }

    void markAsDirty(Record record) {
        if (!record.isDirty()) {
            record.setDirty(true);
            if (writeDelayMillis > 0) {
                record.setWriteTime(System.currentTimeMillis() + writeDelayMillis);
            }
        }
    }

    void markAsActive(Record record) {
        long now = System.currentTimeMillis();
        if (!record.isActive() || !record.isValid(now)) {
            record.setActive();
            record.setCreationTime(now);
            record.setExpirationTime(ttl);
        }
    }

    boolean shouldRemove(Record record, long now) {
        return record.isRemovable() && ((now - record.getRemoveTime()) > removeDelayMillis);
    }

    void markAsRemoved(Record record) {
        if (record.isActive()) {
            record.markRemoved();
            mapRecordEntries.remove(record.getId());
        }
        record.setValue(null);
        record.setMultiValues(null);
        updateIndexes(record);
        markAsDirty(record);
    }

    void removeAndPurgeRecord(Record record) {
        mapRecordEntries.remove(record.getId());
        mapRecords.remove(record.getKey());
        mapIndexService.remove(record);
    }

    void updateIndexes(Record record) {
        mapIndexService.index(record);
    }

    Record createNewRecord(Data key, Data value) {
        if (key == null || key.size() == 0) {
            throw new RuntimeException("Cannot create record from a 0 size key: " + key);
        }
        int blockId = concurrentMapManager.getBlockId(key);
        return new Record(this, blockId, key, value, ttl, maxIdle, concurrentMapManager.newRecordId());
    }

    public void addListener(Data key, Address address, boolean includeValue) {
        if (key == null || key.size() == 0) {
            mapListeners.put(address, includeValue);
        } else {
            Record rec = getRecord(key);
            if (rec == null) {
                rec = createNewRecord(key, null);
                mapRecords.put(key, rec);
            }
            rec.addListener(address, includeValue);
        }
    }

    public void removeListener(Data key, Address address) {
        if (key == null || key.size() == 0) {
            mapListeners.remove(address);
        } else {
            Record rec = getRecord(key);
            if (rec != null) {
                rec.removeListener(address);
            }
        }
    }

    public void appendState(StringBuffer sbState) {
        sbState.append("\nCMap [");
        sbState.append(name);
        sbState.append("] r:");
        sbState.append(mapRecords.size());
        if (locallyOwnedMap != null) {
            locallyOwnedMap.appendState(sbState);
        }
        if (mapNearCache != null) {
            mapNearCache.appendState(sbState);
        }
        mapIndexService.appendState(sbState);
    }

    public static class Values implements Collection, DataSerializable {
        Collection<Data> lsValues = null;

        public Values() {
        }

        public Values(Collection<Data> lsValues) {
            super();
            this.lsValues = lsValues;
        }

        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public boolean contains(Object o) {
            if (o == null) {
                throw new IllegalArgumentException("Contains cannot have null argument.");
            }
            Iterator it = iterator();
            while (it.hasNext()) {
                Object v = it.next();
                if (o.equals(v)) {
                    return true;
                }
            }
            return false;
        }

        public boolean containsAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public boolean isEmpty() {
            return (size() == 0);
        }

        public Iterator iterator() {
            return new ValueIterator(lsValues.iterator());
        }

        class ValueIterator implements Iterator {
            final Iterator<Data> it;

            public ValueIterator(Iterator<Data> it) {
                super();
                this.it = it;
            }

            public boolean hasNext() {
                return it.hasNext();
            }

            public Object next() {
                Data value = it.next();
                return toObject(value);
            }

            public void remove() {
                it.remove();
            }
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            return (lsValues == null) ? 0 : lsValues.size();
        }

        public Object[] toArray() {
            if (size() == 0) {
                return null;
            }
            return toArray(new Object[size()]);
        }

        public Object[] toArray(Object[] a) {
            int size = size();
            if (size == 0) {
                return null;
            }
            if (a == null || a.length < size) {
                a = new Object[size];
            }
            Iterator<Data> it = lsValues.iterator();
            int index = 0;
            while (it.hasNext()) {
                a[index++] = toObject(it.next());
            }
            return a;
        }

        public void readData(DataInput in) throws IOException {
            int size = in.readInt();
            lsValues = new ArrayList<Data>(size);
            for (int i = 0; i < size; i++) {
                Data data = new Data();
                data.readData(in);
                lsValues.add(data);
            }
        }

        public void writeData(DataOutput out) throws IOException {
            int size = (lsValues == null) ? 0 : lsValues.size();
            out.writeInt(size);
            if (size > 0) {
                for (Data data : lsValues) {
                    data.writeData(out);
                }
            }
        }
    }

    public static class CMapEntry implements HazelcastInstanceAware, MapEntry, DataSerializable {
        private long cost = 0;
        private long expirationTime = 0;
        private long lastAccessTime = 0;
        private long lastUpdateTime = 0;
        private long creationTime = 0;
        private long version = 0;
        private int hits = 0;
        private boolean valid = true;
        private String name = null;
        private Object key = null;
        private Object value = null;
        private HazelcastInstance hazelcastInstance = null;

        public CMapEntry() {
        }

        public CMapEntry(long cost, long expirationTime, long lastAccessTime, long lastUpdateTime, long creationTime, long version, int hits, boolean valid) {
            this.cost = cost;
            this.expirationTime = expirationTime;
            this.lastAccessTime = lastAccessTime;
            this.lastUpdateTime = lastUpdateTime;
            this.creationTime = creationTime;
            this.version = version;
            this.hits = hits;
            this.valid = valid;
        }

        public void writeData(DataOutput out) throws IOException {
            out.writeLong(cost);
            out.writeLong(expirationTime);
            out.writeLong(lastAccessTime);
            out.writeLong(lastUpdateTime);
            out.writeLong(creationTime);
            out.writeLong(version);
            out.writeInt(hits);
            out.writeBoolean(valid);
        }

        public void readData(DataInput in) throws IOException {
            cost = in.readLong();
            expirationTime = in.readLong();
            lastAccessTime = in.readLong();
            lastUpdateTime = in.readLong();
            creationTime = in.readLong();
            version = in.readLong();
            hits = in.readInt();
            valid = in.readBoolean();
        }

        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hazelcastInstance = hazelcastInstance;
        }

        public void set(String name, Object key) {
            this.name = name;
            this.key = key;
        }

        public long getCost() {
            return cost;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }

        public int getHits() {
            return hits;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public long getVersion() {
            return version;
        }

        public boolean isValid() {
            return valid;
        }

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            if (value == null) {
                FactoryImpl factory = (FactoryImpl) hazelcastInstance;
                value = ((MProxy) factory.getOrCreateProxyByName(name)).get(key);
            }
            return value;
        }

        public Object setValue(Object value) {
            Object oldValue = this.value;
            FactoryImpl factory = (FactoryImpl) hazelcastInstance;
            ((MProxy) factory.getOrCreateProxyByName(name)).put(key, value);
            return oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CMapEntry cMapEntry = (CMapEntry) o;
            return !(key != null ? !key.equals(cMapEntry.key) : cMapEntry.key != null) &&
                    !(name != null ? !name.equals(cMapEntry.name) : cMapEntry.name != null);
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (key != null ? key.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append("MapEntry");
            sb.append("{key=").append(key);
            sb.append(", valid=").append(valid);
            sb.append(", hits=").append(hits);
            sb.append(", version=").append(version);
            sb.append(", creationTime=").append(creationTime);
            sb.append(", lastUpdateTime=").append(lastUpdateTime);
            sb.append(", lastAccessTime=").append(lastAccessTime);
            sb.append(", expirationTime=").append(expirationTime);
            sb.append(", cost=").append(cost);
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return "CMap [" + getName() + "] size=" + size();
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public MapIndexService getMapIndexService() {
        return mapIndexService;
    }
}
