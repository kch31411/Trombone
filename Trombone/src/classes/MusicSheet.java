package classes;

import classes.Note;
import classes.Memo;

public class MusicSheet {
	private int beat;
	private int pages;
	private String name;
	private Note[][] note;
	private Memo[][] memo;
	
	public int getBeat() {
		return beat;
	}
	public void setBeat(int beat) {
		this.beat = beat;
	}
	public int getPages() {
		return pages;
	}
	public void setPages(int pages) {
		this.pages = pages;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
