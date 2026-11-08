package com.utshob.mybikes;

/**
 * Central place for values you must fill in yourself.
 * Every value that starts with "REPLACE_" is a placeholder. Replace it with the
 * real OFFICIAL BRTA link / number. Nothing here has been invented.
 */
public final class AppConfig {
    private AppConfig() {}

    public static final String PLACEHOLDER_PREFIX = "REPLACE_";

    // ---- BRTA service portal (BSP) ----
    public static final String BRTA_SERVICE_PORTAL_URL = "https://bsp.brta.gov.bd/";

    // ---- BRTA hotline (found in news sources; please re-check on brta.gov.bd before submission) ----
    public static final String BRTA_HELPLINE_NUMBER = "16107";

    // ---- App constants ----
    public static final String DEFAULT_CURRENCY = "BDT";
    public static final String[] FUEL_TYPES = {"Petrol", "Octane", "Diesel"};
    public static final String CATEGORY_FUEL = "Fuel";
    /** Categories the user can add manually. "Fuel" is generated automatically from fuel records. */
    public static final String[] EXPENSE_CATEGORIES =
            {"Engine Oil", "Maintenance", "Repair", "Parts and Accessories", "Other"};
    public static final String[] CURRENCIES = {"BDT", "৳", "USD"};

    public static boolean isPlaceholder(String s) {
        return s == null || s.trim().isEmpty() || s.startsWith(PLACEHOLDER_PREFIX);
    }
}
