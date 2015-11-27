package im.delight.faceless;

/**
 * Copyright (C) 2014 www.delight.im <info@delight.im>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see {http://www.gnu.org/licenses/}.
 */

import im.delight.java.emoji.Emoji;
import android.content.SharedPreferences;
import im.delight.android.location.SimpleLocation;
import android.widget.CheckBox;
import java.util.List;
import im.delight.android.baselib.Data;
import im.delight.android.baselib.UI;
import im.delight.android.keyvaluespinner.KeyValueSpinner;
import im.delight.android.progress.SimpleProgressDialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;

public class ActivityAdd extends Activity implements Server.Callback.MessageEvent {

	private static final int MAX_CHARS_MESSAGE = 240;
	// TWO MAIN VIEW GROUPS BEGIN
	private View mViewOptionsContainer;
	private View mViewMessageContainer;
	// TWO MAIN VIEW GROUPS END
	private Button mButtonNext;
	private Button mButtonPublish;
	private EditText mEditTextMessage;
	private TextView mTextViewDegree;
	private TextView mTextViewCharsLeft;
	private KeyValueSpinner<CharSequence> mSpinnerTopic;
	private KeyValueSpinner<CharSequence> mSpinnerVisibility;
	private CheckBox mCheckBoxLocation;
	private int mColor;
	private int mPatternID;
	private String mText;
	private Resources mResources;
	private BackgroundPatterns mBackgroundPatterns;
	private SimpleProgressDialog mSimpleProgressDialog;
	private SimpleLocation mSimpleLocation;
	private TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			mText = s.toString();

			int charsLeft = MAX_CHARS_MESSAGE - mText.length();
			if (charsLeft < 0) {
				charsLeft = 0;
			}
			mTextViewCharsLeft.setText(mResources.getQuantityString(R.plurals.x_chars_left, charsLeft, charsLeft));
		}

		@Override
		public void afterTextChanged(Editable s) { }

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

	};

	private void updateTextAndColor() {
		final int textColor = UI.getTextColor(mColor);

		mBackgroundPatterns.setViewBackground(this, mEditTextMessage, mPatternID, mColor);
		mEditTextMessage.setTextColor(textColor);
		mEditTextMessage.setLinkTextColor(textColor);
		mEditTextMessage.setHintTextColor(textColor);
		mEditTextMessage.setText(mText);
		UI.putCursorToEnd(mEditTextMessage);

		mBackgroundPatterns.setViewBackground(this, mTextViewDegree, mPatternID, mColor);
		mTextViewDegree.setTextColor(textColor);

		mBackgroundPatterns.setViewBackground(this, mTextViewCharsLeft, mPatternID, mColor);
		mTextViewCharsLeft.setTextColor(textColor);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		updateTextAndColor();
	}

	private void setActiveScreen(int index) {
		if (index == 0) {
			mViewMessageContainer.setVisibility(View.GONE);
			mViewOptionsContainer.setVisibility(View.VISIBLE);
			setTitle(R.string.action_add);
		}
		else if (index == 1) {
			mViewOptionsContainer.setVisibility(View.GONE);
			mViewMessageContainer.setVisibility(View.VISIBLE);
			setTitle(mSpinnerTopic.getValue().toString());
			mTextViewDegree.setText(mSpinnerVisibility.getValue().toString());
		}
		else {
			throw new RuntimeException("Unknown screen index: "+index);
		}

		// update the options menu to show the correct controls for the current screen
		invalidateOptionsMenu();
	}

	@Override
	public void onBackPressed() {
		if (mViewMessageContainer.getVisibility() == View.VISIBLE) {
			setActiveScreen(0);
		}
		else {
			super.onBackPressed();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add);

		// set up resources
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ActivityAdd.this);
		Global.Setup.load(prefs);
		mResources = getResources();
		mBackgroundPatterns = BackgroundPatterns.getInstance(this);

		// set up two main view groups
		mViewOptionsContainer = findViewById(R.id.viewOptionsContainer);
		mViewMessageContainer = findViewById(R.id.viewMessageContainer);

		// set up the buttons
		mButtonNext = (Button) findViewById(R.id.buttonNext);
		mButtonNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mSpinnerTopic.getKey() != null && mSpinnerTopic.getKey().length() > 0) {
					if (mSpinnerVisibility.getKey() != null && mSpinnerVisibility.getKey().length() > 0) {
						setActiveScreen(1);
					}
					else {
						throw new RuntimeException("Undefined visibility level");
					}
				}
				else {
					Toast.makeText(ActivityAdd.this, R.string.please_choose_topic, Toast.LENGTH_SHORT).show();
				}
			}
		});
		mButtonPublish = (Button) findViewById(R.id.buttonPublish);
		mButtonPublish.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String text = Emoji.replaceInText(mText.trim());
				if (text.length() > 0) {
					setLoading(true);

					final SimpleLocation.Point location = mCheckBoxLocation.isChecked() ? mSimpleLocation.getPosition() : null;
					Server.saveMessage(ActivityAdd.this, Data.colorToHex(mColor), mPatternID, text, mSpinnerTopic.getKey().toString(), mSpinnerVisibility.getKey().toString(), location, ActivityAdd.this);
				}
				else {
					Toast.makeText(ActivityAdd.this, getString(R.string.please_enter_message), Toast.LENGTH_SHORT).show();
				}
			}
		});

		// set up the EditText for the message text
		mEditTextMessage = (EditText) findViewById(R.id.editTextMessage);
		mEditTextMessage.addTextChangedListener(mTextWatcher);

		// set up a length filter for the EditText
		UI.setMaxLength(mEditTextMessage, MAX_CHARS_MESSAGE);

		// prevent the user from entering line breaks in the EditText
		mEditTextMessage.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// ignore "Enter" key
				return keyCode == KeyEvent.KEYCODE_ENTER;
			}
		});

		// clear the hint when the user enters the EditText
		mEditTextMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					mEditTextMessage.setHint(null);
				}
			}
		});

		// set up the TextViews for meta information at the bottom
		mTextViewDegree = (TextView) findViewById(R.id.textViewDegree);
		mTextViewCharsLeft = (TextView) findViewById(R.id.textViewCharsLeft);

		// set default values for message properties
		if (Config.DEMO_ACTIVE) {
			mColor = Color.rgb(23, 191, 206);
			mPatternID = 1;
		}
		else {
			mColor = UI.getRandomColor();
			mPatternID = mBackgroundPatterns.getRandomPatternID();
		}
		try {
			// try to get the shared text/URL that has been received via Android's share dialog
			mText = getIntent().getStringExtra(Intent.EXTRA_TEXT).trim();
		}
		catch (Exception e) {
			// or use an empty text as the default
			mText = "";
		}

		// update colors and text in views
		updateTextAndColor();

		// set up the topic selection Spinner
		mSpinnerTopic = (KeyValueSpinner<CharSequence>) findViewById(R.id.spinnerTopic);
		final KeyValueSpinner.Adapter<CharSequence> topicsAdapter = KeyValueSpinner.Adapter.createFromResource(this, R.array.topics_list_machine, R.array.topics_list_human, R.layout.spinner_text_white);
		// first sort the normal entries
		topicsAdapter.sort();
		// then add special entries to the beginning and end
		topicsAdapter.insert(new KeyValueSpinner.Pair<CharSequence>(getString(R.string.topics_empty_machine), getString(R.string.topics_empty_human)), 0);
		topicsAdapter.add(new KeyValueSpinner.Pair<CharSequence>(getString(R.string.topics_meta_machine), getString(R.string.topics_meta_human)));
		// show the Adapter's data in the Spinner
		topicsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerTopic.setAdapter(topicsAdapter);

		// set up the visibility selection Spinner
		mSpinnerVisibility = (KeyValueSpinner<CharSequence>) findViewById(R.id.spinnerVisibility);
		final KeyValueSpinner.Adapter<CharSequence> visibilityAdapter = KeyValueSpinner.Adapter.createFromResource(this, R.array.visibility_list_machine, R.array.visibility_list_human, R.layout.spinner_text_white);
		// show the Adapter's data in the Spinner
		visibilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerVisibility.setAdapter(visibilityAdapter);

		// create the location provider
		mSimpleLocation = new SimpleLocation(this);
		mSimpleLocation.setBlurRadius(Config.LOCATION_BLUR_RADIUS);

		// set up the location feature checkbox
		mCheckBoxLocation = (CheckBox) findViewById(R.id.checkBoxLocation);
		mCheckBoxLocation.setChecked(mSimpleLocation.hasLocationEnabled());
		mCheckBoxLocation.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// if the location feature has just been enabled
				if (mCheckBoxLocation.isChecked()) {
					// if we don't have location access on the device yet
					if (!mSimpleLocation.hasLocationEnabled()) {
						// uncheck the location feature checkbox again
						mCheckBoxLocation.setChecked(false);
						// ask the user to enable location access
						SimpleLocation.openSettings(ActivityAdd.this);
					}
				}
			}

		});

		// if location access has not been enabled yet
		if (!mSimpleLocation.hasLocationEnabled()) {
			// show the location switch (which is normally hidden by default) to ask the user for access
			findViewById(R.id.viewLocation).setVisibility(View.VISIBLE);
		}

		// set up the action bar
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// check if setup has been completed
		if (!Global.Setup.isComplete()) {
			// try to run automatic setup
			if (!Global.Setup.runAuto(this, prefs)) {
				// if automatic setup did not succeed
				// switch to manual setup
				startActivity(new Intent(this, ActivitySetup.class));
				// prevent any window animation because the user is to be redirected immediately
				overridePendingTransition(0, 0);
				finish();
				return;
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		// if location access is enabled
		if (mSimpleLocation.hasLocationEnabled()) {
			// ask the device to update the location
			mSimpleLocation.beginUpdates();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		// stop requesting and receiving updates for the location
		mSimpleLocation.endUpdates();
	}

	private void setLoading(boolean loading) {
		if (loading) {
			mSimpleProgressDialog = SimpleProgressDialog.show(this);
		}
		else {
			if (mSimpleProgressDialog != null) {
				mSimpleProgressDialog.dismiss();
				mSimpleProgressDialog = null;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// on the message compose screen
		if (mViewMessageContainer.getVisibility() == View.VISIBLE) {
			getMenuInflater().inflate(R.menu.add_write, menu);
			return true;
		}
		// on the message options screen
		else {
			getMenuInflater().inflate(R.menu.add_options, menu);

			// if the advanced options are visible
			if (findViewById(R.id.viewAudience).getVisibility() == View.VISIBLE) {
				menu.findItem(R.id.action_more_options).setVisible(false);
			}

			return true;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_change_background:
				mColor = UI.getRandomColor();
				mPatternID = mBackgroundPatterns.getRandomPatternID();
				updateTextAndColor();
				return true;
			case R.id.action_more_options:
				// display the advanced options
				findViewById(R.id.viewLocation).setVisibility(View.VISIBLE);
				findViewById(R.id.viewAudience).setVisibility(View.VISIBLE);

				// update the menu to hide the expand button
				invalidateOptionsMenu();

				return true;
			default:
				finish(); // destroy this Activity to go back to the parent AbstractMessagesActivity
				return true;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mEditTextMessage.removeTextChangedListener(mTextWatcher);
		setLoading(false);
	}

	@Override
	public void onReceivedMessages(int status, int mode, int page, boolean reachedEnd, long latestMessageID, int subscriptionUpdates, List<Message> messages) { }

	@Override
	public void onSentMessage(final int status, final String messageText, final String messageTopic, final String messageID, final long messageTime, final String messageColorHex, final int messagePatternID, final String messageCountryISO3) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				setLoading(false);
				if (status == Server.STATUS_OK) {
					final Message publishedMessage = new Message(messageID, Message.DEGREE_SELF, messageColorHex, messagePatternID, messageText, messageTopic, messageTime, 0, 0, messageCountryISO3, Message.Type.NORMAL, null);
					Intent backToMainScreen = new Intent(ActivityAdd.this, ActivityMain.class);
					backToMainScreen.putExtra(ActivityMain.EXTRA_NEW_MESSAGE, publishedMessage);
					startActivity(backToMainScreen);
					finish();
				}
				else if (status == Server.STATUS_MAINTENANCE) {
					Toast.makeText(ActivityAdd.this, R.string.error_maintenance, Toast.LENGTH_SHORT).show();
				}
				else if (status == Server.STATUS_BAD_REQUEST) {
					Toast.makeText(ActivityAdd.this, R.string.error_bad_request, Toast.LENGTH_SHORT).show();
				}
				else if (status == Server.STATUS_OUTDATED_CLIENT) {
					Toast.makeText(ActivityAdd.this, R.string.error_outdated_client, Toast.LENGTH_SHORT).show();
				}
				else if (status == Server.STATUS_NOT_AUTHORIZED) {
					startActivity(new Intent(ActivityAdd.this, ActivityMain.class));
					finish();
				}
				else if (status == Server.STATUS_TEMPORARILY_BANNED) {
					Toast.makeText(ActivityAdd.this, R.string.error_temporarily_banned, Toast.LENGTH_SHORT).show();
				}
				else if (status == Server.STATUS_LOGIN_THROTTLED) {
					Global.showLoginThrottledInfo(ActivityAdd.this);
				}
				else if (status == Server.STATUS_NO_CONNECTION) {
					Toast.makeText(ActivityAdd.this, R.string.error_no_connection, Toast.LENGTH_SHORT).show();
				}
			}

		});
	}

	@Override
	public void onReceivedDetails(int status, boolean isFavorited, boolean isSubscribed) { }

}
