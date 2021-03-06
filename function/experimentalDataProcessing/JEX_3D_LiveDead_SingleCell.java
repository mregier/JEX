package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import image.roi.PointList;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;

import jex.statics.JEXStatics;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.ExperimentalDataCrunch;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
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
/**
 * @author edmyoung
 *
 */
public class JEX_3D_LiveDead_SingleCell extends ExperimentalDataCrunch{

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
		String result = "3D live-dead cell location for fibroblasts";
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
		String result = "Find locations of singular live and dead cells in a live/dead stain";
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
		String toolbox = "Custom Cell Analysis";
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
		TypeName[] inputNames = new TypeName[3];
		
		inputNames[0] = new TypeName(IMAGE,"Live Image");
		inputNames[1] = new TypeName(IMAGE,"Dead Image");
		inputNames[2] = new TypeName(ROI,"Optional ROI");
		
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	public TypeName[] getOutputs() {
		defaultOutputNames = new TypeName[3];
		defaultOutputNames[0] = new TypeName(VALUE,"Number Live");
		defaultOutputNames[1] = new TypeName(VALUE,"Number Dead");
		defaultOutputNames[2] = new TypeName(VALUE,"More Info");

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
	public ParameterSet requiredParameters() {
		Parameter p0 = new Parameter("Automatic","Enable visual interface",FormLine.DROPDOWN,new String[] {"true","false"},1);
		
		Parameter p1 = new Parameter("Binning","Binning factor for quicker analysis","1.0");
		Parameter p3 = new Parameter("RollingBall","Rolling ball radius for removing background","50.0");
		Parameter p6 = new Parameter("Live tolerance","Threshold for identifying live cell locations","8.0");
		Parameter p7 = new Parameter("Dead tolerance","Threshold for identifying dead cell locations","8.0");
		
		Parameter p2 = new Parameter("Threshold","Threshold for identifying neutrophil locations","40.0");
		Parameter p4 = new Parameter("Live radius","Radius of live cell in pixels (e.g. 3 to 30)","0");
		Parameter p5 = new Parameter("Dead radius","Radius of dead cell in pixels (e.g. 3 to 30)","0");
		
		Parameter p8 = new Parameter("Double counting","Distance of particles to prevent double counting","20");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
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
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs){
		// Collect the inputs
		JEXData data1 = inputs.get("Live Image");
		if (!data1.getTypeName().getType().equals(JEXData.IMAGE)) return false;
		
		JEXData data2 = inputs.get("Dead Image");
		if (!data2.getTypeName().getType().equals(JEXData.IMAGE)) return false;
		
		JEXData data3 = inputs.get("Optional ROI");
		
		// Run the function
		LiveDeadCellFinder graphFunc = new LiveDeadCellFinder(entry,data1,data2,data3,outputNames,parameters);
		graphFunc.doit();
		JEXData output1 = graphFunc.day1_liveNumber;
		JEXData output2 = graphFunc.day1_deadNumber;
		JEXData output3 = graphFunc.moreInfo;

		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);

		// Return status
		return true;
	}
}



class LiveDeadCellFinder implements GraphicalCrunchingEnabling, ImagePanelInteractor{
	
	// Utilities
	ImagePanel imagepanel ;
	GraphicalFunctionWrap wrap ;
	int index    = 0      ;
	int atStep   = 0      ;
	int frame    = 0      ;
	
	// Roi interaction
	boolean interactionMode = false;
	Point first = null;
	Point second = null;
	
	// Outputs
	public JEXData day1_liveNumber;
	public JEXData day1_deadNumber;
	public JEXData moreInfo ;
	
	// Parameters
	ParameterSet params  ;
	boolean auto             = false ;
	int     bin              = 2;
	int     rollingBall      = 30 ;
	int     liveThresh       = 40 ;
	int     deadThresh       = 40 ;
	int     doubleRadius     = 20 ;
	
	double  radius1          = 15.0;
	double  radius2          = 20.0;
	boolean createBackground = false; 
	boolean lightBackground  = false; 
	boolean useParaboloid    = false; 
	boolean doPresmooth      = false;
	boolean correctCorners   = false;
	
	// Variables used during the function steps
	private ByteProcessor imp ;
	private ImagePlus     im  ;
	private ImagePlus     live ;
	private ImagePlus     dead ;
	private ImagePlus     livePreprocessed ;
	private ImagePlus     deadPreprocessed ;
	
	// Input
	JEXData    day1_liveImset      ;
	JEXData    day1_deadImset      ;
	JEXEntry   entry          ;
	TypeName[] outputNames    ;
	JEXData    rectangle      ;
	
	PointList      day1_livePoints  ;
	PointList      day1_deadPoints  ;
	
	LiveDeadCellFinder(
			JEXEntry entry, 
			JEXData day1_liveImset, 
			JEXData day1_deadImset, 
			JEXData rectangle, 
			TypeName[] outputNames, 
			ParameterSet parameters)
			{
		 
		// Pass the variables
		this.day1_liveImset   = day1_liveImset;
		this.day1_deadImset   = day1_deadImset;
		this.rectangle   = rectangle;
		this.params      = parameters;
		this.entry       = entry;
		this.outputNames = outputNames;
		
		////// Get params
		getParams();
		
		// Prepare images
		live = ImageReader.readObjectToImagePlus(day1_liveImset);
		dead = ImageReader.readObjectToImagePlus(day1_deadImset);
		
		// Prepare the graphics
		imagepanel = new ImagePanel(this,"Locate live dead cells and determine proliferation");
		
		displayImage(index);
		wrap = new GraphicalFunctionWrap(this,params);
		wrap.addStep(0, "LIVE - Preprocess image", new String[] {"Binning","RollingBall","Live tolerance"});
		wrap.addStep(1, "LIVE - locate cells", new String[] {"Live radius"});
		wrap.addStep(2, "DEAD - Preprocess image", new String[] {"Binning","RollingBall","Dead tolerance"});
		wrap.addStep(3, "DEAD - locate cells", new String[] {"Dead radius"});
		wrap.addStep(4, "Analyze", new String[] {"Double counting"} );
		
		wrap.setInCentralPanel(imagepanel);
		wrap.setDisplayLoopPanel(true);
	}
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParams(){
		////// Get params
		auto        = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
		bin         = (int) Double.parseDouble(params.getValueOfParameter("Binning"));
		rollingBall = (int) Double.parseDouble(params.getValueOfParameter("RollingBall"));
		liveThresh  = (int) Double.parseDouble(params.getValueOfParameter("Live tolerance"));
		deadThresh  = (int) Double.parseDouble(params.getValueOfParameter("Dead tolerance"));
		doubleRadius = (int) Double.parseDouble(params.getValueOfParameter("Double counting"));
		
		radius1   = (int) Double.parseDouble(params.getValueOfParameter("Live radius"));
		radius2   = (int) Double.parseDouble(params.getValueOfParameter("Dead radius"));
	}

	private void displayImage(int index){
		imagepanel.setImage(im);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * @return the ROI data
	 */
	public void doit(){
		////// Get params
		getParams();
		
		if (auto){
			binImage();
			// TODO
		}
		else {
			wrap.start();
		}
		return;
	}

	public void runStep(int step) {
		atStep = step;
		
		////// Get params
		getParams();
		
		///// Run step index
		JEXStatics.logManager.log("Running step "+atStep,1,this);
		imagepanel.setPointListArray(null,null);
		imagepanel.setRoi(null);
		imagepanel.setTracks(null);
		
		if (atStep == 0){
			preProcessLive();
			imagepanel.setImage(livePreprocessed);
			interactionMode = true;
			if (auto) {
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if (atStep == 1){
			locateLive();
			imagepanel.setPointList(day1_livePoints);
			imagepanel.setImage(live);
			interactionMode = true;
			
			if (auto) {
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if (atStep == 2){
			preProcessDead();
			imagepanel.setImage(deadPreprocessed);
			interactionMode = true;
			
			if (auto) {
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if (atStep == 3){
			locateDead();
			imagepanel.setPointList(day1_deadPoints);
			imagepanel.setImage(dead);
			
			interactionMode = false;
			if (auto) {
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		else if (atStep == 4){
			analyze();
			
			interactionMode = false;
			if (auto) {
				atStep = atStep + 1;
				runStep(atStep);
			}
		}
		
	}
	public void runNext(){
		atStep = atStep+1;
		if (atStep > 4) atStep = 4;
	}
	public void runPrevious(){
		atStep = atStep-1;
		if (atStep < 0) atStep = 0;
	}
	public int getStep(){ return atStep;}
	
	public void loopNext(){
		index = 0;
		runStep(atStep);
	}
	public void loopPrevious(){
		index = 0;
		runStep(atStep);
	}
	public void recalculate(){}

	public void startIT() {
		wrap.displayUntilStep();
	}
	
	/**
	 * Apply the roi to all other images
	 */
	public void finishIT() {
		
	}
	
	
	
	
	
	/**
	 * Bin the image for faster processing
	 */
	private void binImage(){
		int newWidth = (int) ((double) imp.getWidth() / bin);
		imp = (ByteProcessor) imp.resize(newWidth);
	}
	
	/**
	 * Bin the image
	 */
	public ImagePlus binImage(ImagePlus im){
		ImageProcessor imp = (ByteProcessor) im.getProcessor().convertToByte(true);
		int newWidth = (int) ((double) imp.getWidth() / bin);
		imp = (ByteProcessor) imp.resize(newWidth);
		ImagePlus result = new ImagePlus("",imp);
		return result;
	}
	
	/**
	 * Pre process and watershed the live image
	 */
	private void preProcessLive(){
		// Get the processor in the right kind
		ShortProcessor shortLive = (ShortProcessor) live.getProcessor().convertToShort(true);
		JEXStatics.logManager.log("Live cell processor reached",1,this);
		
		// Background subtracter
		BackgroundSubtracter bgs = new BackgroundSubtracter();
		
		// Background subtracter parameters
		double radius1 = this.rollingBall;
		boolean createBackground = false; 
		boolean lightBackground = false; 
		boolean useParaboloid = false; 
		boolean doPresmooth = false;
		boolean correctCorners = false;

		// Perform background subtraction for both Hoechst and p65 images
		bgs.rollingBallBackground(shortLive, radius1, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
		JEXStatics.logManager.log("Live image rolling ball performed",1,this);
		
		// Convert ShortProcessor to ByteProcessor for watershedding
		ByteProcessor byteImPlus1 = (ByteProcessor) shortLive.convertToByte(true);
		ImagePlus tempImage       = new ImagePlus("",byteImPlus1);
		ImageStatistics stats     = tempImage.getStatistics();
		double mean = stats.mean;
		
		// Find maxima
		JEXStatics.logManager.log("Live image Finding particles",1,this);
		MaximumFinder finder = new MaximumFinder();
		ByteProcessor out = finder.findMaxima(byteImPlus1, liveThresh, 2*mean, MaximumFinder.SEGMENTED, true, false);
		out.invertLut();
		
		// Convert ByteProcessor back to ImagePlus after watershedding is done
		livePreprocessed = new ImagePlus("Watershed", out.duplicate());
		imp = (ByteProcessor)livePreprocessed.getProcessor().duplicate().convertToByte(true);
	}
	
	/**
	 * Locate cells in the live image
	 */
	private void locateLive(){
		// Use ImageJ Particle Analyzer on data1
//		int options = ParticleAnalyzer.SHOW_MASKS | ParticleAnalyzer.ADD_TO_MANAGER;
		int options = 0;
		int measure = ParticleAnalyzer.AREA | ParticleAnalyzer.CIRCULARITY  | ParticleAnalyzer.INTEGRATED_DENSITY
		| ParticleAnalyzer.CENTROID | ParticleAnalyzer.ELLIPSE;
		
		// Make the particle analyzer
		ResultsTable rt = new ResultsTable();
		double minSize = radius1;
		double maxSize = Double.POSITIVE_INFINITY;
		double minCirc = 0.0;
		double maxCirc = 1.0;
		ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measure, rt, minSize, maxSize, minCirc, maxCirc);
		analyzer.analyze(livePreprocessed);
		JEXStatics.logManager.log("Live image particle analyzer performed",1,this);

		// Acquire the ROIs from the particle analysis and apply to the p65 image	
		List<Roi> foundRois = analyzer.foundRois;
		JEXStatics.logManager.log("Total number of rois is " + foundRois.size(),1,this);
		
		// Get the results out
//		int nb = rt.getColumn(0).length;
		int lastColumn = rt.getLastColumn();
		float[] xPos      = new float[0] ;
		float[] yPos      = new float[0] ;
//		float[] intDen    = new float[0] ;
//		float[] areas     = new float[0] ;
		
		for (int i=0;i<lastColumn;i++){
			String cName = rt.getColumnHeading(i);
			if (cName.equals("X")){
				xPos = rt.getColumn(i);
			}
			if (cName.equals("Y")){
				yPos = rt.getColumn(i);
			}
//			if (cName.equals("Area")){
//				areas = rt.getColumn(i);
//			}
//			if (cName.equals("Mean")){
//				intDen = rt.getColumn(i);
//			}
		}
		
		day1_livePoints = new PointList();
		for (int i=0; i<xPos.length; i++){
			Point p = new Point((int)xPos[i],(int)yPos[i]);
			day1_livePoints.add(p);
		}
	}
	
	/**
	 * Preprocess the dead image
	 */
	private void preProcessDead(){
		// Get the processor in the right kind
		ShortProcessor shortDead = (ShortProcessor) dead.getProcessor().convertToShort(true);
		JEXStatics.logManager.log("Dead cell processor reached",1,this);
		
		// Background subtracter
		BackgroundSubtracter bgs = new BackgroundSubtracter();
		
		// Background subtracter parameters
		double radius1 = this.rollingBall;
		boolean createBackground = false; 
		boolean lightBackground = false; 
		boolean useParaboloid = false; 
		boolean doPresmooth = false;
		boolean correctCorners = false;

		// Perform background subtraction for both Hoechst and p65 images
		bgs.rollingBallBackground(shortDead, radius1, createBackground, lightBackground, useParaboloid, doPresmooth, correctCorners);
		JEXStatics.logManager.log("Dead image rolling ball performed",1,this);
		
		// Convert ShortProcessor to ByteProcessor for watershedding
		ByteProcessor byteImPlus1 = (ByteProcessor) shortDead.convertToByte(true);
		ImagePlus tempImage       = new ImagePlus("",byteImPlus1);
		ImageStatistics stats     = tempImage.getStatistics();
		double mean = stats.mean;
		
		// Find maxima
		JEXStatics.logManager.log("Dead image Finding particles",1,this);
		MaximumFinder finder = new MaximumFinder();
		ByteProcessor out = finder.findMaxima(byteImPlus1, deadThresh, 2*mean, MaximumFinder.SEGMENTED, true, false);
		out.invertLut();
		
		// Convert ByteProcessor back to ImagePlus after watershedding is done
		deadPreprocessed = new ImagePlus("Watershed", out.duplicate());
		imp = (ByteProcessor)deadPreprocessed.getProcessor().duplicate().convertToByte(true);
	}

	private void locateDead(){// Use ImageJ Particle Analyzer on data1
//		int options = ParticleAnalyzer.SHOW_MASKS | ParticleAnalyzer.ADD_TO_MANAGER;
		int options = 0;
		int measure = ParticleAnalyzer.AREA | ParticleAnalyzer.CIRCULARITY  | ParticleAnalyzer.INTEGRATED_DENSITY
		| ParticleAnalyzer.CENTROID | ParticleAnalyzer.ELLIPSE;
		
		// Make the particle analyzer
		ResultsTable rt = new ResultsTable();
		double minSize = radius2;
		double maxSize = Double.POSITIVE_INFINITY;
		double minCirc = 0.0;
		double maxCirc = 1.0;
		ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measure, rt, minSize, maxSize, minCirc, maxCirc);
		analyzer.analyze(deadPreprocessed);
		JEXStatics.logManager.log("Live image particle analyzer performed",1,this);

		// Acquire the ROIs from the particle analysis and apply to the p65 image	
		List<Roi> foundRois = analyzer.foundRois;
		JEXStatics.logManager.log("Total number of rois is " + foundRois.size(),1,this);
		
		// Get the results out
//		int nb = rt.getColumn(0).length;
		int lastColumn = rt.getLastColumn();
		float[] xPos      = new float[0] ;
		float[] yPos      = new float[0] ;
//		float[] intDen    = new float[0] ;
//		float[] areas     = new float[0] ;
		
		for (int i=0;i<lastColumn;i++){
			String cName = rt.getColumnHeading(i);
			if (cName.equals("X")){
				xPos = rt.getColumn(i);
			}
			if (cName.equals("Y")){
				yPos = rt.getColumn(i);
			}
//			if (cName.equals("Area")){
//				areas = rt.getColumn(i);
//			}
//			if (cName.equals("Mean")){
//				intDen = rt.getColumn(i);
//			}
		}
		
		day1_deadPoints = new PointList();
		for (int i=0; i<xPos.length; i++){
			Point p = new Point((int)xPos[i],(int)yPos[i]);
			day1_deadPoints.add(p);
		}
	}
	
	private double distanceBetweenTwoPoints(Point p1, Point p2){
		double result = Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
		return result;
	}
	
	private boolean isPointCloseToPointInList(int radius, Point p, PointList list){
		for (Point pp: list){
			if (distanceBetweenTwoPoints(p,pp) < radius) return true;
		}
		return false;
	}
	
	/**
	 * Analyze proliferation and fill the variables to output
	 */
	private void analyze(){
		int liveNumber = 0;
		int deadNumber = 0;
		
		PointList validatedLive = new PointList();
		for (Point p: day1_livePoints){
			if (isPointCloseToPointInList(doubleRadius,p,day1_deadPoints)) continue;
			validatedLive.add(p);
		}
		  
		liveNumber = validatedLive.size();
		deadNumber = day1_deadPoints.size();

		day1_liveNumber = ValueWriter.makeValueObject(outputNames[0].getName(), ""+liveNumber);
		day1_liveNumber.setDataObjectInfo("Live cell number found with function: 3D live dead cell location for single cells");
		
		day1_deadNumber = ValueWriter.makeValueObject(outputNames[1].getName(), ""+deadNumber);
		day1_deadNumber.setDataObjectInfo("Dead cell number found with function: 3D live dead cell location for single cells");
		
		String[] columnNames = new String[] {"Start X", "Start Y"};
		HashMap<String,String[]> columns = new HashMap<String,String[]>();
		String[] stX = new String[day1_livePoints.size()];
		String[] stY = new String[day1_livePoints.size()];
		for (int i=0; i<day1_livePoints.size(); i++){
			Point p = day1_livePoints.get(i);
			stX[i]  = ""+p.x;
			stY[i]  = ""+p.y;
		}
		columns.put("Start X",stX);
		columns.put("Start Y",stY);
		moreInfo = ValueWriter.makeValueTable(outputNames[3].getName(), columnNames, columns);
		moreInfo.setDataObjectInfo("Additional info from function: 3D live dead cell location for clusters");
		
	}
	
	public void clickedPoint(Point p) {}
	public void pressedPoint(Point p) {}
	public void mouseMoved(Point p){}
	
}






