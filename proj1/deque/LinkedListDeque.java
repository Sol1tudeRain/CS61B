package deque;

import net.sf.saxon.expr.ItemMappingFunction;

import java.security.PublicKey;

/** An LinkedListDeque is a list of items, which can traverse
 * both forwards and backwards */
public class LinkedListDeque<T> implements Deque<T> {
    private class Node{
        public T item;
        public Node prev;
        public Node next;
        public Node(T i){
            item=i;
        }
    }

    private Node first;
    private Node last;
    private int size;

    /** Creates an empty list */
    public LinkedListDeque(){
        first=null;
        last=null;
        size=0;
    }
    private Node addNode(T x){
        Node t=new Node(x);
        if(size==0){
            t.prev=t;
            t.next=t;
            first=t;
            last=t;
        }else {
            t.next=first;
            t.prev=last;
            last.next=t;
            first.prev=t;
        }
        size++;
        return t;
    }
    public void addFirst(T x){
        first=addNode(x);
    }

    public void addLast(T x){
        last=addNode(x);
    }

    public boolean isEmpty(){
        return size==0;
    }

    public int size(){
        return size;
    }

    public void printDeque(){
        Node t=first;
        int i;
        for(i=0;i<size;i++){
            System.out.print(t.item + " ");
            t=t.next;
        }
        System.out.print("\n");
    }

    /** If the argument is 0, removes the first node
    * If the argument is 1, removes the last node */
    private T removeNode(int x){
        if(size==0) {
            return null;
        }
        T t = null;
        if(x==0){
            t=first.item;
        } else if (x==1) {
            t=last.item;
        }

        if (size==1) {
            first=null;
            last=null;
        } else{
            if(x==0){
                first=first.next;
            } else if (x==1) {
                last=last.prev;
            }
            first.prev=last;
            last.next=first;
        }
        size--;
        return t;
    }
    public T removeFirst(){
        return removeNode(0);
    }

    public T removeLast(){
        return removeNode(1);
    }

    public T get(int index){
        Node t=first;
        int i;
        for(i=0;i<size;i++){
            if (i==index){
                return t.item;
            }
            t=t.next;
        }
        return null;
    }

    private T getRecursiveHelper(Node p,int index){
        Node pointer=p;
        if(index==0){
            return pointer.item;
        }else{
            return getRecursiveHelper(p.next,index-1);
        }
    }
    public T getRecursive(int index){
        if(index>size-1){
            return null;
        }

        return getRecursiveHelper(first,index);
    }

    private class LinkedListDequeIterator implements Iterator<T>{
        private int index;
        public LinkedListDequeIterator(){
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
        return new LinkedListDequeIterator();
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
        Iterator<T> iter=other.iterator();
        Node p=first;
        while (iter.hasNext()){
            if(p.item!=iter.next()){
                return false;
            }
            p=p.next;
        }
        return true;
    }
}