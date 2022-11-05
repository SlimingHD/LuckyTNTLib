package luckytntlib.util.explosions;

import luckytntlib.network.ClientboundExplosionPacket;
import luckytntlib.network.LuckyTNTLibPacketHandler;
import luckytntlib.util.IExplosiveEntity;
import luckytntlib.util.IExplosiveProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public abstract class ExplosiveProjectileEffect extends ExplosiveEffect implements ItemSupplier{
	
	@SuppressWarnings("resource")
	@Override
	public void baseTick(IExplosiveEntity entity) {
		if(entity instanceof IExplosiveProjectileEntity ent) {
			if(entity.level().isClientSide) {
				spawnParticles(entity);
			}
			if((ent.inGround() || ent.hitEntity()) && entity.level() instanceof ServerLevel sLevel) {
				if(explodesOnImpact()) {
					ent.setTNTFuse(0);
				}
				if(ent.getTNTFuse() == 0) {
					if(playsSound()) {
						LuckyTNTLibPacketHandler.CHANNEL.send(PacketDistributor.ALL.with(null), new ClientboundExplosionPacket("luckytntlib.client.ClientExplosions", "playExplosionSoundAt", new BlockPos(entity.getPos()), 0f, 0f, 0));
					}
					serverExplosion(ent);
					ent.destroy();
				}
			}
			if((ent.getTNTFuse() > 0 && airFuse()) || ent.hitEntity() || ent.inGround()) {
				explosionTick(ent);
				ent.setTNTFuse(ent.getTNTFuse() - 1);
			}
		}
	}
	
	@Override	
	public void spawnParticles(IExplosiveEntity entity) {
		entity.level().addParticle(ParticleTypes.SMOKE, entity.getPos().x, entity.getPos().y + 0.5f, entity.getPos().z(), 0, 0, 0);
	}
	
	@Override	
	public void serverExplosion(IExplosiveEntity entity) {		
	}
	
	@Override	
	public void explosionTick(IExplosiveEntity entity) {		
	}
	
	public boolean airFuse() {
		return false;
	}
	
	public boolean explodesOnImpact() {
		return true;
	}
	
	public boolean playsSound() {
		return true;
	}

	public int getDefaultFuse() {
		return -1;
	}
	
	@Override	
	public float getSize() {
		return 1f;
	}
	
	@Override
	public BlockState getBlockState() {
		return getBlock().defaultBlockState();
	}
	
	@Override	
	public Block getBlock() {
		return Blocks.AIR;
	}
	
	@Override
	public ItemStack getItem() {
		return ItemStack.EMPTY;
	}
}
