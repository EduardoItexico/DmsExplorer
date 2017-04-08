/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.view.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateFormat;

import net.mm2d.android.upnp.cds.CdsObject;
import net.mm2d.android.upnp.cds.Tag;
import net.mm2d.android.util.AribUtils;
import net.mm2d.dmsexplorer.R;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class ContentPropertyAdapter extends PropertyAdapter {
    private static String sTb;
    private static String sBs;
    private static String sCs;

    // 初回に変換して保持しておく
    private static void setupString(@NonNull final Context context) {
        if (sTb != null) {
            return;
        }
        sTb = context.getString(R.string.network_tb);
        sBs = context.getString(R.string.network_bs);
        sCs = context.getString(R.string.network_cs);
    }

    public ContentPropertyAdapter(@NonNull final Context context, @NonNull final CdsObject object) {
        super(context);
        setupString(context);
        setCdsObjectInfo(context, this, object);
    }

    private static void setCdsObjectInfo(@NonNull final Context context,
                                         @NonNull final PropertyAdapter adapter,
                                         @NonNull final CdsObject object) {
        adapter.addEntry(context.getString(R.string.prop_title),
                AribUtils.toDisplayableString(object.getTitle()));
        adapter.addEntry(context.getString(R.string.prop_channel),
                getChannel(object));
        adapter.addEntry(context.getString(R.string.prop_date),
                getDate(object));
        adapter.addEntry(context.getString(R.string.prop_schedule),
                getSchedule(object));
        adapter.addEntry(context.getString(R.string.prop_genre),
                object.getValue(CdsObject.UPNP_GENRE));

        adapter.addEntry(context.getString(R.string.prop_album),
                object.getValue(CdsObject.UPNP_ALBUM));
        adapter.addEntry(context.getString(R.string.prop_artist),
                jointMembers(object, CdsObject.UPNP_ARTIST));
        adapter.addEntry(context.getString(R.string.prop_actor),
                jointMembers(object, CdsObject.UPNP_ACTOR));
        adapter.addEntry(context.getString(R.string.prop_author),
                jointMembers(object, CdsObject.UPNP_AUTHOR));
        adapter.addEntry(context.getString(R.string.prop_creator),
                object.getValue(CdsObject.DC_CREATOR));

        adapter.addEntry(context.getString(R.string.prop_description),
                jointTagValue(object, CdsObject.DC_DESCRIPTION));
        adapter.addEntry(context.getString(R.string.prop_long_description),
                jointLongDescription(object), Type.DESCRIPTION);
        adapter.addEntry(CdsObject.UPNP_CLASS + ":",
                object.getUpnpClass());
    }

    @Nullable
    private static String jointTagValue(@NonNull final CdsObject object, @NonNull final String tagName) {
        final List<Tag> tagList = object.getTagList(tagName);
        if (tagList == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (final Tag tag : tagList) {
            if (sb.length() != 0) {
                sb.append('\n');
            }
            sb.append(tag.getValue());
        }
        return AribUtils.toDisplayableString(sb.toString());
    }

    @Nullable
    private static String jointLongDescription(@NonNull final CdsObject object) {
        final List<Tag> tagList = getLongDescription(object);
        if (tagList == null) {
            return null;
        }
        try {
            final StringBuilder sb = new StringBuilder();
            String lastName = null;
            for (final Tag tag : tagList) {
                final String value = tag.getValue();
                if (TextUtils.isEmpty(value)) {
                    continue;
                }
                final byte[] bytes = value.getBytes("UTF-8");
                final int length = Math.min(24, bytes.length);
                final String nameSection = new String(bytes, 0, length, "UTF-8");
                final String name = nameSection.trim();

                if (!TextUtils.equals(lastName, name)) {
                    if (sb.length() != 0) {
                        sb.append('\n');
                    }
                    sb.append(TITLE_PREFIX);
                    sb.append(name);
                    sb.append('\n');
                }
                lastName = name;
                if (value.length() > nameSection.length()) {
                    sb.append(value.substring(nameSection.length()).trim());
                    sb.append('\n');
                }
            }
            return AribUtils.toDisplayableString(sb.toString());
        } catch (final UnsupportedEncodingException ignored) {
        }
        return null;
    }

    private static List<Tag> getLongDescription(@NonNull final CdsObject object) {
        final List<Tag> tagList = object.getTagList(CdsObject.ARIB_LONG_DESCRIPTION);
        if (tagList == null) {
            return null;
        }
        final int size = tagList.size();
        if (size <= 2) {
            return tagList;
        }
        final List<Tag> list = new ArrayList<>(size);
        list.addAll(tagList.subList(size - 2, size));
        list.addAll(tagList.subList(0, size - 2));
        return list;
    }

    @Nullable
    private static String jointMembers(@NonNull final CdsObject object, @NonNull final String tagName) {
        final List<Tag> tagList = object.getTagList(tagName);
        if (tagList == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (final Tag tag : tagList) {
            if (sb.length() != 0) {
                sb.append('\n');
            }
            sb.append(tag.getValue());
            final String role = tag.getAttribute("role");
            if (role != null) {
                sb.append(" : ");
                sb.append(role);
            }
        }
        return sb.toString();
    }

    @Nullable
    private static String getChannel(@NonNull final CdsObject object) {
        final StringBuilder sb = new StringBuilder();
        final String network = getNetworkString(object);
        if (network != null) {
            sb.append(network);
        }
        final String channelNr = object.getValue(CdsObject.UPNP_CHANNEL_NR);
        if (channelNr != null) {
            if (sb.length() == 0) {
                sb.append(channelNr);
            } else {
                try {
                    final int channel = Integer.parseInt(channelNr);
                    final String nr = String.format(Locale.US, "%1$06d", channel);
                    sb.append(nr.substring(2, 5));
                } catch (final NumberFormatException ignored) {
                }
            }
        }
        final String name = object.getValue(CdsObject.UPNP_CHANNEL_NAME);
        if (name != null) {
            if (sb.length() != 0) {
                sb.append("   ");
            }
            sb.append(name);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    @Nullable
    private static String getNetworkString(@NonNull final CdsObject object) {
        final String net = object.getValue(CdsObject.ARIB_OBJECT_TYPE);
        if (net == null) {
            return null;
        }
        switch (net) {
            case "ARIB_TB":
                return sTb;
            case "ARIB_BS":
                return sBs;
            case "ARIB_CS":
                return sCs;
            default:
                return null;
        }
    }

    @Nullable
    private static String getDate(@NonNull final CdsObject object) {
        final String str = object.getValue(CdsObject.DC_DATE);
        final Date date = CdsObject.parseDate(str);
        if (date == null) {
            return null;
        }
        if (str.length() <= 10) {
            return DateFormat.format("yyyy/MM/dd (E)", date).toString();
        }
        return DateFormat.format("yyyy/M/d (E) kk:mm:ss", date).toString();
    }

    @Nullable
    private static String getSchedule(@NonNull final CdsObject object) {
        final Date start = object.getDateValue(CdsObject.UPNP_SCHEDULED_START_TIME);
        final Date end = object.getDateValue(CdsObject.UPNP_SCHEDULED_END_TIME);
        if (start == null || end == null) {
            return null;
        }
        final String startString = DateFormat.format("yyyy/M/d (E) kk:mm", start).toString();
        final String endString;
        if (end.getTime() - start.getTime() > 12 * 3600 * 1000) {
            endString = DateFormat.format("yyyy/M/d (E) kk:mm", end).toString();
        } else {
            endString = DateFormat.format("kk:mm", end).toString();
        }
        return startString + " ～ " + endString;
    }
}
