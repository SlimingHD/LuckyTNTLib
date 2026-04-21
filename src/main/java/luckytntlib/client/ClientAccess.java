package luckytntlib.client;

import java.lang.reflect.Field;
import java.util.BitSet;

import javax.annotation.Nullable;

import luckytntlib.util.explosions.ExplosionHelper;
import luckytntlib.util.explosions.HeightmapUpdateHelper;
import luckytntlib.util.explosions.rules.ExplosionRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;

public class ClientAccess {
	
	@Nullable
	private static ExplosionRule currentRule;
	@Nullable
	private static Vec3 currentCenter;
	private static long levelSeed;

	private static Field heightmapField;
	
	public static void updateChunkSection(SectionPos pos, BitSet changed, boolean allAffected) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		
		if (level == null) {
			return;
		}

		LevelLightEngine engine = level.getLightEngine();
		LevelChunk chunk = level.getChunk(pos.getX(), pos.getZ());
		
		LevelChunkSection section = chunk.getSection(pos.getY());
		PalettedContainer<BlockState> states = section.getStates();
		
		ExplosionRule rule = currentRule;
		Vec3 center = currentCenter;
		BlockPos centerPos = center == null ? null : BlockPos.containing(center);
		boolean useRule = rule != null && center != null;
		long worldSeed = levelSeed;
		SingleThreadedRandomSource random = useRule ? new SingleThreadedRandomSource(0) : null;
		
		if (allAffected) {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = 0; y < 16; y++) {
						BlockPos blockpos = new BlockPos((pos.getX() << 4) + x, ((pos.getY() + level.getMinSection()) << 4) + y, (pos.getZ() << 4) + z);
						
						int offX = 0;
						int offY = 0;
						int offZ = 0;
						if (useRule) {
							offX = blockpos.getX() - centerPos.getX();
							offY = blockpos.getY() - centerPos.getY();
							offZ = blockpos.getZ() - centerPos.getZ();
							random.setSeed(ExplosionHelper.explosionSeed(worldSeed, centerPos, offX, offY, offZ));
						}
						
						chunk.removeBlockEntity(blockpos);
						states.set(x, y, z, useRule ? rule.getState(level, states.get(x, y, z), center, offX, offY, offZ, random) : Blocks.AIR.defaultBlockState());
					}
				}
			}
		} else {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					for (int y = 0; y < 16; y++) {
						if (!changed.get(ExplosionHelper.encodeSectionPos(x, y, z))) {
							continue;
						}
						BlockPos blockpos = new BlockPos((pos.getX() << 4) + x, ((pos.getY() + level.getMinSection()) << 4) + y, (pos.getZ() << 4) + z);
						
						int offX = 0;
						int offY = 0;
						int offZ = 0;
						if (useRule) {
							offX = blockpos.getX() - centerPos.getX();
							offY = blockpos.getY() - centerPos.getY();
							offZ = blockpos.getZ() - centerPos.getZ();
							random.setSeed(ExplosionHelper.explosionSeed(worldSeed, centerPos, offX, offY, offZ));
						}
						
						chunk.removeBlockEntity(blockpos);
						states.set(x, y, z, useRule ? rule.getState(level, states.get(x, y, z), center, offX, offY, offZ, random) : Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		
		section.recalcBlockCounts();
		engine.updateSectionStatus(SectionPos.of(pos.getX(), pos.getY() + level.getMinSection(), pos.getZ()), section.hasOnlyAir());
	}
	
	public static void updateChunkSectionLight(SectionPos pos, BitSet changed) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		
		if (level == null) {
			return;
		}

		LevelLightEngine engine = level.getLightEngine();
		
		level.queueLightUpdate(() -> {
			int sectionX = pos.getX() << 4;
			int sectionY = (pos.getY() + level.getMinSection()) << 4;
			int sectionZ = pos.getZ() << 4;
			for (int x = 0; x < 16; ++x) {
				for (int z = 0; z < 16; ++z) {
					for (int y = 15; y >= 0; --y) {
						if (!changed.get(ExplosionHelper.encodeSectionPos(x, y, z))) {
							continue;
						}
						engine.checkBlock(new BlockPos(sectionX + x, sectionY + y, sectionZ + z));
					}
				}
			}
			engine.runLightUpdates();
		});
	}
	
	public static void updateChunkSkyLightSources(ChunkPos pos, int[] data) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		
		if (level == null) {
			return;
		}
		
		BitStorage heightmap = null;
		
		if (heightmapField == null) {
			for (Field f : ChunkSkyLightSources.class.getDeclaredFields()) {
				f.setAccessible(true);
				if (f.getType() == BitStorage.class) {
					heightmapField = f;
					break;
				}
			}
		}
		
		try {
			heightmap = (BitStorage)heightmapField.get(level.getChunk(pos.x, pos.z).getSkyLightSources());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		if (heightmap != null) {
			for (int i = 0; i < 256; i++) {
				heightmap.set(i, data[i]);
			}
		}
	}
	
	public static void updateChunkSectionBiome(SectionPos pos, BitSet changed, ResourceKey<Biome> biomeKey) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		
		if (level == null) {
			return;
		}
		
		Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
		Holder<Biome> biome = registry.getHolderOrThrow(biomeKey);
		
		LevelChunk chunk = level.getChunk(pos.getX(), pos.getZ());
		LevelChunkSection section = chunk.getSection(pos.getY());
		PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>)section.getBiomes();
		for (int x = 0; x < 4; ++x) {
			for (int y = 0; y < 4; ++y) {
				for (int z = 0; z < 4; ++z) {
					if (!changed.get((x << 4) | (y << 2) | z)) {
						continue;
					}
					biomes.set(x, y, z, biome);
				}
			}
		}
	}
	
	public static void updateHeightmaps(ChunkPos pos, long[][] data) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		
		if (level == null) {
			return;
		}
		
		LevelChunk chunk = level.getChunk(pos.x, pos.z);
		int index = 0;
		for (Heightmap.Types type : HeightmapUpdateHelper.TYPES) {
			chunk.getOrCreateHeightmapUnprimed(type).setRawData(chunk, type, data[index++]);
		}
	}
	
	public static void setupExplosion(@Nullable ExplosionRule rule, @Nullable Vec3 center, long seed) {
		currentRule = rule;
		currentCenter = center;
		levelSeed = seed;
	}
}
