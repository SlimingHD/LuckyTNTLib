package luckytntlib.util.explosions;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luckytntlib.util.ExplosionProfiler;
import luckytntlib.util.ExplosionProfiler.Benchmark;
import luckytntlib.util.ExplosionProfiler.Counter;
import luckytntlib.util.explosions.rules.ExplosionRule;

/**
 * Executes the multi-threaded part of an explosion, if multi-threading is enabled by the user and the explosion is large enough to warrant multi-threading.
 * This is done in its own thread, as blocking the main thread will lead to a deadlock.
 * The threading itself is done using a {@link ForkJoinPool}, with the number of tasks being chosen dynamically based on the available processors and a minimum size.
 * This thread is started and its explosion finalized in {@link MultithreadedExplosionHandler}.
 * Only the gathering of blocks to affect is handled in multiple threads. Both the collection of vectors and the finalization are single-threaded.
 */
public final class ExplosionThread extends Thread {

	private static final int minVectorsPerThread = 25000;
	private static final ForkJoinPool pool = new ForkJoinPool();
	
	private final ImprovedExplosion explosion;
	private final float resistanceFac;
	private final float randomVecLengthFac;
	private final boolean ignoreFluids;
	private final boolean placeFire;
	@Nullable
	private final ExplosionRule rule;
	private final List<Vector3f> vectors;
	private final ExplosionProfiler PROFILER;
	
	final Set<Long> emptySections = ConcurrentHashMap.newKeySet();
	final Set<Long> fullSections = ConcurrentHashMap.newKeySet();
	final ConcurrentHashMap<Long, float[]> sectionResistances = new ConcurrentHashMap<Long, float[]>();
	
	private Map<Long, BitSet> editedSections = new Long2ObjectOpenHashMap<BitSet>();
	
	int taskCount = 0;
	
	ExplosionThread(ImprovedExplosion explosion, float resistanceFac, float randomVecLengthFac, boolean ignoreFluids, boolean placeFire, @Nullable ExplosionRule rule, List<Vector3f> vectors) {
		this.explosion = explosion;
		this.resistanceFac = resistanceFac;
		this.randomVecLengthFac = randomVecLengthFac;
		this.ignoreFluids = ignoreFluids;
		this.placeFire = placeFire;
		this.rule = rule;
		this.vectors = vectors;
		this.PROFILER = explosion.PROFILER;
	}
	
	@Override
	public void run() {
		PROFILER.startBenchmark(Benchmark.BENCHMARK_4, "luckytntlib.benchmarking.blocks_gathered");
		PROFILER.startBenchmark(Benchmark.BENCHMARK_7, "luckytntlib.benchmarking.cpu_tasks");
		
		ExplosionTask task = new ExplosionTask(this, explosion, resistanceFac, ignoreFluids, Math.max(minVectorsPerThread, vectors.size() / (Runtime.getRuntime().availableProcessors() * 4)), vectors);
		editedSections = pool.invoke(task);
		
		PROFILER.addCounterTo(Benchmark.BENCHMARK_7, Counter.COUNTER_0, taskCount);
		PROFILER.stopBenchmarkTime(Benchmark.BENCHMARK_4);
		editedSections.forEach((pos, bitset) -> {
			PROFILER.countUp(Benchmark.BENCHMARK_1, Counter.COUNTER_0, bitset);
		});
		fullSections.forEach(pos -> {
			PROFILER.countUp(Benchmark.BENCHMARK_1, Counter.COUNTER_0, 4096);
		});
	}
	
	public Map<Long, BitSet> getEditedSections() {
		return Map.copyOf(editedSections);
	}
	
	public Set<Long> getFullSections() {
		return Set.copyOf(fullSections);
	}
	
	public ImprovedExplosion getExplosion() {
		return explosion;
	}
	
	public float getRandomVecLengthFac() {
		return randomVecLengthFac;
	}
	
	public boolean shouldPlaceFire() {
		return placeFire;
	}
	
	@Nullable
	public ExplosionRule getExplosionRule() {
		return rule;
	}
}
