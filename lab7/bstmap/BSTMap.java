package bstmap;

import java.util.Iterator;
import java.util.Set;

import static bstmap.MyUtils.find;
import static bstmap.MyUtils.insert;


public class BSTMap<K extends Comparable<K>,V> implements Map61B<K,V>{

    private int size;
    public BSTNode<K,V> root;
    public BSTMap(){
        this.size=0;
        this.root=null;
    }

    public void clear() {
        this.root=null;
        this.size=0;
    }

    public boolean containsKey(K key) {
        return find(this.root,key)!=null;
    }

    /**
     *  Returns the value to which the specified key is mapped, or null if this
     *  map contains no mapping for the key.
     */
    public V get(K key) {
        return find(this.root,key);
    }

    public int size() {
        return this.size;
    }

    public void put(K key, V value) {
        if(!containsKey(key)){
            this.size+=1;
            this.root=insert(this.root,key,value);
        }
    }

    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    public V remove(K key) {
        throw new UnsupportedOperationException();
    }

    public V remove(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public Iterator<K> iterator() {
        throw new UnsupportedOperationException();
    }

}
