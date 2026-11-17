package com.example.messloc.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messloc.R;
import com.example.messloc.data.model.payment.PaymentMethod;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import android.widget.TextView;

import java.util.Locale;

public class PaymentCheckoutActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "mess_id";
    public static final String EXTRA_MESS_NAME = "mess_name";
    public static final String EXTRA_PLAN_ID = "plan_id";
    public static final String EXTRA_PLAN_NAME = "plan_name";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_DURATION_DAYS = "duration_days";

    private PaymentMethod selectedPaymentMethod = PaymentMethod.UPI;

    private String messId;
    private String messName;
    private String planId;
    private String planName;
    private double amount;
    private int durationDays;

    private RadioButton radioUpi;
    private RadioButton radioCard;
    private RadioButton radioNetBanking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_checkout);

        if (!readCheckoutDetails()) {
            Toast.makeText(this, "Checkout details are missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        displayCheckoutDetails();
        setupToolbar();
        setupPaymentMethods();
        setupPayButton();
    }

    private boolean readCheckoutDetails() {
        if (getIntent() == null) {
            return false;
        }

        messId = getIntent().getStringExtra(EXTRA_MESS_ID);
        messName = getIntent().getStringExtra(EXTRA_MESS_NAME);
        planId = getIntent().getStringExtra(EXTRA_PLAN_ID);
        planName = getIntent().getStringExtra(EXTRA_PLAN_NAME);
        amount = getIntent().getDoubleExtra(EXTRA_AMOUNT, -1);
        durationDays = getIntent().getIntExtra(EXTRA_DURATION_DAYS, -1);

        return messId != null && !messId.trim().isEmpty()
                && messName != null && !messName.trim().isEmpty()
                && planId != null && !planId.trim().isEmpty()
                && planName != null && !planName.trim().isEmpty()
                && amount >= 0
                && durationDays > 0;
    }

    private void initializeViews() {
        radioUpi = findViewById(R.id.radioUpi);
        radioCard = findViewById(R.id.radioCard);
        radioNetBanking = findViewById(R.id.radioNetBanking);
    }

    private void displayCheckoutDetails() {
        TextView tvMessName = findViewById(R.id.tvMessName);
        TextView tvPlanName = findViewById(R.id.tvPlanName);
        TextView tvDuration = findViewById(R.id.tvDuration);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);

        tvMessName.setText(messName);
        tvPlanName.setText(planName);
        tvDuration.setText(durationDays + " days");
        tvTotalAmount.setText(String.format(Locale.ENGLISH, "₹%.2f", amount));
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupPaymentMethods() {
        radioUpi.setOnClickListener(v -> selectPaymentMethod(PaymentMethod.UPI));
        radioCard.setOnClickListener(v -> selectPaymentMethod(PaymentMethod.CARD));
        radioNetBanking.setOnClickListener(v -> selectPaymentMethod(PaymentMethod.NET_BANKING));
    }

    private void selectPaymentMethod(PaymentMethod method) {
        selectedPaymentMethod = method;

        radioUpi.setChecked(method == PaymentMethod.UPI);
        radioCard.setChecked(method == PaymentMethod.CARD);
        radioNetBanking.setChecked(method == PaymentMethod.NET_BANKING);
    }

    private void setupPayButton() {
        MaterialButton btnPayNow = findViewById(R.id.btnPayNow);

        btnPayNow.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PaymentCheckoutActivity.this,
                    DemoPaymentActivity.class
            );

            intent.putExtra(
                    DemoPaymentActivity.EXTRA_MESS_ID,
                    messId
            );

            intent.putExtra(
                    DemoPaymentActivity.EXTRA_MESS_NAME,
                    messName
            );

            intent.putExtra(
                    DemoPaymentActivity.EXTRA_PLAN_ID,
                    planId
            );

            intent.putExtra(
                    DemoPaymentActivity.EXTRA_PLAN_NAME,
                    planName
            );

            intent.putExtra(
                    DemoPaymentActivity.EXTRA_AMOUNT,
                    amount
            );

            intent.putExtra(
                    DemoPaymentActivity.EXTRA_DURATION_DAYS,
                    durationDays
            );

            intent.putExtra(
                    DemoPaymentActivity.EXTRA_PAYMENT_METHOD,
                    selectedPaymentMethod.name()
            );

            startActivity(intent);
        });
    }
}