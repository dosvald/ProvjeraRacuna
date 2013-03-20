package dosvald.provjeraracuna.web;

public class VerificationException extends RuntimeException {

	private static final long serialVersionUID = -8428068392831762210L;

	public VerificationException() {
		super();
	}

	public VerificationException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public VerificationException(String detailMessage) {
		super(detailMessage);
	}

	public VerificationException(Throwable throwable) {
		super(throwable);
	}

}
