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
package org.neo4j.com;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

public class ChunkingChannelBuffer implements ChannelBuffer
{
    static final byte CONTINUATION_LAST = 0;
    static final byte CONTINUATION_MORE = 1;
    
    private ChannelBuffer buffer;
    private final Channel channel;
    private final int capacity;
    private int continuationPosition;

    public ChunkingChannelBuffer( ChannelBuffer buffer, Channel channel, int capacity )
    {
        this.buffer = buffer;
        this.channel = channel;
        this.capacity = capacity;
        addRoomForContinuationHeader();
    }

    private void addRoomForContinuationHeader()
    {
        continuationPosition = writerIndex();
        buffer.writeByte( CONTINUATION_LAST );
    }
    
    private void setContinuation( byte value )
    {
        buffer.setByte( continuationPosition, value );
    }

    public ChannelBufferFactory factory()
    {
        return buffer.factory();
    }

    public int capacity()
    {
        return buffer.capacity();
    }

    public ByteOrder order()
    {
        return buffer.order();
    }

    public boolean isDirect()
    {
        return buffer.isDirect();
    }

    public int readerIndex()
    {
        return buffer.readerIndex();
    }

    public void readerIndex( int readerIndex )
    {
        buffer.readerIndex( readerIndex );
    }

    public int writerIndex()
    {
        return buffer.writerIndex();
    }

    public void writerIndex( int writerIndex )
    {
        buffer.writerIndex( writerIndex );
    }

    public void setIndex( int readerIndex, int writerIndex )
    {
        buffer.setIndex( readerIndex, writerIndex );
    }

    public int readableBytes()
    {
        return buffer.readableBytes();
    }

    public int writableBytes()
    {
        return buffer.writableBytes();
    }

    public boolean readable()
    {
        return buffer.readable();
    }

    public boolean writable()
    {
        return buffer.writable();
    }

    public void clear()
    {
        buffer.clear();
    }

    public void markReaderIndex()
    {
        buffer.markReaderIndex();
    }

    public void resetReaderIndex()
    {
        buffer.resetReaderIndex();
    }

    public void markWriterIndex()
    {
        buffer.markWriterIndex();
    }

    public void resetWriterIndex()
    {
        buffer.resetWriterIndex();
    }

    public void discardReadBytes()
    {
        buffer.discardReadBytes();
    }

    public void ensureWritableBytes( int writableBytes )
    {
        buffer.ensureWritableBytes( writableBytes );
    }

    public byte getByte( int index )
    {
        return buffer.getByte( index );
    }

    public short getUnsignedByte( int index )
    {
        return buffer.getUnsignedByte( index );
    }

    public short getShort( int index )
    {
        return buffer.getShort( index );
    }

    public int getUnsignedShort( int index )
    {
        return buffer.getUnsignedShort( index );
    }

    public int getMedium( int index )
    {
        return buffer.getMedium( index );
    }

    public int getUnsignedMedium( int index )
    {
        return buffer.getUnsignedMedium( index );
    }

    public int getInt( int index )
    {
        return buffer.getInt( index );
    }

    public long getUnsignedInt( int index )
    {
        return buffer.getUnsignedInt( index );
    }

    public long getLong( int index )
    {
        return buffer.getLong( index );
    }

    public char getChar( int index )
    {
        return buffer.getChar( index );
    }

    public float getFloat( int index )
    {
        return buffer.getFloat( index );
    }

    public double getDouble( int index )
    {
        return buffer.getDouble( index );
    }

    public void getBytes( int index, ChannelBuffer dst )
    {
        buffer.getBytes( index, dst );
    }

    public void getBytes( int index, ChannelBuffer dst, int length )
    {
        buffer.getBytes( index, dst, length );
    }

    public void getBytes( int index, ChannelBuffer dst, int dstIndex, int length )
    {
        buffer.getBytes( index, dst, dstIndex, length );
    }

    public void getBytes( int index, byte[] dst )
    {
        buffer.getBytes( index, dst );
    }

    public void getBytes( int index, byte[] dst, int dstIndex, int length )
    {
        buffer.getBytes( index, dst, dstIndex, length );
    }

    public void getBytes( int index, ByteBuffer dst )
    {
        buffer.getBytes( index, dst );
    }

    public void getBytes( int index, OutputStream out, int length ) throws IOException
    {
        buffer.getBytes( index, out, length );
    }

    public int getBytes( int index, GatheringByteChannel out, int length ) throws IOException
    {
        return buffer.getBytes( index, out, length );
    }

    public void setByte( int index, int value )
    {
        buffer.setByte( index, value );
    }

    public void setShort( int index, int value )
    {
        buffer.setShort( index, value );
    }

    public void setMedium( int index, int value )
    {
        buffer.setMedium( index, value );
    }

    public void setInt( int index, int value )
    {
        buffer.setInt( index, value );
    }

    public void setLong( int index, long value )
    {
        buffer.setLong( index, value );
    }

    public void setChar( int index, int value )
    {
        buffer.setChar( index, value );
    }

    public void setFloat( int index, float value )
    {
        buffer.setFloat( index, value );
    }

    public void setDouble( int index, double value )
    {
        buffer.setDouble( index, value );
    }

    public void setBytes( int index, ChannelBuffer src )
    {
        buffer.setBytes( index, src );
    }

    public void setBytes( int index, ChannelBuffer src, int length )
    {
        buffer.setBytes( index, src, length );
    }

    public void setBytes( int index, ChannelBuffer src, int srcIndex, int length )
    {
        buffer.setBytes( index, src, srcIndex, length );
    }

    public void setBytes( int index, byte[] src )
    {
        buffer.setBytes( index, src );
    }

    public void setBytes( int index, byte[] src, int srcIndex, int length )
    {
        buffer.setBytes( index, src, srcIndex, length );
    }

    public void setBytes( int index, ByteBuffer src )
    {
        buffer.setBytes( index, src );
    }

    public int setBytes( int index, InputStream in, int length ) throws IOException
    {
        return buffer.setBytes( index, in, length );
    }

    public int setBytes( int index, ScatteringByteChannel in, int length ) throws IOException
    {
        return buffer.setBytes( index, in, length );
    }

    public void setZero( int index, int length )
    {
        buffer.setZero( index, length );
    }

    public byte readByte()
    {
        return buffer.readByte();
    }

    public short readUnsignedByte()
    {
        return buffer.readUnsignedByte();
    }

    public short readShort()
    {
        return buffer.readShort();
    }

    public int readUnsignedShort()
    {
        return buffer.readUnsignedShort();
    }

    public int readMedium()
    {
        return buffer.readMedium();
    }

    public int readUnsignedMedium()
    {
        return buffer.readUnsignedMedium();
    }

    public int readInt()
    {
        return buffer.readInt();
    }

    public long readUnsignedInt()
    {
        return buffer.readUnsignedInt();
    }

    public long readLong()
    {
        return buffer.readLong();
    }

    public char readChar()
    {
        return buffer.readChar();
    }

    public float readFloat()
    {
        return buffer.readFloat();
    }

    public double readDouble()
    {
        return buffer.readDouble();
    }

    public ChannelBuffer readBytes( int length )
    {
        return buffer.readBytes( length );
    }

    public ChannelBuffer readBytes( ChannelBufferIndexFinder indexFinder )
    {
        return buffer.readBytes( indexFinder );
    }

    public ChannelBuffer readSlice( int length )
    {
        return buffer.readSlice( length );
    }

    public ChannelBuffer readSlice( ChannelBufferIndexFinder indexFinder )
    {
        return buffer.readSlice( indexFinder );
    }

    public void readBytes( ChannelBuffer dst )
    {
        buffer.readBytes( dst );
    }

    public void readBytes( ChannelBuffer dst, int length )
    {
        buffer.readBytes( dst, length );
    }

    public void readBytes( ChannelBuffer dst, int dstIndex, int length )
    {
        buffer.readBytes( dst, dstIndex, length );
    }

    public void readBytes( byte[] dst )
    {
        buffer.readBytes( dst );
    }

    public void readBytes( byte[] dst, int dstIndex, int length )
    {
        buffer.readBytes( dst, dstIndex, length );
    }

    public void readBytes( ByteBuffer dst )
    {
        buffer.readBytes( dst );
    }

    public void readBytes( OutputStream out, int length ) throws IOException
    {
        buffer.readBytes( out, length );
    }

    public int readBytes( GatheringByteChannel out, int length ) throws IOException
    {
        return buffer.readBytes( out, length );
    }

    public void skipBytes( int length )
    {
        buffer.skipBytes( length );
    }

    public int skipBytes( ChannelBufferIndexFinder indexFinder )
    {
        return buffer.skipBytes( indexFinder );
    }
    
    private void sendChunkIfNeeded( int bytesPlus )
    {
        if ( writerIndex()+bytesPlus >= capacity )
        {
            setContinuation( CONTINUATION_MORE );
            writeCurrentChunk();
            // TODO Reuse buffers?
            buffer = ChannelBuffers.dynamicBuffer();
            addRoomForContinuationHeader();
        }
    }

    private void writeCurrentChunk()
    {
        channel.write( buffer );
    }

    public void done()
    {
        if ( writable() )
        {
            writeCurrentChunk();
        }
    }

    public void writeByte( int value )
    {
        sendChunkIfNeeded( 1 );
        buffer.writeByte( value );
    }

    public void writeShort( int value )
    {
        sendChunkIfNeeded( 2 );
        buffer.writeShort( value );
    }

    public void writeMedium( int value )
    {
        sendChunkIfNeeded( 4 );
        buffer.writeMedium( value );
    }

    public void writeInt( int value )
    {
        sendChunkIfNeeded( 4 );
        buffer.writeInt( value );
    }

    public void writeLong( long value )
    {
        sendChunkIfNeeded( 8 );
        buffer.writeLong( value );
    }

    public void writeChar( int value )
    {
        sendChunkIfNeeded( 2 );
        buffer.writeChar( value );
    }

    public void writeFloat( float value )
    {
        sendChunkIfNeeded( 8 );
        buffer.writeFloat( value );
    }

    public void writeDouble( double value )
    {
        sendChunkIfNeeded( 8 );
        buffer.writeDouble( value );
    }

    public void writeBytes( ChannelBuffer src )
    {
        sendChunkIfNeeded( src.capacity() );
        buffer.writeBytes( src );
    }

    public void writeBytes( ChannelBuffer src, int length )
    {
        sendChunkIfNeeded( length );
        buffer.writeBytes( src, length );
    }

    public void writeBytes( ChannelBuffer src, int srcIndex, int length )
    {
        sendChunkIfNeeded( length );
        buffer.writeBytes( src, srcIndex, length );
    }

    public void writeBytes( byte[] src )
    {
        sendChunkIfNeeded( src.length );
        buffer.writeBytes( src );
    }

    public void writeBytes( byte[] src, int srcIndex, int length )
    {
        sendChunkIfNeeded( length );
        buffer.writeBytes( src, srcIndex, length );
    }

    public void writeBytes( ByteBuffer src )
    {
        sendChunkIfNeeded( src.limit() );
        buffer.writeBytes( src );
    }

    public int writeBytes( InputStream in, int length ) throws IOException
    {
        sendChunkIfNeeded( length );
        return buffer.writeBytes( in, length );
    }

    public int writeBytes( ScatteringByteChannel in, int length ) throws IOException
    {
        sendChunkIfNeeded( length );
        return buffer.writeBytes( in, length );
    }

    public void writeZero( int length )
    {
        sendChunkIfNeeded( length );
        buffer.writeZero( length );
    }

    public int indexOf( int fromIndex, int toIndex, byte value )
    {
        return buffer.indexOf( fromIndex, toIndex, value );
    }

    public int indexOf( int fromIndex, int toIndex, ChannelBufferIndexFinder indexFinder )
    {
        return buffer.indexOf( fromIndex, toIndex, indexFinder );
    }

    public int bytesBefore( byte value )
    {
        return buffer.bytesBefore( value );
    }

    public int bytesBefore( ChannelBufferIndexFinder indexFinder )
    {
        return buffer.bytesBefore( indexFinder );
    }

    public int bytesBefore( int length, byte value )
    {
        return buffer.bytesBefore( length, value );
    }

    public int bytesBefore( int length, ChannelBufferIndexFinder indexFinder )
    {
        return buffer.bytesBefore( length, indexFinder );
    }

    public int bytesBefore( int index, int length, byte value )
    {
        return buffer.bytesBefore( index, length, value );
    }

    public int bytesBefore( int index, int length, ChannelBufferIndexFinder indexFinder )
    {
        return buffer.bytesBefore( index, length, indexFinder );
    }

    public ChannelBuffer copy()
    {
        return buffer.copy();
    }

    public ChannelBuffer copy( int index, int length )
    {
        return buffer.copy( index, length );
    }

    public ChannelBuffer slice()
    {
        return buffer.slice();
    }

    public ChannelBuffer slice( int index, int length )
    {
        return buffer.slice( index, length );
    }

    public ChannelBuffer duplicate()
    {
        return buffer.duplicate();
    }

    public ByteBuffer toByteBuffer()
    {
        return buffer.toByteBuffer();
    }

    public ByteBuffer toByteBuffer( int index, int length )
    {
        return buffer.toByteBuffer( index, length );
    }

    public ByteBuffer[] toByteBuffers()
    {
        return buffer.toByteBuffers();
    }

    public ByteBuffer[] toByteBuffers( int index, int length )
    {
        return buffer.toByteBuffers( index, length );
    }

    public boolean hasArray()
    {
        return buffer.hasArray();
    }

    public byte[] array()
    {
        return buffer.array();
    }

    public int arrayOffset()
    {
        return buffer.arrayOffset();
    }

    public String toString( Charset charset )
    {
        return buffer.toString( charset );
    }

    public String toString( int index, int length, Charset charset )
    {
        return buffer.toString( index, length, charset );
    }

    public String toString( String charsetName )
    {
        return buffer.toString( charsetName );
    }

    public String toString( String charsetName, ChannelBufferIndexFinder terminatorFinder )
    {
        return buffer.toString( charsetName, terminatorFinder );
    }

    public String toString( int index, int length, String charsetName )
    {
        return buffer.toString( index, length, charsetName );
    }

    public String toString( int index, int length, String charsetName,
            ChannelBufferIndexFinder terminatorFinder )
    {
        return buffer.toString( index, length, charsetName, terminatorFinder );
    }

    public int compareTo( ChannelBuffer buffer )
    {
        return buffer.compareTo( buffer );
    }

    public String toString()
    {
        return buffer.toString();
    }
}
