package com.hagergroup.sweetpotato.appcompat.app

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.hagergroup.sweetpotato.annotation.SweetActionBarAnnotation
import com.hagergroup.sweetpotato.annotation.SweetActivityAnnotation
import com.hagergroup.sweetpotato.fragment.app.DummySweetFragment
import com.hagergroup.sweetpotato.fragment.app.SweetFragment
import timber.log.Timber
import kotlin.reflect.KClass

/**
 * @author Ludovic Roland
 * @since 2018.11.07
 */
abstract class SweetActivityAggregate(val activity: AppCompatActivity, val activityAnnotation: SweetActivityAnnotation?, val actionBarAnnotation: SweetActionBarAnnotation?)
  : FragmentManager.OnBackStackChangedListener
{

  enum class FragmentTransactionType
  {

    Add, Replace
  }

  init
  {
    activity.supportFragmentManager.addOnBackStackChangedListener(this)
  }

  var openedFragment: SweetFragment<*>? = null
    private set

  private var lastBackstackFragment: SweetFragment<*>? = null

  private var lastBackstackCount: Int = 0

  override fun onBackStackChanged()
  {
    val fragmentManager = activity.supportFragmentManager
    val newCount = fragmentManager.backStackEntryCount

    // Fragment just restored from backstack
    if (newCount < lastBackstackCount)
    {
      openedFragment = lastBackstackFragment
    }

    // Save the new backstack count
    lastBackstackCount = newCount

    // Save the new (last) backstack openedFragment
    if (newCount > 1)
    {
      val tag = fragmentManager.getBackStackEntryAt(newCount - 2).name

      if (tag != null)
      {
        lastBackstackFragment = fragmentManager.findFragmentByTag(tag) as SweetFragment<*>
      }
    }
  }

  fun replaceFragment(fragmentClass: KClass<out SweetFragment<*>>)
  {
    addOrReplaceFragment(fragmentClass, activityAnnotation?.fragmentPlaceholderId ?: -1, activityAnnotation?.addFragmentToBackStack ?: false, activityAnnotation?.fragmentBackStackName, null, activity.intent.extras, FragmentTransactionType.Replace)
  }

  fun replaceFragment(fragmentClass: KClass<out SweetFragment<*>>, @IdRes fragmentContainerIdentifer: Int, addFragmentToBackStack: Boolean, fragmentBackStackName: String?)
  {
    addOrReplaceFragment(fragmentClass, fragmentContainerIdentifer, addFragmentToBackStack, fragmentBackStackName, null, activity.intent.extras, FragmentTransactionType.Replace)
  }

  fun replaceFragment(fragmentClass: KClass<out SweetFragment<*>>, savedState: Fragment.SavedState?, arguments: Bundle?)
  {
    addOrReplaceFragment(fragmentClass, activityAnnotation?.fragmentPlaceholderId ?: -1, activityAnnotation?.addFragmentToBackStack ?: false, activityAnnotation?.fragmentBackStackName, savedState, arguments, FragmentTransactionType.Replace)
  }

  fun addOrReplaceFragment(fragmentClass: KClass<out SweetFragment<*>>, @IdRes fragmentContainerIdentifer: Int, addFragmentToBackStack: Boolean, fragmentBackStackName: String?, savedState: Fragment.SavedState?, arguments: Bundle?, fragmentTransactionType: FragmentTransactionType)
  {
    try
    {
      openedFragment = fragmentClass.java.newInstance()
      openedFragment?.arguments = arguments

      // We (re)set its initial state if necessary
      if (savedState != null)
      {
        openedFragment?.setInitialSavedState(savedState)
      }

      val fragmentTransaction = activity.supportFragmentManager.beginTransaction()

      if (fragmentTransactionType == FragmentTransactionType.Replace)
      {
        openedFragment?.let {
          fragmentTransaction.replace(fragmentContainerIdentifer, it, if (addFragmentToBackStack == true) fragmentBackStackName else null)
        }
      }
      else
      {
        openedFragment?.let {
          fragmentTransaction.add(fragmentContainerIdentifer, it, if (addFragmentToBackStack == true) fragmentBackStackName else null)
        }
      }

      if (addFragmentToBackStack == true)
      {
        fragmentTransaction.addToBackStack(fragmentBackStackName)
      }

      fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
      fragmentTransaction.commitAllowingStateLoss()
    }
    catch (exception: Exception)
    {
      Timber.e(exception, "Unable to instanciate the openedFragment '${fragmentClass.simpleName}'")
    }

  }

  fun onCreate()
  {
    if (activity is SweetAppCompatActivity<*> && activity.getContentViewId() != -1)
    {
      activity.setContentView(activity.getContentViewId())
    }
    else
    {
      activityAnnotation?.let {
        activity.setContentView(activityAnnotation.contentViewId)
      }
    }

    openedFragment = if (activity is SweetAppCompatActivity<*> && activity.getFragmentPlaceholderId() != -1)
    {
      activity.supportFragmentManager.findFragmentById(activity.getFragmentPlaceholderId()) as? SweetFragment<*>
    }
    else
    {
      activityAnnotation?.let {
        activity.supportFragmentManager.findFragmentById(activityAnnotation.fragmentPlaceholderId) as? SweetFragment<*>
      }
    }

    if (openedFragment == null)
    {
      openParameterFragment()
    }

    actionBarAnnotation?.apply {
      if (this.actionBarBehavior == SweetActionBarAnnotation.ActionBarBehavior.Drawer)
      {
        activity.supportActionBar?.apply {
          setDisplayHomeAsUpEnabled(true)
          setDisplayShowHomeEnabled(false)
        }
      }
      else if (this.actionBarBehavior == SweetActionBarAnnotation.ActionBarBehavior.Up)
      {
        activity.supportActionBar?.apply {
          setDisplayHomeAsUpEnabled(true)
          setDisplayShowHomeEnabled(true)
        }
      }
      else
      {
        activity.supportActionBar?.apply {
          setDisplayHomeAsUpEnabled(false)
          setDisplayShowHomeEnabled(false)
        }
      }
    }
  }

  private fun openParameterFragment()
  {
    activityAnnotation?.let {
      if (activityAnnotation.fragmentClass != DummySweetFragment::class.java && activityAnnotation.fragmentPlaceholderId != -1)
      {
        replaceFragment(activityAnnotation.fragmentClass)
      }
    }
  }

}