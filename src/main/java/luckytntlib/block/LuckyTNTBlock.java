package luckytntlib.block;

import java.util.List;

import javax.annotation.Nullable;

import luckytntlib.entity.PrimedLTNT;
import luckytntlib.registry.RegistryHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

/**
 * 
 * The LuckyTNTBlock is an extension of the {@link LTNTBlock} and serves the simple purpose of spawning a random
 * {@link PrimedLTNT} of a {@link LTNTBlock} contained in a {@link List}.
 * The list could for instance be set to one of the many lists of {@link LTNTBlock} found in the {@link RegistryHelper}.
 */
public class LuckyTNTBlock extends LTNTBlock{

	public List<RegistryObject<LTNTBlock>> TNTs;
	
	public LuckyTNTBlock(BlockBehaviour.Properties properties, List<RegistryObject<LTNTBlock>> TNTs) {
		super(properties, null, false);
		this.TNTs = TNTs;
	}
	
	/**
	 * Gets a random {@link LTNTBlock} from the list held by this block and calls its explode method.
	 * Can not explode another {@link LuckyTNTBlock}.
	 * @param level  the current level
	 * @param exploded  whether or not the block was destroyed by another explosion (used for randomized fuse)
	 * @param x  the x position
	 * @param y  the y position
	 * @param z  the z position
	 * @param igniter  the owner for the spawned TNT (used primarely for the {@link DamageSource})
	 * @return {@link PrimedLTNT} or null
	 * @throws UnsupportedOperationException
	 */
	@Override
	public PrimedLTNT explode(Level level, boolean exploded, double x, double y, double z, @Nullable LivingEntity igniter) throws UnsupportedOperationException{
		int rand = random.nextInt(TNTs.size());
		if(!(TNTs.get(rand).get() instanceof LuckyTNTBlock)) {
			return TNTs.get(rand).get().explode(level, exploded, x, y, z, igniter);
		}
		throw new UnsupportedOperationException("TNT block is a Lucky TNT itself");
	}
}
