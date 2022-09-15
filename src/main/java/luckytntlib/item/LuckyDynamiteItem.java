package luckytntlib.item;

import java.util.List;

import luckytntlib.entity.LDynamite;
import luckytntlib.util.IllegalTNTException;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

public class LuckyDynamiteItem extends LDynamiteItem{

	public List<RegistryObject<LDynamiteItem>> dynamites;
	
	public LuckyDynamiteItem(Item.Properties properties) {
		super(properties, null, true);
	}

	public void setTNTList(List<RegistryObject<LDynamiteItem>> dynamites) {
		this.dynamites = dynamites;
	}
	
	@Override
	public LDynamite shoot(Level level, double x, double y, double z, Vec3 direction, float power, LivingEntity thrower) throws IllegalTNTException{
		if(!(dynamites.get(random.nextInt(dynamites.size())).get() instanceof LuckyDynamiteItem)) {
			return dynamites.get(random.nextInt(dynamites.size())).get().shoot(level, x, y, z, direction, power, thrower);
		}
		throw new IllegalTNTException("Oh uh, you can't shoot a Lucky Dynamite through a Lucky Dynamite");
	}
}
