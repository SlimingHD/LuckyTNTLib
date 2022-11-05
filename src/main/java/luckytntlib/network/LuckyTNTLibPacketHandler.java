package luckytntlib.network;

import java.util.HashMap;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class LuckyTNTLibPacketHandler {
	
	public static final String PROTOCOL_VERSION = "1";
	
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("luckytntmod", "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
	
	public static final HashMap<String, Runnable> clientExplosion = new HashMap<>();
	
	private LuckyTNTLibPacketHandler() {	
	}
	
	public static void regiser() {
		CHANNEL.registerMessage(0, ClientboundExplosionPacket.class, ClientboundExplosionPacket::encode, ClientboundExplosionPacket::new, ClientboundExplosionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}
}
