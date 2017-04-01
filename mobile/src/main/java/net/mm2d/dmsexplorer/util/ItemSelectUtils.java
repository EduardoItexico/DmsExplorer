/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import net.mm2d.android.upnp.cds.CdsObject;
import net.mm2d.android.upnp.cds.Tag;
import net.mm2d.dmsexplorer.Const;
import net.mm2d.dmsexplorer.DataHolder;
import net.mm2d.dmsexplorer.DmcActivity;
import net.mm2d.dmsexplorer.MovieActivity;
import net.mm2d.dmsexplorer.MusicActivity;
import net.mm2d.dmsexplorer.PhotoActivity;
import net.mm2d.dmsexplorer.dialog.SelectDeviceDialog;
import net.mm2d.dmsexplorer.dialog.SelectResourceDialog;

import java.util.List;

/**
 * Item選択後の処理をまとめるクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class ItemSelectUtils {
    public static void play(final @NonNull Activity activity,
                            final @NonNull CdsObject object) {
        final List<Tag> list = object.getTagList(CdsObject.RES);
        if (list == null || list.isEmpty()) {
            return;
        }
        if (list.size() == 1) {
            play(activity, object, 0);
            return;
        }
        final SelectResourceDialog dialog = SelectResourceDialog.newInstance(object);
        dialog.show(activity.getFragmentManager(), "");
    }

    public static void play(final @NonNull Context context,
                            final @NonNull CdsObject object, final int index) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final Tag res = object.getTag(CdsObject.RES, index);
        if (res == null) {
            return;
        }
        final String protocolInfo = res.getAttribute(CdsObject.PROTOCOL_INFO);
        final String mimeType = CdsObject.extractMimeTypeFromProtocolInfo(protocolInfo);
        final Uri uri = Uri.parse(res.getValue());
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        switch (object.getType()) {
            case CdsObject.TYPE_VIDEO:
                if (pref.getBoolean(Const.LAUNCH_APP_MOVIE, true)) {
                    intent.setClass(context, MovieActivity.class);
                    intent.putExtra(Const.EXTRA_OBJECT, object);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                break;
            case CdsObject.TYPE_AUDIO:
                if (pref.getBoolean(Const.LAUNCH_APP_MUSIC, true)) {
                    intent.setClass(context, MusicActivity.class);
                    intent.putExtra(Const.EXTRA_OBJECT, object);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                break;
            case CdsObject.TYPE_IMAGE:
                if (pref.getBoolean(Const.LAUNCH_APP_PHOTO, true)) {
                    intent.setClass(context, PhotoActivity.class);
                    intent.putExtra(Const.EXTRA_OBJECT, object);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                break;
            default:
                return;
        }
        try {
            context.startActivity(intent);
        } catch (final ActivityNotFoundException ignored) {
        }
    }

    public static void send(final @NonNull Activity activity,
                            final @NonNull String udn, final @NonNull CdsObject object) {
        if (DataHolder.getInstance().getControlPointModel().getMrControlPoint().getDeviceListSize() == 0) {
            return;
        }
        final SelectDeviceDialog dialog = SelectDeviceDialog.newInstance(udn, object);
        dialog.show(activity.getFragmentManager(), "");
    }

    public static void send(final @NonNull Context context,
                            final @NonNull String serverUdn, final @NonNull CdsObject object,
                            final @NonNull String rendererUdn) {
        final Tag res = object.getTag(CdsObject.RES);
        if (res == null) {
            return;
        }
        final Intent intent = DmcActivity.makeIntent(
                context, serverUdn, object, res.getValue(), rendererUdn);
        context.startActivity(intent);
    }
}