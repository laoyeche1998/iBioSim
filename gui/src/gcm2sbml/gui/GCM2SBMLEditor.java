package gcm2sbml.gui;

import gcm2sbml.network.GeneticNetwork;
import gcm2sbml.parser.CompatibilityFixer;
import gcm2sbml.parser.GCMFile;
import gcm2sbml.parser.GCMParser;
import gcm2sbml.util.GlobalConstants;
import gcm2sbml.util.Utility;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.sbml.libsbml.InitialAssignment;
import org.sbml.libsbml.Rule;
import org.sbml.libsbml.SBMLDocument;

import reb2sac.Reb2Sac;
import reb2sac.Reb2SacThread;
import sbmleditor.SBML_Editor;

import biomodelsim.BioSim;
import biomodelsim.Log;

import gcm2sbml.gui.grappa.GCIGrappaPanel;
import gcm2sbml.gui.modelview.movie.MovieContainer;
import gcm2sbml.gui.modelview.movie.visualizations.ColorScheme;
import gcm2sbml.gui.schematic.Schematic;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

/**
 * This is the GCM2SBMLEditor class. It takes in a gcm file and allows the user
 * to edit it by changing different fields displayed in a frame. It also
 * implements the ActionListener class, the MouseListener class, and the
 * KeyListener class which allows it to perform certain actions when buttons are
 * clicked on the frame, when one of the JList's items is double clicked, or
 * when text is entered into the model's ID.
 * 
 * @author Nam Nguyen
 */
public class GCM2SBMLEditor extends JPanel implements ActionListener, MouseListener {

	private String filename = "";

	private String gcmname = "";

	private GCMFile gcm = null;

	private boolean paramsOnly;

	private ArrayList<String> parameterChanges;

	private String paramFile, refFile, simName;

	private Reb2Sac reb2sac;
	
	private SBML_Editor sbmlParamFile;
	
	private String separator;
	
	private GCM2SBMLEditor gcmEditor;

	public GCM2SBMLEditor(String path) {
		this(path, null, null, null, false, null, null, null);
	}

	public GCM2SBMLEditor(String path, String filename, BioSim biosim, Log log, boolean paramsOnly,
			String simName, String paramFile, Reb2Sac reb2sac) {
		super();
		gcmEditor = this;
		if (File.separator.equals("\\")) {
			separator = "\\\\";
		}
		else {
			separator = File.separator;
		}
		this.biosim = biosim;
		this.log = log;
		this.path = path;
		this.paramsOnly = paramsOnly;
		this.paramFile = paramFile;
		this.simName = simName;
		this.reb2sac = reb2sac;
		sbmlParamFile = null;
		if (paramFile != null) {
			try {
				Scanner scan = new Scanner(new File(paramFile));
				if (scan.hasNextLine()) {
					refFile = scan.nextLine();
				}
				scan.close();
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(BioSim.frame, "Unable to read parameter file.", "Error",
						JOptionPane.ERROR_MESSAGE);
				refFile = "";
			}
		}
		if (paramsOnly) {
			parameterChanges = new ArrayList<String>();
			filename = refFile;
		}
		gcm = new GCMFile(path);
		if (filename != null) {
			gcm.load(path + separator + filename);
			this.filename = filename;
			this.gcmname = filename.replace(".gcm", "");
		}
		else {
			this.filename = "";
		}
		if (paramsOnly) {
			loadParams();
		}
		buildGui(this.filename, this.path);
	}

	public String getFilename() {
		return filename;
	}

	public void reload(String newName) {
		filename = newName + ".gcm";
		gcmname = newName;
		gcm.load(path + separator + newName + ".gcm");
		if (paramsOnly) {
			GCMFile refGCM = new GCMFile(path);
			refGCM.load(path + separator + refFile);
			HashMap<String, String> params = refGCM.getGlobalParameters();
			for (String key : params.keySet()) {
				gcm.setDefaultParameter(key, params.get(key));
			}
		}
		GCMNameTextField.setText(newName);
	}
	
	public void refresh() {
		Set<String> prom = gcm.getPromoters().keySet();
		ArrayList<String> proms = new ArrayList<String>();
		for (String s : prom) {
			proms.add(s);
		}
		if (paramsOnly) {
			for (String s : parameterChanges) {
				if (s.contains("/") && proms.contains(s.split("/")[0].trim())) {
					proms.remove(s.split("/")[0].trim());
					proms.add(s.split("/")[0].trim() + " Modified");
				}
			}
		}
		promoters.removeAllItem();
		promoters.addAllItem(proms);
		Set<String> spec = gcm.getSpecies().keySet();
		ArrayList<String> specs = new ArrayList<String>();
		for (String s : spec) {
			specs.add(s);
		}
		if (paramsOnly) {
			for (String s : parameterChanges) {
				if (s.contains("/") && specs.contains(s.split("/")[0].trim())) {
					specs.remove(s.split("/")[0].trim());
					specs.add(s.split("/")[0].trim() + " Modified");
				}
			}
		}
		species.removeAllItem();
		species.addAllItem(specs);
		Set<String> influe = gcm.getInfluences().keySet();
		ArrayList<String> influes = new ArrayList<String>();
		for (String s : influe) {
			influes.add(s);
		}
		if (paramsOnly) {
			for (String s : parameterChanges) {
				if (s.contains("\"") && influes.contains(s.split("\"")[1].trim())) {
					influes.remove(s.split("\"")[1].trim());
					influes.add(s.split("\"")[1].trim() + " Modified");
				}
			}
		}
		influences.removeAllItem();
		influences.addAllItem(influes);
		
		Set<String> comp = gcm.getComponents().keySet();
		ArrayList<String> comps = new ArrayList<String>();
		for (String c : comp) {
			String listVal = 	c + 
								" " + 
								gcm.getComponents().get(c).getProperty("gcm").replace(".gcm", "") + 
								" " + gcm.getComponentPortMap(c);
			comps.add(listVal);
		}
		components.removeAllItem();
		components.addAllItem(comps);	
		
		reloadParameters();
		if (paramsOnly) {
			GCMParser parser = new GCMParser(path + separator + refFile);
			GeneticNetwork network = parser.buildNetwork();
			GeneticNetwork.setRoot(path + separator);
			network.mergeSBML(path + separator + simName + separator + gcmname + ".sbml");
			reb2sac.updateSpeciesList();
		}
		
	}

	public String getGCMName() {
		return gcmname;
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			Object o = e.getSource();
			if (o instanceof PropertyList) {
				PropertyList list = (PropertyList) o;
				new EditCommand("Edit " + list.getName(), list).run();
			}
		}
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public GCMFile getGCM() {
		return gcm;
	}

	public String getSBMLFile() {
		return (String) sbmlFiles.getSelectedItem();
	}

	public void save(String command) {
		//log.addText("save");
		dirty = false;

		if (!sbmlFiles.getSelectedItem().equals(none)) {
			gcm.setSBMLFile(sbmlFiles.getSelectedItem().toString());
		}
		else {
			gcm.setSBMLFile("");
		}
		if (bioAbs.isSelected()) {
			gcm.setBioAbs(true);
		}
		else {
			gcm.setBioAbs(false);
		}
		GeneticNetwork.setRoot(path + separator);

		if (command.contains("GCM as")) {
			String newName = JOptionPane.showInputDialog(BioSim.frame, "Enter GCM name:", "GCM Name",
					JOptionPane.PLAIN_MESSAGE);
			if (newName == null) {
				return;
			}
			if (newName.contains(".gcm")) {
				newName = newName.replace(".gcm", "");
			}
			saveAs(newName);
			return;
		}

		// Write out species and influences to a gcm file
		gcm.save(path + separator + gcmname + ".gcm");
		log.addText("Saving GCM file:\n" + path + separator + gcmname + ".gcm\n");

		if (command.contains("template")) {
			GCMParser parser = new GCMParser(path + separator + gcmname + ".gcm");
			try {
				parser.buildTopLevelNetwork(null);
			}
			catch (IllegalStateException e) {
				JOptionPane.showMessageDialog(BioSim.frame, e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			GeneticNetwork network = new GeneticNetwork();

			String templateName = JOptionPane.showInputDialog(BioSim.frame,
					"Enter SBML template name:", "SBML Template Name", JOptionPane.PLAIN_MESSAGE);
			if (templateName != null) {
				if (!templateName.contains(".sbml") && !templateName.contains(".xml")) {
					templateName = templateName + ".xml";
				}
				if (new File(path + separator + templateName).exists()) {
					int value = JOptionPane.showOptionDialog(BioSim.frame, templateName
							+ " already exists.  Overwrite file?", "Save file",
							JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
							options[0]);
					if (value == JOptionPane.YES_OPTION) {
						network.buildTemplate(parser.getSpecies(), parser.getPromoters(), gcmname
								+ ".gcm", path + separator + templateName);
						log.addText("Saving GCM file as SBML template:\n" + path + separator
								+ templateName + "\n");
						biosim.addToTree(templateName);
						biosim.updateOpenSBML(templateName);
					}
					else {
						// Do nothing
					}
				}
				else {
					network.buildTemplate(parser.getSpecies(), parser.getPromoters(), gcmname
							+ ".gcm", path + separator + templateName);
					log.addText("Saving GCM file as SBML template:\n" + path + separator
							+ templateName + "\n");
					biosim.addToTree(templateName);
				}
			}
		}
		else if (command.contains("LHPN")) {
			String lpnName = JOptionPane.showInputDialog(BioSim.frame,
					"Enter LPN name:", "LPN Name", JOptionPane.PLAIN_MESSAGE);
			if (!lpnName.trim().contains(".lpn")) {
				lpnName = lpnName.trim() + ".lpn";
			}
			if (new File(path + separator + lpnName).exists()) {
				int value = JOptionPane.showOptionDialog(BioSim.frame, lpnName
						+ " already exists.  Overwrite file?", "Save file", JOptionPane.YES_NO_OPTION,
						JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
				if (value == JOptionPane.YES_OPTION) {
					gcm.createLogicalModel(path + separator + lpnName, log, biosim, lpnName);
				}
				else {
					// Do nothing
				}
			}
			else {
				gcm.createLogicalModel(path + separator + lpnName, log, biosim, lpnName);
			}
		}
		else if (command.contains("SBML")) {
			// Then read in the file with the GCMParser
			GCMParser parser = new GCMParser(path + separator + gcmname + ".gcm");
			GeneticNetwork network = null;
			try {
				network = parser.buildNetwork();
			}
			catch (IllegalStateException e) {
				JOptionPane.showMessageDialog(BioSim.frame, e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			network.loadProperties(gcm);
			// Finally, output to a file
			if (new File(path + separator + gcmname + ".xml").exists()) {
				int value = JOptionPane.showOptionDialog(BioSim.frame, gcmname
						+ ".xml already exists.  Overwrite file?", "Save file", JOptionPane.YES_NO_OPTION,
						JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
				if (value == JOptionPane.YES_OPTION) {
					network.mergeSBML(path + separator + gcmname + ".xml");
					log.addText("Saving GCM file as SBML file:\n" + path + separator + gcmname
							+ ".xml\n");
					biosim.addToTree(gcmname + ".xml");
					biosim.updateOpenSBML(gcmname + ".xml");
				}
				else {
					// Do nothing
				}
			}
			else {
				network.mergeSBML(path + separator + gcmname + ".xml");
				log
						.addText("Saving GCM file as SBML file:\n" + path + separator + gcmname
								+ ".xml\n");
				biosim.addToTree(gcmname + ".xml");
			}
		}
		biosim.updateViews(gcmname + ".gcm");

	}

	public void saveAs(String newName) {
		if (new File(path + separator + newName + ".gcm").exists()) {
			int value = JOptionPane.showOptionDialog(BioSim.frame, newName
					+ " already exists.  Overwrite file?", "Save file", JOptionPane.YES_NO_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if (value == JOptionPane.YES_OPTION) {
				gcm.save(path + separator + newName + ".gcm");
				log.addText("Saving GCM file as:\n" + path + separator + newName + ".gcm\n");
				biosim.addToTree(newName + ".gcm");
			}
			else {
				// Do nothing
				return;
			}
		}
		else {
			gcm.save(path + separator + newName + ".gcm");
			log.addText("Saving GCM file as:\n" + path + separator + newName + ".gcm\n");
			biosim.addToTree(newName + ".gcm");
		}
		biosim.updateTabName(gcmname + ".gcm", newName + ".gcm");
		reload(newName);
	}

	public void saveParams(boolean run, String stem) {
		ArrayList<String> sweepThese1 = new ArrayList<String>();
		ArrayList<ArrayList<Double>> sweep1 = new ArrayList<ArrayList<Double>>();
		ArrayList<String> sweepThese2 = new ArrayList<String>();
		ArrayList<ArrayList<Double>> sweep2 = new ArrayList<ArrayList<Double>>();
		for (String s : parameterChanges) {
			if (s.split(" ")[s.split(" ").length - 1].startsWith("(")) {
				if ((s.split(" ")[s.split(" ").length - 1]).split(",")[3].replace(")", "").trim().equals(
						"1")) {
					sweepThese1.add(s.split(" ")[0]);
					double start = Double.parseDouble((s.split(" ")[s.split(" ").length - 1]).split(",")[0]
							.substring(1).trim());
					double stop = Double.parseDouble((s.split(" ")[s.split(" ").length - 1]).split(",")[1]
							.trim());
					double step = Double.parseDouble((s.split(" ")[s.split(" ").length - 1]).split(",")[2]
							.trim());
					if (step <= 0) {
						JOptionPane.showMessageDialog(BioSim.frame, "Step must be a positive number."
								+ "\nDefaulting to a step of 1.", "Error", JOptionPane.ERROR_MESSAGE);
						step = 1;
					}
					ArrayList<Double> add = new ArrayList<Double>();
					for (double i = start; i <= stop; i += step) {
						add.add(i);
					}
					sweep1.add(add);
				}
				else {
					sweepThese2.add(s.split(" ")[0]);
					double start = Double.parseDouble((s.split(" ")[s.split(" ").length - 1]).split(",")[0]
							.substring(1).trim());
					double stop = Double.parseDouble((s.split(" ")[s.split(" ").length - 1]).split(",")[1]
							.trim());
					double step = Double.parseDouble((s.split(" ")[s.split(" ").length - 1]).split(",")[2]
							.trim());
					if (step <= 0) {
						JOptionPane.showMessageDialog(BioSim.frame, "Step must be a positive number."
								+ "\nDefaulting to a step of 1.", "Error", JOptionPane.ERROR_MESSAGE);
						step = 1;
					}
					ArrayList<Double> add = new ArrayList<Double>();
					for (double i = start; i <= stop; i += step) {
						add.add(i);
					}
					sweep2.add(add);
				}
			}
		}
		if (sweepThese1.size() > 0) {
			int max = 0;
			for (ArrayList<Double> d : sweep1) {
				max = Math.max(max, d.size());
			}
			for (int j = 0; j < max; j++) {
				String sweep = "";
				for (int i = 0; i < sweepThese1.size(); i++) {
					int k = j;
					if (k >= sweep1.get(i).size()) {
						k = sweep1.get(i).size() - 1;
					}
					if (sweep.equals("")) {
						sweep += sweepThese1.get(i) + "=" + sweep1.get(i).get(k);
					}
					else {
						sweep += "_" + sweepThese1.get(i) + "=" + sweep1.get(i).get(k);
					}
				}
				if (sweepThese2.size() > 0) {
					int max2 = 0;
					for (ArrayList<Double> d : sweep2) {
						max2 = Math.max(max2, d.size());
					}
					for (int l = 0; l < max2; l++) {
						String sweepTwo = sweep;
						for (int i = 0; i < sweepThese2.size(); i++) {
							int k = l;
							if (k >= sweep2.get(i).size()) {
								k = sweep2.get(i).size() - 1;
							}
							if (sweepTwo.equals("")) {
								sweepTwo += sweepThese2.get(i) + "=" + sweep2.get(i).get(k);
							}
							else {
								sweepTwo += "_" + sweepThese2.get(i) + "=" + sweep2.get(i).get(k);
							}
						}
						new File(path + separator + simName + separator + stem
								+ sweepTwo.replace("/", "-")).mkdir();
						createSBML(stem, sweepTwo);
						if (run) {
							new Reb2SacThread(reb2sac).start(stem + sweepTwo.replace("/", "-"));
							reb2sac.emptyFrames();
						}
					}
				}
				else {
					new File(path + separator + simName + separator + stem
							+ sweep.replace("/", "-")).mkdir();
					createSBML(stem, sweep);
					if (run) {
						new Reb2SacThread(reb2sac).start(stem + sweep.replace("/", "-"));
						reb2sac.emptyFrames();
					}
				}
			}
		}
		else {
			if (!stem.equals("")) {
				new File(path + separator + simName + separator + stem).mkdir();
			}
			createSBML(stem, ".");
			if (run) {
				if (!stem.equals("")) {
					new Reb2SacThread(reb2sac).start(stem);
				}
				else {
					new Reb2SacThread(reb2sac).start(".");
				}
				reb2sac.emptyFrames();
			}
		}
	}

	public void loadParams() {
		if (paramsOnly) {
			HashMap<String, Properties> elements = gcm.getSpecies();
			for (String key : elements.keySet()) {
				ArrayList<Object> remove = new ArrayList<Object>();
				for (Object prop : elements.get(key).keySet()) {
					if (!prop.equals(GlobalConstants.NAME) && !prop.equals(GlobalConstants.ID) && !prop.equals(GlobalConstants.TYPE) &&
							(!((String)prop).startsWith("graph"))) {
						remove.add(prop);
					}
				}
				for (Object prop : remove) {
					elements.get(key).remove(prop);
				}
				
			}
			elements = gcm.getInfluences();
			for (String key : elements.keySet()) {
				ArrayList<Object> remove = new ArrayList<Object>();
				for (Object prop : elements.get(key).keySet()) {
					if (!prop.equals(GlobalConstants.NAME) && !prop.equals(GlobalConstants.PROMOTER) && !prop.equals(GlobalConstants.BIO) && !prop.equals(GlobalConstants.TYPE) &&
							(!((String)prop).startsWith("graph"))) {
						remove.add(prop);
					}
				}
				for (Object prop : remove) {
					elements.get(key).remove(prop);
				}
			}
			elements = gcm.getPromoters();
			for (String key : elements.keySet()) {
				ArrayList<Object> remove = new ArrayList<Object>();
				for (Object prop : elements.get(key).keySet()) {
					if (!prop.equals(GlobalConstants.NAME) && !prop.equals(GlobalConstants.ID) &&
							(!((String)prop).startsWith("graph"))) {
						remove.add(prop);
					}
				}
				for (Object prop : remove) {
					elements.get(key).remove(prop);
				}
			}
			HashMap<String, String> params = gcm.getGlobalParameters();
			ArrayList<Object> remove = new ArrayList<Object>();
			for (String key : params.keySet()) {
				remove.add(key);
			}
			for (Object prop : remove) {
				params.remove(prop);
			}
			GCMFile refGCM = new GCMFile(path);
			refGCM.load(path + separator + refFile);
			params = refGCM.getGlobalParameters();
			for (String key : params.keySet()) {
				gcm.setDefaultParameter(key, params.get(key));
			}
			try {
				Scanner scan = new Scanner(new File(paramFile));
				if (scan.hasNextLine()) {
					scan.nextLine();
				}
				while (scan.hasNextLine()) {
					String s = scan.nextLine();
					if (!s.trim().equals("")) {
						boolean added = false;
						for (int i = 0; i < parameterChanges.size(); i ++) {
							if (parameterChanges.get(i).split(" ")[0].equals(s.split(" ")[0])) {
								parameterChanges.set(i, s);
								added = true;
							}
						}
						if (!added) {
							parameterChanges.add(s);
						}
					}
					else {
						break;
					}
				}
				scan.close();
			}
			catch (Exception e) {
			}
			for (String update : parameterChanges) {
				String id;
				if (update.contains("/")) {
					id = update.split("/")[0];
					id = id.replace("\"", "");
					String prop = CompatibilityFixer.convertSBMLName(update.split("/")[1].substring(0, update.split("/")[1].indexOf(" ")).trim());
					String value = update.split(" ")[update.split(" ").length - 1].trim();
					Properties props = null;
					if(gcm.getSpecies().containsKey(id)) {
						props = gcm.getSpecies().get(id);
					}
					else if(gcm.getPromoters().containsKey(id)) {
						props = gcm.getPromoters().get(id);
					}
					else if(gcm.getInfluences().containsKey(id)) {
						props = gcm.getInfluences().get(id);
					}
					if (props != null) {
						props.put(prop, value);
					}
				}
				else {
					id = update.split(" ")[0];
					String value = update.split(" ")[1].trim();
					gcm.setParameter(CompatibilityFixer.convertSBMLName(id), value);
				}
			}
		}
	}

	public void createSBML(String stem, String direct) {
		GCMFile gcm = new GCMFile(path);
		gcm.load(path + separator + refFile);
		HashMap<String, Properties> elements = this.gcm.getSpecies();
		for (String key : elements.keySet()) {
			for (Object prop : elements.get(key).keySet()) {
				if (!prop.equals(GlobalConstants.NAME) && !prop.equals(GlobalConstants.ID) && !prop.equals(GlobalConstants.TYPE)) {
					if (gcm.getSpecies().containsKey(key)) {
						gcm.getSpecies().get(key).put(prop, elements.get(key).get(prop));
					}
				}
			}			
		}
		elements = this.gcm.getInfluences();
		for (String key : elements.keySet()) {
			for (Object prop : elements.get(key).keySet()) {
				if (!prop.equals(GlobalConstants.NAME) && !prop.equals(GlobalConstants.PROMOTER) && !prop.equals(GlobalConstants.BIO) && !prop.equals(GlobalConstants.TYPE)) {
					if (gcm.getInfluences().containsKey(key)) {
						gcm.getInfluences().get(key).put(prop, elements.get(key).get(prop));
					}
				}
			}
		}
		elements = this.gcm.getPromoters();
		for (String key : elements.keySet()) {
			for (Object prop : elements.get(key).keySet()) {
				if (!prop.equals(GlobalConstants.NAME) && !prop.equals(GlobalConstants.ID)) {
					if (gcm.getPromoters().containsKey(key)) {
						gcm.getPromoters().get(key).put(prop, elements.get(key).get(prop));
					}
				}
			}
		}
		HashMap<String, String> params = this.gcm.getGlobalParameters();
		ArrayList<Object> remove = new ArrayList<Object>();
		for (String key : params.keySet()) {
			gcm.setParameter(key, params.get(key));
			remove.add(key);
		}
		try {
			FileOutputStream out = new FileOutputStream(new File(paramFile));
			out.write((refFile + "\n").getBytes());
			for (String s : parameterChanges) {
				if (!s.trim().equals("")) {
					out.write((s + "\n").getBytes());
				}
			}
			out.write(("\n").getBytes());
			if (sbmlParamFile != null) {
				for (String s : sbmlParamFile.getElementChanges()) {
					out.write((s + "\n").getBytes());
				}
			}
			out.close();
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(BioSim.frame, "Unable to save parameter file.",
					"Error Saving File", JOptionPane.ERROR_MESSAGE);
		}
		try {
			if (!direct.equals(".")) {
				String[] d = direct.split("_");
				ArrayList<String> dd = new ArrayList<String>();
				for (int i = 0; i < d.length; i++) {
					if (!d[i].contains("=")) {
						String di = d[i];
						while (!d[i].contains("=")) {
							i++;
							di += "_" + d[i];
						}
						dd.add(di);
					}
					else {
						dd.add(d[i]);
					}
				}
				for (String di : dd) {
					if (di.contains("/")) {
						if (gcm.getPromoters().containsKey(di.split("=")[0].split("/")[0])) {
							Properties promoterProps = gcm.getPromoters().get(di.split("=")[0].split("/")[0]);
							promoterProps.put(CompatibilityFixer.convertSBMLName(di.split("=")[0].split("/")[1]),
									di.split("=")[1]);
						}
						if (gcm.getSpecies().containsKey(di.split("=")[0].split("/")[0])) {
							Properties speciesProps = gcm.getSpecies().get(di.split("=")[0].split("/")[0]);
							speciesProps.put(CompatibilityFixer.convertSBMLName(di.split("=")[0].split("/")[1]),
									di.split("=")[1]);
						}
						if (gcm.getInfluences().containsKey(di.split("=")[0].split("/")[0].substring(1))) {
							Properties influenceProps = gcm.getInfluences().get(
									di.split("=")[0].split("/")[0].substring(1));
							influenceProps.put(CompatibilityFixer.convertSBMLName(di.split("=")[0].split("/")[1])
									.replace("\"", ""), di.split("=")[1]);
						}
					}
					else {
						if (gcm.getGlobalParameters().containsKey(CompatibilityFixer.convertSBMLName(di.split("=")[0]))) {
							gcm.getGlobalParameters().put(CompatibilityFixer.convertSBMLName(di.split("=")[0]), di.split("=")[1]);
						}
						if (gcm.getParameters().containsKey(CompatibilityFixer.convertSBMLName(di.split("=")[0]))) {
							gcm.getParameters().put(CompatibilityFixer.convertSBMLName(di.split("=")[0]), di.split("=")[1]);
						}
					}
				}
			}
			direct = direct.replace("/", "-");
			if (direct.equals(".") && !stem.equals("")) {
				direct = "";
			}
			gcm.save(path + separator + gcmname + ".gcm.temp");
			GCMParser parser = new GCMParser(path + separator + gcmname + ".gcm.temp");
			GeneticNetwork network = null;
			try {
				network = parser.buildNetwork();
			}
			catch (IllegalStateException e) {
				JOptionPane.showMessageDialog(BioSim.frame, e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			network.loadProperties(gcm);
			if (!getSBMLFile().equals(none)) {
				SBMLDocument d = BioSim.readSBML(path + separator + getSBMLFile());
				for (String s : sbmlParamFile.getElementChanges()) {
					for (long i = d.getModel().getNumInitialAssignments() - 1; i >= 0; i--) {
						if (s.contains("=")) {
							String formula = sbmlParamFile.myFormulaToString(((InitialAssignment) d.getModel()
									.getListOfInitialAssignments().get(i)).getMath());
							String sFormula = s.substring(s.indexOf('=') + 1).trim();
							sFormula = sbmlParamFile.myFormulaToString(sbmlParamFile.myParseFormula(sFormula));
							sFormula = s.substring(0, s.indexOf('=') + 1) + " " + sFormula;
							if ((((InitialAssignment) d.getModel().getListOfInitialAssignments().get(i))
									.getSymbol()
									+ " = " + formula).equals(sFormula)) {
								d.getModel().getListOfInitialAssignments().remove(i);
							}
						}
					}
					for (long i = d.getModel().getNumConstraints() - 1; i >= 0; i--) {
						if (d.getModel().getListOfConstraints().get(i).getMetaId().equals(s)) {
							d.getModel().getListOfConstraints().remove(i);
						}
					}
					for (long i = d.getModel().getNumEvents() - 1; i >= 0; i--) {
						if (d.getModel().getListOfEvents().get(i).getId().equals(s)) {
							d.getModel().getListOfEvents().remove(i);
						}
					}
					for (long i = d.getModel().getNumRules() - 1; i >= 0; i--) {
						if (s.contains("=")) {
							String formula = sbmlParamFile.myFormulaToString(((Rule) d.getModel().getListOfRules()
									.get(i)).getMath());
							String sFormula = s.substring(s.indexOf('=') + 1).trim();
							sFormula = sbmlParamFile.myFormulaToString(sbmlParamFile.myParseFormula(sFormula));
							sFormula = s.substring(0, s.indexOf('=') + 1) + " " + sFormula;
							if ((((Rule) d.getModel().getListOfRules().get(i)).getVariable() + " = " + formula)
									.equals(sFormula)) {
								d.getModel().getListOfRules().remove(i);
							}
						}
					}
				}
				network.mergeSBML(path + separator + simName + separator + stem + direct
						+ separator + gcmname + ".sbml", d);
			}
			else {
				network.mergeSBML(path + separator + simName + separator + stem + direct
						+ separator + gcmname + ".sbml");
			}
		}
		catch (Exception e1) {
			JOptionPane.showMessageDialog(BioSim.frame, "Unable to create sbml file.",
					"Error Creating File", JOptionPane.ERROR_MESSAGE);
		}
	}

	public String getRefFile() {
		return refFile;
	}

	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o instanceof Runnable) {
			((Runnable) o).run();
		}
		else if (o instanceof JComboBox && !lock
				&& !gcm.getSBMLFile().equals(sbmlFiles.getSelectedItem())) {
			dirty = true;
		}
		else if (o instanceof JCheckBox && !lock
				&& (gcm.getBioAbs() != bioAbs.isSelected())) {
			dirty = true;
		}
		// System.out.println(o);
	}

	public synchronized void lock() {
		lock = true;
	}

	public synchronized void unlock() {
		lock = false;
	}

	private void buildGui(String filename, String path) {
		JPanel mainPanelNorth = new JPanel();
		JPanel mainPanelCenter = new JPanel(new BorderLayout());
		JPanel mainPanelCenterUp = new JPanel();
		JPanel mainPanelCenterCenter = new JPanel(new GridLayout(2, 2));
		//JPanel mainPanelCenterDown = new JPanel(new BorderLayout());
		JPanel tabPanel = new JPanel(new BorderLayout());
		mainPanelCenter.add(mainPanelCenterUp, BorderLayout.NORTH);
		mainPanelCenter.add(mainPanelCenterCenter, BorderLayout.CENTER);
		// mainPanelCenter.add(mainPanelCenterDown, BorderLayout.SOUTH);
		GCMNameTextField = new JTextField(filename.replace(".gcm", ""), 15);
		GCMNameTextField.setEditable(false);
		GCMNameTextField.addActionListener(this);
		/*
		JLabel GCMNameLabel = new JLabel("GCM Id:");
		mainPanelNorth.add(GCMNameLabel);
		mainPanelNorth.add(GCMNameTextField);
		 */
		
		JLabel sbmlFileLabel = new JLabel("SBML File:");
		sbmlFiles = new JComboBox();
		sbmlFiles.addActionListener(this);
		if (paramsOnly) {
			sbmlFileLabel.setEnabled(false);
			sbmlFiles.setEnabled(false);
		}
		reloadFiles();
		mainPanelNorth.add(sbmlFileLabel);
		mainPanelNorth.add(sbmlFiles);

		JLabel bioAbsLabel = new JLabel("Complex abstraction:");
		bioAbs = new JCheckBox();
		bioAbs.addActionListener(this);
		if (paramsOnly) {
			bioAbsLabel.setEnabled(false);
			bioAbs.setEnabled(false);
		}
		mainPanelNorth.add(bioAbsLabel);
		mainPanelNorth.add(bioAbs);
		if (gcm.getBioAbs()) {
			bioAbs.setSelected(true);
		}

		/*JLabel dimAbsLabel = new JLabel("Dimerization abstraction:");
		dimAbs = new JCheckBox();
		dimAbs.addActionListener(this);
		if (paramsOnly) {
			dimAbsLabel.setEnabled(false);
			dimAbs.setEnabled(false);
		}
		mainPanelNorth.add(dimAbsLabel);
		mainPanelNorth.add(dimAbs);
		if (gcm.getDimAbs()) {
			dimAbs.setSelected(true);
		}*/

		// create the grappa panel
		//GCIGrappaPanel grappaPanel = new GCIGrappaPanel();
		
		// create the modelview2 (jgraph) panel
		Schematic schematic = new Schematic(gcm, biosim, this, true, null);
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setLayout(new BorderLayout());
		tabPanel.add(mainPanelNorth, "North");
		mainPanel.add(mainPanelCenter, "Center");
		JTabbedPane tab = new JTabbedPane();
		tab.addTab("Schematic", schematic);
		tab.addTab("Parameters/Properties", tabPanel);
		tab.addTab("Main Elements", mainPanel);
 		//tab.addTab("Model View", grappaPanel);
		setLayout(new BorderLayout());
		if (paramsOnly) {
			add(tabPanel, BorderLayout.CENTER);
		}
		else {
			add(tab, BorderLayout.CENTER);
		}
		//add(mainPanelCenterDown, BorderLayout.SOUTH);
		
		// When the Graphical View panel gets clicked on, tell it to display itself.
		tab.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				JTabbedPane selectedTab = (JTabbedPane)(e.getSource());
				JPanel selectedPanel = (JPanel)selectedTab.getComponent(selectedTab.getSelectedIndex());
				String className = selectedPanel.getClass().getName();
				// The old Schematic (grappa panel) 
				if(className.indexOf("GCIGrappaPanel") > 0){
					((GCIGrappaPanel)selectedPanel).display(gcm.saveToBuffer(false, false));
				}
				// The new Schematic
				else if(className.indexOf("Schematic") >= 0){
					((Schematic)selectedPanel).display();
				}
			}
		});

		promoters = new PropertyList("Promoter List");
		EditButton addInit = new EditButton("Add Promoter", promoters);
		RemoveButton removeInit = new RemoveButton("Remove Promoter", promoters);
		if (paramsOnly) {
			addInit.setEnabled(false);
			removeInit.setEnabled(false);
		}
		EditButton editInit = new EditButton("Edit Promoter", promoters);
		if (paramsOnly) {
			Set<String> prom = gcm.getPromoters().keySet();
			ArrayList<String> proms = new ArrayList<String>();
			for (String s : prom) {
				proms.add(s);
			}
			for (String s : parameterChanges) {
				if (s.contains("/") && proms.contains(s.split("/")[0].trim())) {
					proms.remove(s.split("/")[0].trim());
					proms.add(s.split("/")[0].trim() + " Modified");
				}
			}
			promoters.addAllItem(proms);
		}
		else {
			promoters.addAllItem(gcm.getPromoters().keySet());
		}

		JPanel initPanel = Utility.createPanel(this, "Promoters", promoters, addInit, removeInit,
				editInit);
		mainPanelCenterCenter.add(initPanel);

		species = new PropertyList("Species List");
		addInit = new EditButton("Add Species", species);
		removeInit = new RemoveButton("Remove Species", species);
		if (paramsOnly) {
			addInit.setEnabled(false);
			removeInit.setEnabled(false);
		}
		editInit = new EditButton("Edit Species", species);
		if (paramsOnly) {
			Set<String> spec = gcm.getSpecies().keySet();
			ArrayList<String> specs = new ArrayList<String>();
			for (String s : spec) {
				specs.add(s);
			}
			for (String s : parameterChanges) {
				if (s.contains("/") && specs.contains(s.split("/")[0].trim())) {
					specs.remove(s.split("/")[0].trim());
					specs.add(s.split("/")[0].trim() + " Modified");
				}
			}
			species.addAllItem(specs);
		}
		else {
			species.addAllItem(gcm.getSpecies().keySet());
		}

		initPanel = Utility.createPanel(this, "Species", species, addInit, removeInit, editInit);
		mainPanelCenterCenter.add(initPanel);

		influences = new PropertyList("Influence List");
		addInit = new EditButton("Add Influence", influences);
		removeInit = new RemoveButton("Remove Influence", influences);
		if (paramsOnly) {
			addInit.setEnabled(false);
			removeInit.setEnabled(false);
		}
		editInit = new EditButton("Edit Influence", influences);
		if (paramsOnly) {
			Set<String> influe = gcm.getInfluences().keySet();
			ArrayList<String> influes = new ArrayList<String>();
			for (String s : influe) {
				influes.add(s);
			}
			for (String s : parameterChanges) {
				if (s.contains("\"") && influes.contains(s.split("\"")[1].trim())) {
					influes.remove(s.split("\"")[1].trim());
					influes.add(s.split("\"")[1].trim() + " Modified");
				}
			}
			influences.addAllItem(influes);
		}
		else {
			influences.addAllItem(gcm.getInfluences().keySet());
		}

		initPanel = Utility.createPanel(this, "Influences", influences, addInit, removeInit, editInit);
		mainPanelCenterCenter.add(initPanel);

		components = new PropertyList("Component List");
		addInit = new EditButton("Add Component", components);
		removeInit = new RemoveButton("Remove Component", components);
		editInit = new EditButton("Edit Component", components);
		for (String s : gcm.getComponents().keySet()) {
			if (gcm.getComponents().get(s).getProperty("gcm") != null) {
				components.addItem(s + " "
						+ gcm.getComponents().get(s).getProperty("gcm").replace(".gcm", "") + " "
						+ gcm.getComponentPortMap(s));
			}
		}
		initPanel = Utility.createPanel(this, "Components", components, addInit, removeInit, editInit);
		mainPanelCenterCenter.add(initPanel);
		
		parameters = new PropertyList("Parameter List");
		editInit = new EditButton("Edit Parameter", parameters);
		// parameters.addAllItem(gcm.getParameters().keySet());
		parameters.addAllItem(generateParameters());
		initPanel = Utility.createPanel(this, "Parameters", parameters, null, null, editInit);
		tabPanel.add(initPanel, "Center");
		
		conditions = new PropertyList("Property List");
		addInit = new EditButton("Add Property", conditions);
		removeInit = new RemoveButton("Remove Property", conditions);
		editInit = new EditButton("Edit Property", conditions);
		if (paramsOnly) {
			addInit.setEnabled(false);
			removeInit.setEnabled(false);
			editInit.setEnabled(false);
		}
		for (String s : gcm.getConditions()) {
			conditions.addItem(s);
		}
		initPanel = Utility.createPanel(this, "Properties", conditions, addInit, removeInit, editInit);
		tabPanel.add(initPanel, "South");
		
	}

	public void reloadFiles() {
		lock();
		// sbmlFiles.removeAll();
		String[] sbmlList = Utility.getFiles(path, ".sbml");
		String[] xmlList = Utility.getFiles(path, ".xml");

		String[] temp = new String[sbmlList.length + xmlList.length];
		System.arraycopy(sbmlList, 0, temp, 0, sbmlList.length);
		System.arraycopy(xmlList, 0, temp, sbmlList.length, xmlList.length);

		Arrays.sort(temp, String.CASE_INSENSITIVE_ORDER);
		String[] allList = new String[temp.length + 1];
		System.arraycopy(temp, 0, allList, 1, temp.length);
		allList[0] = none;

		DefaultComboBoxModel model = new DefaultComboBoxModel(allList);

		sbmlFiles.setModel(model);

		sbmlFiles.setSelectedItem(none);

		sbmlFiles.setSelectedItem(gcm.getSBMLFile());
		if (!gcm.getSBMLFile().equals("") && sbmlFiles.getSelectedItem().equals(none)) {
			Utility.createErrorMessage("Warning: Missing File", "Unable to find SBML file "
					+ gcm.getSBMLFile() + ".  Setting default SBML file to none");
			gcm.setSBMLFile("");
		}
		unlock();
	}

	public void reloadParameters() {
		parameters.removeAllItem();
		parameters.addAllItem(generateParameters());
	}

	private Set<String> generateParameters() {
		HashSet<String> results = new HashSet<String>();
		if (paramsOnly) {
			HashMap<String, String> params = gcm.getGlobalParameters();
			ArrayList<Object> remove = new ArrayList<Object>();
			for (String key : params.keySet()) {
				remove.add(key);
			}
			for (Object prop : remove) {
				params.remove(prop);
			}
			for (String update : parameterChanges) {
				String id;
				if (!update.contains("/")) {
					id = update.split(" ")[0];
					String value = update.split(" ")[1].trim();
					gcm.setParameter(CompatibilityFixer.convertSBMLName(id), value);
				}
			}
		}
		for (String s : gcm.getParameters().keySet()) {
			if (!s.equals(GlobalConstants.KBIO_STRING) && !s.equals(GlobalConstants.KASSOCIATION_STRING)
					&& !s.equals(GlobalConstants.MAX_DIMER_STRING)) {
				if (gcm.getGlobalParameters().containsKey(s)) {
					if (gcm.getParameter(s).contains("(")) {
						results.add(CompatibilityFixer.getGuiName(s) + " ("
								+ CompatibilityFixer.getSBMLName(s) + "), Sweep, "
								+ gcm.getParameter(s));
					}
					else {
						if (paramsOnly) {
							results.add(CompatibilityFixer.getGuiName(s) + " ("
									+ CompatibilityFixer.getSBMLName(s) + "), Modified, "
									+ gcm.getParameter(s));
						}
						else {
							results.add(CompatibilityFixer.getGuiName(s) + " ("
									+ CompatibilityFixer.getSBMLName(s) + "), Custom, "
									+ gcm.getParameter(s));
						}
					}
				}
				else {
					if (paramsOnly) {
						GCMFile refGCM = new GCMFile(path);
						refGCM.load(path + separator + refFile);
						if (refGCM.getGlobalParameters().containsKey(s)) {
							results.add(CompatibilityFixer.getGuiName(s) + " ("
									+ CompatibilityFixer.getSBMLName(s) + "), Custom, "
									+ gcm.getParameter(s));
						}
						else {
							results.add(CompatibilityFixer.getGuiName(s) + " ("
									+ CompatibilityFixer.getSBMLName(s) + "), Default, "
									+ gcm.getParameter(s));
						}
					}
					else {
						results.add(CompatibilityFixer.getGuiName(s) + " ("
								+ CompatibilityFixer.getSBMLName(s) + "), Default, "
								+ gcm.getParameter(s));
					}
				}
			}
		}
		return results;
	}

	// Internal private classes used only by the gui
	private class SaveButton extends AbstractRunnableNamedButton {
		public SaveButton(String name, JTextField fieldNameField) {
			super(name);
			this.fieldNameField = fieldNameField;
		}

		public void run() {
			save(getName());
		}

		private JTextField fieldNameField = null;
	}

	private class RemoveButton extends AbstractRunnableNamedButton {
		public RemoveButton(String name, PropertyList list) {
			super(name);
			this.list = list;
		}

		public void run() {
			//dirty = true;
			if (getName().contains("Influence")) {
				String name = null;
				if (list.getSelectedValue() != null) {
					name = list.getSelectedValue().toString();
					if (gcm.removeInfluenceCheck(name)) {
						gcm.removeInfluence(name);
						list.removeItem(name);
					}
				}
			}
			else if (getName().contains("Species")) {
				String name = null;
				if (list.getSelectedValue() != null) {
					name = list.getSelectedValue().toString();
					if (gcm.removeSpeciesCheck(name)) {
						gcm.removeSpecies(name);
						list.removeItem(name);
					}
					else {
						JOptionPane.showMessageDialog(BioSim.frame, "Cannot remove species " + name
								+ " because it is currently in other reactions and/or components.");
					}
				}
			}
			else if (getName().contains("Promoter")) {
				String name = null;
				if (list.getSelectedValue() != null) {
					name = list.getSelectedValue().toString();
					if (gcm.removePromoterCheck(name)) {
						gcm.removePromoter(name);
						list.removeItem(name);
					}
					else {
						JOptionPane.showMessageDialog(BioSim.frame, "Cannot remove promoter " + name
								+ " because it is currently in other reactions");
					}
				}
			}
			else if (getName().contains("Property")) {
				String name = null;
				if (list.getSelectedValue() != null) {
					name = list.getSelectedValue().toString();
					gcm.removeCondition(name);
					list.removeItem(name);
				}
			}
			else if (getName().contains("Component")) {
				String name = null;
				if (list.getSelectedValue() != null) {
					name = list.getSelectedValue().toString();
					String comp = name.split(" ")[0];
					if (gcm.removeComponentCheck(comp)) {
						gcm.removeComponent(comp);
						list.removeItem(name);
					}
					else {
						JOptionPane.showMessageDialog(BioSim.frame, "Cannot remove component "
								+ name.split(" ")[0] + " because it is currently in other reactions");
					}
				}
			}
		}

		private PropertyList list = null;
	}

	private class EditButton extends AbstractRunnableNamedButton {
		public EditButton(String name, PropertyList list) {
			super(name);
			this.list = list;
		}

		public void run() {
			new EditCommand(getName(), list).run();
		}

		private PropertyList list = null;
	}

	private class EditCommand implements Runnable {
		public EditCommand(String name, PropertyList list) {
			this.name = name;
			this.list = list;
		}

		public void run() {
			if (name == null || name.equals("")) {
				Utility.createErrorMessage("Error", "Nothing selected to edit");
				return;
			}
			if (list.getSelectedValue() == null && getName().contains("Edit")) {
				Utility.createErrorMessage("Error", "Nothing selected to edit");
				return;
			}
			//dirty = true;
			if (getName().contains("Species")) {
				String selected = null;
				if (list.getSelectedValue() != null && getName().contains("Edit")) {
					selected = list.getSelectedValue().toString();
					if (selected.split(" ").length > 1) {
						selected = selected.split(" ")[0];
					}
				}
				GCMFile refGCM = null;
				if (paramsOnly) {
					refGCM = new GCMFile(path);
					refGCM.load(path + separator + refFile);
				}
				SpeciesPanel panel = new SpeciesPanel(selected, list, influences, conditions, components, gcm, paramsOnly, refGCM, gcmEditor);
				if (paramsOnly) {
					String updates = panel.updates();
					if (!updates.equals("")) {
						for (int i = parameterChanges.size() - 1; i >= 0; i--) {
							if (parameterChanges.get(i).startsWith(updates.split("/")[0])) {
								parameterChanges.remove(i);
							}
						}
						if (updates.contains(" ")) {
							for (String s : updates.split("\n")) {
								parameterChanges.add(s);
							}
						}
					}
				}
			}
			else if (getName().contains("Influence")) {
				String selected = null;
				if (list.getSelectedValue() != null && getName().contains("Edit")) {
					selected = list.getSelectedValue().toString();
					if (selected.split(" ")[selected.split(" ").length - 1].equals("Modified")) {
						selected = selected.substring(0, selected.length() - 9);
					}
				}
				GCMFile refGCM = null;
				if (paramsOnly) {
					refGCM = new GCMFile(path);
					refGCM.load(path + separator + refFile);
				}
				InfluencePanel panel = new InfluencePanel(selected, list, gcm, paramsOnly, refGCM, gcmEditor);
				if (paramsOnly) {
					String updates = panel.updates();
					if (!updates.equals("")) {
						for (int i = parameterChanges.size() - 1; i >= 0; i--) {
							if (parameterChanges.get(i).startsWith(updates.split("/")[0])) {
								parameterChanges.remove(i);
							}
						}
						if (!updates.endsWith("/")) {
							for (String s : updates.split("\n")) {
								parameterChanges.add(s);
							}
						}
					}
				}
			}
			else if (getName().contains("Promoter")) {
				String selected = null;
				if (list.getSelectedValue() != null && getName().contains("Edit")) {
					selected = list.getSelectedValue().toString();
					if (selected.split(" ").length > 1) {
						selected = selected.split(" ")[0];
					}
				}
				GCMFile refGCM = null;
				if (paramsOnly) {
					refGCM = new GCMFile(path);
					refGCM.load(path + separator + refFile);
				}
				PromoterPanel panel = new PromoterPanel(selected, list, influences, gcm, paramsOnly, refGCM, gcmEditor);
				if (paramsOnly) {
					String updates = panel.updates();
					if (!updates.equals("")) {
						for (int i = parameterChanges.size() - 1; i >= 0; i--) {
							if (parameterChanges.get(i).startsWith(updates.split("/")[0])) {
								parameterChanges.remove(i);
							}
						}
						if (updates.contains(" ")) {
							for (String s : updates.split("\n")) {
								parameterChanges.add(s);
							}
						}
					}
				}
			}
			else if (getName().contains("Property")) {
				String selected = null;
				if (list.getSelectedValue() != null && getName().contains("Edit")) {
					selected = list.getSelectedValue().toString();
				}
				ConditionsPanel panel = new ConditionsPanel(selected, list, gcm, paramsOnly,gcmEditor);
			}
			else if (getName().contains("Component")) {
				displayChooseComponentDialog(getName().contains("Edit"), list, false);
			}
			else if (getName().contains("Parameter")) {
				String selected = null;
				if (list.getSelectedValue() != null && getName().contains("Edit")) {
					selected = list.getSelectedValue().toString();
				}
				GCMFile refGCM = null;
				if (paramsOnly) {
					refGCM = new GCMFile(path);
					refGCM.load(path + separator + refFile);
				}
				ParameterPanel panel = new ParameterPanel(selected, list, gcm, paramsOnly, refGCM, gcmEditor);
				if (paramsOnly) {
					String updates = panel.updates();
					if (!updates.equals("")) {
						for (int i = parameterChanges.size() - 1; i >= 0; i--) {
							if (parameterChanges.get(i).startsWith(updates.split(" ")[0])) {
								parameterChanges.remove(i);
							}
						}
						if (updates.contains(" ")) {
							for (String s : updates.split("\n")) {
								parameterChanges.add(s);
							}
						}
					}
				}
			}
		}

		public String getName() {
			return name;
		}

		private String name = null;

		private PropertyList list = null;
	}
	
	/**
	 * launches the promoter panel to edit the promoter with the given id.
	 * If no id is given, then it edits a new promoter.
	 * @param id
	 * @return
	 */
	public PromoterPanel launchPromoterPanel(String id){
		GCMFile refGCM = null;
		if (paramsOnly) {
			refGCM = new GCMFile(path);
			refGCM.load(path + separator + refFile);
		}
		return new PromoterPanel(id, promoters, influences, gcm, paramsOnly, refGCM, gcmEditor);	
	}
	public SpeciesPanel launchSpeciesPanel(String id){
		return launchSpeciesPanel(id, null);
	}
	public SpeciesPanel launchSpeciesPanel(String id, MovieContainer movieContainer){
		GCMFile refGCM = null;
		if (paramsOnly) {
			refGCM = new GCMFile(path);
			refGCM.load(path + separator + refFile);
		}
		return new SpeciesPanel(id, species, influences, conditions, components, gcm, paramsOnly, refGCM, this, movieContainer);
	}
	public InfluencePanel launchInfluencePanel(String id){
		GCMFile refGCM = null;
		if (paramsOnly) {
			refGCM = new GCMFile(path);
			refGCM.load(path + separator + refFile);
		}
		return new InfluencePanel(id, influences, gcm, paramsOnly, refGCM, gcmEditor);
	}
	public String launchComponentPanel(String id){
		GCMFile refGCM = null;
		if (paramsOnly) {
			refGCM = new GCMFile(path);
			refGCM.load(path + separator + refFile);
		}
		// TODO: This is a messy way to do things. We set the selected component in the list
		// and then call displayChooseComponentDialog(). This makes for tight coupling with the
		// component list.
		for(int i=0; i<this.components.getModel().getSize(); i++){
			String componentsListRow = this.components.getModel().getElementAt(i).toString();
			String componentsListId = componentsListRow.split(" ")[0];
			if(componentsListId.equals(id)){
				this.components.setSelectedIndex(i);
				break;
			}
		}
		return displayChooseComponentDialog(true, this.components, false);
	}
	
	public boolean checkNoComponentLoop(String gcm, String checkFile) {
		boolean check = true;
		GCMFile g = new GCMFile(path);
		g.load(path + separator + checkFile);
		for (String comp : g.getComponents().keySet()) {
			String compGCM = g.getComponents().get(comp).getProperty("gcm");
			if (compGCM.equals(gcm)) {
				return false;
			}
			else {
				check = checkNoComponentLoop(gcm, compGCM);
			}
		}
		return check;
	}
	
	/*
	 * 
	 * Displays the "Choose Component" dialog and then adds the component afterward.
	 * @param tryEdit: if true then try to bring up the edit window if a component is selected.
	 * @param list: The PropertiesList. If left null then the gcm2sbmleditor's component
	 * 				list will be used.
	 * @param createUsingDefaults:  If true then a component will be created with a basic name and
	 * 				no port mappings. Otherwise the user will be asked for 
	 * 				the name and mappings.
	 * 
	 * @return: the id of the component that was edited or created.
	 */
	public String displayChooseComponentDialog(boolean tryEdit, PropertyList list, boolean createUsingDefaults){
		
		String outID = null;
		
		if(list == null)
			list = this.components;
		String selected = null;
		String comp = null;
		if (list.getSelectedValue() != null && tryEdit) {
			selected = list.getSelectedValue().toString();
			comp = selected.split(" ")[1] + ".gcm";
		}
		else {
			ArrayList<String> components = new ArrayList<String>();
			for (String s : new File(path).list()) {
				if (s.endsWith(".gcm") && !s.equals(filename) && checkNoComponentLoop(filename, s)) {
					components.add(s);
				}
			}
			int i, j;
			String index;
			for (i = 1; i < components.size(); i++) {
				index = components.get(i);
				j = i;
				while ((j > 0) && components.get(j - 1).compareToIgnoreCase(index) > 0) {
					components.set(j, components.get(j - 1));
					j = j - 1;
				}
				components.set(j, index);
			}
			if (components.size() == 0) {
				comp = null;
				JOptionPane.showMessageDialog(BioSim.frame,
						"There aren't any other gcms to use as components."
								+ "\nCreate a new gcm or import a gcm into the project first.",
						"Add Another GCM To The Project", JOptionPane.ERROR_MESSAGE);
			}
			else {
				comp = (String) JOptionPane.showInputDialog(BioSim.frame,
						"Choose a gcm to use as a component:", "Component Editor",
						JOptionPane.PLAIN_MESSAGE, null, components.toArray(new String[0]), null);
			}
		}
		if (comp != null && !comp.equals("")) {
			GCMFile getSpecs = new GCMFile(path);
			getSpecs.load(path + separator + comp);
			String oldPort = null;
			if (selected != null) {
				oldPort = selected.substring(selected.split(" ")[0].length()
						+ selected.split(" ")[1].length() + 2);
				selected = selected.split(" ")[0];
			}

			ArrayList<String> in = getSpecs.getInputSpecies();
			ArrayList<String> out = getSpecs.getOutputSpecies();
			String[] inputs = in.toArray(new String[0]);
			String[] outputs = out.toArray(new String[0]);
			int i, j;
			String index;
			for (i = 1; i < inputs.length; i++) {
				index = inputs[i];
				j = i;
				while ((j > 0) && inputs[j - 1].compareToIgnoreCase(index) > 0) {
					inputs[j] = inputs[j - 1];
					j = j - 1;
				}
				inputs[j] = index;
			}
			for (i = 1; i < outputs.length; i++) {
				index = outputs[i];
				j = i;
				while ((j > 0) && outputs[j - 1].compareToIgnoreCase(index) > 0) {
					outputs[j] = outputs[j - 1];
					j = j - 1;
				}
				outputs[j] = index;
			}
			
			if(createUsingDefaults){
				Properties properties = new Properties();
				properties.put("gcm", comp);
				outID = gcm.addComponent(null, properties);
			}else{
				new ComponentsPanel(selected, list, influences, gcm,
						inputs, outputs, comp, oldPort, paramsOnly, gcmEditor);
				outID = selected;
			}

		}
		return outID;
	}
	
	public void setSBMLParamFile(SBML_Editor sbmlParamFile) {
		this.sbmlParamFile = sbmlParamFile;
	}

	private boolean lock = false;

	private JTextField GCMNameTextField = null;

	private String[] options = { "Ok", "Cancel" };

	private PropertyList species = null;

	private PropertyList influences = null;

	private PropertyList promoters = null;

	private PropertyList parameters = null;

	private PropertyList components = null;
	
	private PropertyList conditions = null;

	private JComboBox sbmlFiles = null;

	private JCheckBox bioAbs = null;

	private JCheckBox dimAbs = null;

	private String path = null;

	private static final String none = "--none--";

	private BioSim biosim = null;

	private Log log = null;

	private boolean dirty = false;
}
