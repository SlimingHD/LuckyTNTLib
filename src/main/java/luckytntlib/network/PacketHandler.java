package luckytntlib.network;

import java.util.Optional;

import luckytntlib.LuckyTNTLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {

	public static final String PROTOCOL_VERSION = "1";
	
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(LuckyTNTLib.MODID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
	
	private PacketHandler() {	
	}

	public static void register() {
		int index = 0;
		CHANNEL.registerMessage(index++, ClientboundUpdateChunkSectionPacket.class, ClientboundUpdateChunkSectionPacket::encode, ClientboundUpdateChunkSectionPacket::new, ClientboundUpdateChunkSectionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CHANNEL.registerMessage(index++, ClientboundUpdateSkyLightSourcesPacket.class, ClientboundUpdateSkyLightSourcesPacket::encode, ClientboundUpdateSkyLightSourcesPacket::new, ClientboundUpdateSkyLightSourcesPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CHANNEL.registerMessage(index++, ClientboundUpdateChunkSectionBiomePacket.class, ClientboundUpdateChunkSectionBiomePacket::encode, ClientboundUpdateChunkSectionBiomePacket::new, ClientboundUpdateChunkSectionBiomePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CHANNEL.registerMessage(index++, ClientboundUpdateHeightmapsPacket.class, ClientboundUpdateHeightmapsPacket::encode, ClientboundUpdateHeightmapsPacket::new, ClientboundUpdateHeightmapsPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CHANNEL.registerMessage(index++, ClientboundSetupExplosionPacket.class, ClientboundSetupExplosionPacket::encode, ClientboundSetupExplosionPacket::new, ClientboundSetupExplosionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}
}
