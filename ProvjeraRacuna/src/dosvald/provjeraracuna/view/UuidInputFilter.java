package dosvald.provjeraracuna.view;

import java.util.Arrays;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * An {@link InputFilter} which helps input valid UUIDs as lowercase hex
 * strings, separated by dashes ( <code>'-'</code> ).
 * 
 * @author dosvald
 */
public class UuidInputFilter implements InputFilter {

	private static final char DASH = '-';
	private static final int[] dashIndexes = { 8, 13, 18, 23 };
	private static final int TOTAL_LENGTH = 36;

	private static final boolean isLowercaseHex(char c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'f';
	}

	private static final boolean isAllowedAt(char c, int pos) {
		if (Arrays.binarySearch(dashIndexes, pos) < 0)
			return isLowercaseHex(c);
		else
			return c == '-';
	}

	@Override
	public CharSequence filter(CharSequence source, int start, int end,
			Spanned dest, int dstart, int dend) {
		// start >= end means replacing with 0-length = deleting
		if (start >= end) {
			// Can't help if deleting
			return null;
		} else {
			int currPos = dstart;
			/*
			 * Only filter by length if appending (allows fixing by inserting
			 * then deleting)
			 */
			if (currPos == dest.length() && currPos == TOTAL_LENGTH) {
				return "";
			}
			StringBuilder alt = new StringBuilder();
			for (int i = start; i < end; ++i) {
				char c = Character.toLowerCase(source.charAt(i));

				if (isAllowedAt(c, currPos)) {
					alt.append(c);
					++currPos;
					if (isAllowedAt(DASH, currPos)) {
						alt.append(DASH);
						++currPos;
					}
				}
			}
			return alt.toString();
		}
	}
}
