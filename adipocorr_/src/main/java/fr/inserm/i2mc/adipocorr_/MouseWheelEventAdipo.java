package fr.inserm.i2mc.adipocorr_;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class MouseWheelEventAdipo implements MouseWheelListener {

	AdipoTool aditool;
	public MouseWheelEventAdipo(AdipoTool aditool) {
		this.aditool = aditool;
	}
	
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		aditool.diameterChanged(notches);
	}

}
