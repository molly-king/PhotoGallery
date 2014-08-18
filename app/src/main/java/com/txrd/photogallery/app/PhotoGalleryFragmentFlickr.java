package com.txrd.photogallery.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by mollyrand on 6/27/14.
 */
public class PhotoGalleryFragmentFlickr extends Fragment {

    GridView mGridView;
    ArrayList<GalleryItem> mItems;
    int pageNum = 1;
    ThumbnailDownloader<ImageView> mThumbnailThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //retain Fragment
        setRetainInstance(true);
        setHasOptionsMenu(true);
//        new FetchItemsTask().execute();
        updateItems();
        mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>(){
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail){
                if(isVisible()){
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i("Photofrag", "Background thread started");
    }

    public void updateItems(){
        new FetchItemsTask().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_photo_gallery_flickr, container, false);
        mGridView = (GridView)v.findViewById(R.id.gridView);

        //set up adapter here so every time a new gridview is created on rotation
        //it is reconfigured with an appropriate adapter. also call when object set changes
        setupAdapter();

        return v;
    }

    void setupAdapter(){
        if (getActivity() == null || mGridView == null){
            return;
        }

        if (mItems != null){
            mGridView.setAdapter(new GalleryItemAdapter(mItems));
        } else {
            mGridView.setAdapter(null);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_search:
                getActivity().onSearchRequested();
                return true;
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                        .commit();
                updateItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i("Photofrag", "thread quit");

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
        //cleans up invalid imageviews when screen is rotated
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>>{
        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            Activity activity = getActivity();
            if (activity == null){
                return new ArrayList<GalleryItem>();
            }

            String query = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);

            if (query != null){
                return new FlickrFetchr().search(query);
            } else {
                return new FlickrFetchr().fetchItems();
            }
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> galleryItems) {
            mItems = galleryItems;
            //call setupAdapter now that we have data for it. Do it here to be threadsafe
            setupAdapter();
        }
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem>{
        public GalleryItemAdapter(ArrayList<GalleryItem> items){
            super(getActivity(), 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < getCount()-1) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater()
                            .inflate(R.layout.gallery_item, parent, false);
                }

                ImageView imageView = (ImageView) convertView.findViewById(R.id.gallery_item_imageView);
                imageView.setImageResource(R.drawable.skate_placeholder);
                //this keeps randomly giving me null pointer errors. No idea why yet.
                GalleryItem item = getItem(position);
                mThumbnailThread.queueThumbnail(imageView, item.getmUrl());

            } else {
                //last entry, click to get next round of photos
                if (pageNum < FlickrFetchr.getTotalPages()){
                    convertView = getActivity().getLayoutInflater()
                            .inflate(R.layout.get_next_set, parent, false);
                    //todo:check if you're at last page, change onclick to reset pageNum to 1
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pageNum++;
                            new FetchItemsTask().execute();
                        }
                    });
                } else {
                    convertView = getActivity().getLayoutInflater()
                            .inflate(R.layout.back_to_start, parent, false);
                    convertView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pageNum = 1;
                            new FetchItemsTask().execute();
                        }
                    });
                }

            }
            return convertView;
        }

        @Override
        public int getCount() {
            return super.getCount()+1;
        }
    }
}
