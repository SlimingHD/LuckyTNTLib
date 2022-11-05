package luckytntlib.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import luckytntlib.block.LTNTBlock;
import luckytntlib.entity.LExplosiveProjectile;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.item.LDynamiteItem;
import luckytntlib.util.explosions.ExplosiveProjectileEffect;
import luckytntlib.util.explosions.PrimedTNTEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
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
	
	public RegistryHelper(DeferredRegister<Block> blockRegistry, DeferredRegister<Item> itemRegistry, DeferredRegister<EntityType<?>> entityRegistry) {
		this.blockRegistry = blockRegistry;
		this.itemRegistry = itemRegistry;
		this.entityRegistry = entityRegistry;
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, CreativeModeTab tab){
		return registerTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED);
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, CreativeModeTab tab, MaterialColor color){
		return registerTNTBlock(registryName, TNT, tab, color, true);
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, CreativeModeTab tab, MaterialColor color, boolean addDispenserBehaviour){
		return registerTNTBlock(new TNTBlockItemRegistryData.Builder(registryName, () -> new LTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, color).sound(SoundType.GRASS), TNT)).tab(tab).addDispenserBehaviour(addDispenserBehaviour).build());
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(TNTBlockItemRegistryData blockData){
		return registerTNTBlock(blockRegistry, itemRegistry, blockData);
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(DeferredRegister<Block> blockRegistry, DeferredRegister<Item> itemRegistry, String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, CreativeModeTab tab, MaterialColor color){
		return registerTNTBlock(blockRegistry, itemRegistry, new TNTBlockItemRegistryData.Builder(registryName, () -> new LTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, color).sound(SoundType.GRASS), TNT)).tab(tab).build());
	}
	
	public RegistryObject<LTNTBlock> registerTNTBlock(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, TNTBlockItemRegistryData blockData){
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
				if(TNTLists.TNTLists.get(blockData.getTab().getRecipeFolderName()) == null) {
					TNTLists.TNTLists.put(blockData.getTab().getRecipeFolderName(), new ArrayList<RegistryObject<LTNTBlock>>());
				}
				TNTLists.TNTLists.get(blockData.getTab().getRecipeFolderName()).add(block);
			}
			if(blockData.addDispenserBehaviour()) {
				TNTLists.TNT_DISPENSER_REGISTRY_LIST.add(new Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>(block, item));
			}
		}
		return block;	
	}
	
	
	public RegistryObject<Block> registerBlockWithDesc(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, BlockItemRegistryData blockData){
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
	
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(String registryName, ExplosiveProjectileEffect effect){
		return registerExplosiveProjectile(registryName, effect, 1, false);
	}
	
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(String registryName, ExplosiveProjectileEffect effect, float size, boolean fireImmune) {
		return registerExplosiveProjectile(entityRegistry, registryName, effect, size, fireImmune);
	}
	
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(DeferredRegister<EntityType<?>> entityRegistry, String registryName, ExplosiveProjectileEffect effect, float size, boolean fireImmune){
		if(fireImmune) {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<LExplosiveProjectile>of((EntityType<LExplosiveProjectile> type, Level level) -> new LExplosiveProjectile(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).fireImmune().sized(size, size).build(registryName));
		}
		else {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<LExplosiveProjectile>of((EntityType<LExplosiveProjectile> type, Level level) -> new LExplosiveProjectile(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(size, size).build(registryName));
		}
	}
}
