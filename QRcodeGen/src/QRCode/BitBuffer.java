package QRCode;
import java.util.BitSet;
import java.util.Objects;


//An appendable sequence of bits (0s and 1s). Mainly used by QrSegment.
public final class BitBuffer implements Cloneable {
	
	//Fields
	private BitSet data;
	private int bitLength;  //Non-negative
	
	//Constructor - constructs an empty bit buffer
	public BitBuffer() {
		data = new BitSet();
		bitLength = 0;
	}
	
	//Methods
	//Returns the length of the sequence.
	public int bitLength() {
		assert bitLength >= 0;
		return bitLength;
	}
	
	//Returns the bit at the specified index
	public int getBit(int index) {
		if (index < 0 || index >= bitLength)
			throw new IndexOutOfBoundsException();
		return data.get(index) ? 1 : 0;
	}
	
	
	//Appends the specified number of low-order bits of the specified value to this buffer.
	public void appendBits(int val, int len) {
		if (len < 0 || len > 31 || val >>> len != 0)
			throw new IllegalArgumentException("Value out of range");
		if (Integer.MAX_VALUE - bitLength < len)
			throw new IllegalStateException("Maximum length reached");
		for (int i = len - 1; i >= 0; i--, bitLength++)  // Append bit by bit
			data.set(bitLength, QrCode.getBit(val, i));
	}
	
	
	//Appends the content of the specified bit buffer to this buffer.
	public void appendData(BitBuffer bb) {
		Objects.requireNonNull(bb);
		if (Integer.MAX_VALUE - bitLength < bb.bitLength)
			throw new IllegalStateException("Maximum length reached");
		for (int i = 0; i < bb.bitLength; i++, bitLength++)  // Append bit by bit
			data.set(bitLength, bb.data.get(i));
	}
	
	
	//Returns a copy of this buffer.
	public BitBuffer clone() {
		try {
			BitBuffer result = (BitBuffer)super.clone();
			result.data = (BitSet)result.data.clone();
			return result;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
	
}