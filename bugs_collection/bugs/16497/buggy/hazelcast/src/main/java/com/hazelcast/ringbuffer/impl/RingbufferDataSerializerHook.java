/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
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
 */

package com.hazelcast.ringbuffer.impl;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.DataSerializerHook;
import com.hazelcast.nio.serialization.FactoryIdHelper;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.ringbuffer.impl.operations.AddAllBackupOperation;
import com.hazelcast.ringbuffer.impl.operations.AddAllOperation;
import com.hazelcast.ringbuffer.impl.operations.AddBackupOperation;
import com.hazelcast.ringbuffer.impl.operations.AddOperation;
import com.hazelcast.ringbuffer.impl.operations.GenericOperation;
import com.hazelcast.ringbuffer.impl.operations.ReadManyOperation;
import com.hazelcast.ringbuffer.impl.operations.ReadOneOperation;
import com.hazelcast.ringbuffer.impl.operations.ReplicationOperation;

/**
 * The {@link DataSerializerHook} for the Ringbuffer.
 */
public class RingbufferDataSerializerHook implements DataSerializerHook {

    public static final int F_ID = FactoryIdHelper.getFactoryId(FactoryIdHelper.COLLECTION_DS_FACTORY, -26);

    public static final int GENERIC_OPERATION = 1;
    public static final int ADD_BACKUP_OPERATION = 2;
    public static final int ADD_OPERATION = 3;
    public static final int READ_ONE_OPERATION = 4;
    public static final int REPLICATION_OPERATION = 5;
    public static final int READ_MANY_OPERATION = 6;
    public static final int ADD_ALL_OPERATION = 7;
    public static final int ADD_ALL_BACKUP_OPERATION = 8;
    public static final int READ_RESULT_SET = 9;

    @Override
    public int getFactoryId() {
        return F_ID;
    }

    @Override
    public DataSerializableFactory createFactory() {
        return new DataSerializableFactory() {
            @Override
            public IdentifiedDataSerializable create(int typeId) {
                switch (typeId) {
                    case ADD_BACKUP_OPERATION:
                        return new AddBackupOperation();
                    case ADD_OPERATION:
                        return new AddOperation();
                    case READ_ONE_OPERATION:
                        return new ReadOneOperation();
                    case REPLICATION_OPERATION:
                        return new ReplicationOperation();
                    case GENERIC_OPERATION:
                        return new GenericOperation();
                    case READ_MANY_OPERATION:
                        return new ReadManyOperation();
                    case ADD_ALL_OPERATION:
                        return new AddAllOperation();
                    case ADD_ALL_BACKUP_OPERATION:
                        return new AddAllBackupOperation();
                    case READ_RESULT_SET:
                        return new ReadResultSetImpl();
                    default:
                        return null;
                }
            }
        };
    }
}