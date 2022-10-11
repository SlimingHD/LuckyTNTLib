package luckytntlib.util.explosions;

import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.IThrownExplosiveEntity;
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
				if(ent.getTNTFuse() <= 0) {
					ent.setTNTFuse(2);
				}
				if(ent.getTNTLevel().isClientSide) {
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
		}
	}
	
	public void spawnParticles(IExplosiveEntity entity) {
		entity.getTNTLevel().addParticle(ParticleTypes.SMOKE, entity.getTNTPos().x, entity.getTNTPos().y + 0.5f, entity.getTNTPos().z(), 0, 0, 0);
	}
	
	public void clientExplosion(IExplosiveEntity entity) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> entity.getTNTLevel().playSound(Minecraft.getInstance().player, new BlockPos(entity.getTNTPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER, 4f, (1f + (entity.getTNTLevel().random.nextFloat() - entity.getTNTLevel().random.nextFloat()) * 0.2f) * 0.7f));
	}

	public int getDefaultFuse() {
		return -1;
	}
}
