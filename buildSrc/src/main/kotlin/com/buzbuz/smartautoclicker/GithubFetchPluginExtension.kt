/*
* Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class GithubFetchPluginExtension {
    abstract val projects: NamedDomainObjectContainer<GitHubProject>
}

abstract class GitHubProject {
    abstract val name: String

    abstract val projectAccount: Property<String>
    abstract val projectName: Property<String>
    abstract val projectVersion: Property<String>

    abstract val fetchForTask: Property<Task>
    abstract val unzipPath: DirectoryProperty
}