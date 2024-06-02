package me.rages.reliableframework.utils;

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility class for currency-related operations.
 *
 * <p>
 * This class provides methods for abbreviating currency values and converting abbreviated currency values back to their
 * original form.
 * </p>
 *
 * <p>
 * Credits to [@loganb1max](https://github.com/loganb1max) for the implementation.
 * </p>
 */
public class NumberUtils {

    // List of currency suffixes along with their corresponding values
    private static final List<Map.Entry<Long, String>> CURRENCY_SUFFIXES = Arrays.asList(
            Maps.immutableEntry(1000000000000000L, "Q"),
            Maps.immutableEntry(1000000000000L, "T"),
            Maps.immutableEntry(1000000000L, "B"),
            Maps.immutableEntry(1000000L, "M"),
            Maps.immutableEntry(1000L, "K")
    );

    /**
     * Abbreviates a currency value.
     *
     * @param amount the currency amount
     * @param twoDp  whether to include two decimal places
     * @return the abbreviated currency value
     */
    public static String getAbbreviatedCurrency(double amount, boolean twoDp) {
        if (amount < 10000.0) {
            return String.format("%,d", (long) amount);
        }
        for (Map.Entry<Long, String> entry : CURRENCY_SUFFIXES) {
            double value = amount / (double) entry.getKey().longValue();
            if (!(value >= 1.0)) continue;
            return twoDp ? String.format("%,.2f%s", value, entry.getValue()) : String.format("%,d%s", (long) Math.floor(value), entry.getValue());
        }
        return String.format("%,d", (long) amount);
    }

    /**
     * Abbreviates a currency value without two decimal places.
     *
     * @param amount the currency amount
     * @return the abbreviated currency value
     */
    public static String getAbbreviatedCurrency(double amount) {
        return getAbbreviatedCurrency(amount, false);
    }

    /**
     * Retrieves the original value from an abbreviated currency string.
     *
     * @param abbreviated the abbreviated currency string
     * @return the original value
     */
    public static long getValueFromAbbreviatedCurrency(String abbreviated) {
        try {
            char multiplier = Character.toUpperCase(abbreviated.charAt(abbreviated.length() - 1));
            if (Character.isDigit(multiplier)) {
                try {
                    return Long.parseLong(abbreviated);
                } catch (NumberFormatException ex) {
                    return -1L;
                }
            }
            String multiString = String.valueOf(multiplier);
            for (Map.Entry<Long, String> entry : CURRENCY_SUFFIXES) {
                if (!entry.getValue().equals(multiString)) continue;
                return (long) (Double.parseDouble(abbreviated.substring(0, abbreviated.length() - 1).replace(",", "").replace("$", "")) * (double) entry.getKey().longValue());
            }
            return -1L;
        } catch (Throwable ex) {
            return -1L;
        }
    }
}
