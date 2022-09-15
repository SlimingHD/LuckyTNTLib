package luckytntlib.util;

import net.minecraft.world.phys.Vec3;

public class LMathUtil {

	public static double getRandomMinusPlus() {
		return Math.random() - Math.random(); 
	}
	
	public static Vec3 getRandomVector() {
		return new Vec3(getRandomMinusPlus(), getRandomMinusPlus(), getRandomMinusPlus());
	}
}
