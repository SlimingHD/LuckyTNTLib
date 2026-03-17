package luckytntlib.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class LuckyTNTLibConfigValues {

	public static ForgeConfigSpec.BooleanValue MULTITHREADED_EXPLOSIONS;
	public static ForgeConfigSpec.IntValue MAX_EXPLOSION_THREADS;

	@Deprecated(since = "47.2.32.1", forRemoval = true)
	public static ForgeConfigSpec.BooleanValue PERFORMANT_EXPLOSION;
	@Deprecated(since = "47.2.32.1", forRemoval = true)
	public static ForgeConfigSpec.DoubleValue EXPLOSION_PERFORMANCE_FACTOR;
	
	public static void registerConfig(ForgeConfigSpec.Builder builder) {
		builder.comment("Explosions").push("Performance");
		MULTITHREADED_EXPLOSIONS = builder.comment("Enables multithreading for raycasted explosions, making them run in parallel to the game. Adjust the number of threads to keep memory and CPU usage safe").define("multithreadedExplosions", true);
		MAX_EXPLOSION_THREADS = builder.comment("Restricts the maximum amount of simultaneous explosions if multithreaded explosions is enabled. Note that thread count per explosion will be chosen dynamically and cannot be edited. Excess explosions will be queued").defineInRange("maxExplosionThreads", 4, 1, 20);
		
		PERFORMANT_EXPLOSION = builder.comment("Whether or not an explosion should be used that has more performance at the cost of detail").define("performantExplosion", true);
		EXPLOSION_PERFORMANCE_FACTOR = builder.comment("Higher values give more performance at the cost of details, lower values give more details at the cost of performance").defineInRange("explosionPerformanceFactor", 0.3d, 0.3d, 0.6d);
	}
}
