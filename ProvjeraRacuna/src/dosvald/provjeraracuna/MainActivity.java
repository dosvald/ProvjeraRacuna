package dosvald.provjeraracuna;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Map;

import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;
import com.googlecode.tesseract.android.TessBaseAPI.PageSegMode;

import dosvald.provjeraracuna.racun.FiscalReceiptPOD;
import dosvald.provjeraracuna.view.UuidInputFilter;
import dosvald.provjeraracuna.web.VerificationException;
import dosvald.provjeraracuna.web.VerificationFormFields;
import dosvald.provjeraracuna.web.VerificationResult;
import dosvald.provjeraracuna.web.VerificationWebClient;
import dosvald.provjeraracuna.web.WebVerificationException;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class MainActivity extends Activity {

	// these are saved
	private String session;
	private Bitmap captchaBitmap;

	private VerificationWebClient cl;

	private static final String KEY_CAPTCHA_BITMAP = "captcha.bitmap";
	private static final String KEY_SESSION = "session";

	private static final int REQ_CAMERA = 42;
	
	private TessBaseAPI tess;
	private DatePicker datePicker;
	private TimePicker timePicker;
	private EditText jirField;
	private EditText captchaField;
	private EditText amountField;
	private ImageView captchaView;
	private Button okButton;
	private ProgressBar progressBar;
	private TextView txt;

	@Override
	protected void onResume() {
		super.onResume();
		Log.d("activity", "resume");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(KEY_CAPTCHA_BITMAP, captchaBitmap);
		outState.putString(KEY_SESSION, session);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cl.endSession();
		if (tess != null) {
			tess.end();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d("activity", "pause");
	}

	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d("activity", "stop");
	}

	public MainActivity() {
		Log.d("activity", "<init>");
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d("activity", "create");

		setContentView(R.layout.activity_main);

		datePicker = (DatePicker) findViewById(R.id.datePicker);
		timePicker = (TimePicker) findViewById(R.id.timePicker);
		jirField = (EditText) findViewById(R.id.jirField);
		captchaField = (EditText) findViewById(R.id.captchaField);
		amountField = (EditText) findViewById(R.id.amountField);

		captchaView = (ImageView) findViewById(R.id.captchaView);
		okButton = (Button) findViewById(R.id.okButton);

		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		txt = (TextView) findViewById(R.id.textResult);

		//
		jirField.setFilters(new InputFilter[] { new UuidInputFilter() });
		timePicker.setIs24HourView(true);

		int apiVersion = android.os.Build.VERSION.SDK_INT;
		if (apiVersion >= Build.VERSION_CODES.HONEYCOMB) {
			Calendar cal = Calendar.getInstance();
			datePicker.setMaxDate(cal.getTimeInMillis());
			cal.add(Calendar.DAY_OF_MONTH, -32);
			datePicker.setMinDate(cal.getTimeInMillis());
		}

		// web client
		this.cl = new VerificationWebClient();
		if (savedInstanceState == null) {
			// not restoring, initialize session cookie and fetch CAPTCHA image
			new InitSessionTask() {
				@Override
				protected void onPostExecute(String result) {
					super.onPostExecute(result);
					new UpdateCaptchaTask().execute(captchaView);
				}
			}.execute((Void) null);
		} else {
			// restore session and CAPTCHA
			session = savedInstanceState.getString(KEY_SESSION);
			captchaBitmap = savedInstanceState
					.getParcelable(KEY_CAPTCHA_BITMAP);

			cl.resumeSession(session);
			captchaView.setImageBitmap(captchaBitmap);
		}

		// fetches new CAPTCHA on click
		captchaView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new UpdateCaptchaTask().execute(captchaView);
			}
		});

		// OK button submits the form
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// load data from input fields
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.YEAR, datePicker.getYear());
				cal.set(Calendar.MONTH, datePicker.getMonth());
				cal.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
				cal.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
				cal.set(Calendar.MINUTE, timePicker.getCurrentMinute());

				long lipa = 0;
				FiscalReceiptPOD receipt = null;
				boolean errors = false;

				try {
					// DecimalFormat so parsing is locale-aware
					lipa = (long) (DecimalFormat.getInstance()
							.parse(amountField.getText().toString())
							.doubleValue() * 100);
				} catch (ParseException e) {
					amountField.setError(getString(R.string.invalid_amount));
					amountField.requestFocus();
					errors = true;
				}

				try {
					receipt = new FiscalReceiptPOD(jirField.getText()
							.toString(), null, cal.getTime(), lipa);
				} catch (IllegalArgumentException e) {
					//
					jirField.setError(getString(R.string.invalid_JIR));
					jirField.requestFocus();
					errors = true;
				}

				if (errors)
					return;

				new AsyncTask<Object, Integer, Object>() {
					@Override
					protected void onPreExecute() {
						progressBar.setIndeterminate(true);
						progressBar.setVisibility(View.VISIBLE);
					}

					@Override
					protected Object doInBackground(Object... params) {
						String s = (String) params[0];
						FiscalReceiptPOD receipt = (FiscalReceiptPOD) params[1];

						// send request
						try {
							return cl.checkReceiptByJir(receipt, s);
						} catch (VerificationException e) {
							return e;
						}
					}

					@Override
					protected void onPostExecute(Object result) {
						String toast = null;
						String message = null;

						if (result instanceof VerificationResult) {
							VerificationResult verResult = (VerificationResult) result;

							switch (verResult.getStatus()) {
							case OK:
								message = verResult.getHints().get("message");
								break;
							case FAIL:
								message = verResult.getHints().get("message");
								break;
							case VALIDATION_ERROR:
								for (Map.Entry<String, String> e : verResult
										.getHints().entrySet()) {
									String key = e.getKey();
									String val = e.getValue();
									if (VerificationFormFields.JIR_PART5
											.equals(key)) {
										jirField.setError(val);
									} else if (VerificationFormFields.AMOUNT_KN
											.equals(key)) {
										amountField.setError(val);
									} else if (VerificationFormFields.CAPTCHA
											.equals(key)) {
										captchaField.setError(val);
									}
								}
								break;
							default:
								throw new Error("Hopefully unreachable");
							}
						} else if (result instanceof WebVerificationException) {
							WebVerificationException e = (WebVerificationException) result;
							toast = "Web communication error: "
									+ e.getMessage();
						} else if (result instanceof VerificationException) {
							VerificationException e = (VerificationException) result;
							toast = "Form response error: " + e.getMessage();
						} else {
							throw new Error("Hopefully unreachable");
						}

						if (toast != null)
							Toast.makeText(getApplicationContext(), toast,
									Toast.LENGTH_SHORT).show();

						txt.setText(message);
						progressBar.setVisibility(View.GONE);

						new UpdateCaptchaTask().execute(captchaView);
					}
				}.execute(captchaField.getText().toString(), receipt);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.recognize_item:
			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(cameraIntent, REQ_CAMERA);
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQ_CAMERA: {
			if (resultCode == RESULT_OK) {
				Bitmap capturedBitmap = (Bitmap) data.getExtras().get("data");
				new RecognizeTask(  ).execute(capturedBitmap);
			}
			break;
		}
		}
	}
	
	private class RecognizeTask extends AsyncTask<Bitmap, Integer, Void> {
		private static final int PROGRESS_ZERO = 0;
		private static final int PROGRESS_CONSTRUCT = 1;
		private static final int PROGRESS_INIT = 2;
		private static final int PROGRESS_GOTTEXT = 3;
		private static final int PROGRESS_CLEARING = 4;
		private static final int PROGRESS_CLEARED = 5;
		
		private static final int PROGRESS_MAX = 5;
		
		@Override
		protected void onPreExecute() {
			progressBar.setIndeterminate(false);
			progressBar.setMax(PROGRESS_MAX);
			progressBar.setProgress(PROGRESS_ZERO);
			progressBar.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected Void doInBackground(Bitmap... params) {
			if (tess == null) {
				tess = new TessBaseAPI();
				tess.setDebug(true);
				publishProgress(PROGRESS_CONSTRUCT);
				tess.setVariable("load_system_dawg", "0");
				tess.init(Environment.getExternalStorageDirectory().getAbsolutePath(), "hrv+eng");
				tess.setVariable("user_words_suffix", "test");
				tess.setVariable("language_model_penalty_non_freq_dict_word", "0");
				publishProgress(PROGRESS_INIT);
				tess.setPageSegMode(PageSegMode.PSM_AUTO_OSD);
				tess.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "abcdef0123456789-");
			} else {
				publishProgress(PROGRESS_INIT);
			}
			
			tess.setImage(params[0]);
			String txt = tess.getUTF8Text();
			publishProgress(PROGRESS_GOTTEXT);
			
			if ("".equals(txt))
				Log.d("alltext", "(No results)");
			else 
				Log.d("alltext", txt);
			
			ResultIterator iterator = tess.getResultIterator();
			if (iterator != null) {
				while (iterator.next(PageIteratorLevel.RIL_TEXTLINE)) {
					Log.d("line",
							iterator.getUTF8Text(PageIteratorLevel.RIL_TEXTLINE));
				}
			}
			
			publishProgress(PROGRESS_CLEARING);
			tess.clear();
			
			publishProgress(PROGRESS_CLEARED);
			
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			int progress = values[0];
			progressBar.setProgress(progress);
			switch(progress) {
			case PROGRESS_INIT:
				txt.setText("Initialized tess, processing image");
				break;
			case PROGRESS_GOTTEXT:
				txt.setText("OCR'd");
				break;
			case PROGRESS_MAX:
				txt.setText(null);
				break;
			}
		}
		
	}

	private class InitSessionTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... params) {
			return cl.initSession();
		}

		@Override
		protected void onPostExecute(String result) {
			session = result;
		}
	}

	private class UpdateCaptchaTask extends AsyncTask<ImageView, Void, Bitmap> {
		private ImageView captchaView;

		@Override
		protected Bitmap doInBackground(ImageView... params) {
			Log.d("doing", "lol");
			this.captchaView = params[0];
			return cl.fetchCaptchaImage();
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			captchaBitmap = result;
			captchaView.setImageBitmap(captchaBitmap);
		}
	}

}
