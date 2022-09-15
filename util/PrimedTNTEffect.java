package luckytntlib.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public abstract class PrimedTNTEffect extends ExplosiveEffect{
	
	public void baseTick(IExplosiveEntity entity) {
		Level level = entity.getLevel();
		if(level.isClientSide) {
			int clientFuse = entity.getFuse() - 1;
			if (clientFuse <= 0) {
				clientExplosion(entity);
				entity.destroy();
			} else {
				spawnParticles(entity);
			}
		}
		else if(entity.getFuse() <= 0) {
			serverExplosion(entity);
			entity.destroy();
		}
		explosionTick(entity);
		entity.setFuse(entity.getFuse() - 1);
	}
	
	public void spawnParticles(IExplosiveEntity entity) {
		entity.getLevel().addParticle(ParticleTypes.SMOKE, entity.getPos().x(), entity.getPos().y, entity.getPos().z, 0, 0, 0);
	}
	
	public void clientExplosion(IExplosiveEntity entity) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> entity.getLevel().playSound(Minecraft.getInstance().player, new BlockPos(entity.getPos()), SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER, 4f, (1f + (entity.getLevel().random.nextFloat() - entity.getLevel().random.nextFloat()) * 0.2f) * 0.7f));
	}

	public int getDefaultFuse() {
		return 80;
	}
}
