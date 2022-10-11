package luckytntlib.util.explosions;

import luckytntlib.util.IExplosiveEntity;
import net.minecraft.world.level.block.Block;

public abstract class ExplosiveEffect{

	public abstract void baseTick(IExplosiveEntity entity);
	
	public abstract void explosionTick(IExplosiveEntity entity);
	
	public abstract void clientExplosion(IExplosiveEntity entity);
	
	public abstract void serverExplosion(IExplosiveEntity entity);
	
	public abstract void spawnParticles(IExplosiveEntity entity);
	
	public abstract Block getBlock();
	
	public abstract float getSize();
}
