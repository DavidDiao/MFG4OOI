import proxy.Route;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ConfigFrame extends JFrame implements ActionListener, ChangeListener, FocusListener, WindowListener {
	private static ConfigFrame f = null;
	private static int restart;

	private JSpinner logLength, port, mfgPort;
	private JCheckBox startWithSystem, autoStart, minimizeToTray, minimizeAfterAuth, allowConfig;
	private JTextField mfgPath, bindAddr, mfgServer;
	private RouteSelector upstream, mfgProxy, mfgUpstream;
	private JPasswordField pass;

	private ConfigFrame() {
		super("配置");
		setResizable(false);
		addWindowListener(this);
		setLayout(new GridLayout(1, 3, 0, 0));
		SpringLayout layout = new SpringLayout();
		JPanel panel = new JPanel(layout);
		Border border = BorderFactory.createTitledBorder("基本配置");
		panel.setBorder(border);
		JLabel jlbl = new JLabel("日志长度（行）：");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		logLength = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, logLength, 0, SpringLayout.VERTICAL_CENTER, jlbl);
		layout.putConstraint(SpringLayout.WEST, logLength, 0, SpringLayout.EAST, jlbl);
		layout.putConstraint(SpringLayout.EAST, logLength, -8, SpringLayout.EAST, panel);
		panel.add(logLength);
		startWithSystem = new JCheckBox("开机自启动");
		layout.putConstraint(SpringLayout.NORTH, startWithSystem, 8, SpringLayout.SOUTH, logLength);
		layout.putConstraint(SpringLayout.WEST, startWithSystem, 8, SpringLayout.WEST, panel);
		startWithSystem.setEnabled(false);
		panel.add(startWithSystem);
		autoStart = new JCheckBox("服务自启动");
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, autoStart, 0, SpringLayout.VERTICAL_CENTER, startWithSystem);
		layout.putConstraint(SpringLayout.WEST, autoStart, 0, SpringLayout.HORIZONTAL_CENTER, panel);
		panel.add(autoStart);
		minimizeToTray = new JCheckBox("最小化到托盘");
		layout.putConstraint(SpringLayout.NORTH, minimizeToTray, 8, SpringLayout.SOUTH, startWithSystem);
		layout.putConstraint(SpringLayout.WEST, minimizeToTray, 8, SpringLayout.WEST, panel);
		minimizeToTray.setEnabled(Service.isSystemTraySupported());
		panel.add(minimizeToTray);
		minimizeAfterAuth = new JCheckBox("认证完成后自动最小化");
		layout.putConstraint(SpringLayout.NORTH, minimizeAfterAuth, 8, SpringLayout.SOUTH, minimizeToTray);
		layout.putConstraint(SpringLayout.WEST, minimizeAfterAuth, 8, SpringLayout.WEST, panel);
		panel.add(minimizeAfterAuth);
		jlbl = new JLabel("MFG启动命令");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.SOUTH, minimizeAfterAuth);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		mfgPath = new JTextField(22);
		layout.putConstraint(SpringLayout.NORTH, mfgPath, 8, SpringLayout.SOUTH, jlbl);
		layout.putConstraint(SpringLayout.WEST, mfgPath, 8, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, mfgPath, -8, SpringLayout.EAST, panel);
		panel.add(mfgPath);
		allowConfig = new JCheckBox("允许MFG4OOI编辑MFG配置");
		layout.putConstraint(SpringLayout.NORTH, allowConfig, 8, SpringLayout.SOUTH, mfgPath);
		layout.putConstraint(SpringLayout.WEST, allowConfig, 8, SpringLayout.WEST, panel);
		panel.add(allowConfig);
		add(panel);

		layout = new SpringLayout();
		panel = new JPanel(layout);
		border = BorderFactory.createTitledBorder("网络配置");
		panel.setBorder(border);
		jlbl = new JLabel("端口");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		port = new JSpinner(new SpinnerNumberModel(1024, 1024, 65535, 1));
		layout.putConstraint(SpringLayout.NORTH, port, 0, SpringLayout.NORTH, jlbl);
		layout.putConstraint(SpringLayout.WEST, port, 8, SpringLayout.EAST, jlbl);
		layout.putConstraint(SpringLayout.EAST, port, 0, SpringLayout.HORIZONTAL_CENTER, panel);
		panel.add(port);
		jlbl = new JLabel("上游代理");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.SOUTH, port);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		upstream = new RouteSelector(Route.SYSTEM, Route.DIRECT, new Route("localhost", 0));
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, upstream, 0, SpringLayout.VERTICAL_CENTER, jlbl);
		layout.putConstraint(SpringLayout.WEST, upstream, 8, SpringLayout.EAST, jlbl);
		panel.add(upstream);
		AddressPanel addr = upstream.getAddressPanel();
		layout.putConstraint(SpringLayout.NORTH, addr, 8, SpringLayout.SOUTH, upstream);
		layout.putConstraint(SpringLayout.WEST, addr, 8, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, addr, -8, SpringLayout.EAST, panel);
		panel.add(addr);
		jlbl = new JLabel("绑定地址");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.SOUTH, addr);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		bindAddr = new JTextField(15);
		layout.putConstraint(SpringLayout.NORTH, bindAddr, 8, SpringLayout.SOUTH, jlbl);
		layout.putConstraint(SpringLayout.WEST, bindAddr, 8, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, bindAddr, -8, SpringLayout.EAST, panel);
		panel.add(bindAddr);
		add(panel);

		layout = new SpringLayout();
		panel = new JPanel(layout);
		border = BorderFactory.createTitledBorder("MFG配置");
		panel.setBorder(border);
		jlbl = new JLabel("MFG服务器");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		mfgServer = new JTextField(15);
		layout.putConstraint(SpringLayout.NORTH, mfgServer, 8, SpringLayout.SOUTH, jlbl);
		layout.putConstraint(SpringLayout.WEST, mfgServer, 8, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, mfgServer, -8, SpringLayout.EAST, panel);
		panel.add(mfgServer);
		jlbl = new JLabel("MFG代理");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.SOUTH, mfgServer);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		mfgProxy = new RouteSelector(Route.DIRECT, new Route("localhost", 0));
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, mfgProxy, 0, SpringLayout.VERTICAL_CENTER, jlbl);
		layout.putConstraint(SpringLayout.WEST, mfgProxy, 8, SpringLayout.EAST, jlbl);
		panel.add(mfgProxy);
		addr = mfgProxy.getAddressPanel();
		layout.putConstraint(SpringLayout.NORTH, addr, 8, SpringLayout.SOUTH, mfgProxy);
		layout.putConstraint(SpringLayout.WEST, addr, 8, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, addr, -8, SpringLayout.EAST, panel);
		panel.add(addr);
		jlbl = new JLabel("MFG端口");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.SOUTH, addr);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		mfgPort = new JSpinner(new SpinnerNumberModel(1024, 1024, 65535, 1));
		layout.putConstraint(SpringLayout.NORTH, mfgPort, 0, SpringLayout.NORTH, jlbl);
		layout.putConstraint(SpringLayout.WEST, mfgPort, 8, SpringLayout.EAST, jlbl);
		layout.putConstraint(SpringLayout.EAST, mfgPort, -8, SpringLayout.EAST, panel);
		panel.add(mfgPort);
		jlbl = new JLabel("MFG上游代理");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.SOUTH, mfgPort);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		mfgUpstream = new RouteSelector(Config.MFG4OOI, Route.DIRECT, new Route("localhost", 0));
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, mfgUpstream, 0, SpringLayout.VERTICAL_CENTER, jlbl);
		layout.putConstraint(SpringLayout.WEST, mfgUpstream, 8, SpringLayout.EAST, jlbl);
		panel.add(mfgUpstream);
		addr = mfgUpstream.getAddressPanel();
		layout.putConstraint(SpringLayout.NORTH, addr, 8, SpringLayout.SOUTH, mfgUpstream);
		layout.putConstraint(SpringLayout.WEST, addr, 8, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, addr, -8, SpringLayout.EAST, panel);
		panel.add(addr);
		jlbl = new JLabel("MFG密码");
		layout.putConstraint(SpringLayout.NORTH, jlbl, 8, SpringLayout.SOUTH, addr);
		layout.putConstraint(SpringLayout.WEST, jlbl, 8, SpringLayout.WEST, panel);
		panel.add(jlbl);
		pass = new JPasswordField(20);
		layout.putConstraint(SpringLayout.NORTH, pass, 8, SpringLayout.SOUTH, jlbl);
		layout.putConstraint(SpringLayout.WEST, pass, 8, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, pass, -8, SpringLayout.EAST, panel);
		panel.add(pass);
		add(panel);
		updateConfig();
		logLength.addChangeListener(this);
		startWithSystem.addActionListener(this);
		autoStart.addActionListener(this);
		minimizeToTray.addActionListener(this);
		minimizeAfterAuth.addActionListener(this);
		mfgPath.addActionListener(this);
		mfgPath.addFocusListener(this);
		allowConfig.addActionListener(this);
		port.addChangeListener(this);
		upstream.addActionListener(this);
		bindAddr.addActionListener(this);
		bindAddr.addFocusListener(this);
		mfgServer.addActionListener(this);
		mfgServer.addFocusListener(this);
		mfgProxy.addActionListener(this);
		mfgPort.addChangeListener(this);
		mfgUpstream.addActionListener(this);
		pass.addActionListener(this);
		pass.addFocusListener(this);
		setSize(640, 400);
	}

	private void updateConfig() {
		boolean en = Config.isConfigAllowed();
		mfgServer.setEnabled(en);
		mfgProxy.setEnabled(en);
		mfgPort.setEnabled(en);
		mfgUpstream.setEnabled(en);
		pass.setEnabled(en);

		logLength.setValue(Config.getLogLength());
		port.setValue(Config.getPort());
		mfgPort.setValue(Config.getMFGPort());
		startWithSystem.setSelected(false);
		autoStart.setSelected(Config.isAutoStartServices());
		minimizeToTray.setSelected(Config.isMinimizeToSystemTray());
		minimizeAfterAuth.setSelected(Config.isAutoMinimizeAfterAuth());
		allowConfig.setSelected(en);
		mfgPath.setText(Config.getMFGPath());
		bindAddr.setText(Config.getBindAddress() == null ? null : Config.getBindAddress().getHostName());
		mfgServer.setText(Config.getMFGServer());
		upstream.setRoute(Config.getUpstreamRoute());
		mfgProxy.setRoute(Config.getMFGProxy());
		mfgUpstream.setRoute(Config.getMFGUpstream());
		pass.setText(Config.getPass());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		update(e.getSource());
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		update(e.getSource());
	}

	@Override
	public void focusGained(FocusEvent e) {}

	@Override
	public void focusLost(FocusEvent e) {
		update(e.getSource());
	}

	private void update(Object comp) {
		if (comp == logLength) restart |= Config.setLogLength(((Number)logLength.getValue()).intValue());
		else if (comp == port) restart |= Config.setPort(((Number)port.getValue()).intValue());
		else if (comp == mfgPort) restart |= Config.setMFGPort(((Number)mfgPort.getValue()).intValue());
		// else if (comp == startWithSystem) restart |= Config.setStartWithSystem(startWithSystem.isSelected()); // 懒得写恩
		else if (comp == autoStart) restart |= Config.setAutoStartServices(autoStart.isSelected());
		else if (comp == minimizeToTray) restart |= Config.setMinimizeToSystemTray(minimizeToTray.isSelected());
		else if (comp == minimizeAfterAuth) restart |= Config.setAutoMinimizeAfterAuth(minimizeAfterAuth.isSelected());
		else if (comp == allowConfig) restart |= Config.setConfigAllowance(allowConfig.isSelected());
		else if (comp == mfgPath) restart |= Config.setMFGPath(mfgPath.getText());
		else if (comp == bindAddr) restart |= Config.setBindAddress(bindAddr.getText());
		else if (comp == mfgServer) restart |= Config.setMFGServer(mfgServer.getText());
		else if (comp == upstream) restart |= Config.setUpstreamRoute(upstream.getRoute());
		else if (comp == mfgProxy) restart |= Config.setMFGProxy(mfgProxy.getRoute());
		else if (comp == mfgUpstream) restart |= Config.setMFGUpstream(mfgUpstream.getRoute());
		else if (comp == pass) restart |= Config.setPass(new String(pass.getPassword()));

		updateConfig();
	}

	@Override
	public void windowClosing(WindowEvent e) {
		boolean rs = (restart & Config.REQUIRE_SERVICE_RESTART) != 0 && Service.getServiceStatus() != Config.STOPPED;
		boolean rm = (restart & Config.REQUIRE_MFG_RESTART) != 0 && Service.getMFGStatus() != Config.STOPPED;
		if (rs || rm) {
			if (JOptionPane.showConfirmDialog(this, "部分修改需重启生效\n是否重启？", null, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				if (rs) Service.restartService();
				if (rm) Service.restartMFG();
				restart = 0;
			}
		}
		Service.getMonitor().setVisible(true);
	}

	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	public static synchronized void showConfigDialog() {
		if (f == null) f = new ConfigFrame();
		if (!f.isVisible()) restart = 0;
		f.updateConfig();
		f.setVisible(true);
	}
}
