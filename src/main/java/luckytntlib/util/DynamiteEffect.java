package luckytntlib.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public abstract class DynamiteEffect extends ExplosiveEffect implements ItemSupplier{
	
	@SuppressWarnings("resource")
	public void baseTick(IExplosiveEntity entity) {
		if(entity instanceof IThrownExplosiveEntity ent) {
			if(ent.inGround() || ent.hitEntity()) {
				if(ent.getFuse() <= 0) {
					ent.setFuse(2);
				}
				if(ent.getLevel().isClientSide) {
					int clientFuse = ent.getFuse() - 1;
					if(clientFuse == 0) {
						clientExplosion(ent);
						ent.destroy();
					}
					else {
						spawnParticles(ent);
					}
				}
				else {
					if(ent.getFuse() == 0) {
						serverExplosion(ent);
						ent.destroy();
					}
				}
				if(ent.getFuse() > 0) {
					explosionTick(ent);
					ent.setFuse(ent.getFuse() - 1);
				}
			}
		}
	}
	
	public void spawnParticles(IExplosiveEntity entity) {
		entity.getLevel().addParticle(ParticleTypes.SMOKE, entity.getPos().x, entity.getPos().y + 0.5f, entity.getPos().z(), 0, 0, 0);
	}
	
	public void clientExplosion(IExplosiveEntity entity) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> entity.getLevel().playSound(Minecraft.getInstance().player, new BlockPos(entity.getPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER, 4f, (1f + (entity.getLevel().random.nextFloat() - entity.getLevel().random.nextFloat()) * 0.2f) * 0.7f));
	}

	public int getDefaultFuse() {
		return -1;
	}
}
