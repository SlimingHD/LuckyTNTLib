package luckytntlib.registry;

import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import luckytntlib.util.explosions.rules.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryBuilder;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExplosionRuleRegistry {

	public static final ResourceKey<Registry<Function<JsonObject, ExplosionRule>>> EXPLOSION_RULES_KEY = ResourceKey.createRegistryKey(new ResourceLocation(LuckyTNTLib.MODID, "explosion_rules"));
	
	/**
	 * Registry containing decoders for all registered {@link ExplosionRule}s
	 */
	public static Supplier<IForgeRegistry<Function<JsonObject, ExplosionRule>>> EXPLOSION_RULES;
	
	@SubscribeEvent
	public static void onCreateRegistries(NewRegistryEvent event) {
		EXPLOSION_RULES = event.create(RegistryBuilder.<Function<JsonObject, ExplosionRule>>of(EXPLOSION_RULES_KEY.location()).setDefaultKey(AlwaysExplosionRule.RESOURCE_LOCATION));
	}
	
	/**
	 * All rules this library provides by default registered to {@link #EXPLOSION_RULES}
	 */
	@SubscribeEvent
	public static void onRegister(RegisterEvent event) {
		event.register(EXPLOSION_RULES_KEY, BlockExplosionRule.RESOURCE_LOCATION, () -> BlockExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterDistanceExplosionRule.RESOURCE_LOCATION, () -> FilterDistanceExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, StackedExplosionRule.RESOURCE_LOCATION, () -> StackedExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterAirExplosionRule.RESOURCE_LOCATION, () -> FilterAirExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, CraterExplosionRule.RESOURCE_LOCATION, () -> CraterExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, RandomBlockExplosionRule.RESOURCE_LOCATION, () -> RandomBlockExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterBlastResistanceExplosionRule.RESOURCE_LOCATION, () -> FilterBlastResistanceExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterBlockExplosionRule.RESOURCE_LOCATION, () -> FilterBlockExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterRandomExplosionRule.RESOURCE_LOCATION, () -> FilterRandomExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterRandomDistanceExplosionRule.RESOURCE_LOCATION, () -> FilterRandomDistanceExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterSurfaceExplosionRule.RESOURCE_LOCATION, () -> FilterSurfaceExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FireExplosionRule.RESOURCE_LOCATION, () -> FireExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, LogicExplosionRule.RESOURCE_LOCATION, () -> LogicExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, AlwaysExplosionRule.RESOURCE_LOCATION, () -> AlwaysExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, ScheduleTickExplosionRule.RESOURCE_LOCATION, () -> ScheduleTickExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, CanSurviveExplosionRule.RESOURCE_LOCATION, () -> CanSurviveExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterCollidableExplosionRule.RESOURCE_LOCATION, () -> FilterCollidableExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterFullBlockExplosionRule.RESOURCE_LOCATION, () -> FilterFullBlockExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterOffYExplosionRule.RESOURCE_LOCATION, () -> FilterOffYExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, CopyBlockExplosionRule.RESOURCE_LOCATION, () -> CopyBlockExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, OffsetExplosionRule.RESOURCE_LOCATION, () -> OffsetExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterLiquidExplosionRule.RESOURCE_LOCATION, () -> FilterLiquidExplosionRule::decode);
	}
}
