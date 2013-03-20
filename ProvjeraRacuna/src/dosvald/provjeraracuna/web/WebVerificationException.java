package dosvald.provjeraracuna.web;

public class WebVerificationException extends VerificationException {

	private static final long serialVersionUID = 4518301386892505831L;

	public WebVerificationException() {
		super();
	}

	public WebVerificationException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public WebVerificationException(String detailMessage) {
		super(detailMessage);
	}

	public WebVerificationException(Throwable throwable) {
		super(throwable);
	}

}
