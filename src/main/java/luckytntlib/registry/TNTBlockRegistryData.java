package luckytntlib.registry;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.material.MaterialColor;

public class TNTBlockRegistryData {

	private final String registryName;
	private final boolean makeItem;
	private final boolean addDispenserBehaviour;
	private final boolean shouldRandomlyFuse;
	private final boolean addToTNTLists;
	private final TranslatableContents description;
	private final CreativeModeTab tab;
	private final MaterialColor color;
	
	
	private TNTBlockRegistryData(String registryName, boolean makeItem, boolean addDispenserBehaviour, boolean shouldRandomlyFuse, boolean addToTNTLists, TranslatableContents description, CreativeModeTab tab, MaterialColor color) {
		this.registryName = registryName;
		this.makeItem = makeItem;
		this.addDispenserBehaviour = addDispenserBehaviour;
		this.shouldRandomlyFuse = shouldRandomlyFuse;
		this.addToTNTLists = addToTNTLists;
		this.description = description;
		this.tab = tab;
		this.color = color;
	}

	public String getRegistryName() {
		return registryName;
	}

	public boolean makeItem() {
		return makeItem;
	}

	public boolean addDispenserBehaviour() {
		return addDispenserBehaviour;
	}

	public boolean shouldRandomlyFuse() {
		return shouldRandomlyFuse;
	}
	
	public boolean addToTNTLists() {
		return addToTNTLists;
	}
	
	public TranslatableContents getDescription() {
		return description;
	}

	public CreativeModeTab getTab() {
		return tab;
	}

	public MaterialColor getColor() {
		return color;
	}
	
	public static class Builder {
		
		private final String registryName;
		private boolean makeItem = true;
		private boolean addDispenserBehaviour = true;
		private boolean shouldRandomlyFuse = true;
		private boolean addToTNTLists = true;
		private TranslatableContents description = new TranslatableContents("");
		private CreativeModeTab tab = CreativeModeTab.TAB_REDSTONE;
		private MaterialColor color = MaterialColor.COLOR_RED;
		
		public Builder(String registryName) {
			this.registryName = registryName;
		}

		public Builder makeItem(boolean makeItem) {
			this.makeItem = makeItem;
			return this;
		}

		public Builder addDispenserBehaviour(boolean addDispenserBehaviour) {
			this.addDispenserBehaviour = addDispenserBehaviour;
			return this;
		}

		public Builder shouldRandomlyFuse(boolean shouldRandomlyFuse) {
			this.shouldRandomlyFuse = shouldRandomlyFuse;
			return this;
		}
		
		public Builder addToTNTLists(boolean addToTNTLists) {
			this.addToTNTLists = addToTNTLists;
			return this;
		}
		
		public Builder description(TranslatableContents description) {
			this.description = description;
			return this;
		}

		public Builder tab(CreativeModeTab tab) {
			this.tab = tab;
			return this;
		}

		public Builder color(MaterialColor color) {
			this.color = color;
			return this;
		}
		
		public TNTBlockRegistryData build() {
			return new TNTBlockRegistryData(registryName, makeItem, addDispenserBehaviour, shouldRandomlyFuse, addToTNTLists, description, tab, color);
		}
	}
}
