package com.android.server.policy.touchlogger;

public class FastStringBuilder {
    private char[] mChars;
    private int mLength;

    public FastStringBuilder() {
        mChars = new char[64];
    }

    public FastStringBuilder clear() {
        mLength = 0;
        return this;
    }

    public FastStringBuilder append(String value) {
        final int valueLength = value.length();
        final int index = reserve(valueLength);
        value.getChars(0, valueLength, mChars, index);
        mLength += valueLength;
        return this;
    }

    public FastStringBuilder append(int value) {
        return append(value, 0);
    }

    public FastStringBuilder append(int value, int zeroPadWidth) {
        final boolean negative = value < 0;
        if (negative) {
            value = - value;
            if (value < 0) {
                append("-2147483648");
                return this;
            }
        }

        int index = reserve(11);
        final char[] chars = mChars;

        if (value == 0) {
            chars[index++] = '0';
            mLength += 1;
            return this;
        }

        if (negative) {
            chars[index++] = '-';
        }

        int divisor = 1000000000;
        int numberWidth = 10;
        while (value < divisor) {
            divisor /= 10;
            numberWidth -= 1;
            if (numberWidth < zeroPadWidth) {
                chars[index++] = '0';
            }
        }

        do {
            int digit = value / divisor;
            value -= digit * divisor;
            divisor /= 10;
            chars[index++] = (char) (digit + '0');
        } while (divisor != 0);

        mLength = index;
        return this;
    }

    public FastStringBuilder append(float value, int precision) {
        int scale = 1;
        for (int i = 0; i < precision; i++) {
            scale *= 10;
        }
        value = (float) (Math.rint(value * scale) / scale);

        append((int) value);

        if (precision != 0) {
            append(".");
            value = Math.abs(value);
            value -= Math.floor(value);
            append((int) (value * scale), precision);
        }

        return this;
    }

    @Override
    public String toString() {
        return new String(mChars, 0, mLength);
    }

    private int reserve(int length) {
        final int oldLength = mLength;
        final int newLength = mLength + length;
        final char[] oldChars = mChars;
        final int oldCapacity = oldChars.length;
        if (newLength > oldCapacity) {
            final int newCapacity = oldCapacity * 2;
            final char[] newChars = new char[newCapacity];
            System.arraycopy(oldChars, 0, newChars, 0, oldLength);
            mChars = newChars;
        }
        return oldLength;
    }
}
