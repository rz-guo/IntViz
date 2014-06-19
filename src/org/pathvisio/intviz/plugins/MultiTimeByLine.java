package org.pathvisio.intviz.plugins;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bridgedb.Xref;
import org.jdom.Element;
import org.pathvisio.core.model.MLine;
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
import org.pathvisio.intviz.plugins.MultiByLine.ConfiguredSample;
import org.pathvisio.visualization.gui.ColorSetChooser;
import org.pathvisio.visualization.gui.ColorSetCombo;
import org.pathvisio.visualization.plugins.SortSampleCheckList;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Visualization method for coloring line: 
 * create a slider to color the line base on each column of data
 * @author Ruizhou GUO
 */

public class MultiTimeByLine extends AbstractVisualizationMethod implements ActionListener,ChangeListener {
	
	static final String DEFAULT_MINDATAVALUE = "0";
	static final String DEFAULT_MAXDATAVALUE = "10";
	static final String DEFAULT_MINLINETHICKNESS = "1";
	static final String DEFAULT_MAXLINETHICKNESS = "7";
	static final String ACTION_GRADIENT = "Gradient";
	static final String ACTION_COMBO = "Colorset";
	static final String ACTION_CHANGE_LINE = "Change";
	static final String ACTION_LINETTHICKNESS = "Thickness";
	static final String DEFAULT_LABEL = "The selected time is :";
	
	
	String mindatavalue;
	String maxdatavalue;
	String minlinethickness;
	String maxlinethickness;
	private ColorSetCombo colorSetCombo;
	List<ConfiguredSample> useSamples = new ArrayList<ConfiguredSample>();
	private JSlider MulTslider;
	private JLabel SliderLabel;
	private int seletedSlider;
	private JTextField LineMaxTF;
	private JTextField LineMinTF;
	private JCheckBox LineCheckbox;
	private JButton changeLineButton;
	Color[] colors = {
            Color.white, Color.lightGray, Color.gray, Color.darkGray,
            Color.black, Color.red, Color.pink, Color.orange,
            Color.yellow, Color.green, Color.magenta, Color.cyan, Color.blue
        };
	
	private final GexManager gexManager;
	private final ColorSetManager csm;
	GexManager getGexManager() { return gexManager; }

	public MultiTimeByLine(GexManager gexManager,
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
				"4dlu, pref, 4dlu, pref, fill:pref:grow, 4dlu",
				"4dlu, pref, 4dlu, pref, pref,4dlu,pref,4dlu,pref,pref,pref"
				);
		panel.setLayout(layout);

		JRadioButton radioId = new JRadioButton(ACTION_GRADIENT);
		radioId.setActionCommand(ACTION_GRADIENT);
		radioId.addActionListener(this);

		ButtonGroup group = new ButtonGroup();
		group.add(radioId);
		
		LineCheckbox = new JCheckBox(ACTION_LINETTHICKNESS);
		LineCheckbox.setActionCommand(ACTION_LINETTHICKNESS);
		LineCheckbox.addActionListener(this);
		LineCheckbox.setSelected(true);

		SortSampleCheckList sampleList;
		List<ISample> selected = getSelectedSamples();
		for (ISample s : selected) if (s == null) throw new NullPointerException();
		sampleList = new SortSampleCheckList(
				selected, gexManager
		);

		LineMaxTF = new JTextField(DEFAULT_MAXLINETHICKNESS);
		LineMinTF = new JTextField(DEFAULT_MINLINETHICKNESS);
		changeLineButton = new JButton(ACTION_CHANGE_LINE);
		changeLineButton.setActionCommand(ACTION_CHANGE_LINE);
		changeLineButton.addActionListener(this);
		
		ColorSetChooser csChooser = new ColorSetChooser(csm, gexManager);
		colorSetCombo = csChooser.getColorSetCombo();
		colorSetCombo.setActionCommand(ACTION_COMBO);
		colorSetCombo.addActionListener(this);
		
		//get the length of slider
		useSamples = new ArrayList<ConfiguredSample>();
		for(ISample s : sampleList.getList().getSamplesInOrder()) {
			ConfiguredSample cs = new ConfiguredSample(s);

			cs.setColorSet(colorSetCombo.getSelectedColorSet());
			useSamples.add(cs);
		}
		int sliderlength = useSamples.size();
		if (sliderlength != 0){
			MulTslider=new JSlider(0,sliderlength - 1);
		} else {
			MulTslider=new JSlider(0,0);
		}
		MulTslider.addChangeListener(this);
		//default select the first one of data
		seletedSlider = 0;
		SliderLabel = new JLabel(DEFAULT_LABEL+useSamples.get(seletedSlider).getSample().getName());
		MulTslider.setValue(seletedSlider);
		
		CellConstraints cc = new CellConstraints();
		panel.add(radioId, cc.xy(2, 2));
		panel.add(LineCheckbox, cc.xy(4, 2));
		panel.add(SliderLabel, cc.xy(2,4));
		panel.add(MulTslider, cc.xyw(2, 5, 4));
		panel.add(csChooser, cc.xyw(2, 7, 4));
		panel.add(new JLabel("Line minimun thickess:"),cc.xy(2,9));
		panel.add(LineMinTF,cc.xy(4,9));
		panel.add(new JLabel("Line maximum thickess:"),cc.xy(2,10));
		panel.add(LineMaxTF,cc.xy(4,10));
		panel.add(changeLineButton,cc.xy(2, 11));
		radioId.setSelected(true);

		return panel;
	}

	@Override
	public int defaultDrawingOrder() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Multi-time visulization with slider";
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Multi-time visulization";
	}

	@Override
	public void visualizeOnDrawing(Graphics g, Graphics2D g2d) {
		// TODO Auto-generated method stub
				if(g instanceof Line)
				{
					if(useSamples.size() == 0) return; //Nothing to draw
					final Line gp = (Line) g;
					g2d.setClip(null);
					
					ConfiguredSample s = useSamples.get(seletedSlider);
					@SuppressWarnings("deprecation")
					Xref idc = new Xref(gp.getPathwayElement().getGeneID(), gp
								.getPathwayElement().getDataSource());
					CachedData cache = gexManager.getCachedData();
						
					if (cache == null) {
						return;
					}

					if(cache.hasData(idc))
					{
						List<? extends IRow> data = cache.getData(idc);					
						if (data.size() > 0)
						{
							drawSample(s, data, gp, g2d);
						}
					}
					else
					{
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
	
	private void drawSample(ConfiguredSample s, List<? extends IRow> data,
			Line gp, Graphics2D g2d) {
		
		IRow dataval = data.get(0);
		ISample sample = s.getSample();
		ColorSet cs = s.getColorSet();
		Color rgb = cs.getColor(dataval, sample);
		drawColoredLine(gp, rgb, g2d, dataval, sample);
	}
	
	private void drawColoredLine(Line gp, Color rgb, Graphics2D g2d, IRow dataval, ISample sample){
		g2d.setPaint(rgb);
		g2d.setColor(rgb);
		
		double datavalue = (Double) dataval.getSampleData(sample);
		
		if (datavalue < 0){
			datavalue = (-1) * datavalue;
		}
		
		//default line thickness
		float lt = 2;
		if (LineCheckbox.isSelected()){
			lt = setLineThickness((float) datavalue);
		}
		int ls = gp.getPathwayElement().getLineStyle();
		if (ls == 0) {
			g2d.setStroke(new BasicStroke(lt));
		} else if (ls == 1) {
			g2d.setStroke(new BasicStroke(lt, BasicStroke.CAP_SQUARE,
					BasicStroke.JOIN_MITER, 10, new float[] { 4, 4 }, 0));
		}
		g2d.setStroke(new BasicStroke(lt));
		g2d.draw(gp.getVConnectorAdjusted());
		/*wrong here, cannot get the start position and end position
		MLine ml = (MLine)gp.getVConnectorAdjusted();
		int sx = (int)(ml.getStartPoint().getX());
		int sy = (int)(ml.getStartPoint().getY());
		int ex = (int)(ml.getEndPoint().getX());
		int ey = (int)(ml.getEndPoint().getY());
		g2d.drawLine(sx, sy, ex, ey);
		*/
		
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
	
	protected void setMinData(String minD) {
		if (minD !=null ){
			mindatavalue = minD;
			modified();
		}
	}

	private String getMinData(boolean b) {
		String minD = mindatavalue == null ? DEFAULT_MINDATAVALUE
				: mindatavalue;
		VPathway vp = getVisualization().getManager().getEngine()
				.getActiveVPathway();
		if (vp != null) {
			minD = new String(minD);
		}
		return minD;
	}

	protected void setMaxData(String maxD) {
		if (maxD !=null ){
			maxdatavalue = maxD;
			modified();
		}
	}

	private String getMaxData(boolean b) {
		String maxD = maxdatavalue == null ? DEFAULT_MAXDATAVALUE
				: maxdatavalue;
		VPathway vp = getVisualization().getManager().getEngine()
				.getActiveVPathway();
		if (vp != null) {
			maxD = new String(maxD);
		}
		return maxD;
	}
	
	protected void setMinThickness(String minT) {
		if (minT !=null ){
			minlinethickness = minT;
			modified();
		}

	}

	private String getMinThickness() {
		String minT = minlinethickness == null ? DEFAULT_MINLINETHICKNESS : minlinethickness;
		VPathway vp = getVisualization().getManager().getEngine().getActiveVPathway();
		if(vp != null) {
			minT = new String(minT);
		}
		return minT;
	}

	protected void setMaxThickness(String maxT) {
		if (maxT !=null ){
			maxlinethickness = maxT;
			modified();
		}

	}

	private String getMaxThickness() {
		String maxT = maxlinethickness == null ? DEFAULT_MAXLINETHICKNESS : maxlinethickness;
		VPathway vp = getVisualization().getManager().getEngine().getActiveVPathway();
		if(vp != null) {
			maxT = new String(maxT);
		}
		return maxT;
	}
	
	private float setLineThickness(float data) {
		minlinethickness = getMinThickness();
		maxlinethickness = getMaxThickness();
		maxdatavalue = getMaxData(true);
		mindatavalue = getMinData(true);
		float minthickness = Float.parseFloat(minlinethickness);
		float maxthickness = Float.parseFloat(maxlinethickness);
		float maxdata = Float.parseFloat(maxdatavalue);
		float mindata = Float.parseFloat(mindatavalue);

		if (maxdata < mindata) {
			maxdata = Float.parseFloat(mindatavalue);
			mindata = Float.parseFloat(maxdatavalue);
		}
		if (maxthickness < minthickness) {
			minthickness = Float.parseFloat(maxlinethickness);
			maxthickness = Float.parseFloat(minlinethickness);
		}

		float linethickness = (data * ((maxthickness - minthickness) / (maxdata - mindata)))
				+ minthickness;
		return linethickness;
	}

	@Override
	public Component visualizeOnToolTip(Graphics arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		// TODO Auto-generated method stub
		seletedSlider = MulTslider.getValue();
		if (useSamples.size() != 0){
			SliderLabel.setText(DEFAULT_LABEL+useSamples.get(seletedSlider).getSample().getName());
		}
		modified();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		String action = e.getActionCommand();
		if(ACTION_COMBO.equals(action))
		{
			//update color set
			if (colorSetCombo.getSelectedItem() != null)
			{
				setSingleColorSet(colorSetCombo.getSelectedColorSet());
			}
		}
		else if(ACTION_CHANGE_LINE.equals(action))
		{
			//update bar width and height
			try {
				int mid = Integer.parseInt(LineMaxTF.getText());
				if (mid > 0){
					setMaxThickness(LineMaxTF.getText());
				}	
			} catch (NumberFormatException ne) {
				LineMaxTF.setText(getMaxThickness());
			}
			
			try {
				int mid = Integer.parseInt(LineMinTF.getText());
				if (mid > 0){
					setMinThickness(LineMinTF.getText());
				}	
			} catch (NumberFormatException ne) {
				LineMinTF.setText(getMinThickness());
			}
		} else if (ACTION_LINETTHICKNESS.equals(action)) 
		{
			if (LineCheckbox.isSelected()){
				//use line thickness method
				LineMaxTF.setEnabled(true);
				LineMinTF.setEnabled(true);
				changeLineButton.setEnabled(true);
			} else
			{
				//do not use line thickness method
				LineMaxTF.setEnabled(false);
				LineMinTF.setEnabled(false);
				changeLineButton.setEnabled(false);
			}
			modified();
		}
	}
	
	/**
	 * Set a single colorset for all samples.
	 */
	public void setSingleColorSet(ColorSet cs) {
		for(ConfiguredSample s : useSamples) {
			s.setColorSet(cs);
		}
	}
	
	public List<ISample> getSelectedSamples() {
		List<ISample> samples = new ArrayList<ISample>();

		for(ConfiguredSample cs : useSamples)
		{
			samples.add(cs.getSample());
		}
		return samples;
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
