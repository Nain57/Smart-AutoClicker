package com.buzbuz.smartautoclicker

class NativeLib {

    /**
     * A native method that is implemented by the 'smartautoclicker' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'smartautoclicker' library on application startup.
        init {
            System.loadLibrary("smartautoclicker")
        }
    }
}