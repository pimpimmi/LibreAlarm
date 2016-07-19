package com.pimpimmobile.librealarm.nightscout;

import android.content.Context;
import android.util.Log;

import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.PreferencesUtil;

import org.apache.http.Header;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * THIS CLASS WAS ORIGNALLY BUILT BY THE NIGHTSCOUT GROUP FOR THEIR NIGHTSCOUT ANDROID UPLOADER
 * https://github.com/nightscout/android-uploader/
 * IT WAS THEN MODIFIED BY STEPHEN BLACK
 * https://github.com/StephenBlackWasAlreadyTaken/xDrip/
 * Finally there has been modifications to fit this projects needs.
 */
public class NightscoutUploader {
    private static final String TAG = NightscoutUploader.class.getSimpleName();
    private static final int SOCKET_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 30000;
    private Context mContext;
    private Boolean enableRESTUpload;

    public NightscoutUploader(Context context) {
        mContext = context;
        enableRESTUpload = PreferencesUtil.isNsRestEnabled(context);
    }

    public List<PredictionData> upload(List<PredictionData> glucoseDataSets) {
        List<PredictionData> successfulUploads = null;

        if (enableRESTUpload) {
            long start = System.currentTimeMillis();
            Log.i(TAG, String.format("Starting upload of %s record using a REST API", glucoseDataSets.size()));
            successfulUploads = doRESTUpload(glucoseDataSets);
            Log.i(TAG, String.format("Finished upload of %s record using a REST API in %s ms", successfulUploads.size(), System.currentTimeMillis() - start));
        }

        return successfulUploads;
    }

    private List<PredictionData> doRESTUpload(List<PredictionData> glucoseDataSets) {
        String baseURLSettings = PreferencesUtil.getNsRestUrl(mContext);
        ArrayList<String> baseURIs = new ArrayList<>();
        ArrayList<PredictionData> successfulUploads = new ArrayList<>();

        try {
            for (String baseURLSetting : baseURLSettings.split(" ")) {
                String baseURL = baseURLSetting.trim();
                if (baseURL.isEmpty()) continue;
                baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to process API Base URL");
            return successfulUploads;
        }

        for (String baseURI : baseURIs) {
            try {
                successfulUploads = doRESTUploadTo(baseURI, glucoseDataSets);
            } catch (Exception e) {
                Log.e(TAG, "Unable to do REST API Upload");
                return successfulUploads;
            }
        }
        return successfulUploads;
    }

    private ArrayList<PredictionData> doRESTUploadTo(String baseURI, List<PredictionData> glucoseDataSets) throws Exception {
        ArrayList<PredictionData> successfulUploads = new ArrayList<>();
        int apiVersion = 0;
        if (baseURI.endsWith("/v1/")) apiVersion = 1;

        String baseURL = null;
        String secret = null;
        String[] uriParts = baseURI.split("@");

        if (uriParts.length == 1 && apiVersion == 0) {
            baseURL = uriParts[0];
        } else if (uriParts.length == 1 && apiVersion > 0) {
            throw new Exception("Starting with API v1, a pass phase is required");
        } else if (uriParts.length == 2 && apiVersion > 0) {
            secret = uriParts[0];
            baseURL = uriParts[1];
        } else {
            throw new Exception("Unexpected baseURI");
        }

        String postURL = baseURL + "entries";
        Log.i(TAG, "postURL: " + postURL);

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);

        DefaultHttpClient httpclient = new DefaultHttpClient(params);

        HttpPost post = new HttpPost(postURL);

        Header apiSecretHeader = null;

        if (apiVersion > 0) {
            if (secret == null || secret.isEmpty()) {
                throw new Exception("Starting with API v1, a pass phase is required");
            } else {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] bytes = secret.getBytes("UTF-8");
                digest.update(bytes, 0, bytes.length);
                bytes = digest.digest();
                StringBuilder sb = new StringBuilder(bytes.length * 2);
                for (byte b: bytes) {
                    sb.append(String.format("%02x", b & 0xff));
                }
                String token = sb.toString();
                apiSecretHeader = new BasicHeader("api-secret", token);
            }
        }

        if (apiSecretHeader != null) {
            post.setHeader(apiSecretHeader);
        }

        for (PredictionData record : glucoseDataSets) {
            JSONObject json = new JSONObject();

            try {
                if (apiVersion >= 1)
                    populateV1APIBGEntry(json, record);
                else
                    populateLegacyAPIEntry(json, record);
            } catch (Exception e) {
                Log.w(TAG, "Unable to populate entry");
                continue;
            }

            String jsonString = json.toString();

            Log.i(TAG, "SGV JSON: " + jsonString);

            StringEntity se = null;
            se = new StringEntity(jsonString);
            post.setEntity(se);
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");

            ResponseHandler responseHandler = new BasicResponseHandler();
            try {
                httpclient.execute(post, responseHandler);
                successfulUploads.add(record);
            } catch (IOException e) {
                Log.i(TAG, "ioexception: " + e);
                break;
            }
        }
        return successfulUploads;
    }

    private void populateV1APIBGEntry(JSONObject json, GlucoseData record) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
        format.setTimeZone(TimeZone.getDefault());
        json.put("device", "librealarm");
        json.put("date", record.realDate);
        json.put("dateString", format.format(record.realDate));
        json.put("sgv", record.glucoseLevel);
        json.put("direction", getSlopeName(record));
        json.put("type", "sgv");
        json.put("rssi", 100);
    }

    private void populateLegacyAPIEntry(JSONObject json, GlucoseData record) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");
        format.setTimeZone(TimeZone.getDefault());
        json.put("device", "librealarm");
        json.put("date", record.realDate);
        json.put("dateString", format.format(record.realDate));
        json.put("sgv", record.glucoseLevel);
        json.put("direction", getSlopeName(record));
    }

    private String getSlopeName(GlucoseData data) {
        AlgorithmUtil.TrendArrow arrow = AlgorithmUtil.getTrendArrow(data);
        switch (arrow) {
            case UP:
                return "SingleUp";
            case DOWN:
                return "SingleDown";
            case SLIGHTLY_DOWN:
                return "FortyFiveDown";
            case SLIGHTLY_UP:
                return "FortyFiveUp";
            case FLAT:
                return "Flat";
            default:
                return "NONE";
        }
    }
}