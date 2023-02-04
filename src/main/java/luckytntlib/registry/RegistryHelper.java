package luckytntlib.registry;

import java.util.ArrayList;
import java.util.HashMap;
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
import luckytntlib.util.tnteffects.PrimedTNTEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
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
	
	public final HashMap<String, List<RegistryObject<LTNTBlock>>> TNTLists = new HashMap<>();
	public final HashMap<String, List<RegistryObject<LDynamiteItem>>> dynamiteLists = new HashMap<>();
	
	public final HashMap<String, List<RegistryObject<? extends Item>>> creativeTabItemLists = new HashMap<>();
	
	public static final List<Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>> TNT_DISPENSER_REGISTRY_LIST = new ArrayList<>();
	public static final List<RegistryObject<LDynamiteItem>> DYNAMITE_DISPENSER_REGISTRY_LIST = new ArrayList<>();
	
	public static final List<Pair<RegistryObject<EntityType<LivingPrimedLTNT>>, float[]>> ATTRIBUTE_REGISTRY_LIST = new ArrayList<Pair<RegistryObject<EntityType<LivingPrimedLTNT>>, float[]>>();
	
	public RegistryHelper(DeferredRegister<Block> blockRegistry, DeferredRegister<Item> itemRegistry, DeferredRegister<EntityType<?>> entityRegistry) {
		this.blockRegistry = blockRegistry;
		this.itemRegistry = itemRegistry;
		this.entityRegistry = entityRegistry;
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, String tab){
		return registerTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, true);
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, String tab, boolean shouldRandomlyFuse){
		return registerTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, shouldRandomlyFuse);
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, String tab, MaterialColor color, boolean shouldRandomlyFuse){
		return registerTNTBlock(TNT, new TNTBlockRegistryData.Builder(registryName).tab(tab).color(color).shouldRandomlyFuse(shouldRandomlyFuse).build());
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(RegistryObject<EntityType<PrimedLTNT>> TNT, TNTBlockRegistryData blockData){
		return registerTNTBlock(blockRegistry, itemRegistry, () -> new LTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, blockData.getColor()).sound(SoundType.GRASS), TNT, blockData.shouldRandomlyFuse()), blockData);
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, Supplier<LTNTBlock> TNTBlock, TNTBlockRegistryData blockData){
		RegistryObject<LTNTBlock> block = blockRegistry.register(blockData.getRegistryName(), TNTBlock);
		if(itemRegistry != null && blockData.makeItem()) {
			RegistryObject<Item> item = itemRegistry.register(blockData.getRegistryName(), () -> new BlockItem(block.get(), new Item.Properties()) {
				
				@Override
				public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
					super.appendHoverText(stack, level, components, flag);
					if(!blockData.getDescription().getKey().equals("")) {
						components.add(MutableComponent.create(blockData.getDescription()));
					}
				}
			});
			
			if(blockData.addToTNTLists()) {
				if(TNTLists.get(blockData.getTab()) == null) {
					TNTLists.put(blockData.getTab(), new ArrayList<RegistryObject<LTNTBlock>>());
				}
				TNTLists.get(blockData.getTab()).add(block);
			}
			if(blockData.addDispenserBehaviour()) {
				TNT_DISPENSER_REGISTRY_LIST.add(new Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>(block, item));
			}
			if(!blockData.getTab().equals("none")) {
				if(creativeTabItemLists.get(blockData.getTab()) == null) {
					creativeTabItemLists.put(blockData.getTab(), new ArrayList<RegistryObject<? extends Item>>());
				}
				creativeTabItemLists.get(blockData.getTab()).add(item);				
			}
		}
		return block;	
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(String registryName, RegistryObject<EntityType<LivingPrimedLTNT>> TNT, String tab){
		return registerLivingTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, true);
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(String registryName, RegistryObject<EntityType<LivingPrimedLTNT>> TNT, String tab, boolean shouldRandomlyFuse){
		return registerLivingTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, shouldRandomlyFuse);
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(String registryName, RegistryObject<EntityType<LivingPrimedLTNT>> TNT, String tab, MaterialColor color, boolean shouldRandomlyFuse){
		return registerLivingTNTBlock(TNT, new TNTBlockRegistryData.Builder(registryName).tab(tab).color(color).shouldRandomlyFuse(shouldRandomlyFuse).build());
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(RegistryObject<EntityType<LivingPrimedLTNT>> TNT, TNTBlockRegistryData blockData){
		return registerLivingTNTBlock(blockRegistry, itemRegistry, () -> new LivingLTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, blockData.getColor()).sound(SoundType.GRASS), TNT, blockData.shouldRandomlyFuse()), blockData);
	}
	
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, Supplier<LivingLTNTBlock> TNTBlock, TNTBlockRegistryData blockData){
		RegistryObject<LTNTBlock> block = blockRegistry.register(blockData.getRegistryName(), () -> ((LTNTBlock)TNTBlock.get()));
		if(itemRegistry != null && blockData.makeItem()) {
			RegistryObject<Item> item = itemRegistry.register(blockData.getRegistryName(), () -> new BlockItem(block.get(), new Item.Properties()) {
				
				@Override
				public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
					super.appendHoverText(stack, level, components, flag);
					if(!blockData.getDescription().getKey().equals("")) {
						components.add(MutableComponent.create(blockData.getDescription()));
					}
				}
			});
			if(blockData.addToTNTLists()) {
				if(TNTLists.get(blockData.getTab()) == null) {
					TNTLists.put(blockData.getTab(), new ArrayList<RegistryObject<LTNTBlock>>());
				}
				TNTLists.get(blockData.getTab()).add(block);
			}
			if(blockData.addDispenserBehaviour()) {
				TNT_DISPENSER_REGISTRY_LIST.add(new Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>(block, item));
			}
			if(!blockData.getTab().equals("none")) {
				if(creativeTabItemLists.get(blockData.getTab()) == null) {
					creativeTabItemLists.put(blockData.getTab(), new ArrayList<RegistryObject<? extends Item>>());
				}
				creativeTabItemLists.get(blockData.getTab()).add(item);				
			}
		}
		return block;	
	}
	
	public RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, RegistryObject<EntityType<LExplosiveProjectile>> dynamite, String tab){
		return registerDynamiteItem(registryName, () -> new LDynamiteItem(new Item.Properties(), dynamite, true), tab);
	}
	
	public RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, Supplier<LDynamiteItem> dynamiteSupplier, String tab){
		return registerDynamiteItem(registryName, dynamiteSupplier, tab, true, true);
	}
	
	public RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, Supplier<LDynamiteItem> dynamiteSupplier, String tab, boolean addToLists, boolean addDispenserBehaviour){
		return registerDynamiteItem(itemRegistry, registryName, dynamiteSupplier, tab, addToLists, addDispenserBehaviour);
	}
	
	//Not fully done
	public RegistryObject<LDynamiteItem> registerDynamiteItem(DeferredRegister<Item> itemRegistry, String registryName, Supplier<LDynamiteItem> dynamiteSupplier, String tab, boolean addToLists, boolean addDispenserBehaviour){
		RegistryObject<LDynamiteItem> item = itemRegistry.register(registryName, dynamiteSupplier);		
		if(addToLists) {
			if(dynamiteLists.get(tab) == null) {
				dynamiteLists.put(tab, new ArrayList<RegistryObject<LDynamiteItem>>());
			}
			dynamiteLists.get(tab).add(item);
		}
		if(addDispenserBehaviour) {
			DYNAMITE_DISPENSER_REGISTRY_LIST.add(item);
		}
		if(!tab.equals("none")) {
			if(creativeTabItemLists.get(tab) == null) {
				creativeTabItemLists.put(tab, new ArrayList<RegistryObject<? extends Item>>());
			}
			creativeTabItemLists.get(tab).add(item);
		}
		return item;
	}
	
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(String registryName, PrimedTNTEffect effect){
		return registerTNTEntity(registryName, effect, 1f, true);
	}
	
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(String registryName, PrimedTNTEffect effect, float size, boolean fireImmune){
		return registerTNTEntity(entityRegistry, registryName, effect, size, fireImmune);
	}
	
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(DeferredRegister<EntityType<?>> entityRegistry, String registryName, Supplier<EntityType<PrimedLTNT>> TNT){
		return entityRegistry.register(registryName, TNT);
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
			ATTRIBUTE_REGISTRY_LIST.add(new Pair<RegistryObject<EntityType<LivingPrimedLTNT>>, float[]>(register, new float[] {damage, health, speed}));
			return register;
		}
		else {
			RegistryObject<EntityType<LivingPrimedLTNT>> register = entityRegistry.register(registryName, () -> EntityType.Builder.<LivingPrimedLTNT>of((EntityType<LivingPrimedLTNT> type, Level level) -> TNT.apply(type, level), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(size, size).build(registryName));
			ATTRIBUTE_REGISTRY_LIST.add(new Pair<RegistryObject<EntityType<LivingPrimedLTNT>>, float[]>(register, new float[] {damage, health, speed}));
			return register;
		}
	}
	
	public RegistryObject<EntityType<LivingPrimedLTNT>> registerLivingTNTEntity(DeferredRegister<EntityType<?>> entityRegistry, String registryName, Supplier<EntityType<LivingPrimedLTNT>> TNT){
		return entityRegistry.register(registryName, TNT);
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
