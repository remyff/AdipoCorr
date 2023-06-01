package fr.inserm.i2mc.adipocorr_;

import ij.ImagePlus;

public class CodedImage{
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