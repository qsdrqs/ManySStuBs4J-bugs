/* 
 * Copyright (c) 2007-2008, Hazel Ltd. All Rights Reserved.
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

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.Transaction;
import com.hazelcast.impl.ClusterManager.AbstractRemotelyProcessable;
import static com.hazelcast.impl.Constants.ConcurrentMapOperations.*;
import static com.hazelcast.impl.Constants.ResponseTypes.RESPONSE_REDO;
import static com.hazelcast.impl.Constants.Timeouts.DEFAULT_TXN_TIMEOUT;
import static com.hazelcast.impl.SortedHashMap.OrderingType;
import com.hazelcast.nio.Address;
import static com.hazelcast.nio.BufferUtil.*;
import com.hazelcast.nio.Data;
import com.hazelcast.nio.DataSerializable;
import com.hazelcast.nio.PacketQueue;
import com.hazelcast.nio.PacketQueue.Packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

class ConcurrentMapManager extends BaseManager {
    private static ConcurrentMapManager instance = new ConcurrentMapManager();

    private ConcurrentMapManager() {
        //checkIfMigrating, targetAware, schedulable, returnsObject
        ClusterService.get().registerPacketProcessor(OP_CMAP_GET,
                new DefaultPacketProcessor(false, true, false, true) {
                    void handle(Request request) {
                        doGet(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_PUT,
                new DefaultPacketProcessor(false, true, true, true) {
                    void handle(Request request) {
                        doPut(request, true);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_BACKUP_PUT,
                new DefaultPacketProcessor() {
                    void handle(Request request) {
                        doBackup(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_PUT_IF_ABSENT,
                new DefaultPacketProcessor(false, true, true, true) {
                    void handle(Request request) {
                        doPut(request, true);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_REPLACE_IF_NOT_NULL,
                new DefaultPacketProcessor(false, true, true, true) {
                    void handle(Request request) {
                        doPut(request, true);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_BACKUP_ADD,
                new DefaultPacketProcessor() {
                    void handle(Request request) {
                        doBackup(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_BACKUP_REMOVE,
                new DefaultPacketProcessor(false, false, false, false) {
                    void handle(Request request) {
                        doBackup(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_BACKUP_LOCK,
                new DefaultPacketProcessor(false, false, false, false) {
                    void handle(Request request) {
                        doLock(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_PUT_MULTI,
                new DefaultPacketProcessor(false, true, true, false) {
                    void handle(Request request) {
                        doPut(request, false);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_REMOVE,
                new DefaultPacketProcessor(false, true, true, true) {
                    void handle(Request request) {
                        doRemove(request, true);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_REMOVE_MULTI,
                new DefaultPacketProcessor(false, true, true, true) {
                    void handle(Request request) {
                        doRemoveMulti(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_REMOVE_ITEM,
                new DefaultPacketProcessor(false, true, true, false) {
                    void handle(Request request) {
                        doRemove(request, false);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_REMOVE_IF_SAME,
                new DefaultPacketProcessor(false, true, true, false) {
                    void handle(Request request) {
                        doRemove(request, true);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_LOCK,
                new DefaultPacketProcessor(false, true, true, false) {
                    void handle(Request request) {
                        doLock(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_LOCK_RETURN_OLD,
                new DefaultPacketProcessor(false, true, true, false) {
                    void handle(Request request) {
                        doLock(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_UNLOCK,
                new DefaultPacketProcessor(false, true, true, false) {
                    void handle(Request request) {
                        doLock(request);
                    }
                });

        ClusterService.get().registerPacketProcessor(OP_CMAP_SIZE,
                new PacketProcessor() {
                    public void process(PacketQueue.Packet packet) {
                        handleSize(packet);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_ITERATE,
                new DefaultPacketProcessor(true, false, false, true) {
                    void handle(Request request) {
                        CMap cmap = getMap(request.name);
                        cmap.getEntries(remoteReq);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_ITERATE_KEYS,
                new DefaultPacketProcessor(true, false, false, true) {
                    void handle(Request request) {
                        CMap cmap = getMap(request.name);
                        cmap.getEntries(remoteReq);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_ADD_TO_LIST,
                new DefaultPacketProcessor(false, true, false, false) {
                    void handle(Request request) {
                        doAdd(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_ADD_TO_SET,
                new DefaultPacketProcessor(false, true, false, false) {
                    void handle(Request request) {
                        doAdd(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_CONTAINS,
                new DefaultPacketProcessor(true, false, false, false) {
                    void handle(Request request) {
                        doContains(request);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_BLOCK_INFO,
                new PacketProcessor() {
                    public void process(PacketQueue.Packet packet) {
                        handleBlockInfo(packet);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_BLOCKS,
                new PacketProcessor() {
                    public void process(Packet packet) {
                        handleBlocks(packet);
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_MIGRATION_COMPLETE,
                new PacketProcessor() {
                    public void process(Packet packet) {
                        doMigrationComplete(packet.conn.getEndPoint());
                    }
                });
        ClusterService.get().registerPacketProcessor(OP_CMAP_MIGRATE_RECORD,
                new PacketProcessor() {
                    public void process(PacketQueue.Packet packet) {
                        handleMigrateRecord(packet);
                    }
                });
    }

    public static ConcurrentMapManager get() {
        return instance;
    }

    private static final int BLOCK_COUNT = 271;

    private Request remoteReq = new Request();
    //    private Map<Integer, Block> mapBlocks = new HashMap<Integer, Block>(BLOCK_COUNT);
    Block[] blocks = new Block[BLOCK_COUNT];
    private Map<String, CMap> maps = new HashMap<String, CMap>(10);

    public void syncForDead(Address deadAddress) {
//        Collection<Block> blocks = mapBlocks.values();
        for (Block block : blocks) {
            if (deadAddress.equals(block.owner)) {
                MemberImpl member = getNextMemberBeforeSync(block.owner, true, 1);
                block.owner = (member == null) ? thisAddress : member.getAddress();
            }
            if (block.migrationAddress != null) {
                if (deadAddress.equals(block.migrationAddress)) {
                    MemberImpl member = getNextMemberBeforeSync(block.migrationAddress, true, 1);
                    block.migrationAddress = (member == null) ? thisAddress : member
                            .getAddress();
                }
            }
        }
        Collection<CMap> cmaps = maps.values();
        for (CMap map : cmaps) {
            Collection<Record> records = map.mapRecords.values();
            for (Record record : records) {
                record.onDisconnect(deadAddress);
            }
        }
        doResetRecords(deadAddress);
    }

    public void syncForAdd() {
        if (isMaster()) {
            // make sue that all blocks are actually created
            for (int i = 0; i < BLOCK_COUNT; i++) {
                Block block = blocks[i];
                if (block == null) {
                    getOrCreateBlock(i);
                }
            }

            List<Block> lsBlocksToRedistribute = new ArrayList<Block>();
            Map<Address, Integer> addressBlocks = new HashMap<Address, Integer>();
            int storageEnabledMemberCount = 0;
            for (MemberImpl member : lsMembers) {
                if (!member.isSuperClient()) {
                    addressBlocks.put(member.getAddress(), 0);
                    storageEnabledMemberCount++;
                }
            }
            if (storageEnabledMemberCount == 0)
                return;


            int aveBlockOwnCount = BLOCK_COUNT / (storageEnabledMemberCount);
            for (Block block : blocks) {
                if (block.owner == null) {
                    lsBlocksToRedistribute.add(block);
                } else {
                    if (!block.isMigrating()) {
                        Integer countInt = addressBlocks.get(block.owner);
                        int count = (countInt == null) ? 0 : countInt;
                        if (count >= aveBlockOwnCount) {
                            lsBlocksToRedistribute.add(block);
                        } else {
                            count++;
                            addressBlocks.put(block.owner, count);
                        }
                    }
                }
            }

            Set<Address> allAddress = addressBlocks.keySet();
            setNewMembers:
            for (Address address : allAddress) {
                Integer countInt = addressBlocks.get(address);
                int count = (countInt == null) ? 0 : countInt.intValue();
                while (count < aveBlockOwnCount) {
                    if (lsBlocksToRedistribute.size() > 0) {
                        Block blockToMigrate = lsBlocksToRedistribute.remove(0);
                        if (blockToMigrate.owner == null) {
                            blockToMigrate.owner = address;
                        } else {
                            blockToMigrate.migrationAddress = address;
                            if (blockToMigrate.owner.equals(blockToMigrate.migrationAddress)) {
                                blockToMigrate.migrationAddress = null;
                            }
                        }
                        count++;
                    } else {
                        break setNewMembers;
                    }
                }
            }
            int addressIndex = 0;
            for (int i = 0; i < BLOCK_COUNT; i++) {
                Block block = blocks[i];
                if (block.owner == null) {
                    int index = addressIndex++ % addressBlocks.size();
                    block.owner = (Address) addressBlocks.keySet().toArray()[index];
                }
            }

            Data dataAllBlocks = null;
            for (MemberImpl member : lsMembers) {
                if (!member.localMember()) {
                    if (dataAllBlocks == null) {
                        Blocks allBlocks = new Blocks();
                        for (Block block : blocks) {
                            allBlocks.addBlock(block);
                        }
                        dataAllBlocks = ThreadContext.get().toData(allBlocks);
                    }
                    send("blocks", OP_CMAP_BLOCKS, dataAllBlocks, member.getAddress());
                }
            }
            doResetRecords(null);
            if (DEBUG) {
                printBlocks();
            }
        }
    }

    abstract class MBooleanOp extends MTargetAwareOp {
        @Override
        void handleNoneRedoResponse(PacketQueue.Packet packet) {
            handleBooleanNoneRedoResponse(packet);
        }
    }

    class ScheduledLockAction extends ScheduledAction {
        Record record = null;

        public ScheduledLockAction(Request reqScheduled, Record record) {
            super(reqScheduled);
            this.record = record;
        }

        @Override
        public boolean consume() {
            record.lock(request.lockThreadId, request.lockAddress);
            request.lockCount = record.lockCount;
            request.response = Boolean.TRUE;
            returnScheduledAsBoolean(request);
            return true;
        }

        public void onExpire() {
            request.response = Boolean.FALSE;
            returnScheduledAsBoolean(request);
        }
    }

    class MLock extends MBackupAwareOp {
        Data oldValue = null;

        public boolean unlock(String name, Object key, long timeout, long txnId) {
            boolean unlocked = booleanCall(OP_CMAP_UNLOCK, name, key, null, timeout, txnId, -1);
            if (unlocked) {
                backup(OP_CMAP_BACKUP_LOCK);
            }
            return unlocked;
        }

        public boolean lock(String name, Object key, long timeout, long txnId) {
            boolean locked = booleanCall(OP_CMAP_LOCK, name, key, null, timeout, txnId, -1);
            if (locked) {
                backup(OP_CMAP_BACKUP_LOCK);
            }
            return locked;
        }

        public boolean lockAndReturnOld(String name, Object key, long timeout, long txnId) {
            oldValue = null;
            boolean locked = booleanCall(OP_CMAP_LOCK_RETURN_OLD, name, key, null, timeout, txnId,
                    -1);
            if (locked) {
                backup(OP_CMAP_BACKUP_LOCK);
            }
            return locked;
        }

        @Override
        public void doLocalOp() {
            doLock(request);
            oldValue = request.value;
            if (!request.scheduled) {
                setResult(request.response);
            }
        }

        @Override
        void handleNoneRedoResponse(PacketQueue.Packet packet) {
            if (request.operation == OP_CMAP_LOCK_RETURN_OLD) {
                oldValue = doTake(packet.value);
            }
            super.handleBooleanNoneRedoResponse(packet);
        }
    }

    class MContainsKey extends MBooleanOp {

        public boolean containsEntry(String name, Object key, Object value, long txnId) {
            return booleanCall(OP_CMAP_CONTAINS, name, key, value, 0, txnId, -1);
        }

        public boolean containsKey(String name, Object key, long txnId) {
            return booleanCall(OP_CMAP_CONTAINS, name, key, null, 0, txnId, -1);
        }

        @Override
        public void doLocalOp() {
            doContains(request);
            setResult(request.response);
        }
    }


    class MEvict extends MBackupAwareOp {
        public boolean evict(String name, Object key) {
            Data k = (key instanceof Data) ? (Data) key : toData(key);
            request.setLocal(OP_CMAP_EVICT, name, k, null, 0, -1, -1);
            doOp();
            boolean result = getResultAsBoolean();
            if (result) {
                backup(OP_CMAP_BACKUP_REMOVE);
            }
            return result;
        }

        void handleNoneRedoResponse(final PacketQueue.Packet packet) {
            handleBooleanNoneRedoResponse(packet);
        }

        @Override
        public void process() {
            setTarget();
            if (!thisAddress.equals(target)) {
                setResult(false);
            } else {
                prepareForBackup();
                doLocalOp();
            }
        }

        @Override
        void doLocalOp() {
            doEvict(request);
            setResult(request.response);
        }
    }


    class MMigrate extends MBackupAwareOp {
        public boolean migrate(Record record) {
            copyRecordToRequest(record, request, true);
            request.operation = OP_CMAP_MIGRATE_RECORD;
            doOp();
            boolean result = getResultAsBoolean();
            backup(OP_CMAP_BACKUP_PUT);
            return result;
        }

        void handleNoneRedoResponse(final PacketQueue.Packet packet) {
            handleBooleanNoneRedoResponse(packet);
        }

        @Override
        public void setTarget() {
            target = getTarget(request.name, request.key);
            if (target == null) {
                Block block = blocks[(request.blockId)];
                if (block != null) {
                    target = block.migrationAddress;
                }
            }
        }

        @Override
        void doLocalOp() {
            doMigrate(request);
            if (!request.scheduled) {
                setResult(request.response);
            }
        }
    }


    class MGet extends MTargetAwareOp {
        public Object get(String name, Object key, long timeout, long txnId) {
            final ThreadContext tc = ThreadContext.get();
            TransactionImpl txn = tc.txn;
            if (txn != null && txn.getStatus() == Transaction.TXN_STATUS_ACTIVE) {
                if (txn.has(name, key)) {
                    return txn.get(name, key);
                }
            }
            return objectCall(OP_CMAP_GET, name, key, null, timeout, txnId, -1);
        }

        @Override
        public void doLocalOp() {
            doGet(request);
            Data value = (Data) request.response;
            setResult(value);
        }
    }

    class MRemoveItem extends MBackupAwareOp {

        public boolean removeItem(String name, Object key) {
            return removeItem(name, key, null);
        }

        public boolean removeItem(String name, Object key, Object value) {
            ThreadContext threadContext = ThreadContext.get();
            TransactionImpl txn = threadContext.txn;
            if (txn != null && txn.getStatus() == Transaction.TXN_STATUS_ACTIVE) {
                try {
                    boolean locked = false;
                    if (!txn.has(name, key)) {
                        MLock mlock = threadContext.getMLock();
                        locked = mlock
                                .lockAndReturnOld(name, key, DEFAULT_TXN_TIMEOUT, txn.getId());
                        if (!locked)
                            throwCME(key);
                        Object oldObject = null;
                        Data oldValue = mlock.oldValue;
                        if (oldValue != null) {
                            oldObject = threadContext.toObject(oldValue);
                        }
                        txn.attachRemoveOp(name, key, null, (oldObject == null));
                        return (oldObject != null);
                    } else {
                        return (txn.attachRemoveOp(name, key, null, false) != null);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                return false;
            } else {
                boolean removed = booleanCall(OP_CMAP_REMOVE_ITEM, name, key, value, 0, -1, -1);
                if (removed) {
                    backup(OP_CMAP_BACKUP_REMOVE);
                }
                return removed;
            }
        }

        @Override
        void handleNoneRedoResponse(final PacketQueue.Packet packet) {
            handleBooleanNoneRedoResponse(packet);
        }

        public void doLocalOp() {
            doRemove(request, false);
            if (!request.scheduled) {
                setResult(request.response);
            }
        }
    }

    class MRemove extends MBackupAwareOp {

        public Object remove(String name, Object key, long timeout, long txnId) {
            return txnalRemove(OP_CMAP_REMOVE, name, key, null, timeout, txnId);
        }

        public Object removeIfSame(String name, Object key, Object value, long timeout, long txnId) {
            return txnalRemove(OP_CMAP_REMOVE_IF_SAME, name, key, value, timeout, txnId);
        }

        private Object txnalRemove(int operation, String name, Object key, Object value,
                                   long timeout, long txnId) {
            ThreadContext threadContext = ThreadContext.get();
            TransactionImpl txn = threadContext.txn;
            if (txn != null && txn.getStatus() == Transaction.TXN_STATUS_ACTIVE) {
                try {
                    boolean locked = false;
                    if (!txn.has(name, key)) {
                        MLock mlock = threadContext.getMLock();
                        locked = mlock
                                .lockAndReturnOld(name, key, DEFAULT_TXN_TIMEOUT, txn.getId());
                        if (!locked)
                            throwCME(key);
                        Object oldObject = null;
                        Data oldValue = mlock.oldValue;
                        if (oldValue != null) {
                            oldObject = threadContext.toObject(oldValue);
                        }
                        txn.attachRemoveOp(name, key, value, (oldObject == null));
                        return oldObject;
                    } else {
                        return txn.attachRemoveOp(name, key, value, false);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                return null;
            } else {
                Object oldValue = objectCall(operation, name, key, value, timeout, txnId, -1);
                if (oldValue != null) {
                    backup(OP_CMAP_BACKUP_REMOVE);
                }
                return oldValue;
            }
        }

        @Override
        public void doLocalOp() {
            doRemove(request, true);
            if (!request.scheduled) {
                setResult(request.response);
            }
        }
    }

    class MDestroy {
        public void destroy(String name) {
            sendProcessableToAll(new Destroy(name), true);
        }
    }

    public void destroy(String name) {
        maps.remove(name);
    }

    class MAdd extends MBackupAwareOp {
        boolean addToList(String name, Object value) {
            Data key = ThreadContext.get().toData(value);
            boolean result = booleanCall(OP_CMAP_ADD_TO_LIST, name, key, null, 0, -1, -1);
            backup(OP_CMAP_BACKUP_ADD);
            return result;
        }

        boolean addToSet(String name, Object value) {
            Data key = ThreadContext.get().toData(value);
            boolean result = booleanCall(OP_CMAP_ADD_TO_SET, name, key, null, 0, -1, -1);
            backup(OP_CMAP_BACKUP_ADD);
            return result;
        }

        @Override
        void doLocalOp() {
            doAdd(request);
            setResult(request.response);
        }

        @Override
        void handleNoneRedoResponse(final PacketQueue.Packet packet) {
            handleBooleanNoneRedoResponse(packet);
        }
    }

    class MBackup extends MBooleanOp {
        protected Address owner = null;
        protected int distance = 0;

        public void sendBackup(int operation, Address owner, boolean hardCopy,
                               int distance, Request reqBackup) {
            this.owner = owner;
            this.distance = distance;
            request.operation = operation;
            request.setFromRequest(reqBackup, hardCopy);
            request.operation = operation;
            request.caller = thisAddress;
            doOp();

        }

        @Override
        public void process() {
            MemberImpl targetMember = getNextMemberAfter(owner, true, distance);
            if (targetMember == null) {
                setResult(Boolean.TRUE);
                return;
            }
            target = targetMember.getAddress();
            if (target.equals(thisAddress)) {
                doLocalOp();
            } else {
                invoke();
            }
        }

        @Override
        void doLocalOp() {
            doBackup(request);
            setResult(request.response);
        }
    }

    class MPutMulti extends MBackupAwareOp {

        boolean put(String name, Object key, Object value) {
            boolean result = booleanCall(OP_CMAP_PUT_MULTI, name, key, value, 0, -1, -1);
            if (result) {
                backup(OP_CMAP_BACKUP_PUT_MULTI);
            }
            return result;
        }

        @Override
        void doLocalOp() {
            doPut(request, false);
            if (!request.scheduled) {
                setResult(request.response);
            }
        }

        @Override
        void handleNoneRedoResponse(final PacketQueue.Packet packet) {
            handleBooleanNoneRedoResponse(packet);
        }
    }

    class MPut extends MBackupAwareOp {


        public Object replace(String name, Object key, Object value, long timeout, long txnId) {
            return txnalPut(OP_CMAP_REPLACE_IF_NOT_NULL, name, key, value, timeout, txnId);
        }

        public Object putIfAbsent(String name, Object key, Object value, long timeout, long txnId) {
            return txnalPut(OP_CMAP_PUT_IF_ABSENT, name, key, value, timeout, txnId);
        }

        public Object put(String name, Object key, Object value, long timeout, long txnId) {
            return txnalPut(OP_CMAP_PUT, name, key, value, timeout, txnId);
        }

        private Object txnalPut(int operation, String name, Object key, Object value, long timeout,
                                long txnId) {
            ThreadContext threadContext = ThreadContext.get();
            TransactionImpl txn = threadContext.txn;
            if (txn != null && txn.getStatus() == Transaction.TXN_STATUS_ACTIVE) {
                try {
                    boolean locked = false;
                    if (!txn.has(name, key)) {
                        MLock mlock = threadContext.getMLock();
                        locked = mlock
                                .lockAndReturnOld(name, key, DEFAULT_TXN_TIMEOUT, txn.getId());
                        if (!locked)
                            throwCME(key);
                        Object oldObject = null;
                        Data oldValue = mlock.oldValue;
                        if (oldValue != null) {
                            oldObject = threadContext.toObject(oldValue);
                        }
                        txn.attachPutOp(name, key, value, (oldObject == null));
                        return oldObject;
                    } else {
                        return txn.attachPutOp(name, key, value, false);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                return null;
            } else {
                Object oldValue = objectCall(operation, name, key, value, timeout, txnId, -1);
                backup(OP_CMAP_BACKUP_PUT);
                return oldValue;
            }
        }

        @Override
        public void doLocalOp() {
            doPut(request, true);
            if (!request.scheduled) {
                setResult(request.response);
            }
        }
    }

    class MRemoveMulti extends MBackupAwareOp {

        boolean remove(String name, Object key, Object value) {
            boolean result = booleanCall(OP_CMAP_REMOVE_MULTI, name, key, value, 0, -1, -1);
            if (result) {
                backup(OP_CMAP_BACKUP_REMOVE_MULTI);
            }
            return result;
        }

        @Override
        void doLocalOp() {
            doRemoveMulti(request);
            if (!request.scheduled) {
                setResult(request.response);
            }
        }

        @Override
        void handleNoneRedoResponse(final PacketQueue.Packet packet) {
            handleBooleanNoneRedoResponse(packet);
        }
    }


    abstract class MBackupAwareOp extends MTargetAwareOp {
        protected MBackup[] backupOps = new MBackup[3];
        protected int backupCount = 0;
        protected Request reqBackup = new Request();

        protected void backup(int operation) {
            if (backupCount > 0) {
                for (int i = 0; i < backupCount; i++) {
                    int distance = i + 1;
                    MBackup backupOp = backupOps[i];
                    if (backupOp == null) {
                        backupOp = new MBackup();
                        backupOps[i] = backupOp;
                    }
                    backupOp.sendBackup(operation, target, (distance == backupCount), distance, reqBackup);
                }
                for (int i = 0; i < backupCount; i++) {
                    MBackup backupOp = backupOps[i];
                    backupOp.getResultAsBoolean();
                }
            }
        }

        void prepareForBackup() {
            if (lsMembers.size() > 1) {
                CMap map = getMap(request.name);
                backupCount = map.getBackupCount();
                backupCount = Math.min(backupCount, lsMembers.size());
                if (backupCount > 0) {
                    reqBackup.setFromRequest(request, true);
                }
            }
        }


        @Override
        public void process() {
            prepareForBackup();
            super.process();
        }

        @Override
        void setResult(Object obj) {
            if (reqBackup.local) {
                reqBackup.version = request.version;
                reqBackup.lockCount = request.lockCount;
                reqBackup.longValue = request.longValue;
            }
            super.setResult(obj);
        }

        @Override
        public void handleBooleanNoneRedoResponse(Packet packet) {
            handleRemoteResponse(packet);
            super.handleBooleanNoneRedoResponse(packet);
        }

        @Override
        public void handleObjectNoneRedoResponse(Packet packet) {
            handleRemoteResponse(packet);
            super.handleObjectNoneRedoResponse(packet);
        }

        public void handleRemoteResponse(Packet packet) {
            reqBackup.local = false;
            reqBackup.version = packet.version;
            reqBackup.lockCount = packet.lockCount;
            reqBackup.longValue = packet.longValue;
        }
    }

    abstract class MTargetAwareOp extends TargetAwareOp {
        @Override
        void setTarget() {
            setTargetBasedOnKey();
        }

        final void setTargetBasedOnKey() {
            if (target == null) {
                target = getTarget(request.name, request.key);
            }
        }

        final void setTargetBasedOnBlockId() {
            Block block = blocks[(request.blockId)];
            if (block == null) {
                target = thisAddress;
                return;
            }
            if (block.isMigrating()) {
                target = block.migrationAddress;
            } else {
                target = block.owner;
            }
        }
    }

    void fireMapEvent(final Map<Address, Boolean> mapListeners, final String name,
                      final int eventType, final Record record) {
        fireMapEvent(mapListeners, name, eventType, record.key, record.value, record.mapListeners);
    }

    public Address getKeyOwner(String name, Data key) {
        return getTarget(name, key);
    }


    public class MContainsValue extends MMultiCall {
        boolean contains = false;
        final String name;
        final Object value;

        public MContainsValue(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        TargetAwareOp createNewTargetAwareOp(Address target) {
            return new MGetContainsValue(target);
        }

        boolean onResponse(Object response) {
            if (response == Boolean.TRUE) {
                this.contains = true;
                return false;
            }
            return true;
        }

        void onCall() {
            contains = false;
        }

        Object returnResult() {
            return contains;
        }

        class MGetContainsValue extends MMigrationAwareTargettedCall {
            public MGetContainsValue(Address target) {
                this.target = target;
                request.reset();
                setLocal(OP_CMAP_CONTAINS, name, null, value, 0, -1, -1);
            }

            @Override
            void handleNoneRedoResponse(final PacketQueue.Packet packet) {
                handleBooleanNoneRedoResponse(packet);
            }

            public void doLocalCall() {
                CMap cmap = getMap(request.name);
                request.response = cmap.contains(request);
            }
        }
    }


    public class MSize extends MMultiCall {
        int size = 0;
        final String name;

        public MSize(String name) {
            this.name = name;
        }

        TargetAwareOp createNewTargetAwareOp(Address target) {
            return new MGetSize(target);
        }

        boolean onResponse(Object response) {
            size += ((Long) response).intValue();
            return true;
        }

        void onCall() {
            size = 0;
        }

        Object returnResult() {
            return size;
        }

        class MGetSize extends MMigrationAwareTargettedCall {
            public MGetSize(Address target) {
                this.target = target;
                request.reset();
                request.name = name;
                request.operation = OP_CMAP_SIZE;
            }

            @Override
            void handleNoneRedoResponse(final PacketQueue.Packet packet) {
                handleLongNoneRedoResponse(packet);
            }

            public void doLocalCall() {
                CMap cmap = getMap(request.name);
                request.response = Long.valueOf(cmap.size());
            }
        }
    }

    public class MIterate extends MMultiCall {
        Entries entries = null;
        final static int TYPE_ENTRIES = 1;
        final static int TYPE_KEYS = 2;
        final static int TYPE_VALUES = 3;

        final String name;
        final int iteratorType;

        public MIterate(String name, int iteratorType) {
            this.name = name;
            this.iteratorType = iteratorType;
        }

        TargetAwareOp createNewTargetAwareOp(Address target) {
            return new MGetEntries(target);
        }

        void onCall() {
            entries = new Entries(name, iteratorType);
        }

        boolean onResponse(Object response) {
            entries.addEntries((Pairs) response);
            return true;
        }

        Object returnResult() {
            return entries;
        }

        class MGetEntries extends MMigrationAwareTargettedCall {
            public MGetEntries(Address target) {
                this.target = target;
                request.reset();
                request.name = name;
                request.operation = (iteratorType == MIterate.TYPE_KEYS)
                        ? OP_CMAP_ITERATE_KEYS
                        : OP_CMAP_ITERATE;
            }

            public void doLocalCall() {
                CMap cmap = getMap(request.name);
                cmap.getEntries(request);
            }
        }
    }


    Address getTarget(String name, Data key) {
        int blockId = getBlockId(key);
        Block block = blocks[blockId];
        if (block == null) {
            if (isMaster() && !isSuperClient()) {
                block = getOrCreateBlock(blockId);
                block.owner = thisAddress;
            } else
                return null;
        }
        if (block.isMigrating())
            return null;
        if (block.owner == null)
            return null;
        if (block.owner.equals(thisAddress)) {
            if (block.isMigrating()) {
                if (name == null)
                    return block.migrationAddress;
                CMap map = getMap(name);
                Record record = map.getRecord(key);
                if (record == null)
                    return block.migrationAddress;
                else {
                    Address recordOwner = record.owner;
                    if (recordOwner == null)
                        return thisAddress;
                    if ((!recordOwner.equals(thisAddress))
                            && (!recordOwner.equals(block.migrationAddress))) {
                        record.owner = thisAddress;
                    }
                    return record.owner;
                }
            }
        } else if (thisAddress.equals(block.migrationAddress)) {
            if (name == null)
                return thisAddress;
            CMap map = getMap(name);
            Record record = map.getRecord(key);
            if (record == null)
                return thisAddress;
            else
                return record.owner;
        }
        return block.owner;
    }

    @Override
    public boolean isMigrating() {
        for (Block block : blocks) {
            if (block != null && block.isMigrating()) {
                return true;
            }
        }
        return false;
    }

    private int getBlockId(Data key) {
        int hash = key.hashCode();
        return Math.abs(hash) % BLOCK_COUNT;
    }

    Block getOrCreateBlock(Data key) {
        return getOrCreateBlock(getBlockId(key));
    }

    Block getOrCreateBlock(int blockId) {
        Block block = blocks[blockId];
        if (block == null) {
            block = new Block(blockId, null);
            blocks[blockId] = block;
        }
        return block;
    }

    CMap getMap(String name) {
        CMap map = maps.get(name);
        if (map == null) {
            map = new CMap(name);
            maps.put(name, map);
        }
        return map;
    }

    @Override
    void handleListenerRegisterations(boolean add, String name, Data key, Address address,
                                      boolean includeValue) {
        CMap cmap = getMap(name);
        if (add) {
            cmap.addListener(key, address, includeValue);
        } else {
            cmap.removeListener(key, address);
        }
    }

    // master should call this method
    void sendBlockInfo(Block block, Address address) {
        send("mapblock", OP_CMAP_BLOCK_INFO, block, address);
    }

    void handleBlocks(PacketQueue.Packet packet) {
        Blocks blocks = (Blocks) toObject(packet.value);
        handleBlocks(blocks);
        packet.returnToContainer();
        doResetRecords(null);
        if (DEBUG) {
            // printBlocks();
        }
    }

    void handleBlocks(Blocks blocks) {
        List<Block> lsBlocks = blocks.lsBlocks;
        for (Block block : lsBlocks) {
            doBlockInfo(block);
        }
    }

    void printBlocks() {
        if (true)
            return;
        if (DEBUG)
            log("=========================================");
        for (int i = 0; i < BLOCK_COUNT; i++) {
            if (DEBUG)
                log(blocks[i]);
        }
        Collection<CMap> cmaps = maps.values();
        for (CMap cmap : cmaps) {
            if (DEBUG)
                log(cmap);
        }
        if (DEBUG)
            log("=========================================");

    }

    void handleBlockInfo(PacketQueue.Packet packet) {
        Block blockInfo = (Block) toObject(packet.value);
        doBlockInfo(blockInfo);
        packet.returnToContainer();
    }

    void doBlockInfo(Block blockInfo) {
        Block block = blocks[blockInfo.blockId];
        if (block == null) {
            block = blockInfo;
            blocks[block.blockId] = block;
        } else {
            if (thisAddress.equals(block.owner)) {
                // I am already owner!
                if (block.isMigrating()) {
                    if (!block.migrationAddress.equals(blockInfo.migrationAddress)) {
                        throw new RuntimeException();
                    }
                } else {
                    if (blockInfo.migrationAddress != null) {
                        // I am being told to migrate
                        block.migrationAddress = blockInfo.migrationAddress;
                    }
                }
            } else {
                block.owner = blockInfo.owner;
                block.migrationAddress = blockInfo.migrationAddress;
            }
        }
        if (block.owner != null && block.owner.equals(block.migrationAddress)) {
            block.migrationAddress = null;
        }
    }


    void doResetRecords(Address deadAddress) {
        if (isSuperClient())
            return;
        Collection<CMap> cmaps = maps.values();
        for (CMap cmap : cmaps) {
            final Object[] records = cmap.mapRecords.values().toArray();
            cmap.reset();
            for (Object recObj : records) {
                final Record rec = (Record) recObj;
                boolean shouldMigrate = false;
                if (thisAddress.equals(rec.owner)) {
                    shouldMigrate = true;
                } else if (deadAddress != null && deadAddress.equals(rec.owner)) {
                    rec.forceBackupOps();
                    shouldMigrate = true;
                }
                if (shouldMigrate) {
                    executeLocally(new Runnable() {
                        public void run() {
                            MMigrate mmigrate = new MMigrate();
                            mmigrate.migrate(rec);
                        }
                    });
                }
            }
        }
        executeLocally(new Runnable() {
            public void run() {
                Processable processCompletion = new Processable() {
                    public void process() {
                        doMigrationComplete(thisAddress);
                        sendMigrationComplete();
                        if (DEBUG) {
                            printBlocks();
                            logger.log(Level.FINEST, "Migration ended!");
                        }

                    }
                };
                enqueueAndReturn(processCompletion);
            }
        });
    }

    private void doMigrationComplete(Address from) {
        logger.log(Level.FINEST, "Migration Complete from " + from);
        for (Block block : blocks) {
            if (from.equals(block.owner)) {
                if (block.isMigrating()) {
                    block.owner = block.migrationAddress;
                    block.migrationAddress = null;
                }
            }
        }
        if (isMaster() && !from.equals(thisAddress)) {
            // I am the master and I got migration complete from a member
            // I will inform others, in case they did not get it yet.
            for (MemberImpl member : lsMembers) {
                if (!member.localMember() && !from.equals(member.getAddress())) {
                    sendProcessableTo(new MigrationComplete(from), member.getAddress());
                }
            }
        }
    }

    private void sendMigrationComplete() {
        for (MemberImpl member : lsMembers) {
            if (!member.localMember()) {
                sendProcessableTo(new MigrationComplete(thisAddress), member.getAddress());
            }
        }
    }

    private void copyRecordToRequest(Record record, Request request, boolean includeKeyValue) {
        request.name = record.name;
        request.version = record.version;
        request.blockId = record.blockId;
        request.lockThreadId = record.lockThreadId;
        request.lockAddress = record.lockAddress;
        request.lockCount = record.lockCount;
        request.longValue = record.copyCount;
        if (includeKeyValue) {
            request.key = doHardCopy(record.getKey());
            if (record.getValue() != null) {
                request.value = doHardCopy(record.getValue());
            }
        }
    }

    private Packet toPacket(Record record, boolean includeValue) {
        Packet packet = obtainPacket();
        packet.name = record.getName();
        packet.blockId = record.getBlockId();
        packet.version = record.version;
        packet.threadId = record.lockThreadId;
        packet.lockAddress = record.lockAddress;
        packet.lockCount = record.lockCount;
        doHardCopy(record.getKey(), packet.key);
        if (includeValue) {
            if (record.getValue() != null) {
                doHardCopy(record.getValue(), packet.value);
            }
        }
        return packet;
    }

    boolean rightRemoteTarget(Packet packet) {
        boolean right = thisAddress.equals(getTarget(packet.name, packet.key));
        if (!right) {
            // not the owner (at least not anymore)
            if (isMaster()) {
                Block block = getOrCreateBlock(packet.key);
                sendBlockInfo(block, packet.conn.getEndPoint());
            }
            packet.setNoData();
            sendRedoResponse(packet);
        }
        return right;
    }

    void handleMigrateRecord(Packet packet) {
        remoteReq.setFromPacket(packet);
        doMigrate(remoteReq);
        sendResponse(packet);
        remoteReq.reset();
    }

    void handleSize(PacketQueue.Packet packet) {
        if (isMigrating()) {
            packet.responseType = RESPONSE_REDO;
        } else {
            CMap cmap = getMap(packet.name);
            packet.longValue = cmap.size();
        }
        // if (DEBUG) {
        // printBlocks();
        // }
        sendResponse(packet);
    }

    abstract class DefaultPacketProcessor implements PacketProcessor {

        final boolean targetAware;
        final boolean schedulable;
        final boolean returnsObject;
        final boolean checkIfMigrating;
        int operation = -1;

        protected DefaultPacketProcessor() {
            targetAware = false;
            schedulable = false;
            returnsObject = false;
            checkIfMigrating = false;
        }

        protected DefaultPacketProcessor(boolean checkIfMigrating, boolean targetAware,
                                         boolean schedulable, boolean returnsObject) {
            this.targetAware = targetAware;
            this.schedulable = schedulable;
            this.returnsObject = returnsObject;
            this.checkIfMigrating = checkIfMigrating;
        }

        public void process(Packet packet) {
            operation = packet.operation;
            if (checkIfMigrating && isMigrating()) {
                packet.responseType = RESPONSE_REDO;
                sendResponse(packet);
            } else if (targetAware && !rightRemoteTarget(packet)) {
            } else {
                remoteReq.setFromPacket(packet);
                handle(remoteReq);
                if (schedulable && remoteReq.scheduled) {
                    packet.returnToContainer();
                } else {
                    packet.version = remoteReq.version;
                    packet.longValue = remoteReq.longValue;
                    if (returnsObject) {
                        Data oldValue = (Data) remoteReq.response;
                        if (oldValue != null && oldValue.size() > 0) {
                            doSet(oldValue, packet.value);
                        }
                        sendResponse(packet);
                    } else {
                        if (remoteReq.response == Boolean.TRUE) {
                            sendResponse(packet);
                        } else {
                            sendResponseFailure(packet);
                        }
                    }
                }
                remoteReq.reset();
            }
        }

        abstract void handle(Request request);

        @Override
        public String toString() {
            return "DefaultProcessor operation=" + operation;
        }
    }

    final void doLock(Request request) {
        boolean lock = (request.operation == OP_CMAP_LOCK || request.operation == OP_CMAP_LOCK_RETURN_OLD);
        if (!lock) {
            // unlock
            boolean unlocked = true;
            Record record = recordExist(request);
            if (DEBUG) {
                log(request.operation + " unlocking " + record);
            }
            if (record != null) {
                unlocked = record.unlock(request.lockThreadId, request.lockAddress);
                if (unlocked) {
                    request.lockCount = record.lockCount;
                }
            }
            if (request.local) {
                if (unlocked)
                    request.response = Boolean.TRUE;
                else
                    request.response = Boolean.FALSE;
            }
        } else if (!testLock(request)) {
            if (request.hasEnoughTimeToSchedule()) {
                // schedule
                final Record record = ensureRecord(request);
                final Request reqScheduled = (request.local) ? request : request.hardCopy();
                if (request.operation == OP_CMAP_LOCK_RETURN_OLD) {
                    reqScheduled.value = doHardCopy(record.getValue());
                }
                if (DEBUG) {
                    log("scheduling lock");
                }
                record.addScheduledAction(new ScheduledLockAction(reqScheduled, record));
                request.scheduled = true;
            } else {
                request.response = Boolean.FALSE;
            }
        } else {
            if (DEBUG) {
                log("Locking...");
            }
            Record rec = ensureRecord(request);
            if (request.operation == OP_CMAP_LOCK_RETURN_OLD) {
                request.value = doHardCopy(rec.getValue());
            }
            rec.lock(request.lockThreadId, request.lockAddress);
            request.lockCount = rec.lockCount;
            request.response = Boolean.TRUE;
        }
    }

    final void doPut(Request request, final boolean returnsValue) {
        if (!testLock(request)) {
            if (request.hasEnoughTimeToSchedule()) {
                // schedule
                Record record = ensureRecord(request);
                request.scheduled = true;
                final Request reqScheduled = (request.local) ? request : request.hardCopy();
                record.addScheduledAction(new ScheduledAction(reqScheduled) {
                    @Override
                    public boolean consume() {
                        CMap cmap = getMap(reqScheduled.name);
                        reqScheduled.response = (returnsValue) ? cmap.put(reqScheduled) : cmap.putMulti(reqScheduled);
                        reqScheduled.key = null;
                        reqScheduled.value = null;
                        returnScheduledAsSuccess(reqScheduled);
                        return true;
                    }
                });
            } else {
                request.response = null;
            }
        } else {
            CMap cmap = getMap(request.name);
            request.response = (returnsValue) ? cmap.put(request) : cmap.putMulti(request);
        }
    }

    final void doBackup(Request request) {
        CMap cmap = getMap(request.name);
        request.response = cmap.backup(request);
    }

    final void doMigrate(Request request) {
        CMap cmap = getMap(request.name);
        cmap.own(request);
        request.response = Boolean.TRUE;
    }

    final void doAdd(Request request) {
        CMap cmap = getMap(request.name);
        request.response = cmap.add(request);
    }

    final void doGet(Request request) {
        CMap cmap = getMap(request.name);
        request.response = cmap.get(request);
    }

    final void doContains(Request request) {
        CMap cmap = getMap(request.name);
        request.response = cmap.contains(request);
    }

    final void doEvict(Request request) {
        CMap cmap = getMap(request.name);
        if (testLock(request)) {
            request.response = cmap.evict(request);
        } else {
            request.response = false;
        }
    }

    final void doRemoveMulti(Request request) {
        if (!testLock(request)) {
            if (request.hasEnoughTimeToSchedule()) {
                // schedule
                Record record = ensureRecord(request);
                request.scheduled = true;
                final Request reqScheduled = (request.local) ? request : request.hardCopy();
                record.addScheduledAction(new ScheduledAction(reqScheduled) {
                    @Override
                    public boolean consume() {
                        CMap cmap = getMap(reqScheduled.name);
                        boolean isRemoved = cmap.removeMulti(reqScheduled);
                        reqScheduled.response = isRemoved;
                        reqScheduled.key = null;
                        reqScheduled.value = null;
                        returnScheduledAsBoolean(reqScheduled);
                        return true;
                    }
                });
            } else {
                request.response = Boolean.FALSE;
            }
        } else {
            CMap cmap = getMap(request.name);
            boolean isRemoved = cmap.removeMulti(request);
            request.response = isRemoved;
        }
    }

    final void doRemove(Request request, final boolean returnsValue) {
        if (!testLock(request)) {
            if (request.hasEnoughTimeToSchedule()) {
                // schedule
                Record record = ensureRecord(request);
                request.scheduled = true;
                final Request reqScheduled = (request.local) ? request : request.hardCopy();
                record.addScheduledAction(new ScheduledAction(reqScheduled) {
                    @Override
                    public boolean consume() {
                        CMap cmap = getMap(reqScheduled.name);
                        if (returnsValue) {
                            Data oldValue = cmap.remove(reqScheduled);
                            reqScheduled.response = oldValue;
                            returnScheduledAsSuccess(reqScheduled);
                        } else {
                            boolean isRemoved = cmap.removeItem(reqScheduled);
                            reqScheduled.response = isRemoved;
                            reqScheduled.key = null;
                            reqScheduled.value = null;
                            returnScheduledAsBoolean(reqScheduled);
                        }
                        return true;
                    }
                });
            } else {
                request.response = null;
            }
        } else {
            CMap cmap = getMap(request.name);
            if (returnsValue) {
                Data oldValue = cmap.remove(request);
                request.response = oldValue;
            } else {
                boolean isRemoved = cmap.removeItem(request);
                request.response = isRemoved;
            }
        }
    }

    Record recordExist(Request req) {
        CMap cmap = maps.get(req.name);
        if (cmap == null)
            return null;
        return cmap.getRecord(req.key);
    }

    Record ensureRecord(Request req) {
        CMap cmap = getMap(req.name);
        Record record = cmap.getRecord(req.key);
        if (record == null) {
            record = cmap.createNewRecord(req.key, req.value);
            req.key = null;
            req.value = null;
        }
        return record;
    }

    final boolean testLock(Request req) {
        Record record = recordExist(req);
        if (record == null)
            return true;
        return record.testLock(req.lockThreadId, req.lockAddress);
    }

    class LRUComparator implements Comparator<Record> {
        public int compare(Record r1, Record r2) {
            return (r1.lastTouchTime < r2.lastTouchTime) ? -1 : ((r1.lastTouchTime == r2.lastTouchTime) ? 0 : 1);
        }
    }

    class CMap {
        final SortedHashMap<Data, Record> mapRecords = new SortedHashMap<Data, Record>(1000);

        final String name;

        final Map<Address, Boolean> mapListeners = new HashMap<Address, Boolean>(1);

        final int backupCount;

        final OrderingType evictionPolicy;

        final int maxSize;

        final float evictionRate;

        boolean evicting = false;

        int ownedEntryCount = 0;

        public CMap(String name) {
            super();
            this.name = name;
            Config.MapConfig mapConfig = Config.get().getMapConfig(name.substring(2));
            this.backupCount = mapConfig.backupCount;

            if ("LFU".equalsIgnoreCase(mapConfig.evictionPolicy)) {
                evictionPolicy = OrderingType.LFU;
            } else if ("LRU".equalsIgnoreCase(mapConfig.evictionPolicy)) {
                evictionPolicy = OrderingType.LRU;
            } else {
                evictionPolicy = OrderingType.NONE;
            }
            evictionRate = mapConfig.evictionPercentage / 100;
            if (evictionPolicy == OrderingType.NONE) {
                maxSize = Integer.MAX_VALUE;
            } else {
                maxSize = (mapConfig.maxSize == 0) ? Integer.MAX_VALUE : mapConfig.maxSize;
            }
        }

        public Record getRecord(Data key) {
            return mapRecords.get(key);
        }

        public int getBackupCount() {
            return backupCount;
        }

        public void own(Request req) {
            Record record = toRecord(req);
            record.owner = thisAddress;
        }

        public boolean backup(Request req) {
            Record record = getRecord(req.key);

            if (record != null) {
                if (req.version > record.version + 1) {
                    Request reqCopy = new Request();
                    reqCopy.setFromRequest(req, true);
                    req.key = null;
                    req.value = null;
                    record.addBackupOp(new VersionedBackupOp(reqCopy));
                    return true;
                } else if (req.version <= record.version) {
                    return false;
                }
            }
            doBackup(req);
            if (record != null) {
                record.version = req.version;
                record.runBackupOps();
            }

            return true;
        }


        public void doBackup(Request req) {
            if (req.operation == OP_CMAP_BACKUP_PUT) {
                toRecord(req);
            } else if (req.operation == OP_CMAP_BACKUP_REMOVE) {
                Record record = getRecord(req.key);
                if (record != null) {
                    if (record.value != null) {
                        record.value.setNoData();
                    }

                    if (record.lsValues != null) {
                        for (Data value : record.lsValues) {
                            value.setNoData();
                        }
                    }
                    if (record.copyCount > 0) {
                        record.decrementCopyCount();
                    }
                    record.value = null;
                    if (record.isRemovable()) {
                        removeRecord(record.key);
                    }
                }
            } else if (req.operation == OP_CMAP_BACKUP_LOCK) {
                toRecord(req);
            } else if (req.operation == OP_CMAP_BACKUP_ADD) {
                add(req);
            } else if (req.operation == OP_CMAP_BACKUP_PUT_MULTI) {
                Record record = getRecord(req.key);
                if (record == null) {
                    record = createNewRecord(req.key, null);
                }
                record.addValue(req.value);
                req.value = null;
            } else if (req.operation == OP_CMAP_BACKUP_REMOVE_MULTI) {
                Record record = getRecord(req.key);
                if (record != null) {
                    if (req.value == null) {
                        removeRecord(req.key);
                    } else {
                        if (record.containsValue(req.value)) {
                            if (record.lsValues != null) {
                                Iterator<Data> itValues = record.lsValues.iterator();
                                while (itValues.hasNext()) {
                                    Data value = itValues.next();
                                    if (req.value.equals(value)) {
                                        itValues.remove();
                                        value.setNoData();
                                    }
                                }
                            }
                        }
                    }
                    if (record.isRemovable()) {
                        removeRecord(record.key);
                    }
                }
            } else {
                logger.log(Level.SEVERE, "Unknown backup operation " + req.operation);
            }
        }

        public int backupSize() {
            int size = 0;
            Collection<Record> records = mapRecords.values();
            for (Record record : records) {
                Block block = blocks[record.blockId];
                if (!thisAddress.equals(block.owner)) {
                    size += record.valueCount();
                }
            }
            return size;
        }

        public int size() {
            int size = 0;
            Collection<Record> records = mapRecords.values();
            for (Record record : records) {
                Block block = blocks[record.blockId];
                if (thisAddress.equals(block.owner)) {
                    size += record.valueCount();
                }
            }
//            System.out.println(size + " is size.. backup.size " + backupSize() + " ownedEntryCount:" + ownedEntryCount);
            return size;
        }

        public boolean contains(Request req) {
            Data key = req.key;
            Data value = req.value;
            if (key != null) {
                Record record = getRecord(req.key);
                if (record == null) {
                    return false;
                } else {
                    Block block = blocks[record.blockId];
                    if (thisAddress.equals(block.owner)) {
                        touch(record);
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
                    Block block = blocks[record.blockId];
                    if (thisAddress.equals(block.owner)) {
                        if (record.containsValue(value)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public void getEntries(Request request) {
            Collection<Record> colRecords = mapRecords.values();
            Pairs pairs = new Pairs();
            for (Record record : colRecords) {
                Block block = blocks[record.blockId];
                if (thisAddress.equals(block.owner)) {
                    if (record.value != null) {
                        pairs.addKeyValue(new KeyValue(doHardCopy(record.key), doHardCopy(record.value)));
                    } else if (record.copyCount > 0) {
                        for (int i = 0; i < record.copyCount; i++) {
                            pairs.addKeyValue(new KeyValue(doHardCopy(record.key), null));
                        }
                    } else if (record.lsValues != null) {
                        int size = record.lsValues.size();
                        if (size > 0) {
                            if (request.operation == OP_CMAP_ITERATE_KEYS) {
                                pairs.addKeyValue(new KeyValue(doHardCopy(record.key), null));
                            } else {
                                for (int i = 0; i < size; i++) {
                                    Data value = record.lsValues.get(i);
                                    pairs.addKeyValue(new KeyValue(doHardCopy(record.key), doHardCopy(value)));
                                }
                            }
                        }
                    }
                }
            }
            Data dataEntries = toData(pairs);
            request.longValue = pairs.size();
            request.response = dataEntries;
        }

        public Data get(Request req) {
            Record record = getRecord(req.key);
            req.key.setNoData();
            req.key = null;
            if (record == null)
                return null;
            touch(record);
            Data data = record.getValue();
            if (data != null) {
                return doHardCopy(data);
            } else {
                if (record.lsValues != null) {
                    Values values = new Values(record.lsValues);
                    return toData(values);
                }
            }
            return null;
        }

        public boolean add(Request req) {
            Record record = getRecord(req.key);
            if (record == null) {
                record = createNewRecord(req.key, null);
                req.key = null;
            } else {
                if (req.operation == OP_CMAP_ADD_TO_SET) {
                    return false;
                }
            }
            record.incrementCopyCount();
            return true;
        }

        public boolean removeMulti(Request req) {
            Record record = getRecord(req.key);
            if (record == null) return false;
            boolean removed = false;
            if (req.value == null) {
                removed = true;
                removeRecord(req.key);
            } else {
                if (record.containsValue(req.value)) {
                    if (record.lsValues != null) {
                        Iterator<Data> itValues = record.lsValues.iterator();
                        while (itValues.hasNext()) {
                            Data value = itValues.next();
                            if (req.value.equals(value)) {
                                itValues.remove();
                                value.setNoData();
                                removed = true;
                            }
                        }
                    }
                }
            }
            if (removed) {
                record.version++;
                fireMapEvent(mapListeners, name, EntryEvent.TYPE_REMOVED, record);
                logger.log(Level.FINEST, record.value + " RemoveMulti " + record.lsValues);
            }
            req.version = record.version;
            return removed;
        }

        public boolean putMulti(Request req) {
            Record record = getRecord(req.key);
            boolean added = true;
            if (record == null) {
                record = createNewRecord(req.key, null);
                req.key = null;
            } else {
                if (record.containsValue(req.value)) {
                    added = false;
                }
            }
            if (added) {
                record.addValue(req.value);
                req.value = null;
                record.version++;
                touch(record);
                fireMapEvent(mapListeners, name, EntryEvent.TYPE_ADDED, record);
            }
            logger.log(Level.FINEST, record.value + " PutMulti " + record.lsValues);
            req.version = record.version;
            return added;
        }

        public Data put(Request req) {
            if (req.operation == OP_CMAP_PUT_IF_ABSENT) {
                Record record = recordExist(req);
                if (record != null && record.getValue() != null) {
                    return doHardCopy(record.getValue());
                }
            } else if (req.operation == OP_CMAP_REPLACE_IF_NOT_NULL) {
                Record record = recordExist(req);
                if (record == null || record.getValue() == null) {
                    return null;
                }
            }
            Record record = getRecord(req.key);
            Data oldValue = null;
            if (record == null) {
                record = createNewRecord(req.key, req.value);
            } else {
                oldValue = record.getValue();
                record.setValue(req.value);
                record.version++;
                touch(record);
                req.key.setNoData();
            }
            req.version = record.version;
            req.longValue = record.copyCount;
            req.key = null;
            req.value = null;
            if (oldValue == null) {
                fireMapEvent(mapListeners, name, EntryEvent.TYPE_ADDED, record);
            } else {
                fireMapEvent(mapListeners, name, EntryEvent.TYPE_UPDATED, record);
            }
            return oldValue;
        }

        void reset() {
            mapRecords.clear();
            ownedEntryCount = 0;
            evicting = false;
        }

        void touch(Record record) {
            if (evictionPolicy == OrderingType.NONE) return;
            record.lastTouchTime = System.currentTimeMillis();
            SortedHashMap.touch(mapRecords, record.key, evictionPolicy);
        }

        void startEviction() {
            if (evicting) return;
            if (evictionPolicy == OrderingType.NONE) return;
            Collection<Record> values = mapRecords.values();
            int numberOfRecordsToEvict = (int) (ownedEntryCount * evictionRate);
            int evictedCount = 0;
            List<Data> lsKeysToEvict = new ArrayList<Data>(numberOfRecordsToEvict);
            recordsLoop:
            for (Record record : values) {
                if (record.isEvictable()) {
                    lsKeysToEvict.add(doHardCopy(record.key));
                    if (++evictedCount >= numberOfRecordsToEvict) {
                        break recordsLoop;
                    }
                }
            }
            if (lsKeysToEvict.size() > 1) {
//                System.out.println(ownedEntryCount + " stating eviction..." + lsKeysToEvict.size());
                evicting = true;
                int latchCount = lsKeysToEvict.size();
                final CountDownLatch countDownLatchEvictionStart = new CountDownLatch(1);
                final CountDownLatch countDownLatchEvictionEnd = new CountDownLatch(latchCount);

                executeLocally(new Runnable() {
                    public void run() {
                        try {
                            countDownLatchEvictionStart.countDown();
                            countDownLatchEvictionEnd.await(60, TimeUnit.SECONDS);
                            enqueueAndReturn(new Processable() {
                                public void process() {
                                    evicting = false;
                                }
                            });
                        } catch (Exception ignored) {
                        }
                    }
                });
                for (final Data key : lsKeysToEvict) {
                    executeLocally(new Runnable() {
                        public void run() {
                            try {
                                countDownLatchEvictionStart.await();
                                MEvict mEvict = new MEvict();
                                mEvict.evict(name, key);
                                countDownLatchEvictionEnd.countDown();
                            } catch (Exception ignored) {
                            }
                        }
                    });
                }
            }
        }

        boolean evict(Request req) {
            Record record = getRecord(req.key);
            if (record != null && record.isEvictable()) {
                removeRecord(req.key);
                return true;
            }
            return false;
        }

        public Record toRecord(Request req) {
            Record record = getRecord(req.key);
            if (record == null) {
                record = createNewRecord(req.key, req.value);
            } else {
                if (req.value != null) {
                    if (record.value != null) {
                        record.value.setNoData();
                    }
                    record.setValue(req.value);
                    record.version++;
                }
                req.key.setNoData();
            }
            req.key = null;
            req.value = null;
            record.lockAddress = req.lockAddress;
            record.lockThreadId = req.lockThreadId;
            record.lockCount = req.lockCount;
            record.copyCount = (int) req.longValue;

            return record;
        }

        public boolean removeItem(Request req) {
            Record record = mapRecords.get(req.key);
            req.key.setNoData();
            req.key = null;
            if (record == null) {
                return false;
            }
            boolean removed = false;
            if (record.getCopyCount() > 0) {
                record.decrementCopyCount();
                removed = true;
            } else if (record.value != null) {
                removed = true;
            }

            if (removed) {
                fireMapEvent(mapListeners, name, EntryEvent.TYPE_REMOVED, record);
                record.version++;
                if (record.value != null) {
                    record.value.setNoData();
                    record.value = null;
                }
            }

            req.version = record.version;
            req.longValue = record.copyCount;

            if (record.isRemovable()) {
                removeRecord(record.key);
                record.key.setNoData();
                record.key = null;
            }
            return true;
        }

        public Data remove(Request req) {
            Record record = mapRecords.get(req.key);
            req.key.setNoData();
            req.key = null;
            if (record == null) {
                return null;
            }
            if (req.value != null) {
                if (record.value != null) {
                    if (!record.value.equals(req.value)) {
                        return null;
                    }
                }
            }
            Data oldValue = record.getValue();
            if (oldValue == null && record.lsValues != null && record.lsValues.size() > 0) {
                Values values = new Values(record.lsValues);
                oldValue = toData(values);
                record.lsValues = null;
            }
            if (oldValue != null) {
                fireMapEvent(mapListeners, name, EntryEvent.TYPE_REMOVED, record.key, oldValue, record.mapListeners);
                record.version++;
                record.value = null;
            }

            req.version = record.version;
            req.longValue = record.copyCount;

            if (record.isRemovable()) {
                removeRecord(record.key);
                record.key.setNoData();
                record.key = null;
            }

            return oldValue;
        }

        Record removeRecord(Data key) {
            Record record = mapRecords.remove(key);
            if (record != null) {
                Block ownerBlock = blocks[record.blockId];
                if (thisAddress.equals(ownerBlock.getRealOwner())) {
                    ownedEntryCount--;
                }
            }
            return record;
        }

        Record createNewRecord(Data key, Data value) {
            int blockId = getBlockId(key);
            Record rec = new Record(name, blockId, key, value);
            Block ownerBlock = getOrCreateBlock(blockId);
            if (thisAddress.equals(ownerBlock.getRealOwner())) {
                ownedEntryCount++;
            }
            rec.owner = ownerBlock.owner;
            mapRecords.put(key, rec);
            if (evictionPolicy != OrderingType.NONE) {
                if (maxSize != Integer.MAX_VALUE) {
                    int limitSize = (maxSize / lsMembers.size());
//                    System.out.println(ownedEntryCount + " createNewRecord " + limitSize);
                    if (ownedEntryCount > limitSize) {
                        startEviction();
                    }
                }
            }
            return rec;
        }

        public void addListener(Data key, Address address, boolean includeValue) {
            if (key == null || key.size() == 0) {
                mapListeners.put(address, includeValue);
            } else {
                Record rec = getRecord(key);
                if (rec == null) {
                    rec = createNewRecord(key, null);
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

        @Override
        public String toString() {
            return "CMap [" + name + "] size=" + size() + ", backup-size=" + backupSize();
        }
    }


    class VersionedBackupOp implements Runnable, Comparable {
        final Request request;

        VersionedBackupOp(Request request) {
            this.request = request;
        }

        public void run() {
            CMap cmap = getMap(request.name);
            cmap.doBackup(request);
        }

        long getVersion() {
            return request.version;
        }

        public int compareTo(Object o) {
            long v = ((VersionedBackupOp) o).getVersion();
            if (request.version > v) return 1;
            else if (request.version < v) return -1;
            else return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VersionedBackupOp that = (VersionedBackupOp) o;

            if (request.version != that.request.version) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return (int) (request.version ^ (request.version >>> 32));
        }
    }

    class Record {
        private Address owner;
        private Data key;
        private Data value;
        private long version = 0;
        private final long createTime;
        private long lastTouchTime;
        private final String name;
        private final int blockId;
        private int lockThreadId = -1;
        private Address lockAddress = null;
        private int lockCount = 0;
        private List<ScheduledAction> lsScheduledActions = null;
        private Map<Address, Boolean> mapListeners = null;
        private int copyCount = 0;
        private List<Data> lsValues = null; // multimap values
        private SortedSet<VersionedBackupOp> backupOps = null;

        public Record(String name, int blockId, Data key, Data value) {
            super();
            this.name = name;
            this.blockId = blockId;
            this.key = key;
            this.value = value;
            this.createTime = System.currentTimeMillis();
            this.lastTouchTime = createTime;
            this.version = 0;
        }

        public void runBackupOps() {
            if (backupOps != null) {
                if (backupOps.size() > 0) {
                    Iterator<VersionedBackupOp> it = backupOps.iterator();
                    while (it.hasNext()) {
                        VersionedBackupOp bo = it.next();
                        if (bo.getVersion() < version + 1) {
                            it.remove();
                        } else if (bo.getVersion() == version + 1) {
                            bo.run();
                            version = bo.getVersion();
                            bo.request.reset();
                            it.remove();
                        } else {
                            return;
                        }
                    }
                }
            }
        }

        public void addBackupOp(VersionedBackupOp bo) {
            if (backupOps == null) {
                backupOps = new TreeSet<VersionedBackupOp>();
            }
            backupOps.add(bo);
            if (backupOps.size() > 4) {
                logger.log(Level.FINEST, " Forcing backup.run version " + version);
                forceBackupOps();
            }
        }

        public void forceBackupOps() {
            if (backupOps == null) return;
            Iterator<VersionedBackupOp> it = backupOps.iterator();
            while (it.hasNext()) {
                VersionedBackupOp v = it.next();
                v.run();
                version = v.getVersion();
                v.request.reset();
                it.remove();
            }
        }

        public int getBlockId() {
            return blockId;
        }

        public String getName() {
            return name;
        }

        public Data getKey() {
            return key;
        }

        public Data getValue() {
            return value;
        }

        public void setValue(Data value) {
            this.value = value;
        }

        public int valueCount() {
            int count = 0;
            if (value != null) {
                count = 1;
            } else if (lsValues != null) {
                count = lsValues.size();
            } else if (copyCount > 0) {
                count += copyCount;
            }
            return count;
        }

        public boolean containsValue(Data value) {
            if (this.value != null) {
                return this.value.equals(value);
            } else if (lsValues != null) {
                int count = lsValues.size();
                for (int i = 0; i < count; i++) {
                    if (value.equals(lsValues.get(i))) {
                        return true;
                    }
                }
            }
            return false;
        }

        public void addValue(Data value) {
            if (lsValues == null) {
                lsValues = new ArrayList<Data>(2);
            }
            lsValues.add(value);
        }

        public void onDisconnect(Address deadAddress) {
            if (lsScheduledActions != null) {
                if (lsScheduledActions.size() > 0) {
                    Iterator<ScheduledAction> it = lsScheduledActions.iterator();
                    while (it.hasNext()) {
                        ScheduledAction sa = it.next();
                        if (sa.request.caller.equals(deadAddress)) {
                            ClusterManager.get().deregisterScheduledAction(sa);
                            sa.setValid(false);
                            it.remove();
                        }
                    }
                }
            }
            if (lockCount > 0) {
                if (deadAddress.equals(lockAddress)) {
                    unlock();
                }
            }
        }

        public void unlock() {
            lockThreadId = -1;
            lockCount = 0;
            lockAddress = null;
            fireScheduledActions();
        }

        public boolean unlock(int threadId, Address address) {
            if (threadId == -1 || address == null)
                throw new IllegalArgumentException();
            if (lockCount == 0)
                return true;
            if (lockThreadId != threadId || !address.equals(lockAddress)) {
                return false;
            }
            if (lockCount > 0) {
                lockCount--;
            }
            if (DEBUG) {
                log(lsScheduledActions + " unlocked " + lockCount);
            }
            if (lockCount == 0) {
                lockThreadId = -1;
                lockAddress = null;
                fireScheduledActions();
            }
            return true;
        }

        public void fireScheduledActions() {
            if (lsScheduledActions != null) {
                while (lsScheduledActions.size() > 0) {
                    ScheduledAction sa = lsScheduledActions.remove(0);
                    ClusterManager.get().deregisterScheduledAction(sa);
                    if (DEBUG) {
                        log("sa.expired " + sa.expired());
                    }
                    if (sa.expired()) {
                        sa.onExpire();
                    } else {
                        sa.consume();
                        return;
                    }
                }
            }
        }

        public boolean testLock(int threadId, Address address) {
            if (lockCount == 0) {
                return true;
            }
            if (lockThreadId == threadId && lockAddress.equals(address)) {
                return true;
            }
            return false;
        }

        public boolean lock(int threadId, Address address) {
            if (lockCount == 0) {
                lockThreadId = threadId;
                lockAddress = address;
                lockCount++;
                return true;
            }
            if (lockThreadId == threadId && lockAddress.equals(address)) {
                lockCount++;
                return true;
            }
            return false;
        }

        public void addScheduledAction(ScheduledAction scheduledAction) {
            if (lsScheduledActions == null)
                lsScheduledActions = new ArrayList<ScheduledAction>(1);
            lsScheduledActions.add(scheduledAction);
            ClusterManager.get().registerScheduledAction(scheduledAction);
            if (DEBUG) {
                log("scheduling " + scheduledAction);
            }
        }

        public boolean isRemovable() {
            return (valueCount() <= 0 && !hasListener() && (backupOps == null || backupOps.size() == 0));
        }

        public boolean isEvictable() {
            return (lockCount == 0 && !hasListener());
        }

        public boolean hasListener() {
            return (mapListeners != null && mapListeners.size() > 0);
        }

        public void addListener(Address address, boolean returnValue) {
            if (mapListeners == null)
                mapListeners = new HashMap<Address, Boolean>(1);
            mapListeners.put(address, returnValue);
        }

        public void removeListener(Address address) {
            if (mapListeners == null)
                return;
            mapListeners.remove(address);
        }

        public void incrementCopyCount() {
            ++copyCount;
        }

        public void decrementCopyCount() {
            if (copyCount > 0) {
                --copyCount;
            }
        }

        public int getCopyCount() {
            return copyCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Record record = (Record) o;

            if (key != null ? !key.equals(record.key) : record.key != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return key != null ? key.hashCode() : 0;
        }

        public String toString() {
            return "Record key=" + key + ", removable=" + isRemovable();
        }
    }

    public static class Block implements DataSerializable {
        int blockId;
        Address owner;
        Address migrationAddress;

        public Block() {
        }

        public Block(int blockId, Address owner) {
            this.blockId = blockId;
            this.owner = owner;
        }

        public boolean isMigrating() {
            return (migrationAddress != null);
        }

        public Address getRealOwner() {
            return (migrationAddress != null) ? migrationAddress : owner;
        }

        public void readData(DataInput in) throws IOException {
            blockId = in.readInt();
            boolean owned = in.readBoolean();
            if (owned) {
                owner = new Address();
                owner.readData(in);
            }
            boolean migrating = in.readBoolean();
            if (migrating) {
                migrationAddress = new Address();
                migrationAddress.readData(in);
            }
        }

        public void writeData(DataOutput out) throws IOException {
            out.writeInt(blockId);
            boolean owned = (owner != null);
            out.writeBoolean(owned);
            if (owned) {
                owner.writeData(out);
            }
            boolean migrating = (migrationAddress != null);
            out.writeBoolean(migrating);
            if (migrating)
                migrationAddress.writeData(out);
        }

        @Override
        public String toString() {
            return "Block [" + blockId + "] owner=" + owner + " migrationAddress="
                    + migrationAddress;
        }
    }

    public static class MigrationComplete extends AbstractRemotelyProcessable {
        Address completedAddress;

        public MigrationComplete(Address completedAddress) {
            this.completedAddress = completedAddress;
        }

        public MigrationComplete() {
        }

        public void readData(DataInput in) throws IOException {
            completedAddress = new Address();
            completedAddress.readData(in);
        }

        public void writeData(DataOutput out) throws IOException {
            completedAddress.writeData(out);
        }

        public void process() {
            ConcurrentMapManager.get().doMigrationComplete(completedAddress);
        }
    }

    public static class Blocks extends AbstractRemotelyProcessable {
        List<Block> lsBlocks = new ArrayList<Block>(BLOCK_COUNT);

        public void addBlock(Block block) {
            lsBlocks.add(block);
        }

        public void readData(DataInput in) throws IOException {
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                Block block = new Block();
                block.readData(in);
                addBlock(block);
            }
        }

        public void writeData(DataOutput out) throws IOException {
            int size = lsBlocks.size();
            out.writeInt(size);
            for (Block block : lsBlocks) {
                block.writeData(out);
            }
        }

        public void process() {
            ConcurrentMapManager.get().handleBlocks(Blocks.this);
        }
    }

    public static class Entries implements Set {
        final String name;
        final List<Map.Entry> lsKeyValues = new ArrayList<Map.Entry>();
        final int iteratorType;

        public Entries(String name, int iteratorType) {
            this.name = name;
            this.iteratorType = iteratorType;
            TransactionImpl txn = ThreadContext.get().txn;
            if (txn != null) {
                lsKeyValues.addAll(txn.newEntries(name));
            }
        }

        public boolean isEmpty() {
            return (size() == 0);
        }

        public int size() {
            return lsKeyValues.size();
        }

        public Iterator iterator() {
            return new EntryIterator(lsKeyValues.iterator());
        }

        public void addEntries(Pairs pairs) {
            if (pairs.lsKeyValues == null) return;
            for (KeyValue entry : pairs.lsKeyValues) {
                TransactionImpl txn = ThreadContext.get().txn;
                if (txn != null) {
                    Object key = entry.getKey();
                    if (txn.has(name, key)) {
                        Object value = txn.get(name, key);
                        if (value != null) {
                            lsKeyValues.add(createSimpleEntry(name, key, value));
                        }
                    } else {
                        lsKeyValues.add(entry);
                    }
                } else {
                    entry.setName(name);
                    lsKeyValues.add(entry);
                }
            }
        }

        class EntryIterator implements Iterator {
            final Iterator<Map.Entry> it;
            Map.Entry entry = null;

            public EntryIterator(Iterator<Map.Entry> it) {
                super();
                this.it = it;
            }

            public boolean hasNext() {
                return it.hasNext();
            }

            public Object next() {
                entry = it.next();
                if (iteratorType == MIterate.TYPE_KEYS) {
                    return entry.getKey();
                } else if (iteratorType == MIterate.TYPE_VALUES) {
                    return entry.getValue();
                } else if (iteratorType == MIterate.TYPE_ENTRIES) {
                    return entry;
                } else throw new RuntimeException("Unknown iteration type " + iteratorType);
            }

            public void remove() {
                ((FactoryImpl.MProxy) FactoryImpl.getProxy(name)).put(entry.getKey(), entry.getValue());
                it.remove();
            }
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
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection c) {
            throw new UnsupportedOperationException();
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

        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray(Object[] a) {
            throw new UnsupportedOperationException();
        }

    }


    public static class Values implements Collection, DataSerializable {
        List<Data> lsValues = null;

        public Values() {
        }

        public Values(List<Data> lsValues) {
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
            throw new UnsupportedOperationException();
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
                return ThreadContext.get().toObject(value);
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
            return lsValues.size();
        }

        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray(Object[] a) {
            throw new UnsupportedOperationException();
        }

        public void readData(DataInput in) throws IOException {
            int size = in.readInt();
            lsValues = new ArrayList<Data>(size);
            for (int i = 0; i < size; i++) {
                Data data = createNewData();
                data.readData(in);
                lsValues.add(data);
            }
        }

        public void writeData(DataOutput out) throws IOException {
            out.writeInt(lsValues.size());
            for (Data data : lsValues) {
                data.writeData(out);
            }
        }
    }
}
