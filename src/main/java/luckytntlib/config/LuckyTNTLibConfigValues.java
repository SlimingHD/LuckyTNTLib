package luckytntlib.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class LuckyTNTLibConfigValues {

	public static ForgeConfigSpec.BooleanValue MULTITHREADED_EXPLOSIONS;
	public static ForgeConfigSpec.IntValue MAX_EXPLOSION_THREADS;
	public static ForgeConfigSpec.IntValue MULTITHREADING_THRESHOLD;
	
	public static ForgeConfigSpec.BooleanValue UPDATE_BLOCK_LIGHT;
	public static ForgeConfigSpec.IntValue BLOCK_UPDATE_THRESHOLD;
	
	public static ForgeConfigSpec.BooleanValue BENCHMARK_EXPLOSIONS;

	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public static ForgeConfigSpec.BooleanValue PERFORMANT_EXPLOSION;
	@Deprecated(since = "47.2.32.2", forRemoval = true)
	public static ForgeConfigSpec.DoubleValue EXPLOSION_PERFORMANCE_FACTOR;
	
	public static void registerConfig(ForgeConfigSpec.Builder builder) {
		builder.comment("Explosions").push("Performance");
		MULTITHREADED_EXPLOSIONS = builder.comment("Enables multithreading for raycasted explosions, making them run in parallel to the game. Adjust the number of threads to keep memory and CPU usage safe.").define("multithreadedExplosions", true);
		MAX_EXPLOSION_THREADS = builder.comment("Restricts the maximum amount of simultaneous explosions if multi-threaded explosions is enabled. Note that thread count per explosion will be chosen dynamically and cannot be edited. Excess explosions will be queued.").defineInRange("maxExplosionThreads", 4, 1, 20);
		MULTITHREADING_THRESHOLD = builder.comment("Defines the radius at which explosions will be multi-threaded. For reference, TNTx500 = size 80, TNTx2000 = size 160, and TNTx10000 = size 300").defineInRange("multithreadingThreshold", 90, 60, 300);
		builder.pop();
		
		builder.comment("Post explosions").push("Quality");
		UPDATE_BLOCK_LIGHT = builder.comment("En- or disables block light updates when using more performant explosions. Enabling this will make block lights update correctly at the cost of performance.").define("updateBlockLight", true);
		BLOCK_UPDATE_THRESHOLD = builder.comment("Defines the radius at which explosions will stop perform blocks updates. This setting significantly impacts performance. For reference, Supernova has radius 200.").defineInRange("blockUpdateRadius", 90, 30, 300);
		builder.pop();
		
		builder.comment("Debug settings").push("Debug");
		BENCHMARK_EXPLOSIONS = builder.comment("En- or disables the printing of benchmark results of explosions to the general chat.").define("benchmarkExplosions", false);
		builder.pop();
		
		builder.comment("Explosions").push("Deprecated");
		PERFORMANT_EXPLOSION = builder.comment("Whether or not an explosion should be used that has more performance at the cost of detail.").define("performantExplosion", true);
		EXPLOSION_PERFORMANCE_FACTOR = builder.comment("Higher values give more performance at the cost of details, lower values give more details at the cost of performance.").defineInRange("explosionPerformanceFactor", 0.3d, 0.3d, 0.6d);
		builder.pop();
	}
}
