package wrtux;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Random;

final class Tester {
	
	static abstract class Task<T> implements Runnable {
		
		public static long multiTask(Task<?>[] tsks) {
			int tcnt = Thread.activeCount();
			for(Task<?> tsk : tsks)
				new Thread(tsk).start();
			while(Thread.activeCount() > tcnt)
				Thread.yield();
			long max = 0;
			for(Task<?> tsk : tsks) if(tsk.time > max)
				max = tsk.time;
			return max;
		}
		
		protected final T object;
		
		protected long time = -1;
		
		public Task(T o) {
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
	
	static final class Adder extends Task<HashtableS> {
		
		protected final String[] keys, values;
		
		protected Adder(HashtableS htbl, String[] keys, String[] vals) {
			super(htbl);
			this.keys = keys;
			this.values = vals;
		}
		
		public void runImpl() {
			for(int i = 0; i < this.keys.length; i++)
				this.object.add(this.keys[i], this.values[i]);
		}
		
	}
	
	static final class Putter extends Task<HashtableS> {
		
		protected final String[] keys, values;
		
		protected Putter(HashtableS htbl, String[] keys, String[] vals) {
			super(htbl);
			this.keys = keys;
			this.values = vals;
		}
		
		public void runImpl() {
			for(int i = 0; i < this.keys.length; i++)
				this.object.put(this.keys[i], this.values[i]);
		}
		
	}
	
	static final class Getter extends Task<HashtableS> {
		
		protected final String[] keys;
		
		protected Getter(HashtableS htbl, String[] keys) {
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
	
	/** 尝试让JIT本地化用到的方法。 */
	static void init() {
		final int cap = 16384, cnt = 32768;
		HashtableS htbl = new HashtableS(cap);
		Hashtable<String, String> ref = new Hashtable<>(cap);
		for(int i = 0; i < 16; i++) {
			for(int j = 0; j < cnt; j++) {
				String key = Integer.toString(j), val = Integer.toHexString(j);
				htbl.add(key, val);
				htbl.put(key, val);
				htbl.get(key);
				ref.put(key, val);
				ref.get(key);
			}
			htbl.clear();
			ref.clear();
		}
	}
	
	static void accessTest(int cap, int cnt) {
		
		System.out.printf("=====Test %dc %dn=====%n", cap, cnt);
		
		//初始化
		HashtableS htbl = new HashtableS(cap);
		Hashtable<String, String> ref = new Hashtable<>(cap);
		String[] keys = new String[cnt], vals = new String[cnt];
		final Random rand = new Random();
		for(int i = 0; i < keys.length; i++) {
			keys[i] = "k" + i;
			vals[i] = Integer.toHexString(rand.nextInt());
		}
		
		final int tcnt = 4, tnum = cnt / tcnt;
		String[][] tkeys = new String[tcnt][], tvals = new String[tcnt][];
		for(int i = 0; i < tkeys.length; i++) {
			tkeys[i] = Arrays.copyOfRange(keys, tnum * i, tnum * (i + 1));
			tvals[i] = Arrays.copyOfRange(vals, tnum * i, tnum * (i + 1));
		}
		
		//HashtableS单线程效率测试
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
		
		t1 = System.nanoTime();
		ref.clear();
		t2 = System.nanoTime();
		System.out.printf("Clear time ref: %dus%n", (t2 - t1) / 1000);
		System.out.println();
		
		//HashtableS多线程效率测试
		Adder[] adrs = new Adder[tcnt];
		for(int i = 0; i < adrs.length; i++)
			adrs[i] = new Adder(htbl, tkeys[i], tvals[i]);
		System.out.printf("4T add time: %dus%n", Task.multiTask(adrs) / 1000);
		
		Putter[] ptrs = new Putter[tcnt];
		for(int i = 0; i < ptrs.length; i++)
			ptrs[i] = new Putter(htbl, tkeys[i], tvals[i]);
		System.out.printf("4T put time: %dus%n", Task.multiTask(ptrs) / 1000);
		
		Getter[] gtrs = new Getter[tcnt];
		for(int i = 0; i < gtrs.length; i++)
			gtrs[i] = new Getter(htbl, tkeys[i]);
		System.out.printf("4T get time: %dus%n", Task.multiTask(gtrs) / 1000);
		
		//API Hashtable多线程效率测试
		HPutter[] hptrs = new HPutter[tcnt];
		for(int i = 0; i < hptrs.length; i++)
			hptrs[i] = new HPutter(ref, tkeys[i], tvals[i]);
		System.out.printf("4T put time ref: %dus%n", Task.multiTask(hptrs) / 1000);
		
		HGetter[] hgtrs = new HGetter[tcnt];
		for(int i = 0; i < hgtrs.length; i++)
			hgtrs[i] = new HGetter(ref, tkeys[i]);
		System.out.printf("4T get time ref: %dus%n", Task.multiTask(hgtrs) / 1000);
		System.out.println();
		
	}
	
	static void persistTest(int cnt, boolean comp) {
		
		System.out.printf("=====Persist test %dn=====%n", cnt);
		
		//初始化
		HashtableS htbl = new HashtableS(cnt / 2);
		String[] keys = new String[cnt], vals = new String[cnt];
		final Random rand = new Random();
		for(int i = 0; i < keys.length; i++) {
			keys[i] = "k" + i;
			vals[i] = Integer.toHexString(rand.nextInt());
		}
		
		Adder adr = new Adder(htbl, keys, vals);
		adr.run();
		
		//持久化测试
		try {
			
			File fl = new File(comp ? "kv.dfl.bin" : "kv.bin");
			OutputStream out = new BufferedOutputStream(new FileOutputStream(fl));
			htbl.serialize(out, comp);
			out.close();
			out = null;
			System.out.printf("File size: %dB %s%n", fl.length(), comp ? "compressed" : "original");
			
			htbl = null;
			System.gc();
			InputStream in = new BufferedInputStream(new FileInputStream(fl));
			htbl = HashtableS.construct(in, comp);
			in.close();
			in = null;
			
			for(int i = 0; i < keys.length; i++)
				if(!htbl.get(keys[i]).equals(vals[i])) {
					System.err.printf("Key %d not consistent!%n", i);
					return;
				}
			System.out.println("Check pass.");
			System.out.println();
			
		} catch(IOException ex) {
			System.err.println(ex.getLocalizedMessage());
			System.err.println("IOException! Test failed.");
		}
		
	}
	
	public static void main(String[] args) {
		init();
		accessTest(16384, 32768);
		accessTest(65536, 131072);
		persistTest(32768, false);
		persistTest(32768, true);
	}
	
}
