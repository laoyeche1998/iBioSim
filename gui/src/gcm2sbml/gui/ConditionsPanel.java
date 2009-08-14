package gcm2sbml.gui;

import gcm2sbml.parser.GCMFile;
import gcm2sbml.util.Utility;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import biomodelsim.BioSim;

public class ConditionsPanel extends JPanel {
	public ConditionsPanel(String selected, PropertyList conditionList, GCMFile gcm,
			boolean paramsOnly, BioSim biosim) {
		super(new GridLayout(8, 1));
		this.selected = selected;
		this.conditionList = conditionList;
		this.gcm = gcm;
		this.paramsOnly = paramsOnly;
		this.biosim = biosim;

		fields = new HashMap<String, PropertyField>();

		// Condition field
		PropertyField field = new PropertyField("Condition", "", null, null, "Condition",
				paramsOnly);
		if (paramsOnly) {
			field.setEnabled(false);
		}
		fields.put("Condition", field);
		add(field);

		String oldName = null;
		if (selected != null) {
			oldName = selected;
			fields.get("Condition").setValue(selected);
		}

		boolean display = false;
		while (!display) {
			display = openGui(oldName);
		}
	}

	private boolean openGui(String oldName) {
		int value = JOptionPane.showOptionDialog(biosim.frame(), this, "Condition Editor",
				JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		if (value == JOptionPane.YES_OPTION) {
			if (oldName == null) {
				if (gcm.getConditions().containsKey(fields.get("Condition").getValue())) {
					Utility.createErrorMessage("Error", "Condition already exists.");
					return false;
				}
			}
			else if (!oldName.equals(fields.get("Condition").getValue())) {
				if (gcm.getConditions().containsKey(fields.get("Condition").getValue())) {
					Utility.createErrorMessage("Error", "Condition already exists.");
					return false;
				}
			}
			String id = fields.get("Condition").getValue();

			if (selected != null && !oldName.equals(id)) {
				gcm.removeCondition(oldName);
			}
			if (gcm.addCondition(id)) {
				conditionList.removeItem(oldName);
				conditionList.addItem(id);
				conditionList.setSelectedValue(id, true);
			}
			else {
				Utility.createErrorMessage("Error", "Illegal values entered.");
				return false;
			}

		}
		else if (value == JOptionPane.NO_OPTION) {
			// System.out.println();
			return true;
		}
		return true;
	}

	public void actionPerformed(ActionEvent e) {
	}

	private void loadProperties(Properties property) {
		for (Object o : property.keySet()) {
			if (fields.containsKey(o.toString())) {
				fields.get(o.toString()).setValue(property.getProperty(o.toString()));
				fields.get(o.toString()).setCustom();
			}
			// if (o.equals(GlobalConstants.NAME)) {
			// fields.get("ID").setValue(
			// property.getProperty(o.toString()));
			// }
		}
	}

	private String[] options = { "Ok", "Cancel" };
	private HashMap<String, PropertyField> fields = null;
	private String selected = "";
	private GCMFile gcm = null;
	private PropertyList conditionList = null;
	private boolean paramsOnly;
	private BioSim biosim;
}
