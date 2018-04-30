package sc6;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;

import fr.irit.smac.amak.Agent;
import sc6.Area.AreaType;

/**
 * This class represent an agent of the system DrAmas
 *
 */
public class Drone extends Agent<DrAmas, World> {

	/**
	 * The enumeration of all the mission 
	 */
	public enum Mission{
		RTH,WAITING,SURVEY,PREPARATION;
	}

	/**
	 * The enumeration for the direction
	 */
	public enum Orientation{
		TOP,TOPRIGHT,RIGHT,BOTTOMRIGHT,BOTTOM,BOTTOMLEFT,LEFT,TOPLEFT;
	}
	/**
	 * Current coordinate of the drone
	 */
	private int dx, dy;

	/**
	 * View radius of the agent. The drone will be able to perceive drones and
	 * areas within a square of 20x20
	 */
	public static int VIEW_RADIUS = 50;

	/**
	 * The areas perceived by the agent during the perception phase
	 */
	private Area[][] view = new Area[VIEW_RADIUS * 2 + 1][VIEW_RADIUS * 2 + 1];
	/**
	 * The drone perceived by the agent during the perception phase
	 */
	private Drone[][][] neighborsView = new Drone[VIEW_RADIUS * 2 + 1][VIEW_RADIUS * 2 + 1][];

	/**
	 * The area the drone will try to reach during the action phase
	 */
	private Area targetArea;

	/**
	 * The current area of the drone. Located at dx, dy
	 */
	private Area currentArea;

	/**
	 * The recharge station
	 */
	private Area rechargeArea;

	/**
	 * The mission of the drone
	 */
	private Mission mission;

	/**
	 * The target of the drone
	 */
	private Area target;

	/**
	 * The orientation of the drone
	 */
	private Orientation orientation;

	/**
	 * The battery
	 */
	private int battery;

	/**
	 * The trajectory calculated by the drone
	 */
	private Queue<Area> trajectory;

	/**
	 * To stop and change direction
	 */
	private boolean stopAndChange = false;

	/**
	 * 
	 * Stop the drone
	 */

	private boolean stop = false;

	/**
	 * Constructor of the drone
	 * 
	 * @param amas
	 *            The AMAS the dorne belongs to
	 * @param startX
	 *            The initial x coordinate of the drone
	 * @param startY
	 *            The initial y coordinate of the drone
	 */
	public Drone(DrAmas amas, int startX, int startY) {
		super(amas, startX, startY);
		this.rechargeArea = getAmas().getEnvironment().getAreaByPosition(startX, startY);
		this.mission = Mission.PREPARATION;
		this.orientation = Orientation.RIGHT;
		this.battery = 500;
		this.trajectory = new LinkedList<Area>();
	}
	@Override
	protected void onInitialize() {

		dx = (int) params[0];
		dy = (int) params[1];
	}
	/**
	 * Initialize the first area of the drone
	 */
	@Override
	protected void onReady() {
		currentArea = amas.getEnvironment().getAreaByPosition(dx, dy);
	}

	/**
	 * Compute the criticality of the drone (if any)
	 */
	@Override
	protected double computeCriticality() {
		return 0;
	}

	/**
	 * Getter for the x coordinate
	 * 
	 * @return the x coordinate
	 */
	public int getX() {
		return dx;
	}

	/**
	 * Getter for the y coordinate
	 * 
	 * @return the y coordinate
	 */

	public int getY() {
		return dy;
	}

	/**
	 * Perception phase of the agent
	 */
	@Override
	protected void onPerceive() {
		if(this.currentArea.equals(this.target))
			this.target = null;
		if(battery < 50 && this.mission != Mission.RTH) {
			this.mission = Mission.RTH;
			this.trajectory = new LinkedList<Area>();
			this.stop = false;
			this.stopAndChange = false;
		}
		if(this.currentArea.getX() == rechargeArea.getX() && currentArea.getY() == rechargeArea.getY()) {
			this.battery = 500;
		}
		if(this.mission == Mission.RTH && this.battery ==500) {
			this.mission = Mission.PREPARATION;
		}
		// Clear the last set neighbors list
		clearNeighbors();
		// Check areas in a range of 20x20
		for (int x = -VIEW_RADIUS; x <= VIEW_RADIUS; x++) {
			for (int y = -VIEW_RADIUS; y <= VIEW_RADIUS; y++) {
				Area areaByPosition = amas.getEnvironment().getAreaByPosition(dx + x, dy + y);
				// store the seen areas
				view[y + VIEW_RADIUS][x + VIEW_RADIUS] = areaByPosition;
				Drone[] agentsInArea = amas.getAgentsInArea(areaByPosition);
				// store the seen agents
				neighborsView[y + VIEW_RADIUS][x + VIEW_RADIUS] = agentsInArea;
				// Set seen agents as neighbors
				addNeighbor(agentsInArea);
			}
		}
	}

	/**
	 * Clear the neighbors list
	 */
	private void clearNeighbors() {
		neighborhood.clear();
	}

	/**
	 * Agent action phase. Move toward a specified area
	 */
	@Override
	protected void onAct() {
		if (targetArea != null) {
			moveToward(targetArea);
			if(this.areasPossible(this.targetArea, this.findOrientation(this.currentArea, this.targetArea)).isEmpty()) {
				this.trajectory = new LinkedList<Area>();
				this.stop = true;
			}
			if(this.mission == Mission.PREPARATION && this.targetArea.getType() == AreaType.PREPARATION) {
				this.mission = Mission.SURVEY;
			}
			Random r = new Random();
			this.battery -= r.nextInt(5)+1;
		}
		else {
			this.stop = true;
			this.trajectory = new LinkedList<Area>();
		}
	}

	/**
	 * Decision phase. This method must be completed. In the action phase, the
	 * drone will move toward the area in the attribute "targetArea"
	 * 
	 * Examples: Move the drone to the right targetArea =
	 * amas.getEnvironment().getAreaByPosition(dx+1, dy);
	 * 
	 * Move the drone toward another drone targetArea =
	 * otherDrone.getCurrentArea();
	 */
	@Override
	protected void onDecide() {
		switch(this.mission) {
		case PREPARATION:
			this.prepare();;
		case WAITING:
			break;
		case SURVEY:
			this.toSurvey();
			break;
		case RTH:
			rth();
			break;
		default:break;
		}
		if(this.targetArea != null && this.areasPossible(this.targetArea, this.findOrientation(this.currentArea, this.targetArea)).isEmpty()) {
			this.stopAndChange = true;
			this.chooseTarget();
			System.out.println("STEP 10");
		}
	}
	private void rth() {

		this.target = this.rechargeArea;
		if(this.stopAndChange) {
			this.stopAndChange = false;
			this.stop = true;
			this.trajectory = new LinkedList<Area>();
		}
		else
			targetArea = this.firstFromTrajectory();
		/*double dist = 100000;
		for(Area ar : this.areasPossible()) {
			if(ar.distanceTo(target) < dist && !(ar.getType() == AreaType.FORBIDDEN || ar.getType() == AreaType.GEOFENCEE)) {
				this.targetArea = target;
				dist = ar.distanceTo(target);
			}
		}*/
	}

	private void prepare() {
		// On cherche la zone d'attente la plus proche
		Double distance = 1000000.0;
		for (int x = 0; x < getAmas().getEnvironment().getAreas()[0].length; x++) {
			for (int y = 0; y < getAmas().getEnvironment().getAreas().length; y++) {
				Area a = getAmas().getEnvironment().getAreaByPosition(x, y);
				if (a.getType() == AreaType.PREPARATION) {
					if(this.distanceTo(a) < distance) {
						distance = this.distanceTo(a);
						this.target = a;
					}
				}
			}
		}
		//this.targetArea = firstFromTrajectory();

		// On regarde si elle a portée
		List<Area> areas = new ArrayList<>();
		boolean find = false;
		for (int x = -VIEW_RADIUS; x <= VIEW_RADIUS; x++) {
			for (int y = -VIEW_RADIUS; y <= VIEW_RADIUS; y++) {
				Area a = view[y + VIEW_RADIUS][x + VIEW_RADIUS];
				if (a != null) {
					areas.add(a);
					if(a.getX() == this.target.getX() && a.getY() == this.target.getY()) {
						this.targetArea = this.target;
						find = true;
					}
				}
			}
		}
		// Permet de connaitre l'area la plus optimale pour aller vers la cible
		if(!find) {
			Point p = this.calculDifferenceCoordinate(this.currentArea, this.target);
			int x = 0;
			int y = 0;
			if(p.x < 0) {
				x = 1;
			}
			if(p.x > 0) {
				x = -1;
			}
			if(p.y < 0) {
				y = 1;
			}
			if(p.y > 0) {
				y = -1;
			}

			this.targetArea = getAmas().getEnvironment().getAreaByPosition(this.getX()+x, this.getY()+y);
			/*Random r = new Random();
			int result = r.nextInt(Math.abs(p.x)+Math.abs(p.y));
			if(result < Math.abs(p.x)) {

			}*/
		}

	}

	/**
	 * The drone choose an area to survey
	 */
	private void toSurvey() {
		//Création de listes pour faciliter le tri
		List<Area> areas = new ArrayList<>();
		List<Drone> visibleDrones = new ArrayList<>();
		for (int x = -VIEW_RADIUS; x <= VIEW_RADIUS; x++) {
			for (int y = -VIEW_RADIUS; y <= VIEW_RADIUS; y++) {
				if (view[y + VIEW_RADIUS][x + VIEW_RADIUS] != null) {
					Area a = view[y + VIEW_RADIUS][x + VIEW_RADIUS];
					if(a.getType() == AreaType.SURVEY) {
						areas.add(a);
					}
					if (neighborsView[y + VIEW_RADIUS][x + VIEW_RADIUS] != null) {
						for (Drone drone : neighborsView[y + VIEW_RADIUS][x + VIEW_RADIUS]) {
							visibleDrones.add(drone);
						}
					}
				}
			}
		}

		//Tri des parcelles de la plus critique à la moins critique

		Collections.sort(areas, new Comparator<Area>() {

			@Override
			public int compare(Area o1, Area o2) {
				return (int) (o2.computeCriticality()*10000 - o1.computeCriticality()*10000);
			}
		});


		if(this.stopAndChange) {
			this.stopAndChange = false;
			this.stop = true;
			this.trajectory = new LinkedList<Area>();
		}
		else {
			//On choisit la parcelle la plus critique ET dont je suis le plus proche
			Area a = getAreaImTheClosestTo(areas, visibleDrones);
			if(target ==null)
				target = a;
			if(this.isNextToMe()) {
				this.stopAndChange = true;
				double dist = 100000;
				for(Area ar : this.areasPossible()) {
					if(ar.distanceTo(a) < dist && !(ar.getType() == AreaType.FORBIDDEN && ar.getType() == AreaType.GEOFENCEE) && getAmas().getAgentsInAreaList(ar).isEmpty()) {
						this.targetArea = ar;
						dist = ar.distanceTo(a);
					}
				}
			}
			else {
				targetArea = this.firstFromTrajectory();
			}
		}
	}

	/**
	 * Fi
	 * @param nbStep
	 */
	private void bestTrajectory(int nbStep) {
		
	}
	
	/**
	 * Use the potential field to choose the best target
	 */
	private void potentialField() {
		
	}
	/**
	 * From an ordered list of areas (areas) and a list of drone, return the
	 * first area I'm the closest to.
	 * 
	 * @param areas
	 *            Ordered list of areas
	 * @param drones
	 *            List of drones
	 * @return the first area I'm the closest to
	 */
	private Area getAreaImTheClosestTo(List<Area> areas, List<Drone> drones) {
		for (Area area : areas) {
			if (closestDrone(area, drones) == this)
				return area;
		}
		return areas.get(getAmas().getEnvironment().getRandom().nextInt(areas.size()));
	}

	/**
	 * Find the closest drone from an area within a given list of drones
	 * 
	 * @param area
	 *            The concerned area
	 * @param drones
	 *            The list of drones
	 * @return the closest drone
	 */
	private Drone closestDrone(Area area, List<Drone> drones) {
		double distance = Double.POSITIVE_INFINITY;
		Drone closest = this;
		for (Drone drone : drones) {
			if (drone.distanceTo(area) < distance) {
				distance = drone.distanceTo(area);
				closest = drone;
			}
		}
		return closest;
	}

	/**
	 * Distance from the drone to a specified area
	 * 
	 * @param area
	 *            The area
	 * @return the distance between the drone and the area
	 */
	private double distanceTo(Area area) {
		return Math.sqrt(Math.pow(area.getX() - dx, 2) + Math.pow(area.getY() - dy, 2));
	}

	/**
	 * Move toward an area and scan the reached area. A drone can only move at 1
	 * area /cycle so the target area may not be the one seen.
	 * 
	 * @param a
	 *            The target area
	 */
	protected void moveToward(Area a) {
		//Change the orientation
		if(this.stop) {
			Orientation orientTmp = this.findOrientation(this.currentArea, this.target);
			if(this.areasPossible(this.targetArea, orientTmp).isEmpty()) {
				this.rotateRight();
			}
			else {
				this.orientation = orientTmp;
			}
			this.stop = false;
		}
		else {
			int depy = 0;
			int depx = 0;
			if (dx < a.getX()) {
				dx++;
				depx = 1;
			}
			else if (dx > a.getX()) {
				dx--;
				depx = -1;
			}
			if (dy < a.getY()) {
				dy++;
				depy = 1;
			}
			else if (dy > a.getY()) {
				dy--;
				depy = -1;
			}
			currentArea = amas.getEnvironment().getAreaByPosition(dx, dy);
			currentArea.seen(this);
			this.orientation = this.changeOrientation(depx, depy);
		}
	}

	/**
	 * Getter for the area of the drone
	 * 
	 * @return the current area
	 */
	public Area getCurrentArea() {
		return currentArea;
	}

	/**
	 * Calcul the difference of coordinate between two Area 
	 */
	public Point calculDifferenceCoordinate(Area a1,Area a2) {
		return new Point(a1.getX()-a2.getX(),a1.getY()-a2.getY());
	}



	/**
	 * Return the orientation of the drone
	 * @return this.orientation
	 */
	public Orientation getOrientation() {
		return this.orientation;
	}

	/**
	 * Give the areas which are allowed for the drone because of the movements constraints
	 * @return List<Area>
	 * 		the list of all area allowed
	 */
	private List<Area> areasPossible(){
		List<Area> res = new ArrayList<Area>();
		switch(this.orientation) {
		case TOP:
			if(currentArea.getTop().isAllowed())
				res.add(currentArea.getTop());
			if(currentArea.getTopLeft().isAllowed())
				res.add(currentArea.getTopLeft());
			if(currentArea.getTopRight().isAllowed())
				res.add(currentArea.getTopRight());
			break;
		case TOPRIGHT:
			if(currentArea.getTop().isAllowed())
				res.add(currentArea.getTop());
			if(currentArea.getRight().isAllowed())
				res.add(currentArea.getRight());
			if(currentArea.getTopRight().isAllowed())
				res.add(currentArea.getTopRight());
			break;
		case RIGHT:
			if(currentArea.getRight().isAllowed())
				res.add(currentArea.getRight());
			if(currentArea.getBottomRight().isAllowed())
				res.add(currentArea.getBottomRight());
			if(currentArea.getTopRight().isAllowed())
				res.add(currentArea.getTopRight());
			break;
		case BOTTOMRIGHT:
			if(currentArea.getRight().isAllowed())
				res.add(currentArea.getRight());
			if(currentArea.getBottomRight().isAllowed())
				res.add(currentArea.getBottomRight());
			if(currentArea.getBottom().isAllowed())	
				res.add(currentArea.getBottom());
			break;
		case BOTTOM:
			if(currentArea.getBottomLeft().isAllowed())	
				res.add(currentArea.getBottomLeft());
			if(currentArea.getBottomRight().isAllowed())	
				res.add(currentArea.getBottomRight());
			if(currentArea.getBottom().isAllowed())	
				res.add(currentArea.getBottom());
			break;
		case BOTTOMLEFT:
			if(currentArea.getBottomLeft().isAllowed())	
				res.add(currentArea.getBottomLeft());
			if(currentArea.getLeft().isAllowed())	
				res.add(currentArea.getLeft());
			if(currentArea.getBottom().isAllowed())	
				res.add(currentArea.getBottom());
			break;
		case LEFT:
			if(currentArea.getBottomLeft().isAllowed())	
				res.add(currentArea.getBottomLeft());
			if(currentArea.getLeft().isAllowed())	
				res.add(currentArea.getLeft());
			if(currentArea.getTopLeft().isAllowed())	
				res.add(currentArea.getTopLeft());
			break;
		case TOPLEFT:
			if(currentArea.getTop().isAllowed())	
				res.add(currentArea.getTop());
			if(currentArea.getLeft().isAllowed())	
				res.add(currentArea.getLeft());
			if(currentArea.getTopLeft().isAllowed())	
				res.add(currentArea.getTopLeft());
			break;
		}
		return res;
	}

	/**
	 * Give the areas which are allowed for the drone because of the movements constraints
	 * @param current
	 * 			The area
	 * @param orient
	 * 			The orientation
	 * @return List<Area>
	 * 		the list of all area allowed
	 */
	private List<Area> areasPossible(Area current, Orientation orient){
		List<Area> res = new ArrayList<Area>();
		switch(orient) {
		case TOP:
			if(current.getTop().isAllowed())
				res.add(current.getTop());
			if(current.getTopLeft().isAllowed())
				res.add(current.getTopLeft());
			if(current.getTopRight().isAllowed())
				res.add(current.getTopRight());
			break;
		case TOPRIGHT:
			if(current.getTop().isAllowed())
				res.add(current.getTop());
			if(current.getRight().isAllowed())
				res.add(current.getRight());
			if(current.getTopRight().isAllowed())
				res.add(current.getTopRight());
			break;
		case RIGHT:
			if(current.getRight().isAllowed())
				res.add(current.getRight());
			if(current.getBottomRight().isAllowed())
				res.add(current.getBottomRight());
			if(current.getTopRight().isAllowed())
				res.add(current.getTopRight());
			break;
		case BOTTOMRIGHT:
			if(current.getRight().isAllowed())
				res.add(current.getRight());
			if(current.getBottomRight().isAllowed())
				res.add(current.getBottomRight());
			if(current.getBottom().isAllowed())	
				res.add(current.getBottom());
			break;
		case BOTTOM:
			if(current.getBottomLeft().isAllowed())	
				res.add(current.getBottomLeft());
			if(current.getBottomRight().isAllowed())	
				res.add(current.getBottomRight());
			if(current.getBottom().isAllowed())	
				res.add(current.getBottom());
			break;
		case BOTTOMLEFT:
			if(current.getBottomLeft().isAllowed())	
				res.add(current.getBottomLeft());
			if(current.getLeft().isAllowed())	
				res.add(current.getLeft());
			if(current.getBottom().isAllowed())	
				res.add(current.getBottom());
			break;
		case LEFT:
			if(current.getBottomLeft().isAllowed())	
				res.add(current.getBottomLeft());
			if(current.getLeft().isAllowed())	
				res.add(current.getLeft());
			if(current.getTopLeft().isAllowed())	
				res.add(current.getTopLeft());
			break;
		case TOPLEFT:
			if(current.getTop().isAllowed())	
				res.add(current.getTop());
			if(current.getLeft().isAllowed())	
				res.add(current.getLeft());
			if(current.getTopLeft().isAllowed())	
				res.add(current.getTopLeft());
			break;
		}
		res.removeIf(Objects::isNull);
		return res;
	}

	/**
	 * IF the drone does not have a trajectory, create one and then return the first area
	 * @return the first area of the trajectory
	 */
	private Area firstFromTrajectory() {
		Area res = null;
		boolean completed = false;
		boolean arret = false;
		int step = 0;
		List<Area> alreadyCheck = new ArrayList<Area>();
		//System.out.println(this.target.getX()+ " "+this.target.getY());
		//System.out.println("Bateery : "+this.battery);
		// S'il n'existe pas de trajectoire
		if(this.trajectory.isEmpty()) {
			Area current = this.currentArea;
			Orientation orient = this.orientation;
			// Tant que la trajectoire n'est pas termine
			while(!completed && !arret) {
				//Initialisation des variables
				double distance = 100000;
				int nbPossib = 0;
				Area nextArea = null;
				Orientation nextOrient = null;
				boolean redo = false;

				// Recuperation des aires ou le drone peut aller
				List<Area> areapossib = this.areasPossible(current, orient);
				// Pour toute les aires
				for(Area next : areapossib) {
					if(!redo) {
						// Si l'aire est autorisee
						if(next != null && next.getType() != AreaType.GEOFENCEE && next.getType() != AreaType.FORBIDDEN) {
							//On regarde si l'aire est l'objectif
							// Si l'objectif est deja alors on change de cible
							if(this.areaTargetInTrajectory(next, step)) {
								if(next.equals(this.target)) {
									this.trajectory = new LinkedList<Area>();
									current = this.currentArea;
									orient = this.orientation;
									step = 0;
									alreadyCheck.add(this.target);
									this.chooseTarget(alreadyCheck);
									redo = true;
								}
							}
							else {
								if(next.equals(this.target)) {
									completed = true;
									current = next;
									nextArea = next;
								}
								else {
									//Sinon
									//On recherche l'orientation si on se deplace dans cette aire
									Orientation orientTmp = this.findOrientation(current, next);
									//On recupere les deplacements possible depuis cette aire
									List<Area> nextPossib = this.areasPossible(next,orientTmp);
									//Si la trajectoire n'est pas finie
									if(!completed) {
										//On regarde la distance depuis cette aire avec l'objectif
										double nextDistance = this.target.distanceTo(next);
										Orientation orientTarget = this.findOrientation(current, target);

										//Si l'orientation apres le deplacement est oppose a l'orientation de l'objectif
										if(this.oppose(orientTarget, orientTmp)) {
											nextDistance += 10;
										}
										if(nextPossib.size() > nbPossib) {
											nbPossib = nextPossib.size();
											nextArea = next;
											nextOrient = orientTmp;
											distance = nextDistance;
											//System.out.println("ICI");
										}
										else {
											if( nextPossib.size() == nbPossib && nextDistance < distance) {
												nbPossib = nextPossib.size();
												nextArea = next;
												nextOrient = orientTmp;
												distance = nextDistance;
											}
										}
									}
								}
							}
						}
						}

					}
					if(nextArea != null) {
						if(nextArea.equals(this.currentArea))
							arret = true;
						this.trajectory.offer(nextArea);
						current = nextArea;
						orient = nextOrient;
					}
					arret = (step == 80);
					step++;
				}
				//System.out.println("X : "+current.getX()+" Y : "+current.getY());
				/*try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/

		}
		res = this.trajectory.poll();
		if(step == 81) {
			this.stopAndChange = true;
			this.trajectory = new LinkedList<Area>();
		}
		return res;
	}

	/**
	 * Change the orientation of the drone
	 */
	private Orientation changeOrientation(int depx,int depy) {

		if(depx > 0) {
			if(depy > 0) {
				return Orientation.BOTTOMRIGHT;
			}
			else {
				if(depy < 0) {
					return Orientation.TOPRIGHT;
				}
				else
					return Orientation.RIGHT;
			}
		}
		else {
			if(depx < 0) {
				if(depy > 0) {
					return Orientation.BOTTOMLEFT;
				}
				else {
					if(depy < 0) {
						return Orientation.TOPLEFT;
					}
					else
						return Orientation.LEFT;
				}
			}
			else {
				if(depy > 0) {
					return Orientation.BOTTOM;
				}
				else {
					return Orientation.TOP;
				}


			}


		}
	}

	/**
	 * Renvoi l'orientation en fonction d'une aire de depart et d'une aire d'arrivee
	 * @param current
	 * 		l'aire de depart
	 * @param next
	 * 		l'aire d'arrivee
	 * @return Orientation
	 */
	private Orientation findOrientation(Area current, Area next) {
		int depy = 0;
		int depx = 0;
		if (current.getX() < next.getX()) {
			depx = 1;
		}
		else if (current.getX() > next.getX()) {
			depx = -1;
		}
		if (current.getY() < next.getY()) {
			depy = 1;
		}
		else if (current.getY() > next.getY()) {
			depy = -1;
		}
		return changeOrientation(depx, depy);
	}

	/**
	 * Return true if two orientation are opposed
	 * @return
	 */
	private boolean oppose(Orientation or1, Orientation or2) {
		switch(or1) {
		case BOTTOM:
			return or2 == Orientation.TOP;
		case BOTTOMLEFT:
			return or2 == Orientation.TOPRIGHT;
		case BOTTOMRIGHT:
			return or2 == Orientation.TOPLEFT;
		case LEFT:
			return or2 == Orientation.RIGHT;
		case RIGHT:
			return or2 == Orientation.LEFT;
		case TOP:
			return or2 == Orientation.BOTTOM;
		case TOPLEFT:
			return or2 == Orientation.BOTTOMRIGHT;
		case TOPRIGHT:
			return or2 == Orientation.BOTTOMLEFT;
		default:
			return false;

		}
	}

	/**
	 * Change the orientation of the drone by 90 to the right
	 */
	private void rotateRight() {
		switch(this.orientation) {
		case BOTTOM:
			this.orientation = Orientation.LEFT;
			break;
		case BOTTOMLEFT:
			this.orientation = Orientation.TOPLEFT;
			break;
		case BOTTOMRIGHT:
			this.orientation = Orientation.BOTTOMLEFT;
			break;
		case LEFT:
			this.orientation = Orientation.TOP;
			break;
		case RIGHT:
			this.orientation = Orientation.BOTTOM;
			break;
		case TOP:
			this.orientation = Orientation.RIGHT;
			break;
		case TOPLEFT:
			this.orientation = Orientation.TOPRIGHT;
			break;
		case TOPRIGHT:
			this.orientation = Orientation.BOTTOMRIGHT;
			break;
		default:
			break;

		}

	}


	/**
	 * To know if an area is next to the drone 
	 * and if the direction forbid to get to it 
	 * @param target2
	 * @return true if it is impossible to reach
	 */
	private boolean isNextToMe() {
		if(Math.abs(this.currentArea.getX()-this.target.getX()) + Math.abs(this.currentArea.getY()-this.target.getY()) < 3) {
			if(!this.areasPossible().contains(this.target)) {
				return true;
			}
		}
		return false;
	}

	public Area getTarget() {
		return this.target;
	}

	public Queue<Area> getTrajectory() {
		return trajectory;
	}


	/**
	 * Look if the area will not be used at the same time
	 * @param ar
	 * @return
	 */
	private boolean areaTargetInTrajectory(Area ar, int time) {
		for(Agent a : getAmas().getAgents()) {
			int step = 0;
			if(a != this && this.getId() == 2) {
				Drone d = (Drone) a;
				Queue<Area> otherTrajectory = d.getTrajectory();
				while(!otherTrajectory.isEmpty()) {
					
					Area otherArea = otherTrajectory.poll();
					if(otherArea != null && step == time && otherArea.equals(ar)) {
						return true;
					}
					step++;
				}
			}
		}
		return false;
	}

	private List<Area> calculateImpossibleArea(){
		List<Area> ret = new ArrayList<Area>();
		for(Area ar : this.currentArea.getNeighbours()) {
			if(!this.areasPossible().contains(ar)) {
				ret.add(ar);
			}
		}
		return ret;
	}
	public Mission getMission() {
		return mission;
	}


	private void chooseTarget() {
		//Création de listes pour faciliter le tri
		List<Area> areas = new ArrayList<>();
		List<Drone> visibleDrones = new ArrayList<>();
		for (int x = -VIEW_RADIUS; x <= VIEW_RADIUS; x++) {
			for (int y = -VIEW_RADIUS; y <= VIEW_RADIUS; y++) {
				if (view[y + VIEW_RADIUS][x + VIEW_RADIUS] != null) {
					Area a = view[y + VIEW_RADIUS][x + VIEW_RADIUS];
					if(a.getType() == AreaType.SURVEY && !a.equals(this.targetArea)) {
						areas.add(a);
					}
					if (neighborsView[y + VIEW_RADIUS][x + VIEW_RADIUS] != null) {
						for (Drone drone : neighborsView[y + VIEW_RADIUS][x + VIEW_RADIUS]) {
							visibleDrones.add(drone);
						}
					}
				}
			}
		}

		//Tri des parcelles de la plus critique à la moins critique

		Collections.sort(areas, new Comparator<Area>() {

			@Override
			public int compare(Area o1, Area o2) {
				return (int) (o2.computeCriticality()*10000 - o1.computeCriticality()*10000);
			}
		});

		//On choisit la parcelle la plus critique ET dont je suis le plus proche
		this.target = getAreaImTheClosestTo(areas, visibleDrones);
	}

	private void chooseTarget(List<Area> alreadyCheck) {
		//Création de listes pour faciliter le tri
		List<Area> areas = new ArrayList<>();
		List<Drone> visibleDrones = new ArrayList<>();
		for (int x = -VIEW_RADIUS; x <= VIEW_RADIUS; x++) {
			for (int y = -VIEW_RADIUS; y <= VIEW_RADIUS; y++) {
				if (view[y + VIEW_RADIUS][x + VIEW_RADIUS] != null) {
					Area a = view[y + VIEW_RADIUS][x + VIEW_RADIUS];
					if(a.getType() == AreaType.SURVEY && !a.equals(this.targetArea) && !alreadyCheck.contains(a)) {
						areas.add(a);
					}
					if (neighborsView[y + VIEW_RADIUS][x + VIEW_RADIUS] != null) {
						for (Drone drone : neighborsView[y + VIEW_RADIUS][x + VIEW_RADIUS]) {
							visibleDrones.add(drone);
						}
					}
					
				}
			}
		}

		//Tri des parcelles de la plus critique à la moins critique

		Collections.sort(areas, new Comparator<Area>() {

			@Override
			public int compare(Area o1, Area o2) {
				return (int) (o2.computeCriticality()*10000 - o1.computeCriticality()*10000);
			}
		});

		//On choisit la parcelle la plus critique ET dont je suis le plus proche
		this.target = getAreaImTheClosestTo(areas, visibleDrones);
	}


}
