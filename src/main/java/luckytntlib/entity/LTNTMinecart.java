package luckytntlib.entity;

import javax.annotation.Nullable;

import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.tnteffects.PrimedTNTEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

public class LTNTMinecart extends AbstractMinecart implements IExplosiveEntity{

	private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(LExplosiveProjectile.class, EntityDataSerializers.INT);
	private boolean explodeInstantly;
	private PrimedTNTEffect effect;
	private RegistryObject<Item> drop;
	public LivingEntity placer;
	
	public LTNTMinecart(EntityType<? extends LTNTMinecart> type, Level level, PrimedTNTEffect effect, boolean explodeInstantly, RegistryObject<Item> drop) {
		super(type, level);
		this.effect = effect;
		this.explodeInstantly = explodeInstantly;
		this.drop = drop;
		setTNTFuse(-2);
	}
	
	@Override
	public void tick() {
		super.tick();
		if(getTNTFuse() > 0) {
			effect.baseTick(this);
		}
		if(horizontalCollision && getTNTFuse() < 0) {
			if(explodesInstantly()) {
				fuse();
				setTNTFuse(1);
			}
			else {
				fuse();
			}
		}
	}
	
	@Override
	public boolean hurt(DamageSource source, float amount) {
		Entity entity = source.getDirectEntity();
		if (entity instanceof AbstractArrow abstractarrow) {
			if (abstractarrow.isOnFire() && getTNTFuse() < 0) {
				fuse();
			}
		}
		return super.hurt(source, amount);
	}
	
	@Override
	public void destroy(DamageSource source) {
		double speed = getDeltaMovement().horizontalDistanceSqr();
		if (!source.isFire() && !source.isExplosion() && !(speed >= 0.01f)) {
			super.destroy(source);
		} else {
			if (this.getTNTFuse() < 0) {
				this.fuse();
				setTNTFuse(random.nextInt(effect.getDefaultFuse(this) / 4) + random.nextInt(effect.getDefaultFuse(this) / 4));
			}
		}
	}
	
	@Override
	public void activateMinecart(int x, int y, int z, boolean active) {
		if(active && getTNTFuse() < 0) {
			fuse();
		}
	}

	public void fuse() {
		setTNTFuse(effect.getDefaultFuse(this));	
		level.playSound(null, new BlockPos(getPosition(1)), SoundEvents.TNT_PRIMED, getSoundSource(), 1f, 1f);
	}
	
	@Nullable
	public LivingEntity getOwner() {
		return placer;
	}
	
	public void setOwner(LivingEntity owner) {
		this.placer = owner;
	}
	
	@Override
	public ItemStack getPickResult() {
		return null;
	}
	
	@Override
	protected Item getDropItem() {
		return drop.get();
	}
	
	@Override
	public BlockState getDisplayBlockState() {
		return effect.getBlock().defaultBlockState();
	}
	
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		if(placer != null) {
			tag.putInt("placerID", placer.getId());
		}
		tag.putShort("fuse", (short)getTNTFuse());
		super.addAdditionalSaveData(tag);
	}
	
	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		if(level.getEntity(tag.getInt("placerID")) instanceof LivingEntity lEnt) {
			placer = lEnt;
		}
		setTNTFuse(tag.getShort("fuse"));
		super.readAdditionalSaveData(tag);
	}
	
	@Override
	public Type getMinecartType() {
		return AbstractMinecart.Type.TNT;
	}
		
	public boolean explodesInstantly() {
		return explodeInstantly;
	}
	
	public PrimedTNTEffect getEffect() {
		return effect;
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
	public Entity owner() {
		return getOwner();
	}
}
