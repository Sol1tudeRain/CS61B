package bstmap;

public class MyUtils{

    /** Insert a node associating the specified key with the
     *  specified value into the tree. */
    public static <K extends Comparable<K>,V> BSTNode<K,V> insert(BSTNode<K,V> tree, K key, V value){
        if(tree==null){
            return new BSTNode<>(key,value);
        }
        if(key.compareTo(tree.key)<0){
            tree.left=insert(tree.left,key,value);
        }else{
            tree.right=insert(tree.right,key,value);
        }
        return tree;
    }

    /** Finds the value to which the specified key is mapped.
     *  Returns null if it doesn't exist. */
    public static <K extends Comparable<K>,V> V find(BSTNode<K,V> tree, K key){
        if(tree==null){
            return null;
        }
        if(key.compareTo(tree.key)==0){
            return tree.value;
        } else if (key.compareTo(tree.key)<0) {
            return find(tree.left,key);
        } else {
            return find(tree.right,key);
        }
    }

}
