package luckytntlib.util.explosions;

import luckytntlib.network.ClientboundExplosionPacket;
import luckytntlib.network.LuckyTNTLibPacketHandler;
import luckytntlib.util.IExplosiveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public abstract class PrimedTNTEffect extends ExplosiveEffect{
	
	@SuppressWarnings("resource")
	@Override
	public void baseTick(IExplosiveEntity entity) {
		if(entity.level().isClientSide) {
			spawnParticles(entity);
		}
		else if(entity.getTNTFuse() <= 0) {
			if(playsSound()) {
				LuckyTNTLibPacketHandler.CHANNEL.send(PacketDistributor.ALL.with(null), new ClientboundExplosionPacket("luckytntlib.client.ClientExplosions", "playExplosionSoundAt", new BlockPos(entity.getPos()), 0f, 0f, 0));
			}
			serverExplosion(entity);
			entity.destroy();
		}
		explosionTick(entity);
		entity.setTNTFuse(entity.getTNTFuse() - 1);
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
	
	public int getDefaultFuse() {
		return 80;
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
		return Blocks.TNT;
	}
}
