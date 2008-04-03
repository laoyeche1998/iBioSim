package gcm2sbml.scripts;

import java.util.ArrayList;

public class SpeciesThresholdTester extends AbstractTester {
	
	
	public SpeciesThresholdTester(String folder, String type, ArrayList<String> highSpecies,
			ArrayList<String> lowSpecies) {
		super(highSpecies, lowSpecies, null, null, 4000, 100, 6100);
		this.folder = folder;
		this.type = type;
		init();
	}
	
	private void init() {
		GCMScript script = new GCMScript();
		ArrayList<String> species = new ArrayList<String>();
		species.addAll(highSpecies);
		species.addAll(lowSpecies);
		double[][] thresholds = script.generateThreshold(folder, species, type, 3800);
		highThreshold = new double[highSpecies.size()];
		lowThreshold = new double[lowSpecies.size()];
		
		for (int i = 0; i < highSpecies.size(); i++) {
			highThreshold[i] = thresholds[1][i];
		}
		for (int i = 0; i < lowSpecies.size(); i++) {
			lowThreshold[i] = thresholds[0][i];
		}
		System.out.print("");
	}
	
	private String folder = null;
	private String type = null;
}
