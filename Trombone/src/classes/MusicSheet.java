package classes;

import java.util.ArrayList;

import classes.Note;
import classes.Memo;

public class MusicSheet {

	private int id;
	private int beat;
	private int pages;
	private String name;
	private int playCount;
	private int keyNumber;
	private ArrayList<ArrayList<Note>> note;
	private ArrayList<ArrayList<Memo>> memo;
	
	public MusicSheet(String name, int beat, int pages, int keyNumber) {
		this.name = name;
		this.beat = beat;
		this.pages = pages;
		this.keyNumber = keyNumber;
	}
	public MusicSheet(int id, String name, int beat, int pages, int playCount, int keyNumber,
			ArrayList<ArrayList<Note>> note, ArrayList<ArrayList<Memo>> memo) {
		// TODO Auto-generated constructor stub
		this.id = id;
		this.name = name;
		this.beat = beat;
		this.pages = pages;
		this.playCount = playCount;
		this.keyNumber = keyNumber;
		this.note = note;
		this.memo = memo;
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
	public int getPlayCount() {
		return playCount;
	}
	public void setPlayCount(int playCount) {
		this.playCount = playCount;
	}
	public int getKeyNumber() {
		return keyNumber;
	}
	public void setKeyNumber(int keyNumber) {
		this.keyNumber = keyNumber;
	}
	public ArrayList<Note> getNotes(int page) {
		return note.get(page);
	}
	public ArrayList<Memo> getMemos(int page) {
		return memo.get(page);
	}
}
