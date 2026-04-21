package luckytntlib.util;

import java.util.BitSet;

import luckytntlib.network.ClientboundUpdateChunkSectionBiomePacket;
import luckytntlib.network.PacketHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Sets the {@link Biome} in a specified area to the provided one, only sending the necessary information to the client, not the whole chunk,
 * making this method more efficient for massive edits.
 * <p>
 * This class supports basic shapes for spheres, cubes, and cylinders.
 */
public class BiomeSetter {

	public static void setBiomeInSphere(Level level, Vec3 center, int radius, ResourceKey<Biome> biomeKey) {
		if (level.isClientSide()) {
			return;
		}
		ServerLevel server = (ServerLevel)level;
		Registry<Biome> registry = server.registryAccess().registryOrThrow(Registries.BIOME);
		Holder<Biome> biome = registry.getHolderOrThrow(biomeKey);
		int secX = Mth.floor(center.x()) >> 4;
		int secY = Mth.floor(center.y()) >> 4;
		int secZ = Mth.floor(center.z()) >> 4;
		int maxDistanceSqr = radius * radius;
		int secRadius = radius >> 4;
		for (int offX = -secRadius; offX <= secRadius; offX++) {
			for (int offZ = -secRadius; offZ <= secRadius; offZ++) {
				int maxChunkXDistance = (Math.abs(offX) << 4) + 15;
				int maxChunkZDistance = (Math.abs(offZ) << 4) + 15;
				int chunkDistanceSqr = maxChunkXDistance * maxChunkXDistance + maxChunkZDistance * maxChunkZDistance;
				if (chunkDistanceSqr > maxDistanceSqr) {
					continue;
				}
				LevelChunk chunk = server.getChunk(secX + offX, secZ + offZ);
				for (int offY = -secRadius; offY <= secRadius; offY++) {
					int index = chunk.getSectionIndexFromSectionY(secY + offY);
					if (index >= 0 && index < chunk.getSectionsCount()) {
						LevelChunkSection section = chunk.getSection(index);
						BitSet changed = new BitSet(64);
						PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>)section.getBiomes();
						for (int x = 0; x < 4; x++) {
							for (int y = 0; y < 4; y++) {
								for (int z = 0; z < 4; z++) {
									int lX = (offX << 4) + (x << 2);
									int lY = (offY << 4) + (y << 2);
									int lZ = (offZ << 4) + (z << 2);
									int distanceSqr = lX * lX + lY * lY + lZ * lZ;
									if (distanceSqr > maxDistanceSqr) {
										continue;
									}
									if (biomes.get(x, y, z) != biome) {
										biomes.set(x, y, z, biome);
										changed.set((x << 4) | (y << 2) | z);
									}
								}
							}
						}
						if (changed.cardinality() != 0) {
							PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientboundUpdateChunkSectionBiomePacket(SectionPos.of(chunk.getPos(), index), changed, biomeKey));
						}
					}
				}
			}
		}
	}
	
	public static void setBiomeInCube(Level level, Vec3 center, int radius, ResourceKey<Biome> biomeKey) {
		if (level.isClientSide()) {
			return;
		}
		ServerLevel server = (ServerLevel)level;
		Registry<Biome> registry = server.registryAccess().registryOrThrow(Registries.BIOME);
		Holder<Biome> biome = registry.getHolderOrThrow(biomeKey);
		int secX = Mth.floor(center.x()) >> 4;
		int secY = Mth.floor(center.y()) >> 4;
		int secZ = Mth.floor(center.z()) >> 4;
		int secRadius = radius >> 4;
		for (int offX = -secRadius; offX <= secRadius; offX++) {
			for (int offZ = -secRadius; offZ <= secRadius; offZ++) {
				LevelChunk chunk = server.getChunk(secX + offX, secZ + offZ);
				for (int offY = -secRadius; offY <= secRadius; offY++) {
					int index = chunk.getSectionIndexFromSectionY(secY + offY);
					if (index >= 0 && index < chunk.getSectionsCount()) {
						LevelChunkSection section = chunk.getSection(index);
						BitSet changed = new BitSet(64);
						PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>)section.getBiomes();
						for (int x = 0; x < 4; x++) {
							for (int y = 0; y < 4; y++) {
								for (int z = 0; z < 4; z++) {
									int lX = (offX << 4) + (x << 2);
									int lY = (offY << 4) + (y << 2);
									int lZ = (offZ << 4) + (z << 2);
									if (Math.abs(lX) > radius || Math.abs(lY) > radius || Math.abs(lZ) > radius) {
										continue;
									}
									if (biomes.get(x, y, z) != biome) {
										biomes.set(x, y, z, biome);
										changed.set((x << 4) | (y << 2) | z);
									}
								}
							}
						}
						if (changed.cardinality() != 0) {
							PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientboundUpdateChunkSectionBiomePacket(SectionPos.of(chunk.getPos(), index), changed, biomeKey));
						}
					}
				}
			}
		}
	}
	
	public static void setBiomeInCylinder(Level level, Vec3 center, int radius, int radiusY, ResourceKey<Biome> biomeKey) {
		if (level.isClientSide()) {
			return;
		}
		ServerLevel server = (ServerLevel)level;
		Registry<Biome> registry = server.registryAccess().registryOrThrow(Registries.BIOME);
		Holder<Biome> biome = registry.getHolderOrThrow(biomeKey);
		int secX = Mth.floor(center.x()) >> 4;
		int secY = Mth.floor(center.y()) >> 4;
		int secZ = Mth.floor(center.z()) >> 4;
		int maxDistanceSqr = radius * radius;
		int secRadius = radius >> 4;
		int secRadiusY = radiusY >> 4;
		for (int offX = -secRadius; offX <= secRadius; offX++) {
			for (int offZ = -secRadius; offZ <= secRadius; offZ++) {
				int maxChunkXDistance = (Math.abs(offX) << 4) + 15;
				int maxChunkZDistance = (Math.abs(offZ) << 4) + 15;
				int chunkDistanceSqr = maxChunkXDistance * maxChunkXDistance + maxChunkZDistance * maxChunkZDistance;
				if (chunkDistanceSqr > maxDistanceSqr) {
					continue;
				}
				LevelChunk chunk = server.getChunk(secX + offX, secZ + offZ);
				for (int offY = -secRadiusY; offY <= secRadiusY; offY++) {
					int index = chunk.getSectionIndexFromSectionY(secY + offY);
					if (index >= 0 && index < chunk.getSectionsCount()) {
						LevelChunkSection section = chunk.getSection(index);
						BitSet changed = new BitSet(64);
						PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>)section.getBiomes();
						for (int x = 0; x < 4; x++) {
							for (int z = 0; z < 4; z++) {
								int lX = (offX << 4) + (x << 2);
								int lZ = (offZ << 4) + (z << 2);
								int distanceSqr = lX * lX + lZ * lZ;
								if (distanceSqr > maxDistanceSqr) {
									continue;
								}
								for (int y = 0; y < 4; y++) {
									int lY = (offY << 4) + (y << 2);
									if (Math.abs(lY) > radiusY) {
										continue;
									}
									if (biomes.get(x, y, z) != biome) {
										biomes.set(x, y, z, biome);
										changed.set((x << 4) | (y << 2) | z);
									}
								}
							}
						}
						if (changed.cardinality() != 0) {
							PacketHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new ClientboundUpdateChunkSectionBiomePacket(SectionPos.of(chunk.getPos(), index), changed, biomeKey));
						}
					}
				}
			}
		}
	}
}
