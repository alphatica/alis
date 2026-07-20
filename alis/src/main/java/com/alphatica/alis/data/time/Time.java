package com.alphatica.alis.data.time;


import java.time.LocalDate;

public record Time(int time) implements Comparable<Time> {

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
		return Integer.compare(this.time, o.time);
	}

	@Override
	public String toString() {
		return String.format("%d", time);
	}

    public LocalDate toLocalDate() {
        int day = time % 1_00;
        int month = (time / 1_00) % 100;
        int year = (time / 1_00_00);
        return LocalDate.of(year, month, day);
    }
}
