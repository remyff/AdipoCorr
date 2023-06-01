package fr.inserm.i2mc.adipocorr_;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.plugin.filter.MaximumFinder;
import ij.gui.Toolbar;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.indexer.FloatIndexer;

import java.util.List;
import java.awt.Polygon;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class AdipoCorr_ implements PlugInFilter {
	//private static final int CV_32F = 0;
	protected ImagePlus image;

	//@Override
	public void run(ImageProcessor ip) {
		// TODO Auto-generated method stub
		
		//PARAMETERS		
		GUI_parameters parameters = new GUI_parameters();
		parameters.run();
		int[] diameters = parameters.get_diameters();
		int[] diameters_scaled = parameters.get_diameters_scaled();
		double strip = parameters.get_strip();
		int scaling = parameters.get_scaling();
		int dia_step = parameters.get_step();
		
		//IMAGE SETUP
		ImagePlus imp = new ImagePlus("image",ip.convertToFloat());
		ip = imp.getProcessor();
		ip.invert();
		
		//BACKGROUND ESTIMATION		
		int[] hist = ip.getHistogram(1000);
		for(int i:hist) {
			System.out.println(i);
		}
		System.out.println(ip.getHistogramMax());
		System.out.println(ip.getHistogramMin());
		
		
		//IMAGE SCALING
		ImagePlus imp_scaled = new ImagePlus("image scaled",ip.resize(imp.getWidth()*scaling,imp.getHeight()*scaling,true));
		
		//MULTISCALE MODEL MATCH WITH PROJECTION
		CodedImage codedProj = projectedMultiScaleModelMatching(imp_scaled, diameters_scaled ,strip );
		//ImagePlus stack = multiScaleModelMatching(imp_scaled, diameters_scaled, strip );
		//stack.show();
		ImagePlus proj = codedProj.getImage();
		ImagePlus diaImage = codedProj.getCode();
		proj.show();
		diaImage.show();
		
		//FIND ADIPOCYTES
		
		
		//LIVE PARAMETER SELECTION
		GUI_liveMaxSelection liveAdiSelection = new GUI_liveMaxSelection(image,proj,diaImage,scaling);
		
		//MANUAL ADICPOYTES EDIT
		if(liveAdiSelection.wasOKed()){
			List<Adipocyte> adipocytes = liveAdiSelection.getAdipocytes();
			AdipoTool aditool = new AdipoTool(adipocytes,image,dia_step,diameters[0]);
			Toolbar.addPlugInTool(aditool);
			Toolbar toolbar = Toolbar.getInstance();
			int id = toolbar.getToolId("fr.inserm.i2mc.adipocorr .AdipoTool");
			toolbar.setTool(id);
			
			//RoiManager rm = RoiManager.getRoiManager();
			//rm.reset();
			//for(Adipocyte adipocyte : adipocytes) {
			//	adipocyte.addToRoiManager(rm);
			//}
			//rm.runCommand(image,"Show All with labels");
			//IJ.run("Set Measurements...", "bounding redirect=None decimal=3");
			//IJ.setTool("oval");
		}
	}

	//@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		image = imp;
		return DOES_ALL;
	}
	
	
	static public void falsePositiveCheck(List<Adipocyte> adipocytes, ImagePlus image) {
		int max_bin_count = 16;
		int min_bin_size = 10;
		double strip = 0.25;
		
		ImageProcessor ip = image.getProcessor();
		float[][] pixels = ip.getFloatArray();
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		HashMap<Integer, int[][]> bin_maps = new HashMap<Integer, int[][]>();
		HashMap<Integer, Integer> bin_counts = new HashMap<Integer, Integer>();
		
		for(Adipocyte adipocyte : adipocytes){
			int diameter = adipocyte.diameter();
			//create a bin map of not done previously
			if(!bin_counts.containsKey(diameter)) {
				//bin count is either 16 or corresponding to a minimum of 5 pixel by bin
				double area = diameter*diameter*Math.PI/4;
				if((area/max_bin_count)<min_bin_size) {
					bin_counts.put(diameter, (int)(area/min_bin_size));
				}
				else {
					bin_counts.put(diameter, max_bin_count);
				}
				double bin_width = 2*Math.PI/bin_counts.get(diameter);
				//binned angles image : range is -pi / pi
				bin_maps.put(diameter, adipocytes_angular_bins_map(diameter, bin_width));
			}
			//binning
			int[][] bin_map = bin_maps.get(diameter);
			int bin_count = bin_counts.get(diameter)+1;
			int x=adipocyte.x()-diameter/2;
			int y=adipocyte.y()-diameter/2;
			System.out.println(x);
			System.out.println(y);
			if((x>=0)&&((x+diameter)<width)&&(y>=0)&&((y+diameter)<height)) {
				float[] means = new float[bin_count];
				Arrays.fill(means, 0);
				int[] pix_count = new int[bin_count];
				for(int i=0; i<diameter;i++) {
					for(int j=0; j<diameter;j++) {
						means[bin_map[i][j]] = means[bin_map[i][j]] + pixels[i+x][j+y];
						pix_count[bin_map[i][j]] = pix_count[bin_map[i][j]]+1;
					}
				}
				for(int i=0; i<(bin_count);i++) {
					means[i] = means[i]/pix_count[i];
					System.out.println(means[i]);
				}
				//homogeneity factor
				
				
			}
			else {
				System.out.println("excluded");
			}
			
		}
		
		//display
//		FloatProcessor fp = new FloatProcessor(diameter,diameter);
//		float[] pix = (float[])fp.getPixels();
//		int p=0;
//		for(int i=0; i<diameter;i++) {
//			for(int j=0; j<diameter;j++) {
//				pix[p]=(float)bin_map[i][j];
//				p++;
//			}
//		}
//		ImagePlus imp = new ImagePlus("image",fp);
//		imp.show();
	}
	
	static public int[][] adipocytes_angular_bins_map(int diameter, double bin_width){
		int center = diameter/2;
		int cen2 = center*center;
		int[][] bin_map = new int[diameter][diameter];
		for(int i=0; i<diameter;i++) {
			for(int j=0; j<diameter;j++) {
				int x = i-center; int y = j-center;
				if((x*x+y*y)<cen2) { //mask
					bin_map[i][j] = (int)((Math.atan2(x,y)+Math.PI-0.0001)/bin_width)+1;
				}
			}
		}
		return bin_map;
	}
	
	static public ImagePlus multiScaleModelMatching(ImagePlus image, int[] diameters, double strip ) {
		Mat imageMat = ImpToMatfConverter(image);
		ImageStack stack = new ImageStack(image.getWidth(),image.getHeight());
		for(int diameter : diameters){
			Mat model = createModel(diameter,strip);
			Mat output = matchModel(imageMat,model);
			ImagePlus imp = MatToImpfConverter(output);
			stack.addSlice(imp.getProcessor());
		}
		return new ImagePlus("result",stack);
	}
	
	static public CodedImage projectedMultiScaleModelMatching(ImagePlus image, int[] diameters, double strip ) {
		Mat imageMat = ImpToMatfConverter(image);
		FloatProcessor fp = new FloatProcessor(image.getWidth(),image.getHeight());
		ShortProcessor pos = new ShortProcessor(image.getWidth(),image.getHeight());
		float[] fpixels = (float[])fp.getPixels();
		int len = fpixels.length;
		for (int i=0; i<len; i++)
			fpixels[i] = -Float.MAX_VALUE;
		short[] pdiameter = (short[])pos.getPixels();
		for(int diameter : diameters){
			Mat model = createModel(diameter,strip);
			ImagePlus imp = MatToImpfConverter(model);
			imp.setTitle(String.valueOf(diameter));
			imp.show();
			Mat output = matchModel(imageMat,model);
			float[] pixels = getfPixels(output);
			for(int i=0; i<len; i++) {
				if(pixels[i]>fpixels[i]){
		    		fpixels[i] = pixels[i];
		    		pdiameter[i] = (short)diameter;
		    	}
	    	}
		}
		return new CodedImage(new ImagePlus("projection", fp),new ImagePlus("position",pos));
	}
	
	static public Mat matchModel(Mat image,Mat model) {
		Mat output = new  Mat(image.cols(),image.rows(),opencv_core.CV_32F,Scalar.all(0));
		Mat input = new Mat(image.cols()+model.cols()-1,image.rows()+model.rows()-1,opencv_core.CV_32F,Scalar.all(0));
		int row_border_1 = model.rows()/2;
		int row_border_2 = model.rows()-row_border_1-1;
		int col_border_1 = model.cols()/2;
		int col_border_2 = model.cols()-col_border_1-1;
		opencv_core.copyMakeBorder(image, input, row_border_1, row_border_2, col_border_1, col_border_2, opencv_core.BORDER_CONSTANT, Scalar.all(0));
		opencv_imgproc.matchTemplate(input, model, output, opencv_imgproc.TM_CCORR_NORMED);
		return output;
	}
	
	static public Mat createModel(int diameter, double strip) {
		Mat model = new  Mat(diameter,diameter,opencv_core.CV_32F,Scalar.all(0));
		FloatIndexer indexer = model.createIndexer();
		//i = (distance au centre)2 = (x-rad)2+(y-rad)2 tel que i(x-rad = diameter-strip*2
		double in_rad = diameter/2*(1-strip);
		float norm = (float)(Math.pow(in_rad,2)+Math.pow(in_rad,2))/2;
		float value = 0;
		for(int y = 0;y<model.rows();y++) {
			for(int x = 0;x<model.cols();x++) {
				double dist = Math.pow(Math.pow(x-diameter/2,2)+Math.pow(y-diameter/2,2),0.5);	
				if(dist>diameter/2) {value = 0;}
				else if(dist>in_rad) {value = 1;}
				else {value = (float)Math.pow(dist, 2)/norm;}
				indexer.put(y, x, value);
			}
		}
		return model;
	}
	
	
	static public List<Adipocyte> findAdipocytes(ImageProcessor projection,ImageProcessor diaImage,double tolerance,double threshold,int scaling){
		MaximumFinder mf = new MaximumFinder();
		Polygon polygon = mf.getMaxima(projection,tolerance, false);
		int[] X=polygon.xpoints;
		int[] Y=polygon.ypoints;
		int nb = X.length;
		List<Adipocyte> res = new ArrayList<Adipocyte>();
		for(int i=0;i<nb;i++){
			float val = projection.getf(X[i],Y[i]);
			if(val>=threshold){
				int diameter = diaImage.get(X[i],Y[i]);
				res.add(new Adipocyte(X[i]/scaling,Y[i]/scaling,diameter/scaling));}
		}
		return res;
	}
	
	static public float[] getfPixels(Mat m) {
		FloatBuffer floatBuffer = m.createBuffer();
		float[] pix = new float[floatBuffer.capacity()];
		floatBuffer.get(pix);
		return pix;
	}
	
	static public Mat ImpToMatfConverter(ImagePlus imp) {
		Mat m = new  Mat(imp.getHeight(),imp.getWidth(),opencv_core.CV_32F,Scalar.all(0));
		ImageProcessor ip = imp.getProcessor();
		float[] pix = (float[])ip.getPixels();
		FloatBuffer floatBuffer = m.createBuffer();
		floatBuffer.put(pix);
		return m;
	}
	
	static public ImagePlus MatToImpfConverter(Mat m) {
		FloatProcessor fp = new FloatProcessor(m.cols(),m.rows());
		float[] pix = (float[])fp.getPixels();
		FloatBuffer floatBuffer = m.createBuffer();
		floatBuffer.get(pix);
		return new ImagePlus("image",fp);
	}
	
	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = AdipoCorr_.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("C:\\Users\\remy.flores\\Documents\\03-Projets\\AI-AdipoCorr\\poster\\fig multiscale\\Proto DS EP4 fem 21éme 5 µl 4xa.tif");
		image.show();
		
		/*
		//IMAGEPLUS MAT CONVERSION
		ImagePlusMatConverter ic = new ImagePlusMatConverter();
		MatImagePlusConverter mip = new MatImagePlusConverter();
		Mat m = ic.convert(image,Mat.class);
		
		//MATCH MODEL
		Mat model = createModel(12,strip);
		ImagePlus modelimp = mip.convert(model,ImagePlus.class);
		modelimp.show();
		Mat res = matchModel(m,model);
		ImagePlus resimp = mip.convert(res,ImagePlus.class);
		resimp.show();
		
		//MULTISCALE MODEL MATCHING
		ImagePlus res = multiScaleModelMatching(imp_scaled, diameters ,strip );
		res.show();
		
		List<Adipocyte> adipocytes = findAdipocytes(proj.getProcessor(),diaImage.getProcessor(),tolerance,threshold,scaling);
		
		//LOG POLAR TRANSFORM
		Mat r = new  Mat(m.size(),m.type());
		double M = (double)(m.cols()/6.7);
		opencv_imgproc.logPolar(m, r, new opencv_core.Point2f((float)(m.cols()*0.5), (float)(m.rows()*0.5)), M, opencv_imgproc.CV_INTER_LINEAR+opencv_imgproc.CV_WARP_FILL_OUTLIERS);
		ImagePlus plop = mip.convert(r,ImagePlus.class);
		plop.show();
		*/
		
		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
