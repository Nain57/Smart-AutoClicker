
package com.buzbuz.gradle.obfuscation.component

internal class ConfigFileContentBuilder {

    var appId: String? = null
    var isRandomized: Boolean ? = null
    var obfuscatedComponents: Iterable<ObfuscatedComponent>? = null


    fun build(): String =
        buildString {
            append(buildHeader())
            append(buildContent())
            append("\n}")
        }

    private fun buildHeader(): String =
        """
            // THIS FILE IS GENERATED, DO NOT MODIFY
            package ${appId!!}
            
            import android.content.ComponentName
            
            object ComponentConfig {
        """.trimIndent()

    private fun buildContent(): String {
        val appIdVal = createAppIdConstVal(appId ?: "")
        val componentVals = obfuscatedComponents!!.fold("") { acc, new ->
            acc + "\n" + createComponentConstVal(new)
        }

        return appIdVal + componentVals
    }


    private fun createComponentConstVal(obfuscatedComponent: ObfuscatedComponent): String = with(obfuscatedComponent) {
        val variableName = originalClassName.replaceFirstChar { it.lowercase() }
        val flattenComponentName =
            if (isRandomized == true) randomizedFlattenComponentName
            else originalFlattenComponentName

        return "    val $variableName = ComponentName.unflattenFromString(\"$flattenComponentName\") \n" +
                "            ?: throw IllegalStateException(\"Invalid component name for $variableName\")"
    }

    private fun createAppIdConstVal(appId: String): String =
        "\n    const val ORIGINAL_APP_ID = \"$appId\" \n"
}
