package classes;

public class History {
	public History() {
		super();
	}
	public History(int id, String date, int score, int musicsheet_id) {
		super();
		this.id = id;
		this.date = date;
		this.score = score;
		this.musicsheet_id = musicsheet_id;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public int getMusicsheet_id() {
		return musicsheet_id;
	}
	public void setMusicsheet_id(int musicsheet_id) {
		this.musicsheet_id = musicsheet_id;
	}
	
	private int id;
	private String date;
	private int score;
	private int musicsheet_id;
}
