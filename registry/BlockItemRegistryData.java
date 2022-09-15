package luckytntlib.registry;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;

public class BlockItemRegistryData {

	protected final Supplier<? extends Block> block;
	protected final String registryName;
	protected final String description;
	protected final CreativeModeTab tab;
	protected final boolean makeItem;
	
	protected BlockItemRegistryData(Supplier<? extends Block> block, String registryName, String description, @Nullable CreativeModeTab tab, boolean makeItem) {
		this.block = block;
		this.registryName = registryName;
		this.description = description;
		this.tab = tab;
		this.makeItem = makeItem;
	}
	
	public Supplier<? extends Block> getBlock() {
		return block;
	}
	
	public String getRegistryName() {
		return registryName;
	}
	
	public String getDescription() {
		return description;
	}
	
	public CreativeModeTab getTab() {
		return tab;
	}
	
	public boolean makeItem() {
		return makeItem;
	}
	
	public static class Builder{		
		protected final Supplier<? extends Block> block;
		protected final String registryName;
		protected String description = "";
		protected CreativeModeTab tab = CreativeModeTab.TAB_BUILDING_BLOCKS;
		protected boolean makeItem = true;
		
		public Builder(Supplier<? extends Block> block, String registryName) {
			this.block = block;
			this.registryName = registryName;
		}
		
		public Builder description(String description) {
			this.description = description;
			return this;
		}
		
		public Builder tab(CreativeModeTab tab) {
			this.tab = tab;
			return this;
		}
		
		public Builder makeItem(boolean makeItem) {
			this.makeItem = makeItem;
			return this;
		}
		
		public BlockItemRegistryData build() {
			return new BlockItemRegistryData(block, registryName, description, tab, makeItem);
		}
	}
}
