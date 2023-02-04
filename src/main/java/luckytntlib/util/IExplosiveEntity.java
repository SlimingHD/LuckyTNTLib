package luckytntlib.util;

import luckytntlib.util.tnteffects.ExplosiveEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeEntity;

public interface IExplosiveEntity extends IForgeEntity{

	public int getTNTFuse();
	
	public void setTNTFuse(int fuse);
	
	public Level level();
	
	public Vec3 getPos();
	
	public double x();
	
	public double y();
	
	public double z();
	
	public void destroy();
	
	public ExplosiveEffect getEffect();
	
	public Entity owner();
}
