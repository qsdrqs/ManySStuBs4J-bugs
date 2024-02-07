/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Common Public License 1.0 (http://opensource.org/licenses/cpl.php)
 *   which can be found in the file CPL.TXT at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich Dec 18, 2007 */

package clojure.lang;

import java.util.Collection;
import java.util.Iterator;

public abstract class APersistentVector extends AFn implements IPersistentVector, Iterable, Collection{
int _hash = -1;

public APersistentVector(IPersistentMap meta){
	super(meta);
}

public String toString(){
	return "<vector: - " + count() + " items>";
}

public ISeq seq(){
	if(count() > 0)
		return new Seq(this, 0);
	return null;
}

public ISeq rseq(){
	if(count() > 0)
		return new RSeq(this, count() - 1);
	return null;
}

static boolean doEquals(IPersistentVector v, Object obj){
	if(obj instanceof IPersistentVector)
		{
		IPersistentVector ma = (IPersistentVector) obj;
		if(ma.count() != v.count() || ma.hashCode() != v.hashCode())
			return false;
		for(int i = 0; i < v.count(); i++)
			{
			if(!RT.equal(v.nth(i), ma.nth(i)))
				return false;
			}
		}
	else
		{
		if(!(obj instanceof Sequential))
			return false;
		ISeq ms = ((IPersistentCollection) obj).seq();
		for(int i = 0; i < v.count(); i++, ms = ms.rest())
			{
			if(ms == null || !RT.equal(v.nth(i), ms.first()))
				return false;
			}
		if(ms != null)
			return false;
		}

	return true;

}

public boolean equals(Object obj){
	return doEquals(this, obj);
}

public int hashCode(){
	if(_hash == -1)
		{
		int hash = 0;
		for(int i = 0; i < count(); i++)
			{
			hash = RT.hashCombine(hash, RT.hash(nth(i)));
			}
		this._hash = hash;
		}
	return _hash;
}

public Object invoke(Object arg1) throws Exception{
	return nth(((Number) arg1).intValue());
}

public Iterator iterator(){
	//todo - something more efficient
	return new Iterator(){
		int i = 0;

		public boolean hasNext(){
			return i < count();
		}

		public Object next(){
			return nth(i++);
		}

		public void remove(){
			throw new UnsupportedOperationException();
		}
	};
}

public Object peek(){
	if(count() > 0)
		return nth(count() - 1);
	return null;
}

public boolean containsKey(Object key){
	if(!(key instanceof Number))
		return false;
	int i = ((Number) key).intValue();
	return i >= 0 && i < count();
}

public IMapEntry entryAt(Object key){
	if(key instanceof Number)
		{
		int i = ((Number) key).intValue();
		if(i >= 0 && i < count())
			return new MapEntry(key, nth(i));
		}
	return null;
}

public IPersistentVector assoc(Object key, Object val){
	if(key instanceof Number)
		{
		int i = ((Number) key).intValue();
		return assocN(i, val);
		}
	throw new IllegalAccessError("Key must be integer");
}

public Object valAt(Object key, Object notFound){
	if(key instanceof Number)
		{
		int i = ((Number) key).intValue();
		if(i >= 0 && i < count())
			return nth(i);
		}
	return notFound;
}

public Object valAt(Object key){
	return valAt(key, null);
}

// java.util.Collection implementation

public Object[] toArray(){
	return RT.seqToArray(seq());
}

public boolean add(Object o){
	throw new UnsupportedOperationException();
}

public boolean remove(Object o){
	throw new UnsupportedOperationException();
}

public boolean addAll(Collection c){
	throw new UnsupportedOperationException();
}

public void clear(){
	throw new UnsupportedOperationException();
}

public boolean retainAll(Collection c){
	throw new UnsupportedOperationException();
}

public boolean removeAll(Collection c){
	throw new UnsupportedOperationException();
}

public boolean containsAll(Collection c){
	for(Object o : c)
		{
		if(contains(o))
			return true;
		}
	return false;
}

public Object[] toArray(Object[] a){
	if(a.length >= count())
		{
		ISeq s = seq();
		for(int i = 0; s != null; ++i, s = s.rest())
			{
			a[i] = s.first();
			}
		if(a.length >= count())
			a[count()] = null;
		return a;
		}
	else
		return toArray();
}

public int size(){
	return count();
}

public boolean isEmpty(){
	return count() == 0;
}

public boolean contains(Object o){
	for(ISeq s = seq(); s != null; s = s.rest())
		{
		if(RT.equal(s.first(), o))
			return true;
		}
	return false;
}

public int length(){
	return count();
}

static class Seq extends ASeq implements IndexedSeq{
	//todo - something more efficient
	final IPersistentVector v;
	final int i;


	public Seq(IPersistentVector v, int i){
		this.v = v;
		this.i = i;
	}

	Seq(IPersistentMap meta, IPersistentVector v, int i){
		super(meta);
		this.v = v;
		this.i = i;
	}

	public Object first(){
		return v.nth(i);
	}

	public ISeq rest(){
		if(i + 1 < v.count())
			return new PersistentVector.Seq(v, i + 1);
		return null;
	}

	public int index(){
		return i;
	}

	public int count(){
		return v.count() - i;
	}

	public PersistentVector.Seq withMeta(IPersistentMap meta){
		return new PersistentVector.Seq(meta, v, i);
	}

	public Object reduce(IFn f) throws Exception{
		Object ret = v.nth(i);
		for(int x = i + 1; x < v.count(); x++)
			ret = f.invoke(ret, x);
		return ret;
	}

	public Object reduce(IFn f, Object start) throws Exception{
		Object ret = f.invoke(start, v.nth(i));
		for(int x = i + 1; x < v.count(); x++)
			ret = f.invoke(ret, x);
		return ret;
	}
}

static class RSeq extends ASeq implements IndexedSeq{
	final IPersistentVector v;
	final int i;

	RSeq(IPersistentVector vector, int i){
		this.v = vector;
		this.i = i;
	}

	RSeq(IPersistentMap meta, IPersistentVector v, int i){
		super(meta);
		this.v = v;
		this.i = i;
	}

	public Object first(){
		return v.nth(i);
	}

	public ISeq rest(){
		if(i > 0)
			return new PersistentVector.RSeq(v, i - 1);
		return null;
	}

	public int index(){
		return i;
	}

	public int count(){
		return i + 1;
	}

	public PersistentVector.RSeq withMeta(IPersistentMap meta){
		return new PersistentVector.RSeq(meta, v, i);
	}
}

static class SubVector extends APersistentVector{
	final IPersistentVector v;
	final int start;
	final int end;


	public SubVector(IPersistentMap meta, IPersistentVector v, int start, int end){
		super(meta);
		this.v = v;
		this.start = start;
		this.end = end;
	}

	public Object nth(int i){
		if(start + i >= end)
			throw new IndexOutOfBoundsException();
		return v.nth(start + i);
	}

	public IPersistentVector assocN(int i, Object val){
		if(start + i > end)
			throw new IndexOutOfBoundsException();
		else if(start + i == end)
			return cons(val);
		return new SubVector(_meta, v.assocN(start + i, val), start, end);
	}

	public int count(){
		return end - start;
	}

	public IPersistentVector cons(Object o){
		return new SubVector(_meta, v.assocN(end, o), start, end + 1);
	}

	public IPersistentStack pop(){
		if(end - 1 == start)
			{
			return PersistentVector.EMPTY;
			}
		return new SubVector(_meta, v, start, end - 1);
	}

	public SubVector withMeta(IPersistentMap meta){
		if(meta == _meta)
			return this;
		return new SubVector(meta, v, start, end);
	}
}
}