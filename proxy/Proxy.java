package proxy;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Proxy extends Thread {
	private int port;
	private boolean running = true;
	private ServerSocket ss;
	private Handler handler;

	public Proxy(int port) throws IOException {
		this(port, Handler.DEFAULT, null);
	}

	public Proxy(int port, Handler handler) throws IOException {
		this(port, handler, null);
	}

	public Proxy(int port, Handler handler, InetAddress bindAddr) throws IOException {
		this.port = port;
		ss = new ServerSocket(port, 50, bindAddr);
		this.handler = handler;
		start();
	}

	public void shutdown() {
		running = false;
		try {
			ss.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		ss = null;
	}

	@Override
	public void run() {
		try {
			for (; running; ) {
				Socket s = ss.accept();
				if (!running) break;
				new ConnectionHandler(s, handler);
			}
		}
		catch(IOException e) {
			if (ss != null) try {
				ss.close();
			}
			catch(IOException e2) {
				e2.printStackTrace();
			}
		}
		ss = null;
	}
}

class ConnectionHandler extends Thread {
	private Socket s;
	private Handler handler;

	ConnectionHandler(Socket s, Handler handler) throws IOException {
		this.s = s;
		this.handler = handler;
		start();
	}

	@Override
	public void run() {
		UpstreamConnection conn = new UpstreamConnection();
		try {
			OutputStream os = new BufferedOutputStream(s.getOutputStream());
			for (; ; ) {
				Request req = Request.receiveRequest(s);
				Route route = handler.handle(req);
				if (req.getMethod() == Request.CONNECT) {
					Socket out;
					try {
						out = conn.prepareSocket(req, route);
					}
					catch(IOException e) {
						s.getOutputStream().write("HTTP/1.1 503 Service Unavailable\r\n\r\n".getBytes());
						continue;
					}
					if (route.getHost() == null) s.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
					else {
						os = new BufferedOutputStream(out.getOutputStream());
						os.write("CONNECT ".getBytes());
						os.write(req.getHost().getBytes());
						os.write(58);
						os.write(Integer.toString(req.getPort()).getBytes());
						os.write(32);
						os.write(req.getProtocolName().getBytes());
						os.write(13);
						os.write(10);
						Iterator<Map.Entry<String, String>> iter = req.getHeaders().entrySet().iterator();
						while (iter.hasNext()) {
							Map.Entry<String, String> entry = iter.next();
							os.write(entry.getKey().getBytes());
							os.write(58);
							os.write(32);
							os.write(entry.getValue().getBytes());
							os.write(13);
							os.write(10);
						}
						os.write(13);
						os.write(10);
						os.flush();
					}
					new Tunnel(s, out);
					new Tunnel(out, s);
					s = out = null;
					return;
				}
				boolean close = false, lc = false;;
				String foo = req.getHeaders().get("Connection");
				if (foo == null) {
					foo = req.getHeaders().get("connection");
					lc = true;
				}
				if ("close".equals(foo)) {
					close = true;
					if (lc) req.getHeaders().remove("connection");
					else req.getHeaders().remove("Connection");
				}
				Response res = req.send(route, conn);
				handler.handle(req, res);
				os.write(res.getProtocolVersion().getBytes());
				os.write(32);
				os.write(res.getStatusCodeAndReason().getBytes());
				os.write(13);
				os.write(10);
				Iterator<Map.Entry<String, String>> iter = res.getHeaders().entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<String, String> entry = iter.next();
					os.write(entry.getKey().getBytes());
					os.write(58);
					os.write(32);
					os.write(entry.getValue().getBytes());
					os.write(13);
					os.write(10);
				}
				os.write(13);
				os.write(10);
				os.write(res.getSourceMessageBody());
				os.flush();
				if (close) throw new IOException();
			}
		}
		catch(IOException e1) {
			try {
				if (s != null && !s.isClosed()) s.close();
			}
			catch(IOException e) {}
			s = null;
			try {
				conn.close();
			}
			catch(IOException e) {}
			conn = null;
		}
	}
}

class Tunnel extends Thread {
	private Socket s1, s2;

	Tunnel(Socket s1, Socket s2) throws IOException {
		this.s1 = s1;
		this.s2 = s2;
		start();
	}

	@Override
	public void run() {
		byte[] buff = new byte[1024];
		try {
			InputStream is = s1.getInputStream();
			OutputStream os = s2.getOutputStream();
			for (; ; ) {
				if (is.available() == 0) {
					int c = is.read();
					if (c == -1) throw new IOException();
					os.write(c);
				} else {
					int len = is.read(buff);
					os.write(buff, 0, len);
				}
			}
		}
		catch(IOException e1) {
			try {
				if (!s1.isClosed()) s1.close();
				if (!s2.isClosed()) s2.close();
			}
			catch(IOException e2) {}
			s1 = s2 = null;
		}
	}
}
