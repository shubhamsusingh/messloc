package com.example.messloc.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messloc.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class PaymentResultActivity extends AppCompatActivity {

    public static final String EXTRA_SUCCESS = "payment_success";
    public static final String EXTRA_MESS_ID = "mess_id";
    public static final String EXTRA_MESS_NAME = "mess_name";
    public static final String EXTRA_PLAN_ID = "plan_id";
    public static final String EXTRA_PLAN_NAME = "plan_name";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_DURATION_DAYS = "duration_days";
    public static final String EXTRA_PAYMENT_METHOD = "payment_method";

    private boolean paymentSuccess;
    private String messName;
    private String planName;
    private double amount;
    private String paymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_result);

        readResultDetails();
        displayResult();
        setupButtons();
    }

    private void readResultDetails() {
        Intent intent = getIntent();

        paymentSuccess = intent.getBooleanExtra(EXTRA_SUCCESS, false);
        messName = intent.getStringExtra(EXTRA_MESS_NAME);
        planName = intent.getStringExtra(EXTRA_PLAN_NAME);
        amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0);
        paymentMethod = intent.getStringExtra(EXTRA_PAYMENT_METHOD);
    }

    private void displayResult() {
        TextView tvResultTitle = findViewById(R.id.tvResultTitle);
        TextView tvResultMessage = findViewById(R.id.tvResultMessage);
        TextView tvResultAmount = findViewById(R.id.tvResultAmount);
        TextView tvTransactionId = findViewById(R.id.tvTransactionId);
        TextView tvResultMessName = findViewById(R.id.tvResultMessName);

        if (paymentSuccess) {
            tvResultTitle.setText("Payment Successful!");
            tvResultMessage.setText(
                    "Your demo payment was completed successfully."
            );

            String transactionId = "DEMO-" +
                    UUID.randomUUID().toString()
                            .substring(0, 8)
                            .toUpperCase(Locale.ENGLISH);

            tvTransactionId.setText("Transaction ID: " + transactionId);
        } else {
            tvResultTitle.setText("Payment Failed");
            tvResultMessage.setText(
                    "Your simulated payment failed. No money was charged."
            );

            tvTransactionId.setText("Transaction ID: Not generated");
        }

        tvResultAmount.setText(
                String.format(Locale.ENGLISH, "₹%.2f", amount)
        );

        tvResultMessName.setText(
                (messName == null ? "" : messName)
                        + " • "
                        + (planName == null ? "" : planName)
        );
    }

    private void setupButtons() {
        MaterialButton btnDone = findViewById(R.id.btnResultDone);
        MaterialButton btnRetry = findViewById(R.id.btnRetryPayment);

        btnRetry.setVisibility(paymentSuccess
                ? android.view.View.GONE
                : android.view.View.VISIBLE);

        btnRetry.setOnClickListener(v -> finish());

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.messloc.ui.main.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}