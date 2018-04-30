package sc6;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import fr.irit.smac.amak.Agent;
import fr.irit.smac.amak.Scheduling;
import fr.irit.smac.amak.ui.DrawableUI;
import sc6.Drone.Mission;

/**
 * This class is used to display the environment and the drones
 *
 */
public class WorldViewer extends DrawableUI<DrAmas>{

	/**
	 * The size of the areas
	 */
	public static final int AREA_SIZE = 10;

	public static final int RADIUS = 6;

	private boolean neighboursDone;

	/**
	 * Constructor
	 * 
	 * @param _drAmas
	 *            the AMAS
	 */
	public WorldViewer(DrAmas _drAmas) {
		/**
		 * Auto start the rendering thread and allow control on it
		 */
		super(Scheduling.DEFAULT, _drAmas);
		this.neighboursDone = false;
	}

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1L;

	private FontMetrics metrics;

	/**
	 * Display the state of the system
	 */
	@Override
	protected void onDraw(Graphics2D arg0) {
		if (getAmas() != null) {
			// Draw areas
			arg0.setColor(Color.gray);
			for (int x = 0; x < getAmas().getEnvironment().getAreas()[0].length; x++) {
				for (int y = 0; y < getAmas().getEnvironment().getAreas().length; y++) {
					Area area = getAmas().getEnvironment().getAreas()[y][x];
					double timeSinceLastSeen = area.getTimeSinceLastSeen();
					if (timeSinceLastSeen > 1000)
						timeSinceLastSeen = 1000;
					switch(area.getType()){
					case GEOFENCEE:
						arg0.setColor(Color.DARK_GRAY);
						break;
					case SURVEY:
						arg0.setColor(
								new Color((float) timeSinceLastSeen / 1000f, 1 - (float) timeSinceLastSeen / 1000f, 0f));
						break;
					case PREPARATION:
						arg0.setColor(Color.MAGENTA);
						break;
					case RECHARGE:
						arg0.setColor(Color.YELLOW);
						break;
					case FORBIDDEN:
						arg0.setColor(Color.BLACK);
						break;
					default:
						arg0.setColor(Color.LIGHT_GRAY);
						break;
					}
					arg0.fillRect((int) discreteToTopContinuous(x), (int) discreteToTopContinuous(y), AREA_SIZE,
							AREA_SIZE);
					arg0.setColor(Color.BLACK);
					arg0.drawRect((int) discreteToTopContinuous(x), (int) discreteToTopContinuous(y), AREA_SIZE,
							AREA_SIZE);
				}
			}

			// Draw agents
			ArrayList<Agent<?, World>> agents = new ArrayList<>(getAmas().getAgents());
			for (Agent<?, World> agent : agents) {
				Drone drone = (Drone) agent;
				arg0.setColor(Color.white);
				//arg0.fillOval((int) discreteToTopContinuous(drone.getX()), (int) discreteToTopContinuous(drone.getY()), AREA_SIZE,
				// AREA_SIZE);
				Image img = null;
				if(drone.getMission() == Mission.RTH) {
					img = new ImageIcon(WorldViewer.class.getResource("/images/avion.png")).getImage();
				}
				else
				img = new ImageIcon(WorldViewer.class.getResource("/images/black-plane.png")).getImage();

				// Rotation information
				AffineTransform backup = arg0.getTransform();
				AffineTransform a = AffineTransform.getRotateInstance(Math.toRadians(directionToAngle(drone)), (int) (discreteToTopContinuous(drone.getX())+AREA_SIZE/2),
						(int) (discreteToTopContinuous(drone.getY())+AREA_SIZE/2));

				arg0.setTransform(a);

				arg0.drawImage(img, (int) discreteToTopContinuous(drone.getX()), (int) discreteToTopContinuous(drone.getY()),
						AREA_SIZE, AREA_SIZE,null);
				//arg0.drawOval((int) discreteToTopContinuous(drone.getX()), (int) discreteToTopContinuous(drone.getY()), AREA_SIZE,
				//    AREA_SIZE);
				arg0.setTransform(backup);
			}
			if(!this.neighboursDone) {
				for (int x = 0; x < getAmas().getEnvironment().getAreas()[0].length; x++) {
					for (int y = 0; y < getAmas().getEnvironment().getAreas().length; y++) {
						getAmas().getEnvironment().getAreas()[y][x].setNeighbours(getAmas().getEnvironment().getAreas());
					}

				}
				this.neighboursDone = true;
			}
		}
	}

	private double directionToAngle(Drone drone) {
		double res = 0;
		switch(drone.getOrientation()) {
		case TOP:
			return 270;
		case TOPRIGHT:
			return 315;
		case RIGHT:
			return 0;
		case BOTTOMRIGHT:
			return 45;
		case BOTTOM:
			return 90;
		case BOTTOMLEFT:
			return 135;
		case LEFT:
			return 180;
		case TOPLEFT:
			return 225;
		default:
			break;
		}
		return 0;
	}

	/**
	 * Helper function aiming at converting a discrete value to a screen value
	 * 
	 * @param dx
	 * @return
	 */
	public static double discreteToTopContinuous(int dx) {
		return (dx) * AREA_SIZE;
	}


	/**
	 * Helper function aiming at converting a discrete value to a screen value
	 * 
	 * @param dx
	 * @return
	 */
	public static double discreteToTopContinuous(double dx) {
		return (dx) * AREA_SIZE;
	}

	/**
	 * When dragging the mouse, add critical areas
	 */
	@Override
	protected void onMouseDragged(int x, int y) {
		for (int rx = -2; rx <= 2; rx++)
			for (int ry = -2; ry <= 2; ry++)
				getAmas().getEnvironment().getAreaByPosition(x / AREA_SIZE + rx, y / AREA_SIZE + ry).setCritical();
	}

	private void drawHex(Graphics2D g2d, int x, int y, int r,boolean filled) {

		Hexagon hex = new Hexagon(x, y, r);

		hex.draw(g2d, x, y, 0,filled);

	}


}
