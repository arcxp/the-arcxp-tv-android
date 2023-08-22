package com.arcxp.thearcxptv.main

import android.app.Application
import androidx.room.Room
import com.arcxp.ArcXPMobileSDK
import com.arcxp.content.ArcXPContentConfig
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.db.VideoDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module


class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        //Initialize the ArcXP Content SDK
        val contentConfig = ArcXPContentConfig.Builder()
            //This is an additional parameter put on the base URL that retrieves the
            //section data for mobile devices.
            .setNavigationEndpoint(endpoint = getString(R.string.navigation_endpoint))
            //Content SDK caches data to decrease the amount of bandwidth needed.
            //This value can be between 10 and 1024 MB
            .setCacheSize(sizeInMB = 100)
            //After a specified number of minutes cached items will be updated to
            //ensure the latest version is available.
            .setCacheTimeUntilUpdate(minutes = 5)
            //if true will pre-fetch and store in db any stories returned by a collection call
            .setPreloading(preLoading = true)
            .build()

        //Set the base URL for content.  Set the organization, site and environment.
        //These values can be gotten from the ArcXP admin
        ArcXPMobileSDK.initialize(
            application = this,
            site = getString(R.string.siteName),
            org = getString(R.string.orgName),
            environment = getString(R.string.environment),
            contentConfig = contentConfig,
            baseUrl = getString(R.string.contentUrl)
        )

        startKoin {
            androidContext(this@MainApplication)
            modules(listOf(appModule))
        }
    }

    private val appModule: Module = module {
        single {
            Room.databaseBuilder(
                get(),
                VideoDatabase::class.java, "videosToRemember"
            ).build()
        }
        single {
            val database = get<VideoDatabase>()
            database.rememberVideoDao()
        }
        viewModel {
            MainViewModel(application = get(), videoDao = get())
        }
    }

}