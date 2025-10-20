package com.example.spendwise.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ExpenseRepository {
    private static final String COLLECTION_EXPENSES = "expenses";

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public ExpenseRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public Task<DocumentReference> addExpense(@NonNull String name,
                                              double amount,
                                              @NonNull String category,
                                              @NonNull Date date,
                                              String notes) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("name", name);
        data.put("amount", amount);
        data.put("category", category);
        data.put("date", date);
        data.put("notes", notes);
        return firestore.collection(COLLECTION_EXPENSES).add(data);
    }

    public Query queryExpensesForUserBetween(Date startInclusive, Date endExclusive) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        return firestore.collection(COLLECTION_EXPENSES)
                .whereEqualTo("userId", uid)
                .whereGreaterThanOrEqualTo("date", startInclusive)
                .whereLessThan("date", endExclusive);
    }

    public Task<Double> sumAmountBetween(Date startInclusive, Date endExclusive) {
        Query q = queryExpensesForUserBetween(startInclusive, endExclusive);
        return q.get().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return 0.0;
            }
            double total = 0.0;
            for (DocumentSnapshot d : task.getResult().getDocuments()) {
                Number value = (Number) d.get("amount");
                if (value != null) {
                    total += value.doubleValue();
                }
            }
            return total;
        });
    }

    public Task<java.util.Map<String, Double>> sumByCategoryBetween(Date startInclusive, Date endExclusive) {
        Query q = queryExpensesForUserBetween(startInclusive, endExclusive);
        return q.get().continueWith(task -> {
            java.util.Map<String, Double> totals = new java.util.HashMap<>();
            if (!task.isSuccessful() || task.getResult() == null) {
                return totals;
            }
            for (DocumentSnapshot d : task.getResult().getDocuments()) {
                String cat = d.getString("category");
                Number amount = (Number) d.get("amount");
                if (cat == null || amount == null) continue;
                Double prev = totals.get(cat);
                totals.put(cat, (prev == null ? 0.0 : prev) + amount.doubleValue());
            }
            return totals;
        });
    }
}
