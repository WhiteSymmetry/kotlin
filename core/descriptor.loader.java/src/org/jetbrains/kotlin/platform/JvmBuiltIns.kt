/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.kotlin.BuiltInClassesAreSerializableOnJvm
import org.jetbrains.kotlin.serialization.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.sure
import javax.inject.Inject

class JvmBuiltIns(storageManager: StorageManager) : KotlinBuiltIns(storageManager) {
    private var ownerModuleDescriptor: ModuleDescriptor? = null

    @Inject
    fun setOwnerModuleDescriptor(moduleDescriptor: ModuleDescriptor) {
        this.ownerModuleDescriptor = moduleDescriptor
    }

    // Used in container
    @Suppress("unused")
    val isInitialized: Boolean get() = ownerModuleDescriptor != null

    override fun getAdditionalClassPartsProvider(): AdditionalClassPartsProvider {
        return BuiltInClassesAreSerializableOnJvm(builtInsModule, {
            ownerModuleDescriptor.sure { "JvmBuiltins has not been initialized properly" }
        })
    }
}
