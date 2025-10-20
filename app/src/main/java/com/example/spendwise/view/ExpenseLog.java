package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.spendwise.R;
import com.example.spendwise.databinding.ExpenselogBinding;

import com.example.spendwise.viewModel.ExpenseViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import com.example.spendwise.data.CurrentDateManager;

public class ExpenseLog extends AppCompatActivity {

    private ExpenseViewModel expenseViewModel;
    private ExpenselogBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using data binding to inflate the layout
        ExpenselogBinding binding = ExpenselogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel setup
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        binding.setLifecycleOwner(this);

        // Use findViewById for buttons
        View dashboardNavigate = findViewById(R.id.dashboard_navigate);
        View expenseLogNavigate = findViewById(R.id.expenseLog_navigate);
        View budgetNavigate = findViewById(R.id.budget_navigate);
        View savingCircleNavigate = findViewById(R.id.savingCircle_navigate);
        View chatbotNavigate = findViewById(R.id.chatbot_navigate);
        View expenseLogForm = findViewById(R.id.form_Container);
        View expenseLogMsg = findViewById(R.id.expenseLog_msg);

        // Inputs and helpers
        TextInputEditText expenseNameInput = findViewById(R.id.expenseNameInput);
        TextInputEditText amountInput = findViewById(R.id.amountInput);
        AutoCompleteTextView categoryInput = findViewById(R.id.categoryInput);
        TextInputEditText dateInput = findViewById(R.id.dateInput);
        TextInputEditText notesInput = findViewById(R.id.notesInput);

        // Category dropdown setup
        String[] categories = new String[]{"Food","Transport","Entertainment","Bills","Shopping","Health","Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        categoryInput.setAdapter(adapter);

        // Date field setup
        LocalDate current = CurrentDateManager.getCurrentDate(this);
        dateInput.setText(current.toString());
        dateInput.setOnClickListener(v2 -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select date")
                    .setSelection(CurrentDateManager.toStartOfDay(current).getTime())
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                LocalDate selected = Instant.ofEpochMilli(selection).atZone(ZoneId.systemDefault()).toLocalDate();
                dateInput.setText(selected.toString());
            });
            picker.show(getSupportFragmentManager(), "expenseDatePicker");
        });

        // Set click listeners using lambdas for the routing
        dashboardNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, Dashboard.class))
        );

        expenseLogNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, ExpenseLog.class))
        );

        budgetNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, Budget.class))
        );

        savingCircleNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, SavingCircle.class))
        );

        chatbotNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, Chatbot.class))
        );

        // Add Expense button - placeholder for future expense entry form
        View addExpenseButton = findViewById(R.id.add_expense_button);
        addExpenseButton.setOnClickListener(v -> {
            expenseLogForm.setVisibility(View.VISIBLE);
            expenseLogMsg.setVisibility(View.GONE);
        });

        View createExpenseBtn = findViewById(R.id.create_Expense);
        createExpenseBtn.setOnClickListener(v->{
            // Setup category dropdown choices if empty adapter
            if (categoryInput.getAdapter() == null) {
                categoryInput.setAdapter(adapter);
            }

            // Get the text from each field
            String name = expenseNameInput.getText().toString();
            String amount = amountInput.getText().toString();
            String category = categoryInput.getText().toString();
            String dateText = dateInput.getText().toString();
            String notes = notesInput.getText().toString();
            // Save expense via ViewModel
            java.time.LocalDate date = java.time.LocalDate.parse(dateText);
            expenseViewModel.addExpense(name, amount, category, date, notes);

            expenseViewModel.getAddResult().observe(this, result -> {
                if ("SUCCESS".equals(result)) {
                    android.widget.Toast.makeText(this, "Expense saved", android.widget.Toast.LENGTH_SHORT).show();
                } else if (result != null) {
                    android.widget.Toast.makeText(this, result, android.widget.Toast.LENGTH_LONG).show();
                }
            });

        });
    }
}

