package com.example.flashcardapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    FirebaseAuth mAuth;

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Kullanıcı giriş yapmamış, LoginActivity'ye gönder
            sendUserToLoginActivity();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        setContentView(R.layout.activity_main);

        if (mAuth.getCurrentUser() != null) {
            setupBottomNavigation();
        } else {
            sendUserToLoginActivity();
        }
    }

    // Bottom Navigation kurulumunu yapan metot
    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(R.id.fragment_container) == null) {  // İlk açılışta HomeFragment yükle
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) { // Hangi iteme tıklandıysa ona göre fragment yükle
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_list) {
                    selectedFragment = new CardListFragment();
                } else if (itemId == R.id.nav_upload) {
                    selectedFragment = new UploadFragment();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                }
                return false;
            }
        });
    }

    // Fragment yükleme metodu
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    // Kullanıcıyı LoginActivity'ye gönderen metot
    private void sendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish(); // MainActivity'yi kapat
    }
}