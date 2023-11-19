/*
 * Copyright (C) 2023 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.core.ui.overlays.menu

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView

import androidx.annotation.IdRes
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.testutils.captureWindowManagerAddedMenuView
import com.buzbuz.smartautoclicker.core.ui.testutils.captureWindowManagerAddedViews
import com.buzbuz.smartautoclicker.core.ui.R

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when` as mockWhen
import org.robolectric.annotation.Config

/** Test the [OverlayMenu] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayMenuTests {

    private companion object {
        private const val TEST_DATA_DISABLED_ALPHA = 0.4f
        private const val TEST_DATA_DISPLAY_WIDTH = 800
        private const val TEST_DATA_DISPLAY_HEIGHT = 600
    }

    /**
     * Tested class implementation redirecting the abstract method calls to the provided mock interface.
     * @param impl the mock called for each abstract method calls.
     */
    class OverlayMenuTestImpl(private val impl: OverlayMenuControllerImpl) : OverlayMenu() {
        override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup = impl.onCreateMenu(layoutInflater)
        override fun onCreateOverlayView(): View? = impl.onCreateOverlayView()
        override fun onMenuItemClicked(viewId: Int) {
            impl.onMenuItemClicked(viewId)
        }
        override fun onStart() {
            super.onStart()
            impl.onShow()
        }
        fun publicSetMenuItemViewEnabled(view: View, enabled: Boolean, clickable: Boolean = false) {
            setMenuItemViewEnabled(view, enabled, clickable)
        }
    }

    /**
     * Interface to be mocked in order to instantiates an [OverlayMenuTestImpl].
     * Calls on abstract members of [OverlayMenu] can be verified on this mock.
     */
    interface OverlayMenuControllerImpl {
        fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup
        fun onCreateOverlayView(): View? = null
        fun onShow()
        fun onMenuItemClicked(@IdRes viewId: Int): Unit? = null
    }

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockResources: Resources
    @Mock private lateinit var mockDisplay: Display
    @Mock private lateinit var mockSharedPrefs: SharedPreferences
    @Mock private lateinit var mockSharedPrefsEditor: SharedPreferences.Editor
    @Mock private lateinit var mockWindowManager: WindowManager
    @Mock private lateinit var mockDisplayManager: DisplayManager
    @Mock private lateinit var mockLayoutInflater: LayoutInflater
    @Mock private lateinit var overlayMenuControllerImpl: OverlayMenuControllerImpl

    /** The object under tests. */
    private lateinit var overlayMenuController: OverlayMenuTestImpl
    /** The screen metrics singleton for the tests. */
    private lateinit var screenMetrics: DisplayMetrics

    /**
     * Create a mock for the menu view.
     *
     * @param items the menu items
     */
    private fun createMockMenuView(items: Sequence<View>) : ViewGroup {
        val menuView = mock(ViewGroup::class.java)

        mockWhen(menuView.childCount).thenReturn(items.count())
        items.forEachIndexed { index, item ->
            mockWhen(menuView.findViewById<View>(item.id)).thenReturn(item)
            mockWhen(menuView.getChildAt(index)).thenReturn(item)
        }

        return menuView
    }

    /**
     * Create a mock for a menu item.
     *
     * @param viewId the view identifier for the menu item.
     */
    private fun createMockMenuItemView(@IdRes viewId: Int = 0): ImageButton = mock(ImageButton::class.java).also {
        mockWhen(it.id).thenReturn(viewId)
    }

    /**
     * Mock the views provided by the tested object implementation.
     *
     * @param mockMenu the view for the overlay menu.
     * @param mockOverlay the overlay view.
     */
    private fun mockViewsFromImpl(mockMenu: ViewGroup = mock(ViewGroup::class.java), mockOverlay: View? = null) {
        mockWhen(mockMenu.findViewById<ViewGroup>(R.id.menu_items)).thenReturn(mockMenu)
        mockWhen(mockMenu.findViewById<ViewGroup>(R.id.menu_background)).thenReturn(mockMenu)
        mockWhen(mockMenu.context).thenReturn(mockContext)
        mockWhen(overlayMenuControllerImpl.onCreateMenu(mockLayoutInflater)).thenReturn(mockMenu)
        mockWhen(overlayMenuControllerImpl.onCreateOverlayView()).thenReturn(mockOverlay)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Mock Android managers
        mockWhen(mockContext.resources).thenReturn(mockResources)
        mockWhen(mockContext.getSystemService(DisplayManager::class.java)).thenReturn(mockDisplayManager)
        mockWhen(mockContext.getSystemService(LayoutInflater::class.java)).thenReturn(mockLayoutInflater)
        mockWhen(mockContext.getSystemService(WindowManager::class.java)).thenReturn(mockWindowManager)
        mockWhen(mockContext.getSharedPreferences(OverlayMenuPositionDataSource.PREFERENCE_NAME, Context.MODE_PRIVATE))
            .thenReturn(mockSharedPrefs)

        // Mock config
        mockWhen(mockResources.getFraction(R.dimen.alpha_menu_item_disabled, 1, 1))
            .thenReturn(TEST_DATA_DISABLED_ALPHA)

        // Mock shared prefs editor to verify insertions
        mockWhen(mockSharedPrefs.edit()).thenReturn(mockSharedPrefsEditor)
        mockWhen(mockSharedPrefsEditor.putInt(anyString(), anyInt())).thenReturn(mockSharedPrefsEditor)

        // Mock get display size
        mockWhen(mockDisplayManager.getDisplay(0)).thenReturn(mockDisplay)
        doAnswer { invocation ->
            val argument = invocation.arguments[0] as Point
            argument.x = TEST_DATA_DISPLAY_WIDTH
            argument.y = TEST_DATA_DISPLAY_HEIGHT
            null
        }.`when`(mockDisplay).getRealSize(any())

        screenMetrics = DisplayMetrics.getInstance(mockContext)
        screenMetrics.startMonitoring(mockContext)
    }

    @After
    fun tearDown() {
        screenMetrics.stopMonitoring(mockContext)
    }

    @Test
    fun createLifecycle() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        mockViewsFromImpl()

        overlayMenuController.create(mockContext)

        inOrder(overlayMenuControllerImpl).apply {
            verify(overlayMenuControllerImpl).onCreateMenu(mockLayoutInflater)
            verify(overlayMenuControllerImpl).onCreateOverlayView()
        }
    }

    @Test
    fun createAddMenuView() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val menuView = mock(ViewGroup::class.java)
        mockViewsFromImpl(menuView)

        overlayMenuController.create(mockContext)
        overlayMenuController.start()

        val wmAddedView = captureWindowManagerAddedMenuView(mockWindowManager)
        assertEquals(menuView, wmAddedView.view)
    }

    @Test
    fun createAddViews() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val menuView = mock(ViewGroup::class.java)
        val overlayView = mock(View::class.java)
        mockViewsFromImpl(menuView, overlayView)

        overlayMenuController.create(mockContext)
        overlayMenuController.start()

        val wmAddedView = captureWindowManagerAddedViews(mockWindowManager)
        assertEquals(overlayView, wmAddedView.first.view)
        assertEquals(menuView, wmAddedView.second.view)
    }

    @Test
    fun getInitialMenuPositionFirstTime() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        mockViewsFromImpl()

        overlayMenuController.create(mockContext)
        overlayMenuController.start()

        val wmAddedView = captureWindowManagerAddedMenuView(mockWindowManager)
        assertEquals("X position should be 0", 0, wmAddedView.params.x)
        assertEquals("Y position should be 0", 0, wmAddedView.params.y)
    }

    @Test
    fun setupViewClickListeners() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val menuItems = sequenceOf(
            createMockMenuItemView(),
            createMockMenuItemView(),
            createMockMenuItemView()
        )
        mockViewsFromImpl(createMockMenuView(menuItems))

        overlayMenuController.create(mockContext)

        menuItems.forEach { verify(it).setOnClickListener(any()) }
    }

    @Test
    fun setupMoveItemTouchListener() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val moveItem = createMockMenuItemView(R.id.btn_move)
        val menuItems = sequenceOf(
            createMockMenuItemView(),
            moveItem,
            createMockMenuItemView()
        )
        mockViewsFromImpl(createMockMenuView(menuItems))

        overlayMenuController.create(mockContext)

        verify(moveItem).setOnTouchListener(any())
    }

    @Test
    fun setupHideItemTouchListener() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val hideItem = createMockMenuItemView(R.id.btn_hide_overlay)
        val menuItems = sequenceOf(
            createMockMenuItemView(),
            hideItem,
            createMockMenuItemView()
        )
        mockViewsFromImpl(createMockMenuView(menuItems))

        overlayMenuController.create(mockContext)

        verify(hideItem).setOnClickListener(any())
    }

    @Test
    fun destroy_removeView() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val menuView = mock(ViewGroup::class.java)
        mockViewsFromImpl(menuView)
        overlayMenuController.create(mockContext)

        overlayMenuController.destroy()

        verify(mockWindowManager).removeView(menuView)
    }

    @Test
    fun destroy_removeAllViews() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val menuView = mock(ViewGroup::class.java)
        val overlayView = mock(View::class.java)
        mockViewsFromImpl(menuView, overlayView)
        overlayMenuController.create(mockContext)

        overlayMenuController.destroy()

        verify(mockWindowManager).removeView(menuView)
        verify(mockWindowManager).removeView(overlayView)
    }

    @Test
    fun setMenuItemDisabledNotClickable() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val item = createMockMenuItemView(42)
        mockViewsFromImpl(createMockMenuView(sequenceOf(
            createMockMenuItemView(),
            item,
        )))
        overlayMenuController.create(mockContext)

        overlayMenuController.publicSetMenuItemViewEnabled(item, false, false)

        verify(item).isEnabled = false
        verify(item).alpha = TEST_DATA_DISABLED_ALPHA
    }

    @Test
    fun setMenuItemDisabledClickable() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val item = createMockMenuItemView(42)
        mockViewsFromImpl(createMockMenuView(sequenceOf(
            createMockMenuItemView(),
            item,
        )))
        overlayMenuController.create(mockContext)

        overlayMenuController.publicSetMenuItemViewEnabled(item, false, true)

        verify(item).isEnabled = true
        verify(item).alpha = TEST_DATA_DISABLED_ALPHA
    }

    @Test
    fun setMenuItemEnabled() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val item = createMockMenuItemView(51)
        mockViewsFromImpl(createMockMenuView(sequenceOf(
            createMockMenuItemView(),
            item,
        )))
        overlayMenuController.create(mockContext)

        overlayMenuController.publicSetMenuItemViewEnabled(item, true)

        verify(item).isEnabled = true
        verify(item).alpha = 1f
    }

    @Test
    fun onMenuItemClicked() {
        overlayMenuController = OverlayMenuTestImpl(overlayMenuControllerImpl)
        val itemId = 42
        val item = createMockMenuItemView(itemId)
        mockViewsFromImpl(createMockMenuView(sequenceOf(
            createMockMenuItemView(),
            item,
        )))
        overlayMenuController.create(mockContext)

        // Get the button click listener and calls it
        val clickListenerCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)
        verify(item).setOnClickListener(clickListenerCaptor.capture())
        clickListenerCaptor.value.onClick(item)

        verify(overlayMenuControllerImpl).onMenuItemClicked(itemId)
    }
}