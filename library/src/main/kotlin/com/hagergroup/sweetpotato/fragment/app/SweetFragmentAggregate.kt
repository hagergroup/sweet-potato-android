package com.hagergroup.sweetpotato.fragment.app

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.hagergroup.sweetpotato.annotation.SweetFragmentAnnotation
import com.hagergroup.sweetpotato.app.SweetApplication
import timber.log.Timber
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * @author Ludovic Roland
 * @since 2018.11.07
 */
abstract class SweetFragmentAggregate<ApplicationClass : SweetApplication>(val fragment: Fragment, val fragmentAnnotation: SweetFragmentAnnotation)
{

  interface OnBackPressedListener
  {

    fun onBackPressed(): Boolean

  }

  fun getApplication(): ApplicationClass? =
      fragment.activity?.application as? ApplicationClass

  fun openChildFragment(parentFragment: SweetFragment<*>, @IdRes fragmentPlaceholderIdentifier: Int, fragmentClass: KClass<SweetFragment<*>>, savedState: Fragment.SavedState?)
  {
    try
    {
      val fragmentTransaction = parentFragment.childFragmentManager.beginTransaction()
      val childfragment = fragmentClass.createInstance()
      childfragment.arguments = parentFragment.arguments

      // We (re)set its initial state if necessary
      if (savedState != null)
      {
        childfragment.setInitialSavedState(savedState)
      }

      fragmentTransaction.replace(fragmentPlaceholderIdentifier, childfragment)
      fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
      fragmentTransaction.commit()
    }
    catch (exception: Exception)
    {
      Timber.e(exception, "Unable to instanciate the fragment '${fragmentClass.simpleName}'")
    }
  }

  fun onCreate(activity: FragmentActivity?)
  {
    (activity as? AppCompatActivity)?.supportActionBar?.let {
      val titleIdentifier = fragmentAnnotation.fragmentTitleId
      val subTitleIdentifier = fragmentAnnotation.fragmentSubTitleId

      if (titleIdentifier > 0)
      {
        it.setTitle(titleIdentifier)
      }

      if (subTitleIdentifier > 0)
      {
        it.setSubtitle(subTitleIdentifier)
      }
    }
  }

}