package sbol;


import java.awt.GridLayout;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import main.Gui;

import org.sbml.libsbml.Model;
import org.sbolstandard.core.*;
import org.sbolstandard.core.impl.*;
import org.sbolstandard.core.impl.AggregatingResolver.UseFirstFound;
import org.sbolstandard.core.util.SequenceOntology;

import biomodel.annotation.AnnotationUtility;
import biomodel.annotation.SBOLAnnotation;
import biomodel.network.SynthesisNode;
import biomodel.parser.BioModel;
import biomodel.util.GlobalConstants;
import biomodel.util.Utility;

public class SBOLSynthesizer {
	
	private BioModel bioModel;
	private LinkedList<URI> modelURIs;
	private LinkedHashMap<String, SynthesisNode> synMap;
	private Set<String> sbolFiles;
	private Set<String> sbolFilePaths;
	private UseFirstFound<DnaComponent, URI> aggregateCompResolver = new AggregatingResolver.UseFirstFound<DnaComponent, URI>();
	private HashMap<String, Resolver<DnaComponent, URI>> fileResolverMap = new HashMap<String, Resolver<DnaComponent, URI>>();
//	private Set<DnaComponent> localComps;
//	private boolean localMatch;
	private String targetFile;
	private String uriAuthority;
	private String time;
	
	public SBOLSynthesizer(BioModel biomodel, LinkedList<URI> modelURIs, LinkedHashMap<String, SynthesisNode> synMap) {
		this.bioModel = biomodel;
		this.modelURIs = modelURIs;
		this.synMap = synMap;
	}
	
	public boolean loadSbolFiles(Set<String> sbolFilePaths) {
		if (sbolFilePaths.size() == 0) {
			JOptionPane.showMessageDialog(Gui.frame, "No SBOL files are found in project.", 
					"File Not Found", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		this.sbolFiles = new HashSet<String>();
		this.sbolFilePaths = sbolFilePaths;
//		localComps = new HashSet<DnaComponent>();

		LinkedList<Resolver<DnaComponent, URI>> compResolvers = new LinkedList<Resolver<DnaComponent, URI>>();
		for (String filePath : sbolFilePaths) {
			String sbolFile = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
			this.sbolFiles.add(sbolFile);
			SBOLDocument sbolDoc = SBOLUtility.loadSBOLFile(filePath);
			if (sbolDoc != null) {
				SBOLDocumentImpl flattenedDoc = SBOLUtility.flattenDocument(sbolDoc);
				Resolver<DnaComponent, URI> compResolver = flattenedDoc.getComponentUriResolver();
				compResolvers.add(compResolver);
				fileResolverMap.put(sbolFile, compResolver);
//				for (SBOLRootObject sbolObj : flattenedDoc.getContents()) {
//					if (sbolObj instanceof DnaComponent)
//						localComps.add((DnaComponent) sbolObj);
//				}
			} else
				return false;
		}
		aggregateCompResolver.setResolvers(compResolvers);
		
		return true;
	}
	
	public DnaComponent exportDnaComponent(String exportFilePath, String saveDirectory) {
		DnaComponent synthComp = saveDnaComponent(saveDirectory, true);
//		DnaComponent synthComp = null;
//		if (loadNodeDNAComponents("export", filePath)) {
//			synthComp = synthesizeDnaComponent();
//			if (synthComp != null) {
//				if (!localMatch) {
//					String[] descriptors = getDescriptorsFromUser();
//				}
				SBOLDocumentImpl sbolDoc = (SBOLDocumentImpl) SBOLFactory.createDocument();
				SBOLUtility.addDNAComponent(synthComp, sbolDoc, true);
				SBOLUtility.writeSBOLDocument(exportFilePath, sbolDoc);
//			} 
//		}
		return synthComp;
	}
	
	// Saves synthesized DNA component to local SBOL file and annotates SBML model
	public DnaComponent saveDnaComponent(String saveDirectory, boolean saveModel) {
		DnaComponent synthComp = null;
		if (loadNodeDNAComponents("save", saveDirectory)) {
			synthComp = synthesizeDnaComponent();
			if (synthComp != null) {
				for (String sbolFile : sbolFiles) {
					SBOLDocumentImpl sbolDoc = (SBOLDocumentImpl) SBOLUtility.loadSBOLFile(saveDirectory + File.separator + sbolFile);
					SBOLUtility.mergeDNAComponent(synthComp, sbolDoc);
					if (sbolFile.equals(targetFile)) {
						SBOLUtility.addDNAComponent(synthComp, sbolDoc, true);
					}
					SBOLUtility.writeSBOLDocument(saveDirectory + File.separator + sbolFile, sbolDoc);
				}
				Model sbmlModel = bioModel.getSBMLDocument().getModel();
				if (modelURIs.size() == 0)
					modelURIs.add(synthComp.getURI());
				SBOLAnnotation sbolAnnot = new SBOLAnnotation(sbmlModel.getMetaId(), modelURIs);
				AnnotationUtility.setSBOLAnnotation(sbmlModel, sbolAnnot);
				bioModel.setModelSBOLAnnotationFlag(true);
				if (saveModel)
					bioModel.save(saveDirectory + File.separator + sbmlModel.getId() + ".gcm");
				
			}
		}
		return synthComp;
	}
	
	// Loads DNA components for synthesis nodes by resolving URIs and/or running sbolSynthesizers from nodes
	private boolean loadNodeDNAComponents(String subCommand, String saveDirectory) {
		for (SynthesisNode synNode : synMap.values()) {
//			boolean resolveURIs = false;
			LinkedList<DnaComponent> dnaComps = new LinkedList<DnaComponent>();
			LinkedList<URI> sbolURIs = synNode.getSbolURIs();
			SBOLSynthesizer subSynthesizer = synNode.getSynthesizer();
			DnaComponent subSynthComp = null;
			String subSynthURI = "";
			if (subSynthesizer != null) {
				subSynthesizer.loadSbolFiles(sbolFilePaths);
//				if (subCommand.equals("save"))
					subSynthComp = subSynthesizer.saveDnaComponent(saveDirectory, true);
//				else if (subCommand.equals("export"))
//					subSynthComp = subSynthesizer.exportDnaComponent(filePath);
				if (subSynthComp != null) {
//					dnaComps.add(subSynthComp);
//					synNode.setDNAComponents(dnaComps);
					sbolURIs = subSynthesizer.getModelURIs();
					subSynthURI = subSynthComp.getURI().toString();
				} else if (sbolURIs.size() > 0) {
					Object[] options = { "OK", "Cancel" };
					int choice = JOptionPane.showOptionDialog(null, 
							"Failed to synthesize SBOL that annotates elements within submodel.  Proceed with synthesis " +
							"using SBOL that annotates submodel itself?", "Warning", JOptionPane.DEFAULT_OPTION, 
							JOptionPane.WARNING_MESSAGE, null, options, options[0]);
					if (choice != JOptionPane.OK_OPTION)
						return false;
//					else
//						resolveURIs = true;
				} else {
					JOptionPane.showMessageDialog(Gui.frame, "Failed to synthesize SBOL for submodel", 
							"SBOL Synthesis Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
					
			} 
//			else if (sbolURIs.size() > 0)
//				resolveURIs = true;
			if (sbolURIs.size() > 0) {
				for (URI sbolURI : sbolURIs) {
					if (sbolURI.toString().equals(subSynthURI))
						dnaComps.add(subSynthComp);
					else {
						DnaComponent dnaComp = aggregateCompResolver.resolve(sbolURI);
						if (dnaComp != null)
							dnaComps.add(aggregateCompResolver.resolve(sbolURI));
						else {
							JOptionPane.showMessageDialog(Gui.frame, "Component with URI " + sbolURI +
									" is not found in project SBOL files.", "DNA Component Not Found", JOptionPane.ERROR_MESSAGE);
							return false;
						}
					}
				}
				synNode.setDNAComponents(dnaComps);
			}
		}
		return true;
	}
	
	public DnaComponent synthesizeDnaComponent() {	
		// Orders list of subcomponents (to be assembled into composite component) 
		// by walking synthesis nodes
		String regex = Preferences.userRoot().get("biosim.synthesis.regex", "");
		SequenceTypeValidator validator = new SequenceTypeValidator(regex);
		Set<String> startTypes = new HashSet<String>();
		if (validator.getStartTypes().size() > 0)
			startTypes.addAll(validator.getStartTypes());
		else
			startTypes.add("promoter");
		
		LinkedList<DnaComponent> subComps = orderSubComponents(startTypes);
		if (subComps == null)
			return null;
		// Create composite component and its sequence
		DnaComponent synthComp = new DnaComponentImpl();	
		DnaSequence synthSeq = new DnaSequenceImpl();
		synthSeq.setNucleotides("");
		synthComp.setDnaSequence(synthSeq);

		// Load and set URI, display ID, name, description, and SO type for composite component and its sequence
		// URI authority and time are set for possible creation of new URIs in loadSBOLDescriptors() and addSubComponent()
		setAuthorityAndTime();
		loadSBOLDescriptors(synthComp, synthSeq);

		int position = 1;
		int addCount = 0;
		LinkedList<String> types = new LinkedList<String>();
		for (DnaComponent subComp : subComps) {
			position = addSubComponent(position, subComp, synthComp, addCount);
			if (position == -1)
				return null;
			addCount++;
			//				types.add(SBOLUtility.uriToSOTypeConverter(subComp.getTypes().iterator().next()));
			types.addAll(getLowestSequenceTypes(subComp));
		}

		if (validator != null && !validator.validateSequenceTypes(types)) {
			Object[] options = { "OK", "Cancel" };
			int choice = JOptionPane.showOptionDialog(null, 
					"Ordering of SBOL DNA components associated to SBML does not match preferred regular expression.  Proceed with synthesis?", 
					"Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
			if (choice != JOptionPane.OK_OPTION)
				return null;
		}
		return synthComp;
//		}
	}
	
	// Gets sequence ontology types for DNA components at the lowest level in the hierarchy of components
	private LinkedList<String> getLowestSequenceTypes(DnaComponent comp) {
		LinkedList<String> types = new LinkedList<String>();
		List<SequenceAnnotation> annots = comp.getAnnotations();
		if (annots.size() > 0)
			for (SequenceAnnotation anno : annots)
				types.addAll(getLowestSequenceTypes(anno.getSubComponent()));
		else
			types.add(SBOLUtility.convertURIToSOType(comp.getTypes().iterator().next()));
		return types;
	}
	
	// Recursively walks synthesis node graph and orders associated SBOL DNA components (no preference when graph branches)
	// Starts at synthesis nodes with DNA components of the SO type "promoter"
	// Stops at nodes with other promoters or previously visited nodes
	private LinkedList<DnaComponent> orderSubComponents(Set<String> startTypes) {
		LinkedHashSet<SynthesisNode> startNodes = new LinkedHashSet<SynthesisNode>();
		Set<String> startNodeIDs = new HashSet<String>();
		
		// Determines start nodes and counts total number of nodes with DNA components
		int nodesSBOL = determineStartNodes(startNodes, startNodeIDs, startTypes);

		// Walks and orders subcomponents
		LinkedList<DnaComponent> subComps = new LinkedList<DnaComponent>();
		int nodesSBOLVisited = 0;
		for (SynthesisNode startNode : startNodes) {
			Set<String> locallyVisitedNodeIDs = new HashSet<String>();
			nodesSBOLVisited = nodesSBOLVisited + walkSynthesisNodes(startNode, subComps, startNodeIDs, locallyVisitedNodeIDs);
		}
		
		// Orders leftover subcomponents that did not follow the subcomponents matching the beginning of the regex
		if (nodesSBOLVisited < nodesSBOL) {
			subComps.addAll(orderLeftoverSubComponents(new HashSet(synMap.values()), nodesSBOLVisited, nodesSBOL));
		}
		return subComps;
	}
	
	// Populates sets of start nodes and their IDs (1st URI of start node is for DNA component of SO type specified by filter
	// or for DNA component whose recursively 1st subcomponent is of the correct type)
	// Returns total number of nodes visited that had URIs (start nodes or not)
	private int determineStartNodes(LinkedHashSet<SynthesisNode> startNodes, Set<String> startNodeIDs, Set<String> startTypes) {
		int nodesSBOL = 0;
		for (SynthesisNode synNode : synMap.values()) {
			LinkedList<DnaComponent> dnaComps = synNode.getDNAComponents();
			if (dnaComps.size() > 0) {
				nodesSBOL++;
				DnaComponent startComp = dnaComps.get(0);
				if (checkStartCompType(startComp, startTypes)) {
					synNode.setVisited(true);
					startNodes.add(synNode);
					startNodeIDs.add(synNode.getID());
				}
			}
		}
		return nodesSBOL;
	}
	
	// Recursively checks whether DNA component or its 1st subcomponent has SO type in filter
	private boolean checkStartCompType(DnaComponent startComp, Set<String> startTypes) {
		for (URI uri : startComp.getTypes())
			if (startTypes.contains(SBOLUtility.convertURIToSOType(uri)))
				return true;
		if (startComp.getAnnotations().size() > 0)
			return checkStartCompType(startComp.getAnnotations().get(0).getSubComponent(), startTypes);
		else
			return false;
	}
	
	// Recursive helper method for walking synthesis node graph and loading associated SBOL DNA component URIs
	private int walkSynthesisNodes(SynthesisNode synNode, LinkedList<DnaComponent> subComps, Set<String> startNodeIDs,
			Set<String> locallyVisitedNodeIDs) {
		int nodesSBOLVisited = 0;
		LinkedList<DnaComponent> dnaComps = synNode.getDNAComponents();
		if (dnaComps.size() > 0) {
			nodesSBOLVisited++;
			subComps.addAll(dnaComps);
		}
		for (SynthesisNode nextNode : synNode.getNextNodes())
			if (!startNodeIDs.contains(nextNode.getID()) && !locallyVisitedNodeIDs.contains(nextNode.getID())) {
				nextNode.setVisited(true);
				locallyVisitedNodeIDs.add(nextNode.getID());
				nodesSBOLVisited = nodesSBOLVisited + walkSynthesisNodes(nextNode, subComps, startNodeIDs, locallyVisitedNodeIDs);
			}
		return nodesSBOLVisited;
	}
	
	// Orders leftover subcomponents that did not follow the subcomponents matching the beginning of the regex
	private LinkedList<DnaComponent> orderLeftoverSubComponents(Set<SynthesisNode> synNodes, int nodesSBOLVisited, int nodesSBOL) {
		LinkedList<DnaComponent> leftoverSubComponents = new LinkedList<DnaComponent>();
		Set<SynthesisNode> startNodes = new HashSet<SynthesisNode>();
		Set<String> startNodeIDs = new HashSet<String>();
		Set<SynthesisNode> leftoverNodes = new HashSet<SynthesisNode>();
		Set<String> nextIDs = new HashSet<String>();
		for (SynthesisNode synNode : synNodes)
			if (!synNode.isVisited()) {
				leftoverNodes.add(synNode);
				for (SynthesisNode nextNode : synNode.getNextNodes())
					nextIDs.add(nextNode.getID());
			}
		for (SynthesisNode leftover : leftoverNodes)
			if (!nextIDs.contains(leftover.getID())) {
				startNodes.add(leftover);
				startNodeIDs.add(leftover.getID());
			}
		if (startNodes.size() == 0)
			startNodes.add(leftoverNodes.iterator().next());
		for (SynthesisNode startNode : startNodes) {
			Set<String> locallyVisitedNodeIDs = new HashSet<String>();
			nodesSBOLVisited = nodesSBOLVisited + walkSynthesisNodes(startNode, leftoverSubComponents, startNodeIDs, 
					locallyVisitedNodeIDs);
		}
		if (nodesSBOLVisited < nodesSBOL)
			leftoverSubComponents.addAll(orderLeftoverSubComponents(leftoverNodes, nodesSBOLVisited, nodesSBOL));
		return leftoverSubComponents;
	}
	
	private int addSubComponent(int position, DnaComponent subComp, DnaComponent synthComp, int addCount) {	
		if (subComp.getDnaSequence() != null && subComp.getDnaSequence().getNucleotides() != null 
				&& subComp.getDnaSequence().getNucleotides().length() >= 1) {
			SequenceAnnotation annot = new SequenceAnnotationImpl();
			annot.setBioStart(position);
			position += subComp.getDnaSequence().getNucleotides().length() - 1;
			annot.setBioEnd(position);
			annot.setStrand(StrandType.POSITIVE);
			annot.setSubComponent(subComp);
			synthComp.addAnnotation(annot);
			position++;
			try {
				annot.setURI(new URI(uriAuthority + "#anno" + addCount + time + "_iBioSim"));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			synthComp.getDnaSequence().setNucleotides(synthComp.getDnaSequence().getNucleotides() + subComp.getDnaSequence().getNucleotides());
		} else {
			JOptionPane.showMessageDialog(Gui.frame, "DNA Component " + subComp.getDisplayId() + " has no DNA sequence.", 
					"Invalid DNA Sequence", JOptionPane.ERROR_MESSAGE);
			return -1;
		}	
		return position;
	}
	
	private String[] getDescriptorsFromUser() {
		String[] descriptors = new String[3];
		for (int i = 0; i < descriptors.length; i++)
			descriptors[i] = "";
		JPanel inputPanel;
		inputPanel = new JPanel(new GridLayout(4,2));
		
		JTextField modelText = new JTextField(20);
		modelText.setText(bioModel.getSBMLDocument().getModel().getId());
		modelText.setEnabled(false);
		
		JTextField idText = new JTextField(20);
		JTextField nameText = new JTextField(20);
		JTextField descripText = new JTextField(20);

	
		inputPanel.add(new JLabel("Display ID"));
		inputPanel.add(idText);
		inputPanel.add(new JLabel("Model"));
		inputPanel.add(modelText);
		inputPanel.add(new JLabel("Name"));
		inputPanel.add(nameText);
		inputPanel.add(new JLabel("Description"));
		inputPanel.add(descripText);

		String[] options = { "Ok", "Cancel" };
		int option;
		do {
			option = JOptionPane.showOptionDialog(Gui.frame, inputPanel, "Save SBOL DNA Component", 
					JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if (option != JOptionPane.YES_OPTION)
				return null;
		} while (!isSourceIdValid(idText.getText(), null));
		descriptors[0] = idText.getText();
		descriptors[1] = nameText.getText();
		descriptors[2] = descripText.getText();
		return descriptors;
	}
	
	private String[] getDescriptorsFromUser(String filePath) {
		String[] descriptors = new String[4];
		for (int i = 0; i < descriptors.length; i++)
			descriptors[i] = "";
		Object[] targets = sbolFiles.toArray();
		
		JPanel inputPanel;
		inputPanel = new JPanel(new GridLayout(5,2));
		
		JTextField modelText = new JTextField(20);
		modelText.setText(bioModel.getSBMLDocument().getModel().getId());
		modelText.setEnabled(false);
		
		JComboBox fileBox = new JComboBox(targets);
		JTextField idText = new JTextField(20);
		JTextField nameText = new JTextField(20);
		JTextField descripText = new JTextField(20);

		inputPanel.add(new JLabel("Save to File"));
		inputPanel.add(fileBox);
		inputPanel.add(new JLabel("Model"));
		inputPanel.add(modelText);
		inputPanel.add(new JLabel("Display ID"));
		inputPanel.add(idText);
		inputPanel.add(new JLabel("Name"));
		inputPanel.add(nameText);
		inputPanel.add(new JLabel("Description"));
		inputPanel.add(descripText);

		SBOLDocument localDoc = null;
		String[] options = { "Ok", "Cancel" };
		int option;
		do {
			option = JOptionPane.showOptionDialog(Gui.frame, inputPanel, "Save SBOL DNA Component", JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if (option != JOptionPane.YES_OPTION) 
				return null;
			String chosenFileId = fileBox.getSelectedItem().toString();
			if (!descriptors[3].equals(chosenFileId)) {
				descriptors[3] = chosenFileId;
				localDoc = SBOLUtility.loadSBOLFile(filePath + File.separator + chosenFileId);
			}
		} while (localDoc == null || !isSourceIdValid(idText.getText(), localDoc));
		descriptors[0] = idText.getText();
		descriptors[1] = nameText.getText();
		descriptors[2] = descripText.getText();
		return descriptors;
	}
	
	// Loads and sets URIs for newly synthesized DNA component and its sequence, 
	// Also loads and sets display ID, name, description, and SO type for for newly synthesized component
	private void loadSBOLDescriptors(DnaComponent synthComp, DnaSequence synthSeq) {
		URI existingBioSimURI = null;
		DnaComponent existingBioSimComp = null;
		boolean constructCompURI = false;
		boolean constructSeqURI = false;
		boolean placeHolderURI = false;
		if (modelURIs.size() > 0) {
			Iterator<URI> uriIterator = modelURIs.iterator();
			do {
				existingBioSimURI = uriIterator.next();
			} while (uriIterator.hasNext() && !existingBioSimURI.toString().endsWith("iBioSim") && !existingBioSimURI.toString().endsWith("iBioSimPlaceHolder"));
			if (existingBioSimURI.toString().endsWith("iBioSim")) {
				Iterator<String> fileIterator = fileResolverMap.keySet().iterator();
				do { 
					targetFile = fileIterator.next();
					Resolver<DnaComponent, URI> compResolver = fileResolverMap.get(targetFile);
					existingBioSimComp = compResolver.resolve(existingBioSimURI);
				} while (existingBioSimComp == null && fileIterator.hasNext());
				synthComp.setURI(existingBioSimURI);
				DnaSequence existingSeq = existingBioSimComp.getDnaSequence();
				if (existingSeq != null)
					synthSeq.setURI(existingSeq.getURI());
				else {
					constructSeqURI = true;
				}
			} else {
				placeHolderURI = true;
				constructCompURI = true;
				constructSeqURI = true;
			}
		} else {
			constructCompURI = true;
			constructSeqURI = true;
		}
		if (constructCompURI) {
			URI synthCompURI = null;
			try {
				synthCompURI = new URI(uriAuthority + "#comp" + time + "_iBioSim");
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			synthComp.setURI(synthCompURI);
			if (placeHolderURI) {
				int replaceIndex = modelURIs.indexOf(existingBioSimURI);
				modelURIs.remove(replaceIndex);
				modelURIs.add(replaceIndex, synthCompURI);
			}
		}
		if (constructSeqURI) {
			URI synthSeqURI = null;
			try {
				synthSeqURI = new URI(uriAuthority + "#seq" + time + "_iBioSim");
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			// Set component sequence URI
			synthSeq.setURI(synthSeqURI);
		}
		String[] descriptors = bioModel.getSBOLDescriptors();
		if (descriptors != null) {
			synthComp.setDisplayId(descriptors[0]);
			synthComp.setName(descriptors[1]);
			synthComp.setDescription(descriptors[2]);
			targetFile = descriptors[3];
		} else if (existingBioSimComp != null) {
			synthComp.setDisplayId(existingBioSimComp.getDisplayId());
			if (existingBioSimComp.getName() != null)
				synthComp.setName(existingBioSimComp.getName());
			if (existingBioSimComp.getDescription() != null)
				synthComp.setDescription(existingBioSimComp.getDescription());
		} else {
			synthComp.setDisplayId(bioModel.getSBMLDocument().getModel().getId());
			targetFile = sbolFiles.iterator().next();
		}
		synthComp.addType(SequenceOntology.type("SO_0000804"));
	}
	
	private boolean isSourceIdValid(String sourceID, SBOLDocument targetDoc) {
		if (sourceID.equals("")) {
			JOptionPane.showMessageDialog(Gui.frame, "Chosen ID is blank.", "Invalid ID", JOptionPane.ERROR_MESSAGE);
			return false;
		} else if (!Utility.isValid(sourceID, Utility.IDstring)) {
			JOptionPane.showMessageDialog(Gui.frame, "Chosen ID is not alphanumeric.", "Invalid ID", JOptionPane.ERROR_MESSAGE);
			return false;
		} else if (targetDoc != null) {
			SBOLDocumentImpl flattenedDoc = SBOLUtility.flattenDocument(targetDoc);
			if (flattenedDoc.getComponentDisplayIdResolver().resolve(sourceID) != null) {
				JOptionPane.showMessageDialog(Gui.frame, "Chosen SBOL file contains DNA component with chosen ID.", "Invalid ID", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}
	
	private LinkedList<URI> getModelURIs() {
		return modelURIs;
	}
	
	private void setAuthorityAndTime() {
		uriAuthority = Preferences.userRoot().get("biosim.synthesis.uri", "");
		Calendar now = Calendar.getInstance();
		time = "_" + now.get(Calendar.MONTH) + "_" 
				+ now.get(Calendar.DATE) + "_" + now.get(Calendar.YEAR) + "_" + now.get(Calendar.HOUR_OF_DAY) + "_" 
				+ now.get(Calendar.MINUTE) + "_" + now.get(Calendar.SECOND) + "_" + now.get(Calendar.MILLISECOND);
	}
	
}
