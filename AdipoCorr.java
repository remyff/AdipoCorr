import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;

import java.awt.Polygon;
import java.awt.Color;
import java.awt.TextField;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;

/*import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import cvforge.CVForge;
import cvforge.Executer;*/

//import ijopencv.ij.ImagePlusMatConverter;

/**
* AdipoCorr.java
* @author Moi-même
*/

public class AdipoCorr implements PlugIn {
	public void run(String str) {

		/*CVForge forge = new CVForge();
		String lib = forge.activeLib();*/
		IJ.showMessage(lib);
		CodedImage codedProj = codedMaxProjection(image.getStack());
		ImagePlus proj = codedProj.getImage();
		ImagePlus depth = codedProj.getCode();
		proj.show();
		Dialog dialog = new Dialog(proj,depth);
		if(dialog.wasOKed()){
			RoiManager rm = addPositonsToRoiManager(dialog.getPositions());
			rm.runCommand(proj,"Show All with labels");
		}
		
	}
	public class Dialog extends NonBlockingGenericDialog{
		private double tolerance;
		private double threshold;
		//private ImagePlus image;
		private ImagePlus proj;
		private ImagePlus depth;
		private List<Position> positions;
		public List<Position> getPositions(){return positions;}
		public Dialog(ImagePlus proj, ImagePlus depth){
			super("plop");
			tolerance = 0.1;
			threshold = 0.7;
			this.proj = proj;
			this.depth = depth;
			this.addSlider("tolerance", 0, 1, tolerance);
			this.addSlider("threshold", 0, 1, threshold);
			positions = findPositions(proj.getProcessor(),depth.getProcessor(),tolerance,threshold);
			displayPositions(proj,positions);
			this.showDialog();
		}
		public void textValueChanged(java.awt.event.TextEvent e){
			super.textValueChanged(e);
			Vector vec = getNumericFields();
			TextField source1 = (TextField)vec.elementAt(0);
			TextField source2 = (TextField)vec.elementAt(1);
			try{tolerance = Double.parseDouble(source1.getText());
				threshold = Double.parseDouble(source2.getText());
				positions = findPositions(proj.getProcessor(),depth.getProcessor(),tolerance,threshold);
				displayPositions(proj,positions);
				}
			catch(NumberFormatException plop){}
		}
	}
	public RoiManager addPositonsToRoiManager(List<Position> positions){
		RoiManager rm = RoiManager.getRoiManager();
		rm.reset();
		for(Position pos : positions){
			pos.addToRoiManager(rm);
		}
		return rm;
	}
	public class Position{
		private int x;
		private int y;
		private int radius;
		public Position(int x,int y,int radius){
			this.x=x;
			this.y=y;
			this.radius=radius;
		}
		public int x(){return x;}
		public int y(){return y;}
		public int radius(){return radius;}
		public void addToRoiManager(RoiManager rm){rm.addRoi(new OvalRoi(x()-radius,y()-radius,radius*2,radius*2));}
	}
	public void displayPositions(ImagePlus image, List<Position> positions){
		Overlay overlay = new Overlay();
		for(Position pos : positions){
			int rad = pos.radius()+2;
			overlay.add(new OvalRoi(pos.x()-rad,pos.y()-rad,rad*2,rad*2));
		}
		overlay.setStrokeColor(Color.RED);
		image.setOverlay(overlay);
	}
	public List<Position> findPositions(ImageProcessor projection,ImageProcessor depth,double tolerance,double threshold){
		MaximumFinder mf = new MaximumFinder();
		Polygon polygon = mf.getMaxima(projection,tolerance, false);
		int[] X=polygon.xpoints;
		int[] Y=polygon.ypoints;
		int nb = X.length;
		List<Position> res = new ArrayList<Position>();
		for(int i=0;i<nb;i++){
			float val = projection.getf(X[i],Y[i]);
			if(val>=threshold){
				int rad = depth.get(X[i],Y[i]);
				res.add(new Position(X[i],Y[i],rad));}
		}
		return res;
	}
	public CodedImage codedMaxProjection(ImageStack stack){ //float type only and not on hyperstack
		FloatProcessor fp = new FloatProcessor(stack.getWidth(),stack.getHeight());
		ShortProcessor pos = new ShortProcessor(stack.getWidth(),stack.getHeight());
		short stopSlice = (short)stack.getSize();
		MaxIntensity maxInt = new MaxIntensity(fp,pos);
		for (short n=1; n<=stopSlice; n+=1) {
	    	maxInt.projectSlice((float[])stack.getPixels(n),n); 
		}
		return new CodedImage(new ImagePlus("projection", fp),new ImagePlus("position",pos));
	}
     /** Compute max intensity projection. */
    class MaxIntensity {
    	private float[] fpixels;
    	private short[] ppixels;
 		private int len;
		/** Simple constructor since no preprocessing is necessary. */
		public MaxIntensity(FloatProcessor fp, ShortProcessor pos) {
			fpixels = (float[])fp.getPixels();
			ppixels = (short[])pos.getPixels();
			len = fpixels.length;
			for (int i=0; i<len; i++)
				fpixels[i] = -Float.MAX_VALUE;
		}
		public void projectSlice(float[] pixels, short slice) {
	    	for(int i=0; i<len; i++) {
				if(pixels[i]>fpixels[i]){
		    		fpixels[i] = pixels[i];
		    		ppixels[i] = slice;
		    	}
	    	}
		}
    } // end MaxIntensity
	class CodedImage{
		private ImagePlus image;
		private ImagePlus code;
		public CodedImage(ImagePlus image,ImagePlus code){
			this.image = image;
			this.code = code;
		}
		public ImagePlus getImage(){
			return image;
		}
		public ImagePlus getCode(){
			return code;
		}
	}
}