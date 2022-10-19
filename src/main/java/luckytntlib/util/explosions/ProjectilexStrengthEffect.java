package luckytntlib.util.explosions;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import luckytntlib.block.LTNTBlock;
import luckytntlib.util.IExplosiveEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.RegistryObject;

public class ProjectilexStrengthEffect extends ExplosiveProjectileEffect{
	
	@Nullable private final Supplier<RegistryObject<LTNTBlock>> TNT;
	@Nullable private final ItemStack item;
	private final int fuse;
	private final float strength;
	private final float xzStrength, yStrength;
	private final float resistanceImpact;
	private final float randomVecLength;
	private final float knockbackStrength;
	private final boolean isStrongExplosion;
	private final boolean airFuse;
	
	private ProjectilexStrengthEffect(@Nullable Supplier<RegistryObject<LTNTBlock>> TNT, @Nullable Supplier<RegistryObject<ItemLike>> item, int fuse, float strength, float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, float knockbackStrength, boolean isStrongExplosion, boolean airFuse) {
		this.TNT = TNT;
		this.item = item == null ? ItemStack.EMPTY : new ItemStack(item.get().get());
		this.fuse = fuse;
		this.strength = strength;
		this.xzStrength = xzStrength;
		this.yStrength = yStrength;
		this.resistanceImpact = resistanceImpact;
		this.randomVecLength = randomVecLength;
		this.knockbackStrength = knockbackStrength;
		this.isStrongExplosion = isStrongExplosion;
		this.airFuse = airFuse;
	}
	
	@Override
	public void clientExplosion(IExplosiveEntity entity) {
		super.clientExplosion(entity);
		ImprovedExplosion explosion = new ImprovedExplosion(entity.level(), (Entity) entity, entity.getPos().x, entity.getPos().y, entity.getPos().z, strength);
		explosion.doEntityExplosion(knockbackStrength, true);
	}

	@Override
	public void serverExplosion(IExplosiveEntity entity) {
		ImprovedExplosion explosion = new ImprovedExplosion(entity.level(), (Entity) entity, entity.getPos().x, entity.getPos().y, entity.getPos().z, strength);
		explosion.doEntityExplosion(knockbackStrength, true);
		explosion.doBlockExplosion(xzStrength, yStrength, resistanceImpact, randomVecLength, false, isStrongExplosion);
	}
	
	@Override
	public ItemStack getItem() {
		return item;
	}
	
	@Override
	public Block getBlock() {
		return TNT.get().get() == null ? Blocks.TNT : TNT.get().get();
	}
	
	@Override
	public boolean airFuse() {
		return airFuse;
	}
	
	@Override
	public int getDefaultFuse() {
		return fuse;
	}
	
	public static class Builder {
		
		@Nullable private final Supplier<RegistryObject<LTNTBlock>> TNT;
		@Nullable private final Supplier<RegistryObject<ItemLike>> item;
		private int fuse = -1;
		private float strength = 4f;
		private float xzStrength = 1f, yStrength = 1f;
		private float resistanceImpact = 1f;
		private float randomVecLength = 1f;
		private float knockbackStrength = 1f;
		private boolean isStrongExplosion = false;
		private boolean airFuse = false;
		
		public Builder(@Nullable Supplier<RegistryObject<LTNTBlock>> TNT, @Nullable Supplier<RegistryObject<ItemLike>> item) {
			this.TNT = TNT;
			this.item = item;
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
		
		public Builder airFuse(boolean airFuse) {
			this.airFuse = airFuse;
			return this;
		}
		
		public ProjectilexStrengthEffect build() {
			return new ProjectilexStrengthEffect(TNT, item, fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, knockbackStrength, isStrongExplosion, airFuse);
		}
	}
}
