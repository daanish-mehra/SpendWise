package com.example.spendwise.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class CurrentDateManager {
    private static final String PREFS_NAME = "spendwise_prefs";
    private static final String KEY_CURRENT_DATE_MILLIS = "current_date_millis";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private CurrentDateManager() {}

    public static LocalDate getCurrentDate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long saved = prefs.getLong(KEY_CURRENT_DATE_MILLIS, -1L);
        if (saved <= 0) {
            return LocalDate.now();
        }
        return Instant.ofEpochMilli(saved).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void setCurrentDate(Context context, LocalDate date) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        prefs.edit().putLong(KEY_CURRENT_DATE_MILLIS, millis).apply();
    }

    public static void clear(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_CURRENT_DATE_MILLIS).apply();
    }

    public static String format(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public static Date toStartOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date toEndOfDay(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate startOfWeek(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return date.minusDays(dayOfWeek - 1L);
    }

    public static LocalDate endOfWeek(LocalDate date) {
        return startOfWeek(date).plusDays(6);
    }

    public static LocalDate startOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    public static LocalDate endOfMonth(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }
}
