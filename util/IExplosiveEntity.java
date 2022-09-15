package luckytntlib.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IExplosiveEntity{

	public int getFuse();
	
	public void setFuse(int fuse);
	
	public Level getLevel();
	
	public Vec3 getPos();
	
	public void destroy();
}
