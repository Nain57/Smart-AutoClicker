/*
 * Copyright (C) 2025 Kevin Buzeau
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

    private fun buildContent(): String =
        obfuscatedComponents!!.fold("") { acc, new ->
            acc + "\n" + createComponentConstVal(new)
        }

    private fun createComponentConstVal(obfuscatedComponent: ObfuscatedComponent): String = with(obfuscatedComponent) {
        val variableName = originalClassName.replaceFirstChar { it.lowercase() }
        val flattenComponentName =
            if (isRandomized == true) randomizedFlattenComponentName
            else originalFlattenComponentName

        return "    val $variableName = ComponentName.unflattenFromString(\"$flattenComponentName\") \n" +
                "            ?: throw IllegalStateException(\"Invalid component name for $variableName\")"
    }
}
