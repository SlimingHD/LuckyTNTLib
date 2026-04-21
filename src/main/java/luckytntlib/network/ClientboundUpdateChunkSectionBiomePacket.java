package luckytntlib.network;

import java.util.BitSet;
import java.util.function.Supplier;

import luckytntlib.client.ClientAccess;
import luckytntlib.util.BiomeSetter;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet for updating a specific {@link LevelChunkSection} by changing the biome at the specified positions. <br>
 * Only used in {@link BiomeSetter} to transmit changes to the world from server to client.
 */
public class ClientboundUpdateChunkSectionBiomePacket {

	private final int sectionX, sectionY, sectionZ;
	private final BitSet changed;
	private final ResourceKey<Biome> biomeKey;
	
	public ClientboundUpdateChunkSectionBiomePacket(SectionPos pos, BitSet changed, ResourceKey<Biome> biomeKey) {
		this.sectionX = pos.getX();
		this.sectionY = pos.getY();
		this.sectionZ = pos.getZ();
		this.changed = changed;
		this.biomeKey = biomeKey;
	}
	
	public ClientboundUpdateChunkSectionBiomePacket(FriendlyByteBuf buffer) {
		sectionX = buffer.readInt();
		sectionY = buffer.readInt();
		sectionZ = buffer.readInt();
		changed = buffer.readBitSet();
		biomeKey = buffer.readResourceKey(Registries.BIOME);
	}
	
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(sectionX);
		buffer.writeInt(sectionY);
		buffer.writeInt(sectionZ);
		buffer.writeBitSet(changed);
		buffer.writeResourceKey(biomeKey);
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			SectionPos pos = SectionPos.of(sectionX, sectionY, sectionZ);
			ClientAccess.updateChunkSectionBiome(pos, changed, biomeKey);
		}));
		ctx.get().setPacketHandled(true);
	}
}
