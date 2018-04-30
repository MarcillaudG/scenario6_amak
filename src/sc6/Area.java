package sc6;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * An area represents a small part of the world that can be scanned by a drone
 * in one cycle
 *
 */
public class Area {
	/**
	 * The amount of time (in cycles) since when the area hasn't been scanned
	 */
	private double timeSinceLastSeen = 0;
	/**
	 * The X coordinate of the area
	 */
	private int x;
	/**
	 * The Y coordinate of the area
	 */
	private int y;
	/**
	 * The importance of the area. The more this value is high, the more this
	 * area must be scanned often.
	 */
	private double outdateFactor;
	private double nextTimeSinceLastSeen = timeSinceLastSeen;
	private Area topLeft;
	private Area topRight;
	private Area right;
	private Area bottomRight;
	private Area bottomLeft;
	private Area left;
	private Area top;
	private Area bottom;
	private List<Area> neighbours;
	
	private Color[] colors = {Color.CYAN,Color.BLUE,Color.GRAY,Color.GREEN, Color.MAGENTA, Color.PINK, Color.WHITE, Color.YELLOW};

	public enum AreaType{
		GEOFENCEE,SURVEY,WAITING,ALLOWED,FORBIDDEN, RECHARGE,PREPARATION
	}

	private AreaType type;

	/**
	 * Constructor of the area
	 * 
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 */
	public Area(int x, int y) {
		// Set the position
		this.x = x;
		this.y = y;
		this.neighbours = new ArrayList<Area>();
		// Set a high importance for a specific set of areas
		if(x <2 || x >= World.WIDTH-2 || y < 2 || y >=World.HEIGHT-2) {
			this.type = AreaType.GEOFENCEE;
		}
		else
			if((x > 15 && y > 15 && x < 45 && y < 50) ||(x > 50 && y > 15 && x < 60 && y < 50)) {
				this.type = AreaType.SURVEY;
				this.timeSinceLastSeen = 0;
			}
			else
				if(x >10 && x < 16 && y > 15 && y < 40){
					this.type = AreaType.PREPARATION;
				}
				else
					if((y == 24 || y == 26 || y== 28 || y == 30 || y == 32)&& x == 8) {
						this.type = AreaType.RECHARGE;
					}
					else {
						if(x > 44 && x < 51 && y > 25 && y < 35) {
							this.type = AreaType.FORBIDDEN;
						}
						else
							this.type = AreaType.ALLOWED;
					}
		/*if (x > 10 && x < 20 && y > 10 && y < 30)
			this.outdateFactor = 10;
		else*/
			this.outdateFactor = 0.2;
	}

	/**
	 * Getter for the X coordinate
	 * 
	 * @return the x coordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * Getter for the Y coordinate
	 * 
	 * @return the y coordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * This method is called when the drone scans the area
	 * 
	 * @param drone
	 *            The drone which scans the area
	 */
	public void seen(Drone drone) {
		nextTimeSinceLastSeen = 0;
	}

	/**
	 * Getter for the amount of time since last scan
	 * 
	 * @return the amount of time since last scan
	 */
	public double getTimeSinceLastSeen() {
		return timeSinceLastSeen;
	}

	/**
	 * Update the time since last scan at each cycle
	 */
	public void cycle() {
		if(this.type == AreaType.SURVEY) {
			nextTimeSinceLastSeen++;
			timeSinceLastSeen = nextTimeSinceLastSeen;
		}
	}

	/**
	 * Manually set a hgh criticality to request a scan on a specific area
	 */
	public void setCritical() {
		nextTimeSinceLastSeen  = 1000;
	}

	/**
	 * Compute the criticality of the area based on the time since last scan
	 * 
	 * @return the criticality of the area
	 */
	public double computeCriticality() {
		return Math.min(timeSinceLastSeen * outdateFactor / 1000, 1);
	}

	/**
	 * Create the List of the 6 neigbours of the area
	 */
	public void setNeighbours(Area[][] areas) {
		if(x > 0 && y >0) {
			this.topLeft = areas[y-1][x-1];
			this.neighbours.add(this.topLeft);
		}
		if(x+1 < areas[0].length) {
			if(y > 0) {
				this.topRight = areas[y-1][x+1];
				this.neighbours.add(this.topRight);
			}
			this.right = areas[y][x+1];
			this.neighbours.add(this.right);
			if(y+1 < areas.length) {
				this.bottomRight = areas[y+1][x+1];
				this.neighbours.add(this.bottomRight);
			}
		}
		if(x > 0 && y+1 < areas.length ) {
			this.bottomLeft = areas[y+1][x-1];
			this.neighbours.add(this.bottomLeft);
			this.left = areas[y][x-1];
			this.neighbours.add(this.left);
		}
		if(y+1 < areas.length) {
			this.bottom = areas[y+1][x];
			this.neighbours.add(this.bottom);
		}
		if(y-1 > 0) {
			this.top = areas[y-1][x];
			this.neighbours.add(this.top);
		}


	}

	public Area getTopLeft() {
		return topLeft;
	}

	public Area getTopRight() {
		return topRight;
	}

	public Area getRight() {
		return right;
	}

	public Area getBottomRight() {
		return bottomRight;
	}

	public Area getBottomLeft() {
		return bottomLeft;
	}

	public Area getLeft() {
		return left;
	}

	public Area getTop() {
		return top;
	}

	public Area getBottom() {
		return bottom;
	}

	public AreaType getType() {
		return type;
	}

	public void setType(AreaType type) {
		this.type = type;
	}
	
	public List<Area> getNeighbours(){
		return this.neighbours;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Area other = (Area) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	public double distanceTo(Area a) {
		return Math.sqrt(Math.pow(a.getX() - x, 2) + Math.pow(a.getY() - y, 2));
	}

	public boolean isAllowed() {
		return this.type != AreaType.FORBIDDEN && this.type != AreaType.GEOFENCEE;
	}





















}
