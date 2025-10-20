package com.example.spendwise.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.spendwise.data.ExpenseRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class ExpenseViewModel extends AndroidViewModel {
    private final MutableLiveData<String> addResult = new MutableLiveData<>();
    private final ExpenseRepository repository;

    public ExpenseViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ExpenseRepository();
    }

    public LiveData<String> getAddResult() {
        return addResult;
    }

    public void addExpense(String name, String amountText, String category, LocalDate date, String notes) {
        if (name == null || name.trim().isEmpty() || amountText == null || amountText.trim().isEmpty() || date == null) {
            addResult.setValue("Please enter valid data in the input fields");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException ex) {
            addResult.setValue("Amount must be a number");
            return;
        }

        Date expenseDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        repository.addExpense(name.trim(), amount, category == null ? "" : category.trim(), expenseDate, notes)
                .addOnSuccessListener(ref -> addResult.setValue("SUCCESS"))
                .addOnFailureListener(e -> addResult.setValue("Failed: " + e.getMessage()));
    }
}
