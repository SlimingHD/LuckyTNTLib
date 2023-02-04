package luckytntlib.util.tnteffects;

import java.util.function.Supplier;

import luckytntlib.item.LDynamiteItem;
import luckytntlib.util.IExplosiveEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class GeneralDynamiteEffect extends PrimedTNTEffect{

	private final Supplier<RegistryObject<LDynamiteItem>> dynamite;
	private final PrimedTNTEffect effect;
	private ParticleOptions particles = ParticleTypes.SMOKE;
	
	public GeneralDynamiteEffect(Supplier<RegistryObject<LDynamiteItem>> dynamite, ParticleOptions particles, PrimedTNTEffect effect) {
		this.dynamite = dynamite;
		this.particles = particles;
		this.effect = effect;
	}
	
	public GeneralDynamiteEffect(Supplier<RegistryObject<LDynamiteItem>> dynamite, PrimedTNTEffect effect) {
		this.dynamite = dynamite;
		this.effect = effect;
	}
	
	@Override
	public void serverExplosion(IExplosiveEntity entity) {
		effect.serverExplosion(entity);
	}
	
	@Override
	public void explosionTick(IExplosiveEntity entity) {
		effect.explosionTick(entity);
	}
	
	@Override
	public void spawnParticles(IExplosiveEntity entity) {
		entity.level().addParticle(particles, entity.x(), entity.y(), entity.z(), 0, 0, 0);
	}
	
	@Override
	public Item getItem() {
		return dynamite.get().get();
	}
}
