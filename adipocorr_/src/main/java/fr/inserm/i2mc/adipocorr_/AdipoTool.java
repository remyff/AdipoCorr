package fr.inserm.i2mc.adipocorr_;

import ij.plugin.tool.PlugInTool;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.ImageCanvas;
import ij.measure.ResultsTable;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.List;

public class AdipoTool extends PlugInTool {
	private int diameter;
	private int firstDia;
	int dia_step;
	
	private List<Adipocyte> adipocytes;
	public List<Adipocyte> getAdipocytes(){return adipocytes;}
	
	private Overlay overlay;
	
	int x;
	int y;
	
	int cs; //cross size
	
	ImagePlus actualImp;
	
	ResultsTable table;
	
	public AdipoTool(List<Adipocyte> adipocytes, ImagePlus imp, int dia_step, int firstDia) {
		this.adipocytes = adipocytes;
		this.dia_step = dia_step;
		this.firstDia = firstDia;
		this.diameter = this.firstDia;
		this.cs = 3;
		this.table = new ResultsTable();
		this.table.showRowNumbers(true);
		updateTable();
		ImageCanvas ic = imp.getCanvas();
		MouseWheelEventAdipo wheelEvent = new MouseWheelEventAdipo(this);
		ic.addMouseWheelListener(wheelEvent);
		this.actualImp = imp;
		constructOverlay();
	}
	
	public void mousePressed(ImagePlus imp, MouseEvent e) {
		diameter = diameter - ((diameter-firstDia)%dia_step);
		ImageCanvas ic = imp.getCanvas();
		int x = ic.offScreenX(e.getX());
		int y = ic.offScreenY(e.getY());
		int i = 0;
		boolean adinotfound = true;
		while((i < this.adipocytes.size())&&adinotfound) {
			if((Math.abs((x-adipocytes.get(i).x()))<=this.cs)&&(Math.abs((y-adipocytes.get(i).y()))<=this.cs)) {
				adinotfound=false;
			}
			else {
				i++;
			}
		}
		if(adinotfound){this.adipocytes.add(new Adipocyte(x,y,this.diameter));}
		else {this.adipocytes.remove(i);}
		constructOverlay();
		imp.setOverlay(this.overlay);
		updateTable();
	}
	
	public void mouseMoved(ImagePlus imp, MouseEvent e) {
		diameter = diameter - ((diameter-firstDia)%dia_step);
		ImageCanvas ic = imp.getCanvas();
		this.x = ic.offScreenX(e.getX());
		this.y = ic.offScreenY(e.getY());
		this.actualImp = imp;
		updateCircle();
	}
	
	public void mouseEntered(ImagePlus imp, MouseEvent e) {
		ImageCanvas ic = imp.getCanvas();
		MouseWheelEventAdipo wheelEvent = new MouseWheelEventAdipo(this);
		ic.addMouseWheelListener(wheelEvent);
		this.actualImp = imp;
	}
	
	private void constructOverlay() {
		Overlay newOV = new Overlay();
		for(Adipocyte pos : this.adipocytes){
			int diameter = pos.diameter();
			newOV.add(new OvalRoi(pos.x()-diameter/2,pos.y()-diameter/2,diameter,diameter));
			newOV.add(new Line(pos.x()-this.cs,pos.y(),pos.x()+this.cs,pos.y()));
			newOV.add(new Line(pos.x(),pos.y()-this.cs,pos.x(),pos.y()+this.cs));
		}
		newOV.setStrokeColor(Color.RED);
		this.overlay = newOV;
	}
	
	public void updateCircle() {
		this.overlay.remove("temp");
		this.overlay.add(new OvalRoi(this.x-this.diameter/2,this.y-this.diameter/2,this.diameter,this.diameter),"temp");
		actualImp.setOverlay(this.overlay);
	}
	
	public void diameterChanged(int notches) {
		this.diameter = this.diameter - notches;
		if(diameter<firstDia) {diameter=firstDia;}
		updateCircle();
	}
	
	public void updateTable() {
		this.table.reset();
		for(Adipocyte pos : this.adipocytes){
			this.table.incrementCounter();
			this.table.addValue("x", pos.x());
			this.table.addValue("y", pos.y());
			this.table.addValue("diameter", pos.diameter());
		}
		this.table.show("result");
	}
}
