import proxy.Route;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

public class RouteSelector extends JComboBox<String> {
	private AddressPanel addr;
	private Route[] selections;

	public RouteSelector(Route... selections) {
		super();
		addr = new AddressPanel();
		addr.setActionListener(e -> {
			ActionListener[] listeners = getActionListeners();
			e.setSource(this);
			for (int i = 0; i < listeners.length; ++i) listeners[i].actionPerformed(e);
		});
		addr.setRoute(selections[0]);
		String[] names = new String[selections.length];
		for (int i = 0; i < selections.length; ++i) if (selections[i] == Config.MFG4OOI) names[i] = "MFG4OOI";
		else if (selections[i] == Route.SYSTEM) names[i] = "系统代理";
		else if (selections[i] == Route.DIRECT) names[i] = "直接连接";
		else names[i] = "指定代理";
		setModel(new DefaultComboBoxModel<String>(names));
		this.selections = selections;
	}

	@Override
	protected void fireActionEvent() {
		Route r = selections[getSelectedIndex()];
		if (r != Config.MFG4OOI && r != Route.SYSTEM && r != Route.DIRECT) {
			if (!Config.MFG4OOI.equals(r)) r = selections[getSelectedIndex()] = new Route("localhost", Config.getPort());
		}
		addr.setRoute(r);
		super.fireActionEvent();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		addr.setEnabled(enabled);
	}

	public AddressPanel getAddressPanel() { return addr; }

	public Route getRoute() { return addr.getRoute(); }

	public void setRoute(Route r) {
		addr.setRoute(r);
		if (r == Config.MFG4OOI || r == Route.SYSTEM || r == Route.DIRECT) {
			for (int i = 0; i < selections.length; ++i) if (r == selections[i]) {
				if (getSelectedIndex() != i) setSelectedIndex(i);
				break;
			}
		} else {
			for (int i = 0; i < selections.length; ++i) if (selections[i] != Config.MFG4OOI && selections[i] != Route.SYSTEM && selections[i] != Route.DIRECT) {
				if (getSelectedIndex() != i) setSelectedIndex(i);
				break;
			}
		}
	}
}
