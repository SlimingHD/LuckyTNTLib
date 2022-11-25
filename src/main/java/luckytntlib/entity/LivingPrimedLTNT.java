package luckytntlib.entity;

import javax.annotation.Nullable;

import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.explosions.PrimedTNTEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class LivingPrimedLTNT extends Monster implements IExplosiveEntity{
	
	@Nullable 
	private LivingEntity igniter;
	private PrimedTNTEffect effect;
	private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(LivingPrimedLTNT.class, EntityDataSerializers.INT);
	
	public LivingPrimedLTNT(EntityType<? extends Monster> type, Level level, @Nullable PrimedTNTEffect effect) {
		super(type, level);
		this.effect = effect;
	}

	@Override
	public Packet<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}
	
	@Override
	public void tick() {
		super.tick();
		effect.baseTick(this);
	}
		
	@Override
	public void defineSynchedData() {
		entityData.define(DATA_FUSE_ID, -1);
		super.defineSynchedData();
	}
	
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		if(igniter != null) {
			tag.putInt("throwerID", igniter.getId());
		}
		tag.putShort("Fuse", (short)getTNTFuse());
		super.addAdditionalSaveData(tag);
	}
	
	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		if(level.getEntity(tag.getInt("throwerID")) instanceof LivingEntity lEnt) {
			igniter = lEnt;
		}
		setTNTFuse(tag.getShort("Fuse"));
		super.readAdditionalSaveData(tag);
	}
	
	public void setOwner(@Nullable LivingEntity thrower) {
		this.igniter = thrower;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	public int getTNTFuse() {
		return entityData.get(DATA_FUSE_ID);
	}

	@Override
	public void setTNTFuse(int fuse) {
		entityData.set(DATA_FUSE_ID, fuse);
	}

	@Override
	public Level level() {
		return getLevel();
	}

	@Override
	public Vec3 getPos() {
		return getPosition(1);
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
	public void destroy() {
		discard();
	}

	@Override
	public PrimedTNTEffect getEffect() {
		return effect;
	}

	@Override
	public Entity owner() {
		return igniter;
	}
}
