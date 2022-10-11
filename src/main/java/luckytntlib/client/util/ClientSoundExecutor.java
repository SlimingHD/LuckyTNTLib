package luckytntlib.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ClientSoundExecutor {

	public static void playSoundAt(double x, double y, double z, SoundEvent sound, SoundSource source, float strength, float pitch) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, x, y, z, sound, source, strength, pitch));
	}
}
