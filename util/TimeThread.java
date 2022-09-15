package luckytntlib.util;

public class TimeThread extends Thread{

	public int time = 0;
	public boolean done = false;
	
	public void run() {
		while(!done) {
			time++;
			try {
				sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Time elapsed: " + time + "ms");
	}
}
