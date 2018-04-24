import proxy.Route;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.AbstractSpinnerModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpinnerModel;
import javax.swing.Spring;
import javax.swing.SpringLayout;

public class AddressPanel extends JPanel implements StatusChangeListener {
	private JTextField addr;
	private JSpinner port;
	private SpinnerModel defaultModel;
	private Route r = null;
	private boolean enabled = true;

	private static SpinnerModel emptyModel = new AbstractSpinnerModel() {
		@Override
		public Object getValue() { return null; }

		@Override
		public void setValue(Object value) {}

		@Override
		public Object getNextValue() { return null; }

		@Override
		public Object getPreviousValue() { return null; } 
	};

	public AddressPanel() {
		super(new SpringLayout());
		SpringLayout layout = (SpringLayout)getLayout();
		port = new JSpinner(defaultModel = new SpinnerNumberModel(1024, 1024, 65535, 1));
		SpringLayout.Constraints cons = layout.getConstraints(port);
		cons.setWidth(Spring.constant(60));
		layout.putConstraint(SpringLayout.NORTH, port, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, port, 0, SpringLayout.EAST, this);
		port.addChangeListener(e -> {
			if (listener != null) listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, port.getValue().toString()));
		});
		add(port);
		JLabel jlbl = new JLabel(":");
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, jlbl, 0, SpringLayout.VERTICAL_CENTER, port);
		layout.putConstraint(SpringLayout.EAST, jlbl, 0, SpringLayout.WEST, port);
		add(jlbl);
		addr = new JTextField(10);
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, addr, 0, SpringLayout.VERTICAL_CENTER, port);
		layout.putConstraint(SpringLayout.EAST, addr, 0, SpringLayout.WEST, jlbl);
		layout.putConstraint(SpringLayout.WEST, addr, 0, SpringLayout.WEST, this);
		addr.addActionListener(e -> {
			if (listener != null) listener.actionPerformed(e);
		});
		addr.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				if (listener != null) listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, addr.getText()));
			}
		});
		add(addr);
		layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, port);
		Config.addStatusChangeListener(this);
		setRoute(null);
	}

	public Route getRoute() {
		if (r == null || r == Config.MFG4OOI || r == Route.DIRECT || r == Route.SYSTEM) return r;
		if (((Number)port.getValue()).intValue() != r.getPort() || !addr.getText().equals(r.getHost())) r = new Route(addr.getText(), ((Number)port.getValue()).intValue());
		return r;
	}

	public void setRoute(Route r) {
		if (r == Route.SYSTEM) this.r = null; // 强制刷新系统代理
		if (r == this.r) return;
		this.r = r;
		if (r == null || r == Route.DIRECT || r == Route.SYSTEM && r.getHost() == null) {
			enabled = false;
			addr.setText(null);
			port.setModel(emptyModel);
		} else if (r == Config.MFG4OOI || r == Route.SYSTEM) {
			enabled = false;
			addr.setText(r.getHost());
			port.setModel(defaultModel);
			port.setValue(r.getPort());
		} else {
			enabled = true;
			addr.setText(r.getHost());
			port.setModel(defaultModel);
			port.setValue(r.getPort());
		}
		addr.setEnabled(enabled && isEnabled());
		port.setEnabled(enabled && isEnabled());
	}

	// 由于懒，只写一个，而且get什么也不想写了
	private ActionListener listener = null;

	public void setActionListener(ActionListener listener) {
		this.listener = listener;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		addr.setEnabled(this.enabled && enabled);
		port.setEnabled(this.enabled && enabled);
	}

	@Override
	public void statusChanged() {
		if (r == Config.MFG4OOI) port.setValue(r.getPort());
	}
}
