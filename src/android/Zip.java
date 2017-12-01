package org.apache.cordova;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.lingala.zip4j.core.ZipFile;

import android.net.Uri;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi.OpenForReadResult;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Zip extends CordovaPlugin {

    private static final String LOG_TAG = "Zip";

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {

        if ("unzip".equals(action)) {

            unzip(args, callbackContext);

            return true;
        }

        return false;
    }

    private void unzip(final CordovaArgs args, final CallbackContext callbackContext) {

        this.cordova.getThreadPool().execute(new Runnable() {

            public void run() {
                
                unzipSync(args, callbackContext);
            }
        });
    }

    private void unzipSync(CordovaArgs args, CallbackContext callbackContext) {

        try {

            String zipFileName = args.getString(0);

            String outputDirectory = args.getString(1);

            String password = args.getString(2);

            ProgressEvent progress = new ProgressEvent();

            boolean anyEntries = false;

            CordovaResourceApi resourceApi = webView.getResourceApi();

            ZipFile zipFile = new ZipFile(resourceApi.mapUriToFile(getUriForArg(zipFileName)));

            if (zipFile.isEncrypted()) {

                zipFile.setPassword(password);
            }

            File outputDirectoryFile = resourceApi.mapUriToFile(getUriForArg(outputDirectory));

            outputDirectoryFile.mkdirs();

            zipFile.extractAll(outputDirectoryFile.getAbsolutePath());

            progress.setLoaded(100);

            updateProgress(callbackContext, progress);

            callbackContext.success();

        } catch (Exception e) {

            String errorMessage = "An error occurred while unzipping: " + e.getMessage();

            callbackContext.error(errorMessage);

            Log.e(LOG_TAG, errorMessage, e);

        }
    }

    private void updateProgress(CallbackContext callbackContext, ProgressEvent progress) throws JSONException {

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, progress.toJSONObject());

        pluginResult.setKeepCallback(true);

        callbackContext.sendPluginResult(pluginResult);
    }

    private Uri getUriForArg(String arg) {

        CordovaResourceApi resourceApi = webView.getResourceApi();

        Uri tmpTarget = Uri.parse(arg);

        return resourceApi.remapUri(tmpTarget.getScheme() != null ? tmpTarget : Uri.fromFile(new File(arg)));
    }

    private static class ProgressEvent {

        private long loaded;

        private long total;

        public long getLoaded() {

            return loaded;
        }
        public void setLoaded(long loaded) {

            this.loaded = loaded;
        }
        public void addLoaded(long add) {

            this.loaded += add;
        }
        public long getTotal() {

            return total;
        }
        public void setTotal(long total) {

            this.total = total;
        }

        public JSONObject toJSONObject() throws JSONException {

            return new JSONObject(

                    "{loaded:" + loaded +
                    ",total:" + total + "}");
        }
    }
}
