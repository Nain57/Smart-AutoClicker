/*
* Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.processing.shadows;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import static org.mockito.Mockito.when;

/** [org.robolectric.shadow.api.Shadow] for [Bitmap] allowing to provide a mock on [Bitmap.createBitmap] call. */
@Implements(value = Bitmap.class)
public class ShadowBitmapCreator {

    /** Interface to be mocked and provided via {@link #setMockInstance(BitmapCreator)}. */
    public interface BitmapCreator {
        Bitmap createBitmap(int width, int height, @NonNull Bitmap.Config config);
        Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height);
    }

    /** The current mock for the bitmap. */
    @Nullable
    private static BitmapCreator mockBitmapCreator = null;

    /**
     * Method to be called from the tests to set the mock returned by the [Bitmap.createBitmap] method.
     *
     * @param mock the mock to be provided to the tested code.
     */
    public static void setMockInstance(@NonNull BitmapCreator mock) {
        mockBitmapCreator = mock;
    }

    @NonNull
    @Implementation
    public static Bitmap createBitmap(int width, int height, @NonNull Bitmap.Config config) {
        if (mockBitmapCreator == null) {
            throw new IllegalStateException("Bitmap is not mocked");
        }

        Bitmap mockBitmap = mockBitmapCreator.createBitmap(width, height, config);
        when(mockBitmap.getWidth()).thenReturn(width);
        when(mockBitmap.getHeight()).thenReturn(height);
        when(mockBitmap.getConfig()).thenReturn(config);
        return mockBitmap;
    }

    @NonNull
    @Implementation
    public static Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height) {
        if (mockBitmapCreator == null) {
            throw new IllegalStateException("Bitmap is not mocked");
        }

        Bitmap mockBitmap = mockBitmapCreator.createBitmap(source, x, y, width, height);
        when(mockBitmap.getWidth()).thenReturn(width);
        when(mockBitmap.getHeight()).thenReturn(height);
        return mockBitmap;
    }

    /** Call this method between each tests to clean the mock. */
    @Resetter
    public static void reset() {
        mockBitmapCreator = null;
    }
}
