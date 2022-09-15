package luckytntlib.registry;

import java.util.HashMap;

import javax.annotation.Nullable;

import net.minecraft.world.item.CreativeModeTab;

public class LuckyTNTCreativeTabs {

	private static final HashMap<String, LuckyTNTCreativeTabHolder> tabs = new HashMap<>();

    public static void load() {
    	for(LuckyTNTCreativeTabHolder tab : tabs.values()) {
    		tab.load();
    	}
    }
    
    public static void addTab(String name, LuckyTNTCreativeTabHolder tab) {
    	tabs.put(name, tab);
    }
    
    @Nullable
    public static CreativeModeTab getTab(String name) {
    	return tabs.get(name) == null ? null : tabs.get(name).tab;
    }
    
    public static class LuckyTNTCreativeTabHolder{
    	
    	public CreativeModeTab tab;
    	
    	public void load() {
    		
    	}
    }
}
