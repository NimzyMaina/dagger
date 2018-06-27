package com.openshamba.dagger.di.components;

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
