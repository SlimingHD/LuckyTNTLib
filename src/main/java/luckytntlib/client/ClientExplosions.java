package luckytntlib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class ClientExplosions {
	
	@SuppressWarnings("resource")
	public static void playExplosionSoundAt(BlockPos pos) {
		Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER, 4f, (1f + (Minecraft.getInstance().level.random.nextFloat() - Minecraft.getInstance().level.random.nextFloat()) * 0.2f) * 0.7f);
	}
}
