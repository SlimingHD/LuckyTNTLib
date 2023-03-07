package luckytntlib.entity;

import javax.annotation.Nullable;

import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.tnteffects.PrimedTNTEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 
 * A PrimedLTNT is an extension of Minecraft's {@link PrimedTnt}
 * and uses a {@link PrimedTNTEffect} to easily customize the explosion effect and other parameters.
 * It implements {@link IExplosiveEntity}.
 */
public class PrimedLTNT extends PrimedTnt implements IExplosiveEntity{

	@Nullable
	private LivingEntity igniter;
	private PrimedTNTEffect effect;
	
	public PrimedLTNT(EntityType<PrimedLTNT> type, Level level, PrimedTNTEffect effect) {
		super(type, level);
		this.effect = effect;
	    double movement = level.random.nextDouble() * (double)(Math.PI * 2F);
	    this.setDeltaMovement(-Math.sin(movement) * 0.02D, 0.2F, -Math.cos(movement) * 0.02D);
	    this.setTNTFuse(effect.getDefaultFuse(this));
	}
	
	@Override
	public SoundSource getSoundSource() {
		return SoundSource.MASTER;
	}
	
	public void setOwner(@Nullable LivingEntity igniter) {
		this.igniter = igniter;
	}
	
	@Override
	@Nullable
	public LivingEntity getOwner() {
		return igniter;
	}
	
	@Deprecated
	@Override
	public void explode() {
	}
	
	@Override
	public PrimedTNTEffect getEffect() {
		return effect;
	}
	
	@Override
	public void tick() {
		if (!isNoGravity()) {
			setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
			updateInWaterStateAndDoFluidPushing();
		}
		move(MoverType.SELF, getDeltaMovement());
		setDeltaMovement(getDeltaMovement().scale(0.98D));
		if (onGround) {
			setDeltaMovement(getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
		}
		effect.baseTick(this);
	}
	
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		if(igniter != null) {
			tag.putInt("igniterID", igniter.getId());
		}
		super.addAdditionalSaveData(tag);
	}
	
	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		if(level.getEntity(tag.getInt("igniterID")) instanceof LivingEntity lEnt) {
			igniter = lEnt;
		}
		super.readAdditionalSaveData(tag);
	}
	
	@Override
	public void setTNTFuse(int fuse) {
		setFuse(fuse);
	}
	
	@Override
	public int getTNTFuse() {
		return getFuse();
	}

	@Override
	public Vec3 getPos() {
		return getPosition(1);
	}

	@Override
	public void destroy() {
		discard();
	}
	
	@Override
	public Level level() {
		return level;
	}

	@Override
	public double x() {
		return getX();
	}

	@Override
	public double y() {
		return getY();
	}

	@Override
	public double z() {
		return getZ();
	}
	
	@Override
	public LivingEntity owner() {
		return getOwner();
	}
}
