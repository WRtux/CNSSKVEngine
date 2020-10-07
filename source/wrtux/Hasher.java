package wrtux;

public final class Hasher {
	
	protected static final int SEED = 0x7A84105F;
	
	/** 用于替代{@link String#hashCode()}。 */
	public static int hash(String str) {
		char[] chs = str.toCharArray();
		return sun.misc.Hashing.murmur3_32(SEED, chs, 0, chs.length);
	}
	
}
