package org.gdg.frisbee.android.eventseries;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;

import com.squareup.picasso.Picasso;

import org.gdg.frisbee.android.Const;
import org.gdg.frisbee.android.R;
import org.gdg.frisbee.android.api.Callback;
import org.gdg.frisbee.android.api.GdgXHub;
import org.gdg.frisbee.android.api.model.Event;
import org.gdg.frisbee.android.api.model.PagedList;
import org.gdg.frisbee.android.app.App;
import org.gdg.frisbee.android.cache.ModelCache;
import org.gdg.frisbee.android.utils.Utils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class GdgEventListFragment extends EventListFragment {

    private GdgXHub gdgXHub;
    private ModelCache modelCache;
    private String mCacheKey;
    private String mPlusId;

    public static EventListFragment newInstance(String plusId) {
        EventListFragment fragment = new GdgEventListFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Const.EXTRA_PLUS_ID, plusId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    EventAdapter createEventAdapter() {
        Picasso picasso = App.from(getContext()).getPicasso();
        return new EventAdapter(getContext(), picasso);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        gdgXHub = App.from(context).getGdgXHub();
        modelCache = App.from(context).getModelCache();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlusId = getArguments().getString(Const.EXTRA_PLUS_ID);
        mCacheKey = "event_" + mPlusId;
    }

    @Override
    void fetchEvents() {
        setIsLoading(true);

        if (Utils.isOnline(getActivity())) {
            loadFirstPage();
        } else {
            modelCache.getAsync(mCacheKey, false, new ModelCache.CacheListener() {
                @Override
                public void onGet(Object item) {
                    if (checkValidCache(item)) {
                        ArrayList<Event> events = (ArrayList<Event>) item;
                        mAdapter.addAll(events);
                        setIsLoading(false);
                        Snackbar.make(getView(), R.string.cached_content, Snackbar.LENGTH_SHORT).show();
                    } else {
                        modelCache.removeAsync(mCacheKey);
                        onNotFound();
                    }
                }

                @Override
                public void onNotFound(String key) {
                    onNotFound();
                }

                private void onNotFound() {
                    setIsLoading(false);
                    showError(R.string.offline_alert);
                }
            });
        }
    }

    private void loadFirstPage() {
        loadMoreEvents(1);
    }

    @Override
    protected boolean loadMoreEvents(int page) {
        final boolean isInitialPage = page == 1;
        gdgXHub.getChapterAllEventList(mPlusId, page).
            enqueue(new Callback<PagedList<Event>>() {
                @Override
                public void onSuccess(PagedList<Event> eventsPagedList) {
                    List<Event> events = eventsPagedList.getItems();
                    if (isContextValid()) {
                        mAdapter.addAll(events);
                        setIsLoading(false);
                    }
                    if (isInitialPage) {
                        modelCache.putAsync(mCacheKey, events, DateTime.now().plusHours(2));
                    }
                }

                @Override
                public void onError() {
                    setIsLoading(false);
                    showError(R.string.fetch_events_failed);
                }

                @Override
                public void onNetworkFailure(Throwable error) {
                    setIsLoading(false);
                    showError(R.string.offline_alert);
                }
            });
        return true;
    }
}
