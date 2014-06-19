package org.pathvisio.intviz.plugins;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.bridgedb.Xref;
import org.jdom.Element;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.view.ArrowShape;
import org.pathvisio.core.view.Graphics;
import org.pathvisio.core.view.Line;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.data.DataException;
import org.pathvisio.data.IRow;
import org.pathvisio.data.ISample;
import org.pathvisio.desktop.gex.CachedData;
import org.pathvisio.desktop.gex.GexManager;
import org.pathvisio.desktop.gex.CachedData.Callback;
import org.pathvisio.desktop.visualization.AbstractVisualizationMethod;
import org.pathvisio.desktop.visualization.ColorSet;
import org.pathvisio.desktop.visualization.ColorSetManager;
import org.pathvisio.desktop.visualization.VisualizationManager.VisualizationException;
import org.pathvisio.visualization.gui.ColorSetChooser;
import org.pathvisio.visualization.gui.ColorSetCombo;
import org.pathvisio.visualization.plugins.SortSampleCheckList;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MultiByLine extends AbstractVisualizationMethod implements ActionListener, ListDataListener {

	static final String ACTION_BAR = "Bar";
	static final String ACTION_COLOR = "Gradient";
	static final String ACTION_THICK = "Thickness";
	static final String ACTION_LIST = "List";
	static final String ACTION_COMBO = "Colorset";	
	static final String ACTION_CHANGE_BAR = "Change";
	
	//Parameter use for drawing bar
	private int BarW = 50;
	private int BarH = 15;
	
	private List<ConfiguredSample> useSamples = new ArrayList<ConfiguredSample>();
	private SortSampleCheckList sampleList;
	private ColorSetCombo colorSetCombo;
	private JTextField BarWTF;
	private JTextField BarHTF;
	
	private final GexManager gexManager;
	private final ColorSetManager csm;
	GexManager getGexManager() { return gexManager; }
	public MultiByLine(GexManager gexManager,
			ColorSetManager csm) {
		// TODO Auto-generated constructor stub
		this.gexManager = gexManager;
		this.csm = csm;
		setIsConfigurable(true);
		setUseProvidedArea(false);
	}
	
	@Override
	public JPanel getConfigurationPanel() {
		JPanel panel = new JPanel();
		FormLayout layout = new FormLayout(
				"4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu,pref",
				"4dlu, pref, 4dlu, pref, 4dlu, pref,4dlu, pref"
				);
		panel.setLayout(layout);

		JRadioButton radioId = new JRadioButton(ACTION_BAR);
		radioId.setActionCommand(ACTION_BAR);
		radioId.addActionListener(this);
		
		JRadioButton radio2Id = new JRadioButton(ACTION_COLOR);
		radio2Id.setActionCommand(ACTION_COLOR);
		radio2Id.addActionListener(this);
		
		JRadioButton radio3Id = new JRadioButton(ACTION_THICK);
		radio3Id.setActionCommand(ACTION_THICK);
		radio3Id.addActionListener(this);
		
		List<ISample> selected = getSelectedSamples();
		for (ISample s : selected) if (s == null) throw new NullPointerException();
		sampleList = new SortSampleCheckList(
				selected, gexManager
		);
		sampleList.getList().setActionCommand(ACTION_LIST);
		sampleList.getList().getModel().addListDataListener(this);
		sampleList.getList().addActionListener(this);

		ButtonGroup group = new ButtonGroup();
		group.add(radioId);
		group.add(radio2Id);
		group.add(radio3Id);
		
		BarWTF = new JTextField(BarW+"");
		BarHTF = new JTextField(BarH+"");
		JButton changeBarButton = new JButton(ACTION_CHANGE_BAR);
		changeBarButton.setActionCommand(ACTION_CHANGE_BAR);
		changeBarButton.addActionListener(this);
		
		ColorSetChooser csChooser = new ColorSetChooser(csm, gexManager);
		colorSetCombo = csChooser.getColorSetCombo();
		colorSetCombo.setActionCommand(ACTION_COMBO);
		colorSetCombo.addActionListener(this);
		
		CellConstraints cc = new CellConstraints();
		panel.add(radioId, cc.xy(2, 2));
		panel.add(radio2Id, cc.xy(4, 2));
		panel.add(radio3Id, cc.xy(6, 2));
		panel.add(sampleList, cc.xyw(2, 4, 9));
		panel.add(new JLabel("Bar Width:"),cc.xy(2,6));
		panel.add(BarWTF,cc.xy(4,6));
		panel.add(new JLabel("    Height:"),cc.xy(6,6));
		panel.add(BarHTF,cc.xy(8,6));
		panel.add(changeBarButton,cc.xy(10, 6));
		panel.add(csChooser, cc.xyw(2, 8, 9));
		radioId.setSelected(true);

		return panel;
	}
	
	/**
	 * Set a single colorset for all samples.
	 */
	public void setSingleColorSet(ColorSet cs) {
		for(ConfiguredSample s : useSamples) {
			s.setColorSet(cs);
		}
	}

	/**
	 * Get the single colorset that is used for all
	 * samples. Returns null when different colorsets are
	 * used.
	 */
	public ColorSet getSingleColorSet() {
		ColorSet cs = null;
		for(ConfiguredSample s : useSamples) {
			if(cs == null) {
				cs = s.getColorSet();
			} else {
				if(cs != s.getColorSet()) {
					return null;
				}
			}
		}
		return cs;
	}

	/**
	 * Get the configured sample for the given sample. Returns
	 * null when no configured sample is found.
	 */
	public ConfiguredSample getConfiguredSample(ISample s) {
		for(ConfiguredSample cs : useSamples) {
			if(cs.getSample() != null && cs.getSample() == s) {
				return cs;
			}
		}
		return null;
	}
	
	public List<ConfiguredSample> getConfiguredSamples() {
		return useSamples;
	}

	public List<ISample> getSelectedSamples() {
		List<ISample> samples = new ArrayList<ISample>();

		for(ConfiguredSample cs : useSamples)
		{
			samples.add(cs.getSample());
		}
		return samples;
	}
	
	@Override
	public int defaultDrawingOrder() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "draw the selected flux data in same figure";
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Multi-flux in one figure";
	}

	@Override
	public void visualizeOnDrawing(Graphics g, Graphics2D g2d) {
		// TODO Auto-generated method stub
		if(g instanceof Line)
		{
			if(useSamples.size() == 0) return; //Nothing to draw
			Line gp = (Line) g;
			drawSample(gp,g2d);
		}
	}
	
	private void drawSample(final Line gp, Graphics2D g2d) {
		int nr = useSamples.size();
		g2d.setClip(null);
		//weight of each small bar 
		double wf = BarW / nr;
		//start position to draw bar
		int startx = (int)(gp.getVCenterX()- BarW/2);
		int starty = (int)(gp.getVCenterY()- BarH/2);
		for(int i = 0; i < nr; i++) 
		{
			//create the rectangle to draw: use for bar model
			Rectangle r = new Rectangle(
					(int)(startx+i*wf), starty,
					(int)wf,BarH);
			ConfiguredSample s = useSamples.get(i);
			@SuppressWarnings("deprecation")
			Xref idc = new Xref(gp.getPathwayElement().getGeneID(), gp
					.getPathwayElement().getDataSource());
			CachedData cache = gexManager.getCachedData();
			if(cache == null) continue;
			
			if(s.getColorSet() == null) {
				Logger.log.trace("No colorset for sample " + s);
				continue; //No ColorSet for this sample
			}
			
			if(cache.hasData(idc))
			{
				List<? extends IRow> data = cache.getData(idc);
				ColorSet cs = s.getColorSet();
				
				if (data.size() > 0)
				{
					Color rgb = cs.getColor(data.get(0), s.getSample());
					drawColoredRectangle(r,rgb,g2d);
				}
				else
				{
					//no data draw default color
					Color rgb = cs.getColor(cs.ID_COLOR_NO_DATA_FOUND);
					drawColoredRectangle(r,rgb,g2d);
				}
			}
			else
			{
				
				//drawColoredRectangle(r,NoDataColor,g2d);
				//the following use to refresh the cache data
				cache.asyncGet(idc, new Callback()
				{
					@Override
					public void callback()
					{
						gp.markDirty();
					}
				});
			}
		}

	}

	void drawColoredRectangle(Rectangle r, Color c, Graphics2D g2d) {
		g2d.setPaint(c);
		g2d.setColor(c);
		g2d.fill(r);

		g2d.setPaint(Color.BLACK);
		g2d.setColor(Color.BLACK);
		g2d.draw(r);
	}
	
	private void drawColoredLine(Line gp, Color rgb, Graphics2D g2d, ISample sample){
		g2d.setPaint(rgb);
		g2d.setColor(rgb);
		
		float lt = 1;
		g2d.setStroke(new BasicStroke(lt));
		g2d.draw(gp.getVConnectorAdjusted());
		
		ArrowShape[] heads = gp.getVHeadsAdjusted();
		ArrowShape hs = heads[0];
		ArrowShape he = heads[1];

		drawHead(g2d, he, lt, rgb);
		drawHead(g2d, hs, lt, rgb);
	}
	protected void drawHead(Graphics2D g2d, ArrowShape arrow, float lt, Color rgb)
	{
		if(arrow != null)
		{
			// reset stroked line to solid, but use given thickness
			g2d.setStroke(new BasicStroke(lt));

			switch (arrow.getFillType())
			{
			case OPEN:
				g2d.setPaint (Color.WHITE);
				g2d.setColor (rgb);
				g2d.draw (arrow.getShape());
				break;
			case CLOSED:
				g2d.setPaint (rgb);
				g2d.fill (arrow.getShape());
				g2d.draw (arrow.getShape());
				break;
			case WIRE:
				g2d.setColor (rgb);
				g2d.draw (arrow.getShape());
				break;
			default:
				assert (false);
			}
		}
	}
	
	
	
	
	private void refreshUseSamples() {
		useSamples = new ArrayList<ConfiguredSample>();
		for(ISample s : sampleList.getList().getSelectedSamplesInOrder()) {
			ConfiguredSample cs = new ConfiguredSample(s);

			cs.setColorSet(colorSetCombo.getSelectedColorSet());
			useSamples.add(cs);
		}
		modified();
	}

	@Override
	public Component visualizeOnToolTip(Graphics arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		String action = e.getActionCommand();
		if(ACTION_LIST.equals(action)) {
			refreshUseSamples();
		}
		else if(ACTION_COMBO.equals(action))
		{
			//update color set
			if (colorSetCombo.getSelectedItem() != null)
			{
				setSingleColorSet(colorSetCombo.getSelectedColorSet());
			}
		}
		else if(ACTION_CHANGE_BAR.equals(action))
		{
			//update bar width and height
			try {
				int mid = Integer.parseInt(BarWTF.getText());
				if (mid > 0){
					BarW = mid;
				}	
			} catch (NumberFormatException ne) {
				BarWTF.setText(BarW+"");
			}
			
			try {
				int mid = Integer.parseInt(BarHTF.getText());
				if (mid > 0){
					BarH = mid;
				}	
			} catch (NumberFormatException ne) {
				BarHTF.setText(BarH+"");
			}
			modified();

		}
	}
	@Override
	public void contentsChanged(ListDataEvent arg0) {
		// TODO Auto-generated method stub
		refreshUseSamples();
	}
	@Override
	public void intervalAdded(ListDataEvent arg0) {
		// TODO Auto-generated method stub
		refreshUseSamples();
	}
	@Override
	public void intervalRemoved(ListDataEvent arg0) {
		// TODO Auto-generated method stub
		refreshUseSamples();
	}
	
	/**
	 * This class stores the configuration for a sample that is selected for
	 * visualization. In this implementation, a color-set to use for
	 * visualization is stored. Extend this class to store additional
	 * configuration data.
	 */
	public class ConfiguredSample {

		ColorSet colorSet = new ColorSet(csm);

		int tolerance; // range 0 - 255;

		private ISample sample;

		public ISample getSample() {
			return sample;
		}

		public int getId() {
			return sample.getId();
		}

		static final String XML_ELEMENT = "sample";
		static final String XML_ATTR_ID = "id";
		static final String XML_ATTR_COLORSET = "colorset";

		private final Element toXML() {
			Element xml = new Element(XML_ELEMENT);
			xml.setAttribute(XML_ATTR_ID, Integer.toString(sample.getId()));
			xml.setAttribute(XML_ATTR_COLORSET, colorSet.getName());
			return xml;
		}

		private final void loadXML(Element xml) throws VisualizationException {
			int id = Integer.parseInt(xml.getAttributeValue(XML_ATTR_ID));

			String csn = xml.getAttributeValue(XML_ATTR_COLORSET);
			try {
				sample = gexManager.getCurrentGex().getSamples().get(id);
			} catch (DataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (sample == null)
				throw new VisualizationException("Couldn't find Sample with id " + id);

			setColorSet(getVisualization().getManager().getColorSetManager()
					.getColorSet(csn));
		}

		/**
		 * Create a configured sample based on an existing sample
		 * 
		 * @param s
		 *            The sample to base the configured sample on
		 */
		public ConfiguredSample(ISample s) {
			if (s == null)
				throw new NullPointerException();
			sample = s;
		}

		/**
		 * Create a configured sample from the information in the given XML
		 * element
		 * 
		 * @param xml
		 *            The XML element containing information to create the
		 *            configured sample from
		 * @throws VisualizationException
		 */
		public ConfiguredSample(Element xml) throws VisualizationException {
			loadXML(xml);
		}

		/**
		 * Set the color-set to use for visualization of this sample
		 */
		public void setColorSet(ColorSet cs) {
			colorSet = cs;
			modified();
		}

		/**
		 * Get the color-set to use for visualization of this sample
		 * @return the color-set
		 */
		protected ColorSet getColorSet() {
			return colorSet;
		}

		/**
		 * Get the name of the color-sets that is selected for visualization
		 * 
		 * @return The name of the selected color-set, or
		 *         "no colorsets available", if no color-sets exist
		 */
		protected String getColorSetName() {
			ColorSet cs = getColorSet();
			return cs == null ? "no colorsets available" : cs.getName();
		}
	}

}
