package com.sk.editor.utils;

public enum FontSize {
	x8, x10, x12, x14, x16, x24, x32, x40, x48, x56, x64;
	
	public int toInt() {
		return Integer.parseInt(this.name().replace("x", ""));
	}
}
