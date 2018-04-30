package sc6;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Random;

import fr.irit.smac.amak.Agent;
import fr.irit.smac.amak.Amas;
import fr.irit.smac.amak.Scheduling;
import fr.irit.smac.amak.ui.MainWindow;
import fr.irit.smac.lxplot.LxPlot;

/**
 * This class represents the AMAS
 *
 */
public class DrAmas extends Amas<World> {

	/**
	 * Initial drones count on the AMAS creation
	 */
	private static final int INITIAL_DRONE_COUNT = 5;
	/**
	 * Queue used to compute the sliding window
	 */
	private LinkedList<Double> lastSums = new LinkedList<>();

	/**
	 * Constructor
	 * 
	 * @param env
	 *            The environment of the AMAS
	 */
	public DrAmas(World env) {
		// Set the environment and use manual scheduling
		super(env, Scheduling.DEFAULT);
	}

	/**
	 * Create the agents at random positions
	 */
	@Override
	protected void onInitialAgentsCreation() {
		for (int i = 0; i < INITIAL_DRONE_COUNT; i++) {
			int x = 8;
			int y = 24 + i*2;
			Area start = getEnvironment().getAreaByPosition(x, y);
			new Drone(this, start.getX(), start.getY());
		}
			
	}
	

	/**
	 * Launch the system
	 * 
	 * @param args
	 *            Arguments of the problem (not used)
	 */
	public static void main(String[] args) {
		DrAmas drAmas = new DrAmas(new World());
		
		new WorldViewer(drAmas);
		MainWindow.addMenuItem("Remove 10 drones", l->{
			for (int i=0;i<10;i++) {
				drAmas.getAgents().get(drAmas.getEnvironment().getRandom().nextInt(drAmas.getAgents().size())).destroy();
			}
		});
		MainWindow.addMenuItem("Add 10 drones", l->{
			for (int i=0;i<10;i++) {

				new Drone(drAmas, drAmas.getEnvironment().getRandom().nextInt(World.WIDTH), drAmas.getEnvironment().getRandom().nextInt(World.HEIGHT));
			}
		});
		
	}

	/**
	 * At the end of each system cycle, compute the sum and average of area
	 * criticalities and display them
	 */
	@Override
	protected void onSystemCycleEnd() {
		double max = 0;
		double sum = 0;
		for (int x = 0; x < getEnvironment().getAreas()[0].length; x++) {
			for (int y = 0; y < getEnvironment().getAreas().length; y++) {
				double criticality = getEnvironment().getAreaByPosition(x, y).computeCriticality();
				sum += criticality;
				if (criticality > max)
					max = criticality;
			}
		}
		/*List<Agent> toDestroy = new ArrayList<Agent>();
		for(Agent ag : getAgents()) {
			for(Agent ag2 : getAgents()) {
				if(ag != ag2 ) {
					Drone d1 = (Drone)ag;
					Drone d2 = (Drone)ag2;
					if(d1.getCurrentArea().equals(d2.getCurrentArea())) {
						toDestroy.add(ag);
						toDestroy.add(ag2);
						System.out.println("Le drone : "+ d1.getId()+" est mort et sa mission : "+d1.getMission());
						System.out.println("Le drone : "+ d2.getId()+" est mort et sa mission : "+d2.getMission());
					}
				}
			}
		}
		for(Agent d: toDestroy) {
			d.destroy();
		}*/
		lastSums.add(sum);
		if (lastSums.size() > 10000)
			lastSums.poll();

		LxPlot.getChart("Area criticalities").add("Sum", getCycle() % 1000, sum);
		LxPlot.getChart("Area criticalities").add("Sliding average", getCycle() % 1000, average(lastSums));
	}

	/**
	 * Compute the average of a list
	 * 
	 * @param lastSums2
	 *            List on which computing the average
	 * @return the average
	 */
	private double average(LinkedList<Double> lastSums2) {
		OptionalDouble average = lastSums2.stream().mapToDouble(a -> a).average();
		return average.getAsDouble();
	}

	/**
	 * Get agents presents in a specified area
	 * 
	 * @param areaByPosition
	 *            The specified area
	 * @return the list of drones in this area
	 */
	public Drone[] getAgentsInArea(Area areaByPosition) {
		List<Drone> res = new ArrayList<>();
		for (Agent<?, World> agent : agents) {
			if (((Drone) agent).getCurrentArea() == areaByPosition)
				res.add((Drone) agent);
		}
		return res.toArray(new Drone[0]);
	}
	
	/**
	 * Get agents presents in a specified area
	 * 
	 * @param areaByPosition
	 *            The specified area
	 * @return the list of drones in this area
	 */
	public List<Drone> getAgentsInAreaList(Area areaByPosition) {
		List<Drone> res = new ArrayList<>();
		for (Agent<?, World> agent : agents) {
			if (((Drone) agent).getCurrentArea() == areaByPosition)
				res.add((Drone) agent);
		}
		return res;
	}
}
