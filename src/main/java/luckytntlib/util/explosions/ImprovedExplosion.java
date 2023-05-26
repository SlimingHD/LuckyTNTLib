package luckytntlib.util.explosions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import luckytntlib.config.LuckyTNTLibConfigValues;
import luckytntlib.util.IExplosiveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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

/**
 * ImprovedExplosion is an extension of Minecraft's {@link Explosion}.
 * It is needed because the explosion of minecraft is rather limited in functionality and size,
 * while an ImprovedExplosion has no limit in its size and offers multiple and dynamic ways to interact with and customize the explosion.
 */
public class ImprovedExplosion extends Explosion{

	public final Level level;
	public final double posX, posY, posZ;
	public final int size;
	public final ExplosionDamageCalculator damageCalculator;
	List<Integer> affectedBlocks = new ArrayList<>();
	
	private static ImprovedExplosion dummyExplosion;
	
	/**
	 * Creates a new ImprovedExplosion
	 * @implNote size must not be greater than 511 in most cases. See the respective doBlockExplosion method
	 * @param level  the level
	 * @param position  the center position of the explosion
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */
	public ImprovedExplosion(Level level, Vec3 position, int size) {
		this(level, null, null, position, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @implNote size must not be greater than 511 in most cases. See the respective doBlockExplosion method
	 * @param level  the level
	 * @param source  the DamageSource this explosion uses
	 * @param position  the center position of the explosion
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */
	public ImprovedExplosion(Level level, @Nullable DamageSource source, Vec3 position, int size) {
		this(level, null, source, position, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @implNote size must not be greater than 511 in most cases. See the respective doBlockExplosion method
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param position  the center position of the explosion
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */	
	public ImprovedExplosion(Level level, @Nullable Entity explodingEntity, Vec3 position, int size) {
		this(level, explodingEntity, null, position.x, position.y, position.z, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @implNote size must not be greater than 511 in most cases. See the respective doBlockExplosion method
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param source  the DamageSource this explosion uses
	 * @param position  the center position of the explosion
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */	
	public ImprovedExplosion(Level level, @Nullable Entity explodingEntity, @Nullable DamageSource source, Vec3 position, int size) {
		this(level, explodingEntity, source, position.x, position.y, position.z, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @implNote size must not be greater than 511 in most cases. See the respective doBlockExplosion method
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param x  the x center position
	 * @param y  the y center position
	 * @param z  the z center position
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */	
	public ImprovedExplosion(Level level, @Nullable Entity explodingEntity, double x, double y, double z, int size) {
		this(level, explodingEntity, null, x, y, z, size);
	}
	
	/**
	 * Creates a new ImprovedExplosion
	 * @implNote size must not be greater than 511 in most cases. See the respective doBlockExplosion method
	 * @param level  the level
	 * @param entity  the entity not affected by this explosion. Should be the entity causing the explosion and also an IExplosiveEntity
	 * @param source  the DamageSource this explosion uses
	 * @param x  the x center position
	 * @param y  the y center position
	 * @param z  the z center position
	 * @param size  the rough size of the explosion, which must not be greater than 511 in most cases
	 */	
	public ImprovedExplosion(Level level, @Nullable Entity explodingEntity, @Nullable DamageSource source, double x, double y, double z, int size) {
		super(level, explodingEntity, source, null, x, y, z, size, false, BlockInteraction.KEEP);
		this.level = level;
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.size = size;
		damageCalculator = explodingEntity == null ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(explodingEntity);
	}
	
	/**
	 * Gets all blocks in an area calculated by shooting vectors to the borders of a cube determined by the {@link ImprovedExplosion#size} and destroys them.
	 * If any of the relative coordinates of the affected block exceed 511 they will be clamped to that value.
	 * Encodes block positions into a singular int, increasing performance.
	 * The shape the vectors orient to can either be a sphere or a cube, depending on the players config.
	 * @param xzStrength  a multiplier to the x and z vector addition, which makes the explosion more powerful. It should not be set higher than 1.2, otherwise blocks might be skipped
	 * @param yStrength  a multiplier to the y vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param fire  whether or not the explosion should spawn fire afterwards
	 * @param isStrongExplosion  whether or not fluids should be ignored in the explosion resistance calculation. Very useful for large explosions
	 */
	public void doBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean fire, boolean isStrongExplosion) {			
		BlockPos posTNT = new BlockPos(Mth.floor(posX), Mth.floor(posY), Mth.floor(posZ));
		Set<Integer> blocks = new HashSet<>();
		for (int offX = -size; offX <= size; offX++) {
			for (int offY = -size; offY <= size; offY++) {
				for (int offZ = -size; offZ <= size; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if (((int) distance == size && LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get()) || (!LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get() && (offX == -size || offX == size || offY == -size || offY == size || offZ == -size || offZ == size))) {
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float) Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for (float vecStep = 0; vecStep < vecLength; vecStep += LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 1.5f - 0.225f) {
							blockX += xStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							blockY += yStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * yStrength;
							blockZ += zStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							BlockPos pos = new BlockPos((int)blockX, (int)blockY, (int)blockZ);
							if (!level.isInWorldBounds(pos)) {
								break;
							}
							BlockState blockState = level.getBlockState(pos);
							FluidState fluidState = level.getFluidState(pos);
							if (!(isStrongExplosion && blockState.getBlock() instanceof LiquidBlock)) {
								Optional<Float> explosionResistance = damageCalculator.getBlockExplosionResistance(this, level, pos, blockState, fluidState);
								if (explosionResistance.isPresent()) {
									vecLength -= (explosionResistance.get() + 0.3f) * 0.3f * resistanceImpact;
								}
								if (vecLength > 0 && damageCalculator.shouldBlockExplode(this, level, pos, blockState, vecLength) && blockState.getMaterial() != Material.AIR) {
									blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
								}
							} else {
								blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
							}
						}
					}
				}
			}
		}
		affectedBlocks.addAll(blocks);
		for(int intPos : blocks) {
			BlockPos pos = decodeBlockPos(intPos).offset(posTNT);
			level.getBlockState(pos).getBlock().onBlockExploded(level.getBlockState(pos), level, pos, this);
		}
		if(fire) {
			for(int intPos : blocks) {
				BlockPos pos = decodeBlockPos(intPos).offset(posTNT);
				if(Math.random() > 0.75f && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos)) {
					level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
				}
			}
		}
	}
	
	/**
	 * Gets all blocks in an area calculated by shooting vectors to the borders of a cube determined by the {@link ImprovedExplosion#size} 
	 * and does to them whatever specified in the {@link IForEachBlockExplosionEffect}. 
	 * If any of the relative coordinates of the affected block exceed 511 they will be clamped to that value.
	 * Encodes block positions into a singular int, increasing performance.
	 * The shape the vectors orient to can either be a sphere or a cube, depending on the players config.
	 * @param xzStrength  a multiplier to the x and z vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param yStrength  a multiplier to the y vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param fire  whether or not the explosion should spawn fire afterwards
	 * @param isStrongExplosion  whether or not fluids should be ignored in the explosion resistance calculation. Very useful for large explosions
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 */
	public void doBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean isStrongExplosion, IForEachBlockExplosionEffect blockEffect) {
		BlockPos posTNT = new BlockPos(Mth.floor(posX), Mth.floor(posY), Mth.floor(posZ));
		Set<Integer> blocks = new HashSet<>();
		for(int offX = (int)-size; offX <= (int)size; offX++) {
			for(int offY = (int)-size; offY <= (int)size; offY++) {
				for(int offZ = (int)-size; offZ <= (int)size; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(((int)distance == (int)size && LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get()) || (!LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get() && (offX == (int)-size || offX == (int)size || offY == (int)-size || offY == (int)size || offZ == (int)-size || offZ == (int)size))) {
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float)Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for(float vecStep = 0; vecStep < vecLength; vecStep += LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 1.5f - 0.225f) {
							blockX += xStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							blockY += yStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * yStrength;
							blockZ += zStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							BlockPos pos = new BlockPos((int)blockX, (int)blockY, (int)blockZ);
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
									blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
								}
							}
							else {
								blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
							}
						}
					}
				}
			}
		}
		affectedBlocks.addAll(blocks);
		for(int intPos : blocks) {
			BlockPos pos = decodeBlockPos(intPos).offset(posTNT);
			double distance = Math.sqrt(pos.distToLowCornerSqr(posX, posY, posZ));
			blockEffect.doBlockExplosion(level, pos, level.getBlockState(pos), distance);
		}
	}
	
	/**
	 * Gets blocks in an area calculated by shooting vectors to the borders of a cube determined by the {@link ImprovedExplosion#size} if the {@link IBlockExplosionCondition} is met 
	 * and does to them whatever specified in the blockEffect.
	 * If any of the relative coordinates of the affected block exceed 511 they will be clamped to that value.
	 * Encodes block positions into a singular int, increasing performance.
	 * The shape the vectors orient to can either be a sphere or a cube, depending on the players config.
	 * @param xzStrength  a multiplier to the x and z vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param yStrength  a multiplier to the y vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param fire  whether or not the explosion should spawn fire afterwards
	 * @param isStrongExplosion  whether or not fluids should be ignored in the explosion resistance calculation. Very useful for large explosions
	 * @param condition  the condition on which a block is added to the {@link Set} of blocks
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 */
	public void doBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean isStrongExplosion, IBlockExplosionCondition condition, IForEachBlockExplosionEffect blockEffect) {
		BlockPos posTNT = new BlockPos(Mth.floor(posX), Mth.floor(posY), Mth.floor(posZ));
		Set<Integer> blocks = new HashSet<>();
		for(int offX = (int)-size; offX <= (int)size; offX++) {
			for(int offY = (int)-size; offY <= (int)size; offY++) {
				for(int offZ = (int)-size; offZ <= (int)size; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(((int)distance == (int)size && LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get()) || (!LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get() && (offX == (int)-size || offX == (int)size || offY == (int)-size || offY == (int)size || offZ == (int)-size || offZ == (int)size))) {
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float)Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for(float vecStep = 0; vecStep < vecLength; vecStep += LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 1.5f - 0.225f) {
							blockX += xStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							blockY += yStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * yStrength;
							blockZ += zStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							BlockPos pos = new BlockPos((int)blockX, (int)blockY, (int)blockZ);
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
									if(condition.conditionMet(level, pos, blockState, distance)) {
										blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
									}
								}
							}
							else {
								if(condition.conditionMet(level, pos, blockState, distance)) {
									blocks.add(encodeBlockPos(pos.subtract(posTNT).getX(), pos.subtract(posTNT).getY(), pos.subtract(posTNT).getZ()));
								}
							}
						}
					}
				}
			}
		}
		affectedBlocks.addAll(blocks);
		for(int intPos : blocks) {
			BlockPos pos = decodeBlockPos(intPos).offset(posTNT);
			double distance = Math.sqrt(pos.distToLowCornerSqr(posX, posY, posZ));
			blockEffect.doBlockExplosion(level, pos, level.getBlockState(pos), distance);
		}
	}
	
	/**
	 * Executes {@link ImprovedExplosion#doBlockExplosion(float, float, float, float, boolean, boolean, blockEffect)} with default values.
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 * 
	 */
	public void doBlockExplosion(IForEachBlockExplosionEffect blockEffect) {
		doBlockExplosion(1f, 1f, 1f, 1f, false, blockEffect);
	}
	
	/**
	 * Executes {@link ImprovedExplosion#doBlockExplosion(float, float, float, float, boolean, boolean, condition, blockEffect)} with default values.
	 * @param blockEffect  determines what should happen to the blocks gotten by this explosion
	 */
	public void doBlockExplosion(IBlockExplosionCondition condition, IForEachBlockExplosionEffect blockEffect) {
		doBlockExplosion(1f, 1f, 1f, 1f, false, condition, blockEffect);
	}
	
	/**
	 * Executes {@link ImprovedExplosion#doBlockExplosion(float, float, float, float, boolean, boolean)} with default values.
	 */
	public void doBlockExplosion() {
		doBlockExplosion(1f, 1f, 1f, 1f, false, false);
	}
	
	/**
	 * Gets all blocks in an area calculated by shooting vectors to the borders of a cube determined by the {@link ImprovedExplosion#size} and destroys them.
	 * Values of the relative coordinates can exceed 511, allowing for bigger explosions at the cost of more ram usage and slower explosion time.
	 * @param xzStrength  a multiplier to the x and z vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param yStrength  a multiplier to the y vector addition, which makes the explosion more powerful. It should not be set to high, otherwise blocks might be skipped
	 * @param resistanceImpact  the relative impact that explosion resistance of blocks has on the penetration force of explosion
	 * @param randomVecLength  the greater this value, the more distributed the length of the explosion vectors will be. Large explosions should have a value less than 1
	 * @param fire  whether or not the explosion should spawn fire afterwards
	 * @param isStrongExplosion  whether or not fluids should be ignored in the explosion resistance calculation. Very useful for large explosions
	 * @param saveBlockPos  whether or not affected blocks should be saved to be used externally
	 */
	public void doOldBlockExplosion(float xzStrength, float yStrength, float resistanceImpact, float randomVecLength, boolean fire, boolean isStrongExplosion, boolean saveBlockPos) {
		Set<BlockPos> blocks = new HashSet<>();
		for(int offX = (int)-size; offX <= (int)size; offX++) {
			for(int offY = (int)-size; offY <= (int)size; offY++) {
				for(int offZ = (int)-size; offZ <= (int)size; offZ++) {
					double distance = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
					if(((int)distance == (int)size && LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get()) || (!LuckyTNTLibConfigValues.PERFORMANT_EXPLOSION.get() && (offX == (int)-size || offX == (int)size || offY == (int)-size || offY == (int)size || offZ == (int)-size || offZ == (int)size))) {
						double xStep = offX / distance;
						double yStep = offY / distance;
						double zStep = offZ / distance;
						float vecLength = size * (0.7f + (float)Math.random() * 0.6f * randomVecLength);
						double blockX = posX;
						double blockY = posY;
						double blockZ = posZ;
						for(float vecStep = 0; vecStep < vecLength; vecStep += LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * 1.5f - 0.225f) {
							blockX += xStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							blockY += yStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * yStrength;
							blockZ += zStep * LuckyTNTLibConfigValues.EXPLOSION_PERFORMANCE_FACTOR.get() * xzStrength;
							BlockPos pos = new BlockPos((int)blockX, (int)blockY, (int)blockZ);
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
		if(saveBlockPos) {
			for(BlockPos pos : blocks) {
				affectedBlocks.add(encodeBlockPos((int)Math.round(pos.getX() - posX), (int)Math.round(pos.getY() - posY), (int)Math.round(pos.getZ() - posZ)));
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
	
	/**
	 * Encodes 3 coordinates into a singular int value. 
	 * Coordinates greater than the absolute value of 511 will be clamped to 511.
	 * @implNote coordinates given must be realtive coordinates to the center of the explosion
	 * @param x  the x position of the block
	 * @param y  the y position of the block
	 * @param z  the z position of the block
	 * @return encoded int containing information about x, y and z positions, all of which can have values between -511 and 511
	 */
	protected int encodeBlockPos(int x, int y, int z) {
		int x0 = Integer.signum(x);
		x = Math.abs(x) > 511 ? 511 : Math.abs(x);
		x0 = x0 == -1 ? 0b1000000000 : 0;		
		x += x0;
		
		x = x << 20;
		
		int y0 = Integer.signum(y);
		y = Math.abs(y) > 511 ? 511 : Math.abs(y);
		y0 = y0 == -1 ? 0b1000000000 : 0;
		y += y0;

		y = y << 10;
		
		int z0 = Integer.signum(z);
		z = Math.abs(z) > 511 ? 511 : Math.abs(z);
		z0 = z0 == -1 ? 0b1000000000 : 0;
		z += z0;
		
		return (x + y + z);
	}
	
	/**
	 * Decodes an encoded value generated by {@link ImprovedExplosion#encodeBlockPos(int, int, int)} into a {@link BlockPos}.
	 * @param encodedVal  the position encoded by {@link ImprovedExplosion#encodeBlockPos(int, int, int)}
	 * @return BlockPos with the relative x, y and z coordinates decoded again with an absolute max value of 511
	 */
	protected BlockPos decodeBlockPos(int encodedVal) {
		int zRaw = (encodedVal & 0b00000000000000000000000111111111);
		int zNeg = (encodedVal & 0b00000000000000000000001000000000) >> 9;
		int yRaw = (encodedVal & 0b00000000000001111111110000000000) >> 10;
		int yNeg = (encodedVal & 0b00000000000010000000000000000000) >> 19;
		int xRaw = (encodedVal & 0b00011111111100000000000000000000) >> 20;
		int xNeg = (encodedVal & 0b00100000000000000000000000000000) >> 29;
		int xVal = xNeg == 1 ? -xRaw : xRaw;
		int yVal = yNeg == 1 ? -yRaw : yRaw;
		int zVal = zNeg == 1 ? -zRaw : zRaw;
		return new BlockPos(xVal, yVal, zVal);
	}
	
	/**
	 * Damages and throws back all entities affected by this explosion determined by the {@link ImprovedExplosion#size}.
	 * @param knockbackStrength  multiplier to the strength of the knockback
	 * @param damageEntities  whether or not entities should be damaged by this explosion
	 */
	public void doEntityExplosion(float knockbackStrength, boolean damageEntities) {
		List<Entity> entities = level.getEntities(getExploder(), new AABB(posX - size * 2, posY - size * 2, posZ - size * 2, posX + size * 2, posY + size * 2, posZ + size * 2));
		ForgeEventFactory.onExplosionDetonate(level, this, entities, size * 2);
		for(Entity entity : entities) {
			if(!entity.ignoreExplosion()) {
				double distance = Math.sqrt(entity.distanceToSqr(getPosition())) / (size * 2);
				if(distance <= 1f) {
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
						entity.hurt(getDamageSource(), (damage * damage + damage) / 2f * 7 * size + 1f);
					}
					double knockback = damage;
					if(entity instanceof LivingEntity lEnt) {
						knockback = ProtectionEnchantment.getExplosionKnockbackAfterDampener(lEnt, damage);
					}
					entity.setDeltaMovement(entity.getDeltaMovement().add(offX * knockback * knockbackStrength, offY * knockback * knockbackStrength, offZ * knockback * knockbackStrength));
					if(entity instanceof Player) {
						Player player = (Player)entity;
						player.hurtMarked = true;
						if(!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
							getHitPlayers().put(player, new Vec3(offX * damage, offY * damage, offZ * damage));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Does whatever specified in the {@link IForEachBlockExplosionEffect} to all entities gotten by this explosion,
	 * which is determined by the {@link ImprovedExplosion#size}.
	 * @param entityEffect  determines what should be done to the entities gotten by this explosion
	 */
	public void doEntityExplosion(IForEachEntityExplosionEffect entityEffect) {
		List<Entity> entities = level.getEntities(getExploder(), new AABB(posX - size * 2, posY - size * 2, posZ - size * 2, posX + size * 2, posY + size * 2, posZ + size * 2));
		ForgeEventFactory.onExplosionDetonate(level, this, entities, size * 2);
		for(Entity entity : entities) {
			if(!entity.ignoreExplosion()) {
				double distance = Math.sqrt(entity.distanceToSqr(getPosition())) / (size * 2);
				if(distance < 1f && distance != 0) {
					entityEffect.doEntityExplosion(entity, distance);
				}
			}
		}
	}
	
	@Nullable
	@Override
	public LivingEntity getIndirectSourceEntity() {
		if(getExploder() instanceof IExplosiveEntity ent) {
			return ent.owner();
		}
		return super.getIndirectSourceEntity();
	}
	
	/** 
	 * @implNote Must	 not be used to create an actual explosion!
	 * @return ImprovedExplosion with no strength and position at (0, 0, 0)
	 */
	public static ImprovedExplosion dummyExplosion(Level level) {
		return dummyExplosion == null ? dummyExplosion = new ImprovedExplosion(level, new Vec3(0, 0, 0), 0) : dummyExplosion;
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
	@Nullable
	public List<BlockPos> getToBlow(){
		List<BlockPos> blocks = new ArrayList<>();
		for(int intPos : affectedBlocks) {
			blocks.add(new BlockPos(decodeBlockPos(intPos)).offset(Mth.floor(posX), Mth.floor(posY), Mth.floor(posZ)));
		}
		return blocks;
	}
}
