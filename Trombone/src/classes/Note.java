package classes;

public class Note {
	private int pitch; // temporary set 0 as G
	private int beat; // no saenggak yet
	private boolean isRest;
	public int x;
	public int y;

	// TODO : sharp? flat?

	public Note(int pitch, int beat, boolean rest) {
		this.pitch = pitch;
		this.beat = beat;
		this.isRest = rest;
		this.x = -100;
		this.y = -100;
	}

	public Note(int pitch, int beat) {
		this.pitch = pitch;
		this.beat = beat;
		this.isRest = false;
		this.x = -100;
		this.y = -100;
	}

	public Note(int pitch) {
		this.pitch = pitch;
		this.beat = 1;
		this.isRest = false;
		this.x = -100;
		this.y = -100;
	}

	public int getPitch() {
		return pitch;
	}

	public void setPitch(int pitch) {
		this.pitch = pitch;
	}

	public int getBeat() {
		return beat;
	}

	public void setBeat(int beat) {
		this.beat = beat;
	}

	public boolean isRest() {
		return isRest;
	}

	public void setRest(boolean isRest) {
		this.isRest = isRest;
	}
}
