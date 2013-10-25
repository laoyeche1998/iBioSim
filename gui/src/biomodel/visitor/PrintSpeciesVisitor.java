package biomodel.visitor;

import java.util.HashMap;
import java.util.Properties;

import main.Gui;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.text.parser.ParseException;

import biomodel.network.BaseSpecies;
import biomodel.network.ComplexSpecies;
import biomodel.network.ConstantSpecies;
import biomodel.network.DiffusibleConstitutiveSpecies;
import biomodel.network.DiffusibleSpecies;
import biomodel.network.SpasticSpecies;
import biomodel.network.SpeciesInterface;
import biomodel.util.GlobalConstants;
import biomodel.util.Utility;


public class PrintSpeciesVisitor extends AbstractPrintVisitor {

	public PrintSpeciesVisitor(SBMLDocument document,
			HashMap<String, SpeciesInterface> species, HashMap<String, Properties> compartments) {
		super(document);
		this.species = species;
		this.compartments = compartments;
	}

	/**
	 * Prints out all the species to the file
	 * 
	 */
	public void run() {
		for (SpeciesInterface s : species.values()) {
			s.accept(this);
		}
	}
	
	@Override
	public void visitComplex(ComplexSpecies specie) {
		if (!complexAbstraction || (!specie.isAbstractable() && !specie.isSequesterAbstractable())) {
			loadValues(specie);
			String compartment = checkCompartments(specie.getId());
			Species s = Utility.makeSpecies(specie.getId(), compartment, amount, concentration);
//			s.setName(specie.getName()); this causes things to break...specie lacks name for some reason
			s.setHasOnlySubstanceUnits(true);
			Utility.addSpecies(document, s);
		} else
			 document.getModel().removeSpecies(specie.getId());

	}

	@Override
	public void visitBaseSpecies(BaseSpecies specie) {
		loadValues(specie);
		String compartment = checkCompartments(specie.getId());
		Species s = Utility.makeSpecies(specie.getId(), compartment, amount, concentration);
		s.setName(specie.getName());
		s.setHasOnlySubstanceUnits(true);
		Utility.addSpecies(document, s);
	}

	@Override
	public void visitConstantSpecies(ConstantSpecies specie) {
		loadValues(specie);
		String compartment = checkCompartments(specie.getId());
		Species s = Utility.makeSpecies(specie.getId(), compartment, amount, concentration);
		s.setName(specie.getName());
		s.setHasOnlySubstanceUnits(true);
		s.setBoundaryCondition(true);
		//s.setConstant(true);
		Utility.addSpecies(document, s);
	}

	@Override
	public void visitSpasticSpecies(SpasticSpecies specie) {
		loadValues(specie);
		String compartment = checkCompartments(specie.getId());
		Species s = Utility.makeSpecies(specie.getId(), compartment, amount, concentration);
		s.setName(specie.getName());
		s.setHasOnlySubstanceUnits(true);
		Utility.addSpecies(document, s);
		
		r = new org.sbml.jsbml.Reaction(Gui.SBML_LEVEL, Gui.SBML_VERSION);
		r.setId("Constitutive_production_" + s.getId());
		r.setCompartment(compartment);
		r.addProduct(Utility.SpeciesReference(s.getId(), specie.getnp()));
		
		r.setReversible(false);
		r.setFast(false);
		kl = r.createKineticLaw();
		kl.addLocalParameter(Utility.Parameter("kp", specie.getKo()));
		try {
			kl.setFormula("kp");
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Utility.addReaction(document, r);		
	}
	
	public void visitDiffusibleSpecies(DiffusibleSpecies species) {
		
		loadValues(species);
		String compartment = checkCompartments(species.getId());
		Species s = Utility.makeSpecies(species.getId(), compartment, amount, concentration);
		s.setName(species.getName());
		s.setHasOnlySubstanceUnits(true);
		Utility.addSpecies(document, s);
	}
	
	public void visitDiffusibleConstitutiveSpecies(DiffusibleConstitutiveSpecies specie) {
		
		loadValues(specie);
		String compartment = checkCompartments(specie.getId());
		Species s = Utility.makeSpecies(specie.getId(), compartment, amount, concentration);
		s.setName(specie.getName());
		s.setHasOnlySubstanceUnits(true);
		Utility.addSpecies(document, s);
		
		r = new org.sbml.jsbml.Reaction(Gui.SBML_LEVEL, Gui.SBML_VERSION);
		r.setId("Constitutive_production_" + s.getId());
		r.setCompartment(compartment);
		r.addProduct(Utility.SpeciesReference(s.getId(), Double.parseDouble(parameters.getParameter(GlobalConstants.STOICHIOMETRY_STRING))));
		
		r.setReversible(false);
		r.setFast(false);
		kl = r.createKineticLaw();
		kl.addLocalParameter(Utility.Parameter("kp", Double.parseDouble(parameters
					.getParameter((GlobalConstants.OCR_STRING)))));	
		try {
			kl.setFormula("kp");
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Utility.addReaction(document, r);	
	}
	
	private void loadValues(SpeciesInterface specie) {
		amount = specie.getInitialAmount();
		concentration = specie.getInitialConcentration();
	}
	
	//Checks if species belongs in a compartment other than default
	private String checkCompartments(String species) {
		
		String compartment = document.getModel().getCompartment(0).getId();
		
		if (compartments != null) {
			//String[] splitted = species.split("__");
			String component = species;
			
			while (component.contains("__")) {
				
				component = component.substring(0, component.lastIndexOf("__"));
				
				for (String compartmentName : compartments.keySet()) {
					
					if (compartmentName.equals(component))
						return compartmentName;					
					else if (compartmentName.contains("__") && compartmentName.substring(0, compartmentName.lastIndexOf("__"))
							.equals(component)) {
						return compartmentName;
					}
				}
			}
			/*
			if (compartments.contains(splitted[0]))
				compartment = splitted[0];
				*/
		}
		return compartment;
	}
	
	
	private double amount;
	private double concentration;
	private HashMap<String, Properties> compartments;

}

