package luckytntlib.util.tnteffects;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import luckytntlib.block.LTNTBlock;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.item.LDynamiteItem;
import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.explosions.ImprovedExplosion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * TNTXStrengthEffect is an extension of the PrimedTNTEffect and is an easy way to use an {@link ImprovedExplosion} as a TNT effect.
 * <p>
 * It offers all the customization needed to create small and large explosions for Dynamite, TNT and {@link StackedPrimedTNTEffect}.
 */
public class TNTXStrengthEffect extends PrimedTNTEffect{
	
	@Nullable private final Supplier<RegistryObject<LTNTBlock>> TNT;
	@Nullable private final Supplier<RegistryObject<LDynamiteItem>> dynamite;
	private final int fuse;
	private final float strength;
	private final float xzStrength, yStrength;
	private final float resistanceImpact;
	private final float randomVecLength;
	private final boolean fire;
	private final float knockbackStrength;
	private final boolean isStrongExplosion;
	private final float size;
	private final boolean airFuse;
	private final boolean explodesOnImpact;
	
	private TNTXStrengthEffect(@Nullable Supplier<RegistryObject<LTNTBlock>> TNT, @Nullable Supplier<RegistryObject<LDynamiteItem>> dynamite, int fuse, float strength, float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean fire, float knockbackStrength, boolean isStrongExplosion, float size, boolean airFuse, boolean explodesOnImpact) {
		this.TNT = TNT;
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
		this.size = size;
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
	public Block getBlock() {
		return TNT.get().get() == null ? Blocks.TNT : TNT.get().get();
	}
	
	@Override
	public Item getItem() {
		return dynamite.get().get() == null ? Items.AIR : dynamite.get().get();
	}
	
	@Override
	public int getDefaultFuse(IExplosiveEntity entity) {
		return fuse;
	}
	
	@Override
	public float getSize(IExplosiveEntity entity) {
		return size;
	}
		
	@Override
	public boolean airFuse() {
		return airFuse;
	}
	
	@Override
	public boolean explodesOnImpact() {
		return explodesOnImpact;
	}
	
	/**
	 * 
	 * This Builder serves the purpose of not having to fill in a huge constructor
	 */
	public static class Builder {
		
		private int fuse = 80;
		private float strength = 4f;
		private float xzStrength = 1f, yStrength = 1f;
		private float resistanceImpact = 1f;
		private float randomVecLength = 1f;
		private boolean fire = false;
		private float knockbackStrength = 1f;
		private boolean isStrongExplosion = false;
		private float size = 1f;
		private boolean airFuse = false;
		private boolean explodesOnImpact = true;
		
		public Builder() {			
		}
		
		private Builder(int fuse, float strength, float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean fire, float knockbackStrength, boolean isStrongExplosion, float size, boolean airFuse,  boolean explodesOnImpact) {
			this.fuse = fuse;
			this.strength = strength;
			this.xzStrength = xzStrength;
			this.yStrength = yStrength;
			this.resistanceImpact = resistanceImpact;
			this.randomVecLength = randomVecLength;
			this.fire = fire;
			this.knockbackStrength = knockbackStrength;
			this.isStrongExplosion = isStrongExplosion;
			this.size = size;
			this.airFuse = airFuse;
			this.explodesOnImpact = explodesOnImpact;
		}

		/**
		 * This value decides the fuse of this effect
		 * @param fuse
		 */
		public Builder fuse(int fuse) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * This value decides the strength/size/base radius of this explosion
		 * @param strength
		 */
		public Builder strength(float strength) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * This is a specific value that essentially scales the explosion in the x and z direction
		 * @param xzStrength
		 */
		public Builder xzStrength(float xzStrength) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * This is a specific value that essentially scales the explosion in the y direction
		 * @param yStrength
		 */
		public Builder yStrength(float yStrength) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * This value decides the impact that explosion resistance of blocks has on the explosion
		 * @param resistanceImpact
		 */
		public Builder resistanceImpact(float resistanceImpact) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * This value is multiplier to the random vector length of explosion vectors shot by the {@link ImprovedExplosion}, 
		 * which is also defined by the strength/size/base radius
		 * @param randomVecLength
		 */
		public Builder randomVecLength(float randomVecLength) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}

		/**
		 * This boolean decides whether or not this explosion should spawn fire
		 * @param fire
		 */
		public Builder fire(boolean fire) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}

		/**
		 * Tis value is a multiplier to the knockback inflicted by the explosion
		 * @param knockbackStrength
		 */
		public Builder knockbackStrength(float knockbackStrength) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * This boolean decides whether or not the explosion resistance of fluids should be taken into account
		 * @param isStrongExplosion
		 */
		public Builder isStrongExplosion(boolean isStronExplosion) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}

		/**
		 * This value defines the size of the rendered Block/Item
		 * @param size
		 */
		public Builder size(float size) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * This boolean decides whether or not an explosive projectile should be allowed to tick down their fuse in the air
		 * @implNote only works for explosive projectiles
		 * @param airFuse
		 */
		public Builder airFuse(boolean airFuse) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * This boolean decides whether or not an explosive projectile explodes immediately upon impact
		 * @implNote only works for explosive projectiles
		 * @param explodesOnImpact
		 */
		public Builder explodesOnImpact(boolean explodesOnImpact) {
			return new Builder(fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * Builds a new {@link TNTXStrengthEffect} without a TNT Block or Dynamite Item.
		 * @implNote Should only be used in secondary effects for {@link StackedPrimedTNTEffect}
		 * @return new TNTXStrengthEffect
		 */
		public TNTXStrengthEffect build() {
			return new TNTXStrengthEffect(null, null, fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * Builds a new {@link TNTXStrengthEffect} with a TNT Block.
		 * @param TNT  a {@link Supplier} of a {@link RegistryObject} of a {@link PrimedLTNT}
		 * @return new TNTXStrengthEffect
		 */
		public TNTXStrengthEffect buildTNT(Supplier<RegistryObject<LTNTBlock>> TNT) {
			return new TNTXStrengthEffect(TNT, null, fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
		
		/**
		 * Builds a new {@link TNTXStrengthEffect} with a Dynamite Item.
		 * @param dynamite  a {@link Supplier} of a {@link RegistryObject} of a {@link LDynamiteItem}
		 * @return new TNTXStrengthEffect
		 */
		public TNTXStrengthEffect buildDynamite(Supplier<RegistryObject<LDynamiteItem>> dynamite) {
			return new TNTXStrengthEffect(null, dynamite, fuse, strength, xzStrength, yStrength, resistanceImpact, randomVecLength, fire, knockbackStrength, isStrongExplosion, size, airFuse, explodesOnImpact);
		}
	}
}
