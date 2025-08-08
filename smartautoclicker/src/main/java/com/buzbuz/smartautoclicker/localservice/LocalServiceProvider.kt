
package com.buzbuz.smartautoclicker.localservice

object LocalServiceProvider {

    /** The instance of the [ILocalService], providing access for this service to the Activity. */
    var localServiceInstance: ILocalService? = null
        private set(value) {
            field = value
            localServiceCallback?.invoke(field)
        }
    /** Callback upon the availability of the [localServiceInstance]. */
    private var localServiceCallback: ((ILocalService?) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(localServiceInstance)
        }

    fun setLocalService(service: ILocalService?) {
        localServiceInstance = service
    }
    /**
     * Static method allowing an activity to register a callback in order to monitor the availability of the
     * [ILocalService]. If the service is already available upon registration, the callback will be immediately
     * called.
     *
     * @param stateCallback the object to be notified upon service availability.
     */
    fun getLocalService(stateCallback: ((ILocalService?) -> Unit)?) {
        localServiceCallback = stateCallback
    }

    fun isServiceStarted(): Boolean = localServiceInstance != null
}