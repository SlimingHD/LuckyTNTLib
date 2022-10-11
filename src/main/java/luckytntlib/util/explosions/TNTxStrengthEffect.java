package luckytntlib.util.explosions;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import luckytntlib.block.LTNTBlock;
import luckytntlib.util.IExplosiveEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.RegistryObject;

public class TNTxStrengthEffect extends PrimedTNTEffect{
	
	@Nullable private final Supplier<RegistryObject<LTNTBlock>> TNT;
	private final int fuse;
	private final float strength;
	private final float xzStrength, yStrength;
	private final float resistanceImpact;
	private final float randomVecLength;
	private final float knockbackStrength;
	private final boolean isStrongExplosion;
	
	private TNTxStrengthEffect(@Nullable Supplier<RegistryObject<LTNTBlock>> TNT, int fuse, float strength, float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, float knockbackStrength, boolean isStrongExplosion) {
		this.TNT = TNT;
		this.fuse = fuse;
		this.strength = strength;
		this.xzStrength = xzStrength;
		this.yStrength = yStrength;
		this.resistanceImpact = resistanceImpact;
		this.randomVecLength = randomVecLength;
		this.knockbackStrength = knockbackStrength;
		this.isStrongExplosion = isStrongExplosion;
	}
	
	@Override
	public void clientExplosion(IExplosiveEntity entity) {
		super.clientExplosion(entity);
		ImprovedExplosion explosion = new ImprovedExplosion(entity.getTNTLevel(), (Entity) entity, entity.getTNTPos().x, entity.getTNTPos().y, entity.getTNTPos().z, strength);
		explosion.doEntityExplosion(knockbackStrength, true);
	}

	@Override
	public void serverExplosion(IExplosiveEntity entity) {
		ImprovedExplosion explosion = new ImprovedExplosion(entity.getTNTLevel(), (Entity) entity, entity.getTNTPos().x, entity.getTNTPos().y, entity.getTNTPos().z, strength);
		explosion.doEntityExplosion(knockbackStrength, true);
		explosion.doBlockExplosion(xzStrength, yStrength, resistanceImpact, randomVecLength, false, isStrongExplosion);
	}
	
	@Override
	public Block getBlock() {
		return TNT.get().get() == null ? Blocks.TNT : TNT.get().get();
	}
	
	@Override
	public int getDefaultFuse() {
		return fuse;
	}
	
	public static class Builder {
		
		private final Supplier<RegistryObject<LTNTBlock>> TNT;
		private int fuse = 80;
		private float strength = 4f;
		private float xzStrength = 1f, yStrength = 1f;
		private float resistanceImpact = 1f;
		private float randomVecLength = 1f;
		private float knockbackStrength = 1f;
		private boolean isStrongExplosion = false;
		
		public Builder(@Nullable Supplier<RegistryObject<LTNTBlock>> TNT) {
			this.TNT = TNT;
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
		
		public Builder knockbackStrength(float knockbackStrength) {
			this.knockbackStrength = knockbackStrength;
			return this;
		}
		
		public Builder isStrongExplosion(boolean isStronExplosion) {
			this.isStrongExplosion = isStronExplosion;
			return this;
		}
		
		public TNTxStrengthEffect build() {
			return new TNTxStrengthEffect(TNT, fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, knockbackStrength, isStrongExplosion);
		}
	}
}
