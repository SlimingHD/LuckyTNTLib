package luckytntlib.util.light;

import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luckytntlib.config.LuckyTNTLibConfigValues;
import luckytntlib.network.ClientboundUpdateChunkSectionPacket;
import luckytntlib.network.ClientboundUpdateSkyLightSourcesPacket;
import luckytntlib.network.PacketHandler;
import luckytntlib.util.explosions.ExplosionHelper;
import luckytntlib.util.explosions.ImprovedExplosion;
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
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.network.PacketDistributor;

/**
 * The {@code LightUpdateHelper} is used to calculate the light changes caused by explosions. <br>
 * It is primarily used in {@link ExplosionHelper} and {@link ImprovedExplosion}. <br>
 * The results aren't perfect by a long shot and there are some edge cases we know about that are problematic (there are probably even more we don't know about),
 * but the results are good enough, especially when you consider the performance increase.
 */
public class LightUpdateHelper {
	
	/**
	 * A byte array of the length 2048 filled with the value 255. Used to replace the sky light data of empty sections that can see the sky.
	 */
	private static final byte[] skyData = new DataLayer(15).getData();
	/**
	 * Cached {@link Field} for less reflection
	 */
	private static Field heightmapField;
	
	/**
	 * Calculates the updates to direct sky light after explosions. <br>
	 * {@link #updateIndirectSkyLight(ServerLevel, HashMap)} should always be called afterwards!
	 * @param server  the current {@link ServerLevel}
	 * @param chunks  a {@link HashMap} containing {@link BitSet}s linked to a {@link LevelChunk} with bits set to true according to the {@link LevelChunkSection}s in the chunk that have been edited
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	public static void updateDirectSkyLight(ServerLevel server, HashMap<LevelChunk, BitSet> chunks) {
		ThreadedLevelLightEngine engine = server.getChunkSource().getLightEngine();
		BitSet emptyBitSet = new BitSet(0);
		int minY = server.getMinBuildHeight() - 1;
		
		if (heightmapField == null) {
			for (Field f : ChunkSkyLightSources.class.getDeclaredFields()) {
				f.setAccessible(true);
				if (f.getType() == BitStorage.class) {
					heightmapField = f;
					break;
				}
			}
		}
		
		for (Entry<LevelChunk, BitSet> entry : chunks.entrySet()) {
			LevelChunk chunk = entry.getKey();
			BitSet editedSections = entry.getValue();
			ChunkPos pos = chunk.getPos();
			ClientboundLightUpdatePacket packet = new ClientboundLightUpdatePacket(pos, server.getLightEngine(), emptyBitSet, emptyBitSet);
			ClientboundLightUpdatePacketData lightData = packet.getLightData();
			List<byte[]> skyUpdates = lightData.getSkyUpdates();
			BitStorage heightmap = null;
			
			try {
				heightmap = (BitStorage)heightmapField.get(server.getChunk(pos.x, pos.z).getSkyLightSources());
			} catch(IllegalAccessException e) {
				e.printStackTrace();
			}

			int lowestEmptySection = 0;
			int lowestEmptySectionY = 0;

			for (int i = server.getMaxSection(); i >= server.getMinSection() - 1; --i) {
				if (i == server.getMaxSection() || i == server.getMinSection() - 1 || chunk.getSection(chunk.getSectionIndexFromSectionY(i)).hasOnlyAir()) {
					lowestEmptySection = i - (server.getMinSection() - 1);
					lowestEmptySectionY = i;
					if (editedSections.get(lowestEmptySection)) {
						lightData.getSkyYMask().set(i - (server.getMinSection() - 1));
						lightData.getEmptyBlockYMask().set(i - (server.getMinSection() - 1));
						editedSections.set(lowestEmptySection, false);
						
						skyUpdates.add(skyData);
						
						byte[] sky = new byte[2048];
						System.arraycopy(skyData, 0, sky, 0, skyData.length);
						engine.queueSectionData(LightLayer.SKY, SectionPos.of(chunk.getPos(), i), new DataLayer(sky));
						engine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunk.getPos(), i), new DataLayer(new byte[2048]));
						engine.updateSectionStatus(SectionPos.of(chunk.getPos(), i), true);
					}
				} else {
					break;
				}
			}

			if (heightmap != null) {
				int lowestYoffset = (lowestEmptySectionY << 4) - minY;
				
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
				
				DataLayer data = engine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(pos, sectionY));
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
						engine.updateSectionStatus(SectionPos.of(pos, sectionY), false);
					}
				}
			}

			engine.setLightEnabled(pos, true);
			
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
		engine.tryScheduleUpdate();
	}
	
	/**
	 * Calculates the light updates for areas indirectly lit by the sky. <br> 
	 * Should always be called after {@link #updateDirectSkyLight(ServerLevel, HashMap)}! <br>
	 * Depending on user settings this will also update the block light in affected sections. 
	 * Based on the shape of the preceding explosion this may severely increase the amount of iterations and light updates necessary and therefore the time to calculate and process.
	 * @param server  the current {@link ServerLevel}
	 * @param chunks  a {@link HashMap} containing {@link BitSet}s linked to a {@link LevelChunk} with bits set to true according to the {@link LevelChunkSection}s in the chunk that have been edited
	 * 
	 * @see #updateDirectSkyLight(ServerLevel, HashMap)
	 */
	public static void updateIndirectSkyLight(ServerLevel server, HashMap<LevelChunk, BitSet> chunks) {
		LevelLightEngine engine = server.getLightEngine();
		Long2ObjectMap<BitSet> packetData = new Long2ObjectLinkedOpenHashMap<>();
		Long2ObjectMap<LightDataHolder> dataLayerCache = new Long2ObjectOpenHashMap<>();
		boolean updateBlockLight = LuckyTNTLibConfigValues.UPDATE_BLOCK_LIGHT.get();
		
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

			int lowestEditedBlockY = updateBlockLight ? Math.max((lowestEditedSection + (server.getMinSection() - 1)) << 4, server.getMinBuildHeight()) : Integer.MAX_VALUE;
			for (int x = 0; x < 16; ++x) {
				for (int z = 0; z < 16; ++z) {
					int lowestLightY = Math.max(chunk.getSkyLightSources().getLowestSourceY(x, z), server.getMinBuildHeight());
					for (int y = ((highestEditedSection + (server.getMinSection() - 1)) << 4) + 15; y >= Math.min(lowestLightY, lowestEditedBlockY); --y) {
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
			
			PacketHandler.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> server.dimension()), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos.getX(), pos.getY() - server.getMinSection(), pos.getZ()), blocksToCheck, false, true));
			
			for (int x = 0; x < 16; ++x) {
				for (int z = 0; z < 16; ++z) {
					for (int y = 15; y >= 0; --y) {
						if (!blocksToCheck.get(ExplosionHelper.encodeSectionPos(x, y, z))) {
							continue;
						}
						engine.checkBlock(new BlockPos((pos.getX() << 4) + x, (pos.getY() << 4) + y, (pos.getZ() << 4) + z));
					}
				}
			}
			engine.updateSectionStatus(pos, false);
			engine.setLightEnabled(new ChunkPos(pos.getX(), pos.getZ()), true);
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
		
		packetData.get(c).set(ExplosionHelper.encodeSectionPos(realX, (y & 15), realZ));
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
		
		if (y < server.getMinBuildHeight()) {
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
		if (dataLayerCache.get(s) == null) {
			dataLayerCache.put(s, new LightDataHolder());
		}
		if (dataLayerCache.get(s).isEmpty(layer)) {
			dataLayerCache.get(s).put(layer, server.getLightEngine().getLayerListener(layer).getDataLayerData(section));
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
	private static final class LightDataHolder {
		
		private DataLayer skyLayer = null;
		private DataLayer blockLayer = null;
		
		public void put(LightLayer layer, DataLayer data) {
			if (layer == LightLayer.SKY) {
				skyLayer = data;
			} else {
				blockLayer = data;
			}
		}
		
		public boolean isEmpty(LightLayer layer) {
			if (layer == LightLayer.SKY) {
				return skyLayer == null;
			}
			return blockLayer == null;
		}
		
		@Nullable
		public DataLayer get(LightLayer layer) {
			if (layer == LightLayer.SKY) {
				return skyLayer;
			}
			return blockLayer;
		}
	}
}