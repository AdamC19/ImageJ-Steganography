import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*; // HTMLDialog
import java.awt.*;
import java.io.*; // File, FileInputStream
import ij.plugin.filter.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


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
	public void run(ImageProcessor ip){
		this.ip = ip;

		//Get user input to configure the operation, most notably the charset
		Options opts = config();

		GenericDialog gd 		= new GenericDialog("Results");
		GenericDialog summary 	= new GenericDialog("Exit Summary");

		gd.addMessage(opts.toString());
		summary.addMessage(opts.toString());
		//gd.showDialog();


		// ============ ENCODING OPERATION ============
		if(opts.getOpType().equals(OpType.ENCODE)){
			// get info from the image

			// open plain text file
			File plaintxt;
			FileInputStream fis;

			try{
				plaintxt 	= openTextFile("Select Plain-Text...");
				fis 		= null;

				if(plaintxt != null){

					fis = new FileInputStream(plaintxt); 	// init a file input stream
					summary.addMessage("Path to Plaintext File: "+ plaintxt.getPath());
				
					// read in data


					// close input stream
					fis.close();

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
			// read in image from ip

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