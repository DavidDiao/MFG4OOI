package proxy;

import java.util.List;
import java.util.ListIterator;

public class MessageBody {
	private boolean lengthSpecified;
	private byte[] content, source;
	private Chunk[] chunks;

	public MessageBody() {
		lengthSpecified = true;
		content = source = new byte[0];
	}

	public MessageBody(byte[] content) {
		lengthSpecified = true;
		this.content = source = (content == null ? new byte[0] : content);
	}

	public MessageBody(List<Chunk> chunkList) {
		chunks = new Chunk[chunkList.size()];
		try {
			chunkList.toArray(chunks);
		}
		catch(ArrayStoreException | NullPointerException e) {
			e.printStackTrace();
		}
		lengthSpecified = false;
		content = source = null;
	}

	public MessageBody(Chunk[] chunks) {
		lengthSpecified = false;
		content = source = null;
		this.chunks = chunks;
	}

	public byte[] getSource() {
		if (source != null) return source;
		int len = 0;
		for (int i = 0; i < chunks.length; ++i) {
			int clen = chunks[i].length();
			len += clen + Integer.toString(clen, 16).length() + 4;
		}
		source = new byte[len];
		int off = 0;
		for (int i = 0; i < chunks.length; ++i) {
			int clen = chunks[i].length();
			byte[] clenb = Integer.toString(clen, 16).getBytes();
			for (int j = 0; j < clenb.length; ++j, ++off) source[off] = clenb[j];
			source[off++] = (byte)13;
			source[off++] = (byte)10;
			System.arraycopy(chunks[i].getContent(), 0, source, off, clen);
			off += clen;
			source[off++] = (byte)13;
			source[off++] = (byte)10;
		}
		return source;
	}

	public byte[] getBytes() {
		if (content != null) return content;
		int len = 0;
		for (int i = 0; i < chunks.length; ++i) len += chunks[i].length();
		source = new byte[len];
		int off = 0;
		for (int i = 0; i < chunks.length; ++i) {
			int clen = chunks[i].length();
			System.arraycopy(chunks[i].getContent(), 0, source, off, clen);
			off += clen;
		}
		return content;
	}
}
