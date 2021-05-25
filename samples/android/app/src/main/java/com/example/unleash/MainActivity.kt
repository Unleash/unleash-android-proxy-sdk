package com.example.unleash

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.example.unleash.databinding.ActivityMainBinding
import io.getunleash.UnleashClient
import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.cache.InMemoryToggleCache
import io.getunleash.polling.PollingModes
import io.getunleash.polling.ToggleUpdatedListener
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var unleashClient: UnleashClient
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val unleashContext = UnleashContext.newBuilder()
            .appName(applicationContext.getAppName())
            .userId("unleash_demo_user")
            .sessionId(Random.nextLong().toString())
            .build()
        this.unleashClient = UnleashClient.newBuilder()
            .unleashConfig(
                UnleashConfig.newBuilder()
                    .clientSecret("proxy-123")
                    .proxyUrl("https://app.unleash-hosted.com/demo/proxy")
                    .pollingMode(
                        PollingModes.autoPoll(
                            autoPollIntervalSeconds = 5
                        ) {
                            this@MainActivity.runOnUiThread {
                                val firstFragmentText = findViewById<TextView>(R.id.textview_first)
                                firstFragmentText.text =
                                    "Variant ${unleashClient.getVariant("unleash_android_sdk_demo").name}"
                                findViewById<TextView>(R.id.textview_second)?.let {
                                    it.text =
                                        "Unleash is ${unleashClient.isEnabled("unleash_android_sdk_demo")}"
                                }
                            }

                        }
            )
            .environment("dev").build()
        )
        .cache(InMemoryToggleCache())
            .unleashContext(unleashContext)
            .build()
        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun Context.getAppName(): String = applicationInfo.loadLabel(packageManager).toString()
}

