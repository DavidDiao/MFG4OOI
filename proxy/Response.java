package proxy;

import java.util.Map;

public class Response {
	private String ver, status;
	private Map<String, String> headers;
	private MessageBody body;
	final private byte[] empty = new byte[0];

	public Response(String protocolVer, String status, Map<String, String> headers, MessageBody body) {
		ver = protocolVer;
		this.status = status;
		this.headers = headers;
		this.body = body;
	}

	public String getProtocolVersion() { return ver; }

	public String getStatusCodeAndReason() { return status; }

	public String getStatusCode() { return status.substring(0, status.indexOf(' ')); }

	public String getReason() { return status.substring(status.indexOf(' ') + 1); }

	public Map<String, String> getHeaders() { return headers; }

	public MessageBody getMessageBody() { return body; }

	public byte[] getSourceMessageBody() { return body == null ? empty : body.getSource(); }

	public byte[] getBytes() { return body == null ? empty : body.getBytes(); }
}
