package proxy;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.Socket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Request implements Cloneable {
	private String host, path;
	private int method, port, protocol;
	private Map<String, String> headers;
	private byte[] postData;

	final public static int HTTP_1_0 = 0;
	final public static int HTTP_1_1 = 1;

	final public static int OPTIONS = 101;
	final public static int GET     = 102;
	final public static int HEAD    = 103;
	final public static int POST    = 104;
	final public static int PUT     = 105;
	final public static int DELETE  = 106;
	final public static int TRACE   = 107;
	final public static int CONNECT = 108;

	final public static int UNKNOWN = 999;

	public Request(int method, String host, int port, String path, int protocol, Map<String, String> headers, byte[] postData) {
		this.method = method;
		this.host = host;
		this.port = port;
		this.path = path;
		this.protocol = protocol;
		this.headers = headers;
		this.postData = postData;
	}

	public int getMethod() { return method; }

	public String getHost() { return host; }

	public void setHost(String host) { this.host = host; }

	public int getPort() { return port; }

	public String getPath() { return path; }

	public int getProtocol() { return protocol; }

	public Map<String, String> getHeaders() { return headers; }

	public String getMethodName() { return getMethodName(method); }

	public static String getMethodName(int method) {
		if (method == OPTIONS) return "OPTIONS";
		if (method == GET) return "GET";
		if (method == HEAD) return "HEAD";
		if (method == POST) return "POST";
		if (method == PUT) return "PUT";
		if (method == DELETE) return "DELETE";
		if (method == TRACE) return "TRACE";
		if (method == CONNECT) return "CONNECT";
		return "UNKNOWN";
	}

	public String getProtocolName() { return getProtocolName(protocol); }

	public static String getProtocolName(int protocol) {
		if (protocol == HTTP_1_0) return "HTTP/1.0";
		if (protocol == HTTP_1_1) return "HTTP/1.1";
		return "UNKNOWN";
	}

	public byte[] getPostData() { return postData; }

	@Override
	public String toString() {
		return getMethodName(method) + (method == CONNECT ? " " : " http://") + host + (port == 80 ? "" : ":" + port) + path + " " + getProtocolName(protocol);
	}

	public String getFullHeader() {
		String rtn = toString() + "\r\n";
		Iterator<Map.Entry<String, String>> iter = headers.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, String> entry = iter.next();
			rtn += entry.getKey() + ": " + entry.getValue() + "\r\n";
		}
		return rtn;
	}

	public boolean keepAlive() {
		if (protocol == HTTP_1_0) {
			if (get(headers, "Connection").equals("keep-alive")) return true;
			if (headers.containsKey("Keep-Alive")) return true;
			return false;
		}
		if (get(headers, "Connection").equals("close")) return false;
		return true;
	}

	/*
	@Override
	public Request clone() {
		LinkedHashMap<String, String> cheaders = new LinkedHashMap<String, String>();
		Iterator<Map.Entry<String, String>> iter = headers.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, String> entry = iter.next();
			cheaders.put(entry.getKey(), entry.getValue());
		}
		return new Request(method, host, port, path, protocol, cheaders);
	}
	*/

	// get "Key-Value" and "key-value"
	private static String get(Map<String, String> map, String key) {
		String foo = map.get(key);
		if (foo == null) foo = map.get(key.toLowerCase());
		return foo;
	}

	public static Request receiveRequest(Socket s) throws IOException {
		LineInputStream reader = new LineInputStream(s.getInputStream());
		String requestLine;
		do {
			requestLine = reader.readLine();
		} while (requestLine.equals(""));
		int off = requestLine.indexOf(' ');
		String methods = requestLine.substring(0, off);
		int method = Request.UNKNOWN;
		if (methods.equals("GET")) method = Request.GET;
		if (methods.equals("POST")) method = Request.POST;
		if (methods.equals("CONNECT")) method = Request.CONNECT;
		if (method == Request.UNKNOWN) {
			s.getOutputStream().write("HTTP/1.1 405 Method Not Allowed\r\nAllow: GET POST CONNECT\r\n\r\n".getBytes());
			s.close();
			return null;
		}
		int off2 = requestLine.indexOf(' ', ++off);
		String url = requestLine.substring(off, off2);
		String protocols = requestLine.substring(off2 + 1);
		int protocol = Request.UNKNOWN;
		if (protocols.equals("HTTP/1.0")) protocol = Request.HTTP_1_0;
		if (protocols.equals("HTTP/1.1")) protocol = Request.HTTP_1_1;
		if (protocol == Request.UNKNOWN) {
			s.getOutputStream().write("HTTP/1.1 505 HTTP Version Not Supported\r\n\r\n".getBytes());
			s.close();
			return null;
		}
		off = url.indexOf("://");
		String host, path;
		int port = 80;
		if (off == -1) off = -3;
		off2 = url.indexOf('/', off + 3);
		if (off2 == -1) {
			host = url.substring(off + 3);
			path = "";
		} else {
			host = url.substring(off + 3, off2);
			path = url.substring(off2);
		}
		if ((off = host.indexOf(':')) != -1) {
			try {
				port = Integer.parseInt(host.substring(off + 1));
			}
			catch(NumberFormatException e) {
				s.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes());
				s.close();
				return null;
			}
			host = host.substring(0, off);
		}
		LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
		String header;
		byte[] postData = null;
		while(!(header = reader.readLine()).equals("")) {
			off = header.indexOf(':');
			headers.put(header.substring(0, off), header.substring(off + 2));
		}
		try {
			int len = Integer.parseInt(get(headers, "Content-Length"));
			postData = new byte[len];
			reader.readNBytes(postData, 0, len);
		}
		catch(NumberFormatException | NullPointerException e) {}
		return new Request(method, host, port, path, protocol, headers, postData);
	}

	// 不要求conn事先prepareSocket
	public Response send(Route r, UpstreamConnection conn) throws IOException {
		Socket s = conn.prepareSocket(this, r);
		OutputStream os = new BufferedOutputStream(s.getOutputStream());
		os.write(getMethodName().getBytes());
		os.write(32);
		if (r.getHost() != null) {
			os.write("http://".getBytes());
			os.write(getHost().getBytes());
			if (getPort() != 80) {
				os.write(58);
				os.write(Integer.toString(getPort()).getBytes());
			}
		}
		os.write(getPath().getBytes());
		os.write(" HTTP/1.1\r\n".getBytes());
		/*
		os.write(32);
		os.write(getProtocolName().getBytes());
		os.write(13);
		os.write(10);
		*/
		Iterator<Map.Entry<String, String>> iter = getHeaders().entrySet().iterator();
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
		if (postData != null) os.write(postData);
		os.flush();
		LineInputStream is = new LineInputStream(s.getInputStream());
		String statusLine;
		do {
			statusLine = is.readLine();
		} while ("".equals(statusLine));
		int off = statusLine.indexOf(' ');
		String ver = statusLine.substring(0, off);
		String status = statusLine.substring(off + 1);
		LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
		String header;
		while(!(header = is.readLine()).equals("")) {
			off = header.indexOf(':');
			headers.put(header.substring(0, off), header.substring(off + 2));
		}
		if (status.startsWith("1") || status.startsWith("204") || status.startsWith("304") || method == HEAD) {
			if ("close".equals(get(headers, "Connection")))  try {conn.close(); } catch(IOException e) {}
			return new Response(ver, status, headers, null);
		}
		String te = get(headers, "Transfer-Encoding");
		if (te != null && !te.equals("identity")) {
			ArrayList<Chunk> chunks = new ArrayList<Chunk>();
			Chunk c;
			int len;
			do {
				String lens = is.readLine();
				int t = lens.indexOf(' ');
				if (t != -1) lens = lens.substring(0, t);
				try {
					len = Integer.parseInt(lens, 16);
				}
				catch(NumberFormatException e) {
					throw new IOException("Invalid HEX Number", e);
				}
				byte[] data = new byte[len];
				is.readNBytes(data, 0, len);
				chunks.add(new Chunk(data));
				is.readLine();
			} while (len != 0);
			if ("close".equals(get(headers, "Connection")))  try {conn.close(); } catch(IOException e) {}
			return new Response(ver, status, headers, new MessageBody(chunks));
		}
		try {
			int len = Integer.parseInt(get(headers, "Content-Length"));
			byte[] content = new byte[len];
			is.readNBytes(content, 0, len);
			if ("close".equals(get(headers, "Connection")))  try {conn.close(); } catch(IOException e) {}
			return new Response(ver, status, headers, new MessageBody(content));
		}
		catch(NullPointerException | NumberFormatException e) {}
		String range = get(headers, "Content-Range");
		if (range != null && status.startsWith("206")) try {
			off = range.indexOf('-');
			int first = Integer.parseInt(range.substring(0, off));
			int last = Integer.parseInt(range.substring(off + 1, range.indexOf('/')));
			int len = last - first + 1;
			byte[] content = new byte[len];
			is.readNBytes(content, 0, len);
			if ("close".equals(get(headers, "Connection")))  try {conn.close(); } catch(IOException e) {}
			return new Response(ver, status, headers, new MessageBody(content));
		}
		catch(NullPointerException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
			throw new IOException("Unexcepted Content-Range Value", e);
		}
		byte[] buff = is.readAllBytes();
		if ("close".equals(get(headers, "Connection")))  try {conn.close(); } catch(IOException e) {}
		return new Response(ver, status, headers, new MessageBody(buff));
	}
}
