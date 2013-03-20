package dosvald.provjeraracuna.web;

import java.util.HashMap;
import java.util.Map;

public class VerificationResult {
	public static enum Status {
		OK, FAIL, VALIDATION_ERROR;
	}

	private Status status;

	private Map<String, String> hints = new HashMap<String, String>();

	public Status getStatus() {
		return status;
	}

	public VerificationResult(Status status) {
		this.status = status;
	}

	public Map<String, String> getHints() {
		return hints;
	}
}
