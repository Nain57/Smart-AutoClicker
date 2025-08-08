
package com.buzbuz.smartautoclicker.core.common.overlays.menu

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.IdRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buzbuz.smartautoclicker.core.common.overlays.di.OverlaysEntryPoint
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common.OverlayMenuPositionDataSource
import com.buzbuz.smartautoclicker.core.common.overlays.testutils.captureWindowManagerAddedMenuView
import com.buzbuz.smartautoclicker.core.common.overlays.testutils.captureWindowManagerAddedViews
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.display.di.DisplayEntryPoint
import com.buzbuz.smartautoclicker.core.common.overlays.R
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfig
import dagger.hilt.EntryPoints
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config
import org.mockito.Mockito.`when` as mockWhen


/** Test the [OverlayMenu] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q], application = HiltTestApplication::class)
@HiltAndroidTest
class OverlayMenuTests {

    private companion object {
        private const val TEST_DATA_DISABLED_ALPHA = 0.4f
        private const val TEST_DATA_DISPLAY_WIDTH = 800
        private const val TEST_DATA_DISPLAY_HEIGHT = 600

        private val DEFAULT_DISPLAY_CONFIG = DisplayConfig(
            sizePx = Point(TEST_DATA_DISPLAY_WIDTH, TEST_DATA_DISPLAY_HEIGHT),
            orientation = 0,
            safeInsetTopPx = 0,
            roundedCorners = emptyMap(),
        )
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
    @Mock private lateinit var mockSharedPrefs: SharedPreferences
    @Mock private lateinit var mockSharedPrefsEditor: SharedPreferences.Editor
    @Mock private lateinit var mockWindowManager: WindowManager
    @Mock private lateinit var mockLayoutInflater: LayoutInflater

    @Mock private lateinit var mockDisplayEntryPoint: DisplayEntryPoint
    @Mock private lateinit var mockDisplayConfigManager: DisplayConfigManager

    @Mock private lateinit var mockUiEntryPoint: OverlaysEntryPoint
    @Mock private lateinit var mockOverlayMenuPositionDataSource: OverlayMenuPositionDataSource

    @Mock private lateinit var overlayMenuControllerImpl: OverlayMenuControllerImpl

    @get:Rule
    var hiltAndroidRule: HiltAndroidRule = HiltAndroidRule(this)

    /** The object under tests. */
    private lateinit var overlayMenuController: OverlayMenuTestImpl

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

        // Mock hilt entry points
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(mockContext, DisplayEntryPoint::class.java) } returns mockDisplayEntryPoint
        every { EntryPoints.get(mockContext, OverlaysEntryPoint::class.java) } returns mockUiEntryPoint

        // Mock Android managers
        mockWhen(mockContext.applicationContext).thenReturn(mockContext)
        mockWhen(mockContext.resources).thenReturn(mockResources)
        mockWhen(mockContext.getSystemService(LayoutInflater::class.java)).thenReturn(mockLayoutInflater)
        mockWhen(mockContext.getSystemService(WindowManager::class.java)).thenReturn(mockWindowManager)
        mockWhen(mockContext.getSharedPreferences(OverlayMenuPositionDataSource.PREFERENCE_NAME, Context.MODE_PRIVATE))
            .thenReturn(mockSharedPrefs)

        // Mock display metrics
        mockWhen(mockDisplayEntryPoint.displayMetrics()).thenReturn(mockDisplayConfigManager)
        mockWhen(mockDisplayConfigManager.displayConfig).thenReturn(DEFAULT_DISPLAY_CONFIG)

        // Mock position data source
        mockWhen(mockUiEntryPoint.overlayMenuPositionDataSource()).thenReturn(mockOverlayMenuPositionDataSource)

        // Mock config
        mockWhen(mockResources.getFraction(R.dimen.alpha_menu_item_disabled, 1, 1))
            .thenReturn(TEST_DATA_DISABLED_ALPHA)

        // Mock shared prefs editor to verify insertions
        mockWhen(mockSharedPrefs.edit()).thenReturn(mockSharedPrefsEditor)
        mockWhen(mockSharedPrefsEditor.putInt(anyString(), anyInt())).thenReturn(mockSharedPrefsEditor)
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
}