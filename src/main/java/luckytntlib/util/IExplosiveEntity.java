package luckytntlib.util;

import luckytntlib.util.explosions.ExplosiveEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IExplosiveEntity{

	public int getTNTFuse();
	
	public void setTNTFuse(int fuse);
	
	public Level level();
	
	public Vec3 getPos();
	
	public double x();
	
	public double y();
	
	public double z();
	
	public void destroy();
	
	public ExplosiveEffect getEffect();
}
