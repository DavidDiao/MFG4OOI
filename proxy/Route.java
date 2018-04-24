package proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;

public class Route {
	final public static Route DIRECT  = new Route(null, -3);
	final public static Route SYSTEM  = new Route() {
		@Override
		public String getHost() {
			return getSystemProxyHost();
		}

		@Override
		public int getPort() {
			return getSystemProxyPort();
		}
	};

	final private static URI eg = URI.create("http://example.com/");


	private String host;
	private int port;

	private Route() {}

	public Route(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public static String getSystemProxyHost() {
		Proxy p = ProxySelector.getDefault().select(eg).get(0);
		return p == Proxy.NO_PROXY ? null : ((InetSocketAddress)p.address()).getHostName();
	}

	public int getPort() {
		return port;
	}

	public static int getSystemProxyPort() {
		Proxy p = ProxySelector.getDefault().select(eg).get(0);
		return p == Proxy.NO_PROXY ? -2 : ((InetSocketAddress)p.address()).getPort();
	}

	@Override
	public boolean equals(Object r) {
		if (r == this) return true;
		if (!(r instanceof Route)) return false;
		String rh = ((Route)r).getHost();
		if (rh == null) {
			if (getHost() != null) return false;
		} else if (!rh.equals(getHost())) return false;
		return ((Route)r).getPort() == getPort();
	}

	@Override
	public String toString() {
		return getHost() + ":" + getPort();
	}

	static {
		System.setProperty("java.net.useSystemProxies", "true");
	}
}
