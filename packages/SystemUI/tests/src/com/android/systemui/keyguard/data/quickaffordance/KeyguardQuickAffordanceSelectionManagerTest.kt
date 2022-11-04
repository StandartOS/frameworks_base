/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.android.systemui.keyguard.data.quickaffordance

import android.content.SharedPreferences
import android.content.pm.UserInfo
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.filters.SmallTest
import com.android.systemui.SysuiTestCase
import com.android.systemui.settings.FakeUserTracker
import com.android.systemui.settings.UserFileManager
import com.android.systemui.util.FakeSharedPreferences
import com.android.systemui.util.mockito.whenever
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@SmallTest
@RunWith(JUnit4::class)
class KeyguardQuickAffordanceSelectionManagerTest : SysuiTestCase() {

    @Mock private lateinit var userFileManager: UserFileManager

    private lateinit var underTest: KeyguardQuickAffordanceSelectionManager

    private lateinit var userTracker: FakeUserTracker
    private lateinit var sharedPrefs: MutableMap<Int, SharedPreferences>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        sharedPrefs = mutableMapOf()
        whenever(userFileManager.getSharedPreferences(anyString(), anyInt(), anyInt())).thenAnswer {
            val userId = it.arguments[2] as Int
            sharedPrefs.getOrPut(userId) { FakeSharedPreferences() }
        }
        userTracker = FakeUserTracker()

        underTest =
            KeyguardQuickAffordanceSelectionManager(
                userFileManager = userFileManager,
                userTracker = userTracker,
            )
    }

    @Test
    fun setSelections() = runTest {
        val affordanceIdsBySlotId = mutableListOf<Map<String, List<String>>>()
        val job =
            launch(UnconfinedTestDispatcher()) {
                underTest.selections.toList(affordanceIdsBySlotId)
            }
        val slotId1 = "slot1"
        val slotId2 = "slot2"
        val affordanceId1 = "affordance1"
        val affordanceId2 = "affordance2"
        val affordanceId3 = "affordance3"

        underTest.setSelections(
            slotId = slotId1,
            affordanceIds = listOf(affordanceId1),
        )
        assertSelections(
            affordanceIdsBySlotId.last(),
            mapOf(
                slotId1 to listOf(affordanceId1),
            ),
        )

        underTest.setSelections(
            slotId = slotId2,
            affordanceIds = listOf(affordanceId2),
        )
        assertSelections(
            affordanceIdsBySlotId.last(),
            mapOf(
                slotId1 to listOf(affordanceId1),
                slotId2 to listOf(affordanceId2),
            )
        )

        underTest.setSelections(
            slotId = slotId1,
            affordanceIds = listOf(affordanceId1, affordanceId3),
        )
        assertSelections(
            affordanceIdsBySlotId.last(),
            mapOf(
                slotId1 to listOf(affordanceId1, affordanceId3),
                slotId2 to listOf(affordanceId2),
            )
        )

        underTest.setSelections(
            slotId = slotId1,
            affordanceIds = listOf(affordanceId3),
        )
        assertSelections(
            affordanceIdsBySlotId.last(),
            mapOf(
                slotId1 to listOf(affordanceId3),
                slotId2 to listOf(affordanceId2),
            )
        )

        underTest.setSelections(
            slotId = slotId2,
            affordanceIds = listOf(),
        )
        assertSelections(
            affordanceIdsBySlotId.last(),
            mapOf(
                slotId1 to listOf(affordanceId3),
                slotId2 to listOf(),
            )
        )

        job.cancel()
    }

    @Test
    fun `remembers selections by user`() = runTest {
        val slot1 = "slot_1"
        val slot2 = "slot_2"
        val affordance1 = "affordance_1"
        val affordance2 = "affordance_2"
        val affordance3 = "affordance_3"

        val affordanceIdsBySlotId = mutableListOf<Map<String, List<String>>>()
        val job =
            launch(UnconfinedTestDispatcher()) {
                underTest.selections.toList(affordanceIdsBySlotId)
            }

        val userInfos =
            listOf(
                UserInfo(/* id= */ 0, "zero", /* flags= */ 0),
                UserInfo(/* id= */ 1, "one", /* flags= */ 0),
            )
        userTracker.set(
            userInfos = userInfos,
            selectedUserIndex = 0,
        )
        underTest.setSelections(
            slotId = slot1,
            affordanceIds = listOf(affordance1),
        )
        underTest.setSelections(
            slotId = slot2,
            affordanceIds = listOf(affordance2),
        )

        // Switch to user 1
        userTracker.set(
            userInfos = userInfos,
            selectedUserIndex = 1,
        )
        // We never set selections on user 1, so it should be empty.
        assertSelections(
            observed = affordanceIdsBySlotId.last(),
            expected = emptyMap(),
        )
        // Now, let's set selections on user 1.
        underTest.setSelections(
            slotId = slot1,
            affordanceIds = listOf(affordance2),
        )
        underTest.setSelections(
            slotId = slot2,
            affordanceIds = listOf(affordance3),
        )
        assertSelections(
            observed = affordanceIdsBySlotId.last(),
            expected =
                mapOf(
                    slot1 to listOf(affordance2),
                    slot2 to listOf(affordance3),
                ),
        )

        // Switch back to user 0.
        userTracker.set(
            userInfos = userInfos,
            selectedUserIndex = 0,
        )
        // Assert that we still remember the old selections for user 0.
        assertSelections(
            observed = affordanceIdsBySlotId.last(),
            expected =
                mapOf(
                    slot1 to listOf(affordance1),
                    slot2 to listOf(affordance2),
                ),
        )

        job.cancel()
    }

    private fun assertSelections(
        observed: Map<String, List<String>>?,
        expected: Map<String, List<String>>,
    ) {
        assertThat(underTest.getSelections()).isEqualTo(expected)
        assertThat(observed).isEqualTo(expected)
    }
}
