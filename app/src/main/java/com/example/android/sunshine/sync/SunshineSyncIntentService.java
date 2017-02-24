/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.sync;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

// thanks to http://stackoverflow.com/questions/25413162/sending-data-to-android-wear-device
/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SunshineSyncIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "SendDataService";
    GoogleApiClient mGoogleApiClient;
    public SunshineSyncIntentService() {
        super("SunshineSyncIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SunshineSyncTask.syncWeather(this);
        PutDataMapRequest dataMap = PutDataMapRequest.create("/events");
        dataMap.getDataMap().putFloat("LowTemp",SunshineSyncTask.low);
        dataMap.getDataMap().putFloat("HighTemp",SunshineSyncTask.high);
        Bitmap bitmap = BitmapFactory.decodeResource(
                getResources(), SunshineWeatherUtils.getSmallArtResourceIdForWeatherCondition(SunshineSyncTask.w_id)
        );
        Asset asset = createAssetFromBitmap(bitmap);
        dataMap.getDataMap().putAsset("profileImage", asset);
        Log.e("Data",(SunshineSyncTask.high+""));
      //  dataMap.getDataMap().putStringArray("events", eventStrings);
        PutDataRequest request = dataMap.asPutDataRequest().setUrgent();
        Wearable.DataApi
                .putDataItem(mGoogleApiClient, request).setResultCallback(new ResultCallbacks<DataApi.DataItemResult>() {
                    @Override
                    public void onSuccess(@NonNull DataApi.DataItemResult dataItemResult) {
                        Log.e("Sucess", "Sent to wear");
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        Log.e("Failed", status.toString());
                    }
                });
        //mGoogleApiClient.disconnect();
    }
    private Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mGoogleApiClient=new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(Wearable.API).build();
        mGoogleApiClient.connect();
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: " + bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }
}
