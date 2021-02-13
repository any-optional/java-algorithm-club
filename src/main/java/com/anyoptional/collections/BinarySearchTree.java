package com.anyoptional.collections;

import com.anyoptional.lang.Nullable;
import com.anyoptional.lang.VisibleForTesting;
import com.anyoptional.util.Assert;
import com.anyoptional.util.Comparators;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @apiNote BinarySearchTree do not permit null key.
 */
public class BinarySearchTree<K, V> implements Iterable<Entry<K, V>> {

    private int _size = 0;

    @Nullable
    @VisibleForTesting
    BinaryNode<K, V> _root;

    @Nullable
    private final Comparator<? super K> _comparator;

    public BinarySearchTree() {
        _comparator = null;
    }

    public BinarySearchTree(Comparator<? super K> comparator) {
        Assert.notNull(comparator, "comparator is required");
        _comparator = comparator;
    }

    public int size() {
        return _size;
    }

    public boolean isEmpty() {
        return _root == null;
    }

    /**
     * 查询树中是否包含指定key
     */
    public boolean containsKey(K key) {
        return search(key) != null;
    }

    /**
     * 查询树中最高的、指定key对应的值
     */
    @Nullable
    public V searchValue(K key) {
        Entry<K, V> entry = search(key);
        return entry != null ? entry.getValue() : null;
    }

    /**
     * 查询树中最高的、指定key对应的Entry
     */
    @Nullable
    public Entry<K, V> search(K key) {
        BinaryNode<K, V> binNode = searchBinaryNode(key);
        return binNode != null ? binNode.entry : null;
    }

    /**
     * 向树中插入一对键值对
     */
    public void insert(K key, @Nullable V value) {
        doInsert(key, value);
    }

    public void addAll(Collection<? extends K> c) {
        Assert.notNull(c, "collection is required");
        for (K key : c) {
            insert(key, null);
        }
    }

    public void addAll(Map<K, V> map) {
        Assert.notNull(map, "map is required");
        for (Map.Entry<K, V> e : map.entrySet()) {
            insert(e.getKey(), e.getValue());
        }
    }

    /**
     * 删除树中`最高`的、拥有指定key的节点
     */
    @Nullable
    public Entry<K, V> remove(K key) {
        return doRemove(key).first;
    }

    /**
     * 先序遍历
     */
    @SuppressWarnings("all")
    public void traversePreOrder(Consumer<Entry<K, V>> consumer) {
        if (isEmpty()) return;
        _root.traversePreOrder($0 -> consumer.accept($0.entry));
    }

    /**
     * 中序遍历
     */
    @SuppressWarnings("all")
    public void traverseInOrder(Consumer<Entry<K, V>> consumer) {
        if (isEmpty()) return;
        _root.traverseInOrder($0 -> consumer.accept($0.entry));
    }

    /**
     * 后序遍历
     */
    @SuppressWarnings("all")
    public void traversePostOrder(Consumer<Entry<K, V>> consumer) {
        if (isEmpty()) return;
        _root.traversePostOrder($0 -> consumer.accept($0.entry));
    }

    /**
     * 层次遍历
     */
    @SuppressWarnings("all")
    public void traverseLevel(Consumer<Entry<K, V>> consumer) {
        if (isEmpty()) return;
        _root.traverseLevel($0 -> consumer.accept($0.entry));
    }

    /**
     * 中序遍历次序的迭代器
     */
    @Override
    public Iterator<Entry<K, V>> iterator() {
        return new Iter();
    }

    @SuppressWarnings("all")
    protected BinaryNode<K, V> findInsertionPoint(K key) {
        BinaryNode<K, V> hot = null;
        BinaryNode<K, V> cur = _root;
        while (cur != null) {
            hot = cur;
            if (Comparators.compare(key, cur.entry.getKey(), _comparator) >= 0) {
                cur = cur.right;
            } else {
                cur = cur.left;
            }
        }
        return hot;
    }

    @Nullable
    @VisibleForTesting
    BinaryNode<K, V> searchBinaryNode(K key) {
        Assert.notNull(key, "key is required");
        BinaryNode<K, V> cur = _root;
        while (cur != null) {
            int order = Comparators.compare(key, cur.entry.getKey(), _comparator);
            if (order == 0) {
                return cur;
            } else if (order > 0) {
                cur = cur.right;
            } else {
                cur = cur.left;
            }
        }
        return null;
    }

    /**
     * 插入一对键值对，返回新插入的节点
     */
    protected BinaryNode<K, V> doInsert(K key, @Nullable V value) {
        Assert.notNull(key, "key is required");
        BinaryNode<K, V> node;
        if (isEmpty()) {
            node = newBinaryNode(key, value, null);
            _root = node;
        } else {
            BinaryNode<K, V> hot = findInsertionPoint(key);
            node = newBinaryNode(key, value, hot);
            if (Comparators.compare(key, hot.entry.getKey(), _comparator) >= 0) {
                hot.right = node;
            } else {
                hot.left = node;
            }
        }
        // 红黑树的高度需要从node开始更新
        node.updateHeightAbove(); // before -> hot.updateHeightAbove()
        _size += 1;
        return node;
    }

    @SuppressWarnings("all")
    protected Tuple<Entry<K, V>, BinaryNode<K, V>> doRemove(K key) {
        BinaryNode<K, V> node = searchBinaryNode(key);
        if (node == null) return Tuple.empty();
        BinaryNode<K, V> replacement = null;
        if (node.hasBothChildren()) {
            BinaryNode<K, V> next = node.successor();
            if (next != null) {
                // 交换entry
                node.entry.setKey(next.entry.getKey());
                node.entry.setValue(next.entry.getValue());
                // 退化成只有一个孩子的平凡情况
                node = next;
                replacement = next.right;
            }
        } else {
            // 只有一个孩子的情况下
            // 直接由孩子节点顶替即可
            if (node.hasLeftChild()) {
                replacement = node.left;
            } else {
                replacement = node.right;
            }
        }
        // 记录下可能失衡的节点
        BinaryNode<K, V> hot = node.parent;
        // 重新关联父节点，如果后继存在的话
        if (replacement != null) {
            replacement.parent = hot;
        }
        // 如果被删除的是根节点
        if (node.isRoot()) {
            // replacement就成为新的根节点
            _root = replacement;
        } else {
            // 否则replacement顶替其父节点的位置
            if (node.isLeftChild()) {
                node.parent.left = replacement;
            } else {
                node.parent.right = replacement;
            }
            node.parent.updateHeightAbove();
        }
        // 规模递减
        _size -= 1;
        // 清空node，方便GC
        node.left = null;
        node.right = null;
        node.parent = null;
        // 返回被删除的entry和可能失衡的节点
        return new Tuple<>(node.entry, hot);
    }

    protected BinaryNode<K, V> newBinaryNode(K key, @Nullable V value, @Nullable BinaryNode<K, V> parent) {
        return new BinaryNode<>(key, value, parent);
    }

    private class Iter implements Iterator<Entry<K, V>> {

        @Nullable
        private BinaryNode<K, V> cur;

        @SuppressWarnings("all")
        private Iter() {
            if (isEmpty()) {
                cur = null;
            } else {
                cur = _root.minimum();
            }
        }

        @Override
        public boolean hasNext() {
            return cur != null;
        }

        @Override
        @SuppressWarnings("all")
        public Entry<K, V> next() {
            BinaryNode<K, V> next = cur.successor();
            Entry<K, V> ret = cur.entry;
            cur = next;
            return ret;
        }
    }

    @Override
    public String toString() {
        return _root == null ? "(empty tree)" : _root.toString();
    }

}
