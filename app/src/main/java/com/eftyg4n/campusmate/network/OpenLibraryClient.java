package com.eftyg4n.campusmate.network;

import android.content.Context;
import android.net.Uri;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.eftyg4n.campusmate.models.Book;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Searches https://openlibrary.org (free, no API key) with Volley and parses the JSON.
// Response shape used here:
//   { "numFound": 123, "docs": [ { "key": "/works/OL..W", "title": "...",
//       "author_name": ["A","B"], "first_publish_year": 1999, "cover_i": 12345 } ] }
public class OpenLibraryClient {

    public static final String TAG = "open_library_search";

    public interface Callback {
        void onSuccess(List<Book> books);

        // kind: ERROR_OFFLINE, ERROR_TIMEOUT or ERROR_OTHER
        void onError(int kind);
    }

    public static final int ERROR_OFFLINE = 1;
    public static final int ERROR_TIMEOUT = 2;
    public static final int ERROR_OTHER = 3;

    private static final String SEARCH_URL = "https://openlibrary.org/search.json";
    private static final String COVER_URL = "https://covers.openlibrary.org/b/id/%d-M.jpg";

    private OpenLibraryClient() {
    }

    public static void search(Context context, String query, Callback callback) {
        // Uri.Builder encodes the query for us (spaces, &, # ...).
        String url = Uri.parse(SEARCH_URL).buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter("limit", "20")
                .appendQueryParameter("fields",
                        "key,title,author_name,first_publish_year,cover_i")
                .build().toString();

        JsonObjectRequest request = new JsonObjectRequest(url, null,
                response -> callback.onSuccess(parse(response)),
                error -> callback.onError(kindOf(error))) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // Open Library asks apps to identify themselves.
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "CampusMate/1.0 (student project)");
                return headers;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(10_000, 1, 1f));
        request.setTag(TAG);
        VolleySingleton.getInstance(context).add(request);
    }

    public static void cancel(Context context) {
        VolleySingleton.getInstance(context).cancelAll(TAG);
    }

    // opt*() methods return a default instead of throwing: one bad item must not break the list.
    static List<Book> parse(JSONObject root) {
        List<Book> books = new ArrayList<>();
        JSONArray docs = root.optJSONArray("docs");
        if (docs == null) {
            return books;
        }
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.optJSONObject(i);
            if (doc == null) {
                continue;
            }
            String title = doc.optString("title", "").trim();
            if (title.isEmpty()) {
                continue;
            }

            StringBuilder authors = new StringBuilder();
            JSONArray names = doc.optJSONArray("author_name");
            if (names != null) {
                for (int j = 0; j < names.length() && j < 3; j++) {
                    if (authors.length() > 0) {
                        authors.append(", ");
                    }
                    authors.append(names.optString(j));
                }
            }

            int coverId = doc.optInt("cover_i", 0);
            String cover = coverId > 0 ? String.format(java.util.Locale.US, COVER_URL, coverId)
                    : null;
            String key = doc.optString("key", "");
            String web = key.isEmpty() ? null : "https://openlibrary.org" + key;

            books.add(new Book(title, authors.toString(),
                    doc.optInt("first_publish_year", 0), cover, web));
        }
        return books;
    }

    private static int kindOf(VolleyError error) {
        if (error instanceof NoConnectionError || error instanceof NetworkError) {
            return ERROR_OFFLINE;
        }
        if (error instanceof TimeoutError) {
            return ERROR_TIMEOUT;
        }
        return ERROR_OTHER;
    }
}
