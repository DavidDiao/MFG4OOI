package proxy;

abstract public class Handler {
	public static Handler DEFAULT = new Handler() {
		public Route handle(Request origin) {
			return Route.SYSTEM;
		}

		public void handle(Request req, Response res) {}
	};

	// ���ܻ��޸�origin��proxyӦʹ�ñ��޸ĺ��Request
	abstract public Route handle(Request origin);

	abstract public void handle(Request req, Response res);
}
