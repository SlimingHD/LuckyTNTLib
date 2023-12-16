package luckytntlib.entity;

import javax.annotation.Nullable;

import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.tnteffects.PrimedTNTEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 
 * The LExplosiveProjectile is an extension of Minecraft's {@link AbstractArrow} 
 * and represents a projectile that holds a {@link PrimedTNTEffect}.
 * Unlike a {@link PrimedLTNT} a LExplosiveProjectile has access to other types of logic specifically designed
 * for entities that travel through the world with high speeds and hit blocks or entities, while still retaining the abilities of a TNT
 * through its {@link PrimedTNTEffect}.
 * It implements {@link IExplosiveEntity} and {@link ItemSupplier}.
 */
public class LExplosiveProjectile extends AbstractArrow implements IExplosiveEntity, ItemSupplier{
	
	private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(LExplosiveProjectile.class, EntityDataSerializers.INT);
	@Nullable
	private LivingEntity thrower;
	private boolean hitEntity = false;
	private PrimedTNTEffect effect;
	
	public LExplosiveProjectile(EntityType<LExplosiveProjectile> type, Level level, PrimedTNTEffect effect) {
		super(type, level, ItemStack.EMPTY);
		setTNTFuse(effect.getDefaultFuse(this));
		pickup = AbstractArrow.Pickup.DISALLOWED;
		this.effect = effect;
	}
	
	@Override
	public void onHitBlock(BlockHitResult hitResult) {
		Vec3 pos = hitResult.getLocation().subtract(this.getX(), this.getY(), this.getZ());
		setDeltaMovement(pos);
		Vec3 pos2 = pos.normalize().scale((double) 0.05F);
		setPosRaw(this.getX() - pos2.x, this.getY() - pos2.y, this.getZ() - pos2.z);
	    inGround = true;
	}
	
	@Override
	public void onHitEntity(EntityHitResult hitResult) {
		if(hitResult.getEntity() instanceof Player player) {
			if(!(player.isCreative() || player.isSpectator())) {
				hitEntity = true;
			}
		}
		else {
			hitEntity = true;
		}
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
		if(thrower != null) {
			tag.putInt("throwerID", thrower.getId());
		}
		tag.putShort("Fuse", (short)getTNTFuse());
		super.addAdditionalSaveData(tag);
	}
	
	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		if(level().getEntity(tag.getInt("throwerID")) instanceof LivingEntity lEnt) {
			thrower = lEnt;
		}
		setTNTFuse(tag.getShort("Fuse"));
		super.readAdditionalSaveData(tag);
	}
	
	public PrimedTNTEffect getEffect() {
		return effect;
	}
	
	public boolean inGround() {
		return inGround;
	}
	
	public boolean hitEntity() {
		return hitEntity;
	}
	
	@Override
	public void setTNTFuse(int fuse) {
		entityData.set(DATA_FUSE_ID, fuse);
	}
	
	public void setOwner(@Nullable LivingEntity thrower) {
		this.thrower = thrower;
	}
	
	@Override
	public void setOwner(Entity entity) {
		thrower = entity instanceof LivingEntity ? (LivingEntity) entity : thrower;
	}
	
	@Override
	@Nullable
	public LivingEntity getOwner() {
		return thrower;
	}
	
	@Override
	public ItemStack getPickupItem() {
		return null;
	}
	
	@Override
	public int getTNTFuse() {
		return entityData.get(DATA_FUSE_ID);
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
	public Level getLevel() {
		return level();
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
	public ItemStack getItem() {
		return effect.getItemStack();
	}
	
	@Override
	public LivingEntity owner() {
		return getOwner();
	}
}
