package sc6;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sc6.Drone.Orientation;

public class Test {

	public static void main(String[] args) {
		Random r = new Random();
		Area a1 = new Area(12,10);
		Area a2 = new Area(12,11);
		Area a3 = new Area(13,10);
		Area a4 = new Area(13,11);
		Area a5 = new Area(13,9);
		Area current = new Area(12,11);
		List<Area> aresPossib = new ArrayList<Area>();
		aresPossib.add(a3);
		aresPossib.add(a4);
		aresPossib.add(a5);
		//System.out.println(Drone.isNextToMeTest(a1, a2, aresPossib));
		/*Orientation orientTarget = Drone.findOrientation(current, target);
		System.out.println(orientTarget);
		Orientation o2 = Drone.findOrientation(a3, a1);
		System.out.println(o2);
		Orientation o3 = Drone.findOrientation(a4, a1);
		System.out.println(o3);
		Orientation orientTmp = Drone.findOrientation(current, next);
		System.out.println(orientTmp);
		Orientation o5 = Drone.findOrientation(current, target);
		System.out.println(o5);
		System.out.println(Drone.oppose(orientTarget, orientTmp));*/
		
	}

}
