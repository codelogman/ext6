package tar.eof.ext6.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import github.nisrulz.qreader.QRDataListener;
import github.nisrulz.qreader.QREader;
import tar.eof.ext6.AppContext;
import tar.eof.ext6.Constants;
import tar.eof.ext6.R;
import tar.eof.ext6.fileoperations.Operations;
import tar.eof.ext6.filepicker.controller.DialogSelectionListener;
import tar.eof.ext6.filepicker.model.DialogConfigs;
import tar.eof.ext6.filepicker.model.DialogProperties;
import tar.eof.ext6.filepicker.view.FilePickerDialog;
import tar.eof.ext6.filepicker.widget.MaterialCheckbox;
import tar.eof.ext6.filepicker.widget.OnCheckedChangeListener;
import tar.eof.ext6.interfaces.IFuncPtr;

public class UIUtils {

	private static TextView namedrivecurrent;

	public static void ShowError(String msg, Context context)
	{
		AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(context);                      
	    dlgAlert.setMessage(msg);
	    dlgAlert.setTitle(context.getString(R.string.error_common));
	    dlgAlert.setIcon(android.R.drawable.ic_dialog_alert);
	    dlgAlert.setPositiveButton(context.getString(R.string.ok), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface popup, int arg1) {
				// TODO Auto-generated method stub
				popup.dismiss();
			}
		});
	    dlgAlert.setCancelable(false);
	    dlgAlert.show();
	}
	
	public static void ShowMsg(String msg, String title,Context context)
	{
		MaterialAlertDialogBuilder dlgAlert  = new MaterialAlertDialogBuilder(context);
	    dlgAlert.setMessage(msg);
	    dlgAlert.setTitle(title);
	    dlgAlert.setIcon(android.R.drawable.ic_dialog_info);
	    dlgAlert.setPositiveButton(context.getString(R.string.ok), new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface popup, int arg1) {
				// TODO Auto-generated method stub
				popup.dismiss();
			}
		});
	    dlgAlert.setCancelable(false);
	    dlgAlert.show();
	}
	
	public static void ShowToast(String msg,Context context)
	{
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}

	public static void showRadioButtonDialog(Context mContext, String[] options, String title, final RadioGroup.OnCheckedChangeListener listener) {

		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.filter_options, null);

		// custom dialog
		final Dialog dialog = new MaterialAlertDialogBuilder(mContext)
				.setTitle(title)
				.setView(v)
				.create();

		RadioGroup rg = (RadioGroup) v.findViewById(R.id.filter_group);
		BootstrapButton okButton = (BootstrapButton) v.findViewById(R.id.okbutton);
		for(int i=0;i<options.length;i++){
			RadioButton rb=new RadioButton(mContext); // dynamically creating RadioButton and adding to RadioGroup.
			rb.setText(options[i]);
			rb.setId(i);
			rg.addView(rb);
		}

		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int i) {
				listener.onCheckedChanged(radioGroup,i);
			}
		});

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.dismiss();
			}
		});

		dialog.show();
	}


	public static void showEditTextDialog(Context mContext, String initialText, String title_text, final IFuncPtr functionToBeRun) {

		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.dialog_with_text, null);

		// custom dialog
		final Dialog dialog = new MaterialAlertDialogBuilder(mContext)
				.setView(v)
				.create();

		BootstrapButton okButton = (BootstrapButton) v.findViewById(R.id.okbutton);
		final BootstrapEditText insertedText = (BootstrapEditText) v.findViewById(R.id.addedText);
		final TextView errordrive = (TextView) v.findViewById(R.id.error_directory);
		final TextView name_title = (TextView) v.findViewById(R.id.title_text);
		name_title.setText(title_text);

		insertedText.setText(initialText);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(!insertedText.getText().toString().isEmpty()) {
					functionToBeRun.execute(insertedText.getText().toString());
					dialog.dismiss();
				}else{
					errordrive.setVisibility(View.VISIBLE);
					errordrive.setText("*Directory NAME is empty !");
				}
			}
		});

		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		dialog.show();
	}

	private static String convertToHex(byte[] data) {
		StringBuilder buf = new StringBuilder();
		for (byte b : data) {
			int halfbyte = (b >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
				halfbyte = b & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] textBytes = text.getBytes("iso-8859-1");
		md.update(textBytes, 0, textBytes.length);
		byte[] sha1hash = md.digest();
		return convertToHex(sha1hash);
	}

	public static void BaseDriveDialog(Context mContext, String namedrive, final IFuncPtr functionToBeRun) {

		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.dialog_firts_run, null);
		TextView textdrive = (TextView) v.findViewById(R.id.textdrive);
		TextView textSha = (TextView) v.findViewById(R.id.sha);
		ImageButton qrbutton = (ImageButton) v.findViewById(R.id.qrbutton);

		SurfaceView mySurfaceView = (SurfaceView) v.findViewById(R.id.camera_view);

		ImageView checked = (ImageView) v.findViewById(R.id.imageCheck);

		final TextView errordrive = (TextView) v.findViewById(R.id.error_first);
		textdrive.setText("FileSystem name:  " + "[" + namedrive + "]");


		try {
			textSha.setText("SHA{"+SHA1(namedrive)+"}");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// custom dialog
		final Dialog dialog = new MaterialAlertDialogBuilder(mContext)
				.setView(v)
				.setCancelable(false)
				.create();

		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		BootstrapButton okButton = (BootstrapButton) v.findViewById(R.id.okBase);
		final BootstrapEditText PasswordText = (BootstrapEditText) v.findViewById(R.id.passwordbase);
		final BootstrapEditText ConfirmPassword = (BootstrapEditText) v.findViewById(R.id.passwordConfirmation);
		final BootstrapEditText LicenceText = (BootstrapEditText) v.findViewById(R.id.licence_text);

		QREader qrEader = new QREader.Builder(mContext, mySurfaceView, new QRDataListener() {
			@Override
			public void onDetected(final String data) {
				LicenceText.post(new Runnable() {
					@Override
					public void run() {
						LicenceText.setText(data);
					}
				});
			}
		}).facing(QREader.BACK_CAM)
				.enableAutofocus(true)
				.height(mySurfaceView.getHeight())
				.width(mySurfaceView.getWidth())
				.build();


		LicenceText.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {

				if((s.length() >= 43) && (s.length() <= 44)) {
					qrEader.releaseAndCleanup();
					mySurfaceView.setVisibility(View.GONE);
					qrbutton.setVisibility(View.GONE);
					checked.setVisibility(View.VISIBLE);
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});

		qrbutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mySurfaceView.setVisibility(View.VISIBLE);
					qrEader.initAndStart(mySurfaceView);
			}
		});

		KeyChecker licence = new KeyChecker();
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (licence.ValidatorKey(LicenceText.getText().toString())){
					if (!PasswordText.getText().toString().isEmpty() || !ConfirmPassword.getText().toString().isEmpty()) {
						if (!PasswordText.getText().toString().equals(ConfirmPassword.getText().toString())) {
							errordrive.setVisibility(View.VISIBLE);
							errordrive.setText("*Password Confirmation must be equal");
						} else {
							functionToBeRun.execute(PasswordText.getText().toString());
							qrEader.releaseAndCleanup();
							dialog.dismiss();
						}
					} else {
						errordrive.setVisibility(View.VISIBLE);
						errordrive.setText("*Password is empty !");
					}
			}else{
					errordrive.setVisibility(View.VISIBLE);
					errordrive.setText("*Invalid Licence Key !");
				}
			}
		});

		dialog.show();
	}

	public static void CurrentDriveDialog(Context mContext, final IFuncPtr functionToBeRun) {

		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.dialog_import_drive, null);
		MaterialCheckbox check = (MaterialCheckbox) v.findViewById(R.id.import_base_checkbox);
		TextView namedrivebase = (TextView) v.findViewById(R.id.textBasedrive);
		TextView errorcurrent = (TextView) v.findViewById(R.id.error_current);
		namedrivecurrent = (TextView) v.findViewById(R.id.textdriveImport);
		ImageButton addfile = (ImageButton) v.findViewById(R.id.selecFiles);
		LinearLayout layoutbase = (LinearLayout) v.findViewById(R.id.baseview);

		LinearLayout filepickerview = (LinearLayout) v.findViewById(R.id.filepickerview);

		File nameDrive = new File(PreferenceStorage.getBaseDrive());

		addfile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogProperties properties = new DialogProperties();
				properties.selection_mode = DialogConfigs.MULTI_MODE;
				properties.selection_type = DialogConfigs.FILE_SELECT;
				properties.root = new File(DialogConfigs.DEFAULT_DIR);
				properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
				properties.offset = new File(DialogConfigs.DEFAULT_DIR);
				properties.alsodelete = DialogConfigs.ALSODELETE;
				properties.extensions = new String[]{"vfs","vf"};


				FilePickerDialog dialog = new FilePickerDialog(mContext,properties);
				dialog.setTitle("import drive file");

				dialog.setDialogSelectionListener(new DialogSelectionListener() {
					@Override
					public void onSelectedFilePaths(String[] files, boolean alsodelete) {
						namedrivecurrent.setText(files[0]);
					}

					@Override
					public void onCancelSelection() {
					}
				});

				dialog.show();
			}
		});

		check.setChecked(false);
		check.setOnCheckedChangedListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(MaterialCheckbox checkbox, boolean isChecked) {
				if(isChecked) {
					namedrivecurrent.setText("(none selected)");
					filepickerview.setVisibility(View.GONE);
				}
				else
					filepickerview.setVisibility(View.VISIBLE);

			}
		});

		if(!PreferenceStorage.getBaseDrive().equals(PreferenceStorage.getCurrentDrive())){
			layoutbase.setVisibility(View.VISIBLE);
			namedrivebase.setText("Restore Base Drive: " + nameDrive.getName());
		}

		// custom dialog
		final Dialog dialog = new MaterialAlertDialogBuilder(mContext)
				.setView(v)
				.create();

		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		BootstrapButton okButton = (BootstrapButton) v.findViewById(R.id.okCurrent);
		final BootstrapEditText insertedText = (BootstrapEditText) v.findViewById(R.id.passwordcurrent);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				if(check.isChecked()) {
					functionToBeRun.execute(PreferenceStorage.getBaseDrive());
					dialog.dismiss();
				}

				if(!namedrivecurrent.getText().toString().equals("(none selected)") && !check.isChecked()) {
					functionToBeRun.execute(namedrivecurrent.getText().toString());
					dialog.dismiss();
				}else{
					errorcurrent.setText("*Specify Encrypted File System or restore Base Drive");
					errorcurrent.setVisibility(View.VISIBLE);
				}
			}
		});

		dialog.show();
	}


	public static void LicenceDialog(Context mContext, String namedrive, boolean cancelable, final IFuncPtr functionToBeRun) {

		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.dialog_licence, null);
		TextView textSha = (TextView) v.findViewById(R.id.sha1);
		final TextView errordrive = (TextView) v.findViewById(R.id.error_first1);


		try {
			textSha.setText("SHA{"+SHA1(namedrive)+"}");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// custom dialog
		final Dialog dialog = new MaterialAlertDialogBuilder(mContext)
				.setView(v)
				.setCancelable(cancelable)
				.create();

		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		BootstrapButton okButton = (BootstrapButton) v.findViewById(R.id.okBase1);
		final BootstrapEditText LicenceText = (BootstrapEditText) v.findViewById(R.id.licence_text1);

		KeyChecker licence = new KeyChecker();
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(!LicenceText.getText().toString().isEmpty()) {
					if (licence.ValidatorKey(LicenceText.getText().toString())) {
						functionToBeRun.execute(LicenceText.getText().toString());
						dialog.dismiss();
					} else {
						errordrive.setVisibility(View.VISIBLE);
						errordrive.setText("Invalid Licence Key !");
					}
				}else{
					errordrive.setVisibility(View.VISIBLE);
					errordrive.setText("Empty Licence Key !");
				}
			}
		});

		dialog.show();
	}


}
