package QRCode;

//This exception is thrown when the supplied data does not fit any QR code version.
//Some ways to handle this exception include:
//(i) Decrease the error correction level if it was greater than L.
//(ii) Split the text data into better or optimal segments in order to reduce the number of bits required.
//(iii) Change the text or binary data to be shorter.
//(iv) Change the text to fit the character set of a particular segment mode (e.g. alphanumeric).
//(v) Propagate the error upward to the caller/user.
public class DataTooLongException extends IllegalArgumentException {
	
	public DataTooLongException() {}
	
	
	public DataTooLongException(String msg) {
		super(msg);
	}
	
}
