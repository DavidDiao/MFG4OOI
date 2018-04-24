import proxy.Proxy;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Iterator;
import java.util.stream.Stream;

public class Service {
	final private static Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			if (mfg == null) return;
			Process p = mfg;
			mfg = null;
			Stream<ProcessHandle> descendants = p.descendants();
			Iterator<ProcessHandle> iter = descendants.iterator();
			while (iter.hasNext()) iter.next().destroyForcibly();
			p.destroyForcibly();
		}
	};

	final private static Runnable logAppender = new Runnable() {
		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(mfg.getInputStream()));
				Monitor m = getMonitor();
				String line = null;
				while((line = reader.readLine()) != null) m.appendLog(line);
			}
			catch(IOException e) {}
			catch(NullPointerException e) {}
			if (mfg != null) stopMFG();
		}
	};

	private static Proxy service = null;
	private static Process mfg = null;
	private static boolean serviceStarted = false, mfgStarted;

	public static void startService() {
		if (service != null) return;
		if (!Config.checkServiceConfig()) return;
		try {
			service = new Proxy(Config.getPort(), CoreHandler.getHandler(), Config.getBindAddress());
			Config.sendStatusChangeMessage();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void stopService() {
		if (service == null) return;
		service.shutdown();
		service = null;
		Config.sendStatusChangeMessage();
	}

	public static void restartService() {
		stopService();
		startService();
	}

	public static int getServiceStatus() {
		if (service == null) return Config.STOPPED;
		if (serviceStarted) return Config.RUNNING;
		return Config.STARTING;
	}

	public static void startedService() {
		if (serviceStarted) return;
		serviceStarted = true;
		Config.sendStatusChangeMessage();
	}

	public static void startMFG() {
		if (mfg != null) return;
		if (!Config.checkMFGConfig()) return;
		try {
			mfg = new ProcessBuilder(Config.getMFGPath()).redirectInput(new File(Config.getMFGPath())).start();
			Runtime.getRuntime().addShutdownHook(shutdownHook);
			mfgStarted = false;
			new Thread(logAppender).start();
			Config.sendStatusChangeMessage();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void stopMFG() {
		if (mfg == null) return;
		shutdownHook.run();
		Runtime.getRuntime().removeShutdownHook(shutdownHook);
		mfg = null;
		Config.sendStatusChangeMessage();
	}

	public static void restartMFG() {
		stopMFG();
		synchronized(shutdownHook) {
			try {
				shutdownHook.sleep(100); // 想不通为什么，但总之不写会出错
			}
			catch(InterruptedException e) {}
		}
		startMFG();
	}

	public static int getMFGStatus() {
		if (mfg == null) return Config.STOPPED;
		if (mfgStarted) return Config.RUNNING;
		return Config.STARTING;
	}

	public static void startedMFG() {
		if (mfgStarted) return;
		mfgStarted = true;
		Config.sendStatusChangeMessage();
	}

	private static Monitor m = null;

	public static synchronized Monitor getMonitor() {
		if (m == null) {
			m = new Monitor();
			if (SystemTray.isSupported()) {
				trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(Service.class.getResource("assets/mfg.png")), "MFG4OOI");
				trayIcon.setImageAutoSize(true);
				trayIcon.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						m.setVisible(true);
						m.setExtendedState(Frame.NORMAL);
						SystemTray.getSystemTray().remove(trayIcon);
					}
				});
				getMonitor().addWindowListener(new WindowAdapter(){
					@Override
					public void windowIconified(WindowEvent e) {
						if (Config.isMinimizeToSystemTray()) {
							try {
								SystemTray.getSystemTray().add(trayIcon);
								m.setVisible(false);
							}
							catch(AWTException ex) {
								ex.printStackTrace();
							}
						}
					}
				});
			}
		}
		return m;
	}

	private static TrayIcon trayIcon = null;

	public static boolean isSystemTraySupported() {
		return SystemTray.isSupported();
	}
}
