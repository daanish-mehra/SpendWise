package com.example.spendwise.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.spendwise.R;
import com.example.spendwise.databinding.DashboardBinding;
import com.example.spendwise.viewModel.DashboardViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class Dashboard extends AppCompatActivity {

    private DashboardViewModel viewModel;
    private DashboardBinding binding;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.setLifecycleOwner(this);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupObservers();
        setupNavigation();
        setupCalendarSelector();
        setupLogoutButton();

        // Initial data load
        viewModel.refreshDashboard();
    }

    private void setupObservers() {
        // Observe current date
        viewModel.getCurrentDate().observe(this, date -> {
            TextView dateDisplay = findViewById(R.id.current_date_display);
            if (dateDisplay != null) {
                dateDisplay.setText("Current Date: " + date);
            }
        });

        // Observe total spent
        viewModel.getTotalSpent().observe(this, spent -> {
            TextView spentView = findViewById(R.id.total_spent);
            if (spentView != null) {
                spentView.setText(String.format("Total Spent: $%.2f", spent));
            }
        });

        // Observe total budget
        viewModel.getTotalBudget().observe(this, budget -> {
            TextView budgetView = findViewById(R.id.total_budget);
            if (budgetView != null) {
                budgetView.setText(String.format("Total Budget: $%.2f", budget));
            }
        });

        // Observe total remaining
        viewModel.getTotalRemaining().observe(this, remaining -> {
            TextView remainingView = findViewById(R.id.total_remaining);
            if (remainingView != null) {
                remainingView.setText(String.format("Remaining: $%.2f", remaining));
            }
        });

        // Observe category summaries
        viewModel.getCategorySummaries().observe(this, summaries -> {
            updateCategorySummaries(summaries);
        });

        // Observe status messages
        viewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategorySummaries(Map<String, DashboardViewModel.CategorySummary> summaries) {
        TextView categoriesView = findViewById(R.id.category_summaries);
        if (categoriesView != null && summaries != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("By Category:\n\n");
            
            for (DashboardViewModel.CategorySummary summary : summaries.values()) {
                sb.append(String.format("%s:\n", summary.category));
                sb.append(String.format("  Budgeted: $%.2f\n", summary.budgeted));
                sb.append(String.format("  Spent: $%.2f\n", summary.spent));
                sb.append(String.format("  Remaining: $%.2f\n\n", summary.remaining));
            }
            
            categoriesView.setText(sb.toString());
        }
    }

    private void setupCalendarSelector() {
        View calendarIcon = findViewById(R.id.calendar_selector);
        if (calendarIcon != null) {
            calendarIcon.setOnClickListener(v -> showDatePicker());
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        
        // Parse current date from ViewModel
        String currentDate = viewModel.getCurrentDate().getValue();
        if (currentDate != null) {
            try {
                calendar.setTime(dateFormat.parse(currentDate));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String newDate = dateFormat.format(calendar.getTime());
                    viewModel.setCurrentDate(newDate);
                    Toast.makeText(this, "Date set to: " + newDate, Toast.LENGTH_SHORT).show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.show();
    }

    private void setupLogoutButton() {
        View logoutButton = findViewById(R.id.logout_button);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                
                // Clear any cached data if needed
                // Navigate back to login
                Intent intent = new Intent(Dashboard.this, Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupNavigation() {
        View dashboardNavigate = findViewById(R.id.dashboard_navigate);
        View expenseLogNavigate = findViewById(R.id.expenseLog_navigate);
        View budgetNavigate = findViewById(R.id.budget_navigate);
        View savingCircleNavigate = findViewById(R.id.savingCircle_navigate);
        View chatbotNavigate = findViewById(R.id.chatbot_navigate);

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
}
