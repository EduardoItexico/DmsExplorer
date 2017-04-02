/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.view;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.mm2d.android.upnp.avt.MrControlPoint;
import net.mm2d.android.upnp.cds.CdsObject;
import net.mm2d.android.upnp.cds.MediaServer;
import net.mm2d.android.util.AribUtils;
import net.mm2d.dmsexplorer.R;
import net.mm2d.dmsexplorer.Repository;
import net.mm2d.dmsexplorer.view.adapter.CdsPropertyAdapter;
import net.mm2d.dmsexplorer.util.ItemSelectUtils;
import net.mm2d.dmsexplorer.util.ToolbarThemeUtils;
import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

/**
 * CDSアイテムの詳細情報を表示するFragment。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class CdsDetailFragment extends Fragment {
    private static final String KEY_TWO_PANE = "KEY_TWO_PANE";
    /**
     * インスタンスを作成する。
     *
     * <p>Bundleの設定と読み出しをこのクラス内で完結させる。
     *
     * @return インスタンス。
     */
    @NonNull
    public static CdsDetailFragment newInstance() {
        final CdsDetailFragment instance = new CdsDetailFragment();
        final Bundle arguments = new Bundle();
        arguments.putBoolean(KEY_TWO_PANE, true);
        instance.setArguments(arguments);
        return instance;
    }

    private boolean isTwoPane() {
        final Bundle arguments = getArguments();
        if (arguments == null) {
            return false;
        }
        return arguments.getBoolean(KEY_TWO_PANE);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.cds_detail_fragment, container, false);

        final Repository repository = Repository.getInstance();
        final MediaServer server = repository.getControlPointModel().getSelectedMediaServer();
        final CdsObject object = repository.getCdsTreeModel().getSelectedObject();
        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.cdsDetailToolbar);
        if (object == null || server == null || toolbar == null) {
            getActivity().finish();
            return rootView;
        }
        toolbar.setTitle(AribUtils.toDisplayableString(object.getTitle()));

        ToolbarThemeUtils.setCdsDetailTheme(getActivity(), object,
                (CollapsingToolbarLayout) rootView.findViewById(R.id.toolbarLayout), !isTwoPane());

        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.cdsDetail);
        recyclerView.setAdapter(new CdsPropertyAdapter(getActivity(), object));

        setUpPlayButton(getActivity(), (FloatingActionButton) rootView.findViewById(R.id.fabPlay), object);
        setUpSendButton(getActivity(), (FloatingActionButton) rootView.findViewById(R.id.fabSend), server.getUdn(), object);
        return rootView;
    }

    private static void setUpPlayButton(
            @NonNull final Activity activity,
            @NonNull final FloatingActionButton fab,
            @NonNull final CdsObject object) {
        fab.setVisibility(hasResource(object) ? View.VISIBLE : View.GONE);
        final boolean protectedResource = object.hasProtectedResource();
        final int color = protectedResource ?
                ContextCompat.getColor(activity, R.color.fabDisable) :
                ContextCompat.getColor(activity, R.color.accent);
        fab.setBackgroundTintList(ColorStateList.valueOf(color));
        if (protectedResource) {
            fab.setOnClickListener(CdsDetailFragment::showNotSupportDrmSnackbar);
            fab.setOnLongClickListener(view -> {
                Snackbar.make(view, R.string.toast_not_support_drm, Snackbar.LENGTH_LONG).show();
                return true;
            });
            return;
        }
        fab.setOnClickListener(view -> ItemSelectUtils.play(activity, object, 0));
        fab.setOnLongClickListener(view -> {
            ItemSelectUtils.play(activity, object);
            return true;
        });
    }

    private static void showNotSupportDrmSnackbar(@NonNull View view) {
        Snackbar.make(view, R.string.toast_not_support_drm, Snackbar.LENGTH_LONG).show();
    }

    private static void setUpSendButton(
            @NonNull final Activity activity,
            @NonNull final FloatingActionButton fab,
            @NonNull final String udn,
            @NonNull final CdsObject object) {
        final MrControlPoint cp = Repository.getInstance().getControlPointModel().getMrControlPoint();
        if (cp.getDeviceListSize() == 0) {
            fab.setVisibility(View.GONE);
            return;
        }
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(v -> ItemSelectUtils.send(activity, udn, object));
    }

    private static boolean hasResource(@NonNull final CdsObject object) {
        return object.getTagList(CdsObject.RES) != null;
    }
}