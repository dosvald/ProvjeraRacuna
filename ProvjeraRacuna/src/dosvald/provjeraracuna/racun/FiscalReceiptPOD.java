package dosvald.provjeraracuna.racun;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class FiscalReceiptPOD {

	private final UUID jir;
	private final String zk;
	private final Date timestamp;
	private final long lipa;

	public UUID getJir() {
		return jir;
	}

	public String getZk() {
		return zk;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public long getLipa() {
		return lipa;
	}

	public FiscalReceiptPOD(final String jirString, final String md5checksum,
			final Date timestamp, final long lipa) {
		if (timestamp == null)
			throw new IllegalArgumentException("null timestamp");
		this.timestamp = timestamp;
		this.lipa = lipa;

		if (jirString != null) {
			this.jir = UUID.fromString(jirString.toLowerCase(Locale.US));
			if (this.jir.version() != 4)
				throw new IllegalArgumentException("JIR UUID version != 4");
		} else {
			this.jir = null;
		}
		
		if (md5checksum != null) {
			this.zk = md5checksum.toLowerCase(Locale.US);
			if (!this.zk.matches("[0-9a-f]{32}"))
				throw new IllegalArgumentException("checksum not md5");
		} else {
			this.zk = null;
		}
		
		if (jirString == null && md5checksum == null){
			throw new IllegalArgumentException("JIR, checksum null");
		}
	}

}
