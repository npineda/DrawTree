package com.company.draw.shapes;

import com.company.*;
import com.company.Point;
import com.company.draw.*;
import spark.data.*;
import sun.reflect.generics.reflectiveObjects.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import static com.company.draw.shapes.WidgetUtils.*;
import static com.company.draw.shapes.WidgetUtils.mouseType.*;

public class Button extends SOReflect implements Layout, Drawable, Interactable {

//	The content list is generated programatically based on the dimensions specified in setHBounds() and setVBounds()
//	public SA contents;
	public ArrayList<Drawable> contents = new ArrayList<>();
	public String label;
	public SA model;
	public String state;
	public SO idle;
	public SO hover;
	public SO active;
	public String value;
	public double columnSpan;



	private Ellipse ellipse = null;
	private Text text = null;
	private final double BEVEL = 3;
	private final Point MARGIN = new Point(3, 3);

	public Button() { }
	public Button(String label, SA model, String state, SO idle, SO hover, SO active, String value, double columnSpan) {
		this.label = label;
		this.model = model;
		this.state = state;
		this.idle = idle;
		this.hover = hover;
		this.active = active;
		this.value = value;
		this.columnSpan = columnSpan;
	}

	// INTERACTABLE
	@Override
	public Root getPanel() {
		throw new NotImplementedException();
	}

	@Override
	public boolean key(char key) {
		throw new NotImplementedException();
	}

	@Override
	public boolean mouseDown(double x, double y, AffineTransform myTransform) {
		return callHandleMouse(mouseType.DOWN, x, y, myTransform);
	}

	@Override
	public boolean mouseMove(double x, double y, AffineTransform myTransform) {
		return callHandleMouse(mouseType.MOVE, x, y, myTransform);
	}

	@Override
	public boolean mouseUp(double x, double y, AffineTransform myTransform) {
		if (this.state.equals("active")) {
			WidgetUtils.updateModel(this.model, this.value);
		}

		return callHandleMouse(UP, x, y, myTransform);
	}


	private boolean callHandleMouse(WidgetUtils.mouseType mouseType, double x, double y, AffineTransform myTransform) {
		boolean isHandled = handleMouse(contents, x, y, myTransform, mouseType);
		if (!isHandled) {
			this.state = "idle";
			changeState(this.idle);
		} else {
			if (WidgetUtils.getMouseStatus() == MouseStatus.PRESSED) {
				this.state = "active";
				changeState(this.active);
			} else {
				this.state = "hover";
				changeState(this.hover);
			}
		}
		return isHandled;
	}

	public void changeState(SO newState) {
		if(this.text == null) return;
		this.ellipse.setBackgroundColor(newState);
		this.text.text = this.label;
	}

	// DRAWABLE
	@Override
	public void paint(Graphics g) {
		if (ellipse == null || text == null) return;
		ellipse.paint(g);
		text.paint(g);
	}

	//	LAYOUT
	private void initializeContents() {
		int ellipseWidth = 40;
		int ellipseHeight = 40;
		this.text = new Text(this.label, 0, ellipseHeight / 2, "sans-serif", 36, false, -1);
		Graphics g = WidgetUtils.graphics;
		this.text.setFontMetrics(g);
		this.text.adjustFontWidth(this.label, 0, ellipseWidth);
		this.text.adjustFontHeight(0, ellipseHeight);
		this.ellipse = new Ellipse(0, 0, ellipseWidth, ellipseHeight);
		this.ellipse.setBackgroundColor(this.idle);
		this.contents.add(ellipse);
		this.contents.add(text);
	}

	@Override
	public double getColSpan() {
		return columnSpan;
	}

//	This should pick a default font size and then report

	// min size that will create a button of that size based on the contents of the label attribute
	@Override
	public double getMinWidth() {
		return (BEVEL*2) + (getText().getTextWidth());
	}

	@Override
	public double getMinHeight() {
		return (BEVEL*2) + (getText().getFontMetrics().getHeight());
	}

	// desired size that will create a button of that size based on the contents of the label attribute
	@Override
	public double getDesiredWidth() {
		return (BEVEL*2) + (MARGIN.getWidth()*2) + (getText().getTextWidth());
	}

	@Override
	public double getDesiredHeight() {
		return (BEVEL*2) + (MARGIN.getHeight()*2) + (getText().getFontMetrics().getHeight());
	}

	// max size that will create a button of that size based on the contents of the label attribute
	@Override
	public double getMaxWidth() {
		return getDesiredWidth();
	}

	@Override
	public double getMaxHeight() {
		return getDesiredHeight();
	}


	//	When the sizes are actually received via setHBounds() and setVBounds() you should adapt the font size so that the label, border and other visuals will fit in the bounds.
	@Override
	public void setHBounds(double left, double right) {
		if(this.text == null) initializeContents();
		ellipse.left = left;
		ellipse.width = right - left;
//		ellipse.width = min(getMaxWidth(), right - left);
		ellipse.setBackgroundColor(this.idle);
		text.adjustFontWidth(this.label, left, ellipse.width);
	}


	@Override
	public void setVBounds(double top, double bottom) {
		if(this.text == null) initializeContents();
		ellipse.top = top;
		ellipse.height = bottom - top;
//		ellipse.height = min(getMaxHeight(), bottom - top);
		ellipse.setBackgroundColor(this.idle);
		text.adjustFontHeight(top, ellipse.height);
	}

	private Text getText() {
		if(this.text == null) initializeContents();
		return text;
	}
}
