package com.arcxp.thearcxptv.main

import android.app.Application
import androidx.room.Room
import com.arc.arcvideo.ArcXPVideoSDK
import com.arcxp.commerce.ArcXPCommerceConfig
import com.arcxp.content.sdk.ArcXPContentConfig
import com.arcxp.content.sdk.ArcXPContentSDK
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
            //Set the base URL for content.  Set the organization, site and environment.
            //These values can be gotten from the ArcXP admin
            .setBaseUrl(url = getString(R.string.contentUrl))
            .setOrgName(name = getString(R.string.orgName))
            .setSite(site = getString(R.string.siteName))
            .setEnvironment(env = getString(R.string.environment))
            //This is an additional parameter put on the base URL that retrieves the
            //section data for mobile devices.
            .setNavigationEndpoint(endpoint = getString(R.string.navigation_endpoint))

            //Content SDK caches data to decrease the amount of bandwidth needed.
            //This value can be between 10 and 1024 MB
            .setCacheSize(sizeInMB = 0)
            //After a specified number of minutes cached items will be updated to
            //ensure the latest version is available.
            .setCacheTimeUntilUpdate(minutes = 5)
            //if true will pre-fetch and store in db any stories returned by a collection call
            .setPreloading(preLoading = true)
            .build()
        ArcXPContentSDK.initialize(this, contentConfig)


        ArcXPVideoSDK.initialize(
            application = this,
            baseUrl = getString(R.string.contentUrl),
            org = getString(R.string.orgName),
            env = getString(R.string.environment)
        )
        //If the client code caches UUID, refresh token and access token they can
        //be passed into the SDK using this variable
        val commerceAuthData = mutableMapOf<String, String>()
        //Initialize the Commerce SDK.
        val arcCommerceConfig = ArcXPCommerceConfig.Builder()
            .setContext(this)
            //IDs for Facebook and Google.  Needed for third party login capabilities.
            .setFacebookAppId(getString(R.string.facebook_app_id))
            .setGoogleClientId(getString(R.string.google_key))
            //Base URLs provided by ArcXP admin
            .setBaseUrl(getString(R.string.commerceUrl))
            .setBaseSalesUrl(getString(R.string.commerceUrl))
            .setBaseRetailUrl(getString(R.string.commerceUrl))
            .setUrlComponents(
                getString(R.string.orgName),
                getString(R.string.siteName),
                getString(R.string.environment)
            )
            //Will the users email be used as their username.
            .setUserNameIsEmail(false)
            .enableAutoCache(true)
            .usePaywallCache(true)
            .build()


        /**
         * Comment out this line if you are not using the Commerce SDK
         */
        //ArcXPCommerceSDK.initialize(this, commerceAuthData, arcCommerceConfig)

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