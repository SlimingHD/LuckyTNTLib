package luckytntlib.registry;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import luckytntlib.block.LTNTBlock;
import net.minecraft.world.item.CreativeModeTab;

public class TNTBlockItemRegistryData extends BlockItemRegistryData{
	private final boolean addDispenserBehaviour;
	private final boolean addToLists;
	
	private TNTBlockItemRegistryData(Supplier<LTNTBlock> block, String registryName, String description, @Nullable CreativeModeTab tab, boolean makeItem, boolean addDispenserBehaviour, boolean addToLists) {
		super(block, registryName, description, tab, makeItem);
		this.addDispenserBehaviour = addDispenserBehaviour;
		this.addToLists = addToLists;
	}
	
	public boolean addDispenserBehaviour() {
		return addDispenserBehaviour;
	}
	
	public boolean addToTNTLists() {
		return addToLists;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Supplier<LTNTBlock> getBlock(){
		return (Supplier<LTNTBlock>)super.getBlock();
	}
	
	public static class Builder{
		private final Supplier<LTNTBlock> block;
		private final String registryName;
		private String description = "";
		private CreativeModeTab tab;
		private boolean makeItem = true;
		private boolean addDispenserBehaviour = true;
		private boolean addToLists = true;
		
		public Builder(String registryName, Supplier<LTNTBlock> block) {
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
		
		public Builder addDispenserBehaviour(boolean dispenserBehaviour) {
			this.addDispenserBehaviour = dispenserBehaviour;
			return this;
		}
		
		public Builder addToTNTLists(boolean addToLists) {
			this.addToLists = addToLists;
			return this;
		}
		
		public TNTBlockItemRegistryData build() {
			return new TNTBlockItemRegistryData(block, registryName, description, tab, makeItem, addDispenserBehaviour, addToLists);
		}
	}
}
