package com.example.androiddevchallenge.ui

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.androiddevchallenge.data.Dog
import com.example.androiddevchallenge.ui.Screen.Detail
import com.example.androiddevchallenge.ui.Screen.Home
import com.example.androiddevchallenge.ui.ScreenName.DETAIL
import com.example.androiddevchallenge.ui.ScreenName.HOME
import com.example.androiddevchallenge.utils.getMutableStateOf

/**
 * Screen names (used for serialization)
 */
enum class ScreenName { HOME, DETAIL }

/**
 * Class defining the screens we have in the app: home and details
 */
sealed class Screen(val id: ScreenName) {
  object Home : Screen(HOME)
  data class Detail(val dog: Dog) : Screen(DETAIL)
}

/**
 * Helpers for saving and loading a [Screen] object to a [Bundle].
 *
 * This allows us to persist navigation across process death, for example caused by a long video
 * call.
 */
private const val SIS_SCREEN = "sis_screen"
private const val SIS_NAME = "screen_name"
private const val SIS_POST = "post"

/**
 * Convert a screen to a bundle that can be stored in [SavedStateHandle]
 */
private fun Screen.toBundle(): Bundle {
  return bundleOf(SIS_NAME to id.name).also {
    // add extra keys for various types here
    if (this is Detail) {
      it.putParcelable(SIS_POST, dog)
    }
  }
}

/**
 * Read a bundle stored by [Screen.toBundle] and return desired screen.
 *
 * @return the parsed [Screen]
 * @throws IllegalArgumentException if the bundle could not be parsed
 */
private fun Bundle.toScreen(): Screen {
  val screenName = ScreenName.valueOf(getStringOrThrow(SIS_NAME))
  return when (screenName) {
    HOME -> Home
    DETAIL -> {
      val dog = getParcelableOrThrow<Dog>(SIS_POST)
      Detail(dog)
    }
  }
}

/**
 * Throw [IllegalArgumentException] if key is not in bundle.
 *
 * @see Bundle.getString
 */
private fun Bundle.getStringOrThrow(key: String) =
  requireNotNull(getString(key)) { "Missing key '$key' in $this" }

/**
 * Throw [IllegalArgumentException] if key is not in bundle.
 *
 * @see Bundle.getParcelable
 */
private fun <T : Parcelable> Bundle.getParcelableOrThrow(key: String) =
  requireNotNull(getParcelable<T>(key)) { "Missing key '$key' in $this" }

/**
 * This is expected to be replaced by the navigation component, but for now handle navigation
 * manually.
 *
 * Instantiate this ViewModel at the scope that is fully-responsible for navigation, which in this
 * application is [MainActivity].
 *
 * This app has simplified navigation; the back stack is always [Home] or [Home, dest] and more
 * levels are not allowed. To use a similar pattern with a longer back stack, use a [StateList] to
 * hold the back stack state.
 */
class NavigationViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
  /**
   * Hold the current screen in an observable, restored from savedStateHandle after process
   * death.
   *
   * mutableStateOf is an observable similar to LiveData that's designed to be read by compose. It
   * supports observability via property delegate syntax as shown here.
   */
  var currentScreen: Screen by savedStateHandle.getMutableStateOf<Screen>(
    key = SIS_SCREEN,
    default = Home,
    save = { it.toBundle() },
    restore = { it.toScreen() }
  )
    private set // limit the writes to only inside this class.

  /**
   * Go back (always to [Home]).
   *
   * Returns true if this call caused user-visible navigation. Will always return false
   * when [currentScreen] is [Home].
   */
  @MainThread
  fun onBack(): Boolean {
    val wasHandled = currentScreen != Home
    currentScreen = Home
    return wasHandled
  }

  /**
   * Navigate to requested [Screen].
   *
   * If the requested screen is not [Home], it will always create a back stack with one element:
   * ([Home] -> [screen]). More back entries are not supported in this app.
   */
  @MainThread
  fun navigateTo(screen: Screen) {
    currentScreen = screen
  }
}
