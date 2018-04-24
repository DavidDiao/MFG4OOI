public class Main {
	public static void main(String[] args) {
		Config.loadConfig();
		if (Config.checkServiceConfig() && Config.checkMFGConfig()) {
			Service.getMonitor().setVisible(true);
			if (Config.isAutoStartServices()) {
				Service.startService();
				Service.startMFG();
			}
		} else ConfigFrame.showConfigDialog();
	}
}
