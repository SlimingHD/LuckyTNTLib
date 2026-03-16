package luckytntlib.util.explosions;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

@SuppressWarnings("serial")
public class ExplosionTask extends RecursiveTask<Long2ObjectMap<BitSet>> {
	
	private final ExplosionThread explosionThread;
	private final ImprovedExplosion explosion;
	private final Level level;
	private final float x, y, z;
	private final ExplosionDamageCalculator damageCalculator;
	private final float resistanceFac;
	private final boolean ignoreFluids;
	private final int allowedSize;
	private final List<Vector3f> vectors;
	
	public ExplosionTask(ExplosionThread explosionThread, ImprovedExplosion explosion, float resistanceFac, boolean ignoreFluids, int allowedSize, List<Vector3f> vectors) {
		this.explosionThread = explosionThread;
		this.explosion = explosion;
		this.level = explosion.level;
		this.x = (float)explosion.posX;
		this.y = (float)explosion.posY;
		this.z = (float)explosion.posZ;
		this.damageCalculator = explosion.damageCalculator;
		this.resistanceFac = resistanceFac;
		this.ignoreFluids = ignoreFluids;
		this.allowedSize = allowedSize;
		this.vectors = vectors;
	}
	
	/**
	 * Splits itself into two smaller tasks if too many vectors have been supplied.
	 * Amount of vectors per task is determined to be either 50000 or the total amount of vectors divided by the available processor count.
	 */
	@Override
	protected Long2ObjectMap<BitSet> compute() {
		if (vectors.size() > allowedSize) {
			ExplosionTask task1 = new ExplosionTask(explosionThread, explosion, resistanceFac, ignoreFluids, allowedSize, vectors.subList(0, vectors.size() / 2));
			ExplosionTask task2 = new ExplosionTask(explosionThread, explosion, resistanceFac, ignoreFluids, allowedSize, vectors.subList(vectors.size() / 2, vectors.size()));
			task1.fork();
			task2.fork();
			Long2ObjectMap<BitSet> toMergeBitSets = task1.join();
			Long2ObjectMap<BitSet> resultBitSets = task2.join();
			for (long key : toMergeBitSets.keySet()) {
				if (resultBitSets.containsKey(key)) {
					resultBitSets.get(key).or(toMergeBitSets.get(key));
					if (resultBitSets.get(key).nextClearBit(0) >= 4096) {
						resultBitSets.remove(key);
						explosionThread.fullSections.add(key);
					}
				} else {
					resultBitSets.put(key, toMergeBitSets.get(key));
				}
			}
			return resultBitSets;
		}
		return calculate();
	}

	/**
	 * Functionally equivalent to the singlethreaded method.
	 */
	private Long2ObjectMap<BitSet> calculate() {
		Long2ObjectMap<BitSet> editedSections = new Long2ObjectOpenHashMap<BitSet>();

		for (Vector3f v : vectors) {
			float vectorLength = v.length();
			float xStep = v.x / vectorLength * 0.3f;
			float yStep = v.y / vectorLength * 0.3f;
			float zStep = v.z / vectorLength * 0.3f;
			float blockX = x;
			float blockY = y;
			float blockZ = z;
			int lastPosX = 0;
			int lastPosY = -10000;
			int lastPosZ = 0;
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			long sectionPos;
			long lastSectionPos = Long.MAX_VALUE;
			boolean lastSectionEmpty = false;
			boolean lastSectionFull = false;
			LevelChunkSection section;
			BlockState currentBlockState;
			float[] currentExplosionResistances = new float[0];
			BitSet bitSet = new BitSet(0);
			for (float step = 0f; step < vectorLength; step += 0.225f) {
				blockX += xStep;
				blockY += yStep;
				blockZ += zStep;
				pos.set((int)blockX, (int)blockY, (int)blockZ);
				if (!level.isInWorldBounds(pos)) {
					break;
				}
				if (pos.getX() == lastPosX && pos.getY() == lastPosY && pos.getZ() == lastPosZ) {
					continue;
				}
				lastPosX = pos.getX();
				lastPosY = pos.getY();
				lastPosZ = pos.getZ();
				sectionPos = SectionPos.asLong(pos);
				if ((sectionPos == lastSectionPos && lastSectionEmpty) || explosionThread.emptySections.contains(sectionPos)) {
					lastSectionEmpty = true;
					vectorLength -= 0.3f * resistanceFac;
					continue;
				}
				lastSectionEmpty = false;
				if (sectionPos != lastSectionPos) {
					currentExplosionResistances = explosionThread.sectionResistances.get(sectionPos);
					if (currentExplosionResistances == null) {
						section = level.getChunkAt(pos).getSection((pos.getY() >> 4) - level.getMinSection());
						if (section.hasOnlyAir()) {
							explosionThread.emptySections.add(sectionPos);
							vectorLength -= 0.3f * resistanceFac;
							continue;
						}
						float[] explosionResistances = new float[4096];
						explosionThread.sectionResistances.put(sectionPos, explosionResistances);
						for (int x = 0; x < 16; x++) {
							for (int y = 0; y < 16; y++) {
								for (int z = 0; z < 16; z++) {
									currentBlockState = section.getBlockState(x, y, z);
									explosionResistances[x << 8 | y << 4 | z] = ignoreFluids && !currentBlockState.getFluidState().isEmpty() ? 0 : damageCalculator.getBlockExplosionResistance(explosion, level, pos, currentBlockState, currentBlockState.getFluidState()).orElse(0f);
								}
							}
						}
						currentExplosionResistances = explosionResistances;
					}
					bitSet = editedSections.get(sectionPos);
					if (bitSet == null) {
						editedSections.put(sectionPos, bitSet = new BitSet(4096));
					}
				}
				int blockIndex = ((pos.getX() & 15) << 8) | ((pos.getY() & 15) << 4) | (pos.getZ() & 15);
				vectorLength -= (currentExplosionResistances[blockIndex] + 0.3f) * resistanceFac;
				if (vectorLength <= 0f) {
					break;
				}
				if ((sectionPos == lastSectionPos && lastSectionFull) || explosionThread.fullSections.contains(sectionPos)) {
					lastSectionFull = true;
					continue;
				}
				lastSectionFull = false;
				lastSectionPos = sectionPos;
				if (!bitSet.get(blockIndex)) {
					bitSet.set(blockIndex);
					if (bitSet.nextClearBit(0) >= 4096) {
						editedSections.remove(sectionPos);
						explosionThread.fullSections.add(sectionPos);
					}
				}
			}
		}
		return editedSections;
	}
}
