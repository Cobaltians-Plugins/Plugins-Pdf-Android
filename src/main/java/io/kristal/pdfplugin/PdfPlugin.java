/**
 * SharePlugin
 * The MIT License (MIT)
 * Copyright (c) 2016 Cobaltians
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

package io.kristal.pdfplugin;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.cobaltians.cobalt.Cobalt;
import org.cobaltians.cobalt.fragments.CobaltFragment;
import org.cobaltians.cobalt.plugin.CobaltAbstractPlugin;
import org.cobaltians.cobalt.plugin.CobaltPluginWebContainer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import io.kristal.pdfplugin.pdfRenderer.FullScreenActivity;

/**
 * Created by Roxane P. on 4/18/16.
 * SharePlugin
 * Start an intent for sharing a file from data given by a Json Object Message
 */
public class PdfPlugin extends CobaltAbstractPlugin {

    public final static String TAG = PdfPlugin.class.getSimpleName();
    private static final String PDF_APP = "pdf";

    /**************************************************************************************
     * MEMBERS
     **************************************************************************************/

    protected static PdfPlugin sInstance;
    // fragment handler
    public static CobaltFragment currentFragment;
    public static Context currentContext;
    public static Activity currentActivity;

    // path to the pdf, can be a folder from assets or an url
    public static String inputPdfPath;

    /**************************************************************************************
     * CONSTANTS MEMBERS
     **************************************************************************************/

    public static final String JS_TOKEN_DATA = "data";
    public static final String JS_TOKEN_PATH = "path";
    public static final String JS_TOKEN_SOURCE = "source";

    /**************************************************************************************
     * CONFIGURATION
     * *************************************************************************************/

    public static String downloadedPdfPath;
    public static String pdfFileName;

    /**************************************************************************************
     * CONSTRUCTORS
     **************************************************************************************/

    public static CobaltAbstractPlugin getInstance(CobaltPluginWebContainer webContainer) {
        if (sInstance == null) sInstance = new PdfPlugin();
        sInstance.addWebContainer(webContainer);
        return sInstance;
    }

    @Override
    public void onMessage(CobaltPluginWebContainer webContainer, JSONObject message) {
        if (Cobalt.DEBUG) Log.d(TAG, "onMessage called with message: " + message.toString());
        currentFragment = webContainer.getFragment();
        currentContext = currentFragment.getContext();
        currentActivity = currentFragment.getActivity();

        // PDF storage configuration
        downloadedPdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "PDFs/";
        pdfFileName = "default.pdf";

        try {
            String action = message.getString(Cobalt.kJSAction);
            if (action.equals(PDF_APP)) {
                // setting up PDF plugin
                CobaltFragment fragment = webContainer.getFragment();
                // parse JSON from web side
                JSONObject pdfData = message.getJSONObject(JS_TOKEN_DATA);
                if (pdfData == null) {
                    Log.e(TAG, "Fatal: Parsed data is null, check your javascript syntax.");
                    return;
                }
                // get pdf source and associated url/path
                inputPdfPath = pdfData.getString(JS_TOKEN_PATH);
                String inputPdfSource = pdfData.getString(JS_TOKEN_SOURCE);
                // create folder for pdf storage
                Tools.createFolder(PdfPlugin.downloadedPdfPath);
                // open pdf according to source
                switch (inputPdfSource) {
                    case "url":
                        pdfFileName = URLUtil.guessFileName(inputPdfPath, null, null);
                        // Verify that inputPdfPath start with the common protocol key
                        if (!inputPdfPath.startsWith("http://") && !inputPdfPath.startsWith("https://")) {
                            inputPdfPath = "http://" + inputPdfPath;
                        }
                        // download and open pdf
                        File file = new File(downloadedPdfPath, pdfFileName);
                        if (file.exists()) { // no need to download a new one
                            PdfIntent();
                        } else {
                            PdfPlugin.currentFragment.getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    new DownloadFileAsync().execute(inputPdfPath);
                                }
                            });
                        }
                        break;
                    case "local":
                        // keep only the last word to find the file name
                        int indexOfToken = inputPdfPath.indexOf('/') + 1; // +1 because of '/'
                        pdfFileName = inputPdfPath.substring(indexOfToken, inputPdfPath.length());
                        // copy & store it into phone storage
                        copyReadAssets();
                        PdfIntent();
                        break;
                    default:
                        Log.e(TAG, "Invalid source " + inputPdfSource + ". Correct values are <url> or <local>");
                        return;
                }

            } else if (Cobalt.DEBUG)
                Log.e(TAG, "onMessage: invalid action " + action + " in message " + message.toString() + ".");
        } catch (JSONException exception) {
            if (Cobalt.DEBUG) {
                Log.d(TAG, "onMessage: missing action key in message " + message.toString() + ".");
                exception.printStackTrace();
            }
        }
    }

    /**
     * PdfIntent
     * Open a pdf according to the device capability
     */
    private void PdfIntent() {
        // if launch with PdfRenderer failed,
        // check and open pdf if PDF Reader exist on device
        // if no pdf reader are found, open with a browser intent
        if (!openPdfInApp() && !openPdfInLocalApp()){
            openPdfInBrowser();
        }
    }

    /**
     * openPdfInApp
     * check and open pdf if PDF Reader exist on device
     */
    private boolean openPdfInApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Cobalt.DEBUG)
                Log.d(PdfPlugin.TAG, PdfPlugin.pdfFileName + " PDF opened with the in-app PDF Renderer.");
            Intent intent = new Intent(currentActivity, FullScreenActivity.class);
            currentActivity.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    /**
     * openPdfInLocalApp
     * check and open pdf if PDF Reader exist on device
     */
    private boolean openPdfInLocalApp() {
        // check if PDF Reader exist on device
        PackageManager packageManager = currentContext.getPackageManager();
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        testIntent.setType("application/pdf");
        List list = packageManager.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() > 0) {
            // at least 1 pdf application exist
            if (Cobalt.DEBUG)
                Log.d(PdfPlugin.TAG, PdfPlugin.pdfFileName + " PDF opened with a local PDF Reader app.");
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(new File(downloadedPdfPath + pdfFileName));
            intent.setDataAndType(uri, "application/pdf");
            PdfPlugin.currentContext.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    /**
     * openPdfInBrowser
     * Open with a browser intent with google drive and pdf url
     */
    private void openPdfInBrowser() {
        if (Cobalt.DEBUG)
            Log.d(PdfPlugin.TAG, PdfPlugin.pdfFileName + " PDF opened with a browser intent.");
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://drive.google.com/viewer?url=" + inputPdfPath));
        currentActivity.startActivity(browserIntent);
    }

    /**
     * copyReadAssets
     * Used to retrieve files from app assets
     * Copy file to the device's download folder
     */
    public static void copyReadAssets() {
        InputStream in;
        OutputStream out;
        AssetManager assetManager = PdfPlugin.currentContext.getAssets();
        File fileDir = Tools.createFolder(PdfPlugin.downloadedPdfPath);
        File file = new File(fileDir, PdfPlugin.pdfFileName);
        try {
            in = assetManager.open(PdfPlugin.inputPdfPath);
            out = new BufferedOutputStream(new FileOutputStream(file));
            Tools.copyFile(in, out);
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e(PdfPlugin.TAG, e.getMessage());
        }
    }

    /**
     * DownloadFileAsync asynchronous thread
     * downloading a file from a direct url
     */
    private class DownloadFileAsync extends AsyncTask<String, String, String> {
        private PowerManager.WakeLock mWakeLock;
        ProgressDialog mProgressDialog = null;

        public DownloadFileAsync() {
            mProgressDialog = new ProgressDialog(PdfPlugin.currentContext);
            mProgressDialog.setMessage("Downloading attachment in progress..."); // TODO: 5/3/16 multilingual
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) PdfPlugin.currentContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection urlConnection;
            try {
                URL mUrl = new URL(PdfPlugin.inputPdfPath);
                urlConnection = (HttpURLConnection) mUrl.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout (5000) ;
                urlConnection.connect();
                // create file
                File file = new File(PdfPlugin.downloadedPdfPath, PdfPlugin.pdfFileName);
                if (Cobalt.DEBUG)
                    Log.d(PdfPlugin.TAG, PdfPlugin.pdfFileName + " will be stored in " + file.getAbsolutePath());
                // check if connection return proper response
                if (urlConnection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    Tools.deleteFile(file.getAbsolutePath());
                    Log.e(PdfPlugin.TAG, "HttpURLConnection return bad code: " + urlConnection.getResponseCode() + " when downloading from url " + PdfPlugin.inputPdfPath);
                    return null;
                }
                // init stream to be copied
                InputStream input = new BufferedInputStream(mUrl.openStream());
                OutputStream output = new FileOutputStream(PdfPlugin.downloadedPdfPath + PdfPlugin.pdfFileName);
                // write file
                int count;
                long total = 0;
                byte data[] = new byte[1024];
                while ((count = input.read(data)) > 0) {
                    total += count;
                    publishProgress("" + (int) ((total * 100) / urlConnection.getContentLength()));
                    output.write(data, 0, count);
                }
                // close stream
                output.flush();
                output.close();
                input.close();
                return String.valueOf(urlConnection.getContentLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate(progress);
            // update dialog progress
            mProgressDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String result) {
            // remove CPU lock
            mWakeLock.release();
            // remove dialog as the download is complete
            mProgressDialog.dismiss();
            if (result != null) {
                if (Cobalt.DEBUG) Log.d(PdfPlugin.TAG, "File downloaded length " + result);
                Toast.makeText(PdfPlugin.currentFragment.getContext(), "Downloading completed in " + PdfPlugin.inputPdfPath, Toast.LENGTH_LONG).show();
                // finally, open document
                PdfIntent();
            } else {
                Log.e(PdfPlugin.TAG, "Download error: No internet connection.");
                Toast.makeText(PdfPlugin.currentFragment.getContext(), "Download error: No internet connection.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
