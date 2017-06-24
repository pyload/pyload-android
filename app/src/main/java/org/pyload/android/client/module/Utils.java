package org.pyload.android.client.module;

public final class Utils {

	public static String formatSize(long size) {
		double format = size;
		int steps = 0;
		String[] sizes = { "B", "KiB", "MiB", "GiB", "TiB" };
		while (format > 1000) {
			format /= 1024.0;
			steps++;
		}
		return String.format("%.2f %s", format, sizes[steps]);
	}
}
