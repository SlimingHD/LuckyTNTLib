package luckytntlib.registry;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.material.MaterialColor;

/**
 * 
 * The TNTBlockRegistryData only serves the purpose of bringing together relatively simple and repetitive properties that a TNT block/item may have.
 * It is currently only used by the {@link RegistryHelper} in some registering methods of TNT blocks.
 * A TNTBlockRegistryData can be created by using the Builder subclass.
 */
public class TNTBlockRegistryData {

	private final String registryName;
	private final boolean makeItem;
	private final boolean addDispenseBehavior;
	private final boolean randomizedFuseUponExploded;
	private final boolean addToTNTLists;
	private final TranslatableContents description;
	private final String tab;
	private final MaterialColor color;
	
	
	private TNTBlockRegistryData(String registryName, boolean makeItem, boolean addDispenseBehavior, boolean randomizedFuseUponExploded, boolean addToTNTLists, TranslatableContents description, String tab, MaterialColor color) {
		this.registryName = registryName;
		this.makeItem = makeItem;
		this.addDispenseBehavior = addDispenseBehavior;
		this.randomizedFuseUponExploded = randomizedFuseUponExploded;
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

	public boolean addDispenseBehavior() {
		return addDispenseBehavior;
	}

	public boolean randomizedFuseUponExploded() {
		return randomizedFuseUponExploded;
	}
	
	public boolean addToTNTLists() {
		return addToTNTLists;
	}
	
	public TranslatableContents getDescription() {
		return description;
	}

	public String getTab() {
		return tab;
	}

	public MaterialColor getColor() {
		return color;
	}
	
	public static class Builder {
		
		private final String registryName;
		private boolean makeItem = true;
		private boolean addDispenseBehavior = true;
		private boolean randomizedFuseUponExploded = true;
		private boolean addToTNTLists = true;
		private TranslatableContents description = new TranslatableContents("");
		private String tab = "none";
		private MaterialColor color = MaterialColor.COLOR_RED;
		
		public Builder(String registryName) {
			this.registryName = registryName;
		}

		public Builder makeItem(boolean makeItem) {
			this.makeItem = makeItem;
			return this;
		}

		public Builder addDispenseBehavior(boolean addDispenseBehavior) {
			this.addDispenseBehavior = addDispenseBehavior;
			return this;
		}

		public Builder randomizedFuseUponExploded(boolean randomizedFuseUponExploded) {
			this.randomizedFuseUponExploded = randomizedFuseUponExploded;
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

		public Builder tab(String tab) {
			this.tab = tab;
			return this;
		}

		public Builder color(MaterialColor color) {
			this.color = color;
			return this;
		}
		
		public TNTBlockRegistryData build() {
			return new TNTBlockRegistryData(registryName, makeItem, addDispenseBehavior, randomizedFuseUponExploded, addToTNTLists, description, tab, color);
		}
	}
}
