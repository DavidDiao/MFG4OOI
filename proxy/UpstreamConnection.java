package proxy;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

// 应为每一个下游连接单独准备一个这玩意儿
public class UpstreamConnection implements Closeable {
	private Socket s;
	private String h;
	private int p;

	public UpstreamConnection() {
		s = null;
		h = null;
	}

	@Override
	public void close() throws IOException {
		if (s != null && !s.isClosed()) {
			s.close();
			s = null;
			h = null;
		}
	}

	public Socket getSocket() { return s; }

	public Socket prepareSocket(Request req, Route r) throws IOException {
		String host = r.getHost();
		int port = r.getPort();
		if (host == null) { // DIRECT
			host = req.getHost();
			port = req.getPort();
		}
		if (h == null || port != p || !host.equals(h)) {
			if (s != null && !s.isClosed()) s.close();
			s = new Socket(host, port);
			h = host;
			p = port;
		}
		return s;
	}
}
