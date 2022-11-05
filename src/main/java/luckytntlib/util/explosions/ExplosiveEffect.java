package luckytntlib.util.explosions;

import luckytntlib.util.IExplosiveEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ExplosiveEffect{

	public abstract void baseTick(IExplosiveEntity entity);
	
	public abstract void explosionTick(IExplosiveEntity entity);
	
	public abstract void serverExplosion(IExplosiveEntity entity);
	
	public abstract void spawnParticles(IExplosiveEntity entity);
	
	public abstract BlockState getBlockState();
	
	public abstract Block getBlock();
	
	public abstract float getSize();
}
