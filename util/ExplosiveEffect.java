package luckytntlib.util;

@SuppressWarnings("deprecation")
public abstract class ExplosiveEffect implements IRenderAsBlock{

	public abstract void baseTick(IExplosiveEntity entity);
	
	public abstract void explosionTick(IExplosiveEntity entity);
	
	public abstract void clientExplosion(IExplosiveEntity entity);
	
	public abstract void serverExplosion(IExplosiveEntity entity);
	
	public abstract void spawnParticles(IExplosiveEntity entity);
	
	@Override
	public float getSize() {
		return 1;
	}
}
