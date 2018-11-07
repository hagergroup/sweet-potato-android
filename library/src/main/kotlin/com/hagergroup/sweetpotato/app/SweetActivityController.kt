package com.hagergroup.sweetpotato.app

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.hagergroup.sweetpotato.exception.ExceptionHandler
import com.hagergroup.sweetpotato.lifecycle.ViewModelUnavailableException
import timber.log.Timber
import kotlin.reflect.full.findAnnotation

/**
 * @author Ludovic Roland
 * @since 2018.11.05
 */
object SweetActivityController
{

  interface SweetRedirector
  {

    fun getRedirection(activity: FragmentActivity): Intent

  }

  interface SweetInterceptor
  {

    fun onLifeCycleEvent(activity: FragmentActivity?, component: Any?, event: Lifecycle.Event)

  }

  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.CLASS)
  annotation class EscapeToRedirectorAnnotation

  const val CALLING_INTENT_EXTRA = "callingIntentExtra"

  private var redirector: SweetActivityController.SweetRedirector? = null

  private var interceptor: SweetActivityController.SweetInterceptor? = null

  var exceptionHandler: ExceptionHandler? = null
    private set

  fun extractCallingIntent(activity: FragmentActivity): Intent? =
      activity.intent.getParcelableExtra(SweetActivityController.CALLING_INTENT_EXTRA)

  fun registerRedirector(redirector: SweetActivityController.SweetRedirector)
  {
    this.redirector = redirector
  }

  fun registerInterceptor(interceptor: SweetActivityController.SweetInterceptor)
  {
    this.interceptor = interceptor
  }

  @Synchronized
  fun registerExceptionHandler(exceptionHandler: ExceptionHandler)
  {
    this.exceptionHandler = exceptionHandler
  }

  @Synchronized
  fun onLifeCycleEvent(activity: FragmentActivity?, component: Any?, event: Lifecycle.Event)
  {
    interceptor?.onLifeCycleEvent(activity, component, event)
  }

  @Synchronized
  fun handleException(isRecoverable: Boolean, context: Context?, component: Any?, throwable: Throwable?): Boolean
  {
    if (exceptionHandler == null)
    {
      Timber.w(throwable, "Detected an exception which will not be handled during the processing of the context with name '${if (context != null) context::class.qualifiedName else null}'")
      return false
    }

    val activity = if (context is FragmentActivity)
    {
      context
    }
    else
    {
      null
    }

    try
    {
      if (activity != null && throwable is ViewModelUnavailableException)
      {
        Timber.w(throwable, "Caught an exception during the retrieval of the business objects from the activity from class with name '${activity::class.qualifiedName}'")

        // We do nothing if the activity is dying
        return if (activity.isFinishing == true)
        {
          true
        }
        else
        {
          exceptionHandler?.onViewModelUnavailableException(activity, component, throwable) ?: false
        }
      }
      else
      {
        Timber.w(throwable, "Caught an exception during the processing of the Context from class with name '${if (context != null) context::class.qualifiedName else null}'")

        // For this special case, we ignore the case when the activity is dying
        return if (activity != null)
        {
          exceptionHandler?.onActivityException(activity, component, throwable) ?: false
        }
        else if (context != null)
        {
          exceptionHandler?.onContextException(isRecoverable, context, throwable) ?: false
        }
        else
        {
          exceptionHandler?.onException(isRecoverable, throwable) ?: false
        }
      }
    }
    catch (otherThrowable: Throwable)
    {
      // Just to make sure that handled exceptions do not trigger un-handled exceptions on their turn ;(
      Timber.e(otherThrowable, "An error occurred while attempting to handle an exception coming from the Context from class with name '${context?.javaClass?.name}'")

      return false
    }

  }

  fun needsRedirection(activity: FragmentActivity): Boolean
  {
    if (redirector == null)
    {
      return false
    }

    if (activity::class.findAnnotation<SweetActivityController.EscapeToRedirectorAnnotation>() != null)
    {
      Timber.d("The Activity with class '${activity::class.qualifiedName}' is escaped regarding the Redirector")

      return false
    }

    val intent = redirector?.getRedirection(activity) ?: return false

    Timber.d("A redirection is needed")

    // We redirect to the right Activity
    // We consider the parent activity in case it is embedded (like in an ActivityGroup)
    val formerIntent = if (activity.parent != null) activity.parent.intent else activity.intent
    intent.putExtra(SweetActivityController.CALLING_INTENT_EXTRA, formerIntent)

    // Disables the fact that the new started activity should belong to the tasks history and from the recent tasks
    activity.startActivity(intent)

    // We now finish the redirected Activity
    activity.finish()

    return true
  }

}