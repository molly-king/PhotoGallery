package com.txrd.photogallery.app;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mollyrand on 6/27/14.
 */
public class PhotoGalleryFragment extends Fragment {

    GridView mGridView;
    ArrayList<GalleryItem> mItems;
    int pageNum = 1;
    ThumbnailDownloader<ImageView> mThumbnailThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //retain Fragment
        setRetainInstance(true);
        new FetchItemsTask().execute();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
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
            return new FlickrFetchr().fetchItems(pageNum);
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
                imageView.setImageResource(R.drawable.true_roots);
                GalleryItem item = getItem(position);
                mThumbnailThread.queueThumbnail(imageView, item.getmUrl());

            } else {
                //last entry, click to get next round of photos
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

            }
            return convertView;
        }

        @Override
        public int getCount() {
            return super.getCount()+1;
        }
    }
}
