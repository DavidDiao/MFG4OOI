import proxy.Route;

import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

public class Config {
	final public static Route MFG4OOI = new Route(null, 0) {
		@Override
		public String getHost() {
			return "localhost";
		}

		@Override
		public int getPort() {
			return proxyPort;
		}
	};

	final public static int REQUIRE_MFG_RESTART     = 1;
	final public static int REQUIRE_SERVICE_RESTART = 2;

	// Status
	private static List<StatusChangeListener> listeners = new ArrayList<StatusChangeListener>();

	public static void addStatusChangeListener(StatusChangeListener l) {
		listeners.add(l);
	}

	public static void removeStatusChangeListener(StatusChangeListener l) {
		listeners.remove(l);
	}

	static void sendStatusChangeMessage() {
		Iterator<StatusChangeListener> iter = listeners.iterator();
		while (iter.hasNext()) iter.next().statusChanged();
	}

	final public static String[] SERVER_HOSTS = {
		null,
		"203.104.209.71",
		"203.104.209.87",
		"125.6.184.16",
		"125.6.187.205",
		"125.6.187.229",
		"203.104.209.134",
		"203.104.209.167",
		"203.104.248.135",
		"125.6.189.7",
		"125.6.189.39",
		"125.6.189.71",
		"125.6.189.103",
		"125.6.189.135",
		"125.6.189.167",
		"125.6.189.215",
		"125.6.189.247",
		"203.104.209.23",
		"203.104.209.39",
		"203.104.209.55",
		"203.104.209.102"
	};

	private static String ooiServer = null;
	private static int    kcServer  = 0;
	private static boolean autoCheckPrepared = false;

	public static String getOOIServer() { return ooiServer; }

	public static void setOOIServer(String server) {
		if (ooiServer == null && server == null || ooiServer != null && ooiServer.equals(server)) return;
		ooiServer = server;
		sendStatusChangeMessage();
	}

	public static int getKCServer() { return kcServer; }

	public static void setKCServer(int server) {
		if (server == 0) return;
		if (kcServer == server) return;
		Service.startedService();
		kcServer = server;
		sendStatusChangeMessage();
	}

	final public static int STOPPED  = 0;
	final public static int STARTING = 1;
	final public static int RUNNING  = 2;

	public static int getServiceStatus() { return Service.getServiceStatus(); }

	public static int getMFGStatus() { return Service.getMFGStatus(); }

	public static boolean isAutoCheckPrepared() { return autoCheckPrepared; }

	public static void setAutoCheckPrepared(boolean status) {
		if (autoCheckPrepared == status) return;
		autoCheckPrepared = status;
		sendStatusChangeMessage();
	}

	public static void autoCheck() {
		if (!isAutoCheckPrepared()) return;
		CoreHandler.getHandler().autoCheck();
	}

	// Config
	private static Route upstreamRoute = Route.SYSTEM;
	private static int proxyPort = 0;
	private static InetAddress bindAddress = InetAddress.getLoopbackAddress();
	private static int logLength = 500;
	private static boolean allowConfig = true;
	private static boolean autoStartServices = true;
	private static boolean minimizeToSystemTray = true;
	private static boolean autoMinimizeAfterAuth = true;
	private static String mfgPath;
	// MFG Config (Don't support Route.SYSTEM)
	private static String mfgServer = "https://myfleet.moe";
	private static Route mfgProxy = Route.DIRECT;
	private static int mfgPort = 0;
	private static Route mfgUpstream = MFG4OOI;
	private static String pass = null;

	// 应即时读取
	public static Route getUpstreamRoute() {
		return upstreamRoute;
	}

	public static int setUpstreamRoute(Route route) {
		if (route.equals(upstreamRoute)) return 0;
		upstreamRoute = route;
		updateConfig();
		return 0;
	}

	// 仅服务启动时需要
	public static int getPort() { return proxyPort; }

	public static int setPort(int port) {
		if (proxyPort == port) return 0;
		boolean status = checkServiceConfig();
		proxyPort = port;
		// if (status != checkMFGConfig()) sendStatusChangeMessage();
		sendStatusChangeMessage(); // 给 ConfigFrame 发的
		int rtn = 0;
		if ((mfgUpstream == MFG4OOI) && updateMFGConfig()) rtn |= REQUIRE_MFG_RESTART;
		if (updateConfig()) rtn |= REQUIRE_SERVICE_RESTART;
		return rtn;
	}

	// 仅服务启动时需要
	public static InetAddress getBindAddress() { return bindAddress; }

	public static int setBindAddress(String addr) {
		if (bindAddress.getHostName().equals(addr)) return 0;
		return setBindAddress(getByName(addr));
	}

	// return null when addr is null or ""
	// return localhost when addr is unavailable
	private static InetAddress getByName(String addr) {
		if (addr == null) return null;
		if (addr.equals("")) return null;
		try {
			return InetAddress.getByName(addr);
		}
		catch(UnknownHostException e) {}
		return InetAddress.getLoopbackAddress();
	}

	public static int setBindAddress(InetAddress addr) {
		if (bindAddress.equals(addr)) return 0;
		bindAddress = addr;
		return updateConfig() ? REQUIRE_SERVICE_RESTART : 0;
	}

	// 建议即时读取
	public static int getLogLength() { return logLength; }

	public static int setLogLength(int leng) {
		if (logLength == leng) return 0;
		logLength = leng;
		updateConfig();
		return 0;
	}

	// emmmm...
	public static boolean isConfigAllowed() { return allowConfig; }

	public static int setConfigAllowance(boolean allowed) {
		if (allowConfig == allowed) return 0;
		allowConfig = allowed;
		if (allowed) {
			if (MFG4OOI.equals(mfgUpstream)) mfgUpstream = MFG4OOI;
		} else if (MFG4OOI == mfgUpstream) mfgUpstream = new Route("localhost", proxyPort);
		updateConfig();
		return 0;
	}

	public static boolean isAutoStartServices() { return autoStartServices; }

	public static int setAutoStartServices(boolean autoStart) {
		if (autoStartServices == autoStart) return 0;
		autoStartServices = autoStart;
		updateConfig();
		return 0;
	}

	public static boolean isMinimizeToSystemTray() { return minimizeToSystemTray; }

	public static int setMinimizeToSystemTray(boolean autoHide) {
		if (!Service.isSystemTraySupported()) autoHide = false;
		if (minimizeToSystemTray == autoHide) return 0;
		minimizeToSystemTray = autoHide;
		updateConfig();
		return 0;
	}

	public static boolean isAutoMinimizeAfterAuth() { return autoMinimizeAfterAuth; }

	public static int setAutoMinimizeAfterAuth(boolean autoMinimize) {
		if (autoMinimizeAfterAuth == autoMinimize) return 0;
		autoMinimizeAfterAuth = autoMinimize;
		updateConfig();
		return 0;
	}

	// 仅MFG启动时需要
	public static String getMFGPath() { return mfgPath; }

	public static int setMFGPath(String path) {
		if (mfgPath.equals(path)) return 0;
		boolean status = checkMFGConfig();
		mfgPath = path;
		if (status != checkMFGConfig()) sendStatusChangeMessage();
		return updateConfig() ? REQUIRE_MFG_RESTART : 0;
	}

	// 仅配置
	public static String getMFGServer() { return mfgServer; }

	public static int setMFGServer(String host) {
		if (mfgServer.equals(host)) return 0;
		boolean status = checkMFGConfig();
		mfgServer = host;
		if (status != checkMFGConfig()) sendStatusChangeMessage();
		return updateMFGConfig() ? REQUIRE_MFG_RESTART : 0;
	}

	// 仅配置
	public static Route getMFGProxy() { return mfgProxy; }

	public static int setMFGProxy(Route route) {
		if (route.equals(mfgProxy)) return 0;
		mfgProxy = route;
		return updateMFGConfig() ? REQUIRE_MFG_RESTART : 0;
	}

	// 应在服务启动时读取
	public static Route getMFGRoute() {
		return new Route("127.0.0.1", mfgPort);
	}

	// 使用 getMFGRoute
	public static int getMFGPort() { return mfgPort; }

	public static int setMFGPort(int port) {
		if (mfgPort == port) return 0;
		boolean status = checkMFGConfig();
		mfgPort = port;
		if (status != checkMFGConfig()) sendStatusChangeMessage();
		return updateMFGConfig() ? REQUIRE_MFG_RESTART | REQUIRE_SERVICE_RESTART : 0;
	}

	// 仅配置
	public static Route getMFGUpstream() { return mfgUpstream; }

	public static int setMFGUpstream(Route route) {
		mfgUpstream = route;
		if (route.equals(mfgUpstream)) return 0;
		return updateMFGConfig() ? REQUIRE_MFG_RESTART : 0;
	}

	// 仅配置
	public static String getPass() { return pass; }

	public static int setPass(String npass) {
		if (npass.equals(pass)) return 0;
		pass = npass;
		return updateMFGConfig() ? REQUIRE_MFG_RESTART : 0;
	}

	// 检测配置状态
	public static boolean checkServiceConfig() {
		if (proxyPort < 1024) return false;
		return true;
	}

	public static boolean checkMFGConfig() {
		if ("".equals(mfgPath)) return false;
		if (!(new File(mfgPath).isFile())) return false;
		if (mfgPort < 1024) return false;
		if ("".equals(mfgServer)) return false;
		return true;
	}

	public static boolean isTransferReady() {
		return getServiceStatus() == RUNNING && getMFGStatus() == RUNNING;
	}

	// Load Config
	public static void loadConfig() {}

	public static boolean updateConfig() {
		Properties prop = new Properties();
		if (upstreamRoute == Route.DIRECT) prop.setProperty("upstream.type", "DIRECT");
		else if (upstreamRoute == Route.SYSTEM) prop.setProperty("upstream.type", "SYSTEM");
		else {
			prop.setProperty("upstream.type", "SPECIFIC");
			prop.setProperty("upstream.host", upstreamRoute.getHost());
			prop.setProperty("upstream.port", Integer.toString(upstreamRoute.getPort()));
		}
		prop.setProperty("proxy.port", Integer.toString(proxyPort));
		if (bindAddress != null) prop.setProperty("proxy.bindAddress", bindAddress.getHostName());
		prop.setProperty("ui.loglength", Integer.toString(logLength));
		prop.setProperty("mfg.allowconfig", Boolean.toString(allowConfig));
		prop.setProperty("basic.autostartservices", Boolean.toString(autoStartServices));
		prop.setProperty("ui.autohide", Boolean.toString(minimizeToSystemTray));
		prop.setProperty("ui.autominimize", Boolean.toString(autoMinimizeAfterAuth));
		prop.setProperty("mfg.path", mfgPath);
		try(FileWriter writer = new FileWriter("mfg4ooi.properties")) {
			prop.store(writer, null);
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null, "修改配置文件失败\n" + e.getMessage());
			return false;
		}
		return true;
	}

	public static boolean updateMFGConfig() {
		if (!allowConfig) return false;
		try (FileWriter writer = new FileWriter("application.conf")) {
			writer.write("#Generated by MFG4OOI\r\n\r\nurl {\r\n    post: \"");
			writer.write(mfgServer);
			writer.write("\"\r\n\r\n    proxy {");
			if (mfgProxy != Route.DIRECT) {
				writer.write("\r\n        host: \"");
				writer.write(mfgProxy.getHost());
				writer.write("\"\r\n        port: ");
				writer.write(Integer.toString(mfgProxy.getPort()));
			}
			writer.write("\r\n    }\r\n}\r\n\r\nproxy {\r\n    port: ");
			writer.write(Integer.toString(mfgPort));
			writer.write("\r\n\r\n    host: \"localhost\"\r\n}\r\n\r\nupstream_proxy {");
			if (mfgUpstream != Route.DIRECT) {
				writer.write("\r\n    host: \"");
				writer.write(mfgUpstream.getHost());
				writer.write("\"\r\n    port: ");
				writer.write(Integer.toString(mfgUpstream.getPort()));
			}
			writer.write("\r\n}\r\n\r\nauth {\r\n    pass: \"");
			writer.write(pass);
			writer.write("\"\r\n}\r\n");
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null, "修改MFG配置文件失败\n" + e.getMessage());
			return false;
		}
		return true;
	}

	public static boolean updateConfigs() {
		return updateMFGConfig() && updateConfig();
	}

	private static int skip(char[] buff, int off, char... set) {
		while (off < buff.length) {
			boolean f = false;
			for (char c: set) if (buff[off] == c) {
				f = true;
				break;
			}
			if (!f) return off;
			++off;
		}
		return buff.length;
	}

	private static int skipTo(char[] buff, int off, char... set) {
		while (off < buff.length) {
			for (char c: set) if (buff[off] == c) return off;
			++off;
		}
		return buff.length;
	}

	private static int skipSP(char[] buff, int off) {
		boolean found;
		do {
			found = false;
			off = skip(buff, off, ' ', '\n', '\r');
			if (off >= buff.length) return buff.length;
			if (buff[off] == '#' || off < buff.length && buff[off] == '/' && (buff[off + 1] == '/' || buff[off + 1] == '*')) {
				found = true;
				if (buff[off] == '/' && buff[off + 1] == '*') {
					off += 4;
					while (off < buff.length) {
						if (buff[off - 2] == '*' && buff[off - 1] == '/') break;
						++off;
					}
					if (off > buff.length) off = buff.length;
				}
				else off = skipTo(buff, off + 2, '\r') + 1;
			}
		} while (found);
		return off;
	}

	private static int skipJSONString(char[] buff, int off) {
		char label = buff[off++];
		while (buff[off] != label) {
			if (off == '\\') {
				if (buff[off + 1] == 'u') off += 5;
				else ++off;
			}
			++off;
		}
		return off + 1;
	}

	private static String parseJSONString(char[] buff, int begin, int end) {
		++begin;
		--end;
		char[] tmp = new char[end - begin];
		int i;
		for (i = 0; begin < end; ++begin, ++i) {
			if (buff[begin] == '\\') {
				++begin;
				switch (buff[begin]) {
					case 'b':
						tmp[i] = '\b';
						break;
					case 'f':
						tmp[i] = '\f';
						break;
					case 'n':
						tmp[i] = '\n';
						break;
					case 'r':
						tmp[i] = '\r';
						break;
					case 't':
						tmp[i] = '\t';
						break;
					case 'u':
						try {
							tmp[i] = (char)Integer.parseInt(new String(buff, begin, 4), 16);
						}
						catch(NumberFormatException e) {
							tmp[i] = '\0';
						}
						begin += 4;
						break;
					default:
						tmp[i] = buff[begin];
				}
			} else tmp[i] = buff[begin];
		}
		return new String(tmp, 0, i);
	}

	private static Properties loadMFGConfig() {
		File mfgconf = new File("application.conf");
		// 凑活用用的（类似） Hjson 的解析，跨行字符串、数组解析都没写
		// 区别在于这里是 xxx { yyy: zzz }，而 Hjson 是 xxx: { yyy: zzz }
		// 详细语法见 http://hjson.org/syntax.html
		// 只要用户没乱改 application.conf 就没问题
		try(InputStreamReader reader = new InputStreamReader(new FileInputStream(mfgconf), "Shift-JIS")) {
			String prefix = "";
			Properties mfgprop = new Properties();
			char[] content = new char[(int)mfgconf.length()];
			int leng = reader.read(content), off = skipSP(content, 0), endOff;
			boolean startFlag = false;
			if (content[off] == '{') ++off;
			while (off < leng) {
				off = skipSP(content, off);
				String key;
				if (content[off] == '\'' || content[off] == '\"') {
					endOff = skipJSONString(content, off);
					if (endOff == off) break;
					key = parseJSONString(content, off, endOff);
				} else {
					endOff = skipTo(content, off, ',', ':', '{', '}', ' ', '\n', '\r');
					if (endOff == off) break;
					key = new String(content, off, endOff - off);
				}
				key = key.replaceAll("\\.", "\\\\.");
				off = skip(content, endOff, ' ', '\n', '\r');
				if (content[off] == ':') off = skipSP(content, off + 1);
				if (content[off] == '{') {
					prefix += key + ".";
					endOff = off + 1;
				} else if (content[off] == '\'' || content[off] == '\"') {
					endOff = skipJSONString(content, off);
					if (endOff == off) break;
					mfgprop.setProperty(prefix + key, parseJSONString(content, off, endOff));
				} else {
					// 用在线模拟器 http://hjson.org/try.html 试特殊情况时得到了很多奇怪的结果
					// 这边没管那么多就随便写了
					endOff = skipTo(content, off, ',', '}', '\n', '\r');
					if (endOff == off) break;
					String value = new String(content, off, endOff - off), vc = value;
					int cm1 = value.indexOf('#'), cm2 = value.indexOf("//"), cm3 = value.indexOf("/*");
					int min = cm1;
					boolean t3 = false;
					if (min == -1 || cm2 != -1 && cm2 < min) min = cm2;
					if (min == -1 || cm3 != -1 && cm3 < min) {
						min = cm3;
						t3 = true;
					}
					if (min != -1) vc = value.substring(0, min);
					try {
						Double valued = Double.parseDouble(vc);
						value = vc;
					}
					catch(NumberFormatException e) {}
					mfgprop.setProperty(prefix + key, value);
				}
				off = skipSP(content, endOff);
				if (content[off] == ',') off = skipSP(content, off + 1);
				while (content[off] == '}') {
					if (prefix.equals("")) off = content.length;
					int toff = prefix.lastIndexOf('.', prefix.length() - 2);
					while (toff > 0 && prefix.charAt(toff - 1) == '\\') toff = prefix.lastIndexOf('.', toff - 1);
					if (toff <= 0) prefix = "";
					else prefix = prefix.substring(0, toff + 1);
					off = skipSP(content, off + 1);
					if (off >= content.length) break;
					if (content[off] == ',') off = skipSP(content, off + 1);
				}
			}
			return mfgprop;
		}
		catch(IOException e) {
			JOptionPane.showMessageDialog(null, "读取配置文件失败\n" + e.getMessage() + "\n请重新启动");
			System.exit(1);
			return null;
		}
	}

	static {
		Properties mfgprop = loadMFGConfig();
		mfgServer = mfgprop.getProperty("url.post");
		String mph = mfgprop.getProperty("url.proxy.host");
		if (mph != null) try {
			int mpp = Integer.parseInt(mfgprop.getProperty("url.proxy.port"));
			mfgProxy = new Route(mph, mpp);
		}
		catch(NumberFormatException e) {}
		try {
			mfgPort = Integer.parseInt(mfgprop.getProperty("proxy.port"));
		}
		catch(NumberFormatException e) {}
		String usph = mfgprop.getProperty("upstream_proxy.host");
		if (usph != null) try {
			int uspp = Integer.parseInt(mfgprop.getProperty("upstream_proxy.port"));
			mfgUpstream = new Route(usph, uspp);
		}
		catch(NumberFormatException e) {}
		pass = mfgprop.getProperty("auth.pass");

		File conff = new File("mfg4ooi.properties");
		if (!conff.isFile()) {
			String osName = System.getProperty("os.name");
			if (osName.startsWith("Win")) mfgPath = "MyFleetGirls.bat";
			else if (osName.startsWith("Linux")) mfgPath = "./MyFleetGirls.sh";
			else if (osName.startsWith("Mac")) mfgPath = "./MyFleetGirls.command";
			else mfgPath = "";
			int newport = mfgPort + 1;
			String rtn = JOptionPane.showInputDialog(null, "未检测到配置文件，将自动配置\n请输入一个未被占用的端口号\n如果希望手动配置请取消(Esc)", newport);
			try {
				newport = Integer.parseInt(rtn);
			}
			catch(NumberFormatException e) {
				rtn = null;
			}
			bindAddress = getByName(mfgprop.getProperty("proxy.host"));
			if (rtn != null) {
				proxyPort = mfgPort;
				mfgPort = newport;
				upstreamRoute = mfgUpstream;
				mfgUpstream = MFG4OOI;
				updateConfigs();
			} else updateConfig();
		} else {
			try {
				Properties prop = new Properties();
				prop.load(new FileReader("mfg4ooi.properties"));
				String t = prop.getProperty("upstream.type");
				if ("DIRECT".equals(t)) upstreamRoute = Route.DIRECT;
				else if ("SYSTEM".equals(t)) upstreamRoute = Route.SYSTEM;
				else upstreamRoute = new Route(prop.getProperty("upstream.host"), Integer.parseInt(prop.getProperty("upstream.port")));
				proxyPort = Integer.parseInt(prop.getProperty("proxy.port"));
				bindAddress = getByName(prop.getProperty("proxy.bindAddress"));
				t = prop.getProperty("ui.loglength");
				allowConfig = Boolean.parseBoolean(prop.getProperty("mfg.allowconfig"));
				autoStartServices = Boolean.parseBoolean(prop.getProperty("basic.autostartservices"));
				minimizeToSystemTray = Boolean.parseBoolean(prop.getProperty("ui.autohide"));
				autoMinimizeAfterAuth = Boolean.parseBoolean(prop.getProperty("ui.autominimize"));
				if (t != null) logLength = Integer.parseInt(t);
				mfgPath = prop.getProperty("mfg.path");
				if (MFG4OOI.equals(mfgUpstream)) mfgUpstream = MFG4OOI;
			}
			catch(IOException|NullPointerException|NumberFormatException e) {
				JOptionPane.showMessageDialog(null, "读取配置文件失败\n" + e.getMessage() + "\n请重新启动");
				System.exit(1);
			}
		}

		if (!Service.isSystemTraySupported()) setMinimizeToSystemTray(false);
	}
}
