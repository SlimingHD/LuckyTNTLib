package luckytntlib.util.light;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import luckytntlib.network.ClientboundUpdateChunkSectionPacket;
import luckytntlib.network.ClientboundUpdateSkyLightSourcesPacket;
import luckytntlib.network.PacketHandler;
import luckytntlib.util.explosions.ExplosionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
 * The {@code LightUpdateHelper} is used to calculate the light changes caused by removing massive amounts of blocks. <br>
 * It is commonly used by Explosions that bypass {@link Level#setBlock(BlockPos, BlockState, int, int)} in the {@link ExplosionHelper}.
 */
public class LightUpdateHelper {
	
	/**
	 * A byte array of the length 2048 filled with the value 255. Used to replace the sky light data of empty sections that can see the sky.
	 */
	private static final byte[] skyData = new DataLayer(15).getData();
	
	/**
	 * A byte array of the length 2048 filled with the value 0. Used to replace the block light data of empty sections that can see the sky.
	 */
	private static final byte[] blockData = new byte[2048];

	
	/**
	 * Calculates the updates to the direct light after explosions. <br>
	 * Should always be called before {@link #updateIndirectSkyLight(ServerLevel, HashMap)}!
	 * @param server  the current {@link ServerLevel}
	 * @param chunks  a {@link HashMap} containing {@link BitSet}s linked to a {@link LevelChunk} with bits set to true according to the {@link LevelChunkSection}s in the chunk that have been edited
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	public static void updateDirectSkyLight(ServerLevel server, HashMap<LevelChunk, BitSet> chunks) {
		ThreadedLevelLightEngine engine = server.getChunkSource().getLightEngine();
		
		for(Entry<LevelChunk, BitSet> entry : chunks.entrySet()) {
			LevelChunk chunk = entry.getKey();
			BitSet editedSections = entry.getValue();
			ChunkPos pos = chunk.getPos();
			ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk, server.getLightEngine(), null, null);
			ClientboundLightUpdatePacketData lightData = packet.getLightData();
			List<byte[]> skyUpdates = lightData.getSkyUpdates();
			List<byte[]> blockUpdates = lightData.getBlockUpdates();
			BitStorage heightmap = null;
			
			try {
				for(Field f : ChunkSkyLightSources.class.getDeclaredFields()) {
					f.setAccessible(true);
					if(f.get(chunk.getSkyLightSources()) instanceof BitStorage storage) {
						heightmap = storage;
						break;
					}
				}
			} catch(IllegalAccessException e) {
				e.printStackTrace();
			}
			
			blockUpdates.clear();
			skyUpdates.clear();
			for(int i = 0; i < engine.getLightSectionCount(); i++) {
				lightData.getEmptyBlockYMask().set(i, false);
				lightData.getBlockYMask().set(i, false);
				lightData.getEmptySkyYMask().set(i, false);
				lightData.getSkyYMask().set(i, false);
			}

			int lowestEmptySection = 0;
			int lowestEmptySectionY = 0;

			for(int i = server.getMaxSection(); i >= server.getMinSection() - 1; i--) {
				if(i == server.getMaxSection() || i == server.getMinSection() - 1 || chunk.getSection(chunk.getSectionIndexFromSectionY(i)).hasOnlyAir()) {
					lowestEmptySection = i - (server.getMinSection() - 1);
					lowestEmptySectionY = i;
					if(editedSections.get(lowestEmptySection)) {
						lightData.getSkyYMask().set(i - (server.getMinSection() - 1), true);
						lightData.getBlockYMask().set(i - (server.getMinSection() - 1), true);
						editedSections.set(lowestEmptySection, false);
						
						blockUpdates.add(blockData);
						skyUpdates.add(skyData);
	
						engine.queueSectionData(LightLayer.SKY, SectionPos.of(chunk.getPos(), i), new DataLayer(skyData));
						engine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunk.getPos(), i), new DataLayer(blockData));
						engine.updateSectionStatus(SectionPos.of(chunk.getPos(), i), true);
					}
				} else {
					for(int l = lowestEmptySection - 1; l >= 0; l--) {
						if(editedSections.get(l)) {
							lightData.getBlockYMask().set(l, true);
							
							blockUpdates.add(blockData);
							
							engine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunk.getPos(), l + (server.getMinSection() - 1)), new DataLayer(blockData));
							engine.updateSectionStatus(SectionPos.of(chunk.getPos(), l + (server.getMinSection() - 1)), true);
						}
					}
					break;
				}
			}

			int minY = server.getMinBuildHeight() - 1;
			if(heightmap != null) {
				int lowestYoffset = (lowestEmptySectionY << 4) - minY;
				
				for(int i = 0; i < 256; i++) {
					if(heightmap.get(i) > lowestYoffset) {
						heightmap.set(i, lowestYoffset);
					}
				}
			}
			
			boolean continueBelow = true;
			for(int i = lowestEmptySection - 1; i >= 0; i--) {
				if(continueBelow) {
					continueBelow = false;
				} else {
					break;
				}
				
				boolean sectionChanged = false;
				int sectionY = i + (server.getMinSection() - 1);
				
				DataLayer data = engine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(pos, sectionY));
				PalettedContainer<BlockState> states = sectionY < server.getMaxSection() && sectionY >= server.getMinSection() ? chunk.getSection(server.getSectionIndexFromSectionY(sectionY)).getStates() : null;
				if(data != null && states != null) {
					for(byte x = 0; x < 16; x++) {
						for(byte z = 0; z < 16; z++) {
							for(byte y = 15; y >= 0; y--) {
								int lightY = heightmap.get(x + (z << 4)) + minY;
								int yCoord = (sectionY << 4) + y;
								if(lightY < yCoord) {
									if(y == 0) {
										continueBelow = true;
									}
									continue;
								}
								if(states.get(x, y, z).getLightBlock(server, BlockPos.ZERO) == 0 && lightY == yCoord + 1) {
									data.set(x, y, z, 15);
									heightmap.set(x + (z << 4), yCoord - minY);
									
									sectionChanged = true;
									if(y == 0) {
										continueBelow = true;
									}
								}
							}
						}
					}
					if(sectionChanged) {
						skyUpdates.add(skyData);
						for(int s = skyUpdates.size() - 2; s >= 0; s--) {
							skyUpdates.set(s + 1, skyUpdates.get(s));
						}
						skyUpdates.set(0, data.getData());
						lightData.getSkyYMask().set(i);
						
						engine.queueSectionData(LightLayer.SKY, SectionPos.of(pos, sectionY), data);
						engine.updateSectionStatus(SectionPos.of(pos, sectionY), false);
					}
				}
			}

			engine.setLightEnabled(pos, true);
			
			int[] dataToSend = new int[256];
			for(short i = 0; i < 256; i++) {
				dataToSend[i] = heightmap.get(i);
			}
			PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateSkyLightSourcesPacket(pos, dataToSend));
			
			for(ServerPlayer player : server.players()) {
				player.connection.send(packet);
			}
		}
		engine.tryScheduleUpdate();
	}
	
	/**
	 * Calculates the updates to the indirect light after explosions. <br>
	 * Should always be called after {@link #updateDirectSkyLight(ServerLevel, HashMap)}!
	 * @param server  the current {@link ServerLevel}
	 * @param chunks  a {@link HashMap} containing {@link BitSet}s linked to a {@link LevelChunk} with bits set to true according to the {@link LevelChunkSection}s in the chunk that have been edited
	 * 
	 * @see #updateDirectSkyLight(ServerLevel, HashMap)
	 */
	public static void updateIndirectSkyLight(ServerLevel server, HashMap<LevelChunk, BitSet> chunks) {
		LevelLightEngine engine = server.getLightEngine();
		HashMap<ChunkPos, HashMap<Integer, List<Short>>> packetData = new HashMap<>();
		
		for(Entry<LevelChunk, BitSet> entry : chunks.entrySet()) {
			LevelChunk chunk = entry.getKey();
			BitSet editedSections = entry.getValue();
			ChunkPos pos = chunk.getPos();
			
			int lowestEmptySection = 0;
			for(int i = 0; i < engine.getLightSectionCount(); i++) {
				if(editedSections.get(i)) {
					lowestEmptySection = i;
				}
			}
			
			for(byte x = 0; x < 16; x++) {
				for(byte z = 0; z < 16; z++) {
					int lowestLightY = Math.max(chunk.getSkyLightSources().getLowestSourceY(x, z), server.getMinBuildHeight());
					for(int y = ((lowestEmptySection + (server.getMinSection() - 1)) << 4) + 15; y >= lowestLightY; y--) {
						if(getLightBlockAtPos(server, pos, x - 1, y, z) < 15 && getLightAtPos(server, pos, x - 1, y, z) < 15) {
							queuePosForLightUpdate(packetData, pos, x - 1, y, z);
						}
						if(getLightBlockAtPos(server, pos, x + 1, y, z) < 15 && getLightAtPos(server, pos, x + 1, y, z) < 15) {
							queuePosForLightUpdate(packetData, pos, x + 1, y, z);
						}
						if(getLightBlockAtPos(server, pos, x, y, z - 1) < 15 && getLightAtPos(server, pos, x, y, z - 1) < 15) {
							queuePosForLightUpdate(packetData, pos, x, y, z - 1);
						}
						if(getLightBlockAtPos(server, pos, x, y, z + 1) < 15 && getLightAtPos(server, pos, x, y, z + 1) < 15) {
							queuePosForLightUpdate(packetData, pos, x, y, z + 1);
						}
						
						if(y == lowestLightY && getLightBlockAtPos(server, pos, x, y - 1, z) < 15 && getLightAtPos(server, pos, x, y - 1, z) < 15) {
							queuePosForLightUpdate(packetData, pos, x, y - 1, z);
						}
					}
				}
			}
		}
		
		for(Entry<ChunkPos, HashMap<Integer, List<Short>>> entry : packetData.entrySet()) {
			ChunkPos pos = entry.getKey();
			for(Entry<Integer, List<Short>> e : entry.getValue().entrySet()) {
				PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundUpdateChunkSectionPacket(SectionPos.of(pos, e.getKey() - server.getMinSection()), e.getValue(), false, true));
				
				for(Short s : e.getValue()) {
					Vec3i vec = ExplosionHelper.decodeSectionPos(s);
					engine.checkBlock(new BlockPos((pos.x << 4) + vec.getX(), (e.getKey() << 4) + vec.getY(), (pos.z << 4) + vec.getZ()));
				}
				engine.updateSectionStatus(SectionPos.of(pos, e.getKey()), false);
				engine.setLightEnabled(pos, true);
			}
		}
	}
	
	/**
	 * 
	 * @param packetData  the {@link HashMap} that the queued light update will be written into
	 * @param pos  the pos of the chunk of the block to be updated
	 * @param x  the x offset of the block to the chunk
	 * @param y  the y coordinate of the block the update is meant to be scheduled for
	 * @param z  the z offset of the block to the chunk
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	private static void queuePosForLightUpdate(HashMap<ChunkPos, HashMap<Integer, List<Short>>> packetData, ChunkPos pos, int x, int y, int z) {
		ChunkPos chunk = shiftChunkPos(pos, x, z);
		int realX = shiftCoordinate(x);
		int realZ = shiftCoordinate(z);
		
		int sectionY = y >> 4;
		if(packetData.get(chunk) == null) {
			packetData.put(chunk, new HashMap<>());
		}
		if(packetData.get(chunk).get(sectionY) == null) {
			packetData.get(chunk).put(sectionY, new ArrayList<>());
		}
		
		packetData.get(chunk).get(sectionY).add(ExplosionHelper.encodeSectionPos((short)realX, (short)(y & 15), (short)realZ));
	}
	
	/**
	 * Gets the light block of a block at the given position.
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
		
		if(y < server.getMinBuildHeight()) {
			return 15;
		}
		
		return server.getChunk(chunk.x, chunk.z).getSection(server.getSectionIndexFromSectionY(y >> 4)).getStates().get(realX, y & 15, realZ).getLightBlock(server, BlockPos.ZERO);
	}
	
	/**
	 * Gets the sky light level of the given position.
	 * @param server  the current {@link ServerLevel}
	 * @param pos  the {@link ChunkPos} of the chunk the position is located in
	 * @param x  the x offset of the position to the chunk
	 * @param y  the y coordinate of the position
	 * @param z  the z offset of the position to the chunk
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	private static int getLightAtPos(ServerLevel server, ChunkPos pos, int x, int y, int z) {
		ChunkPos chunk = shiftChunkPos(pos, x, z);
		int realX = shiftCoordinate(x);
		int realZ = shiftCoordinate(z);
		
		int sectionY = y >> 4;
		DataLayer data = getDataLayerForSection(server, SectionPos.of(chunk, sectionY));
		if(data == null) {
			return 0;
		}
		return data.get(realX, y & 15, realZ);
	}
	
	/**
	 * Gets the {@link DataLayer} storing the sky light data associated to a {@link LevelChunkSection} at a given {@link SectionPos}.
	 * @param @param server  the current {@link ServerLevel}
	 * @param pos  the position of the {@link LevelChunkSection} in the world
	 * @return the {@link DataLayer} for sky light of the section or <code>null</code> if no data is being stored
	 * 
	 * @see #updateIndirectSkyLight(ServerLevel, HashMap)
	 */
	@Nullable
	private static DataLayer getDataLayerForSection(ServerLevel server, SectionPos pos) {
		return server.getLightEngine().getLayerListener(LightLayer.SKY).getDataLayerData(pos);
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
		
		if(x < 0) {
			chunk = new ChunkPos(chunk.x - 1, chunk.z);
		} else if(x > 15) {
			chunk = new ChunkPos(chunk.x + 1, chunk.z);
		}
		if(z < 0) {
			chunk = new ChunkPos(chunk.x, chunk.z - 1);
		} else if(z > 15) {
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
}