/*
 * Copyright 2013-2015 The GDG Frisbee Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gdg.frisbee.android.pulse;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.gdg.frisbee.android.Const;
import org.gdg.frisbee.android.R;
import org.gdg.frisbee.android.api.Callback;
import org.gdg.frisbee.android.api.GroupDirectory;
import org.gdg.frisbee.android.api.model.Directory;
import org.gdg.frisbee.android.api.model.Pulse;
import org.gdg.frisbee.android.api.model.PulseEntry;
import org.gdg.frisbee.android.app.App;
import org.gdg.frisbee.android.cache.ModelCache;
import org.gdg.frisbee.android.chapter.MainActivity;
import org.gdg.frisbee.android.common.GdgListFragment;
import org.joda.time.DateTime;

import java.util.Map;

public class PulseFragment extends GdgListFragment {

    public static final String GLOBAL = "Global";

    private static final String ARG_MODE = "mode";
    private static final String ARG_TARGET = "target";
    private static final String INSTANCE_STATE_POSITIONS = "INSTANCE_STATE_POSITIONS";

    private int mMode;
    private String mTarget;
    private PulseAdapter adapter;
    private Callbacks mListener;
    private GroupDirectory groupDirectory;
    private ModelCache modelCache;

    public static PulseFragment newInstance(int mode, String target) {
        PulseFragment fragment = new PulseFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_MODE, mode);
        arguments.putString(ARG_TARGET, target);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        if (context instanceof Callbacks) {
            mListener = (Callbacks) context;
        } else {
            throw new ClassCastException("Activity " + context.getClass().getSimpleName()
                + " must implement " + Pulse.class.getSimpleName() + " interface.");
        }
        groupDirectory = App.from(context).getGroupDirectory();
        modelCache = App.from(context).getModelCache();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) {
            outState.putIntArray(INSTANCE_STATE_POSITIONS, adapter.getPositions());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mTarget = getArguments().getString(ARG_TARGET);
        mMode = getArguments().getInt(ARG_MODE);

        final int[] positions = savedInstanceState != null
            ? savedInstanceState.getIntArray(INSTANCE_STATE_POSITIONS) : null;

        modelCache.getAsync(ModelCache.KEY_CHAPTER_LIST_HUB, new ModelCache.CacheListener() {
            @Override
            public void onGet(Object item) {
                createAdapter(positions, (Directory) item);
            }

            @Override
            public void onNotFound(String key) {
                createAdapter(positions, null);
            }
        });

        setIsLoading(true);
    }

    void createAdapter(int[] positions, @Nullable Directory directory) {
        adapter = new PulseAdapter(getActivity(), positions, directory);
        setListAdapter(adapter);
        modelCache.getAsync(ModelCache.KEY_PULSE + mTarget.toLowerCase().replace(" ", "-"),
            true,
            new ModelCache.CacheListener() {
                @Override
                public void onGet(Object item) {
                    Pulse pulse = (Pulse) item;
                    initAdapter(pulse);
                }

                @Override
                public void onNotFound(String key) {
                    fetchPulseTask();
                }
            });
    }


    private void fetchPulseTask() {
        if (isGlobalSelected()) {
            groupDirectory.getPulse().enqueue(new Callback<Pulse>() {
                @Override
                public void onSuccess(Pulse pulse) {
                    if (isContextValid()) {
                        initAdapter(pulse);
                    }
                    modelCache.putAsync(ModelCache.KEY_PULSE + mTarget.toLowerCase(), pulse,
                        DateTime.now().plusDays(1));
                }

                @Override
                public void onError() {
                    showError(R.string.server_error);
                }

                @Override
                public void onNetworkFailure(Throwable error) {
                    showError(R.string.offline_alert);
                }
            });
        } else {
            groupDirectory.getCountryPulse(mTarget).enqueue(new Callback<Pulse>() {
                @Override
                public void onSuccess(Pulse pulse) {
                    if (isContextValid()) {
                        initAdapter(pulse);
                    }
                    modelCache.putAsync(ModelCache.KEY_PULSE + mTarget.toLowerCase().replace(" ", "-"),
                        pulse,
                        DateTime.now().plusDays(1));
                }

                @Override
                public void onError() {
                    showError(R.string.server_error);
                }

                @Override
                public void onNetworkFailure(Throwable error) {
                    showError(R.string.offline_alert);
                }
            });
        }
    }

    private boolean isGlobalSelected() {
        return mTarget.equals(GLOBAL);
    }

    private void initAdapter(Pulse pulse) {
        adapter.setPulse(mMode, pulse, !isGlobalSelected());
        adapter.notifyDataSetChanged();
        setIsLoading(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflateView(inflater, R.layout.fragment_pulse, container);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Map.Entry<String, PulseEntry> pulse = adapter.getItem(position);

        if (isGlobalSelected()) {
            mListener.openPulse(pulse.getKey());
        } else {
            if (v.isEnabled()) {
                Intent chapterIntent = new Intent(getActivity(), MainActivity.class);
                chapterIntent.putExtra(Const.EXTRA_CHAPTER_ID, pulse.getValue().getId());
                startActivity(chapterIntent);
            }
        }
    }

    public interface Callbacks {
        void openPulse(String key);
    }
}
