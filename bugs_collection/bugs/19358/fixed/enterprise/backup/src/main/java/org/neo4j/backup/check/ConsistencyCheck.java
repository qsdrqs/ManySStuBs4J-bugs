/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.backup.check;

import org.neo4j.kernel.impl.nioneo.store.AbstractBaseRecord;
import org.neo4j.kernel.impl.nioneo.store.DynamicRecord;
import org.neo4j.kernel.impl.nioneo.store.NodeRecord;
import org.neo4j.kernel.impl.nioneo.store.PrimitiveRecord;
import org.neo4j.kernel.impl.nioneo.store.PropertyBlock;
import org.neo4j.kernel.impl.nioneo.store.PropertyIndexRecord;
import org.neo4j.kernel.impl.nioneo.store.PropertyRecord;
import org.neo4j.kernel.impl.nioneo.store.PropertyType;
import org.neo4j.kernel.impl.nioneo.store.Record;
import org.neo4j.kernel.impl.nioneo.store.RecordStore;
import org.neo4j.kernel.impl.nioneo.store.RelationshipRecord;
import org.neo4j.kernel.impl.nioneo.store.RelationshipTypeRecord;
import org.neo4j.kernel.impl.nioneo.store.StoreAccess;

/**
 * Finds inconsistency in a Neo4j store.
 *
 * Warning: will not find "dangling" records, i.e. records that are correct but
 * not referenced.
 *
 * Warning: will only find multiple references to the same property chain or
 * dynamic record chain for incremental checks (if the {@link RecordStore stores}
 * are {@link DiffRecordStore diff stores}). Also, this checking is very
 * incomplete.
 */
public class ConsistencyCheck extends RecordStore.Processor implements Runnable
{
    public static void main( String[] args )
    {
        StoreAccess stores = new StoreAccess( args[0] );
        try
        {
            new ConsistencyCheck( stores ).run();
        }
        finally
        {
            stores.close();
        }
    }

    private final RecordStore<NodeRecord> nodes;
    private final RecordStore<RelationshipRecord> rels;
    private final RecordStore<PropertyRecord> props;
    private final RecordStore<DynamicRecord> strings, arrays;
    private final RecordStore<PropertyIndexRecord>  propIndexes;
    private final RecordStore<RelationshipTypeRecord>  relTypes;
    private final RecordStore<DynamicRecord> propKeys;
    private final RecordStore<DynamicRecord> typeNames;
    private long brokenNodes, brokenRels, brokenProps, brokenStrings, brokenArrays, brokenTypes, brokenKeys;

    public ConsistencyCheck( StoreAccess stores )
    {
        this.nodes = stores.getNodeStore();
        this.rels = stores.getRelationshipStore();
        this.props = stores.getPropertyStore();
        this.strings = stores.getStringStore();
        this.arrays = stores.getArrayStore();
        this.relTypes = stores.getRelationshipTypeStore();
        this.propIndexes = stores.getPropertyIndexStore();
        this.propKeys = stores.getPropertyKeyStore();
        this.typeNames = stores.getTypeNameStore();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void run()
    {
        applyFiltered( nodes, RecordStore.IN_USE );
        applyFiltered( rels, RecordStore.IN_USE );
        applyFiltered( props, RecordStore.IN_USE );
        applyFiltered( strings, RecordStore.IN_USE );
        applyFiltered( arrays, RecordStore.IN_USE );
        applyFiltered( relTypes, RecordStore.IN_USE );
        applyFiltered( propIndexes, RecordStore.IN_USE );
        applyFiltered( propKeys, RecordStore.IN_USE );
        applyFiltered( typeNames, RecordStore.IN_USE );
        checkResult();
    }

    public void checkResult() throws AssertionError
    {
        if ( brokenNodes != 0 || brokenRels != 0 || brokenProps != 0 || brokenStrings != 0 || brokenArrays != 0 || brokenTypes != 0 || brokenKeys != 0 )
        {
            throw new AssertionError(
                    String.format(
                            "Store level inconsistency found in %d nodes, %d relationships, %d properties, %d strings, %d arrays, %d types, %d keys",
                            brokenNodes, brokenRels, brokenProps, brokenStrings, brokenArrays, brokenTypes, brokenKeys ) );
        }
    }

    @Override
    public void processNode( RecordStore<NodeRecord> store, NodeRecord node )
    {
        if ( !node.inUse() ) return;
        if ( checkNode( node ) ) brokenNodes++;
    }

    @Override
    public void processRelationship( RecordStore<RelationshipRecord> store, RelationshipRecord rel )
    {
        if ( !rel.inUse() ) return;
        if ( checkRelationship( rel ) ) brokenRels++;
    }

    @Override
    public void processProperty( RecordStore<PropertyRecord> store, PropertyRecord property )
    {
        if ( !property.inUse() ) return;
        if ( checkProperty( property ) ) brokenProps++;
    }

    @Override
    public void processString( RecordStore<DynamicRecord> store, DynamicRecord string )
    {
        if ( !string.inUse() ) return;
        if ( checkDynamic( store, string ) ) brokenStrings++;
    }

    @Override
    public void processArray( RecordStore<DynamicRecord> store, DynamicRecord array )
    {
        if ( !array.inUse() ) return;
        if ( checkDynamic( store, array ) ) brokenArrays++;
    }

    @Override
    public void processRelationshipType( RecordStore<RelationshipTypeRecord> store, RelationshipTypeRecord type )
    {
        if ( !type.inUse() ) return;
        if ( checkType( type ) ) brokenTypes++;
    }

    @Override
    public void processPropertyIndex( RecordStore<PropertyIndexRecord> store, PropertyIndexRecord index )
    {
        if ( !index.inUse() ) return;
        if ( checkKey( index ) ) brokenKeys++;
    }

    private boolean checkNode( NodeRecord node )
    {
        boolean fail = false;
        long relId = node.getNextRel();
        if ( !Record.NO_NEXT_RELATIONSHIP.value( relId ) )
        {
            RelationshipRecord rel = rels.forceGetRecord( relId );
            if ( !rel.inUse() )
                fail |= inconsistent( nodes, node, rels, rel, "reference to relationship not in use" );
            else if ( !( rel.getFirstNode() == node.getId() || rel.getSecondNode() == node.getId() ) )
                fail |= inconsistent( nodes, node, rels, rel, "reference to relationship that does not reference back" );
        }
        if ( props != null )
        {
            long propId = node.getNextProp();
            if ( !Record.NO_NEXT_PROPERTY.value( propId ) )
            {
                PropertyRecord prop = props.forceGetRecord( propId );
                if ( !prop.inUse() )
                    fail |= inconsistent( nodes, node, props, prop, "reference to property not in use" );
                else if ( prop.getRelId() != -1 || ( prop.getNodeId() != -1 && prop.getNodeId() != node.getId() ) )
                    fail |= inconsistent( nodes, node, props, prop, "reference to property for other entity" );
            }
        }
        return fail;
    }

    private boolean checkRelationship( RelationshipRecord rel )
    {
        boolean fail = false;
        if ( rel.getType() < 0 ) fail |= inconsistent( rels, rel, "invalid type id" );
        else
        {
            RelationshipTypeRecord type = relTypes.forceGetRecord( rel.getType() );
            if ( !type.inUse() ) fail |= inconsistent( rels, rel, relTypes, type, "type not in use" );
        }
        for ( RelationshipField field : relFields )
        {
            long otherId = field.relOf( rel );
            if ( otherId == field.none )
            {
                Long nodeId = field.nodeOf( rel );
                if ( nodeId != null )
                {
                    NodeRecord node = nodes.forceGetRecord( nodeId );
                    if ( !node.inUse() || node.getNextRel() != rel.getId() )
                        fail |= inconsistent( rels, rel, nodes, node, "invalid " + field.name()
                                              + " reference, node does not reference back" );
                }
            }
            else
            {
                RelationshipRecord other = rels.forceGetRecord( otherId );
                if ( !other.inUse() )
                    fail |= inconsistent( rels, rel, other, field.name() + " reference to record not used" );
                else if ( !field.invConsistent( rel, other ) )
                    fail |= inconsistent( rels, rel, other, "not part of the same chain, invalid "
                                          + field.name() + " reference" );
            }
        }
        for ( NodeField field : nodeFields )
        {
            long nodeId = field.get( rel );
            if ( nodeId < 0 )
                fail |= inconsistent( rels, rel, "invalid " + field.name() + " node reference" );
            else
            {
                NodeRecord node = nodes.forceGetRecord( nodeId );
                if ( !node.inUse() )
                    fail |= inconsistent( rels, rel, nodes, node, "invalid " + field.name() + " node reference, not in use" );
            }
        }
        if ( props != null )
        {
            long propId = rel.getNextProp();
            if ( !Record.NO_NEXT_PROPERTY.value( propId ) )
            {
                PropertyRecord prop = props.forceGetRecord( propId );
                if ( !prop.inUse() )
                    fail |= inconsistent( rels, rel, props, prop, "reference to property not in use" );
                else if ( prop.getNodeId() != -1 || ( prop.getRelId() != -1 && prop.getRelId() != rel.getId() ) )
                    fail |= inconsistent( rels, rel, props, prop, "reference to property for other entity" );
            }
        }
        return fail;
    }

    private boolean checkProperty( PropertyRecord property )
    {
        boolean fail = false;
        long nextId = property.getNextProp();
        if ( !Record.NO_NEXT_PROPERTY.value( nextId ) )
        {
            PropertyRecord next = props.forceGetRecord( nextId );
            if ( !next.inUse() )
                fail |= inconsistent( props, property, next, "invalid next reference, next record not in use" );
            if ( next.getPrevProp() != property.getId() )
                fail |= inconsistent( props, property, next, "invalid next reference, next record does not reference back" );
        }
        long prevId = property.getPrevProp();
        if ( !Record.NO_PREVIOUS_PROPERTY.value( prevId ) )
        {
            PropertyRecord prev = props.forceGetRecord( prevId );
            if ( !prev.inUse() )
                fail |= inconsistent( props, property, prev, "invalid prev reference, prev record not in use" );
            if ( prev.getNextProp() != property.getId() )
                fail |= inconsistent( props, property, prev, "invalid prev reference, prev record does not reference back" );
        }
        else // property is first in chain
        {
            if ( property.getNodeId() != -1 )
                fail |= checkPropertyOwnerReference( property, nodes );
            else if ( property.getRelId() != -1 )
                fail |= checkPropertyOwnerReference( property, rels );
            // else - this information is only available from logs through DiffRecordStore
        }
        for ( PropertyBlock block : property.getPropertyBlocks() )
        {
            if ( block.getKeyIndexId() < 0 ) fail |= inconsistent( props, property, "invalid key id of " + block );
            else
            {
                PropertyIndexRecord key = propIndexes.forceGetRecord( block.getKeyIndexId() );
                if ( !key.inUse() ) fail |= inconsistent( props, property, propIndexes, key, "key not in use for " + block );
            }
            RecordStore<DynamicRecord> dynStore = null;
            PropertyType type = block.forceGetType();
            if ( type == null )
            {
                fail |= inconsistent( props, property, "illegal property type" );
            }
            else switch ( block.getType() )
            {
            case STRING:
                dynStore = strings;
                break;
            case ARRAY:
                dynStore = arrays;
                break;
            }
            if ( dynStore != null )
            {
                DynamicRecord dynrec = dynStore.forceGetRecord( block.getSingleValueLong() );
                if ( !dynrec.inUse() )
                    fail |= inconsistent( props, property, dynStore, dynrec, "first dynamic record not in use" );
            }
        }
        return fail;
    }

    private boolean checkPropertyOwnerReference( PropertyRecord property, RecordStore<? extends PrimitiveRecord> entityStore )
    {
        boolean fail = false;
        PrimitiveRecord entity = entityStore.forceGetRecord( property.getNodeId() );
        if ( !entity.inUse() )
            fail |= inconsistent( props, property, entityStore, entity, "owning record not in use" );
        else if ( entity.getNextProp() != property.getId() )
            fail |= inconsistent( props, property, entityStore, entity, "owning record does not reference back" );
        if ( entityStore instanceof DiffRecordStore<?> )
        {
            DiffRecordStore<? extends PrimitiveRecord> diffs = (DiffRecordStore<? extends PrimitiveRecord>) entityStore;
            PrimitiveRecord old = diffs.forceGetRaw( entity.getId() );
            // IF old is in use and references a property record
            if ( old.inUse() && !Record.NO_NEXT_PROPERTY.value( old.getNextProp() ) )
                // AND that property record is not the same as this property record
                if ( old.getNextProp() != property.getId() )
                    // THEN that property record must also have been updated!
                    if ( !( (DiffRecordStore<?>) props ).isModified( old.getNextProp() ) )
                        fail |= inconsistent( props, property, entityStore, entity, "overwriting used property record" );
        }
        return fail;
    }

    private boolean checkDynamic( RecordStore<DynamicRecord> store, DynamicRecord record )
    {
        boolean fail = false;
        long nextId = record.getNextBlock();
        if ( !Record.NO_NEXT_BLOCK.value( nextId ) )
        {
            // If next is set, then it must be in use
            DynamicRecord next = store.forceGetRecord( nextId );
            if ( !next.inUse() )
                fail |= inconsistent( store, record, next, "next record not in use" );
            // If next is set, then the size must be max
            if ( record.getLength() < store.getRecordSize() - store.getRecordHeaderSize() )
                fail |= inconsistent( store, record, "next record set, but length less than maximum" );
        }
        if ( record.getId() != 0
             && record.getLength() > store.getRecordSize()
                                  - store.getRecordHeaderSize() )
        {
            /*
             *  The length must always be less than or equal to max,
             *  except for the first dynamic record in a store, which
             *  does not conform to the usual format
             */
            fail |= inconsistent( store, record, "length larger than maximum for store" );
        }
        if ( store instanceof DiffRecordStore<?> )
        {
            DiffRecordStore<DynamicRecord> diffs = (DiffRecordStore<DynamicRecord>) store;
            DynamicRecord prev = diffs.forceGetRaw( record.getId() );
            if ( prev.inUse() ) fail |= inconsistent( store, record, prev, "overwriting used dynamic record" );
        }
        return fail;
    }

    private boolean checkType( RelationshipTypeRecord type )
    {
        if ( Record.NO_NEXT_BLOCK.value( type.getTypeBlock() ) ) return false; // accept this
        DynamicRecord record = typeNames.forceGetRecord( type.getTypeBlock() );
        if ( !record.inUse() ) return inconsistent( relTypes, type, typeNames, record, "reference to unused type name" );
        return false;
    }

    private boolean checkKey( PropertyIndexRecord key )
    {
        if ( Record.NO_NEXT_BLOCK.value( key.getKeyBlockId() ) ) return false; // accept this
        DynamicRecord record = propKeys.forceGetRecord( key.getKeyBlockId() );
        if ( !record.inUse() ) return inconsistent( propIndexes, key, propKeys, record, "reference to unused key string" );
        return false;
    }

    // Inconsistency between two records
    private <R1 extends AbstractBaseRecord, R2 extends AbstractBaseRecord> boolean inconsistent(
            RecordStore<R1> recordStore, R1 record, RecordStore<? extends R2> referredStore, R2 referred, String message )
    {
        report( recordStore, record, referredStore, referred, message );
        return true;
    }
    
    private <R extends AbstractBaseRecord> boolean inconsistent(
            RecordStore<R> store, R record, R referred, String message )
    {
        report( store, record, store, referred, message );
        return true;
    }

    // Internal inconsistency in a single record
    private <R extends AbstractBaseRecord> boolean inconsistent( RecordStore<R> store, R record, String message )
    {
        report( store, record, message );
        return true;
    }

    protected <R1 extends AbstractBaseRecord, R2 extends AbstractBaseRecord> void report(
            RecordStore<R1> recordStore, R1 record, RecordStore<? extends R2> referredStore, R2 referred, String message )
    {
        System.err.println( record + " " + referred + " //" + message );
    }

    protected <R extends AbstractBaseRecord> void report( RecordStore<R> recordStore, R record, String message )
    {
        System.err.println( record + " //" + message );
    }

    private static NodeField[] nodeFields = NodeField.values();
    private static RelationshipField[] relFields = RelationshipField.values();

    private enum NodeField
    {
        FIRST
        {
            @Override
            long get( RelationshipRecord rel )
            {
                return rel.getFirstNode();
            }
        },
        SECOND
        {
            @Override
            long get( RelationshipRecord rel )
            {
                return rel.getSecondNode();
            }
        };

        abstract long get( RelationshipRecord rel );
    }

    @SuppressWarnings( "boxing" )
    private enum RelationshipField
    {
        FIRST_NEXT( true, Record.NO_NEXT_RELATIONSHIP )
        {
            @Override
            long relOf( RelationshipRecord rel )
            {
                return rel.getFirstNextRel();
            }

            @Override
            boolean invConsistent( RelationshipRecord rel, RelationshipRecord other )
            {
                long node = getNode( rel );
                if ( other.getFirstNode() == node ) return other.getFirstPrevRel() == rel.getId();
                if ( other.getSecondNode() == node ) return other.getSecondPrevRel() == rel.getId();
                return false;
            }
        },
        FIRST_PREV( true, Record.NO_PREV_RELATIONSHIP )
        {
            @Override
            long relOf( RelationshipRecord rel )
            {
                return rel.getFirstPrevRel();
            }

            @Override
            Long nodeOf( RelationshipRecord rel )
            {
                return getNode( rel );
            }

            @Override
            boolean invConsistent( RelationshipRecord rel, RelationshipRecord other )
            {
                long node = getNode( rel );
                if ( other.getFirstNode() == node ) return other.getFirstNextRel() == rel.getId();
                if ( other.getSecondNode() == node ) return other.getSecondNextRel() == rel.getId();
                return false;
            }
        },
        SECOND_NEXT( false, Record.NO_NEXT_RELATIONSHIP )
        {
            @Override
            long relOf( RelationshipRecord rel )
            {
                return rel.getSecondNextRel();
            }

            @Override
            boolean invConsistent( RelationshipRecord rel, RelationshipRecord other )
            {
                long node = getNode( rel );
                if ( other.getFirstNode() == node ) return other.getFirstPrevRel() == rel.getId();
                if ( other.getSecondNode() == node ) return other.getSecondPrevRel() == rel.getId();
                return false;
            }
        },
        SECOND_PREV( false, Record.NO_PREV_RELATIONSHIP )
        {
            @Override
            long relOf( RelationshipRecord rel )
            {
                return rel.getSecondPrevRel();
            }

            @Override
            Long nodeOf( RelationshipRecord rel )
            {
                return getNode( rel );
            }

            @Override
            boolean invConsistent( RelationshipRecord rel, RelationshipRecord other )
            {
                long node = getNode( rel );
                if ( other.getFirstNode() == node ) return other.getFirstNextRel() == rel.getId();
                if ( other.getSecondNode() == node ) return other.getSecondNextRel() == rel.getId();
                return false;
            }
        };

        private final boolean first;
        final long none;

        private RelationshipField( boolean first, Record none )
        {
            this.first = first;
            this.none = none.intValue();
        }

        abstract boolean invConsistent( RelationshipRecord rel, RelationshipRecord other );

        long getNode( RelationshipRecord rel )
        {
            return first ? rel.getFirstNode() : rel.getSecondNode();
        }

        abstract long relOf( RelationshipRecord rel );

        Long nodeOf( RelationshipRecord rel )
        {
            return null;
        }
    }
}
