package com.alphatica.alis.tools.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionsTools {
	private CollectionsTools() {
	}

	@SafeVarargs
	public static <T> List<T> arrayList(T... elements) {
		List<T> list = new ArrayList<>();
		Collections.addAll(list, elements);
		return list;
	}

}
