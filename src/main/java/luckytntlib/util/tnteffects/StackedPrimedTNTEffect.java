package luckytntlib.util.tnteffects;

import java.util.ArrayList;
import java.util.List;

import luckytntlib.util.IExplosiveEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A StackedPrimedTNTEffect is a way of combining multiple PrimedTNTEffects into one.
 * <p>
 * It consists out of a primary and a list of secondary effects.
 * Secondary effects get executed, limited to {@link PrimedTNTEffect#serverExplosion(IExplosiveEntity)} 
 * and {@link PrimedTNTEffect#explosionTick(IExplosiveEntity)}, one after the other, 
 * while the primary effect on top of this also decides how the explosive entity will look and behave.
 */
public class StackedPrimedTNTEffect extends PrimedTNTEffect{

	/**
	 * List of all PrimedTNTEffects. The first effect of this list is the primary effect.
	 */
	private final List<PrimedTNTEffect> effects = new ArrayList<>();
	
	/**
	 * @param primaryEffect  the primary PrimedTNTEffect
	 * @param secondaryEffects  the list of secondary PrimedTNTEffects
	 */
	public StackedPrimedTNTEffect(PrimedTNTEffect primaryEffect, List<PrimedTNTEffect> secondaryEffects) {
		effects.add(primaryEffect);
		effects.addAll(secondaryEffects);
	}
	
	@Override
	public void explosionTick(IExplosiveEntity entity) {
		for(PrimedTNTEffect effect : effects) {
			effect.explosionTick(entity);
		}
	}
	
	@Override
	public void serverExplosion(IExplosiveEntity entity) {
		for(PrimedTNTEffect effect : effects) {
			effect.serverExplosion(entity);
		}
	}
	
	@Override
	public void spawnParticles(IExplosiveEntity entity) {
		effects.get(0).spawnParticles(entity);
	}
	
	@Override
	public Item getItem() {
		return effects.get(0).getItem();
	}
	
	@Override
	public ItemStack getItemStack() {
		return effects.get(0).getItemStack();
	}
	
	@Override
	public Block getBlock() {
		return effects.get(0).getBlock();
	}
	
	@Override
	public BlockState getBlockState(IExplosiveEntity entity) {
		return effects.get(0).getBlockState(entity);
	}
	
	@Override
	public boolean airFuse() {
		return effects.get(0).airFuse();
	}
	
	@Override
	public boolean explodesOnImpact() {
		return effects.get(0).explodesOnImpact();
	}
	
	@Override
	public float getSize(IExplosiveEntity entity) {
		return effects.get(0).getSize(entity);
	}
	
	@Override
	public int getDefaultFuse(IExplosiveEntity entity) {
		return effects.get(0).getDefaultFuse(entity);
	}
}
