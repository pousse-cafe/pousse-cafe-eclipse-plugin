package poussecafe.eclipse.plugin.editors;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.swt.graphics.Resource;

public class ResourceMap<K, V extends Resource> {

    public ResourceMap(Function<K, V> resourceBuilder) {
        this.resourceBuilder = resourceBuilder;
    }

    private Map<K, V> map = new HashMap<>(10);
    
    private Function<K, V> resourceBuilder;

    public void dispose() {
        map.values().forEach(V::dispose);
        map.clear();
    }

    public V get(K key) {
        return map.computeIfAbsent(key, this.resourceBuilder);
    }
}
