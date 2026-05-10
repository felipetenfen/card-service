package com.cardservice.card.validation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LuhnCardValidator implements CardValidator {

    private final boolean luhnEnabled;

    public LuhnCardValidator(@Value("${card.validation.luhn.enabled:false}") boolean luhnEnabled) {
        this.luhnEnabled = luhnEnabled;
    }

    @Override
    public boolean isValid(String cardNumber) {
        if (!luhnEnabled) return true;
        if (cardNumber == null) return false;
        String digits = cardNumber.trim();
        if (digits.length() < 13 || digits.length() > 19) return false;
        if (!digits.chars().allMatch(Character::isDigit)) return false;

        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
