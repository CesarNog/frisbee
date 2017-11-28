package org.gdg.frisbee.android.chapter;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Toast;

import org.gdg.frisbee.android.R;
import org.gdg.frisbee.android.api.model.Chapter;
import org.gdg.frisbee.android.api.model.Directory;
import org.gdg.frisbee.android.app.App;
import org.gdg.frisbee.android.cache.ModelCache;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChapterSelectDialog extends AppCompatDialogFragment
    implements AdapterView.OnItemClickListener {

    private static final String EXTRA_SELECTED_CHAPTER = "EXTRA_SELECTED_CHAPTER";

    @BindView(R.id.filter) SearchView cityNameSearchView;
    @BindView(android.R.id.list) ListView listView;

    private List<Chapter> chapters;

    private Listener listener = Listener.EMPTY;
    @Nullable private Chapter selectedChapter;

    public static ChapterSelectDialog newInstance(@Nullable Chapter selectedChapter) {
        ChapterSelectDialog fragment = new ChapterSelectDialog();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_SELECTED_CHAPTER, selectedChapter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedChapter = getArguments().getParcelable(EXTRA_SELECTED_CHAPTER);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater()
            .inflate(R.layout.view_location_list_preference, (ViewGroup) getView(), false);
        ButterKnife.bind(this, view);

        return new AlertDialog.Builder(getActivity())
            .setNegativeButton(android.R.string.cancel, null)
            .setView(view)
            .create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        App.from(getContext()).getModelCache().getAsync(ModelCache.KEY_CHAPTER_LIST_HUB, false,
            new ModelCache.CacheListener() {
                @Override
                public void onGet(Object item) {
                    chapters = ((Directory) item).getGroups();
                    setupUI();
                }

                @Override
                public void onNotFound(String key) {
                    // TODO load from the network.
                    Toast.makeText(getContext(), R.string.fetch_chapters_failed, Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
    }

    private void setupUI() {
        listView.setOnItemClickListener(this);

        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        final CheckedItemAdapter adapter = new CheckedItemAdapter(
            getContext(),
            android.R.layout.simple_list_item_single_choice,
            android.R.id.text1,
            chapters
        );
        listView.setAdapter(adapter);

        if (selectedChapter != null) {
            int selectedItemPos = chapters.indexOf(selectedChapter);
            listView.setSelection(selectedItemPos);
            listView.setItemChecked(selectedItemPos, true);
        }

        final Filter.FilterListener filterListener = new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                int index = findIndexByValueInFilteredListView(selectedChapter);
                listView.setItemChecked(index, true);
            }
        };

        cityNameSearchView.setOnQueryTextListener(
            new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.getFilter().filter(newText, filterListener);
                    return true;
                }
            }
        );
        cityNameSearchView.requestFocus();
        adapter.getFilter().filter(cityNameSearchView.getQuery(), filterListener);
    }

    private int findIndexByValueInFilteredListView(@Nullable Chapter value) {
        if (value == null || listView == null) {
            return -1;
        }
        Adapter adapter = listView.getAdapter();
        for (int i = adapter.getCount() - 1; i >= 0; i--) {
            Chapter item = (Chapter) adapter.getItem(i);
            if (item.equals(value)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        }
    }

    @Override
    public void onDetach() {
        listener = Listener.EMPTY;
        super.onDetach();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (getDialog().isShowing()) {
            selectedChapter = (Chapter) parent.getItemAtPosition(position);
            listener.onChapterSelected(selectedChapter);
            getDialog().dismiss();
        }
    }

    private static class CheckedItemAdapter extends ArrayAdapter<Chapter> {
        CheckedItemAdapter(Context context, int resource, int textViewResourceId,
                           List<Chapter> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    public interface Listener {
        void onChapterSelected(Chapter selectedChapter);

        Listener EMPTY = new Listener() {
            @Override
            public void onChapterSelected(Chapter selectedChapter) {
                // no-op
            }
        };
    }
}
