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

package org.gdg.frisbee.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.gdg.frisbee.android.R;
import org.gdg.frisbee.android.api.model.Chapter;
import org.gdg.frisbee.android.appwidget.UpcomingEventWidgetProvider;
import org.gdg.frisbee.android.chapter.ChapterSelectDialog;
import org.gdg.frisbee.android.common.TrackableActivity;
import org.gdg.frisbee.android.fragment.SettingsFragment;
import org.gdg.frisbee.android.utils.PrefUtils;

public class SettingsActivity extends TrackableActivity implements ChapterSelectDialog.Listener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                .add(R.id.settings_fragment, new SettingsFragment())
                .commit();
        }
    }

    @Override
    protected String getTrackedViewName() {
        return "/Settings";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onChapterSelected(Chapter selectedChapter) {
        PrefUtils.setHomeChapter(this, selectedChapter);
        // Update widgets to show newest chosen GdgHome events
        startService(new Intent(this, UpcomingEventWidgetProvider.UpdateService.class));
    }
}
