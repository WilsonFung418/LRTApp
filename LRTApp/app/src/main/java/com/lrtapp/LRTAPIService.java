package com.lrtapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LRTAPIService {
    private static final String API_URL = "https://rt.data.gov.hk/v1/transport/mtr/lrt/getSchedule";
    private final OkHttpClient wifiClient;
    private final OkHttpClient fallbackClient;
    private final Gson gson;
    private final Context context;

    public LRTAPIService(Context context) {
        this.context = context;
        // First try: WiFi with 3s timeout
        wifiClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
        // Fallback: mobile data with 10s timeout
        fallbackClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    public List<Arrival> getArrivals(String stationId) throws IOException {
        String url = API_URL + "?station_id=" + stationId;

        try {
            // Try WiFi first (3s timeout)
            return executeWithClient(url, wifiClient);
        } catch (IOException e) {
            // WiFi failed — try fallback network (mobile data)
            return tryWithFallbackNetwork(url);
        }
    }

    private List<Arrival> tryWithFallbackNetwork(String url) throws IOException {
        Toast.makeText(context, "📶 WiFi無回應，切換至流動數據", Toast.LENGTH_SHORT).show();
        // On Android 5.0+, try binding to mobile data network
        if (Build.VERSION.SDK_INT >= 21) {
            ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkRequest.Builder reqBuilder = new NetworkRequest.Builder();
                reqBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                reqBuilder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

                final Network[] mobileNetwork = new Network[1];
                final Object lock = new Object();

                ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        synchronized (lock) {
                            mobileNetwork[0] = network;
                            lock.notifyAll();
                        }
                    }
                };

                cm.requestNetwork(reqBuilder.build(), callback);

                synchronized (lock) {
                    try {
                        lock.wait(3000); // Wait up to 3s for mobile network
                    } catch (InterruptedException ignored) {}
                }

                try {
                    cm.unregisterNetworkCallback(callback);
                } catch (Exception ignored) {}

                if (mobileNetwork[0] != null) {
                    try {
                        Network net = mobileNetwork[0];
                        if (Build.VERSION.SDK_INT >= 23) {
                            cm.bindProcessToNetwork(net);
                        } else {
                            ConnectivityManager.setProcessDefaultNetwork(net);
                        }
                        return executeWithClient(url, fallbackClient);
                    } finally {
                        if (Build.VERSION.SDK_INT >= 23) {
                            cm.bindProcessToNetwork(null);
                        } else {
                            ConnectivityManager.setProcessDefaultNetwork(null);
                        }
                    }
                }
            }
        }
        // Last resort: just try with fallback timeout
        return executeWithClient(url, fallbackClient);
    }

    private List<Arrival> executeWithClient(String url, OkHttpClient client) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "LRTApp/1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            String body = response.body().string();
            LRTAPIResponse apiResponse = gson.fromJson(body, LRTAPIResponse.class);
            return flattenArrivals(apiResponse);
        }
    }

    private List<Arrival> flattenArrivals(LRTAPIResponse response) {
        List<Arrival> arrivals = new ArrayList<>();
        if (response == null || response.platformList == null) {
            return arrivals;
        }
        for (LRTAPIResponse.Platform platform : response.platformList) {
            // Add platform header
            Arrival header = new Arrival();
            header.isPlatformHeader = true;
            header.platformId = platform.platformId;
            header.routeNo = "月台 " + platform.platformId;
            arrivals.add(header);

            if (platform.routeList != null) {
                for (LRTAPIResponse.RouteData route : platform.routeList) {
                    Arrival arrival = new Arrival();
                    arrival.routeNo = route.routeNo;
                    arrival.destinationEn = route.destEn;
                    arrival.destinationTc = route.destCh;
                    arrival.arrivalEn = route.timeEn;
                    arrival.arrivalTc = route.timeCh;
                    arrival.trainLength = route.trainLength;
                    arrival.platformId = platform.platformId;
                    arrivals.add(arrival);
                }
            }
        }
        return arrivals;
    }
}
