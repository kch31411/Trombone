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
		this.playCount = 0;
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
		return note.get(page-1);
	}
	public ArrayList<Memo> getMemos(int page) {
		return memo.get(page-1);
	}
	public Note getNote(int page, int index) {
		// also give note in prev or next page
		// simply assume that it do not pointing 2 pages before or after
		int curr_size = getNotes(page).size();
		int new_index = index;
		int new_page = page;
		if (index >= curr_size) {
			new_index = index - curr_size;
			new_page = page + 1;
			
			if (new_page > pages || new_index >= getNotes(new_page).size()) return null;
		} else if (index < 0) {
			
			if(page-1<1) return null;
			
			new_index = index + getNotes(page-1).size();
			new_page = page - 1;

			if (new_page < 1 || new_index < 0) return null;
		}
		
		return getNotes(new_page).get(new_index);
	}
}
