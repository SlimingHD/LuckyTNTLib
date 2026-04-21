package luckytntlib.util.explosions;

import java.util.Set;

import luckytntlib.network.ClientboundUpdateHeightmapsPacket;
import luckytntlib.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.PacketDistributor;

/**
 * Updates the heightmaps after an explosion that doesn't use {@link Level#setBlock(BlockPos, BlockState, int)}, only sending the necessary information to the client, not the whole chunk.
 */
public class HeightmapUpdateHelper {

	public static final Set<Heightmap.Types> TYPES = Set.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE);
	
	public static void updateHeightmaps(Level level, Set<LevelChunk> chunks) {
		if (level.isClientSide()) {
			return;
		}
		for (LevelChunk chunk : chunks) {
			Heightmap.primeHeightmaps(chunk, TYPES);
			PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientboundUpdateHeightmapsPacket(chunk));
		}
	}
}
