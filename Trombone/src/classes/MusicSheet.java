package classes;

import classes.Note;
import classes.Memo;

public class MusicSheet {
	private int id;
	private int beat;
	private int pages;
	private String name;
	private Note[][] note;
	private Memo[][] memo;
	
	
	public MusicSheet(int id, String name, int beat, int pages) {
		// TODO Auto-generated constructor stub
		this.id = id;
		this.name = name;
		this.beat = beat;
		this.pages = pages;
		
		// memo 가져오기, note 가져오기
	}
	public MusicSheet(String name, int beat, int pages) {
		// TODO Auto-generated constructor stub
		this.name = name;
		this.beat = beat;
		this.pages = pages;
	}
	public MusicSheet() {
		// TODO Auto-generated constructor stub
		this.id = this.beat = this.pages = -1;
		this.name = "";
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
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
