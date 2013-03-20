package dosvald.provjeraracuna.web;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import dosvald.provjeraracuna.racun.FiscalReceiptPOD;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.util.Log;

public class VerificationWebClient {

	private static final String BASE_URL = "http://www.provjeri-racun.hr/provjeraracuna";
	private static final String FORM_URL = BASE_URL + "/provjera";
	private static final String CAPTCHA_URL = BASE_URL + "/captcha";

	private static final String SESSION_COOKIE = "JSESSIONID";

	private final AndroidHttpClient client = AndroidHttpClient
			.newInstance("test");
	private final CookieStore cookieStore;
	private final HttpContext context = new BasicHttpContext();

	private final ResponseHandler<String> stringResponseHandler = new BasicResponseHandler();
	private final ResponseHandler<Bitmap> bitmapResponseHandler = new ResponseHandler<Bitmap>() {
		@Override
		public Bitmap handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {

			int statusCode = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();

			if (entity == null)
				throw new ClientProtocolException("no response body");

			try {
				if (statusCode != HttpStatus.SC_OK) {
					entity.consumeContent();
					throw new HttpResponseException(statusCode, response
							.getStatusLine().getReasonPhrase());
				}

				if (!entity.getContentType().getValue().contains("image")) {
					throw new ClientProtocolException("non-image response");
				}

				InputStream responseStream;
				try {
					responseStream = new BufferedInputStream(
							AndroidHttpClient.getUngzippedContent(entity));
				} catch (IOException e) {
					throw new IOException("error processing response stream", e);
				}

				Bitmap bitmap = BitmapFactory.decodeStream(responseStream);
				if (bitmap == null)
					throw new ClientProtocolException(
							"Invalid image in response stream");
				return bitmap;

			} finally {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					// client.close();
					throw new IOException("IO error consuming content", e);
				}
			}
		}
	};

	private static final class ReceiptVerificationResponseHandler implements
			ResponseHandler<VerificationResult> {
		private final BasicResponseHandler delegate = new BasicResponseHandler();

		@Override
		public VerificationResult handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			String responseBody = delegate.handleResponse(response);

			VerificationResult result;
			if (responseBody.contains("validationError")) {
				result = new VerificationResult(
						VerificationResult.Status.VALIDATION_ERROR);
				for (String field : VerificationFormFields.VALIDATED) {
					Pattern p = Pattern
							.compile("<span id=\""
									+ field
									+ "\\.errors\" *(?: *\\w+=\"[^\"]*\" *)*>\\s*([^<>]*)\\s*</span>");
					Matcher m = p.matcher(responseBody);
					if (m.find()) {
						String msg = m.group(1);
						result.getHints().put(field, msg);
					}
				}
				return result;
			}

			Matcher replyMatcher = Pattern.compile(
					"<p class=\"odgovor(\\w*)\">\\s*([^<>]*)\\s*</p>").matcher(
					responseBody);
			if (replyMatcher.find()) {
				String reply = replyMatcher.group(1);
				if ("Error".equals(reply)) {
					result = new VerificationResult(
							VerificationResult.Status.FAIL);
				} else if ("Success".equals(reply)) {
					result = new VerificationResult(
							VerificationResult.Status.OK);
				} else {
					throw new VerificationException(
							"Unhandled verification reply " + reply);
				}
				result.getHints().put("message", replyMatcher.group(2));
				return result;
			}

			throw new VerificationException("Unhandled verification reply");
		}

	}

	public VerificationWebClient() {
		this.cookieStore = new BasicCookieStore();
		context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		client.enableCurlLogging("req", Log.DEBUG);
	}

	public void resumeSession(String sessionToResume) {
		Log.d("sessionResume", sessionToResume);
		this.cookieStore.clear();
		BasicClientCookie cookie = new BasicClientCookie(SESSION_COOKIE,
				sessionToResume);
		cookie.setPath("/");
		cookie.setDomain("www.provjeri-racun.hr");
		this.cookieStore.addCookie(cookie);
	}

	public String initSession() throws WebVerificationException {
		cookieStore.clear();

		try {
			client.execute(new HttpGet(BASE_URL), stringResponseHandler,
					context);
		} catch (HttpResponseException e) {
			throw new WebVerificationException("unexpected http response code",
					e);
		} catch (IOException e) {
			throw new WebVerificationException("IO error in web request", e);
		}

		for (Cookie cookie : cookieStore.getCookies()) {
			Log.d("cookie", cookie.getValue());
			if (SESSION_COOKIE.equals(cookie.getName())) {
				Log.d("sessionGot", cookie.getValue());
				return cookie.getValue();
			}
		}
		throw new WebVerificationException(
				"No cookie was received from server, expected session ID");

	}

	public Bitmap fetchCaptchaImage() throws WebVerificationException {
		HttpUriRequest req = new HttpGet(CAPTCHA_URL);
		try {
			return client.execute(req, bitmapResponseHandler, context);
		} catch (HttpResponseException e) {
			throw new WebVerificationException("unexpected http response code",
					e);
		} catch (ClientProtocolException e) {
			throw new WebVerificationException("error in response content", e);
		} catch (IOException e) {
			throw new WebVerificationException("IO error in web request", e);
		}
		// throw new FiscalReceiptWebException("IO error in web request", e);
	}

	public VerificationResult checkReceiptByJir(final FiscalReceiptPOD receipt,
			final String captcha) throws WebVerificationException {

		Date timestamp = receipt.getTimestamp();
		String[] jirParts = receipt.getJir().toString().split("-");
		long kn = receipt.getLipa() / 100;
		long lp = receipt.getLipa() % 100;

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(
				VerificationFormFields.VERIFICATION_TYPE, "1"));

		params.add(new BasicNameValuePair(VerificationFormFields.JIR_PART1,
				jirParts[0]));
		params.add(new BasicNameValuePair(VerificationFormFields.JIR_PART2,
				jirParts[1]));
		params.add(new BasicNameValuePair(VerificationFormFields.JIR_PART3,
				jirParts[2]));
		params.add(new BasicNameValuePair(VerificationFormFields.JIR_PART4,
				jirParts[3]));
		params.add(new BasicNameValuePair(VerificationFormFields.JIR_PART5,
				jirParts[4]));

		params.add(new BasicNameValuePair(VerificationFormFields.CAPTCHA,
				captcha));
		params.add(new BasicNameValuePair(VerificationFormFields.SUBMIT,
				"Provjeri"));
		params.add(new BasicNameValuePair(VerificationFormFields.DATE,
				new SimpleDateFormat("dd.MM.yyyy", Locale.US).format(timestamp)));
		params.add(new BasicNameValuePair(VerificationFormFields.TIME_HOUR,
				new SimpleDateFormat("HH", Locale.US).format(timestamp)));
		params.add(new BasicNameValuePair(VerificationFormFields.TIME_MINUTE,
				new SimpleDateFormat("mm", Locale.US).format(timestamp)));
		params.add(new BasicNameValuePair(VerificationFormFields.AMOUNT_KN,
				String.format(Locale.US, "%d", kn)));
		params.add(new BasicNameValuePair(VerificationFormFields.AMOUNT_LP,
				String.format(Locale.US, "%02d", lp)));
		params.add(new BasicNameValuePair(VerificationFormFields.CHECKSUM, ""));

		Log.d("POST", params.toString());
		Log.d("POST", kn + " , " + lp + " , " + receipt.getLipa());

		HttpPost req = new HttpPost(FORM_URL);
		

		try {
			req.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		} catch (UnsupportedEncodingException ignore) {
		}

		ResponseHandler<VerificationResult> handler = new ReceiptVerificationResponseHandler();
		try {
			return client.execute(req, handler, context);
		} catch (HttpResponseException e) {
			throw new WebVerificationException("unexpected http response code",
					e);
		} catch (IOException e) {
			throw new WebVerificationException("IO error in web request", e);
		}
	}

	public void endSession() {
		cookieStore.clear();
		client.close();
	}
}
