package luckytntlib.util.tnteffects;

import luckytntlib.entity.LExplosiveProjectile;
import luckytntlib.entity.LTNTMinecart;
import luckytntlib.entity.LivingPrimedLTNT;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.util.IExplosiveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Extensions of this class serve as a way to define how a TNT, Minecart or Dynamite behaves.
 * <p>
 * It controls what a TNT does upon exploding, what particles it displays, what Block/Item gets rendered 
 * and general logic like conditions for exploding.
 */
public abstract class PrimedTNTEffect{	
	/**
	 * 
	 * This void is the heart of the PrimedTNTEffect. It's executed every tick on both the logical client and the logical server side 
	 * and is slightly different for every entity given by LTL implementing {@link IExplosiveEntity}.
	 * <p>
	 * Its default implementation works for all Explosives with littly complexity in their behaviour.
	 * <p>
	 * Override this method if you want to change the way your TNT behaves on a basic level 
	 * or if you want to add logic for your own entity implementing {@link IExplosiveEntity}
	 * @param entity  the {@link IExplosiveEntity} this PrimedTNTEffect belongs to.
	 */
	public void baseTick(IExplosiveEntity entity) {
		Level level = entity.level();
		/**
		 * Default logic implementation for TNT and TNT Minecarts
		 */
		if(entity instanceof PrimedLTNT || entity instanceof LivingPrimedLTNT || entity instanceof LTNTMinecart) {
			if(entity.getTNTFuse() <= 0) {
				if(entity.level() instanceof ServerLevel) {
					if(playsSound()) {
						level.playSound(null, new BlockPos(entity.getPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, (1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
					}
					serverExplosion(entity);
				}
				entity.destroy();
			}
			explosionTick(entity);
			entity.setTNTFuse(entity.getTNTFuse() - 1);
		}
		/**
		 * Default logic implementation for Explosive Projectiles
		 */
		else if(entity instanceof LExplosiveProjectile ent) {
			if((ent.inGround() || ent.hitEntity()) && entity.level() instanceof ServerLevel sLevel) {
				if(explodesOnImpact()) {
					ent.setTNTFuse(0);
				}
				if(ent.getTNTFuse() == 0) {
					if(ent.level instanceof ServerLevel) {
						if(playsSound()) {
							level.playSound(null, new BlockPos(entity.getPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, (1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
						}
						serverExplosion(entity);
					}
					ent.destroy();
				}
			}
			else if(airFuse() && entity.getTNTFuse() == 0) {
				if(ent.level instanceof ServerLevel) {
					if(playsSound()) {
						level.playSound(null, new BlockPos(entity.getPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4f, (1f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
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
	
	/**
	 * This void is only executed on the logical client side by {@link PrimedTNTEffect#baseTick(IExplosiveEntity)} every tick.
	 * <p>
	 * Override this void if you want to display different particles or if you want no particles at all.
	 * @param entity  the {@link IExplosiveEntity} this PrimedTNTEffect belongs to.
	 */
	public void spawnParticles(IExplosiveEntity entity) {
		entity.level().addParticle(ParticleTypes.SMOKE, entity.x(), entity.y() + 0.5f, entity.z(), 0, 0, 0);
	}
	
	/**
	 * This void is only executed on the logical server side by {@link PrimedTNTEffect#baseTick(IExplosiveEntity)} once the fuse hits 0.
	 * <p>
	 * @implNote Due to synchronization inconsistencies a clientExplosion does not exist and must be implemented manually without the dependency of an entity.
	 * @param entity  the {@link IExplosiveEntity} this PrimedTNTEffect belongs to.
	 */
	public void serverExplosion(IExplosiveEntity entity) {	
	}
	
	/**
	 * This void is executed on both logical sides by {@link PrimedTNTEffect#baseTick(IExplosiveEntity)} every tick.
	 * @param entity  the {@link IExplosiveEntity} this PrimedTNTEffect belongs to.
	 */
	public void explosionTick(IExplosiveEntity entity) {		
	}
	
	/**
	 * @param entity  the {@link IExplosiveEntity} this PrimedTNTEffect belongs to.
	 * @return Default Fuse of this Epxlosive Entity.
	 */
	public int getDefaultFuse(IExplosiveEntity entity) {
		return 80;
	}
	
	/**
	 * @param entity  the {@link IExplosiveEntity} this PrimedTNTEffect belongs to.
	 * @return Size of this entity for the renderer.
	 */
	public float getSize(IExplosiveEntity entity) {
		return 1f;
	}
	
	/**
	 * @return Whether or not this TNT plays an explosion sound when executing {@link PrimedTNTEffect#serverExplosion(IExplosiveEntity)}.
	 */
	public boolean playsSound() {
		return true;
	}
	
	/**
	 * @implNote Only used by {@link LExplosiveProjectile}!
	 * @return Whether or not this Explosive Projectile explodes upon hitting a block or an entity or not
	 */
	public boolean explodesOnImpact() {
		return true;
	}
	
	/**
	 * @implNote Only used by {@link LExplosiveProjectile}!
	 * @return Whether or not this Explosive Projectile's fuse should tick down while still in the air
	 */
	public boolean airFuse() {
		return false;
	}
	
	/**
	 * Gets the Itemstack that is rendererd!	 
	 * @implNote Useless if no Item is rendered or this method is not called
	 * @return {@link ItemStack} to render.
	 */
	public ItemStack getItemStack() {
		return new ItemStack(getItem());
	}
	
	/**
	 * Gets the Item that is rendererd!
	 * @implNote Useless if no Item is rendered or this method is not called
	 * @return {@link Item} used for the {@link ItemStack} of {@link PrimedTNTEffect#getItemStack()}
	 */
	public Item getItem() {
		return Items.AIR;
	}
	
	/**
	 * Gets the Blockstate that is rendered!
	 * @implNote Useless if no Block is rendered or this method is not called
	 * @param entity  the {@link IExplosiveEntity} this PrimedTNTEffect belongs to.
	 * @return {@link BlockState} to render.
	 */
	public BlockState getBlockState(IExplosiveEntity entity) {
		return getBlock().defaultBlockState();
	}
	
	/**
	 * Gets the Block that is rendered!
	 * @implNote Useless if no Block is rendered or this method is not called.
	 * @return {@link Block} used for the {@link BlockState} of {@link PrimedTNTEffect#getBlockState(IExplosiveEntity)}.
	 */
	public Block getBlock() {
		return Blocks.TNT;
	}
}
