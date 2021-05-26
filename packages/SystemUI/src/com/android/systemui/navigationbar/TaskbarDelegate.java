/*
 * Copyright 2021 The Android Open Source Project
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
 */

package com.android.systemui.navigationbar;

import android.os.IBinder;

import com.android.internal.view.AppearanceRegion;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.statusbar.CommandQueue;

public class TaskbarDelegate implements CommandQueue.Callbacks {

    private final OverviewProxyService mOverviewProxyService;

    public TaskbarDelegate(OverviewProxyService overviewProxyService) {
        mOverviewProxyService = overviewProxyService;
    }

    @Override
    public void setImeWindowStatus(int displayId, IBinder token, int vis, int backDisposition,
            boolean showImeSwitcher) {
        mOverviewProxyService.notifyImeWindowStatus(displayId, token, vis, backDisposition,
                showImeSwitcher);
    }

    @Override
    public void onRotationProposal(int rotation, boolean isValid) {
        mOverviewProxyService.onRotationProposal(rotation, isValid);
    }

    @Override
    public void disable(int displayId, int state1, int state2, boolean animate) {
        mOverviewProxyService.disable(displayId, state1, state2, animate);
    }

    @Override
    public void onSystemBarAttributesChanged(int displayId, int appearance,
            AppearanceRegion[] appearanceRegions, boolean navbarColorManagedByIme, int behavior,
            boolean isFullscreen) {
        mOverviewProxyService.onSystemBarAttributesChanged(displayId, behavior);
    }
}
