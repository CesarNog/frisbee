package org.gdg.frisbee.android.gde;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import org.gdg.frisbee.android.R;
import org.gdg.frisbee.android.api.Callback;
import org.gdg.frisbee.android.api.model.Gde;
import org.gdg.frisbee.android.api.model.GdeList;
import org.gdg.frisbee.android.app.App;
import org.gdg.frisbee.android.cache.ModelCache;
import org.gdg.frisbee.android.common.GdgNavDrawerActivity;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import butterknife.BindView;

public class GdeActivity extends GdgNavDrawerActivity implements ViewPager.OnPageChangeListener {

    @BindView(R.id.pager)
    ViewPager mViewPager;
    @BindView(R.id.tabs)
    TabLayout mTabLayout;

    private GdeCategoryPagerAdapter mViewPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gde);

        App.from(this).getModelCache().getAsync(ModelCache.KEY_GDE_LIST, new ModelCache.CacheListener() {
            @Override
            public void onGet(Object item) {
                GdeList directory = (GdeList) item;
                setupGdeViewPager(directory);
            }

            @Override
            public void onNotFound(String key) {
                fetchGdeDirectory();
            }
        });
    }

    private void fetchGdeDirectory() {
        App.from(this).getGdeDirectory().getDirectory().enqueue(new Callback<GdeList>() {
            @Override
            public void onSuccess(GdeList directory) {
                App.from(GdeActivity.this).getModelCache().putAsync(ModelCache.KEY_GDE_LIST,
                    directory,
                    DateTime.now().plusDays(4));
                setupGdeViewPager(directory);
            }

            @Override
            public void onError() {
                showError(R.string.fetch_gde_failed);
            }

            @Override
            public void onNetworkFailure(Throwable error) {
                showError(R.string.offline_alert);
            }
        });
    }

    private void setupGdeViewPager(GdeList directory) {
        if (!isContextValid()) {
            return;
        }
        SortedMap<String, GdeList> gdeMap = new TreeMap<>();
        gdeMap.putAll(extractCategoriesFromGdeList(directory));
        List<GdeCategory> gdeCategoryList = convertCategoryMapToList(gdeMap);

        mViewPagerAdapter = new GdeCategoryPagerAdapter(
            getSupportFragmentManager(),
            getString(R.string.about),
            gdeCategoryList
        );
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.addOnPageChangeListener(this);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    private HashMap<String, GdeList> extractCategoriesFromGdeList(GdeList directory) {
        HashMap<String, GdeList> gdeMap = new HashMap<>();

        for (Gde gde : directory) {
            if (gde.getProduct() == null) {
                continue;
            }
            for (String p : gde.getProduct()) {
                String product = p.trim();
                if (!gdeMap.containsKey(product)) {
                    gdeMap.put(product, new GdeList());
                }

                gdeMap.get(product).add(gde);
            }
        }
        return gdeMap;
    }

    private List<GdeCategory> convertCategoryMapToList(SortedMap<String, GdeList> gdeMap) {
        List<GdeCategory> gdeCategoryList = new ArrayList<>();
        for (String category : gdeMap.keySet()) {
            GdeList gdeList = gdeMap.get(category);
            gdeCategoryList.add(new GdeCategory(category, gdeList));
        }
        return gdeCategoryList;
    }

    @Override
    protected String getTrackedViewName() {
        return null;
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {
    }

    @Override
    public void onPageSelected(int position) {
        trackView("GDE/" + mViewPagerAdapter.getPageTitle(position));
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }
}
