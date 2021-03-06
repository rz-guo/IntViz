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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.jdom.Element;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.util.Utils;
import org.pathvisio.core.view.Graphics;
import org.pathvisio.core.view.Line;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.data.IRow;
import org.pathvisio.data.ISample;
import org.pathvisio.desktop.gex.CachedData;
import org.pathvisio.desktop.gex.GexManager;
import org.pathvisio.desktop.gex.ReporterData;
import org.pathvisio.desktop.visualization.AbstractVisualizationMethod;
import org.pathvisio.gui.dialogs.OkCancelDialog;
import org.pathvisio.gui.util.FontChooser;
import org.pathvisio.visualization.plugins.SortSampleCheckList;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Visualization method where data is represented as text,
 * i.e. the actual value written out near the interaction
 * @author anwesha
 */
public class TextByLine extends AbstractVisualizationMethod
 implements
		ActionListener, ListDataListener {
	static final Font DEFAULT_FONT = new Font("Arial narrow", Font.PLAIN, 10);
	static final int SPACING = 20;
	static final String ACTION_SAMPLE = "sample";
	static final String ACTION_APPEARANCE = "Appearance...";

	final static String SEP = ", ";
	int roundTo = 2;
	boolean mean = false;

	Font font;
	List<ISample> useSamples = new ArrayList<ISample>();

	private final GexManager gexManager;

	public TextByLine(GexManager gexManager) {
		this.gexManager = gexManager;
		setIsConfigurable(true);
		setUseProvidedArea(false);
	}

	@Override
	public String getDescription() {
		return "Display a numerical value next to an Edge(Interaction/Reaction)";
	}

	@Override
	public String getName() {
		return "Data as numerical value";
	}

	@Override
	public void visualizeOnDrawing(Graphics g , Graphics2D g2d) {
		if(g instanceof Line) {
			Line gp = (Line) g;

			CachedData  cache = gexManager.getCachedData();
			if (cache == null) return;

			String id = gp.getPathwayElement().getGeneID();
			DataSource ds = gp.getPathwayElement().getDataSource();
			Xref idc = new Xref(id, ds);

			if (cache == null || !cache.hasData(idc) || useSamples.size() == 0)
				return;

			g2d = (Graphics2D)g2d.create();
			g2d.setClip(null);


			Point2D start = ((Line) g).getStartPoint();
			Point2D end = ((Line) g).getEndPoint();

			int startx = (int)(g.getVCenterX()- SPACING);
			int starty = (int)(g.getVCenterY());

			if(start.getY() == end.getY()){
				startx = (int)(start.getX() - SPACING);
				starty = (int)(start.getY()+ SPACING);
			}

			Font f = getFont(true);
			g2d.setFont(f);
			int th = g2d.getFontMetrics().getHeight();
			int w = 0, i = 0;
			for(ISample s : useSamples) {
				String str = getDataString(s, cache.getData(idc), SEP + "\n") +
 (++i == useSamples.size() ? "" : SEP);
				if (str.length() == 0) {
					continue;
				}
				TextLayout tl = new TextLayout(str, f, g2d.getFontRenderContext());
				Rectangle2D tb = tl.getBounds();
				g2d.drawString(str, startx + w, starty + th / 2);
				w += tb.getWidth() - SPACING;
			}
		}
	}

	@Override
	public Component visualizeOnToolTip(Graphics g) {
		if(g instanceof Line) {
			Line gp = (Line) g;
			CachedData  cache = gexManager.getCachedData();

			Xref idc = new Xref(
					gp.getPathwayElement().getGeneID(),
					gp.getPathwayElement().getDataSource());

			if (!cache.hasData(idc) || useSamples.size() == 0)
				return null;

			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createTitledBorder("Expression data"));
			panel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = -1;
			for(ISample s : useSamples) {
				gbc.gridy++;
				gbc.gridx = 0;
				panel.add(new JLabel(getLabelLeftText(s)), gbc);
				gbc.gridx = 1;
				panel.add(new JLabel(getLabelRightText(s, cache.getData(idc))), gbc);
			}
			return panel;
		} else return null;
	}

	String getLabelLeftText(ISample s) {
		return s.getName() + ":";
	}

	String getLabelRightText(ISample s, List<? extends IRow> list) {
		return getDataString(s, list, SEP);
	}

	String getDataString(ISample s, List<? extends IRow> list, String multSep) {
		Object str = null;
		if (list.size() > 1) {
			str = formatData(getSampleStringMult(s, list, multSep));
		} else if (list.size() == 1) {
			str =  formatData(getSampleData(s, list.get(0)));
		}
		return str == null ? "" : str.toString();
	}

	Object getSampleData(ISample s, IRow iRow) {
		return iRow.getSampleData(s);
	}

	Object getSampleStringMult(ISample s, List<? extends IRow> list, String sep) {
		if(mean) return ReporterData.createListSummary(list).getSampleData(s);

		StringBuilder strb = new StringBuilder();
		for(IRow d : list) {
			String str = "" + formatData(d.getSampleData(s));
			if(!str.equals("NaN")) {
				strb.append(str + sep);
			}
		}
		return strb.length() > sep.length() ? strb.substring(0, strb.length() - sep.length()) : strb;
	}

	Object formatData(Object data) {
		if(data instanceof Double) {
			double d = (Double)data;

			if(Double.isNaN(d)) return "NaN";

			int dec = (int)Math.pow(10, getRoundTo());
			double rounded = (double)(Math.round(d * dec)) / dec;
			data = dec == 1 ? Integer.toString((int)rounded) : Double.toString(rounded);
		}
		return data;
	}

	void setFont(Font f) {
		if(f != null) {
			font = f;
			modified();
		}
	}

	Font getFont() {
		return getFont(false);
	}

	Font getFont(boolean adjustZoom) {
		Font f = font == null ? DEFAULT_FONT : font;
		if(adjustZoom) {
			VPathway vp = getVisualization().getManager().getEngine().getActiveVPathway();
			if(vp != null) {
				int size = (int)Math.ceil(vp.vFromM(f.getSize()));
				f = new Font(f.getName(), f.getStyle(), size);
			}
		}
		return f;
	}

	public int getRoundTo() { return roundTo; }

	public void setRoundTo(int dec) {
		if(dec >= 0 && dec < 10) {
			roundTo = dec;
			modified();
		}
	}

	public void setCalcMean(boolean doCalcMean) {
		mean = doCalcMean;
		modified();
	}

	SortSampleCheckList sampleList;

	@Override
	public JPanel getConfigurationPanel() {
		JPanel panel = new JPanel();
		FormLayout layout = new FormLayout(
"4dlu, pref, fill:pref:grow, 4dlu",
				"4dlu, pref, 4dlu, fill:pref:grow, 4dlu, pref, 4dlu");
		panel.setLayout(layout);

		sampleList = new SortSampleCheckList(useSamples, gexManager);

		sampleList.getList().setActionCommand(ACTION_SAMPLE);
		sampleList.getList().getModel().addListDataListener(this);
		sampleList.getList().addActionListener(this);

		JButton appearance = new JButton(ACTION_APPEARANCE);
		appearance.setActionCommand(ACTION_APPEARANCE);
		appearance.addActionListener(this);

		CellConstraints cc = new CellConstraints();
		panel.add(new JLabel("Select samples:"), cc.xyw(2, 2, 2));
		panel.add(sampleList, cc.xyw(2, 4, 2));
		panel.add(appearance, cc.xy(2, 6));
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		if(ACTION_SAMPLE.equals(action)) {
			refreshUseSamples();
		} else if(ACTION_APPEARANCE.equals(action)) {
			OkCancelDialog optionsDlg = new OkCancelDialog(
					null, ACTION_APPEARANCE, (Component)e.getSource(), true, false
);
			optionsDlg.setDialogComponent(createAppearancePanel());
			optionsDlg.pack();
			optionsDlg.setVisible(true);
		}
	}

	JPanel createAppearancePanel() {
		final JLabel preview = new JLabel(getFont().getFamily());
		preview.setOpaque(true);
		preview.setBackground(Color.WHITE);
		preview.setFont(getFont());

		final JButton font = new JButton("...");
		font.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Font f = FontChooser.showDialog(null, (Component)e.getSource(), getFont());
				if(f != null) {
					setFont(f);
					preview.setText(f.getFamily());
					preview.setFont(f);
				}
			}
		});

		SpinnerNumberModel model = new SpinnerNumberModel(getRoundTo(), 0, 10, 1);
		final JSpinner precision = new JSpinner(model);
		precision.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				setRoundTo((Integer)precision.getValue());
			}
		});

		DefaultFormBuilder builder = new DefaultFormBuilder(
				new FormLayout("pref, 4dlu, fill:pref:grow, 4dlu, pref", "")
);
		builder.setDefaultDialogBorder();
		builder.append("Font: ", preview, font);
		builder.nextLine();
		builder.append("Display precision:", precision, 3);
		return builder.getPanel();
	}

	private void refreshUseSamples() {
		useSamples = sampleList.getList().getSelectedSamplesInOrder();
		modified();
	}

	@Override
	public void contentsChanged(ListDataEvent e) {
		refreshUseSamples();
	}

	@Override
	public void intervalAdded(ListDataEvent e) {
		refreshUseSamples();
	}

	@Override
	public void intervalRemoved(ListDataEvent e) {
		refreshUseSamples();
	}

	static final String XML_ATTR_FONTDATA = "font";
	static final String XML_ATTR_AVG = "mean";
	static final String XML_ATTR_ROUND = "round-to";
	static final String XML_ELM_ID = "sample-id";
	@Override
	public Element toXML() {
		Element elm = super.toXML();
		elm.setAttribute(XML_ATTR_FONTDATA, Utils.encodeFont(getFont()));
		elm.setAttribute(XML_ATTR_ROUND, Integer.toString(getRoundTo()));
		elm.setAttribute(XML_ATTR_AVG, Boolean.toString(mean));
		for(ISample s : useSamples) {
			Element selm = new Element(XML_ELM_ID);
			selm.setText(Integer.toString(s.getId()));
			elm.addContent(selm);
		}
		return elm;
	}

	@Override
	public void loadXML(Element xml) {
		super.loadXML(xml);
		for(Object o : xml.getChildren(XML_ELM_ID)) {
			try {
				int id = Integer.parseInt(((Element)o).getText());
				ISample s = gexManager.getCurrentGex().getSample(id);
				if (s != null) {
					useSamples.add(s);
				}
			} catch(Exception e) { Logger.log.error("Unable to add sample", e); }
		}
		roundTo = Integer.parseInt(xml.getAttributeValue(XML_ATTR_ROUND));
		setFont(Font.decode(xml.getAttributeValue(XML_ATTR_FONTDATA)));
		mean = Boolean.parseBoolean(xml.getAttributeValue(XML_ATTR_AVG));
	}

	@Override
	public int defaultDrawingOrder()
	{
		// a high drawing order, so that is comes on top of opaque methods.
		return 3;
	}
}