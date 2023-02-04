package luckytntlib.util.tnteffects;

import luckytntlib.entity.LExplosiveProjectile;
import luckytntlib.entity.LivingPrimedLTNT;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.util.IExplosiveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public abstract class PrimedTNTEffect extends ExplosiveEffect{
	
	private ItemStack stack;
	
	@Override
	public void baseTick(IExplosiveEntity entity) {
		Level level = entity.level();
		if(entity instanceof PrimedLTNT ent) {
			if(ent.getTNTFuse() <= 0) {
				if(ent.level instanceof ServerLevel) {
					if(playsSound()) {
						entity.level().playSound((Entity)entity, new BlockPos(entity.getPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, (1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
					}
					serverExplosion(entity);
				}
				entity.destroy();
			}
			explosionTick(entity);
			entity.setTNTFuse(entity.getTNTFuse() - 1);
		}
		else if(entity instanceof LivingPrimedLTNT ent) {
			if(ent.getTNTFuse() <= 0) {
				if(ent.level instanceof ServerLevel) {
					if(playsSound()) {
						entity.level().playSound((Entity)entity, new BlockPos(entity.getPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, (1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
					}
					serverExplosion(entity);
				}
				entity.destroy();
			}
			explosionTick(entity);
			entity.setTNTFuse(entity.getTNTFuse() - 1);
		}
		else if(entity instanceof LExplosiveProjectile ent) {
			if((ent.inGround() || ent.hitEntity()) && entity.level() instanceof ServerLevel sLevel) {
				if(explodesOnImpact()) {
					ent.setTNTFuse(0);
				}
				if(ent.getTNTFuse() == 0) {
					if(ent.level instanceof ServerLevel) {
						if(playsSound()) {
							entity.level().playSound((Entity)entity, new BlockPos(entity.getPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, (1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
						}
						serverExplosion(entity);
					}
					ent.destroy();
				}
			}
			else if(airFuse() && entity.getTNTFuse() == 0) {
				if(ent.level instanceof ServerLevel) {
					if(playsSound()) {
						entity.level().playSound((Entity)entity, new BlockPos(entity.getPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, (1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
					}
					serverExplosion(entity);
				}
				ent.destroy();
			}
			if((ent.getTNTFuse() > 0 && airFuse()) || ent.hitEntity() || ent.inGround()) {
				explosionTick(ent);
				ent.setTNTFuse(ent.getTNTFuse() - 1);
			}
		}
		if(level.isClientSide) {
			spawnParticles(entity);
		}
	}
	
	@Override
	public void spawnParticles(IExplosiveEntity entity) {
		entity.level().addParticle(ParticleTypes.SMOKE, entity.x(), entity.y() + 0.5f, entity.z(), 0, 0, 0);
	}
	
	@Override
	public void serverExplosion(IExplosiveEntity entity) {	
	}
	
	@Override
	public void explosionTick(IExplosiveEntity entity) {		
	}
	
	public int getDefaultFuse(IExplosiveEntity entity) {
		return 80;
	}
	
	@Override
	public float getSize(IExplosiveEntity entity) {
		return 1f;
	}
	
	public boolean playsSound() {
		return true;
	}
	
	@Override
	public boolean explodesOnImpact() {
		return true;
	}
	
	@Override
	public boolean airFuse() {
		return false;
	}
	
	@Override
	public ItemStack getItemStack() {
		if(stack == null) {
			stack = new ItemStack(getItem());
		}
		return stack;
	}
	
	@Override
	public Item getItem() {
		return Items.AIR;
	}
	
	@Override
	public BlockState getBlockState(IExplosiveEntity entity) {
		return getBlock().defaultBlockState();
	}
	
	@Override
	public Block getBlock() {
		return Blocks.TNT;
	}
}
