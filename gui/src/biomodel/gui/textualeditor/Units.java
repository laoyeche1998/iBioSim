package biomodel.gui.textualeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import main.Gui;
import main.util.MutableBoolean;
import main.util.Utility;

import org.sbml.libsbml.Compartment;
import org.sbml.libsbml.KineticLaw;
import org.sbml.libsbml.ListOf;
import org.sbml.libsbml.Model;
import org.sbml.libsbml.Parameter;
import org.sbml.libsbml.SBMLDocument;
import org.sbml.libsbml.Species;
import org.sbml.libsbml.Unit;
import org.sbml.libsbml.UnitDefinition;
import org.sbml.libsbml.libsbml;


/**
 * This is a class for creating SBML units
 * 
 * @author Chris Myers
 * 
 */
public class Units extends JPanel implements ActionListener, MouseListener {

	private static final long serialVersionUID = 1L;

	private JButton addUnit, removeUnit, editUnit;

	private JButton addList, removeList, editList;

	private JList unitDefs; // JList of units

	private JList unitList; // JList of units

	private JTextField exp, scale, mult; // unit list fields;

	private SBMLDocument document;

	private ArrayList<String> usedIDs;

	private MutableBoolean dirty;

	private Gui biosim;

	//private String[] uList;

	public Units(Gui biosim, SBMLDocument document, ArrayList<String> usedIDs, MutableBoolean dirty) {
		super(new BorderLayout());
		this.document = document;
		this.usedIDs = usedIDs;
		this.biosim = biosim;
		this.dirty = dirty;
		Model model = document.getModel();
		addUnit = new JButton("Add Unit");
		removeUnit = new JButton("Remove Unit");
		editUnit = new JButton("Edit Unit");
		unitDefs = new JList();
		ListOf listOfUnits = model.getListOfUnitDefinitions();
		String[] units = new String[(int) model.getNumUnitDefinitions()];
		for (int i = 0; i < model.getNumUnitDefinitions(); i++) {
			UnitDefinition unit = (UnitDefinition) listOfUnits.get(i);
			units[i] = unit.getId();
			// GET OTHER THINGS
		}
		JPanel addRem = new JPanel();
		addRem.add(addUnit);
		addRem.add(removeUnit);
		addRem.add(editUnit);
		addUnit.addActionListener(this);
		removeUnit.addActionListener(this);
		editUnit.addActionListener(this);
		JLabel panelLabel = new JLabel("List of Units:");
		JScrollPane scroll = new JScrollPane();
		scroll.setMinimumSize(new Dimension(260, 220));
		scroll.setPreferredSize(new Dimension(276, 152));
		scroll.setViewportView(unitDefs);
		Utility.sort(units);
		unitDefs.setListData(units);
		unitDefs.setSelectedIndex(0);
		unitDefs.addMouseListener(this);
		this.add(panelLabel, "North");
		this.add(scroll, "Center");
		this.add(addRem, "South");
	}

	/**
	 * Creates a frame used to edit units or create new ones.
	 */
	private void unitEditor(String option) {
		Pattern IDpat = Pattern.compile("([a-zA-Z]|_)([a-zA-Z]|[0-9]|_)*");
		if (option.equals("OK") && unitDefs.getSelectedIndex() == -1) {
			JOptionPane.showMessageDialog(Gui.frame, "No unit definition selected.", "Must Select a Unit Definition", JOptionPane.ERROR_MESSAGE);
			return;
		}
		String[] kindsL2V4 = { "ampere", "becquerel", "candela", "celsius", "coulomb", "dimensionless", "farad", "gram", "gray", "henry", "hertz",
				"item", "joule", "katal", "kelvin", "kilogram", "litre", "lumen", "lux", "metre", "mole", "newton", "ohm", "pascal", "radian",
				"second", "siemens", "sievert", "steradian", "tesla", "volt", "watt", "weber" };
		String[] kindsL3V1 = { "ampere", "avogadro", "becquerel", "candela", "celsius", "coulomb", "dimensionless", "farad", "gram", "gray", "henry",
				"hertz", "item", "joule", "katal", "kelvin", "kilogram", "litre", "lumen", "lux", "metre", "mole", "newton", "ohm", "pascal",
				"radian", "second", "siemens", "sievert", "steradian", "tesla", "volt", "watt", "weber" };
		String[] kinds;
		if (document.getLevel() < 3) {
			kinds = kindsL2V4;
		}
		else {
			kinds = kindsL3V1;
		}
		JPanel unitDefPanel = new JPanel(new BorderLayout());
		JPanel unitPanel = new JPanel(new GridLayout(2, 2));
		JLabel idLabel = new JLabel("ID:");
		JTextField unitID = new JTextField(12);
		JLabel nameLabel = new JLabel("Name:");
		JTextField unitName = new JTextField(12);
		JPanel unitListPanel = new JPanel(new BorderLayout());
		JPanel addUnitList = new JPanel();
		addList = new JButton("Add to List");
		removeList = new JButton("Remove from List");
		editList = new JButton("Edit List");
		addUnitList.add(addList);
		addUnitList.add(removeList);
		addUnitList.add(editList);
		addList.addActionListener(this);
		removeList.addActionListener(this);
		editList.addActionListener(this);
		JLabel unitListLabel = new JLabel("List of Units:");
		unitList = new JList();
		unitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scroll = new JScrollPane();
		scroll.setMinimumSize(new Dimension(260, 220));
		scroll.setPreferredSize(new Dimension(276, 152));
		scroll.setViewportView(unitList);
		String [] uList = new String[0];
		if (option.equals("OK")) {
			try {
				UnitDefinition unit = document.getModel().getUnitDefinition((((String) unitDefs.getSelectedValue()).split(" ")[0]));
				unitID.setText(unit.getId());
				unitName.setText(unit.getName());
				uList = new String[(int) unit.getNumUnits()];
				for (int i = 0; i < unit.getNumUnits(); i++) {
					uList[i] = "";
					if (unit.getUnit(i).getMultiplier() != 1.0) {
						uList[i] = unit.getUnit(i).getMultiplier() + " * ";
					}
					if (unit.getUnit(i).getScale() != 0) {
						uList[i] = uList[i] + "10^" + unit.getUnit(i).getScale() + " * ";
					}
					uList[i] = uList[i] + unitToString(unit.getUnit(i));
					if (document.getLevel() < 3) {
						if (unit.getUnit(i).getExponent() != 1) {
							uList[i] = "( " + uList[i] + " )^" + unit.getUnit(i).getExponent();
						}
					}
					else {
						if (unit.getUnit(i).getExponentAsDouble() != 1) {
							uList[i] = "( " + uList[i] + " )^" + unit.getUnit(i).getExponentAsDouble();
						}
					}
				}
			}
			catch (Exception e) {
			}
		}

		Utility.sort(uList);
		unitList.setListData(uList);
		unitList.setSelectedIndex(0);
		unitList.addMouseListener(this);
		unitListPanel.add(unitListLabel, "North");
		unitListPanel.add(scroll, "Center");
		unitListPanel.add(addUnitList, "South");
		unitPanel.add(idLabel);
		unitPanel.add(unitID);
		unitPanel.add(nameLabel);
		unitPanel.add(unitName);
		unitDefPanel.add(unitPanel, "North");
		unitDefPanel.add(unitListPanel, "South");
		Object[] options = { option, "Cancel" };
		int value = JOptionPane.showOptionDialog(Gui.frame, unitDefPanel, "Unit Definition Editor", JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		boolean error = true;
		while (error && value == JOptionPane.YES_OPTION) {
			uList = new String[unitList.getModel().getSize()];
			for (int i = 0; i < unitList.getModel().getSize(); i++) {
				uList[i] = unitList.getModel().getElementAt(i).toString();
			}
			error = false;
			if (unitID.getText().trim().equals("")) {
				JOptionPane.showMessageDialog(Gui.frame, "A unit definition ID is required.", "Enter an ID", JOptionPane.ERROR_MESSAGE);
				error = true;
				value = JOptionPane.showOptionDialog(Gui.frame, unitDefPanel, "Unit Definition Editor", JOptionPane.YES_NO_OPTION,
						JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			}
			else {
				String addUnit = "";
				addUnit = unitID.getText().trim();
				if (!(IDpat.matcher(addUnit).matches())) {
					JOptionPane.showMessageDialog(Gui.frame, "A unit definition ID can only contain letters, numbers, and underscores.",
							"Invalid ID", JOptionPane.ERROR_MESSAGE);
					error = true;
				}
				else {
					for (int i = 0; i < kinds.length; i++) {
						if (kinds[i].equals(addUnit)) {
							JOptionPane.showMessageDialog(Gui.frame, "Unit ID matches a predefined unit.", "Enter a Unique ID",
									JOptionPane.ERROR_MESSAGE);
							error = true;
							break;
						}
					}
				}
				if (!error) {
					for (int i = 0; i < unitDefs.getModel().getSize(); i++) {
						if (option.equals("OK")) {
							if (unitDefs.getModel().getElementAt(i).toString().equals((String) unitDefs.getSelectedValue()))
								continue;
						}
						if (unitDefs.getModel().getElementAt(i).toString().equals(addUnit)) {
							JOptionPane.showMessageDialog(Gui.frame, "Unit ID is not unique.", "Enter a Unique ID", JOptionPane.ERROR_MESSAGE);
							error = true;
						}
					}
				}
				if ((!error) && (uList.length == 0)) {
					JOptionPane
							.showMessageDialog(Gui.frame, "Unit definition must have at least one unit.", "Unit Needed", JOptionPane.ERROR_MESSAGE);
					error = true;
				}
				if ((!error)
						&& (document.getLevel() < 3)
						&& ((addUnit.equals("substance")) || (addUnit.equals("length")) || (addUnit.equals("area")) || (addUnit.equals("volume")) || (addUnit
								.equals("time")))) {
					if (uList.length > 1) {
						JOptionPane.showMessageDialog(Gui.frame, "Redefinition of built-in unit must have a single unit.", "Single Unit Required",
								JOptionPane.ERROR_MESSAGE);
						error = true;
					}
					if (!error && document.getLevel() < 3) {
						if (addUnit.equals("substance")) {
							if (!(extractUnitKind(uList[0]).equals("dimensionless")
									|| (extractUnitKind(uList[0]).equals("mole") && Integer.valueOf(extractUnitExp(uList[0])) == 1)
									|| (extractUnitKind(uList[0]).equals("item") && Integer.valueOf(extractUnitExp(uList[0])) == 1)
									|| (extractUnitKind(uList[0]).equals("gram") && Integer.valueOf(extractUnitExp(uList[0])) == 1) || (extractUnitKind(
									uList[0]).equals("kilogram") && Integer.valueOf(extractUnitExp(uList[0])) == 1))) {
								JOptionPane.showMessageDialog(Gui.frame,
										"Redefinition of substance must be dimensionless or\n in terms of moles, items, grams, or kilograms.",
										"Incorrect Redefinition", JOptionPane.ERROR_MESSAGE);
								error = true;
							}
						}
						else if (addUnit.equals("time")) {
							if (!(extractUnitKind(uList[0]).equals("dimensionless") || (extractUnitKind(uList[0]).equals("second") && Integer
									.valueOf(extractUnitExp(uList[0])) == 1))) {
								JOptionPane.showMessageDialog(Gui.frame, "Redefinition of time must be dimensionless or in terms of seconds.",
										"Incorrect Redefinition", JOptionPane.ERROR_MESSAGE);
								error = true;
							}
						}
						else if (addUnit.equals("length")) {
							if (!(extractUnitKind(uList[0]).equals("dimensionless") || (extractUnitKind(uList[0]).equals("metre") && Integer
									.valueOf(extractUnitExp(uList[0])) == 1))) {
								JOptionPane.showMessageDialog(Gui.frame, "Redefinition of length must be dimensionless or in terms of metres.",
										"Incorrect Redefinition", JOptionPane.ERROR_MESSAGE);
								error = true;
							}
						}
						else if (addUnit.equals("area")) {
							if (!(extractUnitKind(uList[0]).equals("dimensionless") || (extractUnitKind(uList[0]).equals("metre") && Integer
									.valueOf(extractUnitExp(uList[0])) == 2))) {
								JOptionPane.showMessageDialog(Gui.frame, "Redefinition of area must be dimensionless or in terms of metres^2.",
										"Incorrect Redefinition", JOptionPane.ERROR_MESSAGE);
								error = true;
							}
						}
						else if (addUnit.equals("volume")) {
							if (!(extractUnitKind(uList[0]).equals("dimensionless")
									|| (extractUnitKind(uList[0]).equals("litre") && Integer.valueOf(extractUnitExp(uList[0])) == 1) || (extractUnitKind(
									uList[0]).equals("metre") && Integer.valueOf(extractUnitExp(uList[0])) == 3))) {
								JOptionPane.showMessageDialog(Gui.frame,
										"Redefinition of volume must be dimensionless or in terms of litres or metres^3.", "Incorrect Redefinition",
										JOptionPane.ERROR_MESSAGE);
								error = true;
							}
						}
					}
				}
				if (!error) {
					if (option.equals("OK")) {
						String[] units = new String[unitDefs.getModel().getSize()];
						for (int i = 0; i < unitDefs.getModel().getSize(); i++) {
							units[i] = unitDefs.getModel().getElementAt(i).toString();
						}
						int index = unitDefs.getSelectedIndex();
						String val = ((String) unitDefs.getSelectedValue()).split(" ")[0];
						unitDefs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
						units = Utility.getList(units, unitDefs);
						unitDefs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
						UnitDefinition u = document.getModel().getUnitDefinition(val);
						UnitDefinition uCopy = u.cloneObject();
						u.setId(unitID.getText().trim());
						u.setName(unitName.getText().trim());
						for (int i = 0; i < usedIDs.size(); i++) {
							if (usedIDs.get(i).equals(val)) {
								usedIDs.set(i, addUnit);
							}
						}
						while (u.getNumUnits() > 0) {
							u.getListOfUnits().remove(0);
						}
						for (int i = 0; i < uList.length; i++) {
							Unit unit = u.createUnit();
							unit.setKind(libsbml.UnitKind_forName(extractUnitKind(uList[i])));
							if (document.getLevel() < 3) {
								unit.setExponent(Integer.valueOf(extractUnitExp(uList[i])).intValue());
							}
							else {
								unit.setExponent(Double.valueOf(extractUnitExp(uList[i])).doubleValue());
							}
							unit.setScale(Integer.valueOf(extractUnitScale(uList[i])).intValue());
							unit.setMultiplier(Double.valueOf(extractUnitMult(uList[i])).doubleValue());
						}
						// error = checkUnits();
						if (!error) {
							units[index] = addUnit;
							Utility.sort(units);
							unitDefs.setListData(units);
							unitDefs.setSelectedIndex(index);
							updateUnitId(val, unitID.getText().trim());
						}
						else {
							u = uCopy;
							unitDefs.setSelectedIndex(index);
						}
					}
					else {
						String[] units = new String[unitDefs.getModel().getSize()];
						for (int i = 0; i < unitDefs.getModel().getSize(); i++) {
							units[i] = unitDefs.getModel().getElementAt(i).toString();
						}
						int index = unitDefs.getSelectedIndex();
						UnitDefinition u = document.getModel().createUnitDefinition();
						u.setId(unitID.getText().trim());
						u.setName(unitName.getText().trim());
						usedIDs.add(addUnit);
						for (int i = 0; i < uList.length; i++) {
							Unit unit = u.createUnit();
							unit.setKind(libsbml.UnitKind_forName(extractUnitKind(uList[i])));
							if (document.getLevel() < 3) {
								unit.setExponent(Integer.valueOf(extractUnitExp(uList[i])).intValue());
							}
							else {
								unit.setExponent(Double.valueOf(extractUnitExp(uList[i])).doubleValue());
							}
							unit.setScale(Integer.valueOf(extractUnitScale(uList[i])).intValue());
							unit.setMultiplier(Double.valueOf(extractUnitMult(uList[i])).doubleValue());
						}
						JList add = new JList();
						Object[] adding = { addUnit };
						add.setListData(adding);
						add.setSelectedIndex(0);
						unitDefs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
						adding = Utility.add(units, unitDefs, add, false, null, null, null, null, null, null, Gui.frame);
						units = new String[adding.length];
						for (int i = 0; i < adding.length; i++) {
							units[i] = (String) adding[i];
						}
						Utility.sort(units);
						unitDefs.setListData(units);
						unitDefs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
						if (document.getModel().getNumUnitDefinitions() == 1) {
							unitDefs.setSelectedIndex(0);
						}
						else {
							unitDefs.setSelectedIndex(index);
						}
					}
					dirty.setValue(true);
				}
				if (error) {
					value = JOptionPane.showOptionDialog(Gui.frame, unitDefPanel, "Unit Definition Editor", JOptionPane.YES_NO_OPTION,
							JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
				}
			}
		}
		if (value == JOptionPane.NO_OPTION) {
			return;
		}
	}

	/**
	 * Creates a frame used to edit unit list elements or create new ones.
	 */
	private void unitListEditor(String option) {
		if (option.equals("OK") && unitList.getSelectedIndex() == -1) {
			JOptionPane.showMessageDialog(Gui.frame, "No unit selected.", "Must Select an Unit", JOptionPane.ERROR_MESSAGE);
			return;
		}
		JPanel unitListPanel = new JPanel();
		JPanel ULPanel = new JPanel(new GridLayout(4, 2));
		JLabel kindLabel = new JLabel("Kind:");
		JLabel expLabel = new JLabel("Exponent:");
		JLabel scaleLabel = new JLabel("Scale:");
		JLabel multLabel = new JLabel("Multiplier:");
		String[] kindsL2V4 = { "ampere", "becquerel", "candela", "celsius", "coulomb", "dimensionless", "farad", "gram", "gray", "henry", "hertz",
				"item", "joule", "katal", "kelvin", "kilogram", "litre", "lumen", "lux", "metre", "mole", "newton", "ohm", "pascal", "radian",
				"second", "siemens", "sievert", "steradian", "tesla", "volt", "watt", "weber" };
		String[] kindsL3V1 = { "ampere", "avogadro", "becquerel", "candela", "celsius", "coulomb", "dimensionless", "farad", "gram", "gray", "henry",
				"hertz", "item", "joule", "katal", "kelvin", "kilogram", "litre", "lumen", "lux", "metre", "mole", "newton", "ohm", "pascal",
				"radian", "second", "siemens", "sievert", "steradian", "tesla", "volt", "watt", "weber" };
		String[] kinds;
		if (document.getLevel() < 3) {
			kinds = kindsL2V4;
		}
		else {
			kinds = kindsL3V1;
		}
		final JComboBox kindBox = new JComboBox(kinds);
		exp = new JTextField(12);
		exp.setText("1");
		scale = new JTextField(12);
		scale.setText("0");
		mult = new JTextField(12);
		mult.setText("1.0");
		if (option.equals("OK")) {
			String selected = (String) unitList.getSelectedValue();
			kindBox.setSelectedItem(extractUnitKind(selected));
			exp.setText(extractUnitExp(selected));
			scale.setText(extractUnitScale(selected));
			mult.setText(extractUnitMult(selected));
		}
		ULPanel.add(kindLabel);
		ULPanel.add(kindBox);
		ULPanel.add(expLabel);
		ULPanel.add(exp);
		ULPanel.add(scaleLabel);
		ULPanel.add(scale);
		ULPanel.add(multLabel);
		ULPanel.add(mult);
		unitListPanel.add(ULPanel);
		Object[] options = { option, "Cancel" };
		int value = JOptionPane.showOptionDialog(Gui.frame, unitListPanel, "Unit List Editor", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE,
				null, options, options[0]);
		boolean error = true;
		while (error && value == JOptionPane.YES_OPTION) {
			error = false;
			if (document.getLevel() < 3) {
				try {
					Integer.valueOf(exp.getText().trim()).intValue();
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(Gui.frame, "Exponent must be an integer.", "Integer Expected", JOptionPane.ERROR_MESSAGE);
					error = true;
				}
			}
			else {
				try {
					Double.valueOf(exp.getText().trim()).doubleValue();
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(Gui.frame, "Exponent must be a double.", "Double Expected", JOptionPane.ERROR_MESSAGE);
					error = true;
				}
			}
			if (!error) {
				try {
					Integer.valueOf(scale.getText().trim()).intValue();
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(Gui.frame, "Scale must be an integer.", "Integer Expected", JOptionPane.ERROR_MESSAGE);
					error = true;
				}
			}
			if (!error) {
				try {
					Double.valueOf(mult.getText().trim()).doubleValue();
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(Gui.frame, "Multiplier must be a double.", "Double Expected", JOptionPane.ERROR_MESSAGE);
					error = true;
				}
			}
			if (!error) {
				if (option.equals("OK")) {
					String [] uList = new String[unitList.getModel().getSize()];
					for (int i = 0; i < unitList.getModel().getSize(); i++) {
						uList[i] = unitList.getModel().getElementAt(i).toString();
					}
					int index = unitList.getSelectedIndex();
					unitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
					uList = Utility.getList(uList, unitList);
					unitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					uList[index] = "";
					if (!mult.getText().trim().equals("1.0")) {
						uList[index] = mult.getText().trim() + " * ";
					}
					if (!scale.getText().trim().equals("0")) {
						uList[index] = uList[index] + "10^" + scale.getText().trim() + " * ";
					}
					uList[index] = uList[index] + kindBox.getSelectedItem();
					if (!exp.getText().trim().equals("1")) {
						uList[index] = "( " + uList[index] + " )^" + exp.getText().trim();
					}
					Utility.sort(uList);
					unitList.setListData(uList);
					unitList.setSelectedIndex(index);
				}
				else {
					String [] uList = new String[unitList.getModel().getSize()];
					for (int i = 0; i < unitList.getModel().getSize(); i++) {
						uList[i] = unitList.getModel().getElementAt(i).toString();
					}
					JList add = new JList();
					int index = unitList.getSelectedIndex();
					String addStr;
					addStr = "";
					if (!mult.getText().trim().equals("1.0")) {
						addStr = mult.getText().trim() + " * ";
					}
					if (!scale.getText().trim().equals("0")) {
						addStr = addStr + "10^" + scale.getText().trim() + " * ";
					}
					addStr = addStr + kindBox.getSelectedItem();
					if (!exp.getText().trim().equals("1")) {
						addStr = "( " + addStr + " )^" + exp.getText().trim();
					}
					Object[] adding = { addStr };
					add.setListData(adding);
					add.setSelectedIndex(0);
					unitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
					adding = Utility.add(uList, unitList, add, false, null, null, null, null, null, null, Gui.frame);
					uList = new String[adding.length];
					for (int i = 0; i < adding.length; i++) {
						uList[i] = (String) adding[i];
					}
					Utility.sort(uList);
					unitList.setListData(uList);
					unitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					if (adding.length == 1) {
						unitList.setSelectedIndex(0);
					}
					else {
						unitList.setSelectedIndex(index);
					}
				}
				dirty.setValue(true);
			}
			if (error) {
				value = JOptionPane.showOptionDialog(Gui.frame, unitListPanel, "Unit List Editor", JOptionPane.YES_NO_OPTION,
						JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			}
		}
		if (value == JOptionPane.NO_OPTION) {
			return;
		}
	}

	/**
	 * Remove a unit from list
	 */
	private void removeList() {
		int index = unitDefs.getSelectedIndex();
		if (index != -1) {
			UnitDefinition tempUnit = document.getModel().getUnitDefinition(((String) unitDefs.getSelectedValue()).split(" ")[0]);
			if (unitList.getSelectedIndex() != -1) {
				String selected = (String) unitList.getSelectedValue();
				ListOf u = tempUnit.getListOfUnits();
				for (int i = 0; i < tempUnit.getNumUnits(); i++) {
					if (selected.contains(unitToString(tempUnit.getUnit(i)))) {
						u.remove(i);
					}
				}
			}
			unitList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			Utility.remove(unitList);
			unitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			if (index < unitList.getModel().getSize()) {
				unitList.setSelectedIndex(index);
			}
			else {
				unitList.setSelectedIndex(index - 1);
			}
			dirty.setValue(true);
		}
	}

	/**
	 * Remove a unit
	 */
	private void removeUnit() {
		int index = unitDefs.getSelectedIndex();
		if (index != -1) {
			if (!unitsInUse(((String) unitDefs.getSelectedValue()).split(" ")[0])) {
				UnitDefinition tempUnit = document.getModel().getUnitDefinition(((String) unitDefs.getSelectedValue()).split(" ")[0]);
				ListOf u = document.getModel().getListOfUnitDefinitions();
				for (int i = 0; i < document.getModel().getNumUnitDefinitions(); i++) {
					if (((UnitDefinition) u.get(i)).getId().equals(tempUnit.getId())) {
						u.remove(i);
					}
				}
				usedIDs.remove(((String) unitDefs.getSelectedValue()).split(" ")[0]);
				unitDefs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				Utility.remove(unitDefs);
				unitDefs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				if (index < unitDefs.getModel().getSize()) {
					unitDefs.setSelectedIndex(index);
				}
				else {
					unitDefs.setSelectedIndex(index - 1);
				}
				dirty.setValue(true);
			}
		}
	}

	/**
	 * Check if a unit is in use.
	 */
	private boolean unitsInUse(String unit) {
		Model model = document.getModel();
		boolean inUse = false;
		ArrayList<String> modelUnitsUsing = new ArrayList<String>();
		if (document.getLevel() > 2) {
			if (model.isSetSubstanceUnits() && model.getSubstanceUnits().equals(unit)) {
				inUse = true;
				modelUnitsUsing.add("substance");
			}
			if (model.isSetTimeUnits() && model.getTimeUnits().equals(unit)) {
				inUse = true;
				modelUnitsUsing.add("time");
			}
			if (model.isSetVolumeUnits() && model.getVolumeUnits().equals(unit)) {
				inUse = true;
				modelUnitsUsing.add("volume");
			}
			if (model.isSetAreaUnits() && model.getAreaUnits().equals(unit)) {
				inUse = true;
				modelUnitsUsing.add("area");
			}
			if (model.isSetLengthUnits() && model.getLengthUnits().equals(unit)) {
				inUse = true;
				modelUnitsUsing.add("length");
			}
			if (model.isSetExtentUnits() && model.getExtentUnits().equals(unit)) {
				inUse = true;
				modelUnitsUsing.add("extent");
			}
		}
		ArrayList<String> compartmentsUsing = new ArrayList<String>();
		for (int i = 0; i < model.getNumCompartments(); i++) {
			Compartment compartment = (Compartment) model.getListOfCompartments().get(i);
			if (compartment.getUnits().equals(unit)) {
				inUse = true;
				compartmentsUsing.add(compartment.getId());
			}
		}
		ArrayList<String> speciesUsing = new ArrayList<String>();
		for (int i = 0; i < model.getNumSpecies(); i++) {
			Species species = (Species) model.getListOfSpecies().get(i);
			if (species.getUnits().equals(unit)) {
				inUse = true;
				speciesUsing.add(species.getId());
			}
		}
		ArrayList<String> parametersUsing = new ArrayList<String>();
		for (int i = 0; i < model.getNumParameters(); i++) {
			Parameter parameters = (Parameter) model.getListOfParameters().get(i);
			if (parameters.getUnits().equals(unit)) {
				inUse = true;
				parametersUsing.add(parameters.getId());
			}
		}
		ArrayList<String> reacParametersUsing = new ArrayList<String>();
		for (int i = 0; i < model.getNumReactions(); i++) {
			for (int j = 0; j < model.getReaction(i).getKineticLaw().getNumParameters(); j++) {
				Parameter parameters = (Parameter) model.getReaction(i).getKineticLaw().getListOfParameters().get(j);
				if (parameters.getUnits().equals(unit)) {
					inUse = true;
					reacParametersUsing.add(model.getReaction(i).getId() + "/" + parameters.getId());
				}
			}
		}
		if (inUse) {
			String message = "Unable to remove the selected unit.";
			String[] ids;
			if (modelUnitsUsing.size() != 0) {
				ids = modelUnitsUsing.toArray(new String[0]);
				Utility.sort(ids);
				message += "\n\nIt is used by the following model units:\n";
				for (int i = 0; i < ids.length; i++) {
					if (i == ids.length - 1) {
						message += ids[i];
					}
					else {
						message += ids[i] + "\n";
					}
				}
			}
			if (compartmentsUsing.size() != 0) {
				ids = compartmentsUsing.toArray(new String[0]);
				Utility.sort(ids);
				message += "\n\nIt is used by the following compartments:\n";
				for (int i = 0; i < ids.length; i++) {
					if (i == ids.length - 1) {
						message += ids[i];
					}
					else {
						message += ids[i] + "\n";
					}
				}
			}
			if (speciesUsing.size() != 0) {
				ids = speciesUsing.toArray(new String[0]);
				Utility.sort(ids);
				message += "\n\nIt is used by the following species:\n";
				for (int i = 0; i < ids.length; i++) {
					if (i == ids.length - 1) {
						message += ids[i];
					}
					else {
						message += ids[i] + "\n";
					}
				}
			}
			if (parametersUsing.size() != 0) {
				ids = parametersUsing.toArray(new String[0]);
				Utility.sort(ids);
				message += "\n\nIt is used by the following parameters:\n";
				for (int i = 0; i < ids.length; i++) {
					if (i == ids.length - 1) {
						message += ids[i];
					}
					else {
						message += ids[i] + "\n";
					}
				}
			}
			if (reacParametersUsing.size() != 0) {
				ids = reacParametersUsing.toArray(new String[0]);
				Utility.sort(ids);
				message += "\n\nIt is used by the following reaction/parameters:\n";
				for (int i = 0; i < ids.length; i++) {
					if (i == ids.length - 1) {
						message += ids[i];
					}
					else {
						message += ids[i] + "\n";
					}
				}
			}
			JTextArea messageArea = new JTextArea(message);
			messageArea.setEditable(false);
			JScrollPane scroll = new JScrollPane();
			scroll.setMinimumSize(new Dimension(350, 350));
			scroll.setPreferredSize(new Dimension(350, 350));
			scroll.setViewportView(messageArea);
			JOptionPane.showMessageDialog(Gui.frame, scroll, "Unable To Remove Variable", JOptionPane.ERROR_MESSAGE);
		}
		return inUse;
	}

	/**
	 * Convert unit kind to string
	 */
	private String unitToString(Unit unit) {
		if (unit.isAmpere()) {
			return "ampere";
		}
		else if (unit.isAvogadro()) {
			return "avogadro";
		}
		else if (unit.isBecquerel()) {
			return "becquerel";
		}
		else if (unit.isCandela()) {
			return "candela";
		}
		else if (unit.isCelsius()) {
			return "celsius";
		}
		else if (unit.isCoulomb()) {
			return "coulomb";
		}
		else if (unit.isDimensionless()) {
			return "dimensionless";
		}
		else if (unit.isFarad()) {
			return "farad";
		}
		else if (unit.isGram()) {
			return "gram";
		}
		else if (unit.isGray()) {
			return "gray";
		}
		else if (unit.isHenry()) {
			return "henry";
		}
		else if (unit.isHertz()) {
			return "hertz";
		}
		else if (unit.isItem()) {
			return "item";
		}
		else if (unit.isJoule()) {
			return "joule";
		}
		else if (unit.isKatal()) {
			return "katal";
		}
		else if (unit.isKelvin()) {
			return "kelvin";
		}
		else if (unit.isKilogram()) {
			return "kilogram";
		}
		else if (unit.isLitre()) {
			return "litre";
		}
		else if (unit.isLumen()) {
			return "lumen";
		}
		else if (unit.isLux()) {
			return "lux";
		}
		else if (unit.isMetre()) {
			return "metre";
		}
		else if (unit.isMole()) {
			return "mole";
		}
		else if (unit.isNewton()) {
			return "newton";
		}
		else if (unit.isOhm()) {
			return "ohm";
		}
		else if (unit.isPascal()) {
			return "pascal";
		}
		else if (unit.isRadian()) {
			return "radian";
		}
		else if (unit.isSecond()) {
			return "second";
		}
		else if (unit.isSiemens()) {
			return "siemens";
		}
		else if (unit.isSievert()) {
			return "sievert";
		}
		else if (unit.isSteradian()) {
			return "steradian";
		}
		else if (unit.isTesla()) {
			return "tesla";
		}
		else if (unit.isVolt()) {
			return "volt";
		}
		else if (unit.isWatt()) {
			return "watt";
		}
		else if (unit.isWeber()) {
			return "weber";
		}
		return "Unknown";
	}

	/**
	 * Extract unit kind from string
	 */
	private String extractUnitKind(String selected) {
		if (selected.contains(")^")) {
			if (selected.contains("10^")) {
				return selected.substring(selected.lastIndexOf("*") + 2, selected.indexOf(")") - 1);
			}
			else if (selected.contains("*")) {
				return selected.substring(selected.lastIndexOf("*") + 2, selected.indexOf(")") - 1);
			}
			else {
				return selected.substring(2, selected.indexOf(")") - 1);
			}
		}
		else if (selected.contains("10^")) {
			return selected.substring(selected.lastIndexOf("*") + 2);
		}
		else if (selected.contains("*")) {
			mult.setText(selected.substring(0, selected.indexOf("*") - 1));
			return selected.substring(selected.indexOf("*") + 2);
		}
		else {
			return selected;
		}
	}

	/**
	 * Extract unit exponent from string
	 */
	private String extractUnitExp(String selected) {
		if (selected.contains(")^")) {
			return selected.substring(selected.indexOf(")^") + 2);
		}
		return "1";
	}

	/**
	 * Extract unit scale from string
	 */
	private String extractUnitScale(String selected) {
		if (selected.contains(")^")) {
			if (selected.contains("10^")) {
				return selected.substring(selected.indexOf("10^") + 3, selected.lastIndexOf("*") - 1);
			}
		}
		else if (selected.contains("10^")) {
			return selected.substring(selected.indexOf("10^") + 3, selected.lastIndexOf("*") - 1);
		}
		return "0";
	}

	/**
	 * Extract unit multiplier from string
	 */
	private String extractUnitMult(String selected) {
		if (selected.contains(")^")) {
			if (selected.contains("10^")) {
				String multStr = selected.substring(2, selected.indexOf("*") - 1);
				if (!multStr.contains("10^")) {
					return multStr;
				}
			}
			else if (selected.contains("*")) {
				return selected.substring(2, selected.indexOf("*") - 1);
			}
		}
		else if (selected.contains("10^")) {
			String multStr = selected.substring(0, selected.indexOf("*") - 1);
			if (!multStr.contains("10^")) {
				return multStr;
			}
		}
		else if (selected.contains("*")) {
			return selected.substring(0, selected.indexOf("*") - 1);
		}
		return "1.0";
	}

	/**
	 * Update unit Id
	 */
	private void updateUnitId(String origId, String newId) {
		if (origId.equals(newId))
			return;
		Model model = document.getModel();
		if (document.getLevel() > 2) {
			if (model.isSetSubstanceUnits()) {
				if (model.getSubstanceUnits().equals(origId)) {
					model.setSubstanceUnits(newId);
				}
			}
			if (model.isSetTimeUnits()) {
				if (model.getTimeUnits().equals(origId)) {
					model.setTimeUnits(newId);
				}
			}
			if (model.isSetVolumeUnits()) {
				if (model.getVolumeUnits().equals(origId)) {
					model.setVolumeUnits(newId);
				}
			}
			if (model.isSetAreaUnits()) {
				if (model.getAreaUnits().equals(origId)) {
					model.setAreaUnits(newId);
				}
			}
			if (model.isSetLengthUnits()) {
				if (model.getLengthUnits().equals(origId)) {
					model.setLengthUnits(newId);
				}
			}
			if (model.isSetExtentUnits()) {
				if (model.getExtentUnits().equals(origId)) {
					model.setExtentUnits(newId);
				}
			}
		}
		if (model.getNumCompartments() > 0) {
			String[] comps = new String[(int) model.getNumCompartments()];
			for (int i = 0; i < model.getNumCompartments(); i++) {
				Compartment compartment = (Compartment) model.getListOfCompartments().get(i);
				if (compartment.getUnits().equals(origId)) {
					compartment.setUnits(newId);
				}
				comps[i] = compartment.getId();
				if (compartment.isSetCompartmentType()) {
					comps[i] += " " + compartment.getCompartmentType();
				}
				if (compartment.isSetSize()) {
					comps[i] += " " + compartment.getSize();
				}
				if (compartment.isSetUnits()) {
					comps[i] += " " + compartment.getUnits();
				}
			}
			Utility.sort(comps);
		}
		if (model.getNumSpecies() > 0) {
			String[] specs = new String[(int) model.getNumSpecies()];
			for (int i = 0; i < model.getNumSpecies(); i++) {
				Species species = (Species) model.getListOfSpecies().get(i);
				if (species.getUnits().equals(origId)) {
					species.setUnits(newId);
				}
				if (species.isSetSpeciesType()) {
					specs[i] = species.getId() + " " + species.getSpeciesType() + " " + species.getCompartment();
				}
				else {
					specs[i] = species.getId() + " " + species.getCompartment();
				}
				if (species.isSetInitialAmount()) {
					specs[i] += " " + species.getInitialAmount();
				}
				else {
					specs[i] += " " + species.getInitialConcentration();
				}
				if (species.isSetUnits()) {
					specs[i] += " " + species.getUnits();
				}
			}
			Utility.sort(specs);
		}
		if (model.getNumParameters() > 0) {
			String[] params = new String[(int) model.getNumParameters()];
			for (int i = 0; i < model.getNumParameters(); i++) {
				Parameter parameter = (Parameter) model.getListOfParameters().get(i);
				if (parameter.getUnits().equals(origId)) {
					parameter.setUnits(newId);
				}
				if (parameter.isSetUnits()) {
					params[i] = parameter.getId() + " " + parameter.getValue() + " " + parameter.getUnits();
				}
				else {
					params[i] = parameter.getId() + " " + parameter.getValue();
				}
			}
			Utility.sort(params);
		}
		for (int i = 0; i < model.getNumReactions(); i++) {
			KineticLaw kineticLaw = (KineticLaw) model.getReaction(i).getKineticLaw();
			for (int j = 0; j < kineticLaw.getNumParameters(); j++) {
				if (kineticLaw.getParameter(j).getUnits().equals(origId)) {
					kineticLaw.getParameter(j).setUnits(newId);
				}
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		// if the add unit button is clicked
		if (e.getSource() == addUnit) {
			unitEditor("Add");
		}
		// if the edit unit button is clicked
		else if (e.getSource() == editUnit) {
			unitEditor("OK");
		}
		// if the remove unit button is clicked
		else if (e.getSource() == removeUnit) {
			removeUnit();
		}
		// if the add to unit list button is clicked
		else if (e.getSource() == addList) {
			unitListEditor("Add");
		}
		// if the edit unit list button is clicked
		else if (e.getSource() == editList) {
			unitListEditor("OK");
		}
		// if the remove from unit list button is clicked
		else if (e.getSource() == removeList) {
			removeList();
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			if (e.getSource() == unitList) {
				unitListEditor("OK");
			}
			else if (e.getSource() == unitDefs) {
				unitEditor("OK");
			}
		}
	}

	/**
	 * This method currently does nothing.
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mouseExited(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mousePressed(MouseEvent e) {
	}

	/**
	 * This method currently does nothing.
	 */
	public void mouseReleased(MouseEvent e) {
	}
}