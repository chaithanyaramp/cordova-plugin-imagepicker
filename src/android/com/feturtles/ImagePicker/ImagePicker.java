/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.feturtles.ImagePicker;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;

public class ImagePicker extends CordovaPlugin {

    private static final String ACTION_GET_PICTURES = "getPictures";
    private static final String ACTION_HAS_READ_PERMISSION = "hasReadPermission";
    private static final String ACTION_REQUEST_READ_PERMISSION = "requestReadPermission";

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int ACTIVITY_RESULT_CANCELED = 1;
    private static final int ACTIVITY_RESULT_OK = 2;

    private CallbackContext callbackContext;

    private static final int CREATE_REQUEST_CODE = 40;
    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;
    private String TAG = "IMAGEPICKER";
    private Map<String, Integer> fileNames = new HashMap<String, Integer>();
    private Cursor imagecursor, actualimagecursor;
    private int maxImages = 20;
    private int maxImageCount = 20;

    private int desiredWidth = 800;
    private int desiredHeight = 800;
    private int quality = 80;
    private OutputType outputType = OutputType.fromValue(0);
    //    private ProgressDialog progress = new ProgressDialog(cordova.getActivity().getApplicationContext());
    private FakeR fakeR;

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (ACTION_HAS_READ_PERMISSION.equals(action)) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, hasReadPermission()));
            return true;

        } else if (ACTION_REQUEST_READ_PERMISSION.equals(action)) {
            requestReadPermission();
            return true;

        } else if (ACTION_GET_PICTURES.equals(action)) {
//            progress.setTitle(cordova.getActivity().getApplicationContext().getString(fakeR.getId("string", "multi_image_picker_processing_images_title")));
//            progress.setMessage(cordova.getActivity().getApplicationContext().getString(fakeR.getId("string", "multi_image_picker_processing_images_message")));

            final JSONObject params = args.getJSONObject(0);
//            final Intent imagePickerIntent = new Intent(cordova.getActivity(), MultiImageChooserActivity.class);
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

//            startActivityForResult(intent, OPEN_REQUEST_CODE);

            int max = 20;
            int desiredWidth = 0;
            int desiredHeight = 0;
            int quality = 100;
            int outputType = 0;
            if (params.has("maximumImagesCount")) {
                max = params.getInt("maximumImagesCount");
            }
            if (params.has("width")) {
                desiredWidth = params.getInt("width");
            }
            if (params.has("height")) {
                desiredHeight = params.getInt("height");
            }
            if (params.has("quality")) {
                quality = params.getInt("quality");
            }
            if (params.has("outputType")) {
                outputType = params.getInt("outputType");
            }

            this.desiredWidth = desiredWidth;
            this.desiredHeight = desiredHeight;
            this.maxImageCount = max;
            this.outputType = OutputType.fromValue(outputType);
            this.quality = quality;
//            if(this.maxImageCount > 1){
//                this.multiple = true;
//            }else{
//                this.multiple = false;
//            }


            // .. until then use:
            if (hasReadPermission()) {
                cordova.startActivityForResult(this, intent, OPEN_REQUEST_CODE);
            } else {
                requestReadPermission();
                // The downside is the user needs to re-invoke this picker method.
                // The best thing to do for the dev is check 'hasReadPermission' manually and
                // run 'requestReadPermission' or 'getPictures' based on the outcome.
            }
            return true;
        }
        return false;
    }

    @SuppressLint("InlinedApi")
    private boolean hasReadPermission() {
        return Build.VERSION.SDK_INT < 23 ||
                PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @SuppressLint("InlinedApi")
    private void requestReadPermission() {
        if (!hasReadPermission()) {
            ActivityCompat.requestPermissions(
                    this.cordova.getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
        // This method executes async and we seem to have no known way to receive the result
        // (that's why these methods were later added to Cordova), so simply returning ok now.
        callbackContext.success();
    }


    /**
     * Added By ARIHANT FROM HERE ONWARDS
     */
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CREATE_REQUEST_CODE) {
                if (resultData != null) {
                    Log.d(TAG, "CREATE_REQUEST_CODE Received");
                }
            } else if (requestCode == SAVE_REQUEST_CODE) {

                if (resultData != null) {
                    Uri currentUri =
                            resultData.getData();
//                    writeFileContent(currentUri);
                    Log.d(TAG, "Content URI: " + currentUri.toString());
                }
            } else if (requestCode == OPEN_REQUEST_CODE) {

                if (resultData != null) {
                        final ClipData clipData = resultData.getClipData();
                        int takeFlags = resultData.getFlags();
                        takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        ArrayList<Uri> uris = new ArrayList<Uri>();
                        if(clipData != null){
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                ClipData.Item item = clipData.getItemAt(i);
                                Uri dataUri = item.getUri();
                                if (dataUri != null) {
                                    uris.add(dataUri);
                                    Log.d(TAG, "File URI : " + dataUri.toString());
                                }
                            }
                        }else{
                            Uri dataUri = resultData.getData();
                            if (dataUri != null) {
                                uris.add(dataUri);
                            }
                        }
                        processFiles(uris);
                }


//                    try {
//                        String content =
//                                readFileContent(currentUri);
//                        textView.setText(content);
//                    } catch (IOException e) {
//                        // Handle error here
//                    }

            } else {
                Log.d(TAG, "Received Code : " + String.valueOf(requestCode));
            }

        }
    }

    private void processFiles(ArrayList<Uri> uris) {
//        progress.show();
        if (uris.isEmpty()) {
            sendResultsToCordova(ACTIVITY_RESULT_CANCELED, null);
//            progress.dismiss();
        } else if(uris.size() > this.maxImageCount){
            Intent data = new Intent();
            Bundle res = new Bundle();
            res.putString("ERRORMESSAGE", "Cannot select more than " + String.valueOf(this.maxImageCount) + " Images");
            data.putExtras(res);
            sendResultsToCordova(ACTIVITY_RESULT_CANCELED, data);
        } else {
            new ResizeImagesTask().execute(uris);
        }
    }

    private class ResizeImagesTask extends AsyncTask<ArrayList<Uri>, Void, ArrayList<String>> {
        private Exception asyncTaskError = null;

        @Override
        protected ArrayList<String> doInBackground(ArrayList<Uri>... fileSets) {
            ArrayList<String> al = new ArrayList<String>();
            ContentResolver cr = cordova.getActivity().getApplicationContext().getContentResolver();
            try {
                ArrayList<Uri> urisCopy = fileSets[0];
                Iterator<Uri> i = urisCopy.iterator();
                Bitmap bmp;
                while (i.hasNext()) {
                    Uri fileuri = i.next();
                    File file = new File(fileuri.toString()); // TODO: IT WONT WORK.
                    int rotate = 0; // TODO:
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    InputStream is = cordova.getActivity().getApplicationContext().getContentResolver().openInputStream(fileuri);
                    BitmapFactory.decodeStream(is, null, options); // TODO: Earlier it was options.
                    int width = options.outWidth;
                    int height = options.outHeight;
                    float scale = calculateScale(width, height);

                    if (scale < 1) {
                        int finalWidth = (int) (width * scale);
                        int finalHeight = (int) (height * scale);
                        int inSampleSize = calculateInSampleSize(options, finalWidth, finalHeight);
                        options = new BitmapFactory.Options();
                        options.inSampleSize = inSampleSize;

                        try {
                            bmp = this.tryToGetBitmap(fileuri, options, rotate, true);
                        } catch (OutOfMemoryError e) {
                            options.inSampleSize = calculateNextSampleSize(options.inSampleSize);
                            try {
                                bmp = this.tryToGetBitmap(fileuri, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                throw new IOException("Unable to load image into memory.");
                            }
                        }
                    } else {
                        try {
                            bmp = this.tryToGetBitmap(fileuri, null, rotate, false);
                        } catch (OutOfMemoryError e) {
                            options = new BitmapFactory.Options();
                            options.inSampleSize = 2;

                            try {
                                bmp = this.tryToGetBitmap(fileuri, options, rotate, false);
                            } catch (OutOfMemoryError e2) {
                                options = new BitmapFactory.Options();
                                options.inSampleSize = 4;

                                try {
                                    bmp = this.tryToGetBitmap(fileuri, options, rotate, false);
                                } catch (OutOfMemoryError e3) {
                                    throw new IOException("Unable to load image into memory.");
                                }
                            }
                        }
                    }

                    if (outputType == OutputType.FILE_URI) {

                        String fileType = cr.getType(fileuri);
                        String ext = "";
                        if(fileType.equals("image/png")){
                          ext = ".png";
                        }else{
                          ext = ".jpeg";
                        }
                        file = storeImage(bmp, file.getName(), ext);
                        al.add(Uri.fromFile(file).toString());

                    } else if (outputType == OutputType.BASE64_STRING) {
                        al.add(getBase64OfImage(bmp));
                    }
                }
                return al;
            } catch (IOException e) {
                try {
                    asyncTaskError = e;
                    for (int i = 0; i < al.size(); i++) {
                        URI uri = new URI(al.get(i));
                        File file = new File(uri);
                        file.delete();
                    }
                } catch (Exception ignore) {
                }

                return new ArrayList<String>();
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> al) {
            Intent data = new Intent();

            if (asyncTaskError != null) {
                Bundle res = new Bundle();
                res.putString("ERRORMESSAGE", asyncTaskError.getMessage());
                data.putExtras(res);
                sendResultsToCordova(ACTIVITY_RESULT_CANCELED, data);

            } else if (al.size() > 0) {
                Bundle res = new Bundle();
                res.putStringArrayList("MULTIPLEFILENAMES", al);

                if (imagecursor != null) {
                    res.putInt("TOTALFILES", imagecursor.getCount());
                }

                int sync = ResultIPC.get().setLargeData(res);
                data.putExtra("bigdata:synccode", sync);
                sendResultsToCordova(ACTIVITY_RESULT_OK, data);

            } else {
                sendResultsToCordova(ACTIVITY_RESULT_CANCELED, data);
            }
            Log.d(TAG, "TASK FINISH");

//            progress.dismiss();
//            finish();

        }

        private Bitmap tryToGetBitmap(Uri fileuri,
                                      BitmapFactory.Options options,
                                      int rotate,
                                      boolean shouldScale) throws IOException, OutOfMemoryError {
            Bitmap bmp;
            InputStream is = cordova.getActivity().getApplicationContext().getContentResolver().openInputStream(fileuri);
            if (options == null) {
                bmp = BitmapFactory.decodeStream(is);
            } else {
                bmp = BitmapFactory.decodeStream(is, null, options);
            }

            if (bmp == null) {
                throw new IOException("The image file could not be opened.");
            }

            if (options != null && shouldScale) {
                float scale = calculateScale(options.outWidth, options.outHeight);
                bmp = this.getResizedBitmap(bmp, scale);
            }

            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotate);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }

            return bmp;
        }

        /*
         * The following functions are originally from
         * https://github.com/raananw/PhoneGap-Image-Resizer
         *
         * They have been modified by Andrew Stephan for Sync OnSet
         *
         * The software is open source, MIT Licensed.
         * Copyright (C) 2012, webXells GmbH All Rights Reserved.
         */
        private File storeImage(Bitmap bmp, String fileName, String ext) throws IOException {
            String name = fileName;
            File file = File.createTempFile("tmp_" + name, ext);
            OutputStream outStream = new FileOutputStream(file);
            if (ext.compareToIgnoreCase(".png") == 0) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality, outStream);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, outStream);
            }
            outStream.flush();
            outStream.close();
            return file;
        }

        private Bitmap getResizedBitmap(Bitmap bm, float factor) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            // create a matrix for the manipulation
            Matrix matrix = new Matrix();
            // resize the bit map
            matrix.postScale(factor, factor);
            // recreate the new Bitmap
            return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        }

        private String getBase64OfImage(Bitmap bm) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private int calculateNextSampleSize(int sampleSize) {
        double logBaseTwo = (int) (Math.log(sampleSize) / Math.log(2));
        return (int) Math.pow(logBaseTwo + 1, 2);
    }

    private float calculateScale(int width, int height) {
        float widthScale = 1.0f;
        float heightScale = 1.0f;
        float scale = 1.0f;
        if (desiredWidth > 0 || desiredHeight > 0) {
            if (desiredHeight == 0 && desiredWidth < width) {
                scale = (float) desiredWidth / width;

            } else if (desiredWidth == 0 && desiredHeight < height) {
                scale = (float) desiredHeight / height;

            } else {
                if (desiredWidth > 0 && desiredWidth < width) {
                    widthScale = (float) desiredWidth / width;
                }

                if (desiredHeight > 0 && desiredHeight < height) {
                    heightScale = (float) desiredHeight / height;
                }

                if (widthScale < heightScale) {
                    scale = widthScale;
                } else {
                    scale = heightScale;
                }
            }
        }

        return scale;
    }

    enum OutputType {

        FILE_URI(0), BASE64_STRING(1);

        int value;

        OutputType(int value) {
            this.value = value;
        }

        public static OutputType fromValue(int value) {
            for (OutputType type : OutputType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid enum value specified");
        }
    }

    void sendResultsToCordova(int resultCode, Intent data) {
        if (resultCode == ACTIVITY_RESULT_OK && data != null) {
            int sync = data.getIntExtra("bigdata:synccode", -1);
            final Bundle bigData = ResultIPC.get().getLargeData(sync);

            ArrayList<String> fileNames = bigData.getStringArrayList("MULTIPLEFILENAMES");

            JSONArray res = new JSONArray(fileNames);
            callbackContext.success(res);

        } else if (resultCode == ACTIVITY_RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            callbackContext.error(error);

        } else if (resultCode == ACTIVITY_RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            callbackContext.success(res);

        } else {
            callbackContext.error("No images selected");
        }
    }

/*
    @Override
    public void onRequestPermissionResult(int requestCode,
                                          String[] permissions,
                                          int[] grantResults) throws JSONException {

        // For now we just have one permission, so things can be kept simple...
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cordova.startActivityForResult(this, imagePickerIntent, 0);
        } else {
            // Tell the JS layer that something went wrong...
            callbackContext.error("Permission denied");
        }
    }
*/
}
