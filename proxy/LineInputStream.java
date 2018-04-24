package proxy;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

public class LineInputStream extends FilterInputStream {
	public LineInputStream(InputStream is) {
		super(is);
	}

	public String readLine() throws IOException {
		byte[] buff = new byte[256];
		int off = 0;
		boolean f = false;
		while (true) {
			int c = super.read();
			if (c == -1) throw new IOException("EOF");
			if (!f && (c == 13)) f = true;
			else if (f) {
				if (c == 10) return new String(buff, 0, off);
				else {
					f = false;
					if (off + 1 == buff.length) {
						byte[] temp = new byte[off << 1];
						System.arraycopy(buff, 0, temp, 0, off);
						buff = temp;
					}
					buff[off++] = 13;
					buff[off++] = (byte)c;
				}
			} else {
				if (off == buff.length) {
					byte[] temp = new byte[off << 1];
					System.arraycopy(buff, 0, temp, 0, off);
					buff = temp;
				}
				buff[off++] = (byte)c;
			}
		}
	}
}
