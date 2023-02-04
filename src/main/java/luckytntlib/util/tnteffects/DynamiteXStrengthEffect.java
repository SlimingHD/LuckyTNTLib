package luckytntlib.util.tnteffects;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import luckytntlib.item.LDynamiteItem;
import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.explosions.ImprovedExplosion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class DynamiteXStrengthEffect extends PrimedTNTEffect{
	
	@Nullable private final Supplier<RegistryObject<LDynamiteItem>> dynamite;
	private final int fuse;
	private final float strength;
	private final float xzStrength, yStrength;
	private final float resistanceImpact;
	private final float randomVecLength;
	private final boolean fire;
	private final float knockbackStrength;
	private final boolean isStrongExplosion;
	private final boolean airFuse;
	private final boolean explodesOnImpact;
	
	private DynamiteXStrengthEffect(@Nullable Supplier<RegistryObject<LDynamiteItem>> dynamite, int fuse, float strength, float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean fire, float knockbackStrength, boolean isStrongExplosion, boolean airFuse, boolean explodesOnImpact) {
		this.dynamite = dynamite;
		this.fuse = fuse;
		this.strength = strength;
		this.xzStrength = xzStrength;
		this.yStrength = yStrength;
		this.resistanceImpact = resistanceImpact;
		this.randomVecLength = randomVecLength;
		this.fire = fire;
		this.knockbackStrength = knockbackStrength;
		this.isStrongExplosion = isStrongExplosion;
		this.airFuse = airFuse;
		this.explodesOnImpact = explodesOnImpact;
	}

	@Override
	public void serverExplosion(IExplosiveEntity entity) {
		ImprovedExplosion explosion = new ImprovedExplosion(entity.level(), (Entity) entity, entity.getPos().x, entity.getPos().y + 0.5f, entity.getPos().z, strength);
		explosion.doEntityExplosion(knockbackStrength, true);
		explosion.doBlockExplosion(xzStrength, yStrength, resistanceImpact, randomVecLength, fire, isStrongExplosion);
	}
	
	@Override
	public Item getItem() {
		return dynamite.get().get();
	}
	
	@Override
	public int getDefaultFuse(IExplosiveEntity entity) {
		return fuse;
	}
	
	@Override
	public boolean airFuse() {
		return airFuse;
	}
	
	@Override
	public boolean explodesOnImpact() {
		return explodesOnImpact;
	}
	
	public static class Builder {
		
		@Nullable private final Supplier<RegistryObject<LDynamiteItem>> dynamite;
		private int fuse = 80;
		private float strength = 4f;
		private float xzStrength = 1f, yStrength = 1f;
		private float resistanceImpact = 1f;
		private float randomVecLength = 1f;
		private boolean fire = false;
		private float knockbackStrength = 1f;
		private boolean isStrongExplosion = false;
		private boolean airFuse = false;
		private boolean explodesOnImpact = true;
		
		public Builder(@Nullable Supplier<RegistryObject<LDynamiteItem>> dynamite) {
			this.dynamite = dynamite;
		}
		
		public Builder fuse(int fuse) {
			this.fuse = fuse;
			return this;
		}
		
		public Builder explosionStrength(float strength) {
			this.strength = strength;
			return this;
		}
		
		public Builder xzStrength(float xzStrength) {
			this.xzStrength = xzStrength;
			return this;
		}
		
		public Builder yStrength(float yStrength) {
			this.yStrength = yStrength;
			return this;
		}
		
		public Builder resistanceImpact(float resistanceImpact) {
			this.resistanceImpact = resistanceImpact;
			return this;
		}
		
		public Builder randomVecLength(float randomVecLength) {
			this.randomVecLength = randomVecLength;
			return this;
		}
		
		public Builder fire(boolean fire) {
			this.fire = fire;
			return this;
		}
		
		public Builder knockbackStrength(float knockbackStrength) {
			this.knockbackStrength = knockbackStrength;
			return this;
		}
		
		public Builder isStrongExplosion(boolean isStronExplosion) {
			this.isStrongExplosion = isStronExplosion;
			return this;
		}
		
		public Builder airFuse(boolean airFuse) {
			this.airFuse = airFuse;
			return this;
		}
		
		public Builder explodesOnImpact(boolean explodesOnImpact) {
			this.explodesOnImpact = explodesOnImpact;
			return this;
		}
		
		public DynamiteXStrengthEffect build() {
			return new DynamiteXStrengthEffect(dynamite, fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, airFuse, explodesOnImpact);
		}
	}
}
