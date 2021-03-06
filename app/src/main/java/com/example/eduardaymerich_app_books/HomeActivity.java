package com.example.eduardaymerich_app_books;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.example.eduardaymerich_app_books.adapters.BookAdapter;
import com.example.eduardaymerich_app_books.db.MySQLiteHelper;
import com.example.eduardaymerich_app_books.models.Book;
import com.example.eduardaymerich_app_books.models.BookClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.Headers;

// To be able to do the call to get all to the book search API
public class HomeActivity extends AppCompatActivity {
    public static final String BOOK_DETAIL_KEY = "book";

    private ListView lvBooks;
    private BookAdapter bookAdapter;
    private BookClient client;
    private ArrayList<Book> books;
    private ProgressBar progress;
    private MySQLiteHelper databaseHelper;
    private String usernameCurrentUser;
    private Integer countBooks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        lvBooks = (ListView) findViewById(R.id.lvBooks);
        ArrayList<Book> aBooks = new ArrayList<Book>();
        bookAdapter = new BookAdapter(this, aBooks);
        lvBooks.setAdapter(bookAdapter);
        setupBookSelectedListener();
        progress = (ProgressBar) findViewById(R.id.progress);
        // load database;
        databaseHelper = new MySQLiteHelper(this);

        // get current user
        SharedPreferences settings = getSharedPreferences("UserInfo", 0);
        usernameCurrentUser = settings.getString("username", "").toString();

        // display username in title
        final TextView title = (TextView) findViewById(R.id.tvTitleSavedBooks);
        countBooks = databaseHelper.countBooksFromUser(usernameCurrentUser);
        title.setText("Hi " + usernameCurrentUser + "! Your books (" + countBooks + ")");

        // get books from the user
        fetchBooksFromUser();

        // Set title
        HomeActivity.this.setTitle("GaboApp");
    }

    private void fetchBooksFromUser() {
        client = new BookClient();

        if( countBooks > 0 ) {
            // remove info no books
            final TextView info = (TextView) findViewById(R.id.tvInfoSavedBooks);
            info.setVisibility(View.GONE);

            // get books from the user
            ArrayList<String> idsBooksFromUser = databaseHelper.getBooksFromUser(usernameCurrentUser);

            // Empty adapater
            bookAdapter.clear();

            // get Books info
            fetchBooksById(idsBooksFromUser);

            // notify changes in adapter
            bookAdapter.notifyDataSetChanged();

            // Hide progress bar
            progress.setVisibility(ProgressBar.GONE);
        }
    }

    private void fetchBooksById(ArrayList<String> ids) {
        // Empty adapater
        bookAdapter.clear();

        // Show progress bar before any request
        progress.setVisibility(ProgressBar.VISIBLE);

        client = new BookClient();

        // for each book from the user
        for (String id : ids) {
            client.getBooks(id, new JsonHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Headers headers, JSON json) {
                    try {
                        JSONArray docs = null;
                        JSONObject response = json.jsonObject;

                        if(response != null) {
                            docs = response.getJSONArray("docs");

                            // Lista temporal
                            final ArrayList<Book> books = Book.fromJson(docs);

                            // Insert books en adapter
                            for (Book book : books) {
                                bookAdapter.add(book);
                            }
                        }

                    } catch (JSONException e) {
                        // Invalid JSON format, show appropriate error.
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                }
            });
        }

        // notify changes in adapter
        bookAdapter.notifyDataSetChanged();

        // Hide progress bar
        progress.setVisibility(ProgressBar.GONE);
    }

    // API call to the OpenLibrary
    private void fetchBooks(String query) {
        client = new BookClient();

        // Empty adapater
        bookAdapter.clear();

        // Show progress bar before any request
        progress.setVisibility(ProgressBar.VISIBLE);

        client.getBooks(query, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                try {
                    JSONArray docs = null;
                    JSONObject response = json.jsonObject;

                    if(response != null) {

                        docs = response.getJSONArray("docs");

                        // Lista temporal
                        final ArrayList<Book> books = Book.fromJson(docs);

                        // Insert books en adapter
                        for (Book book : books) {
                            bookAdapter.add(book);
                        }

                        bookAdapter.notifyDataSetChanged();
                    }
                    // Hide progress bar
                    progress.setVisibility(ProgressBar.GONE);

                } catch (JSONException e) {
                    // Invalid JSON format, show appropriate error.
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                // Hide progress bar
                progress.setVisibility(ProgressBar.GONE);
            }
        });
    }

    // Insertar menu en top
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // hide list saved books
                final TextView title = (TextView) findViewById(R.id.tvTitleSavedBooks);
                title.setVisibility(View.GONE);
                final TextView info = (TextView) findViewById(R.id.tvInfoSavedBooks);
                info.setVisibility(View.GONE);

                // get data from search
                fetchBooks(query);

                // Reset SearchView
                searchView.clearFocus();
                searchView.setQuery("", false);
                searchView.setIconified(true);
                searchItem.collapseActionView();

                // Set activity title to search query
                HomeActivity.this.setTitle(query);

                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        return true;
    }

    // Abrir book details on clicj
    public void setupBookSelectedListener() {
        lvBooks.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(HomeActivity.this, BookDetailsActivity.class);
            intent.putExtra(BOOK_DETAIL_KEY, bookAdapter.getItem(position));
            startActivityForResult(intent, 0);
        });
    }
    
}