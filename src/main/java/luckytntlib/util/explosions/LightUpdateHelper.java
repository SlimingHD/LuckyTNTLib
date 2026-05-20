package luckytntlib.util.explosions;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import luckytntlib.config.LuckyTNTLibConfigValues;
import luckytntlib.network.ClientboundUpdateChunkSectionPacket;
import luckytntlib.network.ClientboundUpdateSkyLightSourcesPacket;
import luckytntlib.network.PacketHandler;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraftforge.network.PacketDistributor;

/**
 * The {@code LightUpdateHelper} is used to calculate the light changes caused by explosions that omit block updates when changing the world. <br>
 * It is primarily used in {@link ExplosionHelper} and {@link ImprovedExplosion}.
 * <p>
 * It is used in favor of Minecraft's light engine, as it provides an immense performance increase.
 * Due to this performance focus, some edge cases may not mimic Minecraft's behavior,
 * but the results are visually correct in almost all situations.
 */
public class LightUpdateHelper {
	
	/**
	 * Cached {@link Field} for less reflection
	 */
	public static final Field HEIGHTMAP_FIELD = Util.make(() -> {
		for (Field f : ChunkSkyLightSources.class.getDeclaredFields()) {
			f.setAccessible(true);
			if (f.getType() == BitStorage.class) {
				return f;
			}
		}
		return null;
	});
	
	/**
	 * A byte array of the length 2048 filled with the value 255. Used to replace the sky light data of empty sections that can see the sky.
	 */
	private static final byte[] skyData = Util.make(new byte[2048], a -> Arrays.fill(a, (byte)255));
	
	
	private LightUpdateHelper() {
	}
	
	/**
	 * Calculates the updates to direct sky light after explosions. <br>
	 * {@link #updateIndirectSkyLight(ServerLevel, HashMap)} should always be called afterwards!
	 * @param server  the current {@link ServerLevel}
	 * @param chunks  a {@link HashMap} containing {@link BitSet}s linked to a {@link LevelChunk} with bits set to true according to the {@link LevelChunkSection}s in the chunk that have been edited
	 * @param dataLayerCache  a {@link Long2ObjectMap} used to cache light data
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	public static void updateDirectSkyLight(ServerLevel server, HashMap<LevelChunk, BitSet> chunks, Long2ObjectMap<LightDataHolder> dataLayerCache) {
		ThreadedLevelLightEngine engine = server.getChunkSource().getLightEngine();
		BitSet emptyBitSet = new BitSet(0);
		int minY = server.getMinBuildHeight() - 1;
		
		for (Entry<LevelChunk, BitSet> entry : chunks.entrySet()) {
			LevelChunk chunk = entry.getKey();
			BitSet editedSections = entry.getValue();
			ChunkPos pos = chunk.getPos();
			ClientboundLightUpdatePacket packet = new ClientboundLightUpdatePacket(pos, server.getLightEngine(), emptyBitSet, emptyBitSet);
			ClientboundLightUpdatePacketData lightData = packet.getLightData();
			List<byte[]> skyUpdates = lightData.getSkyUpdates();
			BitStorage heightmap = null;
			
			chunk.setLightCorrect(false);
			
			try {
				heightmap = (BitStorage)HEIGHTMAP_FIELD.get(server.getChunk(pos.x, pos.z).getSkyLightSources());
			} catch(IllegalAccessException | NullPointerException e) {
				e.printStackTrace();
			}

			int lowestEmptySection = 0;
			int lowestEmptySectionY = 0;

			for (int i = server.getMaxSection(); i >= server.getMinSection() - 1; --i) {
				if (i == server.getMaxSection() || i == server.getMinSection() - 1 || chunk.getSection(chunk.getSectionIndexFromSectionY(i)).hasOnlyAir()) {
					lowestEmptySection = i - (server.getMinSection() - 1);
					lowestEmptySectionY = i;
					long section = SectionPos.asLong(pos.x, i, pos.z);
					
					if (editedSections.get(lowestEmptySection)) {
						lightData.getSkyYMask().set(i - (server.getMinSection() - 1));
						lightData.getEmptyBlockYMask().set(i - (server.getMinSection() - 1));
						editedSections.set(lowestEmptySection, false);
						
						skyUpdates.add(skyData);
						
						engine.queueSectionData(LightLayer.SKY, SectionPos.of(chunk.getPos(), i), new DataLayer(15));
						engine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunk.getPos(), i), new DataLayer());
						engine.updateSectionStatus(SectionPos.of(chunk.getPos(), i), true);
					}
					cacheDataLayer(section, new DataLayer(15), LightLayer.SKY, dataLayerCache);
					cacheDataLayer(section, new DataLayer(), LightLayer.BLOCK, dataLayerCache);
				} else {
					for (int j = i; j >= server.getMinSection() - 1; --j) {
						lightData.getEmptySkyYMask().set(j - (server.getMinSection() - 1));
						engine.queueSectionData(LightLayer.SKY, SectionPos.of(chunk.getPos(), j), new DataLayer());
						engine.updateSectionStatus(SectionPos.of(chunk.getPos(), j), j == server.getMinSection() - 1 ? true : chunk.getSection(chunk.getSectionIndexFromSectionY(j)).hasOnlyAir());
						cacheDataLayer(SectionPos.asLong(pos.x, j, pos.z), new DataLayer(), LightLayer.SKY, dataLayerCache);
					}
					break;
				}
			}

			if (heightmap != null) {
				int lowestYoffset = Math.max((lowestEmptySectionY << 4) - minY, 0);
				
				for (int i = 0; i < 256; ++i) {
					heightmap.set(i, lowestYoffset);
				}
			}
			
			boolean continueBelow = true;
			for (int i = lowestEmptySection - 1; i >= 0; --i) {
				if (continueBelow) {
					continueBelow = false;
				} else {
					break;
				}
				
				boolean sectionChanged = false;
				int sectionY = i + (server.getMinSection() - 1);
				
				DataLayer data = dataLayerCache.get(SectionPos.asLong(pos.x, sectionY, pos.z)).get(LightLayer.SKY);
				PalettedContainer<BlockState> states = sectionY < server.getMaxSection() && sectionY >= server.getMinSection() ? chunk.getSection(server.getSectionIndexFromSectionY(sectionY)).getStates() : null;
				if (data != null && states != null && heightmap != null) {
					for (int x = 0; x < 16; ++x) {
						for (int z = 0; z < 16; ++z) {
							for (int y = 15; y >= 0; --y) {
								int lightY = heightmap.get(x + (z << 4)) + minY;
								int yCoord = (sectionY << 4) + y;
								if (lightY == yCoord + 1 && states.get(x, y, z).getLightBlock(server, BlockPos.ZERO) == 0) {
									data.set(x, y, z, 15);
									heightmap.set(x + (z << 4), yCoord - minY);
									
									sectionChanged = true;
									if (y == 0) {
										continueBelow = true;
									}
								} else {
									break;
								}
							}
						}
					}
					if (sectionChanged) {
						skyUpdates.add(data.getData());
						lightData.getSkyYMask().set(i);
						
						engine.queueSectionData(LightLayer.SKY, SectionPos.of(pos, sectionY), data);
						engine.updateSectionStatus(SectionPos.of(pos, sectionY), chunk.getSection(server.getSectionIndexFromSectionY(sectionY)).hasOnlyAir());
					}
				}
			}

			engine.setLightEnabled(pos, true);
			engine.retainData(pos, true);
			engine.tryScheduleUpdate();
			
			int[] dataToSend = new int[256];
			for (int i = 0; i < 256; ++i) {
				dataToSend[i] = heightmap.get(i);
			}
			PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientboundUpdateSkyLightSourcesPacket(pos, dataToSend));
			
			Collections.reverse(skyUpdates);
			for (ServerPlayer player : server.players()) {
				player.connection.send(packet);
			}
		}
	}
	
	/**
	 * Calculates the light updates for areas indirectly lit by the sky. <br> 
	 * Should always be called after {@link #updateDirectSkyLight(ServerLevel, HashMap)}! <br>
	 * Depending on user settings this will also update the block light in affected sections. 
	 * Based on the shape of the preceding explosion this may severely increase the amount of iterations and light updates necessary and therefore the time to calculate and process.
	 * @param server  the current {@link ServerLevel}
	 * @param chunks  a {@link HashMap} containing {@link BitSet}s linked to a {@link LevelChunk} with bits set to true according to the {@link LevelChunkSection}s in the chunk that have been edited
	 * @param dataLayerCache  a {@link Long2ObjectMap} used to cache light data
	 * 
	 * @see #updateDirectSkyLight(ServerLevel, HashMap)
	 */
	public static void updateIndirectSkyLight(ServerLevel server, HashMap<LevelChunk, BitSet> chunks, Long2ObjectMap<LightDataHolder> dataLayerCache) {
		ThreadedLevelLightEngine engine = server.getChunkSource().getLightEngine();
		Long2ObjectMap<BitSet> packetData = new Long2ObjectLinkedOpenHashMap<>();
		boolean updateBlockLight = LuckyTNTLibConfigValues.UPDATE_BLOCK_LIGHT.get();
		
		updateIndirectSkylightChunkBoarders(server, chunks, packetData, dataLayerCache);
		
		for (Entry<LevelChunk, BitSet> entry : chunks.entrySet()) {
			LevelChunk chunk = entry.getKey();
			BitSet editedSections = entry.getValue();
			ChunkPos pos = chunk.getPos();
			
			int highestEditedSection = 0;
			int lowestEditedSection = editedSections.nextSetBit(0);
			for (int i = engine.getLightSectionCount() - 1; i >= 0; --i) {
				if (editedSections.get(i)) {
					highestEditedSection = i;
					break;
				}
			}
			
			int lowestEmptySectionBottom = 0;
			for (int i = server.getMaxSection() - 1; i >= server.getMinSection(); --i) {
				if (!chunk.getSection(chunk.getSectionIndexFromSectionY(i)).hasOnlyAir()) {
					lowestEmptySectionBottom = (i + 1) << 4;
					break;
				}
			}

			int lowestEditedBlockY = updateBlockLight ? Math.max((lowestEditedSection + (server.getMinSection() - 1)) << 4, server.getMinBuildHeight()) : Integer.MAX_VALUE;
			for (int x = 0; x < 16; ++x) {
				for (int z = 0; z < 16; ++z) {
					int lowestLightY = Math.max(chunk.getSkyLightSources().getLowestSourceY(x, z), server.getMinBuildHeight());
					int startY = ((highestEditedSection + (server.getMinSection() - 1)) << 4) + 15;
					if (startY < lowestLightY) {
						startY = lowestEmptySectionBottom;
					}
					for (int y = startY; y >= Math.min(lowestLightY, lowestEditedBlockY); --y) {
						if (y >= lowestLightY) {
							if (getLightBlockAtPos(server, pos, x - 1, y, z) < 15 && getLightAtPos(server, pos, x - 1, y, z, LightLayer.SKY, dataLayerCache) < 15) {
								queuePosForLightUpdate(packetData, pos, x - 1, y, z);
							}
							if (getLightBlockAtPos(server, pos, x + 1, y, z) < 15 && getLightAtPos(server, pos, x + 1, y, z, LightLayer.SKY, dataLayerCache) < 15) {
								queuePosForLightUpdate(packetData, pos, x + 1, y, z);
							}
							if (getLightBlockAtPos(server, pos, x, y, z - 1) < 15 && getLightAtPos(server, pos, x, y, z - 1, LightLayer.SKY, dataLayerCache) < 15) {
								queuePosForLightUpdate(packetData, pos, x, y, z - 1);
							}
							if (getLightBlockAtPos(server, pos, x, y, z + 1) < 15 && getLightAtPos(server, pos, x, y, z + 1, LightLayer.SKY, dataLayerCache) < 15) {
								queuePosForLightUpdate(packetData, pos, x, y, z + 1);
							}
							
							if (y == lowestLightY && getLightBlockAtPos(server, pos, x, y - 1, z) < 15 && getLightAtPos(server, pos, x, y - 1, z, LightLayer.SKY, dataLayerCache) < 15) {
								queuePosForLightUpdate(packetData, pos, x, y - 1, z);
							}
						}
						if (updateBlockLight && y >= lowestEditedBlockY && getLightAtPos(server, pos, x, y, z, LightLayer.BLOCK, dataLayerCache) > 0) {
							queuePosForLightUpdate(packetData, pos, x, y, z);
						}
					}
				}
			}
		}
		
		for (Entry<Long, BitSet> entry : packetData.long2ObjectEntrySet()) {
			SectionPos pos = SectionPos.of(entry.getKey());
			BitSet blocksToCheck = entry.getValue();
			
			int chunkX = pos.x() << 4;
			int sectionY = pos.y() << 4;
			int chunkZ = pos.z() << 4;
			for (int x = 0; x < 16; ++x) {
				for (int z = 0; z < 16; ++z) {
					for (int y = 15; y >= 0; --y) {
						if (!blocksToCheck.get(ExplosionHelper.encodeSectionPos(x, y, z))) {
							continue;
						}
						engine.checkBlock(new BlockPos(chunkX + x, sectionY + y, chunkZ + z));
					}
				}
			}
			engine.retainData(new ChunkPos(pos.x(), pos.z()), true);
			engine.updateSectionStatus(pos, false);
			engine.setLightEnabled(new ChunkPos(pos.getX(), pos.getZ()), true);

			PacketHandler.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> server.dimension()), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.getX(), pos.getY() - server.getMinSection(), pos.getZ()), blocksToCheck, false, true));
		}
		engine.tryScheduleUpdate();
	}
	
	/**
	 * Updates all chunks that weren't edited themselves but boarder a chunk that has been edited
	 * @param server  the current {@link ServerLevel}
	 * @param chunks  a {@link HashMap} containing {@link BitSet}s linked to a {@link LevelChunk} with bits set to true according to the {@link LevelChunkSection}s in the chunk that have been edited
	 * @param packetData  a {@link Long2ObjectMap} holding all the information on what blocks should be updated
	 * @param dataLayerCache  a {@link Long2ObjectMap} used to cache light data
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap, Long2ObjectMap)
	 */
	private static void updateIndirectSkylightChunkBoarders(ServerLevel server, HashMap<LevelChunk, BitSet> chunks, Long2ObjectMap<BitSet> packetData, Long2ObjectMap<LightDataHolder> dataLayerCache) {
		HashSet<Long> chunkSet = new HashSet<>();
		for (Entry<LevelChunk, BitSet> entry : chunks.entrySet()) {
			chunkSet.add(entry.getKey().getPos().toLong());
		}
		
		HashSet<Long> boarderChunks = new HashSet<>();
		for (long chunk : chunkSet) {
			ChunkPos pos = new ChunkPos(chunk);
			for (int offX = -1; offX <= 1; offX++) {
				for (int offZ = -1; offZ <= 1; offZ++) {
					if (offX == 0 && offZ == 0) {
						continue;
					}
					int x = pos.x + offX;
					int z = pos.z + offZ;
					long offsetChunk = ChunkPos.asLong(x, z);
					if (!chunkSet.contains(offsetChunk) && !boarderChunks.contains(offsetChunk)) {
						processBoarderChunk(server, server.getChunk(x, z), packetData, dataLayerCache);
						boarderChunks.add(offsetChunk);
					}
				}
			}
		}
	}
	
	/**
	 * Calculates indirect light updates for a provided chunk that wasn't edited by an explosion itself but boarders a chunk that was
	 * @param server  the current {@link ServerLevel}
	 * @param chunk  the {@link LevelChunk} to calculate the light updates for
	 * @param packetData  a {@link Long2ObjectMap} holding all the information on what blocks should be updated
	 * @param dataLayerCache  a {@link Long2ObjectMap} used to cache light data
	 * 
	 * @see #updateIndirectSkylightChunkBoarders(ServerLevel, HashMap, Long2ObjectMap)
	 */
	private static void processBoarderChunk(ServerLevel server, LevelChunk chunk, Long2ObjectMap<BitSet> packetData, Long2ObjectMap<LightDataHolder> dataLayerCache) {
		ChunkPos pos = chunk.getPos();
		
		chunk.setLightCorrect(false);
		
		int lowestEmptySectionBottom = 0;
		for (int i = server.getMaxSection() - 1; i >= server.getMinSection(); --i) {
			if (!chunk.getSection(chunk.getSectionIndexFromSectionY(i)).hasOnlyAir()) {
				lowestEmptySectionBottom = (i + 1) << 4;
				break;
			}
		}
		
		for (int x = 0; x < 16; ++x) {
			for (int z = 0; z < 16; ++z) {
				int lowestLightY = Math.max(chunk.getSkyLightSources().getLowestSourceY(x, z), server.getMinBuildHeight());
				for (int y = lowestEmptySectionBottom; y >= lowestLightY; --y) {
					if (getLightBlockAtPos(server, pos, x - 1, y, z) < 15 && getLightAtPos(server, pos, x - 1, y, z, LightLayer.SKY, dataLayerCache) < 15) {
						queuePosForLightUpdate(packetData, pos, x - 1, y, z);
					}
					if (getLightBlockAtPos(server, pos, x + 1, y, z) < 15 && getLightAtPos(server, pos, x + 1, y, z, LightLayer.SKY, dataLayerCache) < 15) {
						queuePosForLightUpdate(packetData, pos, x + 1, y, z);
					}
					if (getLightBlockAtPos(server, pos, x, y, z - 1) < 15 && getLightAtPos(server, pos, x, y, z - 1, LightLayer.SKY, dataLayerCache) < 15) {
						queuePosForLightUpdate(packetData, pos, x, y, z - 1);
					}
					if (getLightBlockAtPos(server, pos, x, y, z + 1) < 15 && getLightAtPos(server, pos, x, y, z + 1, LightLayer.SKY, dataLayerCache) < 15) {
						queuePosForLightUpdate(packetData, pos, x, y, z + 1);
					}
					
					if (y == lowestLightY && getLightBlockAtPos(server, pos, x, y - 1, z) < 15 && getLightAtPos(server, pos, x, y - 1, z, LightLayer.SKY, dataLayerCache) < 15) {
						queuePosForLightUpdate(packetData, pos, x, y - 1, z);
					}
				}
			}
		}
	}
	
	/**
	 * Sets the bit representing the given position in the given {@link Long2ObjectMap} to {@code true} to indicate the light engine should update that position
	 * @param packetData  the {@link Long2ObjectMap} the queued light update will be written into
	 * @param pos  the {@link ChunkPos} of the chunk containing the block that will be updated
	 * @param x  the x offset of the block to the chunk
	 * @param y  the y coordinate of the block the update is meant to be scheduled for
	 * @param z  the z offset of the block to the chunk
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	private static void queuePosForLightUpdate(Long2ObjectMap<BitSet> packetData, ChunkPos pos, int x, int y, int z) {
		long c = SectionPos.of(shiftChunkPos(pos, x, z), y >> 4).asLong();
		int realX = shiftCoordinate(x);
		int realZ = shiftCoordinate(z);
		
		if (packetData.get(c) == null) {
			packetData.put(c, new BitSet(4096));
		}
		
		packetData.get(c).set(ExplosionHelper.encodeSectionPos(realX, y & 15, realZ));
	}
	
	/**
	 * Stores the supplied light data in the provided cache at the given position and for the given layer
	 * @param pos  a long encoded by {@link SectionPos#asLong()} that represents the position
	 * @param data  the {@link DataLayer} to store in the given {@link Long2ObjectMap}
	 * @param layer  the {@link LightLayer} to store the data for
	 * @param dataLayerCache  a {@link Long2ObjectMap} used to cache light data
	 * 
	 * @see #updateDirectSkyLight(ServerLevel, HashMap, Long2ObjectMap)
	 */
	private static void cacheDataLayer(long pos, DataLayer data, LightLayer layer, Long2ObjectMap<LightDataHolder> dataLayerCache) {
		if (dataLayerCache.get(pos) == null) {
			dataLayerCache.put(pos, new LightDataHolder());
		}
		dataLayerCache.get(pos).put(layer, data);
	}
	
	/**
	 * Gets the light block of a block at the given position i.e. how much the light level will be decreased trying to spread through the block.
	 * @param server  the current {@link ServerLevel}
	 * @param pos  the {@link ChunkPos} of the chunk the block is located in
	 * @param x  the x offset of the position to the chunk
	 * @param y  the y coordinate of the position
	 * @param z  the z offset of the position to the chunk
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	private static int getLightBlockAtPos(ServerLevel server, ChunkPos pos, int x, int y, int z) {
		ChunkPos chunk = shiftChunkPos(pos, x, z);
		int realX = shiftCoordinate(x);
		int realZ = shiftCoordinate(z);
		
		if (y < server.getMinBuildHeight() || y >= server.getMaxBuildHeight()) {
			return 0;
		}
		
		return server.getChunk(chunk.x, chunk.z).getSection(server.getSectionIndexFromSectionY(y >> 4)).getStates().get(realX, y & 15, realZ).getLightBlock(server, BlockPos.ZERO);
	}
	
	/**
	 * Gets the light level of the given position in the given layer.
	 * @param server  the current {@link ServerLevel}
	 * @param pos  the {@link ChunkPos} of the chunk the position is located in
	 * @param x  the x offset of the position to the chunk
	 * @param y  the y coordinate of the position
	 * @param z  the z offset of the position to the chunk
	 * @param layer  the {@link LightLayer} to retrieve the data from
	 * @param dataLayerCache  a {@link Long2ObjectMap} caching all used {@link DataLayer}s for quicker access
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	private static int getLightAtPos(ServerLevel server, ChunkPos pos, int x, int y, int z, LightLayer layer, Long2ObjectMap<LightDataHolder> dataLayerCache) {
		SectionPos section = SectionPos.of(shiftChunkPos(pos, x, z), y >> 4);
		int realX = shiftCoordinate(x);
		int realZ = shiftCoordinate(z);
		
		long s = section.asLong();
		if (dataLayerCache.get(s) == null || dataLayerCache.get(s).isEmpty(layer)) {
			cacheDataLayer(s, server.getLightEngine().getLayerListener(layer).getDataLayerData(section), layer, dataLayerCache);
		}
		
		DataLayer data = dataLayerCache.get(s).get(layer);
		if (data == null) {
			return 0;
		}
		return data.get(realX, y & 15, realZ);
	}
	
	/**
	 * Changes the x and z coordinate of the {@link ChunkPos} according to the given offset.
	 * @param pos  the {@link ChunkPos} that is to be offset
	 * @param x  the x offset in blocks
	 * @param z  the z offset in blocks
	 * @return  the offset {@link ChunkPos}
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	private static ChunkPos shiftChunkPos(ChunkPos pos, int x, int z) {
		ChunkPos chunk = pos;
		
		if (x < 0) {
			chunk = new ChunkPos(chunk.x - 1, chunk.z);
		} else if (x > 15) {
			chunk = new ChunkPos(chunk.x + 1, chunk.z);
		}
		if (z < 0) {
			chunk = new ChunkPos(chunk.x, chunk.z - 1);
		} else if (z > 15) {
			chunk = new ChunkPos(chunk.x, chunk.z + 1);
		}
		return chunk;
	}
	
	/**
	 * Offsets a given <code>int</code> to stay between 0 and 15. <br>
	 * Values counting lower than 0 will start back at 15. Values counting higher than 15 will start back at 0.
	 * @param toShift  <code>int</code> to be offset
	 * @return the offset value
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	private static int shiftCoordinate(int toShift) {
		return toShift <= 15 && toShift >= 0 ? toShift : (toShift < 0 ? toShift + 16 : toShift - 16);
	}
	
	/**
	 * Holder class for light data associated to a {@link LevelChunkSection} and a {@link LightLayer} that is stored in a {@link DataLayer}
	 */
	public static final class LightDataHolder {
		
		private DataLayer skyLayer = null;
		private DataLayer blockLayer = null;
		
		private LightDataHolder() {
		}
		
		private void put(LightLayer layer, DataLayer data) {
			if (layer == LightLayer.SKY) {
				skyLayer = data;
			} else {
				blockLayer = data;
			}
		}
		
		private boolean isEmpty(LightLayer layer) {
			if (layer == LightLayer.SKY) {
				return skyLayer == null;
			}
			return blockLayer == null;
		}
		
		@Nullable
		private DataLayer get(LightLayer layer) {
			if (layer == LightLayer.SKY) {
				return skyLayer;
			}
			return blockLayer;
		}
	}
}