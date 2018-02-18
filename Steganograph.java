import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*; // HTMLDialog
import java.awt.*;
import java.io.*; // File, FileInputStream
import ij.plugin.filter.*;
import java.nio.*;
import java.nio.charset.*;
//import java.nio.charset.StandardCharsets;


/**
 *
 * Steganograph
 *
 * This is a plugin that will encode text into a source image or decode 
 * text given an encoded image and the source image.
 *
 */

public class Steganograph implements PlugInFilter {
	ImagePlus imp;
	ImageProcessor ip;

	/* Title for the about dialog */
	private String sTitle 		= "Steganograph";

	/* Description for the about dialog */
	private String sDescription = "This is an ImageJ Plugin that will encode text into an image.";

	/* Enum for flagging operation type (encode or decode) */
	private enum OpType{ENCODE, DECODE};

	/**
	 * This class organizes the supported charsets for this application
	 */
	private static class SupportedCharsets {
		public static final String[] 	CHARSETSTRS = 	{	"UTF-16", 	"UTF-8", 	"7-bit ASCII"	};
		public enum 					CHARSETS 		{	 UTF_16 , 	 UTF_8 , 	 ASCII_7_BIT 	};
		public static final Charset[]  STD_CHARSETS = 	{ 	StandardCharsets.UTF_16, StandardCharsets.UTF_8, StandardCharsets.US_ASCII};

		public static String toString(Charset cset){
			String retval = " ";
			for(int i = 0; i<STD_CHARSETS.length; i++){

				if(cset.equals(STD_CHARSETS[i]))
					retval = CHARSETSTRS[i];
			}
			return retval;
		}
	}

	/**
	 * 	A class to organize configuration options for the Steganograph plugin.
	 * 	This class specifies configuration for:
	 * 		- text encoding (default: UTF-16)
	 * 		- operation type, encode or decode (default: encode)
	 */
	private class Options extends Object {
		/* The chosen character set */
		private Charset charset;
		
		/* enum to set this as an encode or decode operation */
		private OpType optype;

		public Options(){
			this(StandardCharsets.UTF_16, OpType.ENCODE);	// UTF-16 is java default
		}

		public Options(String str){
			this(str, OpType.ENCODE);
		}

		public Options(String encstr, String opstr){
			this();
			setCharsetFromString(encstr);
			setOpTypeFromString(opstr);
		}

		public Options(String str, OpType optype){
			this(optype);
			setCharsetFromString(str);
		}

		public Options(Charset charset){
			this(charset, OpType.ENCODE);
		}
		public Options(OpType optype){
			this(StandardCharsets.UTF_16, optype);
		}

		public Options(Charset charset, OpType optype){
			this.charset = charset;
			this.optype  = optype;
		}

		public Charset getCharset(){
			return charset;
		}
		public OpType getOpType(){
			return optype;
		}
		public void setCharset(Charset charset){
			this.charset = charset;
		}
		public void setOpType(OpType optype){
			this.optype = optype;
		}
		public void setCharsetFromString(String str){
			for(int i = 0; i<SupportedCharsets.CHARSETSTRS.length; i++){

				if( str.equals(SupportedCharsets.CHARSETSTRS[i]) ){
					setCharset(SupportedCharsets.STD_CHARSETS[i]);
					break;
				}
			}
		}
		public void setOpTypeFromString(String str){
			if( str.equalsIgnoreCase("ENCODE") ){
				this.optype = OpType.ENCODE;
			}else{
				this.optype = OpType.DECODE;
			}
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append("ENCODING: ");
			sb.append(  SupportedCharsets.toString( getCharset() )  ); 
			sb.append("\n");
			sb.append("OPERATION: ");
			sb.append( getOpType().equals(OpType.ENCODE) ? "Encode" : "Decode" );
			return sb.toString();
		}
	}

	/** Method to return types supported
     * @param arg 
     * @param imp The ImagePlus, used to get the spatial calibration
     * @return Code describing supported formats etc.
     * (see ij.plugin.filter.PlugInFilter & ExtendedPlugInFilter)
     */
	public int setup(String arg, ImagePlus imp) {
		if(arg.equalsIgnoreCase("about")){
			showAbout();
			return DONE;
		}
		this.imp = imp;
		return DOES_ALL+NO_CHANGES;
	}

	public Options config(){
		Options options;
		GenericDialog gd = new GenericDialog("Configure Encoding");
		gd.addMessage("Choose a text encoding.");
		
		String[] items = SupportedCharsets.CHARSETSTRS; //{"UTF-16", "UTF-8", "7-bit ASCII"};

		/*addRadioButtonGroup(	String label, 
								String[] items, 
								int rows, 
								int columns,
								String defaultItem) */
		gd.addRadioButtonGroup(	"This is a group label", 
								items,
								items.length,
								1,
								items[0] );
		String[] items2 = {"Encode", "Decode"};
		gd.addRadioButtonGroup( "This is a second group label",
								items2,
								2,
								1,
								items2[0] );
		gd.showDialog();

		if(gd.wasCanceled()){
			options = new Options();
		}else{
			// Vector<String> v = gd.getRadioButtonGroups();
			String encoding  = gd.getNextRadioButton();
			String operation = gd.getNextRadioButton();
			options = new Options(encoding, operation);

		}

		return options;
	}


	public File openTextFile(String dialogTitle){
		OpenDialog od = new OpenDialog(dialogTitle);

		if(od.getPath() != null)
			return new File( od.getPath() );
		else
			return null;
	}

	/**
	 * 
	 */
	public boolean addDataToImg(ImageProcessor ip, FileInputStream fis) throws IOException{

		if(ip.getNChannels() != 3){
			return false;
		}

		int[] intPixels = (int[])ip.getPixels();


		// read thru the file byte-by-byte and do the operation to
		// add the data into the image
		int pixSize  = 4;   // bytes per pixel
		int data    = 0;
		int tmp     = 0;
		int rawIndex = 0; 	// index of the bytes of the pixels
		int pixIndex = 0; 	// selects the pixel
		int pixelVal = 0; 	// int value of the pixel
		int dataInt  = 0;   //
		int capacity = intPixels.length * (8/pixSize);
		for(int i = 0; fis.available()>0 && i<capacity; i++){

			data = fis.read();

			// iterate over 1 byte
			for(byte b = 0; b<8; b++){
				rawIndex = 8*i + b;
				tmp = ( data>>(7-b) ) & 1; 	// access the desired bit, MSB first
				dataInt = (dataInt << 8) + tmp; // shift bits over and add our new bit

				if(rawIndex%pixSize == 0){ 		// start of a new pixel, get new pixel data
					pixIndex = rawIndex/pixSize;
					pixelVal = intPixels[pixIndex];
				}
				if((rawIndex+1)%pixSize == 0 ){ // we've reached the last byte in this pixel
					// do ops to add the dataInt it
					// the LSB in each byte of dataInt is the data, rest we know are 0's
					dataInt = pixelVal ^ dataInt; // XOR, flips the LSB if 1, leaves it same if not
					intPixels[pixIndex] = dataInt;
				}
			}
		}
		return true;
	}

	/** 
	 * 
	 */
	public void run(ImageProcessor ip){
		this.ip = ip;

		//Get user input to configure the operation
		Options opts = config();

		//opts.setNChannels(ip.getNChannels());

		GenericDialog gd 		= new GenericDialog("Results");
		GenericDialog summary 	= new GenericDialog("Exit Summary");

		gd.addMessage(opts.toString());
		summary.addMessage(opts.toString());
		//gd.showDialog();


		// ============ ENCODING OPERATION ============
		if(opts.getOpType().equals(OpType.ENCODE)){
			// get info from the image
			int width 		= ip.getWidth();
			int height 		= ip.getHeight();
			int bitDepth 	= ip.getBitDepth(); 	// 8, 16, 24, 32
			int pixSize 	= bitDepth/8; 			// 1, 2,  3,  4
			int nPixels  	= width * height;
			int capacity 	= (nPixels*pixSize)/8;
			
			ImageProcessor newIP; 		// this will hold the duplicate of the image

			StringBuilder imgInfo = new StringBuilder("=== IMAGE INFO ===\n");
			imgInfo.append("Pixels: ");
			imgInfo.append(nPixels);
			imgInfo.append("\n");
			imgInfo.append("Max Capacity (bytes): ");
			imgInfo.append(capacity);
			imgInfo.append("\n");


			summary.addMessage(imgInfo.toString());

			// open plain text file
			File plaintxt;
			FileInputStream fis;

			try{
				plaintxt 	= openTextFile("Select Plain-Text...");
				fis 		= null;

				if(plaintxt != null){

					fis = new FileInputStream(plaintxt); 	// init a file input stream
					summary.addMessage("Path to Plaintext File: "+ plaintxt.getPath());
				
					// copy the image - this is the one that we put the data in
					newIP = ip.duplicate();

					boolean success = addDataToImg(newIP, fis);
					// close input stream
					fis.close();

					summary.addMessage( success ? "Data Addition Succeeded" : "Data Addition Failed");

					ImagePlus newImp = new ImagePlus("Result", newIP);
					newImp.show();

				}else{
					summary.addMessage("No file path provided.");
				}
			}
			catch(FileNotFoundException e){
				summary.addMessage("Error: File Not Found :(");
			}
			catch(IOException e){
				summary.addMessage("Error: IOException");
			}finally{
				
			}

			

			// modify data

			// choose save location for new image

			// write new image

			




		}



		// ============ DECODING OPERATION ============
		else{
			// open original image (source image)

			// extract plaintext data

			// select save location for the new plaintext file 

			// write to the file

		}
		

		// Exit operation
		summary.showDialog();

	}


	void showAbout(){
		IJ.showMessage(sTitle, sDescription);
	}

}