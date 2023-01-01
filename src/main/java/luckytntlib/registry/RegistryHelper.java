package luckytntlib.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import luckytntlib.block.LTNTBlock;
import luckytntlib.block.LivingLTNTBlock;
import luckytntlib.entity.LExplosiveProjectile;
import luckytntlib.entity.LivingPrimedLTNT;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.item.LDynamiteItem;
import luckytntlib.util.explosions.PrimedTNTEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class RegistryHelper {
	
	public final DeferredRegister<Block> blockRegistry;
	public final DeferredRegister<Item> itemRegistry;
	public final DeferredRegister<EntityType<?>> entityRegistry;
	public final String modID;
	
	public RegistryHelper(DeferredRegister<Block> blockRegistry, DeferredRegister<Item> itemRegistry, DeferredRegister<EntityType<?>> entityRegistry, String modID) {
		this.blockRegistry = blockRegistry;
		this.itemRegistry = itemRegistry;
		this.entityRegistry = entityRegistry;
		this.modID = modID;
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, CreativeModeTab tab){
		return registerTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, true);
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, CreativeModeTab tab, boolean shouldRandomlyFuse){
		return registerTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, shouldRandomlyFuse);
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, CreativeModeTab tab, MaterialColor color, boolean shouldRandomlyFuse){
		return registerTNTBlock(TNT, new TNTBlockRegistryData.Builder(registryName).tab(tab).color(color).shouldRandomlyFuse(shouldRandomlyFuse).build());
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(RegistryObject<EntityType<PrimedLTNT>> TNT, TNTBlockRegistryData blockData){
		return registerTNTBlock(blockRegistry, itemRegistry, () -> new LTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, blockData.getColor()).sound(SoundType.GRASS), TNT, blockData.shouldRandomlyFuse()), blockData);
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, Supplier<LTNTBlock> TNTBlock, TNTBlockRegistryData blockData){
		RegistryObject<LTNTBlock> block = blockRegistry.register(blockData.getRegistryName(), TNTBlock);
		if(itemRegistry != null && blockData.makeItem()) {
			RegistryObject<Item> item = itemRegistry.register(blockData.getRegistryName(), () -> new BlockItem(block.get(), new Item.Properties().tab(blockData.getTab())) {
				
				@Override
				public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
					super.appendHoverText(stack, level, components, flag);
					if(!blockData.getDescription().getKey().equals("")) {
						components.add(MutableComponent.create(blockData.getDescription()));
					}
				}
			});
			if(blockData.addToTNTLists()) {
				if(TNTLists.TNTLists.get(modID) == null) {
					TNTLists.TNTLists.put(modID, new ArrayList<RegistryObject<LTNTBlock>>());
				}
				TNTLists.TNTLists.get(modID).add(block);
			}
			if(blockData.addDispenserBehaviour()) {
				TNTLists.TNT_DISPENSER_REGISTRY_LIST.add(new Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>(block, item));
			}
		}
		return block;	
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(String registryName, RegistryObject<EntityType<LivingPrimedLTNT>> TNT, CreativeModeTab tab){
		return registerLivingTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, true);
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(String registryName, RegistryObject<EntityType<LivingPrimedLTNT>> TNT, CreativeModeTab tab, boolean shouldRandomlyFuse){
		return registerLivingTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, shouldRandomlyFuse);
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(String registryName, RegistryObject<EntityType<LivingPrimedLTNT>> TNT, CreativeModeTab tab, MaterialColor color, boolean shouldRandomlyFuse){
		return registerLivingTNTBlock(TNT, new TNTBlockRegistryData.Builder(registryName).tab(tab).color(color).shouldRandomlyFuse(shouldRandomlyFuse).build());
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(RegistryObject<EntityType<LivingPrimedLTNT>> TNT, TNTBlockRegistryData blockData){
		return registerLivingTNTBlock(blockRegistry, itemRegistry, () -> new LivingLTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, blockData.getColor()).sound(SoundType.GRASS), TNT, blockData.shouldRandomlyFuse()), blockData);
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, Supplier<LivingLTNTBlock> TNTBlock, TNTBlockRegistryData blockData){
		RegistryObject<LTNTBlock> block = blockRegistry.register(blockData.getRegistryName(), () -> ((LTNTBlock)TNTBlock.get()));
		if(itemRegistry != null && blockData.makeItem()) {
			RegistryObject<Item> item = itemRegistry.register(blockData.getRegistryName(), () -> new BlockItem(block.get(), new Item.Properties().tab(blockData.getTab())) {
				
				@Override
				public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
					super.appendHoverText(stack, level, components, flag);
					if(!blockData.getDescription().getKey().equals("")) {
						components.add(MutableComponent.create(blockData.getDescription()));
					}
				}
			});
			if(blockData.addToTNTLists()) {
				if(TNTLists.TNTLists.get(modID) == null) {
					TNTLists.TNTLists.put(modID, new ArrayList<RegistryObject<LTNTBlock>>());
				}
				TNTLists.TNTLists.get(modID).add(block);
			}
			if(blockData.addDispenserBehaviour()) {
				TNTLists.TNT_DISPENSER_REGISTRY_LIST.add(new Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>(block, item));
			}
		}
		return block;	
	}
	
	public RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, RegistryObject<EntityType<LExplosiveProjectile>> dynamite){
		return registerDynamiteItem(registryName, () -> new LDynamiteItem(new Item.Properties(), dynamite, true));
	}
	
	public RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, Supplier<LDynamiteItem> dynamiteSupplier){
		return registerDynamiteItem(registryName, dynamiteSupplier, true);
	}
	
	public RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, Supplier<LDynamiteItem> dynamiteSupplier, boolean addToLists){
		return registerDynamiteItem(itemRegistry, registryName, dynamiteSupplier, addToLists);
	}
	
	//Not fully done
	public RegistryObject<LDynamiteItem> registerDynamiteItem(DeferredRegister<Item> itemRegistry, String registryName, Supplier<LDynamiteItem> dynamiteSupplier, boolean addToLists){
		RegistryObject<LDynamiteItem> item = itemRegistry.register(registryName, dynamiteSupplier);		
		if(addToLists) {
			if(TNTLists.DynamiteLists.get("dynamites") == null) {
				TNTLists.DynamiteLists.put("dynamites", new ArrayList<RegistryObject<LDynamiteItem>>());
			}
			TNTLists.DynamiteLists.get("dynamites").add(item);
		}
		return item;
	}
	
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(String registryName, PrimedTNTEffect effect){
		return registerTNTEntity(registryName, effect, 1f, true);
	}
	
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(String registryName, PrimedTNTEffect effect, float size, boolean fireImmune){
		return registerTNTEntity(entityRegistry, registryName, effect, size, fireImmune);
	}
	
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(DeferredRegister<EntityType<?>> entityRegistry, String registryName, PrimedTNTEffect effect, float size, boolean fireImmune){
		if(fireImmune) {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<PrimedLTNT>of((EntityType<PrimedLTNT> type, Level level) -> new PrimedLTNT(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).fireImmune().sized(size, size).build(registryName));
		}
		else {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<PrimedLTNT>of((EntityType<PrimedLTNT> type, Level level) -> new PrimedLTNT(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(size, size).build(registryName));			
		}
	}
	
	public RegistryObject<EntityType<LivingPrimedLTNT>> registerLivingTNTEntity(String registryName, BiFunction<EntityType<LivingPrimedLTNT>, Level, LivingPrimedLTNT> TNT, float damage, float health, float speed, float size, boolean fireImmune){
		return registerLivingTNTEntity(entityRegistry, registryName, TNT, damage, health, speed, size, fireImmune);
	}
	
	public RegistryObject<EntityType<LivingPrimedLTNT>> registerLivingTNTEntity(DeferredRegister<EntityType<?>> entityRegistry, String registryName, BiFunction<EntityType<LivingPrimedLTNT>, Level, LivingPrimedLTNT> TNT, float damage, float health, float speed, float size, boolean fireImmune){
		if(fireImmune) {
			RegistryObject<EntityType<LivingPrimedLTNT>> register = entityRegistry.register(registryName, () -> EntityType.Builder.<LivingPrimedLTNT>of((EntityType<LivingPrimedLTNT> type, Level level) -> TNT.apply(type, level), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).fireImmune().sized(size, size).build(registryName));
			TNTLists.attributeRegistries.add(new Pair<RegistryObject<EntityType<LivingPrimedLTNT>>, float[]>(register, new float[] {damage, health, speed}));
			return register;
		}
		else {
			RegistryObject<EntityType<LivingPrimedLTNT>> register = entityRegistry.register(registryName, () -> EntityType.Builder.<LivingPrimedLTNT>of((EntityType<LivingPrimedLTNT> type, Level level) -> TNT.apply(type, level), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(size, size).build(registryName));
			TNTLists.attributeRegistries.add(new Pair<RegistryObject<EntityType<LivingPrimedLTNT>>, float[]>(register, new float[] {damage, health, speed}));
			return register;
		}
	}
	
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(String registryName, PrimedTNTEffect effect){
		return registerExplosiveProjectile(registryName, effect, 1, false);
	}
	
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(String registryName, PrimedTNTEffect effect, float size, boolean fireImmune) {
		return registerExplosiveProjectile(entityRegistry, registryName, effect, size, fireImmune);
	}
	
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(DeferredRegister<EntityType<?>> entityRegistry, String registryName, PrimedTNTEffect effect, float size, boolean fireImmune){
		if(fireImmune) {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<LExplosiveProjectile>of((EntityType<LExplosiveProjectile> type, Level level) -> new LExplosiveProjectile(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).fireImmune().sized(size, size).build(registryName));
		}
		else {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<LExplosiveProjectile>of((EntityType<LExplosiveProjectile> type, Level level) -> new LExplosiveProjectile(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(size, size).build(registryName));
		}
	}
}
