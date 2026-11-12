package com.azuredevid.cassy

import android.app.Application
import com.azuredevid.cassy.di.appModule
import com.azuredevid.cassy.di.repositoryModule
import com.azuredevid.cassy.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CassyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@CassyApplication)
            modules(
                appModule,
                repositoryModule,
                viewModelModule
            )
        }
    }
}
