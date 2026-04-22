package luckytntlib.registry;

import luckytntlib.LuckyTNTLib;
import luckytntlib.block.LTNTBlock;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.util.BiomeSetter;
import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.explosions.ExplosionHelper;
import luckytntlib.util.explosions.ImprovedExplosion;
import luckytntlib.util.explosions.rules.BlockExplosionRule;
import luckytntlib.util.tnteffects.PrimedTNTEffect;
import luckytntlib.util.tnteffects.TNTXStrengthEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
	public static final RegistryObject<EntityType<PrimedLTNT>> ENT_TNT_X10000_MULTI = RH.registerTNTEntity("tnt_x10000_multi", TNT_X10000_EFFECT.fuse(80).buildTNT(() -> TempRegistry.BLOCK_TNT_X10000_MULTI));
	public static final RegistryObject<EntityType<PrimedLTNT>> ENT_SUPERNOVA = RH.registerTNTEntity("supernova", new TestEffect());
	
	//BLOCKS
	public static final RegistryObject<LTNTBlock> BLOCK_TNT_X10000 = RH.registerTNTBlock("tnt_x10000", TempRegistry.ENT_TNT_X10000, "d", MapColor.COLOR_PINK, true);
	public static final RegistryObject<LTNTBlock> BLOCK_TNT_X10000_MULTI = RH.registerTNTBlock("tnt_x10000_multi", TempRegistry.ENT_TNT_X10000_MULTI, "d", MapColor.COLOR_PINK, true);
	public static final RegistryObject<LTNTBlock> BLOCK_SUPERNOVA = RH.registerTNTBlock("supernova", TempRegistry.ENT_SUPERNOVA, "d", MapColor.COLOR_RED, true);
	
	
	static class TestEffect extends PrimedTNTEffect {
		
		@Override
		public void serverExplosion(IExplosiveEntity ent) {
			ExplosionHelper.createCylindricalCrater(ent.getLevel(), ent.getPos(), 100, 50, 100f, new BlockExplosionRule(Blocks.WATER.defaultBlockState()));
			BiomeSetter.setBiomeInCylinder(ent.getLevel(), ent.getPos(), 110, 60, Biomes.WARM_OCEAN);
		}
	}
	
	static class TestEffectSingleThreaded extends PrimedTNTEffect {
		
		@Override
		public void serverExplosion(IExplosiveEntity ent) {
			ImprovedExplosion explosion = new ImprovedExplosion(ent.getLevel(), (Entity)ent, null, ent.getPos(), 300);
			explosion.doEntityExplosion(1f, true);
			explosion.doImprovedBlockExplosion(0.167f, 0.05f, true, true, null);
			explosion.spawnExplosionParticles();
		}
	}
}
