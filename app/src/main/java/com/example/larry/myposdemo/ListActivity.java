package com.example.larry.myposdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;

import com.example.larry.myposdemo.utils.CustomAdapter;
import com.example.larry.myposdemo.utils.POSHttpConnector;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ListActivity extends AppCompatActivity {

    private Boolean isRental = true;
    private View mProgressView;
    private ListView mListView;

    private Button mRental;
    private Button mInvoice;
    private SearchView mSearch;

    private Handler handler;

    private RadioGroup group;

    private final static String pageCriteria = "&page-limit=20&sort-by=created_at&sort-order=desc&page=";

    private static int PAGE_NO = 1;

    private RadioButton[] radios = new RadioButton[3];

    private Map<Integer, String> filters = new HashMap<>();

    private static CustomAdapter adapter;

    private int checkedId;

    private boolean loading;

    private boolean isLastRow = false;
    private boolean isFirstRow = true;
    private boolean isForward = true;
    private boolean preparePage = false;
    private float startY = 0;
    private String filterValue = "";
    private String tmpFilterValue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mProgressView = (View) findViewById(R.id.listProgressBar);
        mListView = (ListView) findViewById(R.id.list);

        mRental = (Button) findViewById(R.id.rentals);
        mInvoice = (Button) findViewById(R.id.invoices);
        mSearch = (SearchView) findViewById(R.id.search_list);

        mRental.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRental = true;
                mRental.setEnabled(!isRental);
                mInvoice.setEnabled(isRental);
                setStatusFilters4Rental();
            }
        });

        mInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRental = false;
                mRental.setEnabled(!isRental);
                mInvoice.setEnabled(isRental);
                setStatusFilters4Invoice();
            }
        });

        mSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(!query.equals(filterValue)) {
                    filterValue = query;
                    PAGE_NO = 1;
                    if (isRental) {
                        getRentalList();
                    } else {
                        getInvoiceList();
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                tmpFilterValue = newText;
                return false;
            }
        });

        mSearch.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                if(tmpFilterValue.isEmpty() && !filterValue.isEmpty()) {
                    filterValue = "";
                    PAGE_NO = 1;
                    if (isRental) {
                        getRentalList();
                    } else {
                        getInvoiceList();
                    }
                }
                return false;
            }
        });

        group = (RadioGroup) findViewById(R.id.filters);

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int id) {
                if(loading) return;
                if(checkedId != id) {
                    PAGE_NO = 1;
                    if(isRental){
                        getRentalList();
                    } else {
                        getInvoiceList();
                    }
                }
            }
        });

        handler = new Handler();

        init();

    }

    private void refreshList(List<JsonElement> entries) {
        adapter= new CustomAdapter(entries,getApplicationContext());
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // if (pageNo > 1 && isFirstRow && scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING)
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount > 0) {
                    //scroll to the last row
                    isLastRow = true;
                } else {
                    isLastRow = false;
                }

                if(firstVisibleItem == 0) {
                    isFirstRow = true;
                } else {
                    isFirstRow = false;
                }
            }
        });

        mListView.setOnTouchListener(new View.OnTouchListener() {
            private int mTouchSlop = ViewConfiguration.get(getApplicationContext()).getScaledTouchSlop();
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(startY == 0) {
                            startY = event.getY();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getY() - startY) > mTouchSlop) {
                            if (event.getY() - startY >= 0) {
                                if(isForward) preparePage = false;
                                if(!isForward && isFirstRow && PAGE_NO > 1){
                                    if(preparePage){
                                        PAGE_NO--;
                                        if(isRental){
                                            getRentalList();
                                        } else {
                                            getInvoiceList();
                                        }
                                        preparePage = false;
                                    } else {
                                        preparePage = true;
                                    }
                                }
                                isForward = false;
                            } else {
                                if(!isForward) preparePage = false;
                                if(isForward && isLastRow) {
                                    if(preparePage){
                                        PAGE_NO++;
                                        if(isRental){
                                            getRentalList();
                                        } else {
                                            getInvoiceList();
                                        }
                                        preparePage = false;
                                    } else {
                                        preparePage = true;
                                    }
                                }
                                isForward = true;
                            }
                        }
                        startY = 0;
                        break;
                }
                return false;
            }
        });
    }

    private String generateSearchFilter(){
        StringBuilder sb = new StringBuilder();
        if(filterValue.startsWith("G201")){
            sb.append("&bid=").append(filterValue);
        } else if (Pattern.matches("^(1)\\d{10}$", filterValue)){
            sb.append("$mobile=").append(filterValue);
        }
        return sb.toString();
    }

    private void getRentalListFail(){
        //mListView
        showListProgress(false);
        group.check(checkedId);
    }

    private void getRentalListSuccess(String body){
        //mListView
        JsonObject obj = new JsonParser().parse(body).getAsJsonObject();
        List<JsonElement> entries = copyIterator(obj.getAsJsonArray("entries").iterator());

        refreshList(entries);
        checkedId = group.getCheckedRadioButtonId();

        showListProgress(false);
    }

    public void getRentalList(){

        showListProgress(true);

        POSHttpConnector.getInstance().get(getString(R.string.api_get_rental)+"="+filters.get(group.getCheckedRadioButtonId())+pageCriteria+PAGE_NO+generateSearchFilter(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getRentalListFail();
                        loading = false;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String body = response.body().string();
                if(response.isSuccessful()){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getRentalListSuccess(body);
                            loading = false;
                        }
                    });
                } else {
                    this.onFailure(call, null);
                }
            }
        });
    }

    private void getInvoiceListFail(){
        //mListView
        showListProgress(false);
        group.check(checkedId);
    }

    private void getInvoiceListSuccess(String body){
        //mListView
        JsonObject obj = new JsonParser().parse(body).getAsJsonObject();
        List<JsonElement> entries = copyIterator(obj.getAsJsonArray("entries").iterator());

        refreshList(entries);
        checkedId = group.getCheckedRadioButtonId();

        showListProgress(false);
    }

    public void getInvoiceList(){

        showListProgress(true);

        POSHttpConnector.getInstance().get(getString(R.string.api_get_invoice)+"="+filters.get(group.getCheckedRadioButtonId())+pageCriteria+PAGE_NO+generateSearchFilter(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getInvoiceListFail();
                        loading = false;
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String body = response.body().string();
                if(response.isSuccessful()){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getInvoiceListSuccess(body);
                            loading = false;
                        }
                    });
                } else {
                    this.onFailure(call, null);
                }
            }
        });
    }

    private void setStatusFilters4Rental(){

        loading = true;

        filterValue = "";

        PAGE_NO = 1;

        checkedId = radios[0].getId();

        radios[0].setText("未预授权");
        radios[0].setVisibility(View.VISIBLE);
        filters.put(radios[0].getId(), "UNPAID");

        radios[1].setText("未支付");
        radios[1].setVisibility(View.VISIBLE);
        filters.put(radios[1].getId(), "UNPAID");

        radios[2].setText("已支付");
        radios[2].setVisibility(View.VISIBLE);
        filters.put(radios[2].getId(), "PAID");

        group.check(radios[0].getId());

        getRentalList();

    }

    private void setStatusFilters4Invoice(){

        loading = true;

        PAGE_NO = 1;

        checkedId = radios[0].getId();

        radios[0].setText("未支付");
        radios[0].setVisibility(View.VISIBLE);
        filters.put(radios[0].getId(), "PENDING");

        radios[1].setText("已支付");
        radios[1].setVisibility(View.VISIBLE);
        filters.put(radios[1].getId(), "PAID");

        radios[2].setVisibility(View.GONE);

        group.check(radios[0].getId());

        getInvoiceList();
    }

    private void init(){
        //showListProgress(true);
        mRental.setEnabled(!isRental);
        mInvoice.setEnabled(isRental);

        radios[0] = (RadioButton) findViewById(R.id.filter0);
        radios[1] = (RadioButton) findViewById(R.id.filter1);
        radios[2] = (RadioButton) findViewById(R.id.filter2);

        setStatusFilters4Rental();
        getRentalList();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showListProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
            mListView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mListView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public static <T> List<T> copyIterator(Iterator<T> iter) {
        List<T> copy = new ArrayList<T>();
        while (iter.hasNext())
            copy.add(iter.next());
        return copy;
    }

}
