package dbtools;

public class ParserException extends Exception {

	private static final long serialVersionUID = 1L;

	public ParserException() {
		super();
	}
	
	public ParserException(String message) {
		super(message);
	}
	
	public ParserException(String message, Throwable t) {
		super(message, t);
	}
	
}
