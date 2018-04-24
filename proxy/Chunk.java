package proxy;

public class Chunk {
	private byte[] content;

	public Chunk(byte[] content) {
		this.content = content;
	}

	public byte[] getContent() { return content; }

	public int length() { return content.length; }
}
