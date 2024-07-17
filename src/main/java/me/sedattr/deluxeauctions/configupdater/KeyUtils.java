package me.sedattr.deluxeauctions.configupdater;

public class KeyUtils {

	public static boolean isSubKeyOf(final String parentKey, final String subKey, final char separator) {
		if (parentKey.isEmpty())
			return false;

		return subKey.startsWith(parentKey)
				&& subKey.substring(parentKey.length()).startsWith(String.valueOf(separator));
	}

	public static String getIndents(final String key, final char separator) {
		final String[] splitKey = key.split("[" + separator + "]");
        return "  ".repeat(Math.max(0, splitKey.length - 1));
	}
}
