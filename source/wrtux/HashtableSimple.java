package wrtux;

import java.util.Hashtable;
import java.util.Random;

/**
 * 简单的哈希表类。不支持并发操作，索引不会自动扩展。
 * @author Wilderness Ranger
 */
public class HashtableSimple {
	
	/** 索引项类。结点构成链表结构。 */
	public static final class Entry {
		
		/** 指向首个结点。若链表为空，则为{@code null}。 */
		protected Node head;
		
		/** 链表大小，即结点数。 */
		protected int size;
		
		protected Entry() {}
		
		/** 获取链表的大小{@link #size}。 */
		public int getSize() {
			return this.size;
		}
		
		/** 根据哈希值取值。 */
		public String get(int hash) {
			Node ptr = this.head;
			while(ptr != null) {
				if(ptr.hash == hash)
					return ptr.value;
				ptr = ptr.next;
			}
			return null;
		}
		
		/** 根据key值取值。 */
		public String get(String key) {
			Node ptr = this.head;
			int hash = key.hashCode();
			while(ptr != null) {
				if(ptr.hash == hash && ptr.key.equals(key))
					return ptr.value;
				ptr = ptr.next;
			}
			return null;
		}
		
		/**
		 * 直接加入结点，不考虑重复key值。
		 * 即使key值重复，之后也能取到新加入的值，但是会影响整体效率。
		 */
		public void add(Node nod) {
			nod.next = this.head;
			this.head = nod;
			this.size++;
		}
		
		/**
		 * 直接在指定位置插入结点，不考虑重复key值。
		 * 如果key值重复，之后会取得靠前结点的值。
		 */
		public void insert(Node nod, int pos) {
			if(pos < 0)
				throw new IllegalArgumentException("Negative position.");
			if(pos == 0) {
				nod.next = this.head;
				this.head = nod;
				this.size++;
				return;
			}
			Node ptr = this.head;
			for(int i = 1; i < pos && ptr.next != null; i++)
				ptr = ptr.next;
			nod.next = ptr.next;
			ptr.next = nod;
			this.size++;
		}
		
		/**
		 * 加入结点，若有重复，则替换原有结点。
		 * @param nod 待加入结点。若链接了更多结点，将一并加入。
		 */
		public String put(Node nod) {
			if(nod == null)
				throw new NullPointerException();
			Node ptr = this.head;
			if(ptr == null) {
				this.head = nod;
				this.size = 1; //TODO
				return null;
			}
			if(ptr.hash == nod.hash && ptr.key.equals(nod.key)) {
				this.head = nod;
				nod.next = ptr.next; //TODO
				ptr.next = null;
				return ptr.value;
			}
			while(ptr.next != null) {
				Node nxt = ptr.next;
				if(nxt.hash == nod.hash && nxt.key.equals(nod.key)) {
					ptr.next = nod;
					nod.next = nxt.next; //TODO
					nxt.next = null;
					return nxt.value;
				}
				ptr = nxt;
			}
			ptr.next = nod;
			this.size++; //TODO
			return null;
		}
		
		public void optimize() {
			//TODO
		}
		
	}
	
	/** 结点类。 */
	public static final class Node {
		
		public final String key;
		protected final int hash;
		
		public final String value;
		
		/** 指向下一个结点，构成链表。 */
		protected Node next;
		
		Node(String key, int hash, String val) {
			if(val == null)
				throw new NullPointerException();
			this.key = key;
			this.hash = hash;
			this.value = val;
		}
		protected Node(String key, String val) {
			this(key, key.hashCode(), val);
		}
		
	}
	
	/** 索引数组。 */
	protected Entry[] field;
	
	/** 哈希表大小，即结点数。 */
	protected int size;
	
	int shift;
	
	void initField(int b) {
		if(b > 24)
			throw new IllegalArgumentException("Illegal bit count.");
		this.field = new Entry[1 << b];
		for(int i = 0; i < this.field.length; i++)
			this.field[i] = new Entry();
		this.shift = 32 - b;
	}
	
	/** 参考给定容量构造哈希表。 */
	public HashtableSimple(int cap) {
		int b = 0;
		for(cap--; cap > 0; cap >>= 1)
			b++;
		this.initField(Math.max(b, 4));
	}
	
	/** 使用默认容量构造哈希表。 */
	public HashtableSimple() {
		this.initField(4);
	}
	
	/** 获取哈希表的大小{@link #size}。 */
	public int getSize() {
		return this.size;
	}
	
	/** 获取哈希表的容量（{@link #field}的大小）。 */
	public int getCapacity() {
		return this.field.length;
	}
	
	/** 根据哈希值取值。 */
	public String get(int hash) {
		return this.field[hash >>> this.shift].get(hash);
	}
	
	/** 根据key值取值。 */
	public String get(String key) {
		int i = key.hashCode() >>> this.shift;
		return this.field[i].get(key);
	}
	
	/**
	 * 直接加入项目，不考虑重复key值。
	 * 即使key值重复，之后也能取到新加入的值，但是会影响整体效率。
	 */
	public void add(String key, String val) {
		int hash = key.hashCode();
		this.field[hash >>> this.shift].add(new Node(key, hash, val));
		this.size++;
	}
	
	/**
	 * 加入项目，若有重复，则替换原有值。
	 * @return 被替换的值。
	 */
	public String put(String key, String val) {
		int hash = key.hashCode(), i = hash >>> this.shift;
		String prev = this.field[i].put(new Node(key, hash, val));
		if(prev == null)
			this.size++;
		return prev;
	}
	
	public void optimize() {
		//TODO
	}
	
}

final class Main {
	
	static void init() {
		HashtableSimple htbl = new HashtableSimple(256);
		for(int i = 0; i < 16384; i++) {
			String key = Integer.toString(i), val = Integer.toHexString(i);
			htbl.add(key, val);
			htbl.put(key, val);
			htbl.get(key);
		}
	}
	
	public static void main(String[] args) {
		
		//初始化
		HashtableSimple htbl = new HashtableSimple(256);
		Hashtable<String, String> ref = new Hashtable<>(256);
		String[] keys = new String[1024], vals = new String[keys.length];
		Random rand = new Random();
		for(int i = 0; i < keys.length; i++) {
			keys[i] = "k" + i;
			vals[i] = Integer.toHexString(rand.nextInt());
		}
		if(args.length == 1 && args[0].equals("-i")) init();
		
		long t1 = System.nanoTime();
		for(int i = 0; i < keys.length; i++)
			keys[i].hashCode();
		long t2 = System.nanoTime();
		System.out.printf("Hash time: %dus%n", (t2 - t1) / 1000);
		
		//HashtableSimple效率测试
		t1 = System.nanoTime();
		for(int i = 0; i < keys.length; i++)
			htbl.add(keys[i], vals[i]);
		t2 = System.nanoTime();
		System.out.printf("Add time: %dus%n", (t2 - t1) / 1000);
		
		t1 = System.nanoTime();
		for(int i = 0; i < keys.length; i++)
			htbl.put(keys[i], vals[i]);
		t2 = System.nanoTime();
		System.out.printf("Put time: %dus%n", (t2 - t1) / 1000);
		
		t1 = System.nanoTime();
		@SuppressWarnings("unused") String val;
		for(String key : keys)
			val = htbl.get(key);
		t2 = System.nanoTime();
		System.out.printf("Get time: %dus%n", (t2 - t1) / 1000);
		
		//API Hashtable效率测试
		t1 = System.nanoTime();
		for(int i = 0; i < keys.length; i++)
			ref.put(keys[i], vals[i]);
		t2 = System.nanoTime();
		System.out.printf("Put time ref: %dus%n", (t2 - t1) / 1000);
		
		t1 = System.nanoTime();
		for(String key : keys)
			val = ref.get(key);
		t2 = System.nanoTime();
		System.out.printf("Get time ref: %dus%n", (t2 - t1) / 1000);
		
		//数据验证
		System.out.println();
		for(int i = 1; i <= keys.length; i <<= 1) {
			int j = i - 1;
			System.out.printf("Key<%s>: %s - %s%n", keys[j], htbl.get(keys[j]), vals[j]);
		}
		System.out.printf("Capacity: %d, size: %d%n", htbl.getCapacity(), htbl.getSize());
		
	}
	
}
