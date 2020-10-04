package wrtux;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Random;

final class Tester {
	
	static abstract class Task<T> implements Runnable {
		
		protected final T object;
		
		protected long time = -1;
		
		protected Task(T o) {
			this.object = o;
		}
		
		public final void run() {
			long t1, t2;
			t1 = System.nanoTime();
			this.runImpl();
			t2 = System.nanoTime();
			this.time = t2 - t1;
		}
		
		protected abstract void runImpl();
		
	}
	
	static final class Adder extends Task<HashtableSimple> {
		
		protected final String[] keys, values;
		
		protected Adder(HashtableSimple htbl, String[] keys, String[] vals) {
			super(htbl);
			this.keys = keys;
			this.values = vals;
		}
		
		public void runImpl() {
			for(int i = 0; i < this.keys.length; i++)
				this.object.add(this.keys[i], this.values[i]);
		}
		
	}
	
	static final class Putter extends Task<HashtableSimple> {
		
		protected final String[] keys, values;
		
		protected Putter(HashtableSimple htbl, String[] keys, String[] vals) {
			super(htbl);
			this.keys = keys;
			this.values = vals;
		}
		
		public void runImpl() {
			for(int i = 0; i < this.keys.length; i++)
				this.object.put(this.keys[i], this.values[i]);
		}
		
	}
	
	static final class Getter extends Task<HashtableSimple> {
		
		protected final String[] keys;
		
		protected Getter(HashtableSimple htbl, String[] keys) {
			super(htbl);
			this.keys = keys;
		}
		
		public void runImpl() {
			for(String key : keys)
				this.object.get(key);
		}
		
	}
	
	static final class HPutter extends Task<Hashtable<String, String>> {
		
		protected final String[] keys, values;
		
		protected HPutter(Hashtable<String, String> htbl, String[] keys, String[] vals) {
			super(htbl);
			this.keys = keys;
			this.values = vals;
		}
		
		public void runImpl() {
			for(int i = 0; i < this.keys.length; i++)
				this.object.put(this.keys[i], this.values[i]);
		}
		
	}
	
	static final class HGetter extends Task<Hashtable<String, String>> {
		
		protected final String[] keys;
		
		protected HGetter(Hashtable<String, String> htbl, String[] keys) {
			super(htbl);
			this.keys = keys;
		}
		
		public void runImpl() {
			for(String key : keys)
				this.object.get(key);
		}
		
	}
	
	/** 尝试让JIT本地化{@link HashtableSimple}。 */
	static void init() {
		HashtableSimple htbl = new HashtableSimple(256);
		for(int i = 0; i < 16384; i++) {
			String key = Integer.toString(i), val = Integer.toHexString(i);
			htbl.add(key, val);
			htbl.put(key, val);
			htbl.get(key);
		}
		htbl.clear();
		System.gc();
	}
	
	public static void main(String[] args) {
		
		//初始化
		HashtableSimple htbl = new HashtableSimple(4096);
		Hashtable<String, String> ref = new Hashtable<>(4096);
		String[] keys = new String[16384], vals = new String[keys.length];
		Random rand = new Random();
		for(int i = 0; i < keys.length; i++) {
			keys[i] = "k" + i;
			vals[i] = Integer.toHexString(rand.nextInt());
		}
		init();
		
		final int tcnt = 4, tnum = keys.length / tcnt;
		String[][] tkeys = new String[tcnt][], tvals = new String[tcnt][];
		for(int i = 0; i < tkeys.length; i++) {
			tkeys[i] = Arrays.copyOfRange(keys, tnum * i, tnum * (i + 1));
			tvals[i] = Arrays.copyOfRange(vals, tnum * i, tnum * (i + 1));
		}
		
		//HashtableSimple单线程效率测试
		Adder adr = new Adder(htbl, keys, vals);
		adr.run();
		System.out.printf("1T add time: %dus%n", adr.time / 1000);
		
		Putter ptr = new Putter(htbl, keys, vals);
		ptr.run();
		System.out.printf("1T put time: %dus%n", ptr.time / 1000);
		
		Getter gtr = new Getter(htbl, keys);
		gtr.run();
		System.out.printf("1T get time: %dus%n", gtr.time / 1000);
		
		long t1 = System.nanoTime();
		htbl.clear();
		long t2 = System.nanoTime();
		System.out.printf("Clear time: %dus%n", (t2 - t1) / 1000);
		
		//API Hashtable单线程效率测试
		HPutter hptr = new HPutter(ref, keys, vals);
		hptr.run();
		System.out.printf("1T put time ref: %dus%n", hptr.time / 1000);
		
		HGetter hgtr = new HGetter(ref, keys);
		hgtr.run();
		System.out.printf("1T get time ref: %dus%n", hgtr.time / 1000);
		System.out.println();
		
		//HashtableSimple多线程效率测试
		Adder[] adrs = new Adder[tcnt];
		for(int i = 0; i < adrs.length; i++)
			adrs[i] = new Adder(htbl, tkeys[i], tvals[i]);
		for(Adder i : adrs)
			new Thread(i).start();
		while(Thread.activeCount() > 1)
			Thread.yield();
		long max = 0;
		for(Adder i : adrs)
			if(i.time > max)
				max = i.time;
		System.out.printf("4T add time: %dus%n", max / 1000);
		
		Putter[] ptrs = new Putter[tcnt];
		for(int i = 0; i < ptrs.length; i++)
			ptrs[i] = new Putter(htbl, tkeys[i], tvals[i]);
		for(Putter i : ptrs)
			new Thread(i).start();
		while(Thread.activeCount() > 1)
			Thread.yield();
		max = 0;
		for(Putter i : ptrs)
			if(i.time > max)
				max = i.time;
		System.out.printf("4T put time: %dus%n", max / 1000);
		
		Getter[] gtrs = new Getter[tcnt];
		for(int i = 0; i < gtrs.length; i++)
			gtrs[i] = new Getter(htbl, tkeys[i]);
		for(Getter i : gtrs)
			new Thread(i).start();
		while(Thread.activeCount() > 1)
			Thread.yield();
		max = 0;
		for(Getter i : gtrs)
			if(i.time > max)
				max = i.time;
		System.out.printf("4T get time: %dus%n", max / 1000);
		
		//API Hashtable多线程效率测试
		HPutter[] hptrs = new HPutter[tcnt];
		for(int i = 0; i < hptrs.length; i++)
			hptrs[i] = new HPutter(ref, tkeys[i], tvals[i]);
		for(HPutter i : hptrs)
			new Thread(i).start();
		while(Thread.activeCount() > 1)
			Thread.yield();
		max = 0;
		for(HPutter i : hptrs)
			if(i.time > max)
				max = i.time;
		System.out.printf("4T put time ref: %dus%n", max / 1000);
		
		HGetter[] hgtrs = new HGetter[tcnt];
		for(int i = 0; i < hgtrs.length; i++)
			hgtrs[i] = new HGetter(ref, tkeys[i]);
		for(HGetter i : hgtrs)
			new Thread(i).start();
		while(Thread.activeCount() > 1)
			Thread.yield();
		max = 0;
		for(HGetter i : hgtrs)
			if(i.time > max)
				max = i.time;
		System.out.printf("4T get time ref: %dus%n", max / 1000);
		
		//数据验证
		System.out.println();
		for(int i = 1; i <= keys.length; i <<= 1) {
			int j = i - 1;
			System.out.printf("Key<%s>: %s - %s%n", keys[j], htbl.get(keys[j]), vals[j]);
		}
		System.out.printf("Capacity: %d, size: %d%n", htbl.getCapacity(), htbl.getSize());
		
	}
	
}
