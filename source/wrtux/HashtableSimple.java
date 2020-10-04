package wrtux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
		protected final AtomicInteger size = new AtomicInteger();
		
		/** 读写锁。 */
		final ReadWriteLock enlock;
		
		/** 读取锁，不阻塞读取。 */
		protected final Lock readLock;
		/** 写入锁。 */
		protected final Lock writeLock;
		
		protected Entry() {
			this.enlock = new ReentrantReadWriteLock();
			this.readLock = this.enlock.readLock();
			this.writeLock = this.enlock.writeLock();
		}
		
		/** 获取链表的大小{@link #size}。 */
		public int getSize() {
			return this.size.intValue();
		}
		
		/** 根据哈希值取值。 */
		public String get(int hash) {
			this.readLock.lock();
			Node ptr = this.head;
			while(ptr != null) {
				if(ptr.hash == hash) {
					this.readLock.unlock();
					return ptr.value;
				}
				ptr = ptr.next;
			}
			//不会抛出异常，不使用finally
			this.readLock.unlock();
			return null;
		}
		
		/** 根据key值取值。 */
		public String get(String key) {
			this.readLock.lock();
			try {
				Node ptr = this.head;
				int hash = key.hashCode();
				while(ptr != null) {
					if(ptr.hash == hash && ptr.key.equals(key))
						return ptr.value;
					ptr = ptr.next;
				}
				return null;
			} finally {
				this.readLock.unlock();
			}
		}
		
		/**
		 * 直接加入结点，不考虑重复key值。
		 * 即使key值重复，之后也能取到新加入的值，但是会影响整体效率。
		 */
		public void add(Node nod) {
			this.writeLock.lock();
			nod.next = this.head;
			this.head = nod;
			this.size.incrementAndGet();
			//不会抛出异常，不使用finally
			this.writeLock.unlock();
		}
		
		/**
		 * 直接在指定位置插入结点，不考虑重复key值。
		 * 如果key值重复，之后会取得靠前结点的值。
		 */
		public void insert(Node nod, int pos) {
			if(pos < 0)
				throw new IllegalArgumentException("Negative position.");
			this.writeLock.lock();
			try {
				if(pos == 0) {
					nod.next = this.head;
					this.head = nod;
					this.size.incrementAndGet();
					return;
				}
				Node ptr = this.head;
				for(int i = 1; i < pos && ptr.next != null; i++)
					ptr = ptr.next;
				nod.next = ptr.next;
				ptr.next = nod;
				this.size.incrementAndGet();
			} finally {
				this.writeLock.unlock();
			}
		}
		
		/**
		 * 加入结点，若有重复，则替换原有结点。
		 * @param nod 待加入结点。若链接了更多结点，将一并加入。
		 */
		public String put(Node nod) {
			if(nod == null)
				throw new NullPointerException();
			this.writeLock.lock();
			try {
				Node ptr = this.head;
				if(ptr == null) {
					this.head = nod;
					this.size.set(1);
					for(ptr = nod.next; ptr != null; ptr = ptr.next)
						this.size.incrementAndGet();
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
				for(ptr = nod; ptr != null; ptr = ptr.next)
					this.size.incrementAndGet();
				return null;
			} finally {
				this.writeLock.unlock();
			}
		}
		
		public void clear() {
			this.writeLock.lock();
			Node ptr = this.head;
			this.head = null;
			while(ptr != null) {
				Node nxt = ptr.next;
				ptr.next = null;
				ptr = nxt;
			}
			this.size.set(0);
			//不会抛出异常，不使用finally
			this.writeLock.unlock();
		}
		
		public void optimize() {
			this.writeLock.lock();
			try {
				//TODO 将来实现
			} finally {
				this.writeLock.unlock();
			}
		}
		
	}
	
	/** 结点类。 */
	public static final class Node {
		
		/** key值，不能为{@code null}。 */
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
	protected final AtomicInteger size = new AtomicInteger();
	
	int shift;
	
	void initField(int b) {
		if(b > 24)
			throw new IllegalArgumentException("Bit count too large.");
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
		return this.size.intValue();
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
		this.size.incrementAndGet();
	}
	
	/**
	 * 加入项目，若有重复，则替换原有值。
	 * @return 被替换的值。
	 */
	public String put(String key, String val) {
		int hash = key.hashCode();
		String prev = this.field[hash >>> this.shift].put(new Node(key, hash, val));
		if(prev == null)
			this.size.incrementAndGet();
		return prev;
	}
	
	public void clear() {
		for(Entry en : this.field)
			en.clear();
		this.size.set(0);
	}
	
	public void optimize() {
		//TODO 实现需要调整模型
	}
	
}
