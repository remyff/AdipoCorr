package fr.inserm.i2mc.adipocorr_;

import ij.ImagePlus;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;

import java.awt.Color;
import java.util.List;
import java.awt.TextField;
import java.util.Vector;

public class GUI_liveMaxSelection extends NonBlockingGenericDialog{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double tolerance;
	private double threshold;
	private double homogeneity;
	private ImagePlus image;
	private int scaling;
	private ImagePlus proj;
	private ImagePlus depth;
	private List<Adipocyte> adipocytes;
	public List<Adipocyte> getAdipocytes(){return adipocytes;}
	public GUI_liveMaxSelection(ImagePlus image,ImagePlus proj, ImagePlus depth, int scaling){
		super("Adipocytes selection");
		tolerance = 0.1;
		threshold = 0.7;
		homogeneity = 0.5;
		this.scaling = scaling;
		this.proj = proj;
		this.depth = depth;
		this.image = image;
		this.addSlider("tolerance", 0.01, 1, tolerance,0.01);
		this.addSlider("threshold", 0, 1, threshold,0.01);
		this.addSlider("homogeneity", 0, 1, homogeneity,0.01);
		//find all adipocyte with tolerance = 0.01 to calculate the homogeneity
		adipocytes = AdipoCorr_.findAdipocytes(proj.getProcessor(),depth.getProcessor(),0.01,threshold,scaling);
		
		adipocytes = AdipoCorr_.findAdipocytes(proj.getProcessor(),depth.getProcessor(),tolerance,threshold,scaling);
		displayAdipocytes(image,adipocytes);
		this.showDialog();
	}
	public void textValueChanged(java.awt.event.TextEvent e){
		super.textValueChanged(e);
		@SuppressWarnings("rawtypes")
		Vector vec = getNumericFields();
		TextField source1 = (TextField)vec.elementAt(0);
		TextField source2 = (TextField)vec.elementAt(1);
		TextField source3 = (TextField)vec.elementAt(2);
		try{tolerance = Double.parseDouble(source1.getText());
			threshold = Double.parseDouble(source2.getText());
			homogeneity = Double.parseDouble(source3.getText());
			if(tolerance<0.01) {source1.setText(String.valueOf(0.01));}
			else if(tolerance>1) {source1.setText(String.valueOf(1));}
			else if(threshold<0) {source2.setText(String.valueOf(0));}
			else if(threshold>1) {source2.setText(String.valueOf(1));}
			else if(homogeneity<0) {source3.setText(String.valueOf(0));}
			else if(homogeneity>1) {source3.setText(String.valueOf(1));}
			else {
				adipocytes = AdipoCorr_.findAdipocytes(proj.getProcessor(),depth.getProcessor(),tolerance,threshold,scaling);
				displayAdipocytes(image,adipocytes);}
			}
		catch(NumberFormatException plop){}
	}
	
	public void displayAdipocytes(ImagePlus image, List<Adipocyte> adipocytes){
		Overlay overlay = new Overlay();
		for(Adipocyte pos : adipocytes){
			int diameter = pos.diameter();
			overlay.add(new OvalRoi(pos.x()-diameter/2,pos.y()-diameter/2,diameter,diameter));
		}
		overlay.setStrokeColor(Color.RED);
		image.setOverlay(overlay);
	}
}