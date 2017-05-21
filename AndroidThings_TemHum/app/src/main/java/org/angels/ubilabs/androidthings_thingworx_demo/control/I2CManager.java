package org.angels.ubilabs.androidthings_thingworx_demo.control;


import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.angels.ubilabs.androidthings_thingworx_demo.model.WeatherData;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class I2CManager {
    private static final String TAG = I2CManager.class.getSimpleName();

    private Handler handler;
    private HandlerThread handlerThread;


    private WeatherData weatherData;
    private I2cDevice device;
    private int startAddress;


    private static final long PUBLISH_INTERVAL_MS = TimeUnit.SECONDS.toMillis(3);

    public I2CManager(WeatherData weatherData, I2cDevice device, int startAddress) throws IOException {
        this.weatherData = weatherData;
        this.device = device;
        this.startAddress = startAddress;

        handlerThread = new HandlerThread("I2CThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public void start() {
        handler.post(publishRunnable);
    }


    public void close() {
        handler.removeCallbacks(publishRunnable);
        handlerThread.quitSafely();
    }

    private Runnable publishRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                readCalibration(device, startAddress);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handler.postDelayed(publishRunnable, PUBLISH_INTERVAL_MS);
            }
        }
    };

    // Read a register block
    private byte[] readCalibration(I2cDevice device, int startAddress) throws IOException {
        // Read three consecutive register values
        byte[] data = new byte[7];
        device.readRegBuffer(startAddress, data, data.length);
        String dataString = new String(data);
        int ts = dataString.indexOf('T');
        int te = dataString.indexOf('H');
        int he = dataString.indexOf('E');
        int tem=Integer.valueOf(dataString.substring(ts + 1, te));
        int hum=Integer.valueOf(dataString.substring(te + 1, he));
        if (ts >= 0 && te >= 0 && he >= 0) {
            weatherData.addTemperature(tem);
            weatherData.addHumidity(hum);
        }
        Log.d(TAG, "Current T: " + weatherData.getCurrentTemperature() + ", H: " + weatherData.getCurrentHumidity());
        AsyncHttpClient client=new AsyncHttpClient();
        RequestParams params=new RequestParams();
        params.add("tem",String.valueOf(tem));
        params.add("hum",String.valueOf(hum));
        params.add("deviceId","1");
        client.post("http://192.168.1.103/pushhumtem",params,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.w(TAG, response.toString());
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.w(TAG, "onFailure(int, Header[], Throwable, JSONObject) was not overriden, but callback was received", throwable);
            }
        });


        return data;
    }
}
