import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;


/**
 *
 * Steganograph
 *
 * This is a plugin that will encode text into the source image
 *
 */

public class Steganograph implements PlugInFilter {
	ImageProcessor ip;

	/** Method to return types supported
     * @param arg 
     * @param imp The ImagePlus, used to get the spatial calibration
     * @return Code describing supported formats etc.
     * (see ij.plugin.filter.PlugInFilter & ExtendedPlugInFilter)
     */
	public int setup(String arg, ImagePlus imp) {
	}

	/** 
	 * 
	 */
	public void run(ImageProcessor ip){
		this.ip = ip;
	}

}