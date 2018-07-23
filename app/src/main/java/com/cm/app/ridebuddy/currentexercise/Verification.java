package com.cm.app.ridebuddy.currentexercise;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

import com.cm.app.ridebuddy.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Verification extends Activity implements TextToSpeech.OnInitListener {
	private Button buttonYes;
	private Button buttonNo;
	WindowManager.LayoutParams layoutParams;
	Uri notification;

	Timer tim;
	TimerTask lvl1;
	TimerTask lvl2;
	TimerTask lvl3;
	TimerTask lvl4;

	private TextToSpeech tts;







	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_verification);
 		tts = new TextToSpeech(this, this);



		layoutParams = this.getWindow().getAttributes();
		notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		tim = new Timer();

		buttonYes = (Button) findViewById(R.id.buttonYes);
		buttonYes.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				tim.cancel();
				lvl1.cancel();
				lvl2.cancel();
				lvl3.cancel();
				lvl4.cancel();
				Intent intent = new Intent(getApplicationContext(), RuningExerciseActivity.class);
				startActivity(intent);
				finish();
			}
		});

		buttonNo = (Button) findViewById(R.id.buttonNo);
		buttonNo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				lvl3.run();
			}
		});



		lvl4 = new TimerTask() { //After 480 seconds, call emergency contact
			public void run() {
				tim.cancel(); //Drop all alarms
				//Contact emergency number
				sendAndCall(null);
				//tim.cancel();
			}
		};
		lvl3 = new TimerTask() { //After 480 seconds, call emergency contact
			public void run() {
				tim.schedule(lvl4, 2000);
				speak("I am calling for rescue");
			}
		};
		lvl2 = new TimerTask() { //After 300 seconds, increase alarm intensity
			public void run() {
				tim.schedule(lvl3, 4000);
				speak("I think you have fallen");
			}
		};
		lvl1 = new TimerTask() { //After 180 seconds, set off alarm/flash/vibrate
			public void run() {
				speak("Are you okay?");
				tim.schedule(lvl2, 4000);
			}
		};

		tim.schedule(lvl1, 2000);
	}

	@Override
	public void onBackPressed() {
		// Swallow all back button presses
	}

	private Contact loadContact() {
		Contact contact = new Contact();
		return contact;
	}

	public void sendAndCall(View view) {
		//Contact emergency number
		Contact c = loadContact();

		if (c != null) {
			String number = c.cell;
			Time now = new Time();
			now.setToNow();
			/*String msg = "[LogitAPP]\n(" + now.format("%D, %R") + ")\nProvavelmente cai de bicicleta, por favor ajuda-me!";
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
				return;
			}
			SmsManager man = SmsManager.getDefault();
			man.sendTextMessage(number, null, msg, null, null);*/

			Intent callIntent = new Intent(Intent.ACTION_CALL);
			callIntent.setData(Uri.parse("tel:" + c.cell));
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
				return;
			}
			startActivity(callIntent);

         	finish();
		}
	}

	private void speak(String txt){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			ttsGreater21(txt);
		} else {
			ttsUnder20(txt);
		}
	}


	@SuppressWarnings("deprecation")
	private void ttsUnder20(String text) {
		HashMap<String, String> map = new HashMap<>();
		map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void ttsGreater21(String text) {
		String utteranceId = this.hashCode() + "";
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
	}


	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {

			int result = tts.setLanguage(Locale.US);

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("TTS", "This Language is not supported");
			}

		} else {
			Log.e("TTS", "Initilization Failed!");
		}
	}


}
