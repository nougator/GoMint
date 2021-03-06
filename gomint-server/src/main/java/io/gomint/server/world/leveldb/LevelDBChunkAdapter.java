/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world.leveldb;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.math.MathUtils;
import io.gomint.server.entity.Entity;
import io.gomint.server.entity.tileentity.SerializationReason;
import io.gomint.server.entity.tileentity.TileEntity;
import io.gomint.server.util.Allocator;
import io.gomint.server.util.BlockIdentifier;
import io.gomint.server.util.Palette;
import io.gomint.server.util.collection.FreezableSortedMap;
import io.gomint.server.world.BlockRuntimeIDs;
import io.gomint.server.world.ChunkAdapter;
import io.gomint.server.world.ChunkSlice;
import io.gomint.server.world.WorldAdapter;
import io.gomint.server.world.block.Block;
import io.gomint.taglib.AllocationLimitReachedException;
import io.gomint.taglib.NBTReader;
import io.gomint.taglib.NBTTagCompound;
import io.gomint.taglib.NBTWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/**
 * @author geNAZt
 * @version 1.0
 */
public class LevelDBChunkAdapter extends ChunkAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger( LevelDBChunkAdapter.class );
    private static final int BLOCK_VERSION = 17760256;
    private int chunkVersion;

    /**
     * Create a new level db backed chunk
     *
     * @param worldAdapter which loaded this chunk
     * @param x            position of chunk
     * @param z            position of chunk
     * @param chunkVersion version of this chunk
     * @param populated    true when chunk is already populated, false when not
     */
    public LevelDBChunkAdapter( WorldAdapter worldAdapter, int x, int z, byte chunkVersion, boolean populated ) {
        super( worldAdapter, x, z );
        this.chunkVersion = chunkVersion;
        this.setPopulated( populated );
        this.loadedTime = this.lastSavedTimestamp = worldAdapter.getServer().getCurrentTickTime();
    }

    public LevelDBChunkAdapter( WorldAdapter worldAdapter, int x, int z ) {
        super( worldAdapter, x, z );
        this.chunkVersion = 15;

        this.loadedTime = this.lastSavedTimestamp = worldAdapter.getServer().getCurrentTickTime();
    }

    void save( DB db ) {
        WriteBatch writeBatch = db.createWriteBatch();

        // We do blocks first
        for ( int i = 0; i < this.chunkSlices.length; i++ ) {
            if ( this.chunkSlices[i] == null ) {
                continue;
            }

            saveChunkSlice( i, writeBatch );
        }

        // Save metadata
        byte[] key = ( (LevelDBWorldAdapter) this.world ).getKey( this.x, this.z, (byte) 0x76 );
        byte[] val = new byte[]{ (byte) this.chunkVersion };
        writeBatch.put( key, val );

        key = ( (LevelDBWorldAdapter) this.world ).getKey( this.x, this.z, (byte) 0x36 );
        val = isPopulated() ? new byte[]{ 2, 0, 0, 0 } : new byte[]{ 0, 0, 0, 0 };
        writeBatch.put( key, val );

        // Save tiles
        ByteBuf out = PooledByteBufAllocator.DEFAULT.heapBuffer();
        NBTWriter nbtWriter = new NBTWriter( out, ByteOrder.LITTLE_ENDIAN );
        for ( TileEntity tileEntity : this.getTileEntities() ) {
            NBTTagCompound compound = new NBTTagCompound( "" );
            tileEntity.toCompound( compound, SerializationReason.PERSIST );

            try {
                nbtWriter.write( compound );
            } catch ( IOException e ) {
                LOGGER.warn( "Could not write tile to leveldb", e );
            }
        }

        if ( out.readableBytes() > 0 ) {
            key = ( (LevelDBWorldAdapter) this.world ).getKey( this.x, this.z, (byte) 0x31 );
            writeBatch.put( key, out.array() );
        }

        db.write( writeBatch );
        out.release();

        try {
            writeBatch.close();
        } catch (IOException e) {
            LOGGER.warn("Could not close write batch", e);
        }
    }

    private void saveChunkSlice( int i, WriteBatch writeBatch ) {
        ChunkSlice slice = this.chunkSlices[i];
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.heapBuffer();

        buffer.writeByte( (byte) 8 );
        buffer.writeByte( (byte) slice.getAmountOfLayers() );

        for ( int layer = 0; layer < slice.getAmountOfLayers(); layer++ ) {
            List<BlockIdentifier> blocks = slice.getBlocks( layer );

            // Count how many unique blocks we have in this chunk
            int[] indexIDs = new int[4096];

            IntList indexList = new IntArrayList();
            IntList runtimeIndex = new IntArrayList();
            Int2ObjectMap<BlockIdentifier> block = new Int2ObjectOpenHashMap<>();

            int foundIndex = 0;

            int lastBlockId = -1;
            int runtimeIdCounter = 0;

            for ( short blockIndex = 0; blockIndex < indexIDs.length; blockIndex++ ) {
                int blockId = blocks.get( blockIndex ).getRuntimeId();

                if ( lastBlockId != blockId ) {
                    foundIndex = indexList.indexOf( blockId );
                    if ( foundIndex == -1 ) {
                        int runtimeId = runtimeIdCounter++;
                        block.put( runtimeId, blocks.get( blockIndex ) );
                        runtimeIndex.add( runtimeId );
                        indexList.add( blockId );
                        foundIndex = indexList.size() - 1;
                    }

                    lastBlockId = blockId;
                }

                indexIDs[blockIndex] = foundIndex;
            }

            // Get correct wordsize
            int value = indexList.size();
            int numberOfBits = MathUtils.fastFloor( MathUtils.log2( value ) ) + 1;

            // Prepare palette
            int amountOfBlocks = MathUtils.fastFloor( 32f / (float) numberOfBits );

            Palette palette = new Palette( buffer, amountOfBlocks, false );

            byte paletteWord = (byte) ( (byte) ( palette.getPaletteVersion().getVersionId() << 1 ) | 1 );
            buffer.writeByte( paletteWord );
            palette.addIndexIDs( indexIDs );
            palette.finish();

            // Write persistent ids
            buffer.writeIntLE( indexList.size() );
            for ( int value1 : runtimeIndex.toArray( new int[0] ) ) {
                BlockIdentifier blockIdentifier = block.get( value1 );

                NBTTagCompound compound = new NBTTagCompound( "" );
                compound.addValue( "name", blockIdentifier.getBlockId() );
                compound.addValue("states", blockIdentifier.getNbt());
                compound.addValue("version", BLOCK_VERSION);

                try {
                    compound.writeTo( buffer, ByteOrder.LITTLE_ENDIAN );
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }

        byte[] key = ( (LevelDBWorldAdapter) this.world ).getKeySubChunk( this.x, this.z, (byte) 0x2f, (byte) i );
        buffer.readerIndex(0);
        writeBatch.put( key, buffer.array() );
        buffer.release();
    }

    void loadSection( int sectionY, byte[] chunkData ) {
        ByteBuf buffer = Allocator.allocate(chunkData);

        // First byte is chunk section version
        byte subchunkVersion = buffer.readByte();
        int storages = 1;
        switch ( subchunkVersion ) {
            case 8:
                storages = buffer.readByte();
            case 1:
                for ( int sI = 0; sI < storages; sI++ ) {
                    byte data = buffer.readByte();
                    boolean isPersistent = ( ( data >> 8 ) & 1 ) != 1; // last bit is the isPresent state (shift and mask it to 1)
                    byte wordTemplate = (byte) ( data >>> 1 ); // Get rid of the last bit (which seems to be the isPresent state)

                    Palette palette = new Palette( buffer, wordTemplate, true );
                    short[] indexes = palette.getIndexes();

                    // Read NBT data
                    int needed = buffer.readIntLE();
                    Int2IntMap chunkPalette = new Int2IntOpenHashMap( needed ); // Varint my ass

                    int index = 0;
                    NBTReader reader = new NBTReader( buffer, ByteOrder.LITTLE_ENDIAN );
                    while ( index < needed ) {
                        try {
                            NBTTagCompound compound = reader.parse();
                            String blockId = compound.getString( "name", "minecraft:air" );
                            NBTTagCompound states = compound.getCompound("states", false);
                            if (states != null) {
                                FreezableSortedMap<String, Object> mStates = new FreezableSortedMap<>();
                                for (Map.Entry<String, Object> entry : states.entrySet()) {
                                    mStates.put(entry.getKey(), entry.getValue());
                                }

                                mStates.setFrozen(true);

                                BlockIdentifier identifier = BlockRuntimeIDs.toBlockIdentifier( blockId, mStates );
                                if (identifier == null) {
                                    LOGGER.error("Unknown block / state config: {} / {}", blockId, states);
                                }

                                chunkPalette.put( index++, identifier.getRuntimeId() );
                            } else {
                                chunkPalette.put( index++, BlockRuntimeIDs.toBlockIdentifier( blockId, null ).getRuntimeId() );
                            }
                        } catch ( IOException | AllocationLimitReachedException e ) {
                            LOGGER.error( "Error in loading tile entities", e );
                            break;
                        }
                    }

                    ChunkSlice slice = this.ensureSlice( sectionY );
                    for ( short i = 0; i < indexes.length; i++ ) {
                        int runtimeID = chunkPalette.get( indexes[i] );
                        slice.setRuntimeIdInternal( i, sI, runtimeID );
                    }
                }

                break;
        }

        buffer.release();
    }

    void loadTileEntities( byte[] tileEntityData ) {
        ByteBuf data = Allocator.allocate(tileEntityData);
        NBTReader nbtReader = new NBTReader( data, ByteOrder.LITTLE_ENDIAN );
        while ( data.readableBytes() > 0 ) {
            TileEntity tileEntity = null;

            try {
                NBTTagCompound compound = nbtReader.parse();

                Block block = this.getBlockAt( compound.getInteger( "x", 0 ) & 0xF, compound.getInteger( "y", 0 ), compound.getInteger( "z", 0 ) & 0xF );

                tileEntity = this.world.getServer().getTileEntities().construct( compound, block );
                if ( tileEntity != null ) {
                    this.addTileEntity( tileEntity );
                }
            } catch ( Exception e ) {
                LOGGER.error( "Error in loading tile entities", e );
                break;
            }
        }

        data.release();
    }

    void loadEntities( byte[] entityData ) {
        ByteBuf data = Allocator.allocate(entityData);
        NBTReader nbtReader = new NBTReader( data, ByteOrder.LITTLE_ENDIAN );
        while ( data.readableBytes() > 0 ) {
            try {
                NBTTagCompound compound = nbtReader.parse();
                String identifier = compound.getString( "identifier", null );

                Entity entity = this.world.getServer().getEntities().create( identifier );
                if ( entity != null ) {
                    entity.initFromNBT( compound );
                    entity.setWorld( this.world );
                    this.addEntity( entity );
                }
            } catch ( IOException | AllocationLimitReachedException e ) {
                LOGGER.error( "Error in loading entities", e );
                break;
            }
        }

        data.release();
    }

}
