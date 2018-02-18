import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 *
 * Steganograph
 *
 * This is a plugin that will encode text into the source image
 *
 */

public class Steganograph implements PlugInFilter {
	ImagePlus imp;
	ImageProcessor ip;

	/**
	 * 	A class to organize configuration options for the Steganograph plugin.
	 * 	The configuration is for:
	 * 		- text encoding
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
	public class Options extends Object {
		private Charset charset;

		// public final String[] 	CHARSETSTRS = 	{	"UTF-16", 	"UTF-8", 	"7-bit ASCII"	};
		// public static final enum CHARSETS 		{	 UTF_16 , 	 UTF_8 , 	 ASCII_7_BIT 	};

		public Options(){
			this(StandardCharsets.UTF_16);	// UTF-16 is java default
		}

		public Options(String str){
			this();
			setCharsetFromString(str);
			
		}

		public Options(Charset charset){
			this.charset = charset;
		}

		public Charset getCharset(){
			return charset;
		}
		public void setCharset(Charset charset){
			this.charset = charset;
		}
		public void setCharsetFromString(String str){
			for(int i = 0; i<SupportedCharsets.CHARSETSTRS.length; i++){

				if( str.equals(SupportedCharsets.CHARSETSTRS[i]) ){
					setCharset(SupportedCharsets.STD_CHARSETS[i]);
					break;
				}
			}
		}

		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append("ENCODING:\t");
			sb.append(  SupportedCharsets.toString( getCharset() )  ); 
			// switch(getCharset()){
			// 	case StandardCharsets.UTF_16: 	sb.append("UTF-16"); 		break;
			// 	case StandardCharsets.UTF_8: 	sb.append("UTF-8"); 		break;
			// 	case StandardCharsets.US_ASCII:	sb.append("7-bit ASCII"); 	break;
			// }
			
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
			//showAbout();
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
		
		gd.showDialog();

		if(gd.wasCanceled()){
			options = new Options();
		}else{
			String choice = gd.getNextRadioButton();
			options = new Options(choice);
		}

		return options;
	}




	public boolean getTextFile(){
		return true;
	}

	/** 
	 * 
	 */
	public void run(ImageProcessor ip){
		this.ip = ip;

		//Get user input to configure the operation, most notably the charset
		Options opts = config();

		GenericDialog gd = new GenericDialog("Results");

		gd.addMessage(opts.toString());
		gd.showDialog();
		// Open the text file
		// if(getTextFile()){
		// 	// Got tha file
		// }
		

		// do the encoding, writing to a new image

		// Choose save location


	}

}