package com.openshamba.dagger;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.openshamba.dagger.app.App;
import com.openshamba.dagger.remote.GitHubClient;
import com.openshamba.dagger.remote.models.GitHubRepo;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {

    @Inject
    SharedPreferences preferences;
    @Inject @Named("non_cached")
    Retrofit retrofit;
    @Inject
    Call<List<GitHubRepo>> gitHub;

    TextView tv;
    ListView lv;
    StringBuilder sb = new StringBuilder();
    String[] repoArray;
    List<String> mStrings = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((App) getApplication()).getNetComponent().inject(this);

        setUp();
        getRepos();


    }

    public void setUp() {
        tv = findViewById(R.id.point);
        lv = findViewById(R.id.repo_list);
    }

    public void getRepos() {
        tv.setText("Getting github repos for NimzyMaina");
        gitHub.enqueue(new Callback<List<GitHubRepo>>() {
            @Override
            public void onResponse(Call<List<GitHubRepo>> call, Response<List<GitHubRepo>> response) {
                if(response.isSuccessful()){
                    for (GitHubRepo rep: response.body()) {
                        mStrings.add(rep.getName());
                    }
                    ArrayAdapter adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.item_view, mStrings);

                    tv.setVisibility(View.INVISIBLE);

                    lv.setAdapter(adapter);

                }else{
                    tv.setText("Failed to get response");
                }
            }

            @Override
            public void onFailure(Call<List<GitHubRepo>> call, Throwable t) {
                tv.setText("Check Internet connection");
            }
        });
    }
}
