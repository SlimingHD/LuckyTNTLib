package luckytntlib.block;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import luckytntlib.entity.LivingPrimedLTNT;
import luckytntlib.entity.PrimedLTNT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * The LivingLTNTBlock is a simple extension of the {@link LTNTBlock} and only serves the purpose of hosting
 * a {@link LivingPrimedLTNT} instead of a {@link PrimedLTNT}.
 * This class is necessary because Minecraft's {@link LivingEntity} is fundamentally different from {@link PrimedTnt}.
 */
public class LivingLTNTBlock extends LTNTBlock{

	@Nullable
	protected RegistryObject<EntityType<LivingPrimedLTNT>> TNT;
	protected Random random = new Random();
	
	public LivingLTNTBlock(BlockBehaviour.Properties properties, @Nullable RegistryObject<EntityType<LivingPrimedLTNT>> TNT, boolean randomizedFuseUponExploded) {
		super(properties, null, randomizedFuseUponExploded);
		this.TNT = TNT;
	}
	
	@Override
	public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
		if(!level.isClientSide) {
			explode(level, false, pos.getX(), pos.getY(), pos.getZ(), igniter);
		}
	}
	
	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		return 0f;
	}
	
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return true;
	}
	
	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return 200;
	}
	
	@Override
	public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
		if(!level.isClientSide) {
			explode(level, true, pos.getX(), pos.getY(), pos.getZ(), explosion.getSourceMob());
		}
	}
	
	@Override
	public List<ItemStack> getDrops(BlockState state, LootContext.Builder lootBuilder) {
		return Collections.singletonList(new ItemStack(this));
	}
	
	@Deprecated //onBlockExploded does the same with the added benifit of a BlockState being given
	@Override
	public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
	}
	
	@Override
	@Nullable
	public PrimedLTNT explode(Level level, boolean exploded, double x, double y, double z, @Nullable LivingEntity igniter) {
		explodus(level, exploded, x, y, z, igniter);
		return null;
	}
	
	/**
	 * Spawns a new {@link LivingPrimedLTNT} held by this block
	 * @param level  the current level
	 * @param exploded  whether or not the block was destroyed by another explosion (used for randomized fuse)
	 * @param x  the x position
	 * @param y  the y position
	 * @param z  the z position
	 * @param igniter  the owner for the spawned TNT (used primarely for the {@link DamageSource})
	 * @return {@link LivingPrimedLTNT} or null
	 * @throws NullPointerException
	 */
	@Nullable
	public LivingPrimedLTNT explodus(Level level, boolean exploded, double x, double y, double z, @Nullable LivingEntity igniter) throws NullPointerException {
		if(TNT != null) {
			LivingPrimedLTNT tnt = TNT.get().create(level);
			tnt.setTNTFuse(exploded && randomizedFuseUponExploded() ? tnt.getEffect().getDefaultFuse(tnt) / 8 + random.nextInt(Mth.clamp(tnt.getEffect().getDefaultFuse(tnt) / 4, 1, Integer.MAX_VALUE)) : tnt.getEffect().getDefaultFuse(tnt));
			tnt.setPos(x + 0.5f, y, z + 0.5f);
			tnt.setOwner(igniter);
			level.addFreshEntity(tnt);
			level.playSound(null, new BlockPos(x, y, z), SoundEvents.TNT_PRIMED, SoundSource.MASTER, 1, 1);
			if(level.getBlockState(new BlockPos(x, y, z)).getBlock() == this) {
				level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
			}
			return tnt;
		}
		throw new NullPointerException("Living TNT entity type is null");
	}
}
