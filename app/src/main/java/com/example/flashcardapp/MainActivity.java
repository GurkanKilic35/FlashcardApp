package com.example.flashcardapp; // Kendi paket adınızı kullanın

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
    FirebaseAuth mAuth; // FirebaseAuth instance'ı ekle

    @Override
    protected void onStart() {
        super.onStart();
        // Kullanıcının giriş yapıp yapmadığını kontrol et
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Kullanıcı giriş yapmamış, LoginActivity'ye gönder
            sendUserToLoginActivity();
        }
        // Kullanıcı giriş yapmışsa, MainActivity normal şekilde devam eder.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance(); // onCreate içinde initialize et

        // Kullanıcı kontrolünü onStart'a taşıdık, ancak burada tekrar kontrol etmek
        // veya doğrudan onStart'taki yönlendirmeye güvenmek de bir seçenek.
        // Şimdilik onStart yeterli.

        setContentView(R.layout.activity_main); // Layout'u auth kontrolünden sonra yükle

        // Giriş yapılmışsa arayüzü kur
        if (mAuth.getCurrentUser() != null) {
            setupBottomNavigation(); // Navigasyon kurulumunu ayrı bir metoda taşıyalım
        } else {
            // Eğer bir şekilde buraya gelinirse (normalde onStart yönlendirir) tekrar Login'e gönder
            sendUserToLoginActivity();
        }
    }

    // Bottom Navigation kurulumunu yapan metot
    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Başlangıçta HomeFragment'ı yükle (Sadece ilk oluşturulduğunda)
        // saveInstanceState kontrolü burada önemli, ekran döndüğünde tekrar yüklemesin diye.
        // Ancak onStart'ta zaten kontrol olduğu için, buraya giriş yapmış kullanıcı kesin gelir.
        // Yine de Fragment'ın tekrar tekrar yüklenmesini önlemek için kontrol iyi olur.
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(R.id.fragment_container) == null) {
            loadFragment(new HomeFragment());
        }


        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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

    // Fragment yükleme metodu (değişiklik yok)
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