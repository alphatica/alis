package com.alphatica.alis.data.time;


import java.time.LocalDate;
import java.util.Objects;

public record Time(long time) implements Comparable<Time> {

	public boolean isBefore(Time time) {
		return this.time < time.time;
	}

	public Time next() {
		return new Time(this.time + 1);
	}

	public boolean isAfter(Time time) {
		return this.time > time.time;
	}

	@Override
	public int compareTo(Time o) {
		return Long.compare(this.time, o.time);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Time time1 = (Time) o;
		return time == time1.time;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(time);
	}

	@Override
	public String toString() {
		return String.format("%d", time);
	}

    public LocalDate toLocalDate() {
        int day = (int)time % 1_00;
        int month = (int)(time / 1_00) % 100;
        int year = (int)(time / 1_00_00);
        return LocalDate.of(year, month, day);
    }
}
