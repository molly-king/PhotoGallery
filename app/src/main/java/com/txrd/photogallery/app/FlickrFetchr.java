package com.txrd.photogallery.app;

import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by mollyrand on 6/27/14.
 */
public class FlickrFetchr {

    public static final String TAG = "FlickrFetchr";
    private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "aa3295803575cee4e4f659bc143c4113";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String PARAM_TAGS = "tags";
    private static final String TAG_DERBY = "txrd";
    private static final String PARAM_LICENSE = "license";
    private static final String LICENSE_OPEN = "7";
    private static final String PARAM_EXTRAS = "extras";
    private static final String XML_PHOTO = "photo";
    private static final String PARAM_PAGENUM = "page";
    private static final String PARAM_PER_PAGE = "per_page";
    private static final String PER_PAGE_50 = "50";
    private static int totalPages;


    //only get the URL for the small version of the picture (if available)
    private static final String EXTRA_SMALL_URL = "url_s";

    public byte[] getUrlBytes(String url) throws IOException{
        URL url1 = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) url1.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                return null;
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0){
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrl(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws XmlPullParserException, IOException{
        int eventType = parser.next();

        while (eventType != XmlPullParser.END_DOCUMENT){
            if (eventType == XmlPullParser.START_TAG &&
                    XML_PHOTO.equals(parser.getName())){
                String id = parser.getAttributeValue(null, "id");
                String caption = parser.getAttributeValue(null, "title");
                String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);

                GalleryItem item = new GalleryItem();

                item.setmId(id);
                item.setmCaption(caption);
                item.setmUrl(smallUrl);
                items.add(item);
            }
            eventType = parser.next();
        }
    }

    public ArrayList<GalleryItem> fetchItems(int page){//put pagenum as parameter

        ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
        try{
            String url = Uri.parse(ENDPOINT).buildUpon()
                    .appendQueryParameter("method", METHOD_SEARCH)
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                    .appendQueryParameter(PARAM_TAGS, TAG_DERBY)
//                    .appendQueryParameter(PARAM_LICENSE, LICENSE_OPEN)
                    .appendQueryParameter(PARAM_PAGENUM, String.valueOf(page))
                    .appendQueryParameter(PARAM_PER_PAGE, PER_PAGE_50)
                    .build().toString();
            String xmlString = getUrl(url);
            Log.i(TAG, "received XML: " + xmlString);
            int start = xmlString.indexOf("pages") + 7;
            String total = xmlString.substring(start, xmlString.indexOf(" ", start)-1);
            totalPages = Integer.parseInt(total);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlString));

            parseItems(items, parser);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items" , ioe);
        } catch (XmlPullParserException xppe){
            Log.e(TAG, "Failed to parse items", xppe);
        }
        return items;
    }

    public static int getTotalPages(){
        return totalPages;
    }
}
