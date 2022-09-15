package luckytntlib.entity;

import javax.annotation.Nullable;

import luckytntlib.util.DynamiteEffect;
import luckytntlib.util.IExplosiveEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class LDynamite extends AbstractArrow implements IExplosiveEntity{
	
	private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(LDynamite.class, EntityDataSerializers.INT);
	@Nullable
	private LivingEntity thrower;
	private boolean hitEntity = false;
	private DynamiteEffect effect;
	
	public LDynamite(EntityType<LDynamite> type, Level level, DynamiteEffect effect) {
		super(type, level);
		setFuse(effect.getDefaultFuse());
		pickup = AbstractArrow.Pickup.DISALLOWED;
		this.effect = effect;
	}
	
	@Override
	public Packet<?> getAddEntityPacket(){
		return NetworkHooks.getEntitySpawningPacket(this);
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
	public ItemStack getPickupItem() {
		return null;
	}
	
	public void setOwner(@Nullable LivingEntity thrower) {
		this.thrower = thrower;
	}
	
	public DynamiteEffect getEffect() {
		return effect;
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
	
	public void setFuse(int fuse) {
		entityData.set(DATA_FUSE_ID, fuse);
	}
	
	public int getFuse() {
		return entityData.get(DATA_FUSE_ID);
	}
	
	@Override
	public void defineSynchedData() {
		entityData.define(DATA_FUSE_ID, -1);
		super.defineSynchedData();
	}
	
	@Override
	@Nullable
	public LivingEntity getOwner() {
		return thrower;
	}
	
	@Override
	public void setOwner(Entity entity) {
		thrower = entity instanceof LivingEntity ? (LivingEntity) entity : thrower;
	}
	
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		if(thrower != null) {
			tag.putInt("throwerID", thrower.getId());
		}
		tag.putShort("Fuse", (short)getFuse());
		super.addAdditionalSaveData(tag);
	}
	
	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		if(level.getEntity(tag.getInt("throwerID")) instanceof LivingEntity lEnt) {
			thrower = lEnt;
		}
		setFuse(tag.getShort("Fuse"));
		super.readAdditionalSaveData(tag);
	}
	
	public boolean inGround() {
		return inGround;
	}
	
	public boolean hitEntity() {
		return hitEntity;
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
