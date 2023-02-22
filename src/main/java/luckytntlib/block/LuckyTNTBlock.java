package luckytntlib.block;

import java.util.List;

import javax.annotation.Nullable;

import luckytntlib.entity.PrimedLTNT;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

public class LuckyTNTBlock extends LTNTBlock{

	public List<RegistryObject<LTNTBlock>> TNTs;
	
	public LuckyTNTBlock(BlockBehaviour.Properties properties, List<RegistryObject<LTNTBlock>> TNTs) {
		super(properties, null, false);
		this.TNTs = TNTs;
	}
	
	@Override
	public PrimedLTNT explode(Level level, boolean exploded, double x, double y, double z, @Nullable LivingEntity igniter) throws NullPointerException{
		int rand = random.nextInt(TNTs.size());
		if(!(TNTs.get(rand).get() instanceof LuckyTNTBlock)) {
			return TNTs.get(rand).get().explode(level, exploded, x, y, z, igniter);
		}
		throw new NullPointerException();
	}
}
