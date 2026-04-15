package luckytntlib.client;

import java.lang.reflect.Field;
import java.util.BitSet;

import javax.annotation.Nullable;

import luckytntlib.util.explosions.ExplosionHelper;
import luckytntlib.util.explosions.rules.ExplosionRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainer;
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
	
	public static void updateChunkSection(SectionPos pos, BitSet changed, boolean allAffected, boolean updateLight) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		
		if (level == null) {
			return;
		}
		
		LevelChunk chunk = level.getChunk(pos.getX(), pos.getZ());
		chunk.setLoaded(true);
		
		if (updateLight) {		
			level.queueLightUpdate(() -> {
				LevelLightEngine engine = level.getLightEngine();
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
		} else {
			PalettedContainer<BlockState> states = chunk.getSection(pos.getY()).getStates();
			
			ExplosionRule rule = currentRule;
			Vec3 center = currentCenter;
			BlockPos centerPos = center == null ? null : BlockPos.containing(center);
			boolean useRule = rule != null && center != null;
			long worldSeed = levelSeed;
			SingleThreadedRandomSource random = useRule ? new SingleThreadedRandomSource(0) : null;
			
			if (allAffected) {
				for (int s = 0; s < 4096; s++) {
					Vec3i secpos = ExplosionHelper.decodeSectionPos(s);
					BlockPos blockpos = new BlockPos((pos.getX() << 4) + secpos.getX(), ((pos.getY() + level.getMinSection()) << 4) + secpos.getY(), (pos.getZ() << 4) + secpos.getZ());
					
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
					states.set(secpos.getX(), secpos.getY(), secpos.getZ(), useRule ? rule.getState(level, states.get(secpos.getX(), secpos.getY(), secpos.getZ()), center, offX, offY, offZ, random) : Blocks.AIR.defaultBlockState());
				}
			} else {
				for (int s = 0; s < 4096; ++s) {
					if (!changed.get(s)) {
						continue;
					}
					Vec3i secpos = ExplosionHelper.decodeSectionPos(s);
					BlockPos blockpos = new BlockPos((pos.getX() << 4) + secpos.getX(), ((pos.getY() + level.getMinSection()) << 4) + secpos.getY(), (pos.getZ() << 4) + secpos.getZ());
					
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
					states.set(secpos.getX(), secpos.getY(), secpos.getZ(), useRule ? rule.getState(level, states.get(secpos.getX(), secpos.getY(), secpos.getZ()), center, offX, offY, offZ, random) : Blocks.AIR.defaultBlockState());
				}
			}
		}
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
	
	public static void setupExplosion(@Nullable ExplosionRule rule, @Nullable Vec3 center, long seed) {
		currentRule = rule;
		currentCenter = center;
		levelSeed = seed;
	}
}
