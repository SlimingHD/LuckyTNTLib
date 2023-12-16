package luckytntlib.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;

import luckytntlib.block.LTNTBlock;
import luckytntlib.block.LivingLTNTBlock;
import luckytntlib.block.LuckyTNTBlock;
import luckytntlib.entity.LExplosiveProjectile;
import luckytntlib.entity.LTNTMinecart;
import luckytntlib.entity.LivingPrimedLTNT;
import luckytntlib.entity.LuckyTNTMinecart;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.item.LDynamiteItem;
import luckytntlib.item.LTNTMinecartItem;
import luckytntlib.item.LuckyDynamiteItem;
import luckytntlib.util.tnteffects.PrimedTNTEffect;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.chat.Component;
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

/**
 * 
 * The RegistryHelper offers many methods with varying complexity for each important part of the TNT/Dynamite/TNT Minecart registering 
 * and even allows easy registration of {@link DispenseItemBehavior} where possible.
 * On top of all this it also saves List of TNT, dynamite and minecarts in a {@link HashMap} with the corresponding string assigned while registering.
 * These lists can be used to simply add the items into a tab or pass the whole list to the {@link LuckyTNTBlock}, {@link LuckyDynamiteItem} and {@link LuckyTNTMinecart} respectively.
 */
public class RegistryHelper {
	
	private final DeferredRegister<Block> blockRegistry;
	private final DeferredRegister<Item> itemRegistry;
	private final DeferredRegister<EntityType<?>> entityRegistry;
	
	/**
	 * {@link HashMap}, with strings as keys, of Lists of all registered TNT blocks.
	 * The key is in this case the variable 'tab' in the individual register method
	 */
	public final HashMap<String, List<RegistryObject<LTNTBlock>>> TNTLists = new HashMap<>();
	/**
	 * {@link HashMap}, with strings as keys, of Lists of all registered dynamite items.
	 * The key is in this case the variable 'tab' in the individual register method
	 */
	public final HashMap<String, List<RegistryObject<LDynamiteItem>>> dynamiteLists = new HashMap<>();
	/**
	 * {@link HashMap}, with strings as keys, of Lists of all registered TNT minecart items.
	 * The key is in this case the variable 'tab' in the individual register method
	 */
	public final HashMap<String, List<RegistryObject<LTNTMinecartItem>>> minecartLists = new HashMap<>();
	
	/**
	 * {@link HashMap}, with strings as keys, of Lists of all registered items, if the strings passed were not 'none'.
	 * The key is in this case the variable 'tab' in the individual register method
	 */
	public final HashMap<String, List<RegistryObject<? extends Item>>> creativeTabItemLists = new HashMap<>();
	
	/**
	 * List of pairs of {@link LTNTBlock} that get a {@link DispenseItemBehavior} automatically registered 
	 */
	public static final List<Pair<RegistryObject<LTNTBlock>, RegistryObject<Item>>> TNT_DISPENSER_REGISTRY_LIST = new ArrayList<>();
	/**
	 * List of {@link LDynamiteItem} that get a {@link DispenseItemBehavior} automatically registered
	 */
	public static final List<RegistryObject<LDynamiteItem>> DYNAMITE_DISPENSER_REGISTRY_LIST = new ArrayList<>();
	/**
	 * List of {@link LTNTMinecartItem} that get a {@link DispenseItemBehavior} automatically registered
	 */
	public static final List<RegistryObject<LTNTMinecartItem>> MINECART_DISPENSER_REGISTRY_LIST = new ArrayList<>();
	
	/**
	 * Creates a new instance of the RegistryHelper
	 * @param blockRegistry  the {@link DeferredRegister} in which all blocks get registered if not stated otherwise
	 * @param itemRegistry  the {@link DeferredRegister} in which all items get registered if not stated otherwise
	 * @param entityRegistry  the {@link DeferredRegister} in which all entities get registered if not stated otherwise
	 */
	public RegistryHelper(DeferredRegister<Block> blockRegistry, DeferredRegister<Item> itemRegistry, DeferredRegister<EntityType<?>> entityRegistry) {
		this.blockRegistry = blockRegistry;
		this.itemRegistry = itemRegistry;
		this.entityRegistry = entityRegistry;
	}
	
	/**
	 * Registers a new {@link LTNTBlock}
	 * @param registryName  the registry name of this TNT (for block and for item)
	 * @param TNT  the {@link PrimedLTNT} that is passed to this block and spawned when the block is ignited
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#TNTLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, String tab){
		return registerTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, true);
	}
	
	/**
	 * Registers a new {@link LTNTBlock}
	 * @param registryName  the registry name of this TNT (for block and for item)
	 * @param TNT  the {@link PrimedLTNT} that is passed to this block and spawned when the block is ignited
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#TNTLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @param randomizedFuseUponExploded  whether or not the TNT should have a random fuse based upon the default fuse when removed by another explosion
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, String tab, boolean randomizedFuseUponExploded){
		return registerTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, randomizedFuseUponExploded);
	}
	
	/**
	 * Registers a new {@link LTNTBlock}
	 * @param registryName  the registry name of this TNT (for block and for item)
	 * @param TNT  the {@link PrimedLTNT} that is passed to this block and spawned when the block is ignited
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#TNTLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @param color  the color the block will have on the map
	 * @param randomizedFuseUponExploded  whether or not the TNT should have a random fuse based upon the default fuse when removed by another explosion
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerTNTBlock(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, String tab, MaterialColor color, boolean randomizedFuseUponExploded){
		return registerTNTBlock(TNT, new TNTBlockRegistryData.Builder(registryName).tab(tab).color(color).randomizedFuseUponExploded(randomizedFuseUponExploded).build());
	}
	
	/**
	 * Registers a new {@link LTNTBlock}
	 * @param TNT  the {@link PrimedLTNT} that is passed to this block and spawned when the block is ignited
	 * @param blockData  all the information that a TNT block may need, e.g. registry name and color, contained in an object
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerTNTBlock(RegistryObject<EntityType<PrimedLTNT>> TNT, TNTBlockRegistryData blockData){
		return registerTNTBlock(blockRegistry, itemRegistry, () -> new LTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, blockData.getColor()).sound(SoundType.GRASS), TNT, blockData.randomizedFuseUponExploded()), blockData);
	}
	
	/**
	 * Registers a new {@link LTNTBlock}
	 * @param blockRegistry  the registry in which the block is being registered into
	 * @param itemRegistry  the registry in which the block item is being registered into
	 * @param TNTBlock  the TNT block that is being registered
	 * @param blockData  all the information that a TNT block may need, e.g. registry name and color, contained in an object
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerTNTBlock(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, Supplier<LTNTBlock> TNTBlock, TNTBlockRegistryData blockData){
		RegistryObject<LTNTBlock> block = blockRegistry.register(blockData.getRegistryName(), TNTBlock);
		if(itemRegistry != null && blockData.makeItem()) {
			RegistryObject<Item> item = itemRegistry.register(blockData.getRegistryName(), () -> new BlockItem(block.get(), new Item.Properties()) {
				
				@Override
				public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
					super.appendHoverText(stack, level, components, flag);
					if(!blockData.getDescription().getString().equals("")) {
						components.add(blockData.getDescription());
					}
				}
			});
			
			if(blockData.addToTNTLists()) {
				if(TNTLists.get(blockData.getTab()) == null) {
					TNTLists.put(blockData.getTab(), new ArrayList<RegistryObject<LTNTBlock>>());
				}
				TNTLists.get(blockData.getTab()).add(block);
			}
			if(blockData.addDispenseBehavior()) {
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
	
	/**
	 * Registers a new {@link LivingLTNTBlock}
	 * @param registryName  the registry name of this TNT (for block and for item)
	 * @param TNT  the {@link LivingPrimedLTNT} that is passed to this block and spawned when the block is ignited
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#TNTLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(String registryName, RegistryObject<EntityType<LivingPrimedLTNT>> TNT, String tab){
		return registerLivingTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, true);
	}
	
	/**
	 * Registers a new {@link LivingLTNTBlock}
	 * @param registryName  the registry name of this TNT (for block and for item)
	 * @param TNT  the {@link LivingPrimedLTNT} that is passed to this block and spawned when the block is ignited
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#TNTLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @param randomizedFuseUponExploded  whether or not the TNT should have a random fuse based upon the default fuse when removed by another explosion
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(String registryName, RegistryObject<EntityType<LivingPrimedLTNT>> TNT, String tab, boolean randomizedFuseUponExploded){
		return registerLivingTNTBlock(registryName, TNT, tab, MaterialColor.COLOR_RED, randomizedFuseUponExploded);
	}
	
	/**
	 * Registers a new {@link LivingLTNTBlock}
	 * @param registryName  the registry name of this TNT (for block and for item)
	 * @param TNT  the {@link LivingPrimedLTNT} that is passed to this block and spawned when the block is ignited
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#TNTLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @param color  the color the block will have on the map
	 * @param randomizedFuseUponExploded  whether or not the TNT should have a random fuse based upon the default fuse when removed by another explosion
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(String registryName, RegistryObject<EntityType<LivingPrimedLTNT>> TNT, String tab, MaterialColor color, boolean randomizedFuseUponExploded){
		return registerLivingTNTBlock(TNT, new TNTBlockRegistryData.Builder(registryName).tab(tab).color(color).randomizedFuseUponExploded(randomizedFuseUponExploded).build());
	}
	
	/**
	 * Registers a new {@link LivingLTNTBlock}
	 * @param TNT  the {@link LivingPrimedLTNT} that is passed to this block and spawned when the block is ignited
	 * @param blockData  all the information that a TNT block may need, e.g. registry name and color, contained in an object
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(RegistryObject<EntityType<LivingPrimedLTNT>> TNT, TNTBlockRegistryData blockData){
		return registerLivingTNTBlock(blockRegistry, itemRegistry, () -> new LivingLTNTBlock(BlockBehaviour.Properties.of(Material.EXPLOSIVE, blockData.getColor()).sound(SoundType.GRASS), TNT, blockData.randomizedFuseUponExploded()), blockData);
	}
	
	/**
	 * Registers a new {@link LivingLTNTBlock}
	 * @param blockRegistry  the registry in which the block is being registered into
	 * @param itemRegistry  the registry in which the block item is being registered into
	 * @param TNTBlock  the living TNT block that is being registered
	 * @param blockData  all the information that a TNT block may need, e.g. registry name and color, contained in an object
	 * @return {@link RegistryObject} of a {@link LTNTBlock}
	 */
	public RegistryObject<LTNTBlock> registerLivingTNTBlock(DeferredRegister<Block> blockRegistry, @Nullable DeferredRegister<Item> itemRegistry, Supplier<LivingLTNTBlock> TNTBlock, TNTBlockRegistryData blockData){
		RegistryObject<LTNTBlock> block = blockRegistry.register(blockData.getRegistryName(), () -> ((LTNTBlock)TNTBlock.get()));
		if(itemRegistry != null && blockData.makeItem()) {
			RegistryObject<Item> item = itemRegistry.register(blockData.getRegistryName(), () -> new BlockItem(block.get(), new Item.Properties()) {
				
				@Override
				public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
					super.appendHoverText(stack, level, components, flag);
					if(!blockData.getDescription().getString().equals("")) {
						components.add(blockData.getDescription());
					}
				}
			});
			if(blockData.addToTNTLists()) {
				if(TNTLists.get(blockData.getTab()) == null) {
					TNTLists.put(blockData.getTab(), new ArrayList<RegistryObject<LTNTBlock>>());
				}
				TNTLists.get(blockData.getTab()).add(block);
			}
			if(blockData.addDispenseBehavior()) {
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
	
	/**
	 * Registers a new {@link LDynamiteItem}
	 * @param registryName  the registry name of this dynamite item
	 * @param dynamiteSupplier  the dynamite item which is being registered
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#dynamiteLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @return {@link RegistryObject} of a {@link LDynamiteItem}
	 */
	public RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, Supplier<LDynamiteItem> dynamiteSupplier, String tab){
		return registerDynamiteItem(registryName, dynamiteSupplier, tab, true, true);
	}
	
	/**
	 * Registers a new {@link LDynamiteItem}
	 * @param registryName  the registry name of this dynamite item
	 * @param dynamiteSupplier  the dynamite item which is being registered
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#dynamiteLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @param addToLists  whether or not this dynamite should be added to {@link RegistryHelper#dynamiteLists} or not
	 * @param addDispenseBehavior  whether or not a {@link DispenseItemBehavior} should be registered or not
	 * @return {@link RegistryObject} of a {@link LDynamiteItem}
	 */
	public RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, Supplier<LDynamiteItem> dynamiteSupplier, String tab, boolean addToLists, boolean addDispenseBehavior){
		return registerDynamiteItem(itemRegistry, registryName, dynamiteSupplier, tab, addToLists, addDispenseBehavior);
	}
	
	/**
	 * Registers a new {@link LDynamiteItem}
	 * @param registryName  the registry name of this dynamite item
	 * @param dynamite  the {@link LExplosiveProjectile} that is passed to this item and thrown upon right clicking the item
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#dynamiteLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @return {@link RegistryObject} of a {@link LDynamiteItem}
	 */
	public RegistryObject<LDynamiteItem> registerDynamiteItem(String registryName, RegistryObject<EntityType<LExplosiveProjectile>> dynamite, String tab){
		return registerDynamiteItem(registryName, () -> new LDynamiteItem(new Item.Properties(), dynamite), tab);
	}
	
	/**
	 * Registers a new {@link LDynamiteItem}
	 * @param itemRegistry  the registry in which this dynamite is being registered into
	 * @param registryName  the registry name of this dynamite item
	 * @param dynamiteSupplier  the dynamite item which is being registered
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#dynamiteLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @param addToLists  whether or not this dynamite should be added to {@link RegistryHelper#dynamiteLists} or not
	 * @param addDispenseBehavior  whether or not a {@link DispenseItemBehavior} should be registered or not
	 * @return {@link RegistryObject} of a {@link LDynamiteItem}
	 */
	public RegistryObject<LDynamiteItem> registerDynamiteItem(DeferredRegister<Item> itemRegistry, String registryName, Supplier<LDynamiteItem> dynamiteSupplier, String tab, boolean addToLists, boolean addDispenseBehavior){
		RegistryObject<LDynamiteItem> item = itemRegistry.register(registryName, dynamiteSupplier);		
		if(addToLists) {
			if(dynamiteLists.get(tab) == null) {
				dynamiteLists.put(tab, new ArrayList<RegistryObject<LDynamiteItem>>());
			}
			dynamiteLists.get(tab).add(item);
		}
		if(addDispenseBehavior) {
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
	
	/**
	 * Registers a new {@link LTNTMinecart}
	 * @param registryName  the registry name of this minecart item
	 * @param TNT  the {@link LTNTMinecart} that is passed to this item and thrown
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#minecartLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @return {@link RegistryObject} of a {@link LTNTMinecartItem}
	 */
	public RegistryObject<LTNTMinecartItem> registerTNTMinecartItem(String registryName, Supplier<RegistryObject<EntityType<LTNTMinecart>>> TNT, String tab){
		return registerTNTMinecartItem(registryName, () -> new LTNTMinecartItem(new Item.Properties().stacksTo(1), TNT), tab, true, true);
	}
	
	/**
	 * Registers a new {@link LTNTMinecart}
	 * @param registryName  the registry name of this minecart item
	 * @param TNT  the {@link LTNTMinecart} that is passed to this item and thrown
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#minecartLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @param addToLists  whether or not this minecart should be added to {@link RegistryHelper#dynamiteLists} or not
	 * @param addDispenseBehavior  whether or not a {@link DispenseItemBehavior} should be registered or not
	 * @return {@link RegistryObject} of a {@link LTNTMinecartItem}
	 */
	public RegistryObject<LTNTMinecartItem> registerTNTMinecartItem(String registryName, Supplier<LTNTMinecartItem> minecartSupplier, String tab, boolean addToLists, boolean addDispenseBehavior){
		return registerTNTMinecartItem(itemRegistry, registryName, minecartSupplier, tab, addToLists, addDispenseBehavior);
	}
	
	/**
	 * Registers a new {@link LTNTMinecart}
	 * @param itemRegistry  the registry in which this minecart is being registered into
	 * @param registryName  the registry name of this minecart item
	 * @param TNT  the {@link LTNTMinecart} that is passed to this item and thrown
	 * @param tab  the string which is passed as a key to {@link RegistryHelper#minecartLists} and {@link RegistryHelper#creativeTabItemLists}
	 * @param addToLists  whether or not this minecart should be added to {@link RegistryHelper#dynamiteLists} or not
	 * @param addDispenseBehavior  whether or not a {@link DispenseItemBehavior} should be registered or not
	 * @return {@link RegistryObject} of a {@link LTNTMinecartItem}
	 */
	public RegistryObject<LTNTMinecartItem> registerTNTMinecartItem(DeferredRegister<Item> itemRegistry, String registryName, Supplier<LTNTMinecartItem> minecartSupplier, String tab, boolean addToLists, boolean addDispenseBehavior){
		RegistryObject<LTNTMinecartItem> item = itemRegistry.register(registryName, minecartSupplier);
		if(addToLists) {
			if(minecartLists.get(tab) == null) {
				minecartLists.put(tab, new ArrayList<RegistryObject<LTNTMinecartItem>>());
			}
			minecartLists.get(tab).add(item);
		}
		if(addDispenseBehavior) {
			MINECART_DISPENSER_REGISTRY_LIST.add(item);
		}
		if(!tab.equals("none")) {
			if(creativeTabItemLists.get(tab) == null) {
				creativeTabItemLists.put(tab, new ArrayList<RegistryObject<? extends Item>>());
			}
			creativeTabItemLists.get(tab).add(item);
		}
		return item;
	}
	
	/**
	 * Registers a new {@link PrimedLTNT}
	 * @param registryName  the registry name of this primed TNT
	 * @param effect  the TNT effect this primed TNT will have
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link PrimedLTNT}
	 */
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(String registryName, PrimedTNTEffect effect){
		return registerTNTEntity(registryName, effect, 1f, true);
	}
	
	/**
	 * Registers a new {@link PrimedLTNT}
	 * @param registryName  the registry name of this primed TNT
	 * @param effect  the TNT effect this primed TNT will have
	 * @param size  the size of the hitbox of this primed TNT
	 * @param fireImmune whether or not this primed TNT can burn (visual only)
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link PrimedLTNT}
	 */
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(String registryName, PrimedTNTEffect effect, float size, boolean fireImmune){
		return registerTNTEntity(entityRegistry, registryName, effect, size, fireImmune);
	}
	
	/**
	 * Registers a new {@link PrimedLTNT}
	 * @param entityRegistry  the registry in which this primed TNT is being registered into
	 * @param registryName  the registry name of this primed TNT
	 * @param effect  the TNT effect this primed TNT will have
	 * @param size  the size of the hitbox of this primed TNT
	 * @param fireImmune whether or not this primed TNT can burn (visual only)
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link PrimedLTNT}
	 */
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(DeferredRegister<EntityType<?>> entityRegistry, String registryName, PrimedTNTEffect effect, float size, boolean fireImmune){
		if(fireImmune) {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<PrimedLTNT>of((EntityType<PrimedLTNT> type, Level level) -> new PrimedLTNT(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).fireImmune().sized(size, size).build(registryName));
		}
		else {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<PrimedLTNT>of((EntityType<PrimedLTNT> type, Level level) -> new PrimedLTNT(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(size, size).build(registryName));			
		}
	}
		
	/**
	 * Registers a new {@link PrimedLTNT}
	 * @param entityRegistry  the registry in which this primed TNT is being registered into
	 * @param registryName  the registry name of this primed TNT
	 * @param TNT  the primed TNT that is being registered
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link PrimedLTNT}
	 */
	public RegistryObject<EntityType<PrimedLTNT>> registerTNTEntity(DeferredRegister<EntityType<?>> entityRegistry, String registryName, Supplier<EntityType<PrimedLTNT>> TNT){
		return entityRegistry.register(registryName, TNT);
	}
	
	/**
	 * Registers a new {@link LTNTMinecart}
	 * @param registryName  the registry name of this minecart
	 * @param TNT  the {@link PrimedLTNT} that passes the TNT effect over to this minecart
	 * @param pickItem  the minecart item that is gotten when this minecart is middle-clicked
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LTNTMinecart}
	 */
	public RegistryObject<EntityType<LTNTMinecart>> registerTNTMinecart(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, Supplier<RegistryObject<LTNTMinecartItem>> pickItem){
		return registerTNTMinecart(registryName, TNT, pickItem, true);
	}
	
	/**
	 * Registers a new {@link LTNTMinecart}
	 * @param registryName  the registry name of this minecart
	 * @param TNT  the {@link PrimedLTNT} that passes the TNT effect over to this minecart
	 * @param pickItem  the minecart item that is gotten when this minecart is middle-clicked
	 * @param explodesInstantly  whether or not this minecart will fuse or explode immediately
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LTNTMinecart}
	 */
	public RegistryObject<EntityType<LTNTMinecart>> registerTNTMinecart(String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, Supplier<RegistryObject<LTNTMinecartItem>> pickItem, boolean explodesInstantly){
		return registerTNTMinecart(entityRegistry, registryName, TNT, pickItem, explodesInstantly);
	}
	
	/**
	 * Registers a new {@link LTNTMinecart}
	 * @param entityRegistry  the registry in which this minecart is being registered into
	 * @param registryName  the registry name of this minecart
	 * @param TNT  the {@link PrimedLTNT} that passes the TNT effect over to this minecart
	 * @param pickItem  the minecart item that is gotten when this minecart is middle-clicked
	 * @param explodesInstantly  whether or not this minecart will fuse or explode immediately
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LTNTMinecart}
	 */
	public RegistryObject<EntityType<LTNTMinecart>> registerTNTMinecart(DeferredRegister<EntityType<?>> entityRegistry, String registryName, RegistryObject<EntityType<PrimedLTNT>> TNT, Supplier<RegistryObject<LTNTMinecartItem>> pickItem, boolean explodesInstantly){		
		return entityRegistry.register(registryName, () -> EntityType.Builder.<LTNTMinecart>of((EntityType<LTNTMinecart> type, Level level) -> new LTNTMinecart(type, level, TNT, pickItem, explodesInstantly), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(0.98f, 0.7f).build(registryName));
	}
	
	/**
	 * Registers a new {@link LTNTMinecart}
	 * @param entityRegistry  the registry in which this minecart is being registered into
	 * @param registryName  the registry name of this minecart
	 * @param minecart  the minecart that is being registered
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LTNTMinecart}
	 */
	public RegistryObject<EntityType<LTNTMinecart>> registerTNTMinecart(DeferredRegister<EntityType<?>> entityRegistry, String registryName, Supplier<EntityType<LTNTMinecart>> minecart){		
		return entityRegistry.register(registryName, minecart);
	}
	
	/**
	 * Registers a new {@link LivingPrimedLTNT}
	 * @param registryName  the registry name of this living primed TNT
	 * @param effect  the TNT effect this living primed TNT will have
	 * @param size  the size of the hitbox of this living primed TNT
	 * @param fireImmune  whether or not this living primed TNT can burn (visual only)
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LivingPrimedLTNT}
	 */
	public RegistryObject<EntityType<LivingPrimedLTNT>> registerLivingTNTEntity(String registryName, Supplier<EntityType<LivingPrimedLTNT>> TNT){
		return registerLivingTNTEntity(entityRegistry, registryName, TNT);
	}
	
	/**
	 * Registers a new {@link LivingPrimedLTNT}
	 * @param entityRegistry  the registry in which this living primed TNT is being registered into
	 * @param registryName  the registry name of this living primed TNT
	 * @param TNT  the TNT that is being registered
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LivingPrimedLTNT}
	 */
	public RegistryObject<EntityType<LivingPrimedLTNT>> registerLivingTNTEntity(DeferredRegister<EntityType<?>> entityRegistry, String registryName, Supplier<EntityType<LivingPrimedLTNT>> TNT){
		return entityRegistry.register(registryName, TNT);
	}
	
	/**
	 * Registers a new {@link LExplosiveProjectile}
	 * @param registryName  the registry name of this explosive projectile
	 * @param effect  the TNT effect this explosive projectile will have
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LExplosiveProjectile}
	 */
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(String registryName, PrimedTNTEffect effect){
		return registerExplosiveProjectile(registryName, effect, 1f, false);
	}
	
	/**
	 * Registers a new {@link LExplosiveProjectile}
	 * @param registryName  the registry name of this explosive projectile
	 * @param effect  the TNT effect this explosive projectile will have
	 * @param size  the size of the hitbox of this explosive projectile
	 * @param fireImmune  whether or not this explosive projectile can burn (visual only)
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LExplosiveProjectile}
	 */
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(String registryName, PrimedTNTEffect effect, float size, boolean fireImmune) {
		return registerExplosiveProjectile(entityRegistry, registryName, effect, size, fireImmune);
	}
	
	/**
	 * Registers a new {@link LExplosiveProjectile}
	 * @param entityRegistry  the registry in which this explosive projectile is being registered into
	 * @param registryName  the registry name of this explosive projectile
	 * @param effect  the TNT effect this explosive projectile will have
	 * @param size  the size of the hitbox of this explosive projectile
	 * @param fireImmune  whether or not this explosive projectile can burn (visual only)
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LExplosiveProjectile}
	 */
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(DeferredRegister<EntityType<?>> entityRegistry, String registryName, PrimedTNTEffect effect, float size, boolean fireImmune){
		if(fireImmune) {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<LExplosiveProjectile>of((EntityType<LExplosiveProjectile> type, Level level) -> new LExplosiveProjectile(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).fireImmune().sized(size, size).build(registryName));
		}
		else {
			return entityRegistry.register(registryName, () -> EntityType.Builder.<LExplosiveProjectile>of((EntityType<LExplosiveProjectile> type, Level level) -> new LExplosiveProjectile(type, level, effect), MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).sized(size, size).build(registryName));
		}
	}
	
	/**
	 * Registers a new {@link LExplosiveProjectile}
	 * @param entityRegistry  the registry in which this explosive projectile is being registered into
	 * @param registryName  the registry name of this explosive projectile
	 * @param projectile  the explosive projectile that is being registered
	 * @return {@link RegistryObject} of an {@link EntityType} of a {@link LExplosiveProjectile}
	 */
	public RegistryObject<EntityType<LExplosiveProjectile>> registerExplosiveProjectile(DeferredRegister<EntityType<?>> entityRegistry, String registryName, Supplier<EntityType<LExplosiveProjectile>> projectile){
		return entityRegistry.register(registryName, projectile);
	}
}
