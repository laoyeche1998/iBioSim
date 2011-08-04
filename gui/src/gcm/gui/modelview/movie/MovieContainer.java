package gcm.gui.modelview.movie;

import gcm.gui.GCM2SBMLEditor;
import gcm.gui.schematic.ListChooser;
import gcm.gui.schematic.Schematic;
import gcm.gui.schematic.SchematicObjectClickEvent;
import gcm.gui.schematic.SchematicObjectClickEventListener;
import gcm.gui.schematic.TreeChooser;
import gcm.gui.schematic.Utils;
import gcm.parser.GCMFile;
import gcm.util.GlobalConstants;

import main.Gui;
import parser.TSDParser;
import reb2sac.Reb2Sac;

import com.google.gson.Gson;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;


public class MovieContainer extends JPanel implements ActionListener {

	public static final String COLOR_PREPEND = "_COLOR";
	public static final String MIN_PREPEND = "_MIN";
	public static final String MAX_PREPEND = "_MAX";
	
	private final String PLAYING = "playing";
	private final String PAUSED = "paused";
	
	private String mode = PLAYING;
	
	public static final int FRAME_DELAY_MILLISECONDS = 20;
	
	private static final long serialVersionUID = 1L;
	
	private Schematic schematic;
	private Reb2Sac reb2sac;	
	private GCMFile gcm;
	private Gui biosim;
	private GCM2SBMLEditor gcm2sbml;	
	private TSDParser parser;
	private Timer playTimer;
	private MoviePreferences moviePreferences;
	private MovieScheme movieScheme;
	
	private boolean isUIInitialized;
	private boolean isDirty = false;
	
	//movie toolbar/UI elements
	private JButton fileButton;
	private JButton playPauseButton;
	private JButton rewindButton;
	private JButton singleStepButton;
	private JSlider slider;
	
	
	/**
	 * constructor
	 * 
	 * @param reb2sac_
	 * @param gcm
	 * @param biosim
	 * @param gcm2sbml
	 */
	public MovieContainer(Reb2Sac reb2sac_, GCMFile gcm, Gui biosim, GCM2SBMLEditor gcm2sbml){
		
		super(new BorderLayout());
		
		schematic = new Schematic(gcm, biosim, gcm2sbml, false, this, null, gcm.getReactionPanel());
		this.add(schematic, BorderLayout.CENTER);
		
		this.gcm = gcm;
		this.biosim = biosim;
		this.reb2sac = reb2sac_;
		this.gcm2sbml = gcm2sbml;
		this.movieScheme = new MovieScheme();
		
		loadPreferences();
		
		this.playTimer = new Timer(0, playTimerEventHandler);
		mode = PAUSED;
		
		//registerEventListeners();
	}	
	
	
	
	//TSD FILE METHODS
	
	/**
	 * returns a vector of strings of TSD filenames within a directory
	 * i don't know why it doesn't return a vector of strings
	 * 
	 * @param directoryName directory for search for files in
	 * @return TSD filenames within the directory
	 */
	private Vector<Object> recurseTSDFiles(String directoryName){
		
		Vector<Object> filenames = new Vector<Object>();
		
		filenames.add(new File(directoryName).getName());
		
		for (String s : new File(directoryName).list()){
			
			String fullFileName = directoryName + File.separator + s;
			File f = new File(fullFileName);
			
			if(s.endsWith(".tsd") && f.isFile()){
				filenames.add(s);
			}
			else if(f.isDirectory()){
				filenames.add(recurseTSDFiles(fullFileName));
			}
		}
		
		return filenames;
	}
	
	
	/**
	 * opens a treechooser of the TSD files, then loads and parses the selected TSD file
	 * 
	 * @throws ListChooser.EmptyListException
	 */
	private void prepareTSDFile(){
		
		pause();
	
		// if simID is present, go up one directory.
		String simPath = reb2sac.getSimPath();
		String simID = reb2sac.getSimID();
		
		if(!simID.equals("")){
			simPath = new File(simPath).getParent();
		}
		
		Vector<Object> filenames = recurseTSDFiles(simPath);
		
		String filename;
		
		try{
			filename = TreeChooser.selectFromTree(Gui.frame, filenames, "Choose a simulation file");
		}
		catch(TreeChooser.EmptyTreeException e){
			JOptionPane.showMessageDialog(Gui.frame, "Sorry, there aren't any simulation files. Please simulate then try again.");
			return;
		}
		
		if(filename == null)
			return;
		
		String fullFilePath = reb2sac.getRootPath() + filename;
		this.parser = new TSDParser(fullFilePath, false);
		
		slider.setMaximum(parser.getNumSamples()-1);
		slider.setValue(0);
		
		biosim.log.addText(fullFilePath + " loaded. " + 
				String.valueOf(parser.getData().size()) +
				" rows of data loaded.");
	}
	
	
	
	//UI METHODS
	
	/**
	 * displays the schematic and the movie UI
	 */
	public void display(){
		
		schematic.display();

		if(isUIInitialized == false){
			this.addPlayUI();
			
			isUIInitialized = true;
		}
		/*
		if(this.parser == null)
			prepareTSDFile();
		 */
	}
	
	
	/**
	 * adds the toolbar at the bottom
	 */
	private void addPlayUI(){
		// Add the bottom menu bar
		JToolBar mt = new JToolBar();
		
		fileButton = Utils.makeToolButton("", "choose_simulation_file", "Choose TSD File", this);
		mt.add(fileButton);
		
		rewindButton = Utils.makeToolButton("movie" + File.separator + "rewind.png", "rewind", "Rewind", this);
		mt.add(rewindButton);

		singleStepButton = Utils.makeToolButton("movie" + File.separator + "single_step.png", "singlestep", "Single Step", this);
		mt.add(singleStepButton);
		
		playPauseButton = Utils.makeToolButton("movie" + File.separator + "play.png", "playpause", "Play", this);
		mt.add(playPauseButton);
		
		slider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 0);
		slider.setSnapToTicks(true);
		mt.add(slider);
		
		mt.setFloatable(false);

		this.add(mt, BorderLayout.SOUTH);
	}
	
	
	
	//EVENT METHODS
	
	/**
	 * event handler for when UI buttons are pressed.
	 */
	public void actionPerformed(ActionEvent event) {
		// TODO Auto-generated method stub
		String command = event.getActionCommand();
		
		if(command.equals("rewind")){
			if(parser == null){
				JOptionPane.showMessageDialog(Gui.frame, "Must first choose a simulation file.");
			} 
			else {
				slider.setValue(0);
				updateVisuals();
			}
		}
		else if(command.equals("playpause")){
			if(parser == null){
				JOptionPane.showMessageDialog(Gui.frame, "Must first choose a simulation file.");
			} 
			else {
				playPauseButtonPress();
			}
		}
		else if(command.equals("singlestep")){
			if(parser == null){
				JOptionPane.showMessageDialog(Gui.frame, "Must first choose a simulation file.");
			} 
			else {
				nextFrame();
			}
		}
		else if(command.equals("choose_simulation_file")){
			prepareTSDFile();
		}
		else{
			throw new Error("Unrecognized command '" + command + "'!");
		}
	}
	
	
	/**
	 * event handler for when the timer ticks
	 */
	ActionListener playTimerEventHandler = new ActionListener() {
		public void actionPerformed(ActionEvent evt) {
			nextFrame();
		}
	};
	
	
	
	//MOVIE CONTROL METHODS
	
	/**
	 * switches between play/pause modes
	 * 
	 * Called whenever the play/pause button is pressed, or when the system needs to 
	 * pause the movie (such as at the end)
	 */
	private void playPauseButtonPress(){
		
		if(mode == PAUSED){
			
			if(slider.getValue() >= slider.getMaximum()-1)
				slider.setValue(0);
			
			playTimer.setDelay(FRAME_DELAY_MILLISECONDS);
			//playPauseButton.setText("Pause");
			Utils.setIcon(playPauseButton, "movie" + File.separator + "pause.png");
			playTimer.start();
			mode = PLAYING;
		}
		else{
			
			Utils.setIcon(playPauseButton, "movie" + File.separator + "play.png");
			playTimer.stop();
			mode = PAUSED;
		}		
	}
	
	/**
	 * calls playpausebuttonpress to pause the movie
	 */
	private void pause(){
		
		if(mode == PLAYING)
			playPauseButtonPress();
	}
	
	/**
	 * advances the movie to the next frame
	 */
	private void nextFrame(){
		
		slider.setValue(slider.getValue()+1);
		
		if(slider.getValue() >= slider.getMaximum()){
			pause();
		}
		
		updateVisuals();
	}
	
	
	/**
	 * updates the visual appearance of cells on the graph (ie, species, components, etc.)
	 * gets called when the timer ticks
	 */
	private void updateVisuals(){
		
		if(parser == null){
			throw new Error("NoSimFileChosen");
		}
		
		int frameIndex = slider.getValue();
		
		if(frameIndex < 0 || frameIndex > parser.getNumSamples()-1){
			throw new Error("Invalid slider value! It is outside the data range!");
		}
		
		HashMap<String, ArrayList<Double>> speciesTSData = parser.getHashMap();
		
		//doesn't do anything
		//schematic.beginFrame();
		
		//loop through the species and set their appearances
		for(String speciesID : gcm.getSpecies().keySet()){
			
			//make sure this species has data in the TSD file
			if(speciesTSData.containsKey(speciesID)){
				
				double value = speciesTSData.get(speciesID).get(frameIndex);
				MovieAppearance appearance = 
					moviePreferences.getOrCreateColorSchemeForSpecies(speciesID, null).getAppearance(value);
				schematic.getGraph().setSpeciesAnimationValue(speciesID, appearance);
			}
		}
		
		//loop through the components and set their appearances
		for(String compID : gcm.getComponents().keySet()){
			
			//get the component's appearance and send it to the graph for updating
			MovieAppearance compAppearance = 
				movieScheme.getAppearance(compID, GlobalConstants.COMPONENT, frameIndex, speciesTSData);
			
			if (compAppearance != null)
				schematic.getGraph().setComponentAnimationValue(compID, compAppearance);
			
			
//			ComponentScheme componentScheme = 
//				moviePreferences.getComponentSchemeForComponent(compID);
//			
//			if(componentScheme != null){
//				
//				MovieAppearance appearance = componentScheme.getAppearance(speciesTSData, frameIndex);
//				schematic.getGraph().setComponentAnimationValue(compID, appearance);
//			}
		}
		
		//if there's a grid to set the appearance of
		if (gcm.getGrid().isEnabled()) {
			
			//loop through all grid locations and set appearances
			for (int row = 0; row < gcm.getGrid().getNumRows(); ++row) {
				for (int col = 0; col < gcm.getGrid().getNumCols(); ++col) {
					
					String gridID = "ROW" + row + "_COL" + col;
					
					//get the component's appearance and send it to the graph for updating
					MovieAppearance gridAppearance = 
						movieScheme.getAppearance(gridID, GlobalConstants.GRID_RECTANGLE, frameIndex, speciesTSData);
					
					if (gridAppearance != null)
						schematic.getGraph().setGridRectangleAnimationValue(gridID, gridAppearance);
				}
			}			
		}
		
		schematic.getGraph().refresh();	
	}

	
	
	//PREFERENCES METHODS
	
	/**
	 * outputs the preferences file
	 */
	public void savePreferences(){
		/*
		 * TODO: This should be getting called when the properties get saved.
		 * Save them to a file in path, then remove the test save button.
		 * Then work on loading the properties back.
		 */
		Gson gson = (new GsonMaker()).makeGson();
		String out = gson.toJson(this.getMoviePreferences());
		
		String fullPath = getPreferencesFullPath();
		
		FileOutputStream fHandle;
		
		try {
			fHandle = new FileOutputStream(fullPath);
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(Gui.frame, "An error occured opening preferences file " + fullPath + "\nmessage: " + e.getMessage());
			return;
		}
		
		try {
			fHandle.write(out.getBytes());
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(Gui.frame, "An error occured writing the preferences file " + fullPath + "\nmessage: " + e.getMessage());
		}
		
		try {
			fHandle.close();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(Gui.frame, "An error occured closing the preferences file " + fullPath + "\nmessage: " + e.getMessage());
			return;
		}
		
		biosim.log.addText("file saved to " + fullPath);
		
		this.gcm2sbml.saveParams(false, "", true);
	}

	
	/**
	 * Loads the preferences file if it exists and stores its values into the moviePreferences object.
	 * If no preferences file exists, a new moviePreferences file will still be created.
	 */
	public void loadPreferences(){
		
		// load the prefs file if it exists
		String fullPath = getPreferencesFullPath();
		String json = null;
		
		try {
			json = TSDParser.readFileToString(fullPath);
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		if(json == null){
			moviePreferences = new MoviePreferences();			
		}
		else{
			
			Gson gson = (new GsonMaker()).makeGson();
			
			try{
				moviePreferences = gson.fromJson(json, MoviePreferences.class);
			}
			catch(Exception e){
				biosim.log.addText("An error occured trying to load the preferences file " + fullPath + " ERROR: " + e.toString());
			}
		}
	}
	
	
	/**
	 * 
	 * @param compName
	 */
	public void copyMoviePreferencesComponent(String compName){
		this.moviePreferences.copyMoviePreferencesComponent(compName, this.gcm, this.getTSDParser());
	}
	
	
	/**
	 * 
	 * @param speciesName
	 */
	public void copyMoviePreferencesSpecies(String speciesName){
		this.moviePreferences.copyMoviePreferencesSpecies(speciesName, this.gcm, this.getTSDParser());
	}


	
	//GET/SET METHODS
	
	public boolean getIsDirty(){
		return isDirty;
	}
	
	public void setIsDirty(boolean value) {
		isDirty = value;
	}
	
	public TSDParser getTSDParser() {
		return parser;
	}

	public GCM2SBMLEditor getGCM2SBMLEditor() {
		return gcm2sbml;
	}
	
	private String getPreferencesFullPath(){
		String path = reb2sac.getSimPath();
		String fullPath = path + File.separator + "schematic_preferences.json";
		return fullPath;
	}
	
	public MoviePreferences getMoviePreferences() {
		return moviePreferences;
	}

	public Schematic getSchematic() {
		return schematic;
	}

	public MovieScheme getMovieScheme() {
		return movieScheme;
	}
	
	public GCMFile getGCM() {
		return gcm;
	}

//	//this currently does nothing
//	private void registerEventListeners(){
//		
//	//	final MovieContainer self = this;
//		
//		// When the user clicks on an object in the schematic
//		this.schematic.addSchematicObjectClickEventListener(new SchematicObjectClickEventListener() {
//			
//			public void SchematicObjectClickEventOccurred(SchematicObjectClickEvent evt) {
////				
////				
////				if(evt.getType() == GlobalConstants.SPECIES){
////					// A species was clicked on
////				}
////			
////				}else if(evt.getType() == GlobalConstants.COMPONENT){
////					// the clicked object is a component
////				}
//			}
//		});
//	}
}
