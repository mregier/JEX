package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import utilities.FunctionUtility;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.dimension.DimensionMap;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.ExperimentalDataCrunch;
import function.tracker.SimpleConvolve;
import guiObject.FormLine;


/**
 * This is a JEXperiment function template
 * To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions
 * 2. Place the file in the Functions/SingleDataPointFunctions folder
 * 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types
 * The specific API for these can be found in the main JEXperiment folder.
 * These API provide methods to retrieve data from these objects,
 * create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 *
 */
public class JEX_Convolve extends ExperimentalDataCrunch{

	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------
	
	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	@Override
	public String getName() {
		String result = "Convolve image";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does
	 * This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	@Override
	public String getInfo() {
		String result = "Algorithm performing a convolution of one image on another. Used mainly to filter shapes and transform a phase contrast image into a intensity value image.";
		return result;
	}

	/**
	 * This method defines in which group of function this function 
	 * will be shown in... 
	 * Toolboxes (choose one, caps matter):
	 * Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools
	 * Stack processing, Data Importing, Custom image analysis, Matlab/Octave
	 * 
	 */
	@Override
	public String getToolbox() {
		String toolbox = "Image processing";
		return toolbox;
	}

	/**
	 * This method defines if the function appears in the list in JEX
	 * It should be set to true expect if you have good reason for it
	 * 
	 * @return true if function shows in JEX
	 */
	@Override
	public boolean showInList() {
		return true;
	}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * @return
	 */
	@Override
	public boolean allowMultithreading()
	{
		return false;
	}
	
	
	
	// ----------------------------------------------------
	// --------- INPUT OUTPUT DEFINITIONS -----------------
	// ----------------------------------------------------

	
	/**
	 * Return the array of input names
	 * 
	 * @return array of input names
	 */
	@Override
	public TypeName[] getInputNames(){
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(IMAGE,"Base image");
		inputNames[1] = new TypeName(IMAGE,"Small convolution image");
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	@Override
	public TypeName[] getOutputs() {
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(IMAGE,"Convolved");

		if (outputNames == null) return defaultOutputNames;
		return outputNames;
	}
	
	/**
	 * Returns a list of parameters necessary for this function 
	 * to run...
	 * Every parameter is defined as a line in a form that provides 
	 * the ability to set how it will be displayed to the user and 
	 * what options are available to choose from
	 * The simplest FormLine can be written as:
	 * FormLine p = new FormLine(parameterName);
	 * This will provide a text field for the user to input the value
	 * of the parameter named parameterName
	 * More complex displaying options can be set by consulting the 
	 * FormLine API
	 * 
	 * @return list of FormLine to create a parameter panel
	 */
	@Override
	public ParameterSet requiredParameters() {
		Parameter p1 = new Parameter("Normalize","Normalize the image",FormLine.DROPDOWN,new String[] {"true","false"},1);
		Parameter p2 = new Parameter("Output Bit Depth","Depth of the outputted image",FormLine.DROPDOWN,new String[] {"8","16","32"},1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		return parameterArray;
	}
	
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking
	 * It is HIGHLY recommended to implement input checking
	 * however this can be over-rided by returning false
	 * If over-ridden ANY batch function using this function 
	 * will not be able perform error checking... 
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled(){
		return true;
	}
	
	
	
	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------

	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs){
		// Get the base images
		JEXData data1 = inputs.get("Base image");
		if (!data1.getTypeName().getType().equals(JEXData.IMAGE)) return false;

		JEXData data2 = inputs.get("Small convolution image");
		if (!data2.getTypeName().getType().equals(JEXData.IMAGE)) return false;
		ImagePlus conv = ImageReader.readObjectToImagePlus(data2);
		
		////// Get params
		String norm  = parameters.getValueOfParameter("Normalize");
		int depth    = Integer.parseInt(parameters.getValueOfParameter("Output Bit Depth"));
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(data1);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		int count                   = 0;
		int total                   = images.size();
		SimpleConvolve convolver    = new SimpleConvolve();
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap dim:images.keySet()){
			String path = images.get(dim);
//			File   f    = new File(path);
			
			// get the image
			ImagePlus im = new ImagePlus(path);
			
			////// Begin Actual Function
			ImagePlus result = convolver.start(im, conv, null);
			FloatProcessor fpr = (FloatProcessor) result.getProcessor().convertToFloat();
			////// End Actual Function
			
			////// Save the results
//			String localDir    = JEXWriter.getEntryFolder(entry);
//			String newFileName = FunctionUtility.getNextName(localDir, f.getName(), "A");
//			String finalPath   = localDir + File.separator + newFileName;
//			FunctionUtility.imSave(fpr, norm, depth, finalPath);
			ImagePlus toSave = FunctionUtility.makeImageToSave(fpr, norm, depth);
			String finalPath = JEXWriter.saveImage(toSave);

			outputMap.put(dim.copy(), finalPath);
			JEXStatics.logManager.log("Finished processing " + count + " of " + total + ".",1,this);
			count++;
			
			// Status bar
			int percentage = (int) (100 * ((double) count/ (double)images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Set the outputs
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputMap);
		output1.setDataObjectInfo("Stack convolved using stack convolution function");
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
}
