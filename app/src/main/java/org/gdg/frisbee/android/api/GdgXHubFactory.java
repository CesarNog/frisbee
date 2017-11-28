package org.gdg.frisbee.android.api;

import com.google.gson.FieldNamingPolicy;

import org.gdg.frisbee.android.api.deserializer.ZuluDateTimeDeserializer;
import org.gdg.frisbee.android.utils.Utils;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class GdgXHubFactory {

    private static final String BASE_URL = "https://hub.gdgx.io/api/v1/";

    private GdgXHubFactory() {
    }

    private static Retrofit provideRestAdapter(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create(Utils.getGson(FieldNamingPolicy.IDENTITY, new ZuluDateTimeDeserializer()))
            )
            .build();
    }

    public static GdgXHub provideHubApi(OkHttpClient okHttpClient) {
        return provideRestAdapter(okHttpClient).create(GdgXHub.class);
    }
}
