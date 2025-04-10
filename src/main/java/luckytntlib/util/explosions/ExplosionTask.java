package luckytntlib.util.explosions;

import java.lang.management.ManagementFactory;
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
	
	private final MasterExplosionThread explosionThread;
	private final ImprovedExplosion explosion;
	private final Level level;
	private final float x, y, z;
	private final ExplosionDamageCalculator damageCalculator;
	private final float resistanceFac;
	private final boolean ignoreFluids;
	private final int allowedSize;
	private final List<Vector3f> vectors;
	
	public ExplosionTask(MasterExplosionThread explosionThread, ImprovedExplosion explosion, float resistanceFac, boolean ignoreFluids, int allowedSize, List<Vector3f> vectors) {
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
					if (resultBitSets.get(key).cardinality() == 4096) {
						resultBitSets.remove(key);
						explosionThread.sectionsToRemove.put(key, key);
					}
				} else {
					resultBitSets.put(key, toMergeBitSets.get(key));
				}
			}
			return resultBitSets;
		}
		return calculate();
	}

	private Long2ObjectMap<BitSet> calculate() {
		Long2ObjectMap<BitSet> editedSections = new Long2ObjectOpenHashMap<BitSet>();
		
		float vectorLength;
		float xStep;
		float yStep;
		float zStep;
		float blockX;
		float blockY;
		float blockZ;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int lastPosX;
		int lastPosY;
		int lastPosZ;
		long sectionPos;
		long lastSectionPos = Long.MAX_VALUE;
		LevelChunkSection section;
		BlockState currentBlockState;
		float[] lastExplosionResistances = new float[0];
		BitSet bitSet = new BitSet(4096);
		for (Vector3f v : vectors) {
			vectorLength = v.length();
			xStep = v.x / vectorLength * 0.3f;
			yStep = v.y / vectorLength * 0.3f;
			zStep = v.z / vectorLength * 0.3f;
			blockX = x;
			blockY = y;
			blockZ = z;
			lastPosX = 0;
			lastPosY = -10000;
			lastPosZ = 0;
			for (float step = 0f; step < vectorLength; step += 0.225f) {
				blockX += xStep;
				blockY += yStep;
				blockZ += zStep;
				pos.set((int) blockX, (int) blockY, (int) blockZ);
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
				/**
				 * If the section is empty, we can skip to the next step
				 */
				if (explosionThread.emptySections.containsKey(sectionPos)) {
					vectorLength -= 0.3f * resistanceFac;
					continue;
				}
				float[] currentExplosionResistances;
				if (sectionPos == lastSectionPos) {
					currentExplosionResistances = lastExplosionResistances;
				} else {
					if (!explosionThread.sectionResistances.containsKey(sectionPos)) {
						section = level.getChunkAt(pos).getSection((pos.getY() >> 4) - level.getMinSection());
						if (section.hasOnlyAir()) {
							explosionThread.emptySections.put(sectionPos, sectionPos);
							vectorLength -= 0.3f * resistanceFac;
							continue;
						} else {
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
						}
					}
					currentExplosionResistances = explosionThread.sectionResistances.get(sectionPos);
				}
				if (currentExplosionResistances != lastExplosionResistances) {		
					if (editedSections.containsKey(sectionPos)) {
						bitSet = editedSections.get(sectionPos);
					} else {
						editedSections.put(sectionPos, bitSet = new BitSet(4096));
					}
				}
				float resistance = currentExplosionResistances[((pos.getX() & 15) << 8) | ((pos.getY() & 15) << 4) | (pos.getZ() & 15)];
				if (resistance != 0) {
					vectorLength -= (resistance + 0.3f) * resistanceFac;
				}
				if (vectorLength > 0) {
					bitSet.set(((pos.getX() & 15) << 8) | ((pos.getY() & 15) << 4) | (pos.getZ() & 15));
				}
				/**
				 * If all bits are set to true, we can mark the section as "to remove"
				 */
				if (bitSet.cardinality() == 4096) {
					editedSections.remove(sectionPos);
					explosionThread.sectionsToRemove.put(sectionPos, sectionPos);
				}
				lastSectionPos = sectionPos;
				lastExplosionResistances = currentExplosionResistances;
			}
		}
		long cpuTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 1000000;
		System.out.println("CPU-Zeit (ms): " + vectors.size() + " " + cpuTime);
		System.out.println("Task done");
		return editedSections;
	}
}
