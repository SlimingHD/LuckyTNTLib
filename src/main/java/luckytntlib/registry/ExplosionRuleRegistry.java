package luckytntlib.registry;

import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonObject;

import luckytntlib.LuckyTNTLib;
import luckytntlib.util.explosions.rules.CraterExplosionRule;
import luckytntlib.util.explosions.rules.DistanceExplosionRule;
import luckytntlib.util.explosions.rules.DistanceExplosionRule.DistanceComparator;
import luckytntlib.util.explosions.rules.DistanceExplosionRule.GreaterThanDistanceComparator;
import luckytntlib.util.explosions.rules.DistanceExplosionRule.SmallerAndGreaterThanDistanceComparator;
import luckytntlib.util.explosions.rules.DistanceExplosionRule.SmallerThanDistanceComparator;
import luckytntlib.util.explosions.rules.ExplosionRule;
import luckytntlib.util.explosions.rules.FilterAirExplosionRule;
import luckytntlib.util.explosions.rules.RandomBlockExplosionRule;
import luckytntlib.util.explosions.rules.SimpleExplosionRule;
import luckytntlib.util.explosions.rules.StackedExplosionRule;
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
	public static final ResourceKey<Registry<Function<JsonObject, DistanceComparator>>> DISTANCE_COMPARATORS_KEY = ResourceKey.createRegistryKey(new ResourceLocation(LuckyTNTLib.MODID, "distance_comparators"));
	
	public static Supplier<IForgeRegistry<Function<JsonObject, ExplosionRule>>> EXPLOSION_RULES;
	public static Supplier<IForgeRegistry<Function<JsonObject, DistanceComparator>>> DISTANCE_COMPARATORS;
	
	@SubscribeEvent
	public static void onCreateRegistries(NewRegistryEvent event) {
		EXPLOSION_RULES = event.create(RegistryBuilder.<Function<JsonObject, ExplosionRule>>of(EXPLOSION_RULES_KEY.location()).setDefaultKey(SimpleExplosionRule.RESOURCE_LOCATION));
		DISTANCE_COMPARATORS = event.create(RegistryBuilder.<Function<JsonObject, DistanceComparator>>of(DISTANCE_COMPARATORS_KEY.location()).setDefaultKey(GreaterThanDistanceComparator.RESOURCE_LOCATION));
	}
	
	@SubscribeEvent
	public static void onRegister(RegisterEvent event) {
		event.register(EXPLOSION_RULES_KEY, SimpleExplosionRule.RESOURCE_LOCATION, () -> SimpleExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, DistanceExplosionRule.RESOURCE_LOCATION, () -> DistanceExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, StackedExplosionRule.RESOURCE_LOCATION, () -> StackedExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, FilterAirExplosionRule.RESOURCE_LOCATION, () -> FilterAirExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, CraterExplosionRule.RESOURCE_LOCATION, () -> CraterExplosionRule::decode);
		event.register(EXPLOSION_RULES_KEY, RandomBlockExplosionRule.RESOURCE_LOCATION, () -> RandomBlockExplosionRule::decode);
		
		event.register(DISTANCE_COMPARATORS_KEY, GreaterThanDistanceComparator.RESOURCE_LOCATION, () -> GreaterThanDistanceComparator::decode);
		event.register(DISTANCE_COMPARATORS_KEY, SmallerThanDistanceComparator.RESOURCE_LOCATION, () -> SmallerThanDistanceComparator::decode);
		event.register(DISTANCE_COMPARATORS_KEY, SmallerAndGreaterThanDistanceComparator.RESOURCE_LOCATION, () -> SmallerAndGreaterThanDistanceComparator::decode);
	}
}
