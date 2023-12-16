package luckytntlib.entity;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import luckytntlib.item.LTNTMinecartItem;
import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.tnteffects.PrimedTNTEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * The LTNTMinecart is an extension of Minecraft's {@link AbstractMinecart}
 * and can hold an already existing {@link PrimedLTNT} and its {@link PrimedTNTEffect}.
 * It implements {@link IExplosiveEntity}.
 */
public class LTNTMinecart extends Minecart implements IExplosiveEntity{

	private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(LTNTMinecart.class, EntityDataSerializers.INT);
	private boolean explodeInstantly;
	protected PrimedTNTEffect effect;
	protected Supplier<RegistryObject<LTNTMinecartItem>> pickItem;
	public LivingEntity placer;
	
	public LTNTMinecart(EntityType<LTNTMinecart> type, Level level, RegistryObject<EntityType<PrimedLTNT>> TNT, Supplier<RegistryObject<LTNTMinecartItem>> pickItem, boolean explodeInstantly) {
		super(type, level);
		if(TNT != null) {
			PrimedLTNT tnt = TNT.get().create(level);
			this.effect = tnt.getEffect();
			tnt.discard();
		}
		else if(!(this instanceof LuckyTNTMinecart)) {
			discard();
		}
		this.explodeInstantly = explodeInstantly;
		this.pickItem = pickItem;
		setTNTFuse(-1);
	}
	
	@Override
	public void tick() {
		super.tick();
		if(getTNTFuse() >= 0) {
			getEffect().baseTick(this);
		}
		if(horizontalCollision && getDeltaMovement().horizontalDistanceSqr() >= 0.01f && getTNTFuse() < 0) {
			if(explodesInstantly()) {
				fuse();
				setTNTFuse(0);
			}
			else {
				fuse();
			}
		}
	}

	@Override
	public InteractionResult interact(Player p_38483_, InteractionHand p_38484_) {
		return InteractionResult.PASS;
	}
	
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!this.level().isClientSide && !this.isRemoved()) {
			Entity entity = source.getDirectEntity();
			if (entity instanceof AbstractArrow abstractarrow) {
				if (abstractarrow.isOnFire() && getTNTFuse() < 0) {
					fuse();
				}
			}
			if(source.is(DamageTypes.LIGHTNING_BOLT) && getTNTFuse() >= 0) {
				return false;
			}
			if (this.isInvulnerableTo(source)) {
				return false;
			} else {
				this.setHurtDir(-this.getHurtDir());
				this.setHurtTime(10);
				this.markHurt();
				this.setDamage(this.getDamage() + amount * 10.0F);
				this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
				boolean flag = source.getEntity() instanceof Player && ((Player) source.getEntity()).getAbilities().instabuild;
				if (flag || this.getDamage() > 40.0F) {
					this.ejectPassengers();
					if (flag && !this.hasCustomName()) {
						this.discard();
					} else {
						this.destroy(source);
					}
				}

				return true;
			}
		} else {
			return true;
		}
	}
	
	@Override
	public void destroy(DamageSource source) {
		double speed = getDeltaMovement().horizontalDistanceSqr();
		if (!source.is(DamageTypes.ON_FIRE) && !source.is(DamageTypes.EXPLOSION) && !(speed >= 0.01f)) {
			super.destroy(source);
		} else {
			if(getTNTFuse() < 0) {
				if(explodesInstantly()) {
					fuse();
					Level level = level();
					setTNTFuse(getEffect().getDefaultFuse(this) / 4 + level.random.nextInt(getEffect().getDefaultFuse(this)) / 4);
				}
				else {
					fuse();
				}
			}
		}
	}
	
	@Override
	public boolean causeFallDamage(float ditance, float damage, DamageSource source) {
		if (ditance >= 3.0F && getTNTFuse() < 0) {
			if(explodesInstantly()) {
				fuse();
				setTNTFuse(0);
			}
			else {
				fuse();
			}
		}

		return super.causeFallDamage(ditance, damage, source);
	}
	
	@Override
	public void activateMinecart(int x, int y, int z, boolean active) {
		if(active && getTNTFuse() < 0) {
			fuse();
		}
	}

	public void fuse() {
		setTNTFuse(getEffect().getDefaultFuse(this));	
		level().playSound(null, new BlockPos((int)getPosition(1).x, (int)getPosition(1).y, (int)getPosition(1).z), SoundEvents.TNT_PRIMED, getSoundSource(), 1f, 1f);
	}
	
	@Override
	public void defineSynchedData() {
		entityData.define(DATA_FUSE_ID, -1);
		super.defineSynchedData();
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
		return new ItemStack(pickItem.get().get());
	}
	
	@Override
	protected Item getDropItem() {
		return pickItem.get().get();
	}
	
	@Override
	public BlockState getDisplayBlockState() {
		return getEffect().getBlock().defaultBlockState();
	}
	
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		if(placer != null) {
			tag.putInt("placerID", placer.getId());
		}
		tag.putShort("Fuse", (short)getTNTFuse());
		super.addAdditionalSaveData(tag);
	}
	
	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		if(level().getEntity(tag.getInt("placerID")) instanceof LivingEntity lEnt) {
			placer = lEnt;
		}
		setTNTFuse(tag.getShort("Fuse"));
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
	public LivingEntity owner() {
		return getOwner();
	}
}
