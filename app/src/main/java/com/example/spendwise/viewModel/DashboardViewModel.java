package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Expense;
import com.example.spendwise.repo.BudgetRepo;
import com.example.spendwise.repo.ExpenseRepo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ViewModel for Dashboard
 * Handles aggregating expense and budget data for display
 */
public class DashboardViewModel extends ViewModel {

    private final BudgetRepo budgetRepo = BudgetRepo.getInstance();
    private final ExpenseRepo expenseRepo = ExpenseRepo.getInstance();
    
    private final MutableLiveData<String> currentDate = new MutableLiveData<>();
    private final MutableLiveData<Double> totalSpent = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalBudget = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalRemaining = new MutableLiveData<>(0.0);
    private final MutableLiveData<Map<String, CategorySummary>> categorySummaries = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public DashboardViewModel() {
        // Set current date as default
        currentDate.setValue(dateFormat.format(new Date()));
    }

    public LiveData<String> getCurrentDate() {
        return currentDate;
    }

    public LiveData<Double> getTotalSpent() {
        return totalSpent;
    }

    public LiveData<Double> getTotalBudget() {
        return totalBudget;
    }

    public LiveData<Double> getTotalRemaining() {
        return totalRemaining;
    }

    public LiveData<Map<String, CategorySummary>> getCategorySummaries() {
        return categorySummaries;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    /**
     * Set the current date for testing purposes
     */
    public void setCurrentDate(String date) {
        currentDate.setValue(date);
        refreshDashboard();
    }

    /**
     * Refresh all dashboard data
     */
    public void refreshDashboard() {
        // First seed data if needed, then load everything
        budgetRepo.seedIfEmpty(new BudgetRepo.RepoCallback() {
            @Override
            public void onSuccess() {
                expenseRepo.seedIfEmpty(new ExpenseRepo.RepoCallback() {
                    @Override
                    public void onSuccess() {
                        loadDashboardData();
                    }

                    @Override
                    public void onError(String error) {
                        loadDashboardData();
                    }
                });
            }

            @Override
            public void onError(String error) {
                loadDashboardData();
            }
        });
    }

    /**
     * Load and compute all dashboard data
     */
    private void loadDashboardData() {
        // Load budgets
        budgetRepo.fetchBudgets(new BudgetRepo.BudgetsCallback() {
            @Override
            public void onSuccess(List<Budget> budgets) {
                // Load expenses
                expenseRepo.fetchExpenses(new ExpenseRepo.ExpensesCallback() {
                    @Override
                    public void onSuccess(List<Expense> expenses) {
                        computeDashboardData(budgets, expenses);
                    }

                    @Override
                    public void onError(String error) {
                        statusMessage.postValue("Error loading expenses: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                statusMessage.postValue("Error loading budgets: " + error);
            }
        });
    }

    /**
     * Compute aggregate totals and category summaries
     */
    private void computeDashboardData(List<Budget> budgets, List<Expense> expenses) {
        String currentDateStr = currentDate.getValue();
        if (currentDateStr == null) {
            currentDateStr = dateFormat.format(new Date());
        }

        // Filter budgets and expenses based on current date
        List<Budget> activeBudgets = filterActiveBudgets(budgets, currentDateStr);
        List<Expense> relevantExpenses = filterRelevantExpenses(expenses, activeBudgets, currentDateStr);

        // Calculate totals
        double spent = 0.0;
        double budget = 0.0;

        for (Expense expense : relevantExpenses) {
            spent += expense.getAmount();
        }

        for (Budget b : activeBudgets) {
            budget += b.getAmount();
        }

        double remaining = budget - spent;

        // Update LiveData
        totalSpent.postValue(spent);
        totalBudget.postValue(budget);
        totalRemaining.postValue(remaining);

        // Compute category summaries
        Map<String, CategorySummary> summaries = computeCategorySummaries(activeBudgets, relevantExpenses);
        categorySummaries.postValue(summaries);
    }

    /**
     * Filter budgets that are active for the current date
     */
    private List<Budget> filterActiveBudgets(List<Budget> budgets, String currentDateStr) {
        List<Budget> activeBudgets = new ArrayList<>();
        
        try {
            Date currentDate = dateFormat.parse(currentDateStr);
            Calendar currentCal = Calendar.getInstance();
            currentCal.setTime(currentDate);

            for (Budget budget : budgets) {
                if (budget.getStartDate() == null || budget.getFrequency() == null) {
                    continue;
                }

                Date startDate = dateFormat.parse(budget.getStartDate());
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(startDate);

                boolean isActive = false;

                if ("Weekly".equalsIgnoreCase(budget.getFrequency())) {
                    // Check if current date is within a 7-day window from start date
                    Calendar endCal = (Calendar) startCal.clone();
                    while (endCal.before(currentCal) || endCal.equals(currentCal)) {
                        Calendar windowEnd = (Calendar) endCal.clone();
                        windowEnd.add(Calendar.DAY_OF_YEAR, 7);
                        
                        if ((currentCal.after(endCal) || currentCal.equals(endCal)) && 
                            currentCal.before(windowEnd)) {
                            isActive = true;
                            break;
                        }
                        
                        endCal.add(Calendar.DAY_OF_YEAR, 7);
                    }
                } else if ("Monthly".equalsIgnoreCase(budget.getFrequency())) {
                    // Check if current date is in the same month as any period from start date
                    Calendar endCal = (Calendar) startCal.clone();
                    while (endCal.before(currentCal) || endCal.equals(currentCal)) {
                        if (endCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                            endCal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH)) {
                            isActive = true;
                            break;
                        }
                        endCal.add(Calendar.MONTH, 1);
                    }
                }

                if (isActive) {
                    activeBudgets.add(budget);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return activeBudgets;
    }

    /**
     * Filter expenses that fall within the active budget windows
     */
    private List<Expense> filterRelevantExpenses(List<Expense> expenses, List<Budget> activeBudgets, String currentDateStr) {
        List<Expense> relevantExpenses = new ArrayList<>();

        try {
            Date currentDate = dateFormat.parse(currentDateStr);

            for (Expense expense : expenses) {
                Date expenseDate = parseExpenseDate(expense.getDate());
                if (expenseDate == null || expenseDate.after(currentDate)) {
                    continue;
                }

                // Check if expense matches any active budget category
                for (Budget budget : activeBudgets) {
                    if (expense.getCategory().getDisplayName().equals(budget.getCategory())) {
                        // Check if expense is within the budget window
                        if (isExpenseInBudgetWindow(expense, budget, currentDateStr)) {
                            relevantExpenses.add(expense);
                            break;
                        }
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return relevantExpenses;
    }

    /**
     * Check if expense falls within the budget window
     */
    private boolean isExpenseInBudgetWindow(Expense expense, Budget budget, String currentDateStr) {
        try {
            Date expenseDate = parseExpenseDate(expense.getDate());
            Date startDate = dateFormat.parse(budget.getStartDate());
            Date currentDate = dateFormat.parse(currentDateStr);

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startDate);
            
            Calendar expenseCal = Calendar.getInstance();
            expenseCal.setTime(expenseDate);

            if ("Weekly".equalsIgnoreCase(budget.getFrequency())) {
                // Find which week the current date is in
                Calendar windowStart = (Calendar) startCal.clone();
                while (windowStart.before(currentDate) || windowStart.equals(currentDate)) {
                    Calendar windowEnd = (Calendar) windowStart.clone();
                    windowEnd.add(Calendar.DAY_OF_YEAR, 7);
                    
                    if ((currentDate.after(windowStart.getTime()) || currentDate.equals(windowStart.getTime())) && 
                        currentDate.before(windowEnd.getTime())) {
                        // Current date is in this window, check if expense is too
                        return (expenseDate.after(windowStart.getTime()) || expenseDate.equals(windowStart.getTime())) &&
                               expenseDate.before(windowEnd.getTime());
                    }
                    
                    windowStart.add(Calendar.DAY_OF_YEAR, 7);
                }
            } else if ("Monthly".equalsIgnoreCase(budget.getFrequency())) {
                Calendar currentCal = Calendar.getInstance();
                currentCal.setTime(currentDate);
                
                // Check if expense is in the same month as current date
                return expenseCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                       expenseCal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Parse expense date (supports both yyyy-MM-dd and MM/dd/yyyy)
     */
    private Date parseExpenseDate(String dateStr) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat usFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        
        try {
            return isoFormat.parse(dateStr);
        } catch (ParseException e) {
            try {
                return usFormat.parse(dateStr);
            } catch (ParseException ex) {
                return null;
            }
        }
    }

    /**
     * Compute summaries by category
     */
    private Map<String, CategorySummary> computeCategorySummaries(List<Budget> budgets, List<Expense> expenses) {
        Map<String, CategorySummary> summaries = new HashMap<>();

        // Initialize with budgets
        for (Budget budget : budgets) {
            String category = budget.getCategory();
            if (!summaries.containsKey(category)) {
                summaries.put(category, new CategorySummary(category));
            }
            CategorySummary summary = summaries.get(category);
            summary.budgeted += budget.getAmount();
        }

        // Add expenses
        for (Expense expense : expenses) {
            String category = expense.getCategory().getDisplayName();
            if (!summaries.containsKey(category)) {
                summaries.put(category, new CategorySummary(category));
            }
            CategorySummary summary = summaries.get(category);
            summary.spent += expense.getAmount();
        }

        // Calculate remaining
        for (CategorySummary summary : summaries.values()) {
            summary.remaining = summary.budgeted - summary.spent;
        }

        return summaries;
    }

    /**
     * Helper class to hold category summary data
     */
    public static class CategorySummary {
        public String category;
        public double budgeted;
        public double spent;
        public double remaining;

        public CategorySummary(String category) {
            this.category = category;
            this.budgeted = 0.0;
            this.spent = 0.0;
            this.remaining = 0.0;
        }
    }
}
