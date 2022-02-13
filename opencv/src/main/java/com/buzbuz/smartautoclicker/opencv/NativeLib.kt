package com.buzbuz.smartautoclicker.opencv

import android.graphics.Bitmap

class NativeLib {

    /**
     * A native method that is implemented by the 'smartautoclicker' native library,
     * which is packaged with this application.
     */
    external fun matchCondition(image: Bitmap, condition: Bitmap): Boolean

    companion object {
        // Used to load the 'smartautoclicker' library on application startup.
        init {
            System.loadLibrary("smartautoclicker")
        }
    }
}