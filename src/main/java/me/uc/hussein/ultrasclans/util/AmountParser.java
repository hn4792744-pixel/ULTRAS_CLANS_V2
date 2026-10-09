package me.uc.hussein.ultrasclans.util;

/** تحليل آمن لمبلغ مكتوب: يرفض النص الطويل وNaN وInfinity والسالب والقيم الضخمة (قسم 55). */
public final class AmountParser {

    private AmountParser() {
    }

    /** @return المبلغ (بخانتين عشريتين كحد أقصى) أو -1 عند أي مشكلة. */
    public static double parse(String text) {
        if (text == null || text.length() > 15) {
            return -1;
        }
        try {
            double value = Double.parseDouble(text.replace(",", "").trim());
            if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0 || value > 1_000_000_000_000.0) {
                return -1;
            }
            return Math.floor(value * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
