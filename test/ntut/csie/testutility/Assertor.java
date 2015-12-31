package ntut.csie.testutility;

import static org.junit.Assert.assertEquals;
import java.util.List;
import ntut.csie.csdet.data.MarkerInfo;

public class Assertor {

	public static void assertListSize(int expectedSize, List<?> list) {
		makeSureListNotNull(list);

		assertEquals(colloectListContents(list), expectedSize, list.size());
	}

	public static void assertMarkerInfoListSize(int expectedSize,
			List<MarkerInfo> list) {
		makeSureListNotNull(list);

		assertEquals(colloectMarkerInfoListContents(list), expectedSize,
				list.size());
	}

	private static void makeSureListNotNull(List<?> list) {
		if (list == null) {
			throw new IllegalArgumentException("Null list");
		}
	}

	/**
	 * Record context of each component in this list
	 */
	private static String colloectListContents(List<?> list) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i).toString()).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Record context of each bad smells and its line number
	 */
	private static String colloectMarkerInfoListContents(List<MarkerInfo> list) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		for (int i = 0; i < list.size(); i++) {
			MarkerInfo m = list.get(i);
			sb.append(m.getLineNumber()).append("\t").append(m.getStatement())
					.append("\n");
		}
		return sb.toString();
	}

}
