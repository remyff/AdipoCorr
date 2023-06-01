package fr.inserm.i2mc.adipocorr_;

import ij.gui.GenericDialog;

public class GUI_parameters extends GenericDialog{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static int dia_start=10,dia_stop=150;
	static int dia_step = 2;
	static double strip = 0.25;
	static boolean upscale = false;
	static int scaling = 1;
	int[] diameters;
	int[] diameters_scaled;
	public GUI_parameters(){
		super("Cross-correlations parameters");
		this.addNumericField("strip size ratio", strip, 2);
		this.addNumericField("lower diameter", dia_start, 0);
		this.addNumericField("upper diameter", dia_stop, 0);
		this.addNumericField("diameter step", dia_step, 0);
		this.addCheckbox("upscale", upscale);
	}
	public boolean run(){
		this.showDialog();
		if(this.wasCanceled()) {return false;}
		strip = this.getNextNumber();
	    dia_start = (int)this.getNextNumber();
	    dia_stop = (int)this.getNextNumber();
	    dia_step = (int)this.getNextNumber();
	    upscale = this.getNextBoolean();
	    if(upscale) {scaling = 2;}
		else {
			scaling = 1;
			if(dia_step%2 ==1) {dia_step++;}
			}
	    int dia_n = (dia_stop-dia_start)/dia_step+1;
		diameters = new int[dia_n];
		diameters_scaled = new int[dia_n];
		for(int i = 0;i<dia_n;i++) {
			diameters[i]=(dia_start+dia_step*i);
			diameters_scaled[i]=diameters[i]*scaling;
		}
	    return true;
	}
	public int[] get_diameters() {return diameters;}
	public int[] get_diameters_scaled() {return diameters_scaled;}
	public int get_step() {return dia_step;}
	public double get_strip() {return strip;}
	public int get_scaling() {return scaling;}
	public boolean get_upscale() {return upscale;}
}
