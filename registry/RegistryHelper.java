package luckytntlib.registry;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import luckytntlib.LuckyTNTLib;
import luckytntlib.block.LTNTBlock;
import luckytntlib.entity.LDynamite;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.item.LDynamiteItem;
import luckytntlib.util.DynamiteEffect;
import luckytntlib.util.PrimedTNTEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
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
	
	public static RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, CreativeModeTab tab){
		return registerTNTBlock(new TNTBlockItemRegistryData.Builder(registryName, () -> new LTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, MaterialColor.COLOR_RED).sound(SoundType.GRASS), TNT)).tab(tab).build());
	}
	
	public static RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, CreativeModeTab tab, MaterialColor color){
		return registerTNTBlock(new TNTBlockItemRegistryData.Builder(registryName, () -> new LTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, color).sound(SoundType.GRASS), TNT)).tab(tab).build());
	}
	
	public static RegistryObject<LTNTBlock> registerTNTBlock(TNTBlockItemRegistryData blockData){
		return registerTNTBlock(LuckyTNTLib.TNT_BLOCKS, LuckyTNTLib.TNT_ITEMS, blockData);
	}
	
	public static RegistryObject<LTNTBlock> registerTNTBlock(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, TNTBlockItemRegistryData blockData){
		RegistryObject<LTNTBlock> block = blockRegistry.register(blockData.getRegistryName(), (Supplier<LTNTBlock>)blockData.getBlock());
		if(blockData.makeItem() && itemRegistry != null) {
			RegistryObject<Item> item = itemRegistry.register(blockData.getRegistryName(), () -> new BlockItem(block.get(), new Item.Properties().tab(blockData.getTab())) {		
				@Override
				public void appendHoverText(ItemStack stack, Level level, List<Component> text, TooltipFlag flag) {
					super.appendHoverText(stack, level, text, flag);
					if(!blockData.getDescription().equals("")) {						
						text.add(MutableComponent.create(new TranslatableContents(blockData.getDescription())));
					}
				}
			});
			if(blockData.addToTNTLists()) {
				//To do: add to lists
			}
			if(blockData.addDispenserBehaviour()) {
				TNTLists.TNT_DISPENSER_REGISTRY_LIST.add(new Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>(block, item));
			}
		}
		return block;	
	}
	
	
	public static RegistryObject<Block> registerBlockWithDesc(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, BlockItemRegistryData blockData){
		RegistryObject<Block> block = blockRegistry.register(blockData.getRegistryName(), blockData.getBlock());
		if(blockData.makeItem() && itemRegistry != null){
			itemRegistry.register(blockData.getRegistryName(), () -> new BlockItem(block.get(), new Item.Properties().tab(blockData.getTab())) {		
				@Override
				public void appendHoverText(ItemStack stack, Level level, List<Component> text, TooltipFlag flag) {
					super.appendHoverText(stack, level, text, flag);
					if(!blockData.getDescription().equals("")) {
						text.add(MutableComponent.create(new TranslatableContents(blockData.getDescription())));
					}
				}
			});
		}
		return block;
	}
	
	public static RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, RegistryObject<EntityType<LDynamite>> dynamite){
		return registerDynamiteItem(registryName, () -> new LDynamiteItem(new Item.Properties(), dynamite, true));
	}
	
	public static RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, Supplier<LDynamiteItem> dynamiteSupplier){
		return registerDynamiteItem(registryName, dynamiteSupplier, true);
	}
	
	public static RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, Supplier<LDynamiteItem> dynamiteSupplier, boolean addToLists){
		return registerDynamiteItem(LuckyTNTLib.DYNAMITE_ITEMS, registryName, dynamiteSupplier, addToLists);
	}
	
	public static RegistryObject<LDynamiteItem> registerDynamiteItem(DeferredRegister<Item> itemRegistry, String registryName, Supplier<LDynamiteItem> dynamiteSupplier, boolean addToLists){
		RegistryObject<LDynamiteItem> item = itemRegistry.register(registryName, dynamiteSupplier);		
		if(addToLists) {
			TNTLists.DYNAMITE.add(item);
		}
		return item;
	}
	
	public static RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(String registryName, PrimedTNTEffect effect){
		return registerTNTEntity(registryName, effect, 1f, true);
	}
	
	public static RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(String registryName, PrimedTNTEffect effect, float size, boolean fireImmune){
		return registerTNTEntity(LuckyTNTLib.TNT_ENTITIES, registryName, effect, size, fireImmune);
	}
	
	public static RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(DeferredRegister<EntityType<?>> entityRegistry, String registryName, PrimedTNTEffect effect, float size, boolean fireImmune){
		if(fireImmune) {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<PrimedLTNT>of((EntityType<PrimedLTNT> type, Level level) -> new PrimedLTNT(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).fireImmune().sized(size, size).build(registryName));
		}
		else {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<PrimedLTNT>of((EntityType<PrimedLTNT> type, Level level) -> new PrimedLTNT(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(size, size).build(registryName));			
		}
	}
	
	public static RegistryObject<EntityType<LDynamite>> registerDynamiteProjectile(DeferredRegister<EntityType<?>> entityRegistry, String registryName, RegistryObject<EntityType<LDynamite>> entity, DynamiteEffect effect, float size, boolean fireImmune){
		if(fireImmune) {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<LDynamite>of((EntityType<LDynamite> type, Level level) -> new LDynamite(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).fireImmune().sized(size, size).build(registryName));
		}
		else {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<LDynamite>of((EntityType<LDynamite> type, Level level) -> new LDynamite(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(size, size).build(registryName));
		}
	}
	
	public static RegistryObject<Item> registerSimpleItem(DeferredRegister<Item> itemRegistry, Supplier<Item> itemSupplier, String registryName){
		return itemRegistry.register(registryName, itemSupplier);
	}
	
	public static RegistryObject<Block> registerSimpleBlock(DeferredRegister<Block> blockRegistry, Supplier<Block> blockSupplier, String registryName){
		return blockRegistry.register(registryName, blockSupplier);
	}
	
	public static<T extends Entity> RegistryObject<EntityType<T>> registerSimpleEntity(DeferredRegister<EntityType<?>> entityRegistry, String registryName, EntityType.Builder<T> builder){
		return entityRegistry.register(registryName, () -> builder.build(registryName));
	}
}
