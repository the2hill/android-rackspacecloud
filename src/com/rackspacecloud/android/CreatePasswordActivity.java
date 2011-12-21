package com.rackspacecloud.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.rackspace.cloud.android.R;

public class CreatePasswordActivity extends CloudActivity {

	private PasswordManager pwManager;
	private EditText passwordText;
	private EditText confirmText;
	private Button submitPassword;
	private CheckBox passwordCheckBox;
	private boolean isChecked;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.password);
		pwManager = new PasswordManager(getSharedPreferences(
				Preferences.SHARED_PREFERENCES_NAME, MODE_PRIVATE));
		restoreState(savedInstanceState);
	}

	protected void restoreState(Bundle state) {
		super.restoreState(state);
		if (state != null && state.containsKey("isChecked")) {
			isChecked = state.getBoolean("isChecked");
		}
		else{
			isChecked = hadPassword();
		}
		setUpWidgets();

	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("isChecked", passwordCheckBox.isChecked());
	}

	private void setUpWidgets() {
		setUpCheckBox();
		passwordText = (EditText) findViewById(R.id.password_edittext);
		confirmText = (EditText) findViewById(R.id.confirm_edittext);
		if(!passwordCheckBox.isChecked()){
			passwordText.setEnabled(false);
			confirmText.setEnabled(false);
		}
		setUpSubmit();
	}

	private void setUpCheckBox() {
		passwordCheckBox = (CheckBox) findViewById(R.id.password_checkbox);
		passwordCheckBox.setChecked(isChecked);
		passwordCheckBox.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (((CheckBox) v).isChecked()) {
					passwordText.setEnabled(true);
					confirmText.setEnabled(true);
				} else {
					passwordText.setEnabled(false);
					confirmText.setEnabled(false);
				}
			}
		});
	}


	/*
	 * sets up submit button to deal with each
	 * case of starting having/not having a password
	 * and ending with having/not having a password
	 */
	private void setUpSubmit() {
		submitPassword = (Button) findViewById(R.id.create_password_button);
		submitPassword.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// didn't have password before and still doesn't
				if (!hadPassword() && !willHavePassword()) {
					finish();
				}
				// didnt have password before and does now
				else if (!hadPassword() && willHavePassword()) {
					if (passwordsMatch()) {
						if(validInputs()){
							pwManager.changePassword(passwordText.getText().toString());
							showToast("Password has been enabled.");
							finish();
						}
						else{
							showAlert("Missing Field",
							"Password and confirmation are required.");
						}
					} else {
						showAlert("Passwords must match",
						"Password and confirmation did not match. Try again.");
					}
				} else if (hadPassword()) {
					showDialog(R.id.create_password_button);

				}
			}
		});
	}

	/*
	 * handles the case where the was a password before
	 * the user will need to enter the old password
	 * in order to make any changes
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.id.create_password_button:		
			final EditText input = new EditText(this);
			input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
					| android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
			input.setText("");
			input.setMaxWidth(10);
			return new AlertDialog.Builder(CreatePasswordActivity.this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setView(input)
			.setTitle("Verification Required")
			.setMessage("Enter your old password: ")
			.setPositiveButton("Submit",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
					// had password before and doesnt now
					if (!willHavePassword()) {
						if (pwManager.verifyEnteredPassword(input.getText().toString())) {
							pwManager.turnOffPassword();
							showToast("Password has been disabled.");
							finish();
						} else {
							showAlert("Problem with password",
							"The entered password was incorrect");
							passwordCheckBox.setChecked(true);
							passwordText.setEnabled(true);
							confirmText.setEnabled(true);
						}
					}
					// had a password and still has one
					else {
						if (pwManager.verifyEnteredPassword(input.getText().toString())) {
							if (passwordsMatch()) {
								if(validInputs()){
									pwManager.changePassword(passwordText.getText().toString());
									showToast("Password has been changed.");
									finish();
								}
								else{
									showAlert("Missing Field",
									"Password and confirmation are required.");
								}
							} else {
								showAlert("Passwords must match",
								"Password and confirmation did not match. Try again.");
							}

						} else {
							showAlert("Problem with password",
							"The entered password was incorrect");
							passwordCheckBox.setChecked(true);
							passwordText.setEnabled(true);
							confirmText.setEnabled(true);
						}
					}
				}
			})
			.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
					removeDialog(R.id.create_password_button);
					//passwordCheckBox.setChecked(true);
				}
			}).create();
		}
		return null;
	}


	/*
	 * must ensure that the passwords are the same before you can submit
	 */
	private boolean passwordsMatch() {
		return passwordText.getText().toString()
		.equals(confirmText.getText().toString());
	}

	private boolean willHavePassword() {
		return passwordCheckBox.isChecked();
	}

	/*
	 * returns true if application required a 
	 * password before current edit
	 */
	private boolean hadPassword(){
		return pwManager.hasPassword();
	}

	/*
	 * checks that when the user submits
	 * the password fields are not empty
	 */
	private boolean validInputs(){
		return !passwordText.getText().toString().equals("") 
		|| !confirmText.getText().toString().equals("");
	}
}
