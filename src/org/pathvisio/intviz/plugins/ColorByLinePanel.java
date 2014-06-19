// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2011 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.pathvisio.intviz.plugins;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.pathvisio.core.util.Resources;
import org.pathvisio.data.ISample;
import org.pathvisio.desktop.visualization.ColorSet;
import org.pathvisio.desktop.visualization.ColorSetManager;
import org.pathvisio.gui.dialogs.OkCancelDialog;
import org.pathvisio.intviz.plugins.ColorByLine.ConfiguredSample;
import org.pathvisio.visualization.gui.ColorSetChooser;
import org.pathvisio.visualization.gui.ColorSetCombo;
import org.pathvisio.visualization.plugins.SortSampleCheckList;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Configuration panel for the ColorByLine visualization method
 * 
 * @author anwesha
 */
public class ColorByLinePanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;

	static final String ACTION_BASIC = "Basic";
	static final String ACTION_GRADIENT = "Gradient";
	static final String ACTION_SAMPLE = "sample";
	static final String ACTION_OPTIONS = "options";
	static final String ACTION_COMBO = "Colorset";	
	static final String ACTION_LINETTHICKNESS = "Thickness";
	static final int BASIC_MODEL = 1;
	static final int GRADIENT_MODEL = 2;

	static final ImageIcon COLOR_PICK_ICON = new ImageIcon(
			Resources.getResourceURL("colorpicker.gif"));
	static final Cursor COLOR_PICK_CURS = Toolkit.getDefaultToolkit()
			.createCustomCursor(COLOR_PICK_ICON.getImage(), new Point(4, 19),
					"Color picker");

	private final ColorByLine method;
	private final Basic basic;
	private final Gradient gradient;
	private final CardLayout cardLayout;
	private final JPanel settings;
	private final ColorSetManager csm;
	private JCheckBox LineCheckbox;
	private int model;
	JButton options = new JButton("Set Visualization");


	public ColorByLinePanel(ColorByLine method, ColorSetManager csm) {
		this.method = method;
		this.csm = csm;

		model = BASIC_MODEL;
		
		setLayout(new FormLayout(
				"4dlu, pref, 4dlu, pref, fill:pref:grow, 4dlu",
				"4dlu, pref, 4dlu, fill:pref:grow, 4dlu"
				));

		ButtonGroup buttons = new ButtonGroup();
		JRadioButton rbBasic = new JRadioButton(ACTION_BASIC);
		rbBasic.setActionCommand(ACTION_BASIC);
		rbBasic.addActionListener(this);
		buttons.add(rbBasic);
		
		//add the gradient method by ruizhou guo
		JRadioButton rgBasic = new JRadioButton(ACTION_GRADIENT);
		rgBasic.setActionCommand(ACTION_GRADIENT);
		rgBasic.addActionListener(this);
		buttons.add(rgBasic);
		
		//select if use line thickness method
		LineCheckbox = new JCheckBox(ACTION_LINETTHICKNESS);
		LineCheckbox.setActionCommand(ACTION_LINETTHICKNESS);
		LineCheckbox.addActionListener(this);
		LineCheckbox.setSelected(true);

		CellConstraints cc = new CellConstraints();
		add(rbBasic, cc.xy(2, 2));
		add(rgBasic, cc.xy(4, 2));
		add(LineCheckbox, cc.xy(5, 2));

		settings = new JPanel();
		settings.setBorder(BorderFactory.createEtchedBorder());
		cardLayout = new CardLayout();
		settings.setLayout(cardLayout);

		basic = new Basic();
		gradient = new Gradient();

		settings.add(basic, ACTION_BASIC);
		settings.add(gradient, ACTION_GRADIENT);

		add(settings, cc.xyw(2, 4, 4));

		rbBasic.doClick();

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		if (ACTION_BASIC.equals(action)) {
			model = BASIC_MODEL;
			basic.refresh();
			basic.setModel(BASIC_MODEL);
			basic.refreshSamples();
			cardLayout.show(settings, action);
		} else if (ACTION_GRADIENT.equals(action)) 
		{
			model = GRADIENT_MODEL;
			gradient.refresh();
			gradient.setModel(GRADIENT_MODEL);
			gradient.refreshSamples();
			cardLayout.show(settings, action);
		} else if (ACTION_LINETTHICKNESS.equals(action)) 
		{
			if (BASIC_MODEL == model){
				basic.setThicknessSelect(LineCheckbox.isSelected());
			} else if (GRADIENT_MODEL == model)
			{
				gradient.setThicknessSelect(LineCheckbox.isSelected());
			}
		}
	}

	/** Panel for editing colorByLine */
	class Basic extends JPanel implements ActionListener, ListDataListener {
		private static final long serialVersionUID = 1L;

		private final SortSampleCheckList sampleList;

		ColorSetCombo colorSetCombo;

		public Basic() {
			setLayout(new FormLayout(
					"4dlu, pref, 2dlu, fill:pref:grow, 4dlu",
					"4dlu, pref:grow, 4dlu, pref, 4dlu"
					));

			List<ISample> selected = method.getSelectedSamples();
			for (ISample s : selected) if (s == null) throw new NullPointerException();
			sampleList = new SortSampleCheckList(
					selected, method.getGexManager()
					);
			sampleList.getList().addActionListener(this);
			sampleList.getList().setActionCommand(ACTION_SAMPLE);
			sampleList.getList().getModel().addListDataListener(this);

			options.addActionListener(this);
			options.setActionCommand(ACTION_OPTIONS);

			CellConstraints cc = new CellConstraints();
			add(sampleList, cc.xyw(2, 2, 3));
			add(options, cc.xy(2, 4));		
			refresh();

		}

		void refresh() {
			sampleList.getList().setSelectedSamples(method.getSelectedSamples());
		}
		
		void setModel(int model){
			method.setModel(model);
		}
		
		void setThicknessSelect(boolean selected){
			method.setThicknessSelect(selected);
			refreshSamples();
		}

		private void refreshSamples() {
			List<ConfiguredSample> csamples = new ArrayList<ConfiguredSample>();
			for(ISample s : sampleList.getList().getSelectedSamplesInOrder()) {
				ConfiguredSample cs = method.new ConfiguredSample(s);
				cs.setColorSet(method.getSingleColorSet());
				csamples.add(cs);
			}
			method.setUseSamples(csamples);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String action = e.getActionCommand();
			if(ACTION_SAMPLE.equals(action)) {
				refreshSamples();
				method.setSingleColorSet(new ColorSet(csm));
			}
			else if (ACTION_OPTIONS.equals(action)){
				OkCancelDialog optionsDlg = new OkCancelDialog(
						null, ACTION_OPTIONS, (Component)e.getSource(), true, false
						);
				optionsDlg.setDialogComponent(createAppearancePanel());
				optionsDlg.pack();
				optionsDlg.setVisible(true);
			}
		}

		/**
		 * Creates subpanel to set colours for positive and negative data and
		 * minimum and maximum linthickness to create a line thickness gradient
		 * 
		 * @return
		 */
		public JPanel createAppearancePanel(){

			final JLabel colorLabel = new JLabel("Color of line : ");
			final JLabel poscolorLabel = new JLabel("Positive data");
			final JLabel negcolorLabel = new JLabel("Negative data");
			final JLabel thicknessLabel = new JLabel("Thickness Gradient : ");
			final JLabel minthicknesslabel = new JLabel("Minimum thickness");
			final JLabel maxthicknesslabel = new JLabel("Maximum thickness");
			final String[] gradientStrings = {"1","2","3","4","5","6","7","8","9","10"};

			final JButton poscolorButton = new JButton("...");
			final JButton negcolorButton = new JButton("...");

			final JComboBox minthicknessbox = new JComboBox(gradientStrings);
			final JComboBox maxthicknessbox = new JComboBox(gradientStrings);

			final JTextField minvalue = new JTextField(5);
			minvalue.setEditable(true);
			final JTextField maxvalue = new JTextField(5);
			maxvalue.setEditable(true);

			/**
			 * Assigns default colours and values
			 */
			poscolorButton.setOpaque(true);
			poscolorButton.setForeground(Color.GREEN);
			poscolorButton.setBackground(Color.GREEN);
			negcolorButton.setOpaque(true);
			negcolorButton.setForeground(Color.RED);
			negcolorButton.setBackground(Color.RED);
			minthicknessbox.setSelectedIndex(0);
			maxthicknessbox.setSelectedIndex(7);
			minvalue.setText("1");
			maxvalue.setText("5");

			/**
			 * Action Listeners for buttons, combo boxes and text boxes to set
			 * colours, linethicknesses and values to be used for the
			 * visualization
			 */
			poscolorButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Color posColor = JColorChooser.showDialog(poscolorButton,
							"Choose", Color.GREEN);
					poscolorButton.setForeground(posColor);
					poscolorButton.setBackground(posColor);
					method.setPosColor(posColor);
				}
			});

			negcolorButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Color negColor = JColorChooser.showDialog(negcolorButton,
							"Choose", Color.RED);
					negcolorButton.setForeground(negColor);
					negcolorButton.setBackground(negColor);
					method.setNegColor(negColor);
				}
			});

			minthicknessbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox min = (JComboBox) e.getSource();
					String minthickness = (String) min.getSelectedItem();
					method.setMinThickness(minthickness);
				}
			});

			maxthicknessbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComboBox max = (JComboBox) e.getSource();
					String maxthickness = (String) max.getSelectedItem();
					method.setMaxThickness(maxthickness);
				}
			});

			maxvalue.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String maxdata = maxvalue.getText();
					method.setMaxData(maxdata);
				}
			});

			minvalue.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String mindata = minvalue.getText();
					method.setMinData(mindata);
				}
			});

			DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
					"pref, 4dlu, fill:pref:grow, 4dlu, pref", ""));
			builder.setDefaultDialogBorder();
			builder.append(colorLabel);
			builder.nextLine();
			builder.append(poscolorLabel, poscolorButton);
			builder.nextLine();
			builder.append(negcolorLabel, negcolorButton);
			builder.nextLine();
			builder.append(thicknessLabel);
			builder.nextLine();
			builder.append(minthicknesslabel, minthicknessbox, minvalue);
			builder.nextLine();
			builder.append(maxthicknesslabel, maxthicknessbox, maxvalue);
			return builder.getPanel();
		}

		@Override
		public void contentsChanged(ListDataEvent e) {
			refreshSamples();
		}

		@Override
		public void intervalAdded(ListDataEvent e) {
			refreshSamples();
		}

		@Override
		public void intervalRemoved(ListDataEvent e) {
			refreshSamples();
		}
	}

	/** Panel for editing GradientByLine by Ruizhou GUO*/
	class Gradient extends JPanel implements ActionListener, ListDataListener {
		private static final long serialVersionUID = 1L;

		private final SortSampleCheckList sampleList;

		ColorSetCombo colorSetCombo;

		public Gradient() {
			setLayout(new FormLayout(
					"4dlu, pref, 2dlu, fill:pref:grow, 4dlu",
					"4dlu, pref:grow, 4dlu, pref, 4dlu"
					));

			List<ISample> selected = method.getSelectedSamples();
			for (ISample s : selected) if (s == null) throw new NullPointerException();
			sampleList = new SortSampleCheckList(
					selected, method.getGexManager()
					);
			sampleList.getList().addActionListener(this);
			sampleList.getList().setActionCommand(ACTION_SAMPLE);
			sampleList.getList().getModel().addListDataListener(this);

			ColorSetChooser csChooser = new ColorSetChooser(csm, method.getGexManager());
			colorSetCombo = csChooser.getColorSetCombo();
			colorSetCombo.setActionCommand(ACTION_COMBO);
			colorSetCombo.addActionListener(this);
			
			CellConstraints cc = new CellConstraints();
			add(sampleList, cc.xyw(2, 2, 3));
			add(csChooser, cc.xyw(2, 4, 3));
			refresh();

		}

		void refresh() {
			sampleList.getList().setSelectedSamples(method.getSelectedSamples());	
			
		}
		
		void setModel(int model){
			method.setModel(model);
		}
		
		void setThicknessSelect(boolean selected){
			method.setThicknessSelect(selected);
			refreshSamples();
		}

		private void refreshSamples() {
			List<ConfiguredSample> csamples = new ArrayList<ConfiguredSample>();
			for(ISample s : sampleList.getList().getSelectedSamplesInOrder()) {
				ConfiguredSample cs = method.new ConfiguredSample(s);
				cs.setColorSet(colorSetCombo.getSelectedColorSet());
				csamples.add(cs);
			}
			method.setUseSamples(csamples);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String action = e.getActionCommand();
			if(ACTION_SAMPLE.equals(action)) {
				refreshSamples();
			}
			else if(ACTION_COMBO.equals(action))
			{
				//update color set
				if (colorSetCombo.getSelectedItem() != null)
				{
					method.setSingleColorSet(colorSetCombo.getSelectedColorSet());
				}
			}
			
		}

		@Override
		public void contentsChanged(ListDataEvent arg0) {
			// TODO Auto-generated method stub
			refreshSamples();
		}

		@Override
		public void intervalAdded(ListDataEvent arg0) {
			// TODO Auto-generated method stub
			refreshSamples();
		}

		@Override
		public void intervalRemoved(ListDataEvent arg0) {
			// TODO Auto-generated method stub
			refreshSamples();
		}
	}
}