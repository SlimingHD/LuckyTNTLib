package luckytntlib.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IExplosiveEntity{

	public int getTNTFuse();
	
	public void setTNTFuse(int fuse);
	
	public Level getTNTLevel();
	
	public Vec3 getTNTPos();
	
	public double x();
	
	public double y();
	
	public double z();
	
	public void destroy();
}
