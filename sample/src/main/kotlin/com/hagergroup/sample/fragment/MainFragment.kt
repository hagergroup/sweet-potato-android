package com.hagergroup.sample.fragment

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hagergroup.sample.R
import com.hagergroup.sample.SecondActivity
import com.hagergroup.sweetpotato.annotation.SweetFragmentAnnotation
import com.hagergroup.sweetpotato.content.SweetBroadcastListener
import com.hagergroup.sweetpotato.content.SweetBroadcastListenerProvider
import com.hagergroup.sweetpotato.coroutines.SweetCoroutines
import com.hagergroup.sweetpotato.lifecycle.ModelUnavailableException
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.delay
import timber.log.Timber
import java.net.URL
import java.net.UnknownHostException

/**
 * @author Ludovic Roland
 * @since 2018.11.08
 */
@SweetFragmentAnnotation(layoutId = R.layout.fragment_main, fragmentTitleId = R.string.app_name, surviveOnConfigurationChanged = true)
class MainFragment
  : SampleFragment(),
    View.OnClickListener, SweetBroadcastListenerProvider
{

  companion object
  {

    const val MY_ACTION = "myAction"

  }

  private var throwError = false

  private var throwInternetError = false

  override fun getBroadcastListener(): SweetBroadcastListener
  {
    return object : SweetBroadcastListener
    {
      override fun getIntentFilter(): IntentFilter
      {
        return IntentFilter(MainFragment.MY_ACTION)
      }

      override fun onReceive(context: Context?, intent: Intent?)
      {
        if (intent?.action == MainFragment.MY_ACTION)
        {
          Toast.makeText(context, "Click !", Toast.LENGTH_SHORT).show()
        }
      }

    }
  }

  @Throws(ModelUnavailableException::class)
  override suspend fun onRetrieveModel()
  {
    super.onRetrieveModel()

    Thread.sleep(1_000)

    if (throwError == true)
    {
      throwError = false

      throw ModelUnavailableException("Cannot retrieve the model")
    }

    if (throwInternetError == true)
    {
      throwInternetError = false

      throw ModelUnavailableException("Cannot retrieve the model", UnknownHostException())
    }
  }

  override fun onBindModel()
  {
    super.onBindModel()

    binding2.setOnClickListener(this)
    binding.setOnClickListener(this)
    click.setOnClickListener(this)
    refreshLoading.setOnClickListener(this)
    refreshNoLoading.setOnClickListener(this)
    refreshError.setOnClickListener(this)
    refreshInternetError.setOnClickListener(this)
    coroutines.setOnClickListener(this)
    coroutinesError.setOnClickListener(this)
  }

  override fun onClick(view: View?)
  {
    if (view == binding)
    {
      val intent = Intent(context, SecondActivity::class.java).apply {
        putExtra(SecondFragment.MY_EXTRA, "hey !")
        putExtra(SecondFragment.ANOTHER_EXTRA, "Another Hey !")
      }

      startActivity(intent)
    }
    else if (view == binding2)
    {
      val intent = Intent(context, SecondActivity::class.java).apply {
        putExtra(ThirdFragment.MY_EXTRA, "go !")
        putExtra(ThirdFragment.ANOTHER_EXTRA, "Another go !")
      }

      startActivity(intent)
    }
    else if (view == click)
    {
      context?.let {
        LocalBroadcastManager.getInstance(it).sendBroadcast(Intent(MainFragment.MY_ACTION))
      }
    }
    else if (view == refreshLoading)
    {
      refreshModelAndBind(true, Runnable {
        Toast.makeText(context, "Finish !", Toast.LENGTH_SHORT).show()
      }, true)
    }
    else if (view == refreshNoLoading)
    {
      getAggregate()?.getLoadingErrorAndRetryAggregate()?.doNotDisplayLoadingViewNextTime()
      refreshModelAndBind(true, Runnable {
        Toast.makeText(context, "Finish !", Toast.LENGTH_SHORT).show()
      }, true)
    }
    else if (view == refreshError)
    {
      throwError = true
      refreshModelAndBind(true, Runnable {
        Toast.makeText(context, "Finish !", Toast.LENGTH_SHORT).show()
      }, true)
    }
    else if (view == refreshInternetError)
    {
      throwInternetError = true
      refreshModelAndBind(true, Runnable {
        Toast.makeText(context, "Finish !", Toast.LENGTH_SHORT).show()
      }, true)
    }
    else if (view == coroutines)
    {
      startSweetCoroutines()
    }
    else if (view == coroutinesError)
    {
      startSweetCoroutinesError()
    }
  }

  private fun startSweetCoroutines()
  {
    SweetCoroutines.execute(lifecycleScope, object : SweetCoroutines.SweetGuardedCoroutine(context)
    {
      override suspend fun run()
      {
        val oracle = URL("https://www.google.com/")
        oracle.openConnection().inputStream.use {
          Timber.d(it.bufferedReader().readText())
        }
      }
    })
  }

  private fun startSweetCoroutinesError()
  {
    SweetCoroutines.execute(lifecycleScope, object : SweetCoroutines.SweetGuardedCoroutine(context)
    {
      override suspend fun run()
      {
        delay(2_000)

        23.div(0)
      }

      override suspend fun onThrowable(throwable: Throwable): Throwable?
      {
        Timber.w(throwable, "An error occurred")

        return null
      }

    })
  }

}
