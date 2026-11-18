package com.example.messloc.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messloc.R;
import com.example.messloc.data.model.payment.PaymentMethod;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class DemoPaymentActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "mess_id";
    public static final String EXTRA_MESS_NAME = "mess_name";
    public static final String EXTRA_PLAN_ID = "plan_id";
    public static final String EXTRA_PLAN_NAME = "plan_name";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_DURATION_DAYS = "duration_days";
    public static final String EXTRA_PAYMENT_METHOD = "payment_method";

    private String messId;
    private String messName;
    private String planId;
    private String planName;
    private double amount;
    private int durationDays;
    private PaymentMethod paymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_payment);

        if (!readPaymentDetails()) {
            finish();
            return;
        }

        setupToolbar();
        displayPaymentDetails();
        setupPaymentButtons();
    }

    private boolean readPaymentDetails() {
        Intent intent = getIntent();

        messId = intent.getStringExtra(EXTRA_MESS_ID);
        messName = intent.getStringExtra(EXTRA_MESS_NAME);
        planId = intent.getStringExtra(EXTRA_PLAN_ID);
        planName = intent.getStringExtra(EXTRA_PLAN_NAME);
        amount = intent.getDoubleExtra(EXTRA_AMOUNT, -1);
        durationDays = intent.getIntExtra(EXTRA_DURATION_DAYS, -1);

        String method = intent.getStringExtra(EXTRA_PAYMENT_METHOD);

        if (method == null) {
            return false;
        }

        try {
            paymentMethod = PaymentMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return messId != null && !messId.trim().isEmpty()
                && messName != null && !messName.trim().isEmpty()
                && planId != null && !planId.trim().isEmpty()
                && planName != null && !planName.trim().isEmpty()
                && amount >= 0
                && durationDays > 0;
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void displayPaymentDetails() {
        TextView tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        TextView tvPaymentAmount = findViewById(R.id.tvPaymentAmount);

        String methodName;

        switch (paymentMethod) {
            case UPI:
                methodName = "UPI Payment";
                break;

            case CARD:
                methodName = "Card Payment";
                break;

            case NET_BANKING:
                methodName = "Net Banking";
                break;

            default:
                methodName = "Demo Payment";
                break;
        }

        tvPaymentMethod.setText(methodName);
        tvPaymentAmount.setText(
                String.format(Locale.ENGLISH, "₹%.2f", amount)
        );
    }

    private void setupPaymentButtons() {
        MaterialButton btnSuccess = findViewById(R.id.btnSimulateSuccess);
        MaterialButton btnFailure = findViewById(R.id.btnSimulateFailure);

        btnSuccess.setOnClickListener(v -> openResultScreen(true));
        btnFailure.setOnClickListener(v -> openResultScreen(false));
    }

    private void openResultScreen(boolean success) {
        Intent intent = new Intent(this, PaymentResultActivity.class);

        intent.putExtra(PaymentResultActivity.EXTRA_SUCCESS, success);
        intent.putExtra(PaymentResultActivity.EXTRA_MESS_ID, messId);
        intent.putExtra(PaymentResultActivity.EXTRA_MESS_NAME, messName);
        intent.putExtra(PaymentResultActivity.EXTRA_PLAN_ID, planId);
        intent.putExtra(PaymentResultActivity.EXTRA_PLAN_NAME, planName);
        intent.putExtra(PaymentResultActivity.EXTRA_AMOUNT, amount);
        intent.putExtra(PaymentResultActivity.EXTRA_DURATION_DAYS, durationDays);
        intent.putExtra(PaymentResultActivity.EXTRA_PAYMENT_METHOD, paymentMethod.name());

        startActivity(intent);
    }
}