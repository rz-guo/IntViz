package org.pathvisio.intviz.plugins;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.gex.GexManager.GexManagerEvent;
import org.pathvisio.desktop.gex.GexManager.GexManagerListener;
import org.pathvisio.desktop.plugin.Plugin;
import org.pathvisio.desktop.visualization.Visualization;
import org.pathvisio.desktop.visualization.VisualizationEvent;
import org.pathvisio.desktop.visualization.VisualizationManager;
import org.pathvisio.desktop.visualization.VisualizationMethod;
import org.pathvisio.desktop.visualization.VisualizationMethodProvider;
import org.pathvisio.desktop.visualization.VisualizationMethodRegistry;
import org.pathvisio.gui.MainPanel;
import org.pathvisio.intviz.gui.EdgeVisualizationDialog;




/**
 * Plugin that registers visualization method for Lines
 * @author anwesha
 */
public class IntVizPlugin implements Plugin{
	private PvDesktop desktop;
	private VisualizationComboModel model;

	public void init(PvDesktop aDesktop)
	{
		desktop = aDesktop;
		
		/**
		 * Register visualization methods 
		 */
		
		VisualizationMethodRegistry reg =
			aDesktop.getVisualizationManager().getVisualizationMethodRegistry();
		
		//register multi-time method by Ruizhou GUO
		reg.registerEdgeMethod(
				MultiTimeByLine.class.toString(),
				new VisualizationMethodProvider() {
					public VisualizationMethod create() {
						return new MultiTimeByLine(desktop.getGexManager(), desktop.getVisualizationManager().getColorSetManager());
					}
			}
		);
		
		//register multi-flux method by Ruizhou GUO
				reg.registerEdgeMethod(
						MultiByLine.class.toString(),
						new VisualizationMethodProvider() {
							public VisualizationMethod create() {
								return new MultiByLine(desktop.getGexManager(), desktop.getVisualizationManager().getColorSetManager());
							}
					}
				);
		
		// put gradient method in colorbyline also by Ruizhou GUO
		reg.registerEdgeMethod(
				ColorByLine.class.toString(),
				new VisualizationMethodProvider() {
					public VisualizationMethod create() {
						return new ColorByLine(desktop.getGexManager(), desktop.getVisualizationManager().getColorSetManager());
					}
			}
		);
		
		reg.registerEdgeMethod(
				TextByLine.class.toString(),
				new VisualizationMethodProvider() {
					public VisualizationMethod create() {
						return new TextByLine(desktop.getGexManager());
					}
			}
		);
		
		reg.registerEdgeMethod(
				LineLabel.class.toString(),
				new VisualizationMethodProvider() {
					public VisualizationMethod create() {
						return new LineLabel();
					}
			}
		);
		
		/**
		 * Register the EdgeVisualization plugin in the menu bar 
		 */
		desktop.registerMenuAction ("Data", new VisualizationAction(
				aDesktop)
		);
	}

	public void done()
	{
		model.dispose();
	};


	/**
	 * Action / Menu item for opening the visualization dialog
	 */
	public static class VisualizationAction extends AbstractAction implements GexManagerListener {
		private static final long serialVersionUID = 1L;
		MainPanel mainPanel;
		private final PvDesktop ste;

		public VisualizationAction(PvDesktop ste)
		{
			this.ste = ste;
			putValue(NAME, "Edge Visualization options");
			this.mainPanel = ste.getSwingEngine().getApplicationPanel();
			setEnabled(ste.getGexManager().isConnected());
			ste.getGexManager().addListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			new EdgeVisualizationDialog(
					ste.getVisualizationManager(),
					ste.getSwingEngine().getFrame(),
					mainPanel
			).setVisible(true);
		}

		public void gexManagerEvent(GexManagerEvent e)
		{
			boolean isConnected = ste.getGexManager().isConnected();
			setEnabled(isConnected);
		}
	}

	/**
	 * Model for ComboBox in toolbar, that selects you one of the
	 * visualizations contained in VisualizationManager, or "No Visualization"
	 */
	private static class VisualizationComboModel extends AbstractListModel
		implements ComboBoxModel, VisualizationManager.VisualizationListener
	{
		private static final String NO_VISUALIZATION = "No Visualization";
		private final VisualizationManager manager;
		VisualizationComboModel (VisualizationManager manager)
		{
			this.manager = manager;
			manager.addListener(this);
		}

		/**
		 * Call this to unregister from VisualizationManager
		 */
		public void dispose()
		{
			manager.removeListener(this);
		}

		public void visualizationEvent(VisualizationEvent e)
		{
			switch (e.getType())
			{
			case VisualizationEvent.VISUALIZATION_ADDED:
				fireIntervalAdded(this, 0, manager.getVisualizations().size() + 1);
				//TODO: smaller interval?
				break;
			case VisualizationEvent.VISUALIZATION_REMOVED:
				fireIntervalRemoved(this, 0, manager.getVisualizations().size() + 1);
				//TODO: smaller interval?
				break;
			case VisualizationEvent.VISUALIZATION_MODIFIED:
				fireContentsChanged(this, 0, manager.getVisualizations().size() + 1);
				//TODO: smaller interval?
				break;
			case VisualizationEvent.VISUALIZATION_SELECTED:
				fireContentsChanged(this, 0, manager.getVisualizations().size() + 1);
				break;
			}
		}

		public Object getSelectedItem()
		{
			Object result = manager.getActiveVisualization();
			if (result == null)
			{
				result = NO_VISUALIZATION;
			}
			return result;
		}

		public void setSelectedItem(Object arg0)
		{
			if (arg0 instanceof Visualization)
				manager.setActiveVisualization((Visualization)arg0);
			else
				manager.setActiveVisualization(-1);
		}

		public Object getElementAt(int arg0)
		{
			if (arg0 == 0)
			{
				return NO_VISUALIZATION;
			}
			else
			{
				return manager.getVisualizations().get(arg0-1);
			}
		}

		public int getSize() {
			return manager.getVisualizations().size() + 1;
		}
	}
}

