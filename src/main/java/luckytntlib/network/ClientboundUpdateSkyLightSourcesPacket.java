package luckytntlib.network;

import java.util.function.Supplier;

import luckytntlib.client.ClientAccess;
import luckytntlib.util.light.LightUpdateHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet that updates the {@link ChunkSkyLightSources} for a specific chunk.
 * This is necessary to ensure future light updates work correctly. <br>
 * Only used in {@link LightUpdateHelper}.
 */
public class ClientboundUpdateSkyLightSourcesPacket {

	private final ChunkPos pos;
	private final int[] data;
	
	public ClientboundUpdateSkyLightSourcesPacket(ChunkPos pos, int[] data) {
		this.pos = pos;
		this.data = data;
	}
	
	public ClientboundUpdateSkyLightSourcesPacket(FriendlyByteBuf buffer) {
		pos = new ChunkPos(buffer.readInt(), buffer.readInt());
		data = buffer.readVarIntArray();
	}
	
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(pos.x);
		buffer.writeInt(pos.z);
		buffer.writeVarIntArray(data);
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAccess.updateChunkSkyLightSources(pos, data)));
		ctx.get().setPacketHandled(true);
	}
}
