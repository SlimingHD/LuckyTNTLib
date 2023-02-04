package luckytntlib.util.tnteffects;

import luckytntlib.util.IExplosiveEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ExplosiveEffect{

	public abstract void baseTick(IExplosiveEntity entity);
	
	public abstract void explosionTick(IExplosiveEntity entity);
	
	public abstract void serverExplosion(IExplosiveEntity entity);

	public abstract void spawnParticles(IExplosiveEntity entity);
	
	public abstract BlockState getBlockState(IExplosiveEntity entity);
	
	public abstract Block getBlock();
	
	public abstract ItemStack getItemStack();
	
	public abstract Item getItem();
	
	public abstract float getSize(IExplosiveEntity entity);
	
	public abstract boolean explodesOnImpact();
	
	public abstract boolean airFuse();
}
