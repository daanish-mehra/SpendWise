package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.DashboardBinding;
import com.example.spendwise.data.CurrentDateManager;
import com.example.spendwise.data.ExpenseRepository;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

public class Dashboard extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using data binding to inflate the layout
        DashboardBinding binding = DashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Optional ViewModel setup
        // binding.setVariable(BR.viewModel, viewModel); //Use the right view model
        binding.setLifecycleOwner(this);

        // Use findViewById for buttons
        View dashboardNavigate = findViewById(R.id.dashboard_navigate);
        View expenseLogNavigate = findViewById(R.id.expenseLog_navigate);
        View budgetNavigate = findViewById(R.id.budget_navigate);
        View savingCircleNavigate = findViewById(R.id.savingCircle_navigate);
        View chatbotNavigate = findViewById(R.id.chatbot_navigate);

        // Dashboard controls
        TextView currentDateText = findViewById(R.id.current_date_text);
        View calendarButton = findViewById(R.id.calendar_button);
        TextView weeklyTotalText = findViewById(R.id.weekly_total_text);
        TextView monthlyTotalText = findViewById(R.id.monthly_total_text);
        TextView categoryTotalsText = findViewById(R.id.category_totals_text);
        View logoutButton = findViewById(R.id.logout_button);

        ExpenseRepository repository = new ExpenseRepository();
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.getDefault());

        // Initial date and totals
        LocalDate current = CurrentDateManager.getCurrentDate(this);
        currentDateText.setText("Current: " + CurrentDateManager.format(current));
        refreshTotals(repository, currency, current, weeklyTotalText, monthlyTotalText, categoryTotalsText);

        // Calendar date picker to set simulated current date
        calendarButton.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select current date")
                    .setSelection(CurrentDateManager.toStartOfDay(current).getTime())
                    .build();

            picker.addOnPositiveButtonClickListener(selection -> {
                LocalDate newDate = Instant.ofEpochMilli(selection)
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                CurrentDateManager.setCurrentDate(this, newDate);
                currentDateText.setText("Current: " + CurrentDateManager.format(newDate));
                refreshTotals(repository, currency, newDate, weeklyTotalText, monthlyTotalText, categoryTotalsText);
            });
            picker.show(getSupportFragmentManager(), "currentDatePicker");
        });

        // Logout
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(Dashboard.this, Login.class));
            finish();
        });

        // Set click listeners using lambdas
        dashboardNavigate.setOnClickListener(v ->
                startActivity(new Intent(Dashboard.this, Dashboard.class))
        );

        expenseLogNavigate.setOnClickListener(v ->
                startActivity(new Intent(Dashboard.this, ExpenseLog.class))
        );

        budgetNavigate.setOnClickListener(v ->
                startActivity(new Intent(Dashboard.this, Budget.class))
        );

        savingCircleNavigate.setOnClickListener(v ->
                startActivity(new Intent(Dashboard.this, SavingCircle.class))
        );

        chatbotNavigate.setOnClickListener(v ->
                startActivity(new Intent(Dashboard.this, Chatbot.class))
        );
    }

    private void refreshTotals(ExpenseRepository repository,
                               NumberFormat currency,
                               LocalDate current,
                               TextView weeklyTarget,
                               TextView monthlyTarget,
                               TextView categoryTarget) {
        Date weekStart = com.example.spendwise.data.CurrentDateManager.toStartOfDay(CurrentDateManager.startOfWeek(current));
        Date weekEnd = com.example.spendwise.data.CurrentDateManager.toEndOfDay(CurrentDateManager.endOfWeek(current));
        Date monthStart = com.example.spendwise.data.CurrentDateManager.toStartOfDay(CurrentDateManager.startOfMonth(current));
        Date monthEnd = com.example.spendwise.data.CurrentDateManager.toEndOfDay(CurrentDateManager.endOfMonth(current));

        repository.sumAmountBetween(weekStart, weekEnd).addOnSuccessListener(total -> {
            weeklyTarget.setText("This week: " + currency.format(total));
        });

        repository.sumAmountBetween(monthStart, monthEnd).addOnSuccessListener(total -> {
            monthlyTarget.setText("This month: " + currency.format(total));
        });

        repository.sumByCategoryBetween(monthStart, monthEnd).addOnSuccessListener(map -> {
            if (map == null || map.isEmpty()) {
                categoryTarget.setText("By category: none");
                return;
            }
            StringBuilder sb = new StringBuilder("By category: ");
            boolean first = true;
            for (java.util.Map.Entry<String, Double> e : map.entrySet()) {
                if (!first) sb.append("  •  ");
                sb.append(e.getKey()).append(": ").append(currency.format(e.getValue()));
                first = false;
            }
            categoryTarget.setText(sb.toString());
        });
    }
}

