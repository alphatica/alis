package com.alphatica.alis.data.market;


import java.util.Objects;

public record MarketName(String name) implements Comparable<MarketName> {
	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(MarketName o) {
		return name.compareTo(o.name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MarketName that = (MarketName) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}
}
