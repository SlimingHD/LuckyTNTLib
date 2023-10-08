package luckytntlib.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class LuckyTNTLibConfigValues {

	public static ForgeConfigSpec.BooleanValue PERFORMANT_EXPLOSION;
	public static ForgeConfigSpec.DoubleValue EXPLOSION_PERFORMANCE_FACTOR;
	
	public static void registerConfig(ForgeConfigSpec.Builder builder) {
		builder.comment("General Explosions").push("Performance");
		PERFORMANT_EXPLOSION = builder.comment("Whether or not an explosion should be used that has more performance at the cost of detail").define("performantExplosion", false);
		EXPLOSION_PERFORMANCE_FACTOR = builder.comment("Higher values give more performance at the cost of details, lower values give more details at the cost of performance").defineInRange("explosionPerformanceFactor", 0.3d, 0.3d, 0.6d);
	}
}
