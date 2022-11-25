package luckytntlib.util.explosions;

import luckytntlib.entity.LExplosiveProjectile;
import luckytntlib.entity.LivingPrimedLTNT;
import luckytntlib.entity.PrimedLTNT;
import luckytntlib.network.ClientboundExplosionPacket;
import luckytntlib.network.LuckyTNTLibPacketHandler;
import luckytntlib.util.IExplosiveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public abstract class PrimedTNTEffect extends ExplosiveEffect{
	
	@SuppressWarnings("resource")
	@Override
	public void baseTick(IExplosiveEntity entity) {
		if(entity instanceof PrimedLTNT ent) {
			if(ent.getTNTFuse() <= 0) {
				if(ent.level instanceof ServerLevel) {
					if(playsSound()) {
						LuckyTNTLibPacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundExplosionPacket("luckytntlib.client.ClientExplosions", "playExplosionSoundAt", new BlockPos(entity.getPos())));
					}
					serverExplosion(entity);
				}
				entity.destroy();
			}
			explosionTick(entity);
			entity.setTNTFuse(entity.getTNTFuse() - 1);
		}
		else if(entity instanceof LivingPrimedLTNT ent) {
			if(ent.getTNTFuse() <= 0) {
				if(ent.level instanceof ServerLevel) {
					if(playsSound()) {
						LuckyTNTLibPacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundExplosionPacket("luckytntlib.client.ClientExplosions", "playExplosionSoundAt", new BlockPos(entity.getPos())));
					}
					serverExplosion(entity);
				}
				entity.destroy();
			}
			explosionTick(entity);
			entity.setTNTFuse(entity.getTNTFuse() - 1);
		}
		else if(entity instanceof LExplosiveProjectile ent) {
			if((ent.inGround() || ent.hitEntity()) && entity.level() instanceof ServerLevel sLevel) {
				if(explodesOnImpact()) {
					ent.setTNTFuse(0);
				}
				if(ent.getTNTFuse() == 0) {
					if(ent.level instanceof ServerLevel) {
						if(playsSound()) {
							LuckyTNTLibPacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new ClientboundExplosionPacket("luckytntlib.client.ClientExplosions", "playExplosionSoundAt", new BlockPos(entity.getPos())));
						}
						serverExplosion(entity);
					}
					ent.destroy();
				}
			}
			if((ent.getTNTFuse() > 0 && airFuse()) || ent.hitEntity() || ent.inGround()) {
				explosionTick(ent);
				ent.setTNTFuse(ent.getTNTFuse() - 1);
			}
		}
		if(entity.level().isClientSide) {
			spawnParticles(entity);
		}
	}
	
	@Override
	public void spawnParticles(IExplosiveEntity entity) {
		entity.level().addParticle(ParticleTypes.SMOKE, entity.getPos().x(), entity.getPos().y + 0.5f, entity.getPos().z, 0, 0, 0);
	}
	
	@Override
	public void serverExplosion(IExplosiveEntity entity) {	
	}
	
	@Override
	public void explosionTick(IExplosiveEntity entity) {		
	}
	
	public boolean playsSound() {
		return true;
	}
	
	public int getDefaultFuse(IExplosiveEntity entity) {
		return 80;
	}
	
	@Override
	public float getSize() {
		return 1f;
	}
	
	@Override
	public boolean explodesOnImpact() {
		return true;
	}
	
	@Override
	public boolean airFuse() {
		return false;
	}
	
	@Override
	public ItemStack getItem() {
		return ItemStack.EMPTY;
	}
	
	@Override
	public BlockState getBlockState() {
		return getBlock().defaultBlockState();
	}
	
	@Override
	public Block getBlock() {
		return Blocks.TNT;
	}
}
