/**
 Copyright (c) 2007-2008, Rich Hickey
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the following
   disclaimer in the documentation and/or other materials provided
   with the distribution.

 * Neither the name of Clojure nor the names of its contributors
   may be used to endorse or promote products derived from this
   software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 **/

package clojure.lang;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/*
 A persistent rendition of Phil Bagwell's Hash Array Mapped Trie

 Uses path copying for persistence
 HashCollision leaves vs. extended hashing
 Node polymorphism vs. conditionals
 No sub-tree pools or root-resizing
 Any errors are my own
 */

public class PersistentHashMap2 extends APersistentMap implements IEditableCollection {

final int count;
final INode root;
final boolean hasNull;
final Object nullValue;

final public static PersistentHashMap2 EMPTY = new PersistentHashMap2(0, null, false, null);
final private static Object NOT_FOUND = new Object();

static public IPersistentMap create(Map other){
	IPersistentMap ret = EMPTY;
	for(Object o : other.entrySet())
		{
		Map.Entry e = (Entry) o;
		ret = ret.assoc(e.getKey(), e.getValue());
		}
	return ret;
}

/*
 * @param init {key1,val1,key2,val2,...}
 */
public static PersistentHashMap2 create(Object... init){
	IPersistentMap ret = EMPTY;
	for(int i = 0; i < init.length; i += 2)
		{
		ret = ret.assoc(init[i], init[i + 1]);
		}
	return (PersistentHashMap2) ret;
}

public static PersistentHashMap2 create(List init){
	IPersistentMap ret = EMPTY;
	for(Iterator i = init.iterator(); i.hasNext();)
		{
		Object key = i.next();
		if(!i.hasNext())
			throw new IllegalArgumentException(String.format("No value supplied for key: %s", key));
		Object val = i.next();
		ret = ret.assoc(key, val);
		}
	return (PersistentHashMap2) ret;
}

static public PersistentHashMap2 create(ISeq items){
	IPersistentMap ret = EMPTY;
	for(; items != null; items = items.next().next())
		{
		if(items.next() == null)
			throw new IllegalArgumentException(String.format("No value supplied for key: %s", items.first()));
		ret = ret.assoc(items.first(), RT.second(items));
		}
	return (PersistentHashMap2) ret;
}

/*
 * @param init {key1,val1,key2,val2,...}
 */
public static PersistentHashMap2 create(IPersistentMap meta, Object... init){
	IPersistentMap ret = EMPTY.withMeta(meta);
	for(int i = 0; i < init.length; i += 2)
		{
		ret = ret.assoc(init[i], init[i + 1]);
		}
	return (PersistentHashMap2) ret;
}

PersistentHashMap2(int count, INode root, boolean hasNull, Object nullValue){
	this.count = count;
	this.root = root;
	this.hasNull = hasNull;
	this.nullValue = nullValue;
}

public PersistentHashMap2(IPersistentMap meta, int count, INode root, boolean hasNull, Object nullValue){
	super(meta);
	this.count = count;
	this.root = root;
	this.hasNull = hasNull;
	this.nullValue = nullValue;
}

public boolean containsKey(Object key){
	if(key == null)
		return hasNull;
	return (root != null) ? root.find(0, Util.hash(key), key, NOT_FOUND) != NOT_FOUND : false;
}

public IMapEntry entryAt(Object key){
	if(key == null)
		return hasNull ? new MapEntry(null, nullValue) : null;
	return (root != null) ? root.find(0, Util.hash(key), key) : null;
}

public IPersistentMap assoc(Object key, Object val){
	if(key == null) {
		if(hasNull && val == nullValue)
			return this;
		return new PersistentHashMap2(meta(), hasNull ? count : count + 1, root, true, val);
	}
	Box addedLeaf = new Box(null);
	INode newroot = (root == null ? BitmapIndexedNode.EMPTY : root) 
			.assoc(0, Util.hash(key), key, val, addedLeaf);
	if(newroot == root)
		return this;
	return new PersistentHashMap2(meta(), addedLeaf.val == null ? count : count + 1, newroot, hasNull, nullValue);
}

public Object valAt(Object key, Object notFound){
	if(key == null)
		return hasNull ? nullValue : notFound;
	return root != null ? root.find(0, Util.hash(key), key, notFound) : notFound;
}

public Object valAt(Object key){
	return valAt(key, null);
}

public IPersistentMap assocEx(Object key, Object val) throws Exception{
	if(containsKey(key))
		throw new Exception("Key already present");
	return assoc(key, val);
}

public IPersistentMap without(Object key){
	if(key == null)
		return hasNull ? new PersistentHashMap2(meta(), count - 1, root, false, null) : this;
	if(root == null)
		return this;
	INode newroot = root.without(0, Util.hash(key), key);
	if(newroot == root)
		return this;
	return new PersistentHashMap2(meta(), count - 1, newroot, hasNull, nullValue); 
}

public Iterator iterator(){
	return new SeqIterator(seq());
}

public int count(){
	return count;
}

public ISeq seq(){
	ISeq s = root != null ? root.nodeSeq() : null; 
	return hasNull ? new Cons(new MapEntry(null, nullValue), s) : s;
}

public IPersistentCollection empty(){
	return EMPTY.withMeta(meta());	
}

static int mask(int hash, int shift){
	//return ((hash << shift) >>> 27);// & 0x01f;
	return (hash >>> shift) & 0x01f;
}

public PersistentHashMap2 withMeta(IPersistentMap meta){
	return new PersistentHashMap2(meta, count, root, hasNull, nullValue);
}

public TransientHashMap asTransient() {
	return new TransientHashMap(this);
}

static final class TransientHashMap extends ATransientMap {
	AtomicReference<Thread> edit;
	INode root;
	int count;
	boolean hasNull;
	Object nullValue;
	
	
	TransientHashMap(PersistentHashMap2 m) {
		this(new AtomicReference<Thread>(Thread.currentThread()), m.root, m.count, m.hasNull, m.nullValue);
	}
	
	TransientHashMap(AtomicReference<Thread> edit, INode root, int count, boolean hasNull, Object nullValue) {
		this.edit = edit;
		this.root = root; 
		this.count = count; 
		this.hasNull = hasNull;
		this.nullValue = nullValue;
	}

	ITransientMap doAssoc(Object key, Object val) {
		if (key == null) {
			if (this.nullValue != val)
				this.nullValue = val;
			if (!hasNull) {
				this.count++;
				this.hasNull = true;
			}
			return this;
		}
		Box addedLeaf = new Box(null);
		INode n = (root == null ? BitmapIndexedNode.EMPTY : root)
			.assoc(edit, 0, Util.hash(key), key, val, addedLeaf);
		if (n != this.root)
			this.root = n; 
		if(addedLeaf.val != null) this.count++;
		return this;
	}

	ITransientMap doWithout(Object key) {
		if (key == null) {
			if (!hasNull) return this;
			hasNull = false;
			nullValue = null;
			this.count--;
			return this;
		}
		if (root == null) return this;
		Box removedLeaf = new Box(null);
		INode n = root.without(edit, 0, Util.hash(key), key, removedLeaf);
		if (n != root)
			this.root = n;
		if(removedLeaf.val != null) this.count--;
		return this;
	}

	IPersistentMap doPersistent() {
		edit.set(null);
		return new PersistentHashMap2(count, root, hasNull, nullValue);
	}

	Object doValAt(Object key, Object notFound) {
		return root.find(0, Util.hash(key), key, notFound);
	}

	int doCount() {
		return count;
	}
	
	void ensureEditable(){
		Thread owner = edit.get();
		if(owner == Thread.currentThread())
			return;
		if(owner != null)
			throw new IllegalAccessError("Mutable used by non-owner thread");
		throw new IllegalAccessError("Mutable used after immutable call");
	}
}

static interface INode{
	INode assoc(int shift, int hash, Object key, Object val, Box addedLeaf);

	INode without(int shift, int hash, Object key);

	IMapEntry find(int shift, int hash, Object key);

	Object find(int shift, int hash, Object key, Object notFound);

	ISeq nodeSeq();

	INode assoc(AtomicReference<Thread> edit, int shift, int hash, Object key, Object val, Box addedLeaf);

	INode without(AtomicReference<Thread> edit, int shift, int hash, Object key, Box removedLeaf);
}

final static class ArrayNode implements INode{
	final INode[] array;
	final AtomicReference<Thread> edit;

	ArrayNode(AtomicReference<Thread> edit, INode[] array){
		this.array = array;
		this.edit = edit;
	}

	public INode assoc(int shift, int hash, Object key, Object val, Box addedLeaf){
		int idx = mask(hash, shift);
		INode node = array[idx];
		if(node == null)
			return new ArrayNode(null, cloneAndSet(array, idx, BitmapIndexedNode.EMPTY.assoc(shift + 5, hash, key, val, addedLeaf)));			
		INode n = node.assoc(shift + 5, hash, key, val, addedLeaf);
		if(n == node)
			return this;
		return new ArrayNode(null, cloneAndSet(array, idx, n));
	}

	public INode without(int shift, int hash, Object key){
		int idx = mask(hash, shift);
		INode node = array[idx];
		if(node == null)
			return this;
		INode n = node.without(shift + 5, hash, key);
		if(n == node)
			return this;
		// TODO: shrink if underflow
		return new ArrayNode(null, cloneAndSet(array, idx, n));
	}

	public IMapEntry find(int shift, int hash, Object key){
		int idx = mask(hash, shift);
		INode node = array[idx];
		if(node == null)
			return null;
		return node.find(shift + 5, hash, key); 
	}

	public Object find(int shift, int hash, Object key, Object notFound){
		int idx = mask(hash, shift);
		INode node = array[idx];
		if(node == null)
			return notFound;
		return node.find(shift + 5, hash, key, notFound); 
	}
	
	public ISeq nodeSeq(){
		return Seq.create(array);
	}

	ArrayNode ensureEditable(AtomicReference<Thread> edit){
		if(this.edit == edit)
			return this;
		return new ArrayNode(edit, this.array.clone());
	}

	public INode assoc(AtomicReference<Thread> edit, int shift, int hash, Object key, Object val, Box addedLeaf){
		int idx = mask(hash, shift);
		INode node = array[idx];
		if(node == null) {
			ArrayNode editable = ensureEditable(edit);
			editable.array[idx] = BitmapIndexedNode.EMPTY.assoc(edit, shift + 5, hash, key, val, addedLeaf);
			return editable;
		}
		INode n = node.assoc(edit, shift + 5, hash, key, val, addedLeaf);
		if(n == node)
			return this;
		ArrayNode editable = ensureEditable(edit);
		editable.array[idx] = n;
		return editable;
	}	

	public INode without(AtomicReference<Thread> edit, int shift, int hash, Object key, Box removedLeaf){
		return null;
//		int idx = mask(hash, shift);
//		INode n = nodes[idx].without(edit, null, hash, key, removedLeaf);
//		if(n != nodes[idx])
//			{
//			if(n == null)
//				{
//				INode[] newnodes = new INode[nodes.length - 1];
//				System.arraycopy(nodes, 0, newnodes, 0, idx);
//				System.arraycopy(nodes, idx + 1, newnodes, idx, nodes.length - (idx + 1));
//				return new BitmapIndexedNode(edit, ~bitpos(hash, shift), newnodes, shift);
//				}
//			ArrayNode node = ensureEditable(edit);
//			node.nodes[idx] = n;
//			return node;
//			}
//		return this;
	}
	
	static class Seq extends ASeq {
		final INode[] nodes;
		final int i;
		final ISeq s; 
		
		static ISeq create(INode[] nodes) {
			return create(null, nodes, 0, null);
		}
		
		private static ISeq create(IPersistentMap meta, INode[] nodes, int i, ISeq s) {
			if (s != null)
				return new Seq(meta, nodes, i, s);
			for(int j = i; j < nodes.length; j++)
				if (nodes[j] != null)
					return new Seq(meta, nodes, j + 1, nodes[j].nodeSeq());
			return null;
		}
		
		private Seq(IPersistentMap meta, INode[] nodes, int i, ISeq s) {
			super(meta);
			this.nodes = nodes;
			this.i = i;
			this.s = s;
		}

		public Obj withMeta(IPersistentMap meta) {
			return new Seq(meta, nodes, i, s);
		}

		public Object first() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ISeq next() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}

final static class BitmapIndexedNode implements INode{
	static final BitmapIndexedNode EMPTY = new BitmapIndexedNode(null, 0, new Object[0]);
	
	int bitmap;
	Object[] array;
	final AtomicReference<Thread> edit;

	final int index(int bit){
		return Integer.bitCount(bitmap & (bit - 1));
	}

	BitmapIndexedNode(AtomicReference<Thread> edit, int bitmap, Object[] array){
		this.bitmap = bitmap;
		this.array = array;
		this.edit = edit;
	}

	public INode assoc(int shift, int hash, Object key, Object val, Box addedLeaf){
		int bit = bitpos(hash, shift);
		int idx = index(bit);
		if((bitmap & bit) != 0) {
			Object keyOrNull = array[2*idx];
			Object valOrNode = array[2*idx+1];
			if(keyOrNull == null) {
				INode n = ((INode) valOrNode).assoc(shift + 5, hash, key, val, addedLeaf);
				if(n == valOrNode)
					return this;
				return new BitmapIndexedNode(null, bitmap, cloneAndSet(array, 2*idx+1, n));
			} 
			if(Util.equals(key, keyOrNull)) {
				if(val == valOrNode)
					return this;
				return new BitmapIndexedNode(null, bitmap, cloneAndSet(array, 2*idx+1, val));
			} 
			addedLeaf.val = val;
			return new BitmapIndexedNode(null, bitmap, 
					cloneAndSet(array, 
							2*idx, null, 
							2*idx+1, createNode(shift + 5, keyOrNull, valOrNode, hash, key, val)));
		} else {
			int n = Integer.bitCount(bitmap);
			if(n >= 16) {
				INode[] nodes = new INode[32];
				int jdx = mask(hash, shift);
				nodes[jdx] = EMPTY.assoc(shift + 5, hash, key, val, addedLeaf);  
				int j = 0;
				for(int i = 0; i < 32; i++)
					if(((bitmap >>> i) & 1) != 0) {
						if (array[j] == null)
							nodes[i] = (INode) array[j+1];
						else
							nodes[i] = EMPTY.assoc(shift + 5, Util.hash(array[j]), array[j], array[j+1], addedLeaf);
						j += 2;
					}
				return new ArrayNode(null, nodes);
			} else {
				Object[] newArray = new Object[2*(n+1)];
				System.arraycopy(array, 0, newArray, 0, 2*idx);
				newArray[2*idx] = key;
				addedLeaf.val = newArray[2*idx+1] = val;
				System.arraycopy(array, 2*idx, newArray, 2*(idx+1), 2*(n-idx));
				return new BitmapIndexedNode(null, bitmap | bit, newArray);
			}
		}
	}

	public INode without(int shift, int hash, Object key){
		int bit = bitpos(hash, shift);
		if((bitmap & bit) == 0)
			return this;
		int idx = index(bit);
		Object keyOrNull = array[2*idx];
		Object valOrNode = array[2*idx+1];
		if(keyOrNull == null) {
			INode n = ((INode) valOrNode).without(shift + 5, hash, key);
			if (n == valOrNode)
				return this;
			if (n != null)
				return new BitmapIndexedNode(null, bitmap, cloneAndSet(array, 2*idx+1, n));
			if (bitmap == bit) 
				return null;
			return new BitmapIndexedNode(null, bitmap ^ bit, removePair(array, idx));
		}
		if(Util.equals(key, keyOrNull))
			// TODO: collapse
			return new BitmapIndexedNode(null, bitmap ^ bit, removePair(array, idx));
		return this;
	}
	
	public IMapEntry find(int shift, int hash, Object key){
		int bit = bitpos(hash, shift);
		if((bitmap & bit) == 0)
			return null;
		int idx = index(bit);
		Object keyOrNull = array[2*idx];
		Object valOrNode = array[2*idx+1];
		if(keyOrNull == null)
			return ((INode) valOrNode).find(shift + 5, hash, key);
		if(Util.equals(key, keyOrNull))
			return new MapEntry(keyOrNull, valOrNode);
		return null;
	}

	public Object find(int shift, int hash, Object key, Object notFound){
		int bit = bitpos(hash, shift);
		if((bitmap & bit) == 0)
			return notFound;
		int idx = index(bit);
		Object keyOrNull = array[2*idx];
		Object valOrNode = array[2*idx+1];
		if(keyOrNull == null)
			return ((INode) valOrNode).find(shift + 5, hash, key, notFound);
		if(Util.equals(key, keyOrNull))
			return valOrNode;
		return notFound;
	}

	public ISeq nodeSeq(){
		return NodeSeq.create(array);
	}

	BitmapIndexedNode ensureEditable(AtomicReference<Thread> edit){
		if(this.edit == edit)
			return this;
		int n = Integer.bitCount(bitmap);
		Object[] newArray = new Object[n >= 0 ? 2*(n+1) : 4]; // make room for next assoc
		System.arraycopy(array, 0, newArray, 0, 2*n);
		return new BitmapIndexedNode(edit, bitmap, newArray);
	}
	
	BitmapIndexedNode editAndSet(int i, Object a) {
		BitmapIndexedNode editable = ensureEditable(edit);
		editable.array[i] = a;
		return editable;
	}

	BitmapIndexedNode editAndSet(int i, Object a, int j, Object b) {
		BitmapIndexedNode editable = ensureEditable(edit);
		editable.array[i] = a;
		editable.array[j] = b;
		return editable;
	}

	public INode assoc(AtomicReference<Thread> edit, int shift, int hash, Object key, Object val, Box addedLeaf){
		int bit = bitpos(hash, shift);
		int idx = index(bit);
		if((bitmap & bit) != 0) {
			Object keyOrNull = array[2*idx];
			Object valOrNode = array[2*idx+1];
			if(keyOrNull == null) {
				INode n = ((INode) valOrNode).assoc(edit, shift + 5, hash, key, val, addedLeaf);
				if(n == valOrNode)
					return this;
				return editAndSet(2*idx+1, n);
			} 
			if(Util.equals(key, keyOrNull)) {
				if(val == valOrNode)
					return this;
				return editAndSet(2*idx+1, val);
			} 
			addedLeaf.val = val;
			return editAndSet(2*idx, null, 2*idx+1, 
					createNode(edit, shift + 5, keyOrNull, valOrNode, hash, key, val)); 
		} else {
			int n = Integer.bitCount(bitmap);
			if(n*2 < array.length) {
				addedLeaf.val = val;
				BitmapIndexedNode editable = ensureEditable(edit);
				System.arraycopy(editable.array, 2*idx, editable.array, 2*(idx+1), 2*(n-idx));
				editable.array[2*idx] = key;
				editable.array[2*idx+1] = val;
				editable.bitmap |= bit;
				return editable;
			}
			if(n >= 16) {
				INode[] nodes = new INode[32];
				int jdx = mask(hash, shift);
				nodes[jdx] = EMPTY.assoc(edit, shift + 5, hash, key, val, addedLeaf);  
				int j = 0;
				for(int i = 0; i < 32; i++)
					if(((bitmap >>> i) & 1) != 0) {
						if (array[j] == null)
							nodes[i] = (INode) array[j+1];
						else
							nodes[i] = EMPTY.assoc(edit, shift + 5, Util.hash(array[j]), array[j], array[j+1], addedLeaf);
						j += 2;
					}
				return new ArrayNode(edit, nodes);
			} else {
				Object[] newArray = new Object[2*(n+2)];
				System.arraycopy(array, 0, newArray, 0, 2*idx);
				newArray[2*idx] = key;
				addedLeaf.val = newArray[2*idx+1] = val;
				System.arraycopy(array, 2*idx, newArray, 2*(idx+1), 2*(n-idx));
				BitmapIndexedNode editable = ensureEditable(edit);
				editable.array = newArray;
				editable.bitmap |= bit;
				return editable;
			}
		}
	}

	public INode without(AtomicReference<Thread> edit, int shift, int hash, Object key, Box removedLeaf){
		return null;
		// TODO
//		int bit = bitpos(hash, shift);
//		if((bitmap & bit) != 0)
//			{
//			int idx = index(bit);
//			INode n = nodes[idx].without(edit, null, hash, key, removedLeaf);
//			if(n != nodes[idx])
//				{
//				if(n == null)
//					{
//					if(bitmap == bit)
//						return null;
//					INode[] newnodes = new INode[nodes.length - 1];
//					System.arraycopy(nodes, 0, newnodes, 0, idx);
//					System.arraycopy(nodes, idx + 1, newnodes, idx, nodes.length - (idx + 1));
//					return new BitmapIndexedNode(edit, bitmap & ~bit, newnodes, shift);
//					}
//				BitmapIndexedNode node = ensureEditable(edit);
//				node.nodes[idx] = n;
//				return node;
//				}
//			}
//		return this;
	}
}

final static class HashCollisionNode implements INode{

	final int hash;
	Object[] array;
	final AtomicReference<Thread> edit;

	HashCollisionNode(AtomicReference<Thread> edit, int hash, Object... array){
		this.edit = edit;
		this.hash = hash;
		this.array = array;
	}

	public INode assoc(int shift, int hash, Object key, Object val, Box addedLeaf){
		if(hash == this.hash) {
			int idx = findIndex(key);
			if(idx != -1) {
				if(array[idx + 1] == val)
					return this;
				return new HashCollisionNode(null, hash, cloneAndSet(array, idx + 1, val));
			}
			Object[] newArray = new Object[array.length + 2];
			System.arraycopy(array, 0, newArray, 0, array.length);
			newArray[array.length] = key;
			newArray[array.length + 1] = val;
			return new HashCollisionNode(edit, hash, newArray);
		}
		// nest it in a bitmap node
		return new BitmapIndexedNode(null, bitpos(this.hash, shift), new Object[] {this})
			.assoc(shift, hash, key, val, addedLeaf);
	}

	public INode without(int shift, int hash, Object key){
		int idx = findIndex(key);
		if(idx == -1)
			return this;
		if(array.length == 2)
			return null;
		return new HashCollisionNode(null, hash, removePair(array, idx));
	}

	public IMapEntry find(int shift, int hash, Object key){
		int idx = findIndex(key);
		if(idx < 0)
			return null;
		if(Util.equals(key, array[idx]))
			return new MapEntry(array[idx], array[idx+1]);
		return null;
	}

	public Object find(int shift, int hash, Object key, Object notFound){
		int idx = findIndex(key);
		if(idx < 0)
			return notFound;
		if(Util.equals(key, array[idx]))
			return array[idx+1];
		return notFound;
	}

	public ISeq nodeSeq(){
		return NodeSeq.create(array);
	}

	public int findIndex(Object key){
		for(int i = 0; i < array.length; i+=2)
			{
			if(Util.equals(key, array[i]))
				return i;
			}
		return -1;
	}

	HashCollisionNode ensureEditable(AtomicReference<Thread> edit){
		if(this.edit == edit)
			return this;
		return new HashCollisionNode(edit, hash, array);
	}

	HashCollisionNode ensureEditable(AtomicReference<Thread> edit, Object[] array){
		if(this.edit == edit) {
			this.array = array;
			return this;
		}
		return new HashCollisionNode(edit, hash, array);
	}

	HashCollisionNode editAndSet(int i, Object a) {
		HashCollisionNode editable = ensureEditable(edit);
		editable.array[i] = a;
		return editable;
	}

	HashCollisionNode editAndSet(int i, Object a, int j, Object b) {
		HashCollisionNode editable = ensureEditable(edit);
		editable.array[i] = a;
		editable.array[j] = b;
		return editable;
	}


	public INode assoc(AtomicReference<Thread> edit, int shift, int hash, Object key, Object val, Box addedLeaf){
		if(hash == this.hash) {
			int idx = findIndex(key);
			if(idx != -1) {
				if(array[idx + 1] == val)
					return this;
				return editAndSet(idx+1, val); 
			}
			Object[] newArray = new Object[array.length + 2];
			System.arraycopy(array, 0, newArray, 0, array.length);
			newArray[array.length] = key;
			newArray[array.length + 1] = val;
			return ensureEditable(edit, newArray);
		}
		// nest it in a bitmap node
		return new BitmapIndexedNode(edit, bitpos(this.hash, shift), new Object[] {this})
			.assoc(edit, shift, hash, key, val, addedLeaf);
	}	

	public INode without(AtomicReference<Thread> edit, int shift, int hash, Object key, Box removedLeaf){
		return null;
		// TODO
//		int idx = findIndex(hash, key);
//		if(idx == -1)
//			return this;
//		removedLeaf.val = leaves[idx];
//		if(leaves.length == 2)
//			return idx == 0 ? leaves[1] : leaves[0];
//		IMapEntry[] newLeaves = new IMapEntry[leaves.length - 1];
//		System.arraycopy(leaves, 0, newLeaves, 0, idx);
//		System.arraycopy(leaves, idx + 1, newLeaves, idx, leaves.length - (idx + 1));
//		return new HashCollisionNode(edit, hash, newLeaves);
	}
}

/*
public static void main(String[] args){
	try
		{
		ArrayList words = new ArrayList();
		Scanner s = new Scanner(new File(args[0]));
		s.useDelimiter(Pattern.compile("\\W"));
		while(s.hasNext())
			{
			String word = s.next();
			words.add(word);
			}
		System.out.println("words: " + words.size());
		IPersistentMap map = PersistentHashMap.EMPTY;
		//IPersistentMap map = new PersistentTreeMap();
		//Map ht = new Hashtable();
		Map ht = new HashMap();
		Random rand;

		System.out.println("Building map");
		long startTime = System.nanoTime();
		for(Object word5 : words)
			{
			map = map.assoc(word5, word5);
			}
		rand = new Random(42);
		IPersistentMap snapshotMap = map;
		for(int i = 0; i < words.size() / 200; i++)
			{
			map = map.without(words.get(rand.nextInt(words.size() / 2)));
			}
		long estimatedTime = System.nanoTime() - startTime;
		System.out.println("count = " + map.count() + ", time: " + estimatedTime / 1000000);

		System.out.println("Building ht");
		startTime = System.nanoTime();
		for(Object word1 : words)
			{
			ht.put(word1, word1);
			}
		rand = new Random(42);
		for(int i = 0; i < words.size() / 200; i++)
			{
			ht.remove(words.get(rand.nextInt(words.size() / 2)));
			}
		estimatedTime = System.nanoTime() - startTime;
		System.out.println("count = " + ht.size() + ", time: " + estimatedTime / 1000000);

		System.out.println("map lookup");
		startTime = System.nanoTime();
		int c = 0;
		for(Object word2 : words)
			{
			if(!map.contains(word2))
				++c;
			}
		estimatedTime = System.nanoTime() - startTime;
		System.out.println("notfound = " + c + ", time: " + estimatedTime / 1000000);
		System.out.println("ht lookup");
		startTime = System.nanoTime();
		c = 0;
		for(Object word3 : words)
			{
			if(!ht.containsKey(word3))
				++c;
			}
		estimatedTime = System.nanoTime() - startTime;
		System.out.println("notfound = " + c + ", time: " + estimatedTime / 1000000);
		System.out.println("snapshotMap lookup");
		startTime = System.nanoTime();
		c = 0;
		for(Object word4 : words)
			{
			if(!snapshotMap.contains(word4))
				++c;
			}
		estimatedTime = System.nanoTime() - startTime;
		System.out.println("notfound = " + c + ", time: " + estimatedTime / 1000000);
		}
	catch(FileNotFoundException e)
		{
		e.printStackTrace();
		}

}
*/

private static INode[] cloneAndSet(INode[] array, int i, INode a) {
	INode[] clone = array.clone();
	clone[i] = a;
	return clone;
}

private static Object[] cloneAndSet(Object[] array, int i, Object a) {
	Object[] clone = array.clone();
	clone[i] = a;
	return clone;
}

private static Object[] cloneAndSet(Object[] array, int i, Object a, int j, Object b) {
	Object[] clone = array.clone();
	clone[i] = a;
	clone[j] = b;
	return clone;
}

private static Object[] removePair(Object[] array, int i) {
	Object[] newArray = new Object[array.length - 2];
	System.arraycopy(array, 0, newArray, 0, 2*i);
	System.arraycopy(array, 2*(i+1), newArray, 2*i, newArray.length - 2*i);
	return newArray;
}

private static INode createNode(int shift, Object key1, Object val1, int key2hash, Object key2, Object val2) {
	int key1hash = Util.hash(key1);
	if(key1hash == key2hash)
		return new HashCollisionNode(null, key1hash, new Object[] {key1, val1, key2, val2});
	// TODO: optimize;
	Box _ = new Box(null);
	return BitmapIndexedNode.EMPTY
		.assoc(shift, key1hash, key1, val1, _)
		.assoc(shift, key2hash, key2, val2, _);
}

private static INode createNode(AtomicReference<Thread> edit, int shift, Object key1, Object val1, int key2hash, Object key2, Object val2) {
	int key1hash = Util.hash(key1);
	if(key1hash == key2hash)
		return new HashCollisionNode(null, key1hash, new Object[] {key1, val1, key2, val2});
	Box _ = new Box(null);
	return BitmapIndexedNode.EMPTY
		.assoc(edit, shift, key1hash, key1, val1, _)
		.assoc(edit, shift, key2hash, key2, val2, _);
}

private static int bitpos(int hash, int shift){
	return 1 << mask(hash, shift);
}

static final class NodeSeq extends ASeq {
	final Object[] array;
	final int i;
	final ISeq s;
	
	NodeSeq(Object[] array, int i) {
		this(null, array, i, null);
	}

	static ISeq create(Object[] array) {
		return create(array, 0, null);
	}

	private static ISeq create(Object[] array, int i, ISeq s) {
		if(s != null)
			return new NodeSeq(null, array, i, s);
		for(int j = i; j < array.length; j+=2) {
			if(array[j] != null)
				return new NodeSeq(null, array, j, null);
			INode node = (INode) array[j+1];
			if (node != null) {
				ISeq nodeSeq = node.nodeSeq();
				if(nodeSeq != null)
					return new NodeSeq(null, array, j + 2, nodeSeq);
			}
		}
		return null;
	}
	
	NodeSeq(IPersistentMap meta, Object[] array, int i, ISeq s) {
		super(meta);
		this.array = array;
		this.i = i;
		this.s = s;
	}

	public Obj withMeta(IPersistentMap meta) {
		return new NodeSeq(meta, array, i, s);
	}

	public Object first() {
		if(s != null)
			return s.first();
		return new MapEntry(array[i], array[i+1]);
	}

	public ISeq next() {
		if(s != null)
			return create(array, i, s.next());
		return create(array, i + 2, null);
	}
}

}