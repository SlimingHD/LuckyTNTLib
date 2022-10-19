package luckytntlib.util.explosions;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

public class ImprovedExplosion extends Explosion{

	public final Level level;
	public final double posX, posY, posZ;
	public final float size;
	public final ExplosionDamageCalculator damageCalculator;
	
	public ImprovedExplosion(Level level, Vec3 position, float size) {
		this(level, null, position, size);
	}
	
	public ImprovedExplosion(Level level, @Nullable Entity owner, Vec3 position, float size) {
		this(level, owner, position.x, position.y, position.z, size);
	}
	
	public ImprovedExplosion(Level level, @Nullable Entity owner, double x, double y, double z, float size) {
		super(level, owner, null, null, x, y, z, size, false, BlockInteraction.NONE);
		this.level = level;
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.size = size;
		damageCalculator = owner == null ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(owner);
	}
	
	public void doBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean fire, boolean isStrongExplosion) {
		Set<BlockPos> blocks = new HashSet<>();
		for(int offX = (int)-size; offX <= size; offX++) {
			for(int offY = (int)-size; offY <= size; offY++) {
				for(int offZ = (int)-size; offZ <= size; offZ++) {
					if(offX == (int)-size || offX == size || offY == (int)-size || offY == size || offZ == (int)-size || offZ == size) {
						double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float)Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for(float vecStep = 0; vecStep < vecLength; vecStep += 0.225f) {
							blockX += xStep * 0.3f * xzStrength;
							blockY += yStep * 0.3f * yStrength;
							blockZ += zStep * 0.3f * xzStrength;
							BlockPos pos = new BlockPos(blockX, blockY, blockZ);
							if(!level.isInWorldBounds(pos)) {
								break;
							}
							BlockState blockState = level.getBlockState(pos);
							FluidState fluidState = level.getFluidState(pos);
							if(!(isStrongExplosion && blockState.getBlock() instanceof LiquidBlock)) {
								Optional<Float> explosionResistance = damageCalculator.getBlockExplosionResistance(this, level, pos, blockState, fluidState);
								if(explosionResistance.isPresent()) {
									vecLength -= (explosionResistance.get() + 0.3f) * 0.3f * resistanceImpact;
								}
								if(vecLength > 0 && damageCalculator.shouldBlockExplode(this, level, pos, blockState, vecLength) && blockState.getMaterial() != Material.AIR) {
									blocks.add(pos);
								}
							}
							else {
								blocks.add(pos);
							}
						}
					}
				}
			}
		}
		for(BlockPos pos : blocks) {
			level.getBlockState(pos).getBlock().onBlockExploded(level.getBlockState(pos), level, pos, this);
		}
		if(fire) {
			for(BlockPos pos : blocks) {
				if(Math.random() > 0.75f && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos)) {
					level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
				}
			}
		}
	}
	
	public void doBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean isStrongExplosion, IForEachBlockExplosionEffect blockEffect) {
		Set<BlockPos> blocks = new HashSet<>();
		for(int offX = (int)-size; offX <= size; offX++) {
			for(int offY = (int)-size; offY <= size; offY++) {
				for(int offZ = (int)-size; offZ <= size; offZ++) {
					if(offX == (int)-size || offX == size || offY == (int)-size || offY == size || offZ == (int)-size || offZ == size) {
						double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float)Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for(float vecStep = 0; vecStep < vecLength; vecStep += 0.225f) {
							blockX += xStep * 0.3f * xzStrength;
							blockY += yStep * 0.3f * yStrength;
							blockZ += zStep * 0.3f * xzStrength;
							BlockPos pos = new BlockPos(blockX, blockY, blockZ);
							if(!level.isInWorldBounds(pos)) {
								break;
							}
							BlockState blockState = level.getBlockState(pos);
							FluidState fluidState = level.getFluidState(pos);
							if(!(isStrongExplosion && blockState.getBlock() instanceof LiquidBlock)) {
								Optional<Float> explosionResistance = damageCalculator.getBlockExplosionResistance(this, level, pos, blockState, fluidState);
								if(explosionResistance.isPresent()) {
									vecLength -= (explosionResistance.get() + 0.3f) * 0.3f * resistanceImpact;
								}
								if(vecLength > 0 && damageCalculator.shouldBlockExplode(this, level, pos, blockState, vecLength) && blockState.getMaterial() != Material.AIR) {
									blocks.add(pos);
								}
							}
							else {
								blocks.add(pos);
							}
						}
					}
				}
			}
		}
		for(BlockPos pos : blocks) {
			double distance = Math.sqrt(pos.distToLowCornerSqr(posX, posY, posZ));
			blockEffect.doBlockExplosion(level, pos, level.getBlockState(pos), distance);
		}
	}
	
	public void doBlockExplosion(IForEachBlockExplosionEffect blockEffect) {
		doBlockExplosion(1f, 1f, 1f, 1f, false, blockEffect);
	}
	
	public void doEntityExplosion(float knockbackStrength, boolean damageEntities) {
		List<Entity> entities = level.getEntities(getExploder(), new AABB(posX - size * 2, posY - size * 2, posZ - size * 2, posX + size * 2, posY + size * 2, posZ + size * 2));
		ForgeEventFactory.onExplosionDetonate(level, this, entities, size*2);
		for(Entity entity : entities) {
			if(!entity.ignoreExplosion()) {
				double distance = Math.sqrt(entity.distanceToSqr(getPosition())) / (size * 2);
				if(distance < 1f && distance != 0) {
					double offX = (entity.getX() - posX);
					double offY = (entity.getEyeY() - posY);
					double offZ = (entity.getZ() - posZ);
					double distance2 = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					offX /= distance2;
					offY /= distance2;
					offZ /= distance2;
					double seenPercent = getSeenPercent(getPosition(), entity);
					float damage = (1f - (float)distance) * (float)seenPercent;
					if(damageEntities) {
						entity.hurt(getDamageSource(), (damage * damage + damage) * 7 * size + 1f);
					}
					double knockback = damage;
					if(entity instanceof LivingEntity lEnt) {
						knockback = ProtectionEnchantment.getExplosionKnockbackAfterDampener(lEnt, damage);
					}
					entity.setDeltaMovement(entity.getDeltaMovement().add(offX * knockback * knockbackStrength, offY * knockback * knockbackStrength, offZ * knockback * knockbackStrength));
					if(entity instanceof Player player) {
						if(!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
							getHitPlayers().put(player, new Vec3(offX * damage, offY * damage, offZ * damage));
						}
					}
				}
			}
		}
	}

	@Override
	@Deprecated
	public void explode() {
	}
	
	@Override
	@Deprecated
	public void finalizeExplosion(boolean spawnParticles) {		
	}
	
	@Override
	@Deprecated
	@Nullable
	public List<BlockPos> getToBlow(){
		return null;
	}
}