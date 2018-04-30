package sc6;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class Hexagon extends Polygon {

    private static final long serialVersionUID = 1L;

    public static final int SIDES = 6;

    private Point[] points = new Point[SIDES];
    private Point2D[] points2D = new Point2D[SIDES];
    private Point center = new Point(0, 0);
    private Point2D center2D = new Point(0, 0);
    private int radius;
    private int rotation = 90;
    
    private Double[] xpoints2D;
    private Double[] ypoints2D;

    public Hexagon(Point center, int radius) {
        npoints = SIDES;
        xpoints = new int[SIDES];
        ypoints = new int[SIDES];

        this.center = center;
        this.radius = radius;

        updatePoints();
    }
    
    public Hexagon(Point2D center, int radius) {
        npoints = SIDES;
        xpoints2D = new Double[SIDES];
        ypoints2D = new Double[SIDES];

        this.center2D = center;
        this.radius = radius;

        updatePoints2D();
    }

    public Hexagon(int x, int y, int radius) {
        this(new Point(x, y), radius);
    }
    
    public Hexagon(double x, double y, int radius) {
        this(new Point2D.Double(x, y), radius);
    }
    
    

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;

        updatePoints();
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;

        updatePoints();
    }

    public void setCenter(Point center) {
        this.center = center;

        updatePoints();
    }

    public void setCenter(int x, int y) {
        setCenter(new Point(x, y));
    }

    private double findAngle(double fraction) {
        return fraction * Math.PI * 2 + Math.toRadians((rotation + 180) % 360);
    }

    private Point findPoint(double angle) {
        int x = (int) (center.x + Math.cos(angle) *radius);
        int y = (int) (center.y + Math.sin(angle)*radius);

        return new Point(x, y);
    }
    private Point2D findPoint2D(double angle) {
        int x = (int) (center2D.getX() + Math.cos(angle) *radius);
        int y = (int) (center2D.getY() + Math.sin(angle)*radius);

        return new Point2D.Double(x, y);
    }

    protected void updatePoints() {
        for (int p = 0; p < SIDES; p++) {
            double angle = findAngle((double) p / SIDES);
            Point point = findPoint(angle);
            xpoints[p] = point.x;
            ypoints[p] = point.y;
            points[p] = point;
        }
    }
    
    protected void updatePoints2D() {
        for (int p = 0; p < SIDES; p++) {
            double angle = findAngle((double) p / SIDES);
            Point2D point = findPoint2D(angle);
            xpoints2D[p] = point.getX();
            ypoints2D[p] = point.getY();
            points2D[p] = point;
        }
    }

    public void draw(Graphics2D g, int x, int y, int lineThickness, boolean filled) {
        // Store before changing.
        Stroke tmpS = g.getStroke();
        Color tmpC = g.getColor();

        //g.setStroke(new BasicStroke(lineThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

        if(filled)
            g.fillPolygon(xpoints, ypoints, npoints);
        else
        	g.drawPolygon(xpoints, ypoints, npoints);

        // Set values to previous when done.
        g.setColor(tmpC);
        g.setStroke(tmpS);
    }
    

    public void draw2D(Graphics2D g, double x, double y, int lineThickness, boolean filled) {
        // Store before changing.
        Stroke tmpS = g.getStroke();
        Color tmpC = g.getColor();

        //g.setStroke(new BasicStroke(lineThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        
        Shape s = this;
        
        g.draw(s);
        
       /* math.geom2d.polygon.SimplePolygon2D poly = new SimplePolygon2D();
		
        if(filled)
            g.fillPolygon(xpoints2D, ypoints2D, npoints);
        else
        	g.drawPolygon(xpoints2D, ypoints2D, npoints);*/

        // Set values to previous when done.
        g.setColor(tmpC);
        g.setStroke(tmpS);
    }
}