package luckytntlib.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class LuckyTNTLibConfigs {

	public static void register() {
		registerCommonConfig();
	}
	
	public static void registerCommonConfig() {
		ForgeConfigSpec.Builder S_BUILDER = new ForgeConfigSpec.Builder();
		LuckyTNTLibConfigValues.registerConfig(S_BUILDER);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, S_BUILDER.build());
	}
}
