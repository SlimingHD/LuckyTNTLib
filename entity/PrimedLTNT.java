package luckytntlib.entity;

import javax.annotation.Nullable;

import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.PrimedTNTEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class PrimedLTNT extends PrimedTnt implements IExplosiveEntity{

	@Nullable
	private LivingEntity igniter;
	private PrimedTNTEffect effect;
	
	public PrimedLTNT(EntityType<PrimedLTNT> type, Level level, PrimedTNTEffect effect) {
		super(type, level);
		this.effect = effect;
	}
	
	@Override
	public Packet<?> getAddEntityPacket(){
		return NetworkHooks.getEntitySpawningPacket(this);
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
	public Vec3 getPos() {
		return getPosition(1);
	}

	@Override
	public void destroy() {
		discard();
	}
}
