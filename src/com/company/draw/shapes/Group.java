package com.company.draw.shapes;

import com.company.*;
import com.company.draw.*;
import spark.data.*;
import sun.reflect.generics.reflectiveObjects.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import static com.company.draw.shapes.WidgetUtils.*;

public class Group extends SOReflect implements Drawable, Selectable, Interactable, Layout {

	public SA contents;
	public double sx;
	public double sy;
	public double rotate;
	public double tx;
	public double ty;
	public double width;
	public double height;

	private int layoutCount = -1;
//	private double rowCount;
//	private double columnCount;

	public Group(){}

	public Group(SA contents, double sx, double sy, double tx, double ty, double rotate, double width, double height) {
		this.contents = contents;
		this.sx	= sx;
		this.sy = sy;
		this.tx = tx;
		this.ty = ty;
		this.rotate = rotate;
		this.width = width;
		this.height = height;
	}

	@Override
	public void paint(Graphics g) {
		int cSize = contents.size();
//		The original and next we transform and repaint
		Graphics2D g2 = (Graphics2D) g;
//		Perform Transformations
		if (sx != 0) g2.scale(sx, sy);
		g2.rotate(-Math.toRadians(rotate));
		g2.translate((int) tx, (int) ty);

//		Call Draw on all contained objects
		for (int i = 0; i < cSize; i++) {
			callPaintOnContents(contents.get(i), g2);
		}

//		Revert Transformations
		g2.translate((int) -tx, (int) -ty);
		g2.rotate(Math.toRadians(rotate));
		if (sx != 0) g2.scale(1 / sx, 1 / sy);
	}

	public void callPaintOnContents(SV sv, Graphics g) {
		SO so = sv.getSO();
		Drawable drawable = (Drawable) so;
		drawable.paint(g);
	}


	@Override
	public ArrayList<Integer> select(double x, double y, int myIndex, AffineTransform oldTrans) {
		AffineTransform transform = new AffineTransform();
		transform.translate((int) -tx, (int) -ty);
		transform.rotate(-Math.toRadians(rotate));
		transform.scale(1 / sx, 1 / sy);
		// Add on old transform
		transform.concatenate(oldTrans);

		for (int i = 0; i < contents.size(); i++) {
			SV sv = contents.get(i);
			SO so = sv.getSO();
			if(!(so instanceof Selectable)) continue;
			Selectable selectable = (Selectable) so;
			ArrayList<Integer> path = selectable.select(x, y, i, transform);
			if (path != null) {
				path.add(myIndex);
				return path;
			}
		}
		return null;
	}

	@Override
	public Point2D[] controls() {
		throw new UnsupportedOperationException("This method is not implemented");
	}

	@Override
	public void setBackgroundColor(SO newColor) {
		throw new NotImplementedException();
	}

	@Override
	public Root getPanel() {
		throw new NotImplementedException();
	}

	@Override
	public boolean key(char key) {
		return false;
	}

	@Override
	public boolean mouseDown(double x, double y, AffineTransform myTransform) {
		return callHandleMouse(WidgetUtils.mouseType.DOWN, x, y, myTransform);
	}

	@Override
	public boolean mouseMove(double x, double y, AffineTransform myTransform) {
		return callHandleMouse(WidgetUtils.mouseType.MOVE, x, y, myTransform);
	}

	@Override
	public boolean mouseUp(double x, double y, AffineTransform myTransform) {
		return callHandleMouse(WidgetUtils.mouseType.UP, x, y, myTransform);
	}

	private boolean callHandleMouse(WidgetUtils.mouseType mouseType, double x, double y, AffineTransform oldTrans) {
		AffineTransform newTransform = getTransform(tx, ty, sx, sy, rotate);
		// Add on old transform
		newTransform.concatenate(oldTrans);
		return handleMouse(contents, x, y, newTransform, mouseType);
	}


//	LAYOUT
	@Override
	public double getMinWidth() {
		return this.width;
	}

	@Override
	public double getDesiredWidth() {
		return this.width;
	}

	@Override
	public double getMaxWidth() {
		return 10000000;
	}

	@Override
	public double getMinHeight() {
		return this.height;
	}

	@Override
	public double getDesiredHeight() {
		return this.height;
	}

	@Override
	public double getMaxHeight() {
		return 10000000;
	}

	//	TODO: redo the setting, also look at pg. 114
	@Override
	public void setHBounds(double left, double right) {
		this.tx = left;
		this.width = right - left;

//		double individualWidth = width / getColumnCount();
		double individualWidth = width / getLayoutContentCount();
		double currLeft = left;
		for (int i = 0; i < contents.size(); i++) {
			SV sv = contents.get(i);
			SO so = sv.getSO();
			if (so instanceof Layout) {
				Layout layout = (Layout) so;
//				TODO: the next line might break with scales/rotations
				layout.setHBounds(left, right - tx);
//				layout.setHBounds(currLeft, currLeft + individualWidth);
//				currLeft += individualWidth;
			}
		}
	}

	@Override
	public void setVBounds(double top, double bottom) {
		this.ty = top;
		this.height = top - bottom;

//		double individualHeight = height / getRowCount();
		double individualHeight = height / getLayoutContentCount();
		double currTop = top;
		for (int i = 0; i < contents.size(); i++) {
			SV sv = contents.get(i);
			SO so = sv.getSO();
			if (so instanceof Layout) {
				Layout layout = (Layout) so;
//				TODO: the next line might break with scales/rotations
				layout.setVBounds(top - ty, bottom);
//				layout.setVBounds(currTop, currTop + individualHeight);
//				currTop += individualHeight;
			}
		}
	}

	private int getLayoutContentCount() {
		if (layoutCount == -1) {
			for (int i = 0; i < contents.size(); i++) {
				SV sv = contents.get(i);
				SO so = sv.getSO();
				if (so instanceof Layout) {
					layoutCount++;
				}
			}
		}
		return layoutCount;
	}

//	private double getRowCount() {
//		return rowCount;
//	}
//
//	private double getColumnCount() {
//		return columnCount;
//	}
}