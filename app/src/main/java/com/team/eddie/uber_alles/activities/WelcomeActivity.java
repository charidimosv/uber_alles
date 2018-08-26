package com.team.eddie.uber_alles.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.team.eddie.uber_alles.R;
import com.team.eddie.uber_alles.utils.LoginServices;
import com.team.eddie.uber_alles.utils.RetrofitClient;
import com.team.eddie.uber_alles.utils.SaveSharedPreference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class WelcomeActivity extends AppCompatActivity {

    Button logoutBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        logoutBT = findViewById(R.id.logoutBT);

        logoutBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set LoggedIn status to false
                SaveSharedPreference.setLoggedIn(getApplicationContext(), null);

                // Logout
                logout();
            }
        });
    }

    /**
     * Logout
     * TODO: Please modify according to your need it is just an example
     */
    public void logout() {
        Retrofit retrofit = RetrofitClient.getClient();
        LoginServices loginServices = retrofit.create(LoginServices.class);
        Call<Void> logout = loginServices.logout();

        logout.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 200) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("TAG", "=======onFailure: " + t.toString());
                t.printStackTrace();
            }
        });
    }
}
