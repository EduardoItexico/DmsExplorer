/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.domain.model;

import android.support.annotation.NonNull;

import net.mm2d.android.upnp.cds.CdsObject;

import java.util.List;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface ExploreListener {
    void onStart();

    void onUpdate(@NonNull List<CdsObject> list);

    void onComplete();
}