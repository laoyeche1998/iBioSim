package gcm2sbml.parser;

import gcm2sbml.network.BaseSpecies;
import gcm2sbml.network.ConstantSpecies;
import gcm2sbml.network.GeneticNetwork;
import gcm2sbml.network.PartSpecies;
import gcm2sbml.network.Promoter;
import gcm2sbml.network.Reaction;
import gcm2sbml.network.SpasticSpecies;
import gcm2sbml.network.SpeciesInterface;
import gcm2sbml.util.GlobalConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * This class parses a genetic circuit model.
 * 
 * @author Nam Nguyen
 * 
 */
public class GCMParser {
	
	private String separator;

	public GCMParser(String filename) {
		this(filename, false);
	}

	public GCMParser(String filename, boolean debug) {
		if (File.separator.equals("\\")) {
			separator = "\\\\";
		}
		else {
			separator = File.separator;
		}
		this.debug = debug;
		gcm = new GCMFile(filename.substring(0, filename.length()
				- filename.split(separator)[filename.split(separator).length - 1]
						.length()));
		gcm.load(filename);
		data = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String str;
			while ((str = in.readLine()) != null) {
				data.append(str + "\n");
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Error opening file");
		}
	}

	public GeneticNetwork buildNetwork() {
		org.sbml.libsbml.SBMLDocument sbml = gcm.flattenGCM(true);
		return buildTopLevelNetwork(sbml);
	}
	
	public GeneticNetwork buildTopLevelNetwork(org.sbml.libsbml.SBMLDocument sbml) {
		HashMap<String, Properties> speciesMap = gcm.getSpecies();
		HashMap<String, Properties> reactionMap = gcm.getInfluences();
		HashMap<String, Properties> promoterMap = gcm.getPromoters();

		species = new HashMap<String, SpeciesInterface>();
		promoters = new HashMap<String, Promoter>();
		complexMap = new HashMap<String, ArrayList<PartSpecies>>();

		for (String s : speciesMap.keySet()) {
			SpeciesInterface specie = parseSpeciesData(s, speciesMap.get(s));
			species.put(specie.getId(), specie);
		}
		
		for (String s : promoterMap.keySet()) {
			parsePromoterData(s, promoterMap.get(s));	
		}
		
		for (String s : reactionMap.keySet()) {
			parseReactionData(s, reactionMap.get(s));			
		}
		
		GeneticNetwork network = new GeneticNetwork(species, complexMap,
				promoters, gcm);
		
		network.setSBMLFile(gcm.getSBMLFile());
		if (sbml != null) {
			network.setSBML(sbml);
		}
		return network;		
	}

	public void printFile() {
		System.out.println(data.toString());
	}
	

	public HashMap<String, SpeciesInterface> getSpecies() {
		return species;
	}

	public void setSpecies(HashMap<String, SpeciesInterface> species) {
		this.species = species;
	}

	public HashMap<String, Promoter> getPromoters() {
		return promoters;
	}

	public void setPromoters(HashMap<String, Promoter> promoters) {
		this.promoters = promoters;
	}

	private Promoter parsePromoterData(String promoterID, Properties property) {
		Promoter p = new Promoter();
		p.setId(promoterID);
		promoters.put(promoterID, p);
		
		if (property != null && property.containsKey(GlobalConstants.PROMOTER_COUNT_STRING)) {
			p.addProperty(GlobalConstants.PROMOTER_COUNT_STRING, property.getProperty(GlobalConstants.PROMOTER_COUNT_STRING));
		} else {
			p.addProperty(GlobalConstants.PROMOTER_COUNT_STRING, gcm.getParameter(GlobalConstants.PROMOTER_COUNT_STRING));
		} 
		
		if (property != null && property.containsKey(GlobalConstants.ACTIVED_STRING)) {
			p.addProperty(GlobalConstants.ACTIVED_STRING, property.getProperty(GlobalConstants.ACTIVED_STRING));
		} else {
			p.addProperty(GlobalConstants.ACTIVED_STRING, gcm.getParameter(GlobalConstants.ACTIVED_STRING));
		} 
		
		if (property != null && property.containsKey(GlobalConstants.STOICHIOMETRY_STRING)) {
			p.addProperty(GlobalConstants.STOICHIOMETRY_STRING, property.getProperty(GlobalConstants.STOICHIOMETRY_STRING));
		} else {
			p.addProperty(GlobalConstants.STOICHIOMETRY_STRING, gcm.getParameter(GlobalConstants.STOICHIOMETRY_STRING));
		} 
		
		if (property != null && property.containsKey(GlobalConstants.OCR_STRING)) {
			p.addProperty(GlobalConstants.OCR_STRING, property.getProperty(GlobalConstants.OCR_STRING));
		} else {
			p.addProperty(GlobalConstants.OCR_STRING, gcm.getParameter(GlobalConstants.OCR_STRING));
		} 
		
		if (property != null && property.containsKey(GlobalConstants.KBASAL_STRING)) {
			p.addProperty(GlobalConstants.KBASAL_STRING, property.getProperty(GlobalConstants.KBASAL_STRING));
		} else {
			p.addProperty(GlobalConstants.KBASAL_STRING, gcm.getParameter(GlobalConstants.KBASAL_STRING));
		} 
		

		if (property != null && property.containsKey(GlobalConstants.RNAP_BINDING_STRING)) {
			p.addProperty(GlobalConstants.RNAP_BINDING_STRING, property.getProperty(GlobalConstants.RNAP_BINDING_STRING));
		} else {
			p.addProperty(GlobalConstants.RNAP_BINDING_STRING, gcm.getParameter(GlobalConstants.RNAP_BINDING_STRING));
		} 
		return p;
		
	}
	
	
	/**
	 * Parses the reactions in the network
	 * 
	 * @param reaction
	 *            the reaction to parse
	 * @param stateNameOutput
	 *            the name of the output
	 * 
	 */
	// TODO: Match rate constants
	private void parseReactionData(String reaction, Properties property) {
		Reaction r = new Reaction();		
		r.generateName();		
		
		if (property.containsKey(GlobalConstants.COOPERATIVITY_STRING)) {
			r.addProperty(GlobalConstants.COOPERATIVITY_STRING, property.getProperty(GlobalConstants.COOPERATIVITY_STRING));
		} else {
			r.addProperty(GlobalConstants.COOPERATIVITY_STRING, gcm.getParameter(GlobalConstants.COOPERATIVITY_STRING));
		} 
		
		if (property.getProperty(GlobalConstants.TYPE).equals(GlobalConstants.ACTIVATION)) {
			r.setType("vee");
			if (property.containsKey(GlobalConstants.KACT_STRING)) {
				r.addProperty(GlobalConstants.KACT_STRING, property.getProperty(GlobalConstants.KACT_STRING));
			} else {
				r.addProperty(GlobalConstants.KACT_STRING, gcm.getParameter(GlobalConstants.KACT_STRING));
			} 
		} else if (property.getProperty(GlobalConstants.TYPE).equals(GlobalConstants.REPRESSION)) {
			r.setType("tee");
			if (property.containsKey(GlobalConstants.KREP_STRING)) {
				r.addProperty(GlobalConstants.KREP_STRING, property.getProperty(GlobalConstants.KREP_STRING));
			} else {
				r.addProperty(GlobalConstants.KREP_STRING, gcm.getParameter(GlobalConstants.KREP_STRING));
			} 	
		} else if (property.getProperty(GlobalConstants.TYPE).equals(GlobalConstants.COMPLEX)) {
			r.setType("plus");
		}
		else {
			r.setType("dot");
			if (property.containsKey(GlobalConstants.KREP_STRING)) {
				r.addProperty(GlobalConstants.KREP_STRING, property.getProperty(GlobalConstants.KREP_STRING));
			}					
		}
		
		String input = GCMFile.getInput(reaction);
		String output = GCMFile.getOutput(reaction);
		r.setInput(input);
		r.setOutput(output);
		if (r.getType().equals("plus")) {
			ArrayList<PartSpecies> parts = null;
			if (complexMap.containsKey(output)) {
				parts = complexMap.get(output);
			} else { 
				parts = new ArrayList<PartSpecies>();
				complexMap.put(output, parts);
			}
			PartSpecies ps = new PartSpecies(species.get(input), r.getCoop());
			parts.add(ps);
		} else {	
			String promoterName = "";
			if (property.containsKey(GlobalConstants.PROMOTER)) {
				promoterName = property.getProperty(GlobalConstants.PROMOTER);
			} else {
				promoterName = "Promoter_" + GCMFile.getOutput(reaction);
			}
			// Check if promoter exists. If not, create it.
			Promoter p = null;
			if (promoters.containsKey(promoterName)) {
				p = promoters.get(promoterName);
			} else {
				p = parsePromoterData(promoterName, null);
			}
			p.addToReactionMap(input, r);
			if (r.getType().equals("vee"))
				p.addActivator(input, species.get(input));
			else
				p.addRepressor(input, species.get(input));
		}
	}
	

	/**
	 * Parses the data and put it into the species
	 * 
	 * @param name
	 *            the name of the species
	 * @param properties
	 *            the properties of the species
	 */
	private SpeciesInterface parseSpeciesData(String name, Properties property) {
		SpeciesInterface specie = null;

		if (property.getProperty(GlobalConstants.TYPE).equals(GlobalConstants.CONSTANT) ||
				property.getProperty(GlobalConstants.TYPE).equals(GlobalConstants.INPUT)) {
			specie = new ConstantSpecies();
		} else if (property.getProperty(GlobalConstants.TYPE).equals(GlobalConstants.SPASTIC)) {
			specie = new SpasticSpecies();
		} else {
			specie = new BaseSpecies();
		}

		if (property.containsKey(GlobalConstants.KCOMPLEX_STRING)) {
			specie.addProperty(GlobalConstants.KCOMPLEX_STRING, property.getProperty(GlobalConstants.KCOMPLEX_STRING));
		} else {
			specie.addProperty(GlobalConstants.KCOMPLEX_STRING, gcm.getParameter(GlobalConstants.KCOMPLEX_STRING));
		}
		
		if (property.containsKey(GlobalConstants.KASSOCIATION_STRING)) {
			specie.addProperty(GlobalConstants.KASSOCIATION_STRING, property.getProperty(GlobalConstants.KASSOCIATION_STRING));
		} else {
			specie.addProperty(GlobalConstants.KASSOCIATION_STRING, gcm.getParameter(GlobalConstants.KASSOCIATION_STRING));
		}
		
		if (property.containsKey(GlobalConstants.INITIAL_STRING)) {
			specie.addProperty(GlobalConstants.INITIAL_STRING, property.getProperty(GlobalConstants.INITIAL_STRING));
		} else {
			specie.addProperty(GlobalConstants.INITIAL_STRING, gcm.getParameter(GlobalConstants.INITIAL_STRING));
		}
		
		if (property.containsKey(GlobalConstants.KDECAY_STRING)) {
			specie.addProperty(GlobalConstants.KDECAY_STRING, property.getProperty(GlobalConstants.KDECAY_STRING));
		} else {
			specie.addProperty(GlobalConstants.KDECAY_STRING, gcm.getParameter(GlobalConstants.KDECAY_STRING));
		}
		
		if (property.containsKey(GlobalConstants.TYPE)) {
			specie.addProperty(GlobalConstants.TYPE, property.getProperty(GlobalConstants.TYPE));
		} else {
			specie.addProperty(GlobalConstants.TYPE, gcm.getParameter(GlobalConstants.TYPE));
		}
		
		specie.setId(property.getProperty(GlobalConstants.ID));
		specie.setName(property.getProperty(GlobalConstants.NAME,
				property.getProperty(GlobalConstants.ID)));
		specie.setStateName(property.getProperty(GlobalConstants.ID));
		
		return specie;
	}
	
	public void setParameters(HashMap<String, String> parameters) {
		gcm.setParameters(parameters);
	}

	// Holds the text of the GCM
	private StringBuffer data = null;

	private HashMap<String, SpeciesInterface> species;

	private HashMap<String, Promoter> promoters;
	
	private HashMap<String, ArrayList<PartSpecies>> complexMap;

	private GCMFile gcm = null;

	// A regex that matches information
	private static final String STATE = "(^|\\n) *([^- \\n]*) *\\[(.*)\\]";

	private static final String REACTION = "(^|\\n) *([^ \\n]*) *\\-\\> *([^ \n]*) *\\[(.*)arrowhead=([^,\\]]*)(.*)";

	private static final String PROPERTY_NUMBER = "([a-zA-Z]+)=\"([\\d]*[\\.\\d]?\\d+)\"";

	// private static final String PROPERTY_STATE = "([a-zA-Z]+)=([^\\s,.\"]+)";

	// private static final String PROPERTY_QUOTE =
	// "([a-zA-Z]+)=\"([^\\s,.\"]+)\"";

	private static final String PROPERTY_STATE = "([a-zA-Z\\s\\-]+)=([^\\s,]+)";

	// Debug level
	private boolean debug = false;
}
