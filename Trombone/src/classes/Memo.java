package classes;

public class Memo {
	public Memo() {
		super();
	}
	public Memo(int id, int x, int y, int opacity, int page, String content,
			int musicsheet_id) {
		super();
		this.id = id;
		this.x = x;
		this.y = y;
		this.opacity = opacity;
		this.page = page;
		this.content = content;
		this.musicsheet_id = musicsheet_id;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getOpacity() {
		return opacity;
	}
	public void setOpacity(int opacity) {
		this.opacity = opacity;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}	
	public int getPage() {
		return page;
	}
	public void setPage(int page) {
		this.page = page;
	}
	public int getMusicsheet_id() {
		return musicsheet_id;
	}
	public void setMusicsheet_id(int musicsheet_id) {
		this.musicsheet_id = musicsheet_id;
	}
	
	private int id;
	private int x;
	private int y;
	private int opacity;  // 1~100
	private int page;
	private String content;
	private int musicsheet_id;
}
