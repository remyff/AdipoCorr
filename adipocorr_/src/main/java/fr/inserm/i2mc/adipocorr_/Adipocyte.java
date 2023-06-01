package fr.inserm.i2mc.adipocorr_;


import ij.plugin.frame.RoiManager;

import ij.gui.OvalRoi;

public class Adipocyte {
	private int x;
	private int y;
	private int diameter;
	private double homogeneity;
	public Adipocyte(int x,int y,int diameter){
		this.x=x;
		this.y=y;
		this.diameter=diameter;
		this.homogeneity=0;
	}
	public int x(){return x;}
	public int y(){return y;}
	public int diameter(){return diameter;}
	public double homogeneity() {return homogeneity;}
	public void addToRoiManager(RoiManager rm){rm.addRoi(new OvalRoi(x()-diameter/2,y()-diameter/2,diameter,diameter));}
}
