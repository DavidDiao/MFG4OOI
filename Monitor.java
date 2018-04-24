import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class Monitor extends JFrame implements StatusChangeListener, ActionListener {
	final public static Color[] COLORS = { Color.BLACK, new Color(0x7F0000), new Color(0x007F00), new Color(0x7F7F00), new Color(0x00007F), new Color(0x7F007F), new Color(0x007F7F), Color.WHITE };
	final public static AttributeSet[] colorAttribs;
	final public static String[] SERVERS = {};

	// Log
	private JTextPane MFGLog;
	private StyledDocument doc;
	private int color = 7, head, tail;
	private int[] lens = null;

	private int xf = 4;

	// Status
	private JLabel ooiServer, serviceStatus, mfgStatus;
	private JButton service, mfg, autoCheck, config;
	private JComboBox<String> servers;
	private boolean unselected = true;

	void appendLog(String log) {
		if (log == null) return;
		if (lens == null) {
			// (tail - head + length) % length ֻ�ܱ�ʾ length ��ֵ
			// �� 0~length �� length +1 ��ֵ
			// �ǳ�����
			lens = new int[Config.getLogLength() + 1];
			head = 0;
			tail = lens.length - 1;
		} else if (lens.length != Config.getLogLength() + 1) {
			int[] t = new int[Config.getLogLength() + 1];
			int crntlen = head - tail - 1;
			if (crntlen < 0) crntlen += lens.length;
			if (crntlen >= t.length) {
				int target = head - t.length, total = 0;
				if (target < 0) target += lens.length;
				for(++tail; tail != target; ++tail) {
					if (tail == lens.length) tail = 0;
					total += lens[tail];
				}
				try {
					doc.remove(0, total + lens[tail]);
				}
				catch(BadLocationException e) {}
			}
			if (tail < head) {
				System.arraycopy(lens, tail + 1, t, 0, head - tail - 1);
				head = head - tail - 1;
			} else if (tail == lens.length - 1) {
				System.arraycopy(lens, 0, t, 0, head);
			} else {
				System.arraycopy(lens, tail + 1, t, 0, lens.length - tail - 1);
				System.arraycopy(lens, 0, t, lens.length - tail - 1, head);
				head = lens.length - tail - 1 + head;
			}
			tail = t.length - 1;
			lens = t;
		}
		if (log.startsWith("  Welcome to MyFleetGirls Client Ver")) Service.startedMFG();
		if (Config.isAutoMinimizeAfterAuth() && log.startsWith("MyFleetGirls���`�ФؤνӾA�˳ɹ����ޤ���")) setExtendedState(ICONIFIED);
		// java.io.IOException: Could not get the screen info ����ӡ����
		// ����������ĵڶ��л��кܳ���\0������JTextPane�޷������Զ�����
		if (log.indexOf('\0') != -1) return;
		if (xf++ <= 3) {
			System.out.print('[');
			for (byte b: log.getBytes()) System.out.print(b + ", ");
			System.out.println(']');
		}
		int last = 0, off = 0, end = -1, total = doc.getLength();
		while ((off = log.indexOf("\033[", off)) != -1) {
			end = log.indexOf('m', off + 2);
			if (end == -1) break;
			try {
				doc.insertString(doc.getLength(), log.substring(last, off), colorAttribs[color]);
			}
			catch(BadLocationException e) {}
			String[] msgs = log.substring(off + 2, end).split(";");
			for (int i = 0; i < msgs.length; ++i) {
				if (msgs[i].equals("0")) {
					color = 7;
				} else if (msgs[i].length() == 2) {
					char c1 = msgs[i].charAt(0), c2 = msgs[i].charAt(1);
					if ((c1 == '3' || c1 == '4') && (c2 >= '0' && c2 <= '7')) {
						if (c1 == '3') {
							color = c2 - '0';
						} else {
							// Not supported
						}
					}
				}
			}
			last = off = end + 1;
		}
		try {
			doc.insertString(doc.getLength(), log.substring(last) + "\n", colorAttribs[color]);
		}
		catch(BadLocationException e) {}
		total = doc.getLength() - total;
		if (head == tail) {
			++tail;
			if (tail == lens.length) tail = 0;
			try {
				doc.remove(0, lens[tail]);
			}
			catch(BadLocationException e) {}
		}
		lens[head] = total;
		++head;
		if (head == lens.length) head = 0;
		try {
			MFGLog.setCaretPosition(doc.getLength());
		}
		catch(IllegalArgumentException e) {}
	}

	public Monitor() {
		super("MFG4OOI");
		setSize(800, 600);
		setLayout(new GridBagLayout());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.gridx = 0; gbc.gridy = 0;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		JLabel lbl = new JLabel("OOI��");
		add(lbl, gbc);
		gbc.gridx = 1; gbc.gridy = 0;
		gbc.gridwidth = 2; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		ooiServer = new JLabel();
		add(ooiServer, gbc);
		gbc.gridx = 0; gbc.gridy = 1;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		lbl = new JLabel("������");
		add(lbl, gbc);
		gbc.gridx = 1; gbc.gridy = 1;
		gbc.gridwidth = 2; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		autoCheck = new JButton("�Զ����");
		autoCheck.setEnabled(false);
		autoCheck.addActionListener(this);
		add(autoCheck, gbc);
		gbc.gridx = 0; gbc.gridy = 2;
		gbc.gridwidth = 3; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		String[] srvnms = {
			"��ѡ��",
			"����R��ظ�(203.104.209.71)",
			"����ظ�(203.104.209.87)",
			"��������ظ�(125.6.184.16)",
			"���Q��ظ�(125.6.187.205)",
			"�󜐾��丮(125.6.187.229)",
			"�ȥ�å�����(203.104.209.134)",
			"��󥬲���(203.104.209.167)",
			"��Х������(203.104.248.135)",
			"����`�ȥ��ɲ���(125.6.189.7)",
			"�֥������(125.6.189.39)",
			"����������������(125.6.189.71)",
			"�ѥ饪����(125.6.189.103)",
			"�֥�ͥ�����(125.6.189.135)",
			"�g���岴��(125.6.189.167)",
			"���۲���(125.6.189.215)",
			"��ë�岴��(125.6.189.247)",
			"¹�ݻ���(203.104.209.23)",
			"�Ҵ�����(203.104.209.39)",
			"�����岴��(203.104.209.55)",
			"���u����(203.104.209.102)"
		};
		servers = new JComboBox<String>(srvnms);
		servers.addActionListener(this);
		add(servers, gbc);
		gbc.gridx = 0; gbc.gridy = 3;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		lbl = new JLabel("״̬");
		add(lbl, gbc);
		gbc.gridx = 1; gbc.gridy = 3;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		serviceStatus = new JLabel();
		add(serviceStatus, gbc);
		gbc.gridx = 2; gbc.gridy = 3;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		service = new JButton();
		service.addActionListener(this);
		add(service, gbc);
		gbc.gridx = 0; gbc.gridy = 4;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		lbl = new JLabel("MFG");
		add(lbl, gbc);
		gbc.gridx = 1; gbc.gridy = 4;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		mfgStatus = new JLabel();
		add(mfgStatus, gbc);
		gbc.gridx = 2; gbc.gridy = 4;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		mfg = new JButton();
		mfg.addActionListener(this);
		add(mfg, gbc);
		gbc.gridx = 0; gbc.gridy = 5;
		gbc.gridwidth = 3; gbc.gridheight = 1;
		gbc.weightx = 0; gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		config = new JButton("����");
		config.addActionListener(this);
		add(config, gbc);
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.gridx = 3; gbc.gridy = 0;
		gbc.gridwidth = 1; gbc.gridheight = 7;
		gbc.weightx = 1; gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		doc = new DefaultStyledDocument();
		MFGLog = new JTextPane(doc);
		MFGLog.setEditable(false);
		MFGLog.setBackground(Color.BLACK);
		MFGLog.setSelectedTextColor(Color.BLACK);
		MFGLog.setSelectionColor(Color.WHITE);
		try {
			MFGLog.setFont(Font.createFont(Font.TRUETYPE_FONT, this.getClass().getResourceAsStream("assets/YaHei Consolas Hybrid 1.12.ttf")).deriveFont(16f));
		}
		catch(IOException | FontFormatException e) {
			MFGLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
		}
		JScrollPane pane = new JScrollPane(MFGLog, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setMaximumSize(new Dimension(2147483647, 2147483647)); // emmmm���Լ�������д�����Ǹ���
		pane.setViewportView(MFGLog); // ����Ҳ��
		add(pane, gbc);
		Config.addStatusChangeListener(this);
		statusChanged();
	}

	public void statusChanged() {
		ooiServer.setText(Config.getOOIServer());
		servers.setSelectedIndex(Config.getKCServer() - (unselected ? 0 : 1));
		if (unselected && (Config.getKCServer() != 0)) {
			servers.removeItemAt(0);
			unselected = false;
		}
		int status = Config.getServiceStatus();
		serviceStatus.setText(status == Config.STOPPED ? "��ֹͣ" : status == Config.STARTING ? "������" : "������");
		service.setText(status == Config.STOPPED ? "����" : "ֹͣ");
		service.setEnabled(Config.checkServiceConfig());
		status = Config.getMFGStatus();
		mfgStatus.setText(status == Config.STOPPED ? "��ֹͣ" : status == Config.STARTING ? "������" : "������");
		mfg.setText(status == Config.STOPPED ? "����" : "ֹͣ");
		mfg.setEnabled(Config.checkMFGConfig());
		autoCheck.setEnabled(Config.isAutoCheckPrepared());
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == servers) {
			if (unselected && (servers.getSelectedIndex() == 0)) return;
			Config.setKCServer(servers.getSelectedIndex() + (unselected ? 0 : 1));
		} else if (e.getSource() == autoCheck) {
			Config.autoCheck();
		} else if (e.getSource() == service) {
			if (service.getText().equals("����")) Service.startService();
			else Service.stopService();
		} else if (e.getSource() == mfg) {
			if (mfg.getText().equals("����")) Service.startMFG();
			else Service.stopMFG();
		} else if (e.getSource() == config) {
			ConfigFrame.showConfigDialog();
		}
	}

	static {
		SimpleAttributeSet[] sas = new SimpleAttributeSet[8];
		for (int i = 0; i < 8; ++i) {
			sas[i] = new SimpleAttributeSet();
			sas[i].addAttribute(StyleConstants.Foreground, COLORS[i]);
		}
		colorAttribs = sas;
	}
}
