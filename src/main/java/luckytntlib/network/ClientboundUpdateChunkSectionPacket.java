package luckytntlib.network;

import java.util.BitSet;
import java.util.function.Supplier;

import luckytntlib.client.ClientAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundUpdateChunkSectionPacket {

	private final BitSet changed;
	private final boolean updateLight, empty;
	private final int sectionX, sectionY, sectionZ;
	
	public ClientboundUpdateChunkSectionPacket(SectionPos pos, BitSet changed, boolean empty, boolean updateLight) {
		this.sectionX = pos.getX();
		this.sectionY = pos.getY();
		this.sectionZ = pos.getZ();
		this.empty = empty;
		this.updateLight = updateLight;
		this.changed = changed;
	}
	
	public ClientboundUpdateChunkSectionPacket(FriendlyByteBuf buffer) {
		sectionX = buffer.readInt();
		sectionY = buffer.readInt();
		sectionZ = buffer.readInt();
		empty = buffer.readBoolean();
		updateLight = buffer.readBoolean();
		
		if(!empty) {
			changed = buffer.readBitSet();
		} else {
			changed = new BitSet(0);
		}
	}
	
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(sectionX);
		buffer.writeInt(sectionY);
		buffer.writeInt(sectionZ);
		buffer.writeBoolean(empty);
		buffer.writeBoolean(updateLight);
		
		if(!empty) {
			buffer.writeBitSet(changed);
		}
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAccess.updateChunkSection(SectionPos.of(sectionX, sectionY, sectionZ), changed, empty, updateLight));
		});
		ctx.get().setPacketHandled(true);
	}
}
