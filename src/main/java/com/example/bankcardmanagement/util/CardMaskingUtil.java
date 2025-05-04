package com.example.bankcardmanagement.util;

public class CardMaskingUtil {

    private static final int VISIBLE_DIGITS = 4;
    private static final char MASK_CHAR = '*';
    private static final int GROUP_SIZE = 4;

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() <= VISIBLE_DIGITS) {
            return cardNumber;
        }

        int length = cardNumber.length();
        int maskLength = length - VISIBLE_DIGITS;

        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maskLength; i++) {
            masked.append(MASK_CHAR);
            if ((i + 1) % GROUP_SIZE == 0 && (i + 1) < maskLength) {
                masked.append(" ");
            }
        }

        if (maskLength % GROUP_SIZE != 0 && maskLength > 0) {
            masked.append(" ");
        } else if (maskLength == 0) {
            int visibleGroups = (length / GROUP_SIZE) - (length % GROUP_SIZE == 0 ? 1 : 0);
            for(int i=0; i < visibleGroups; ++i){
                masked.append(String.valueOf(MASK_CHAR).repeat(GROUP_SIZE)).append(" ");
            }
        }


        masked.append(cardNumber.substring(maskLength));

        return masked.toString();
    }

    // Простой тест
    public static void main(String[] args) {
        System.out.println(maskCardNumber("1234567890123456"));
        System.out.println(maskCardNumber("123456789012"));
        System.out.println(maskCardNumber("123456"));
        System.out.println(maskCardNumber("1234"));
        System.out.println(maskCardNumber(null));
    }
}