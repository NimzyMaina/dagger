# Dependency Injection (DI) with Dagger 2

This is an example of how to use dagger to provide dependencies in Android. **Retrofit 2** is used to get list of my Repos from my **Github** account using their API. The repos are then displayed in a ListView.

## Installation

First you need to install the Dagger2 and Retrofit.

```text
dependencies {
    // Dagger
    implementation 'com.google.dagger:dagger-android:2.11'
    implementation 'com.google.dagger:dagger-android-support:2.11' // if you use the support libraries
    annotationProcessor 'com.google.dagger:dagger-android-processor:2.11'
    annotationProcessor 'com.google.dagger:dagger-compiler:2.11'
    // Retrofit
    implementation 'com.google.code.gson:gson:2.8.2'
    implementation 'com.squareup.retrofit2:retrofit:2.3.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.3.0'
    implementation "com.squareup.okhttp3:logging-interceptor:3.9.1"
}
```

Once that is done, we need to grant internet permissions to the app.
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Then we need to create **AppModule.java** provide the Application context.

```java
import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    Application mApplication;

    public AppModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    Application providesApplication() {
        return mApplication;
    }

}
```


Once that is done we are going to create **NetModule.java**. This will be used to provide the retrofit dependencies.

```java
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.openshamba.dagger.app.Config;
import com.openshamba.dagger.infrastructure.AuthenticationInterceptor;
import com.openshamba.dagger.infrastructure.Tls12SocketFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;

import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class NetModule {

    private String mBaseUrl;

    // Constructor needs one parameter to instantiate.
    public NetModule(String baseUrl) {
        this.mBaseUrl = baseUrl;
    }

    // Dagger will only look for methods annotated with @Provides
    @Provides
    @Singleton
    // Application reference must come from AppModule.class
    SharedPreferences providesSharedPreferences(Application application) {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    @Provides
    @Singleton
    Cache provideOkHttpCache(Application application) {
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        return new Cache(application.getCacheDir(), cacheSize);
    }

    @Provides
    @Singleton
    Gson provideGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        return gsonBuilder.create();
    }

    @Provides
    @Singleton
    @Named("cached")
    OkHttpClient provideCachedOkHttpClient(Cache cache,HttpLoggingInterceptor loggingInterceptor) {
        return new OkHttpClient.Builder()
                .cache(cache)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(40, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();
    }

    @Provides
    @Singleton
    @Named("non_cached")
    OkHttpClient provideNonCachedOkHttpClient(HttpLoggingInterceptor loggingInterceptor) {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(40, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor);

        client = enableTls12OnPreLollipop(client);

        return client.build();
    }

    @Provides
    @Singleton
    HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        return new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    @Provides
    @Singleton
    AuthenticationInterceptor providesAuthenticationInterceptor(SharedPreferences pref) {
        String token = pref.getString(Config.API_KEY,"");
        return new AuthenticationInterceptor(token);
    }

    @Provides
    @Singleton
    @Named("cached")
    Retrofit provideCachedRetrofit(Gson gson, @Named("cached")OkHttpClient okHttpClient,SharedPreferences pref,AuthenticationInterceptor auth) {

        if(pref.contains(Config.API_KEY) && !okHttpClient.interceptors().contains(auth)){
            okHttpClient.interceptors().add(auth);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(mBaseUrl)
                .client(okHttpClient)
                .build();
        return retrofit;
    }

    @Provides
    @Singleton
    @Named("non_cached")
    Retrofit provideNonCachedRetrofit(Gson gson, @Named("non_cached")OkHttpClient okHttpClient,SharedPreferences pref,AuthenticationInterceptor auth) {

        if(pref.contains(Config.API_KEY) && !okHttpClient.interceptors().contains(auth)){
            okHttpClient.interceptors().add(auth);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(mBaseUrl)
                .client(okHttpClient)
                .build();
        return retrofit;
    }

    private static OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);
                client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()));

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                client.connectionSpecs(specs);
            } catch (Exception exc) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
            }
        }

        return client;
    }

}
```    

Then we will create a **NetComponent.java**. This enables us to inject the dependencies where we need to use them.

```java
import com.openshamba.dagger.MainActivity;
import com.openshamba.dagger.di.modules.AppModule;
import com.openshamba.dagger.di.modules.NetModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { AppModule.class, NetModule.class})
public interface NetComponent {

    void inject(MainActivity activity);
    // void inject(MyFragment fragment);
   // void inject(MyService service);

}

```

We should do all this work within a specialization of the Application class since these instances should be declared only once throughout the entire lifespan of the application. After we have done this make sure to **rebuild** the application so that Dagger can generate the component builders. 

```java
import android.app.Application;

public class App extends Application {

    private NetComponent mNetComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        // Dagger%COMPONENT_NAME%
        mNetComponent = DaggerNetComponent.builder()
                // list of modules that are part of this component need to be created here too
                .appModule(new AppModule(this)) // This also corresponds to the name of your module: %component_name%Module
                .netModule(new NetModule("https://api.github.com/"))
                .build();

        // If a Dagger 2 component does not have any constructor arguments for any of its modules,
        // then we can use .create() as a shortcut instead:
        //  mNetComponent = com.codepath.dagger.components.DaggerNetComponent.create();
    }

    public NetComponent getNetComponent() {
        return mNetComponent;
    }
}

```

Specify **App.java** as the application name in the manifest so that it to be instanciated.

```text
<application
      android:allowBackup="true"
      android:name=".MyApp">
```




Have a look at the [**MainActivity.java**](https://github.com/NimzyMaina/dagger/blob/master/app/src/main/java/com/openshamba/dagger/MainActivity.java) in order to view how these dependencies are used inside an Activity.

### SSL Bug on pre Lollipop devices

I found an issue with SSL validation. This is the error I got:

**SSLHandshakeException / SSLProtocolException: handshake aborted: Failure in SSL library, usually a protocol error (Android 4)** 

Did a quick search and found a fix here [**support enabling TLSv1.2 on Android 4.1-4.4.**](https://github.com/square/okhttp/issues/2372)

### References
1 [CodePath](https://guides.codepath.com/android/dependency-injection-with-dagger-2)

2 [Future Studio](https://futurestud.io/tutorials/retrofit-getting-started-and-android-client) 