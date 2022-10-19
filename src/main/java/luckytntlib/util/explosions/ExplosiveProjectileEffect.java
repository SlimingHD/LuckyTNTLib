package luckytntlib.util.explosions;

import luckytntlib.client.util.ClientSoundExecutor;
import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.IExplosiveProjectileEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public abstract class ExplosiveProjectileEffect extends ExplosiveEffect implements ItemSupplier{
	
	@SuppressWarnings("resource")
	public void baseTick(IExplosiveEntity entity) {
		if(entity instanceof IExplosiveProjectileEntity ent) {
			if(ent.inGround() || ent.hitEntity()) {
				if(ent.getTNTFuse() < 0 || airFuse()) {
					ent.setTNTFuse(2);
				}
				if(ent.level().isClientSide) {
					int clientFuse = ent.getTNTFuse() - 1;
					if(clientFuse == 0) {
						clientExplosion(ent);
						ent.destroy();
					}
					else {
						spawnParticles(ent);
					}
				}
				else {
					if(ent.getTNTFuse() == 0) {
						serverExplosion(ent);
						ent.destroy();
					}
				}
				if(ent.getTNTFuse() > 0) {
					explosionTick(ent);
					ent.setTNTFuse(ent.getTNTFuse() - 1);
				}
			}
			if(ent.getTNTFuse() > 0 && airFuse()) {
				explosionTick(ent);
				ent.setTNTFuse(ent.getTNTFuse() - 1);
			}
		}
	}
	
	public void spawnParticles(IExplosiveEntity entity) {
		entity.level().addParticle(ParticleTypes.SMOKE, entity.getPos().x, entity.getPos().y + 0.5f, entity.getPos().z(), 0, 0, 0);
	}
	
	@SuppressWarnings("resource")
	public void clientExplosion(IExplosiveEntity entity) {
		ClientSoundExecutor.playSoundAt(entity.getPos().x, entity.getPos().y, entity.getPos().z, SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER, 4f, (1f + (entity.level().random.nextFloat() - entity.level().random.nextFloat()) * 0.2f) * 0.7f);
	}
	
	public void serverExplosion(IExplosiveEntity entity) {		
	}
	
	public void explosionTick(IExplosiveEntity entity) {		
	}
	
	public boolean airFuse() {
		return false;
	}

	public int getDefaultFuse() {
		return -1;
	}
	
	public float getSize() {
		return 1f;
	}
	
	public Block getBlock() {
		return Blocks.AIR;
	}
	
	public ItemStack getItem() {
		return ItemStack.EMPTY;
	}
}
