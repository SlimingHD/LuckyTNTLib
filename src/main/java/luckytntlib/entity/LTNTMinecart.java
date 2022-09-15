package luckytntlib.entity;

import javax.annotation.Nullable;

import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.PrimedTNTEffect;
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

	private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(LDynamite.class, EntityDataSerializers.INT);
	private boolean explodeInstantly;
	private PrimedTNTEffect effect;
	private RegistryObject<Item> drop;
	public LivingEntity placer;
	
	public LTNTMinecart(EntityType<? extends LTNTMinecart> type, Level level, PrimedTNTEffect effect, boolean explodeInstantly, RegistryObject<Item> drop) {
		super(type, level);
		this.effect = effect;
		this.explodeInstantly = explodeInstantly;
		this.drop = drop;
		setFuse(-2);
	}
	
	@Override
	public void tick() {
		super.tick();
		if(getFuse() > 0) {
			effect.baseTick(this);
		}
		if(horizontalCollision && getFuse() < 0) {
			if(explodesInstantly()) {
				fuse();
				setFuse(1);
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
			if (abstractarrow.isOnFire() && getFuse() < 0) {
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
			if (this.getFuse() < 0) {
				this.fuse();
				setFuse(random.nextInt(effect.getDefaultFuse() / 4) + random.nextInt(effect.getDefaultFuse() / 4));
			}
		}
	}
	
	@Override
	public void activateMinecart(int x, int y, int z, boolean active) {
		if(active && getFuse() < 0) {
			fuse();
		}
	}

	public void fuse() {
		setFuse(effect.getDefaultFuse());	
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
	
	public int getFuse() {
		return entityData.get(DATA_FUSE_ID);
	}
	
	public void setFuse(int fuse) {
		entityData.set(DATA_FUSE_ID, fuse);
	}
	
	@Override
	protected Item getDropItem() {
		return drop.get();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public BlockState getDisplayBlockState() {
		return effect.getBlock().defaultBlockState();
	}
	
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		if(placer != null) {
			tag.putInt("placerID", placer.getId());
		}
		tag.putShort("fuse", (short)getFuse());
		super.addAdditionalSaveData(tag);
	}
	
	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		if(level.getEntity(tag.getInt("placerID")) instanceof LivingEntity lEnt) {
			placer = lEnt;
		}
		setFuse(tag.getShort("fuse"));
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
	public Vec3 getPos() {
		return getPosition(1);
	}

	@Override
	public void destroy() {
		discard();
	}
}
