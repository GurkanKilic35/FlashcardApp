package com.example.flashcardapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

public class CardFragment extends Fragment {

    private static final String ARG_FRONT_TEXT = "front_text";
    private static final String ARG_BACK_TEXT = "back_text";
    private static final String TAG = "CardFragment";

    private String frontText;
    private String backText;
    private boolean isFrontShowing = true;
    private boolean isAnimating = false;

    private CardView cardViewContainer;
    private TextView textViewCardContent;

    public CardFragment() { } // Boş constructor

    // newInstance metodu ile fragment oluşturma (güvenli argüman geçişi)
    public static CardFragment newInstance(String front, String back) {
        CardFragment fragment = new CardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FRONT_TEXT, front);
        args.putString(ARG_BACK_TEXT, back);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            frontText = getArguments().getString(ARG_FRONT_TEXT);
            backText = getArguments().getString(ARG_BACK_TEXT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_card, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardViewContainer = view.findViewById(R.id.cardViewFragmentContainer);
        textViewCardContent = view.findViewById(R.id.textViewFragmentCardContent);

        // Başlangıç metnini ayarla
        if (frontText != null) {
            textViewCardContent.setText(frontText);
            isFrontShowing = true; // Her zaman ön yüzle başla
        } else {
            textViewCardContent.setText(""); // Veri yoksa boş göster
        }

        // Çevirme animasyonu için tıklama listener'ı
        cardViewContainer.setOnClickListener(v -> {
            if (!isAnimating && frontText != null && backText != null) {
                flipCardAnimation();
            }
        });
    }

    // Çevirme animasyonu (ListDetailActivity'den buraya taşındı)
    private void flipCardAnimation() {
        if (getContext() == null) return; // Context kontrolü

        float scale = getResources().getDisplayMetrics().density;
        cardViewContainer.setCameraDistance(8000 * scale);

        ObjectAnimator animator1 = ObjectAnimator.ofFloat(cardViewContainer, "rotationY", 0f, 90f);
        animator1.setInterpolator(new AccelerateDecelerateInterpolator());
        animator1.setDuration(250);

        ObjectAnimator animator2 = ObjectAnimator.ofFloat(cardViewContainer, "rotationY", -90f, 0f);
        animator2.setInterpolator(new DecelerateInterpolator());
        animator2.setDuration(250);

        animator1.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) { isAnimating = true; }
            @Override public void onAnimationEnd(Animator animation) {
                if (isFrontShowing) {
                    textViewCardContent.setText(backText);
                } else {
                    textViewCardContent.setText(frontText);
                }
                isFrontShowing = !isFrontShowing;
                cardViewContainer.setRotationY(-90f);
                animator2.start();
            }
        });

        animator2.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) { isAnimating = true; }
            @Override public void onAnimationEnd(Animator animation) { isAnimating = false; }
        });

        animator1.start();
    }

    // Fragment görünümden kaybolduğunda animasyon veya durumu sıfırlama (opsiyonel)
    @Override
    public void onPause() {
        super.onPause();
        if (isAnimating && cardViewContainer != null) {
            cardViewContainer.clearAnimation(); // Varsa animasyonu durdur
            cardViewContainer.setRotationY(0f); // Rotasyonu sıfırla
            isAnimating = false;
        }
        // Fragment tekrar görünür olduğunda ön yüzün gösterilmesini sağla
        if (!isFrontShowing && frontText != null && textViewCardContent != null) {
            textViewCardContent.setText(frontText);
            isFrontShowing = true;
        }
    }
}