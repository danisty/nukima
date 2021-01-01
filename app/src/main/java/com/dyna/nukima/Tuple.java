package com.dyna.nukima;

public class Tuple<X, Y> {
	public final X key;
	public final Y value;
	public Tuple(X key, Y value) {
		this.key = key;
		this.value = value;
	}
}
