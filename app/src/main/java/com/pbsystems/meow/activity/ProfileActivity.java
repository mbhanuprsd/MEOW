package com.pbsystems.meow.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pbsystems.meow.R;
import com.pbsystems.meow.app.MyApplication;
import com.pbsystems.meow.service.ChatService;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.iqregister.packet.Registration;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;

import java.io.ByteArrayOutputStream;

public class ProfileActivity extends Activity {

    protected static final int FILE_SELECT_CODE = 2343;
    AbstractXMPPConnection connection;
    String userid;
    TextView pUsername, pfullName, pPhone, pEmail, pRegistered;
    ImageView ivProfilePic, ivProfileIcon;
    Button btnSave, btnCancel, btnChange;
    byte[] imgbyteArray;

    VCard userVCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userVCard = null;

        loadResources();

        MyApplication app = (MyApplication) getApplication();
        connection = app.getConnection();

        if (connection == null) {
            finish();
        }

        userid = connection.getUser().substring(0,
                connection.getUser().indexOf("@"));
        new loadVcard().execute("");
    }

    void loadResources() {
        pUsername = (TextView) findViewById(R.id.profile_username);
        btnSave = (Button) findViewById(R.id.profile_edit);
        btnCancel = (Button) findViewById(R.id.profile_cancel);
        btnChange = (Button) findViewById(R.id.change_pic);
        ivProfilePic = (ImageView) findViewById(R.id.profile_pic);
        pfullName = (TextView) findViewById(R.id.profile_fullname);
        pPhone = (TextView) findViewById(R.id.profile_phone);
        pEmail = (TextView) findViewById(R.id.profile_email);
        pRegistered = (TextView) findViewById(R.id.profile_registered);
        ivProfileIcon = (ImageView) findViewById(R.id.profile_icon);

        btnChange.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                try {
                    startActivityForResult(intent, FILE_SELECT_CODE);
                } catch (Exception ex) {
                    Toast.makeText(ProfileActivity.this,
                            "Please install a File Manager.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    Intent serviceIntent = new Intent(getBaseContext(),
                            ChatService.class);
                    serviceIntent.putExtra("image", imgbyteArray);
                    startService(serviceIntent);
                } catch (Exception e) {
                    Log.e("Vcard", "Failed to start service");
                }
            }
        });

        btnCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private class loadVcard extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            ProviderManager.addIQProvider("vCard", "vcard-temp", new VCardProvider());
            VCardManager vCardManager = VCardManager.getInstanceFor(connection);
            try {
                userVCard = vCardManager.loadVCard();
                Log.i("VCARD", "Vcard loaded");
            } catch (Exception e) {
                Log.e("Vcard", e.getLocalizedMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            pUsername.setText(userid);

            if (userVCard != null) {
                try{
                    Bitmap bitmap;
                    if (userVCard.getAvatar() == null) {
                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user);
                    } else {
                        byte[] imgBytes = userVCard.getAvatar();
                        bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                    }
                    ivProfilePic.setImageBitmap(bitmap);
                    ivProfileIcon.setImageBitmap(bitmap);
                    try{
                        AccountManager accManager = AccountManager.getInstance(connection);
                        pfullName.setText(accManager.getAccountAttribute("name"));
                        pEmail.setText(accManager.getAccountAttribute("email"));
                        pRegistered.setText(accManager.getAccountAttribute("date"));
                        Log.i("Registered", ""+accManager.getAccountAttribute("date"));
                    }catch (Exception e){
                        Log.i("Profile", "Error loading accountmanager");
                    }
                }catch (Exception e){
                    Log.i("Profile", "Error loading profile");
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    if (uri.toString().length() > 3) {
                        try {
                            Bitmap temp = decodeUri(ProfileActivity.this, uri, 150);
                            Bitmap bmp = getCroppedBitmap(temp);
                            if (bmp != null) {
                                ivProfilePic.setImageBitmap(bmp);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                imgbyteArray = stream.toByteArray();
                                btnSave.setVisibility(View.VISIBLE);
                            } else {
                                Toast.makeText(ProfileActivity.this,
                                        "Image cannot load", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ProfileActivity.this,
                                    "Image cannot load", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static Bitmap decodeUri(Context c, Uri uri, final int requiredSize)
            throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o);

        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;

        while (true) {
            if (width_tmp / 2 < requiredSize || height_tmp / 2 < requiredSize)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o2);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
