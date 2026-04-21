package luckytntlib.network;

import java.util.BitSet;
import java.util.function.Supplier;

import luckytntlib.client.ClientAccess;
import luckytntlib.util.explosions.ExplosionHelper;
import luckytntlib.util.explosions.ImprovedExplosion;
import luckytntlib.util.light.LightUpdateHelper;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Packet for updating a specific {@link LevelChunkSection} by either changing the blocks at the specified positions or queuing light updates for said positions. <br>
 * Only used in {@link ExplosionHelper}, {@link LightUpdateHelper} and {@link ImprovedExplosion} to transmit changes to the world from server to client.
 */
public class ClientboundUpdateChunkSectionPacket {

	private final BitSet changed;
	private final boolean allAffected, updateLight;
	private final int sectionX, sectionY, sectionZ;
	
	public ClientboundUpdateChunkSectionPacket(SectionPos pos, BitSet changed, boolean allAffected, boolean updateLight) {
		this.sectionX = pos.getX();
		this.sectionY = pos.getY();
		this.sectionZ = pos.getZ();
		this.allAffected = allAffected;
		this.updateLight = updateLight;
		this.changed = changed;
	}
	
	public ClientboundUpdateChunkSectionPacket(FriendlyByteBuf buffer) {
		sectionX = buffer.readInt();
		sectionY = buffer.readInt();
		sectionZ = buffer.readInt();
		allAffected = buffer.readBoolean();
		updateLight = buffer.readBoolean();
		
		if (!allAffected) {
			changed = buffer.readBitSet();
		} else {
			changed = new BitSet(0);
		}
	}
	
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(sectionX);
		buffer.writeInt(sectionY);
		buffer.writeInt(sectionZ);
		buffer.writeBoolean(allAffected);
		buffer.writeBoolean(updateLight);
		
		if (!allAffected) {
			buffer.writeBitSet(changed);
		}
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			SectionPos pos = SectionPos.of(sectionX, sectionY, sectionZ);
			if (updateLight) {
				ClientAccess.updateChunkSectionLight(pos, changed);
			} else {
				ClientAccess.updateChunkSection(pos, changed, allAffected);
			}
		}));
		ctx.get().setPacketHandled(true);
	}
}
