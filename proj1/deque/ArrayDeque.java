package deque;

import java.util.Iterator;
/** Array based list */
public class ArrayDeque<T> implements Deque<T>,Iterable<T>{
    private T[] items;
    private int size;
    private int first;
    private int last;
    /** Creates an empty list. */
    public ArrayDeque() {
        //If there is no item in the list, both first and last are -1
        items = (T[]) new Object[8];
        first=-1;
        last=-1;
        size=0;
    }

    public void resize(int capacity){
        T[] a=(T[]) new Object[capacity];
        if(first>last){
            System.arraycopy(items,first,a,0,items.length-first);
            System.arraycopy(items,0,a,items.length-first,last+1);
        } else if (first<last) {
            System.arraycopy(items,first,a,0,size);
        }
        first=0;
        last=first+size-1;
        items=a;
    }

    public void addFirst(T x){
        if(size== items.length){
            resize(size*2);
        }
        if(size==0){
            first=0;
            last=0;
        }else if(first==0){
            first= items.length-1;
        }else {
            first--;
        }
        items[first]=x;
        size++;
    }

    public void addLast(T x){
        if(size== items.length){
            resize(size*2);
        }
        if(size==0){
            first=0;
            last=0;
        }else if(last==items.length-1){
            last=0;
        }else {
            last++;
        }
        items[last]=x;
        size++;
    }

    public boolean isEmpty(){
        return size==0;
    }

    public int size(){
        return size;
    }

    public void printDeque(){
        int i;
        int t=first;
        for(i=size;i>0;i--){
            System.out.print(items[t] + " ");
            t++;
        }
        System.out.print("\n");
    }

    public T removeFirst(){
        if(size==0){
            return null;
        } else if (size==1) {
            size--;
            T t=items[first];
            first=-1;
            last=-1;
            return t;
        }

        T t=items[first];
        if(first== items.length-1){
            first=0;
        }else {
            first++;
        }
        size--;
        if(items.length>=16&&size<items.length/4){
            resize(items.length/2);
        }
        return t;
    }
    public T removeLast(){
        if(size==0){
            return null;
        }else if (size==1) {
            size--;
            T t=items[last];
            first=-1;
            last=-1;
            return t;
        }

        T t=items[last];
        if(last==0){
            last= items.length-1;
        }else {
            last--;
        }
        size--;
        if(items.length>=16&&size<items.length/4){
            resize(items.length/2);
        }
        return t;
    }
    public T get(int index){
        int i=0;
        int p=first;
        while(i<size){
            if(i==index){
                return items[p];
            }
            if(p== items.length-1){
                p=0;
            }else{
                p++;
            }
            i++;
        }
        return null;
    }

    private class ArrayDequeIterator implements Iterator<T>{
        private int index;

        public ArrayDequeIterator(){
            index=0;
        }

        public boolean hasNext(){
            return index < size;
        }

        public T next() {
            T returnItem = get(index);
            index++;
            return returnItem;
        }

    }

    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof Deque)) {
            return false;
        }
        Deque<T> other = (Deque<T>) o;
        if (other.size() != this.size()) {
            return false;
        }
        for(int i=0;i<size;i++){
            if(!(other.get(i).equals(get(i)))){
                return false;
            }
        }
        return true;
    }
}