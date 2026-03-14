package luckytntlib.registry;

import org.joml.Vector3f;

import luckytntlib.LuckyTNTLib;
import luckytntlib.block.LTNTBlock;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.RandomList;
import luckytntlib.util.explosions.DistanceCalculator;
import luckytntlib.util.explosions.ExplosionHelper;
import luckytntlib.util.explosions.ImprovedExplosion;
import luckytntlib.util.explosions.rules.DistanceExplosionRule;
import luckytntlib.util.explosions.rules.DistanceExplosionRule.SmallerAndGreaterThanDistanceComparator;
import luckytntlib.util.explosions.rules.DistanceExplosionRule.SmallerThanDistanceComparator;
import luckytntlib.util.explosions.rules.ExplosionRule;
import luckytntlib.util.explosions.rules.FilterAirExplosionRule;
import luckytntlib.util.explosions.rules.RandomBlockExplosionRule;
import luckytntlib.util.explosions.rules.StackedExplosionRule;
import luckytntlib.util.tnteffects.PrimedTNTEffect;
import luckytntlib.util.tnteffects.TNTXStrengthEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TempRegistry {

	public static final DeferredRegister<Block> blockRegistry = DeferredRegister.create(ForgeRegistries.BLOCKS, LuckyTNTLib.MODID);
    public static final DeferredRegister<Item> itemRegistry = DeferredRegister.create(ForgeRegistries.ITEMS, LuckyTNTLib.MODID);
    public static final DeferredRegister<EntityType<?>> entityRegistry = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LuckyTNTLib.MODID);
    public static final RegistryHelper RH = new RegistryHelper(blockRegistry, itemRegistry, entityRegistry);
    
    //TNT EFFECTS
	public static final TNTXStrengthEffect.Builder TNT_X10000_EFFECT = new TNTXStrengthEffect.Builder().strength(300).resistanceImpact(0.167f).randomVecLength(0.05f).knockbackStrength(30f).isStrongExplosion(true);

	//ENTITIES
	public static final RegistryObject<EntityType<PrimedLTNT>> ENT_TNT_X10000 = RH.registerTNTEntity("tnt_x10000", /*TNT_X10000_EFFECT.fuse(80).buildTNT(() -> TempRegistry.BLOCK_TNT_X10000)*/new TestEffectSingleThreaded());
	public static final RegistryObject<EntityType<PrimedLTNT>> ENT_SUPERNOVA = RH.registerTNTEntity("supernova", new TestEffect());
	
	//BLOCKS
	public static final RegistryObject<LTNTBlock> BLOCK_TNT_X10000 = RH.registerTNTBlock("tnt_x10000", TempRegistry.ENT_TNT_X10000, "d", MapColor.COLOR_PINK, true);
	public static final RegistryObject<LTNTBlock> BLOCK_SUPERNOVA = RH.registerTNTBlock("supernova", TempRegistry.ENT_SUPERNOVA, "d", MapColor.COLOR_RED, true);
	
	
	static class TestEffect extends PrimedTNTEffect {
		
		@Override
		public void serverExplosion(IExplosiveEntity ent) {
			DistanceCalculator calc = (x, z, r, s) -> (int)(Math.sqrt(r * r - x * x / s.x - z * z / s.z) * Math.sqrt(s.y));
			ExplosionRule rule = new FilterAirExplosionRule(new StackedExplosionRule(new DistanceExplosionRule(new RandomBlockExplosionRule(RandomList.<BlockState>floatBuilder().addEntry(Blocks.RED_WOOL.defaultBlockState(), 0.95f).addEntry(Blocks.LIME_WOOL.defaultBlockState(), 0.05f).build()), new SmallerThanDistanceComparator(75)), new DistanceExplosionRule(new RandomBlockExplosionRule(RandomList.ofEqualProbability(Blocks.YELLOW_WOOL.defaultBlockState(), Blocks.PURPLE_WOOL.defaultBlockState())), new SmallerAndGreaterThanDistanceComparator(85, 120))));
			ExplosionHelper.createCrater(ent.getLevel(), ent.getPos(), 150, new Vector3f(1f, 1f, 1f), 5000, calc, rule);
			//ExplosionHelper.createSphericalCrater(ent.getLevel(), ent.getPos(), 300, 5000);
		}
	}
	
	static class TestEffectSingleThreaded extends PrimedTNTEffect {
		
		@Override
		public void serverExplosion(IExplosiveEntity ent) {
			ExplosionRule rule = new FilterAirExplosionRule(new StackedExplosionRule(new DistanceExplosionRule(new RandomBlockExplosionRule(RandomList.<BlockState>floatBuilder().addEntry(Blocks.RED_WOOL.defaultBlockState(), 0.95f).addEntry(Blocks.LIME_WOOL.defaultBlockState(), 0.05f).build()), new SmallerThanDistanceComparator(75)), new DistanceExplosionRule(new RandomBlockExplosionRule(RandomList.ofEqualProbability(Blocks.YELLOW_WOOL.defaultBlockState(), Blocks.PURPLE_WOOL.defaultBlockState())), new SmallerAndGreaterThanDistanceComparator(85, 120))));

			ImprovedExplosion explosion = new ImprovedExplosion(ent.getLevel(), (Entity)ent, null, ent.getPos(), 300);
			explosion.doEntityExplosion(1f, true);
			explosion.doImprovedBlockExplosion(0.167f, 0.05f, true, false, ent.getLevel().getRandom(), rule);
			explosion.spawnExplosionParticlesServer();
		}
	}
}
