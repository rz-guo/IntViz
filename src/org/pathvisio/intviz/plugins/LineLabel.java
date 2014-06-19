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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.jdom.Element;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.util.ColorConverter;
import org.pathvisio.core.util.Utils;
import org.pathvisio.core.view.Graphics;
import org.pathvisio.core.view.Line;
import org.pathvisio.desktop.visualization.AbstractVisualizationMethod;
import org.pathvisio.gui.dialogs.OkCancelDialog;
import org.pathvisio.gui.util.FontChooser;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Visualization method to put a label on a Interaction
 */
public class LineLabel extends AbstractVisualizationMethod implements ActionListener {
	static final int SPACING = 5;
	static final String DISPLAY_ID = "Identifier";
	static final String DISPLAY_LABEL = "Text label";
	static final String ACTION_APPEARANCE = "Appearance...";

	static final Font DEFAULT_FONT = new Font("Arial narrow", Font.PLAIN, 10);
	String display = DISPLAY_LABEL;
	boolean adaptFontSize;
	Font font;
	Color fontColor;

	public LineLabel() {
		setIsConfigurable(true);
		setUseProvidedArea(false); //Overlay by default
	}

	@Override
	public String getDescription() {
		return "Draws a label for the edge [Interaction/Reaction]";
	}

	@Override
	public String getName() {
		return "Text label";
	}

	@Override
	public JPanel getConfigurationPanel() {
		JPanel panel = new JPanel();
		FormLayout layout = new FormLayout(
				"pref, 4dlu, pref, 4dlu, pref, 8dlu, pref",
				"pref"
				);
		panel.setLayout(layout);

		JRadioButton radioId = new JRadioButton(DISPLAY_ID);
		JRadioButton radioLabel = new JRadioButton(DISPLAY_LABEL);
		radioId.setActionCommand(DISPLAY_ID);
		radioLabel.setActionCommand(DISPLAY_LABEL);
		radioId.addActionListener(this);
		radioLabel.addActionListener(this);

		ButtonGroup group = new ButtonGroup();
		group.add(radioId);
		group.add(radioLabel);

		JButton appearance = new JButton(ACTION_APPEARANCE);
		appearance.setActionCommand(ACTION_APPEARANCE);
		appearance.addActionListener(this);

		CellConstraints cc = new CellConstraints();
		panel.add(new JLabel("Display: "), cc.xy(1, 1));
		panel.add(radioLabel, cc.xy(3, 1));
		panel.add(radioId, cc.xy(5, 1));
		panel.add(appearance, cc.xy(7, 1));

		//Initial values
		if(DISPLAY_ID.equals(display)) {
			radioId.setSelected(true);
		} else {
			radioLabel.setSelected(true);
		}
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		if(DISPLAY_ID.equals(action) || DISPLAY_LABEL.equals(action)) {
			setDisplayAttribute(action);
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


		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("pref, 4dlu, fill:pref:grow, 4dlu, pref", ""));
		builder.setDefaultDialogBorder();
		builder.append("Font: ", preview, font);
		builder.nextLine();

		return builder.getPanel();
	}

	void setDisplayAttribute(String display) {
		this.display = display;
		modified();
	}



	void setOverlay(boolean overlay) {
		setUseProvidedArea(!overlay);
		modified();
	}

	boolean getOverlay() { return !isUseProvidedArea(); }

	@Override
	public void visualizeOnDrawing(Graphics g, Graphics2D g2d) {
		if(g instanceof Line) {
			String label = getLabelText((Line) g);
			if (label == null || label.length() == 0)
				return;

			Font f = getFont();

			Point2D start = ((Line) g).getStartPoint();
			Point2D end = ((Line) g).getEndPoint();

			int startx = (int)(g.getVCenterX()+ SPACING);
			int starty = (int)(g.getVCenterY());

			if(start.getY() == end.getY()){
				startx = (int)(start.getX());
				starty = (int)(start.getY()- SPACING);
			}

			int th = g2d.getFontMetrics().getHeight();
			int w = 0;

			if(!getOverlay()) {
				g2d.setColor(Color.WHITE);
			}
			g2d.setColor(Color.BLACK);



			if(adaptFontSize) {
				//TODO: adapt font size for awt
				//f = SwtUtils.adjustFontSize(f, new Dimension(area.width, area.height), label, g2d);
			}
			g2d.setFont(f);

			g2d.setColor(getFontColor());

			TextLayout tl = new TextLayout(label, g2d.getFont(), g2d.getFontRenderContext());
			Rectangle2D tb = tl.getBounds();
			g2d.drawString(label, startx + w, starty + th / 2);
			w += tb.getWidth() + SPACING;

		}
	}

	@Override
	public Component visualizeOnToolTip(Graphics g) {
		// TODO Auto-generated method stub
		return null;
	}

	void setAdaptFontSize(boolean adapt) {
		adaptFontSize = adapt;
		modified();
	}

	void setFont(Font f) {
		if(f != null) {
			font = f;
			modified();
		}
	}

	void setFontColor(Color fc) {
		fontColor = fc;
		modified();
	}

	Color getFontColor() {
		return fontColor == null ? Color.BLACK : fontColor;
	}

	int getFontSize() {
		return getFont().getSize();
	}

	Font getFont() {
		return getFont(false);
	}

	Font getFont(boolean adjustZoom) {
		Font f = font == null ? DEFAULT_FONT : font;
		if(adjustZoom) {
			//int fs = (int)Math.ceil(Engine.getCurrent().getActiveVPathway().vFromM(f.getSize()));
			f = new Font(f.getName(), f.getStyle(), f.getSize());
		}
		return f;
	}

	private String getLabelText(Line l) {
		String text = l.getPathwayElement().getGeneID();
		return text;
	}

	static final String XML_ATTR_DISPLAY = "display";
	static final String XML_ATTR_ADAPT_FONT = "adjustFontSize";
	static final String XML_ATTR_FONTDATA = "font";
	static final String XML_ELM_FONTCOLOR = "font-color";
	static final String XML_ATTR_OVERLAY = "overlay";
	@Override
	public Element toXML() {
		Element elm = super.toXML();
		elm.setAttribute(XML_ATTR_DISPLAY, display);
		elm.setAttribute(XML_ATTR_ADAPT_FONT, Boolean.toString(adaptFontSize));

		elm.setAttribute(XML_ATTR_FONTDATA, Utils.encodeFont(getFont()));

		elm.addContent(ColorConverter.createColorElement(XML_ELM_FONTCOLOR, getFontColor()));
		elm.setAttribute(XML_ATTR_OVERLAY, Boolean.toString(getOverlay()));
		return elm;
	}

	@Override
	public void loadXML(Element xml) {
		super.loadXML(xml);

		String styleStr = xml.getAttributeValue(XML_ATTR_DISPLAY);
		String adaptStr = xml.getAttributeValue(XML_ATTR_ADAPT_FONT);
		String fontStr = xml.getAttributeValue(XML_ATTR_FONTDATA);
		String ovrStr = xml.getAttributeValue(XML_ATTR_OVERLAY);
		Element fcElm = xml.getChild(XML_ELM_FONTCOLOR);
		try {
			if (styleStr != null) {
				setDisplayAttribute(styleStr);
			}
			if (adaptStr != null) {
				adaptFontSize = Boolean.parseBoolean(adaptStr);
			}
			if (fontStr != null) {
				font = Font.decode(fontStr);
			}
			if (ovrStr != null) {
				setOverlay(Boolean.parseBoolean(ovrStr));
			}
			if (fcElm != null) {
				fontColor = ColorConverter.parseColorElement(fcElm);
			}

		} catch(NumberFormatException e) {
			Logger.log.error("Unable to load configuration for " + getName(), e);
		}
	}

	@Override
	public int defaultDrawingOrder()
	{
		return 1;
	}
}