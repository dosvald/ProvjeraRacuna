package dosvald.provjeraracuna.web;

public final class VerificationFormFields {
	public static final String VERIFICATION_TYPE = "idProvjere";
	public static final String CHECKSUM = "zastitniKod";

	public static final String JIR_PART1 = "jirPart1";
	public static final String JIR_PART2 = "jirPart2";
	public static final String JIR_PART3 = "jirPart3";
	public static final String JIR_PART4 = "jirPart4";
	public static final String JIR_PART5 = "jirPart5";

	public static final String DATE = "datumIzdavanja";
	public static final String TIME_MINUTE = "vrijeme_min";
	public static final String TIME_HOUR = "vrijeme_sat";
	public static final String AMOUNT_KN = "iznos_kn";
	public static final String AMOUNT_LP = "iznos_lp";
	public static final String CAPTCHA = "captcha";
	public static final String SUBMIT = "submit";

	static final String[] VALIDATED = { CHECKSUM, JIR_PART5, DATE,
			TIME_HOUR, AMOUNT_KN, CAPTCHA };
}