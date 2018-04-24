import proxy.Handler;
import proxy.Request;
import proxy.Response;
import proxy.Route;
import proxy.UpstreamConnection;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class CoreHandler extends Handler implements StatusChangeListener {
	private static CoreHandler handler = null;

	private String cookie = null, auth;
	private volatile Thread thread;

	private CoreHandler() {
		handler = this;
		Config.addStatusChangeListener(this);
	}

	public static CoreHandler getHandler() {
		if (handler != null) return handler;
		return new CoreHandler();
	}

	private String os = null, cprfn = null;

	@Override
	public void statusChanged() {
		if (os == null) {
			if (Config.getOOIServer() == null) return;
		} else if (os.equals(Config.getOOIServer())) return;
		os = Config.getOOIServer();
		int off = -1, off2;
		cprfn = "/kcs/resources/image/world/";
		do {
			off2 = os.indexOf('.', ++off);
			String foo = off2 == -1 ? os.substring(off) : os.substring(off, off2);
			try {
				int value = Integer.parseInt(foo);
				cprfn += String.format("%03d", value) + "_";
			}
			catch(NumberFormatException e) {
				cprfn += foo + "_";
			}
			off = off2;
		} while (off != -1);
	}

	@Override
	public Route handle(Request origin) {
		// System.out.println(Thread.currentThread().getName() + "\t-> " + origin);
		if (origin.getPath().startsWith("/kcs")) {
			Config.setOOIServer(origin.getHost());
			String foo = origin.getHeaders().get("cookie");
			if (foo == null) foo = origin.getHeaders().get("Cookie");
			if (foo != null) cookie = foo;
			foo = origin.getHeaders().get("authorization");
			if (foo == null) foo = origin.getHeaders().get("Authorization");
			if (foo != null) auth = foo;
			Config.setAutoCheckPrepared(true);
		}
		String x_origin = origin.getHeaders().get("X-Origin-Host");
		if (x_origin == null) x_origin = origin.getHeaders().get("x-origin-host");
		if (x_origin != null) {
			origin.getHeaders().remove("host");
			origin.getHeaders().put("Host", x_origin);
			if (origin.getHeaders().remove("X-Origin-Host") == null) origin.getHeaders().remove("x-origin-host");
		} else if (origin.getPath().startsWith("/kcsapi/") && origin.getMethod() == Request.POST) {
			if (Config.isTransferReady()) {
				String name = "Host", host = origin.getHeaders().get("Host");
				if (host == null) {
					name = "host";
					host = origin.getHeaders().get("host");
				}
				origin.getHeaders().put("X-Origin-Host", host);
				origin.getHeaders().put(name, Config.SERVER_HOSTS[Config.getKCServer()]);
				return Config.getMFGRoute();
			}
		}
		return Config.getUpstreamRoute();
	}

	@Override
	public void handle(Request req, Response res) {
		// System.out.println(Thread.currentThread().getName() + "\t<- " + req);
		if (cprfn == null) return;
		String path = req.getPath();
		if (path.length() < 33) return;
		char ch = path.charAt(path.length() - 5);
		if (path.startsWith(cprfn) && (ch == 't' || ch == 'l') && path.endsWith(".png")) checkPic(res);
	}

	public void autoCheck() {
		if (!Config.isAutoCheckPrepared()) return;
		Config.setAutoCheckPrepared(false);
		if (cookie == null) return;
		if (thread != null) return;
		LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
		headers.put("Host", os);
		headers.put("Cookie", cookie);
		if (auth != null) headers.put("Authorization", auth);
		Request req = new Request(Request.GET, os, 80, cprfn + "t.png", Request.HTTP_1_1, headers, null);
		thread = new Thread() {
			@Override
			public void run() {
				try(UpstreamConnection conn = new UpstreamConnection()) {
					checkPic(req.send(Config.getUpstreamRoute(), conn));
				}
				catch(IOException e) {}
				thread = null;
				Config.setAutoCheckPrepared(true);
			}
		};
		thread.start();
	}

	private void checkPic(Response res) {
		for (int i = 1; i <= 20; ++i) {
			try {
				InputStream is = getClass().getResourceAsStream(String.format("assets/%02d.png", i));
				byte[] buff = is.readAllBytes();
				if (Arrays.equals(buff, res.getBytes())) {
					Config.setKCServer(i);
					return;
				}
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
