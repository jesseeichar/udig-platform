package com.camptocamp.sbb.process;

public class WpsProcess implements Comparable<WpsProcess>{
	public final String title;
	public final String id;
	public final String abstr;
	public WpsProcess(String title, String id, String abstr) {
		super();
		this.title = title;
		this.id = id;
		this.abstr = abstr;
	}
	@Override
	public int compareTo(WpsProcess o) {
		return toString().toString().compareToIgnoreCase(o.toString());
	}
	@Override
	public String toString() {
		if (id.equals("gs:Import")) {
			return "Publish Layer (gs:Import)";
		}
		return title + " (" + id + ")";
	}
	
}
