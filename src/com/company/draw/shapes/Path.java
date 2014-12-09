package com.company.draw.shapes;

import com.company.*;
import com.company.Point;
import com.company.draw.*;
import org.ejml.simple.*;
import spark.data.*;
import sun.reflect.generics.reflectiveObjects.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import static com.company.draw.shapes.WidgetUtils.*;
import static java.lang.Math.*;

public class Path extends SOReflect implements Drawable, Selectable, Interactable, Layout, ModelListener {

	public SA contents;
	public SA path;
	public SA model;
	public double width;
	public double height;
	public double columnSpan;
	public SO slider;
	public double sliderVal;

	private Selectable selected = null;

	private ArrayList<Point> pointsList = null;
	private int pointCount = 1;
	private double originalWidth = -1;
	private double originalHeight = -1;

	private double currTop = 0;
	private double currLeft = 0;


	private SimpleMatrix cr;
	private SimpleMatrix pts;
	private SimpleMatrix t;
	private int currsliderSegment = 0;

	public Path() {
		WidgetUtils.addListener(this);
	}


	// DRAWABLE
	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		double curveLeft = 0;
		double curveTop = 0;

		for (int i = 0; i < contents.size(); i++) {
			SO so = contents.get(i).getSO();
			Drawable drawable = (Drawable) so;
			if (drawable instanceof Curve) {
				Curve curve = (Curve) drawable;
				curveTop = curve.currTop;
				curveLeft = curve.currLeft;
				g2.translate((int) currLeft, (int) currTop);
				drawable.paint(g2);
				g2.translate((int) -currLeft, (int) -currTop);
			} else {
				drawable.paint(g2);
			}
		}
		setSliderPoint();
		double rotation = getSliderRotation();
		double oldRotation = getSliderGroup().rotate;
		getSliderGroup().rotate -= Math.toDegrees(rotation);
		g2.translate((int) currLeft + curveLeft, (int) currTop + curveTop);
		getSliderGroup().paint(g2);
		g2.translate((int) -(currLeft + curveLeft), (int) -(currTop + curveTop));
		getSliderGroup().rotate = oldRotation;

//		g.setColor(Color.black);
//		ArrayList<Point> points = getPoints();
//		for (Point point : points) {
//			g2.drawRect((int) point.getX(), (int) point.getY(), 8, 8);
//		}
	}


	//	INTERACTABLE
	@Override
	public Root getPanel() {
		throw new NotImplementedException();
	}

	@Override
	public boolean key(char key) {
		return false;
	}

	@Override
	public void makeIdle() {}

	@Override
	public boolean mouseDown(double x, double y, AffineTransform myTransform) {
		if (mouseIsOnSlider(WidgetUtils.mouseType.DOWN, x, y, myTransform)) {
			WidgetUtils.setSliderBeingUsed(this);
			return true;
		} else {
			return handleMouse(contents, x, y, myTransform, WidgetUtils.mouseType.DOWN);
		}
	}

	@Override
	public boolean mouseMove(double x, double y, AffineTransform myTransform) {
		if (WidgetUtils.sliderBeingUsed(this)) {
			Point2D ptSrc = new Point(x, y);
			Point2D ptDst = myTransform.transform(ptSrc, null);
			Point nearestPoint = findNearestPoint(new Point(ptDst.getX(), ptDst.getY()));
			this.getSliderGroup().tx = nearestPoint.getX();
			this.getSliderGroup().ty = nearestPoint.getY();
			return true;
		}
		return handleMouse(contents, x, y, myTransform, mouseType.MOVE);
	}

	@Override
	public boolean mouseUp(double x, double y, AffineTransform myTransform) {
		return handleMouse(contents, x, y, myTransform, WidgetUtils.mouseType.UP);
	}

	//	LAYOUT
	@Override
	public double getColSpan() {
		return columnSpan;
	}

	private double getOriginalWidth(){
		if(this.originalWidth == -1) {
			this.originalWidth = this.width;
			for (int i = 0; i < contents.size(); i++) {
				SV sv = contents.get(i);
				SO so = sv.getSO();
				if (so instanceof Layout) {
					Layout layout = (Layout) so;
					layout.setHBounds(0, this.width);
				}
			}
		}
		return originalWidth;
	}

	private double getOriginalHeight(){
		if(this.originalHeight == -1) {
			this.originalHeight = this.height;
			for (int i = 0; i < contents.size(); i++) {
				SV sv = contents.get(i);
				SO so = sv.getSO();
				if (so instanceof Layout) {
					Layout layout = (Layout) so;
					layout.setVBounds(0, this.height);
				}
			}
		}
		return originalHeight;
	}

	@Override
	public double getMinWidth() {
		return getOriginalWidth();
	}

	@Override
	public double getDesiredWidth() {
		return getOriginalWidth();
	}

	@Override
	public double getMaxWidth() {
		return getOriginalWidth();
	}

	@Override
	public double getMinHeight() {
		return getOriginalHeight();
	}

	@Override
	public double getDesiredHeight() {
		return getOriginalHeight();
	}

	@Override
	public double getMaxHeight() {
		return getOriginalHeight();
	}


	//	MODEL LISTENER
	@Override
	public void modelUpdated(ArrayList<String> modelPath, String newValue) {
		if (slider == null || modelPath.size() != model.size()) return;

		for (int i = 0; i < model.size(); i++) // Verify that it matches
			if (!modelPath.get(i).equals(model.getString(i))) return; //IT WASN'T A MATCH
		getWindowCoordsFromSlideVal(Double.valueOf(newValue));
	}

	@Override
	public void setHBounds(double left, double right) {
		this.currLeft = left;
		for (int i = 0; i < contents.size(); i++) {
			SV sv = contents.get(i);
			SO so = sv.getSO();
			if (so instanceof Layout) {
				Layout layout = (Layout) so;
				layout.setHBounds(left, right);
			}
		}
		getSliderGroup().setHBounds(left, right);
		this.width = right - left;
		if (originalWidth == -1) { //I don't think this ever occurs here
			originalWidth = this.width;
		}
		recalibratePoints();
	}

	@Override
	public void setVBounds(double top, double bottom) {
		this.currTop = top;
		for (int i = 0; i < contents.size(); i++) {
			SV sv = contents.get(i);
			SO so = sv.getSO();
			if (so instanceof Layout) {
				Layout layout = (Layout) so;
				layout.setVBounds(top, bottom);
			}
		}
		getSliderGroup().setVBounds(top, bottom);
		this.height = bottom - top;
		if(originalHeight == -1) { //I don't think this ever occurs here
			originalHeight = this.height;
		}
		recalibratePoints();
	}



	///////////////////////////////////////////////////////////////////////////////////
	/////PRIVATE CLASS METHODS/////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////
	private void createMatrices() {
		cr = new SimpleMatrix(4, 4);
		cr.set(0, 0, -0.5);
		cr.set(0, 1, 1);
		cr.set(0, 2, -0.5);
		cr.set(0, 3, 0);
		cr.set(1, 0, 1.5);
		cr.set(1, 1, -2.5);
		cr.set(1, 2, 0);
		cr.set(1, 3, 1);
		cr.set(2, 0, -1.5);
		cr.set(2, 1, 2);
		cr.set(2, 2, 0.5);
		cr.set(2, 3, 0);
		cr.set(3, 0, 0.5);
		cr.set(3, 1, -0.5);
		cr.set(3, 2, 0);
		cr.set(3, 3, 0);

		this.pts = updatePointsMatrix(currsliderSegment);
		this.t = updateMatrixT(this.sliderVal);
	}

	private SimpleMatrix updatePointsMatrix(int segNum) {
		SimpleMatrix newPts = new SimpleMatrix(2, 4);
//		Get "i-1"
		if (segNum == 0) {
			newPts.set(0, 0, pointsList.get(0).getX());
			newPts.set(1, 0, pointsList.get(0).getY());
		} else {
			newPts.set(0, 0, pointsList.get(segNum - 1).getX());
			newPts.set(1, 0, pointsList.get(segNum - 1).getY());
		}
//		Get "i"
		newPts.set(0, 1, pointsList.get(segNum).getX());
		newPts.set(1, 1, pointsList.get(segNum).getY());
//		Get "i+1"
		if (segNum + 1 < pointsList.size()) {
			newPts.set(0, 2, pointsList.get(segNum + 1).getX());
			newPts.set(1, 2, pointsList.get(segNum + 1).getY());
		} else {
			newPts.set(0, 2, pointsList.get(segNum).getX());
			newPts.set(1, 2, pointsList.get(segNum).getY());
		}
//		Get "i+2"
		if (segNum + 2 < pointsList.size()) {
			newPts.set(0, 3, pointsList.get(segNum + 2).getX());
			newPts.set(1, 3, pointsList.get(segNum + 2).getY());
		} else if (segNum + 1 < pointsList.size()) {
			newPts.set(0, 3, pointsList.get(segNum + 1).getX());
			newPts.set(1, 3, pointsList.get(segNum + 1).getY());
		} else {
			newPts.set(0, 3, pointsList.get(segNum).getX());
			newPts.set(1, 3, pointsList.get(segNum).getY());
		}
		return newPts;
	}

	private SimpleMatrix updateMatrixT(double slideVal) {
		SimpleMatrix newT = new SimpleMatrix(4, 1);
		newT.set(0, 0, pow(slideVal, 3));
		newT.set(1, 0, pow(slideVal, 2));
		newT.set(2, 0, slideVal);
		newT.set(3, 0, 1);
		return newT;
	}

	private SimpleMatrix getDerivativeOfT() {
		double slideVal = sliderVal;
		SimpleMatrix newT = new SimpleMatrix(4, 1);
		newT.set(0, 0, 3 * pow(slideVal, 2));
		newT.set(1, 0, 2 * slideVal);
		newT.set(2, 0, 1);
		newT.set(3, 0, 0);
		return newT;
	}

	private void recalibratePoints() {
		ArrayList<Point> originalPoints = getPoints();
		double vertDiffRatio = 1 + ((this.height - this.originalHeight) / this.originalHeight);
		double horzDiffRatio = 1 + ((this.width - this.originalWidth) / this.originalWidth);
		this.pointsList = new ArrayList<>();
		for (Point point : originalPoints) {
			point.setX(point.getX() * horzDiffRatio);
			point.setY(point.getY() * vertDiffRatio);
			pointsList.add(point);
		}
		this.pts = updatePointsMatrix(currsliderSegment);
		this.t = updateMatrixT(this.sliderVal);
	}

	private ArrayList<Point> getPoints() {
		int[] xArray = getPoints("X");
		int[] yArray = getPoints("Y");
		ArrayList<Point> points = new ArrayList<>();
		for (int i = 0; i < xArray.length; i++) {
			Point point = new Point(xArray[i], yArray[i]);
			points.add(point);
		}
		return points;
	}

	private int[] getPoints(String coord) {
		int[] xArray = new int[path.size()];
		int[] yArray = new int[path.size()];
		for (int i = 0; i < path.size(); i++) {
			SV xPoint = path.get(i).get("x");
			SV yPoint = path.get(i).get("y");
			try {
				Long x = xPoint.getLong();
				Long y = yPoint.getLong();
				xArray[i] = x.intValue();
				yArray[i] = y.intValue();
			} catch (Exception e) {
				double x = xPoint.getDouble();
				double y = yPoint.getDouble();
				xArray[i] = (int) x;
				yArray[i] = (int) y;
			}
		}
		if (coord.equals("X")) {
			return xArray;
		} else {
			return yArray;
		}
	}

	private double getSliderRotation() {
		SimpleMatrix derivT = getDerivativeOfT();
		Point point = getNewSlideLoc(this.pts, derivT);
		return Math.atan2(point.getY(), point.getX());
	}

	private void setSliderPoint() {
		if (model == null) return;
		Point updatedSlideLocation = getNewSlideLoc();
		getSliderGroup().tx = updatedSlideLocation.getX();
		getSliderGroup().ty = updatedSlideLocation.getY() - 2;
		double modelValue = (sliderVal + this.currsliderSegment) / (this.pointCount - 1);
		WidgetUtils.updateModel(model, String.valueOf(modelValue));
	}

	private void getWindowCoordsFromSlideVal(Double modelValue) {
		if (this.pointCount <= 1) {
			this.pointsList = getPoints();
			this.pointCount = this.pointsList.size();
		} else if (sliderVal == ((this.pointCount - 1) * modelValue) - this.currsliderSegment) return;

		this.currsliderSegment = (int) Math.ceil(modelValue * (this.pointCount - 1)) - 1;
		if (this.currsliderSegment < 0) this.currsliderSegment = 0;

		sliderVal = ((this.pointCount - 1) * modelValue) - this.currsliderSegment;
		this.pts = updatePointsMatrix(currsliderSegment);
		this.t = updateMatrixT(this.sliderVal);
		setSliderPoint();
	}

	public Group getSliderGroup() {
		return (Group) slider;
	}

	private boolean mouseIsOnSlider(WidgetUtils.mouseType mouseType, double x, double y, AffineTransform oldTrans) {
//		x -= currLeft + 10; // + getSliderGroup().tx;
//		y -= currTop + 10; // + getSliderGroup().ty;
		double curveLeft = 0;
		double curveTop = 0;

		for (int i = 0; i < contents.size(); i++) {
			SO so = contents.get(i).getSO();
			Drawable drawable = (Drawable) so;
			if (drawable instanceof Curve) {
				Curve curve = (Curve) drawable;
				curveTop = curve.currTop;
				curveLeft = curve.currLeft;
			}
		}
		double rotation = getSliderRotation();
		double oldRotation = getSliderGroup().rotate;
		getSliderGroup().rotate -= Math.toDegrees(rotation);
		x -= currLeft + curveLeft;
		y -= currTop + curveTop;
//		Get the slider now that it's rotated
		ArrayList<Drawable> drawables = new ArrayList<>();
		drawables.add(getSliderGroup());
		boolean result = handleMouse(drawables, x, y, oldTrans, mouseType);

//		Remove the rotation that was added and return
		getSliderGroup().rotate = oldRotation;
		return result;
	}

	public Point getNewSlideLoc() {
		if (cr == null) createMatrices();
		return getNewSlideLoc(this.pts, this.t);
	}

	public Point getNewSlideLoc(SimpleMatrix points, SimpleMatrix tVals) {
		if (cr == null) createMatrices();
		SimpleMatrix pointsWithCatmull = points.mult(cr);
		SimpleMatrix newPoint = pointsWithCatmull.mult(tVals);
		return new Point(newPoint.get(0),newPoint.get(1));
	}

	@Override
	public ArrayList<Integer> select(double x, double y, int myIndex, AffineTransform transform) {
		for (int i = contents.size() - 1; i >= 0; i--) {
			SV sv = contents.get(i);
			SO so = sv.getSO();
			if (!(so instanceof Selectable)) continue;
			Selectable selectable = (Selectable) so;
			ArrayList<Integer> path = selectable.select(x, y, i, transform);
			if (path != null) {
				selected = selectable;
				path.add(myIndex);
				return path;
			}
		}
		return null;
	}

	@Override
	public Point2D[] controls() {
		if (selected != null) return selected.controls();
		else return new Point2D[0];
	}

	@Override
	public void setBackgroundColor(SO newColor) { }

	private static class tuple {
		public double sliderValue;
		public Point closestPoint;
		public tuple(double sliderValue, Point closestPoint) {
			this.sliderValue = sliderValue;
			this.closestPoint = closestPoint;
		}
	}

	private Point findNearestPoint(Point convertedPoint) {
		double dist = Double.MAX_VALUE;
		Point returnPoint = null;
		double bestSliderVal = 0;
		for (int i = 0; i < this.pointCount - 1; i++) {
			tuple bestTuple = getClosestPointInSegment(i, convertedPoint, 0, 1);
			Point closestInSegment = bestTuple.closestPoint;
			double closestInSegDist = getDistance(convertedPoint, closestInSegment);
			if (closestInSegDist < dist) {
				this.currsliderSegment = i;
				bestSliderVal = bestTuple.sliderValue;
				dist = closestInSegDist;
				returnPoint = closestInSegment;
			}
		}
		this.t = updateMatrixT(bestSliderVal);
		this.pts = updatePointsMatrix(this.currsliderSegment);
		this.sliderVal = bestSliderVal;
		return returnPoint;
	}


	private tuple getClosestPointInSegment(int segmentNum, Point clickPoint, double low, double high) {
		SimpleMatrix points = updatePointsMatrix(segmentNum);
		SimpleMatrix lowTVals = updateMatrixT(low);
		Point lowPoint = getNewSlideLoc(points, lowTVals);
		double lowDist = getDistance(clickPoint, lowPoint);

		SimpleMatrix highTVals = updateMatrixT(high);
		Point highPoint = getNewSlideLoc(points, highTVals);
		double highDist = getDistance(clickPoint, highPoint);

		double diff = high - low;
		if (diff < 0.05) {
			if (lowDist < highDist) return new tuple(low, lowPoint);
			else return new tuple(high, highPoint);
		} else if (lowDist < highDist) {
			return getClosestPointInSegment(segmentNum, clickPoint, low, (low + (diff / 2)));
		} else {
			return getClosestPointInSegment(segmentNum, clickPoint, (low + (diff / 2)), high);
		}
	}

	private double getDistance(Point from, Point to) {
		return Math.sqrt((from.getX() - to.getX()) * (from.getX() - to.getX()) + (from.getY() - to.getY()) * (from.getY() - to.getY()));
	}
}