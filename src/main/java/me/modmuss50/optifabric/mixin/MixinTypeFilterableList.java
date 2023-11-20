package me.modmuss50.optifabric.mixin;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterators;
import net.minecraft.util.TypeInstanceMultiMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TypeInstanceMultiMap.class)
public abstract class MixinTypeFilterableList<T> {


	@Shadow
	@Final
	private Map<Class<?>, List<T>> delegate;

	@Shadow
	protected abstract Class<?> findOrThrow(Class<?> var1);

	@Shadow
	@Final
	private List<T> instances;

	/**
	 * @author hydos
	 */
	@Overwrite
	public <S> Iterable<S> find(final Class<S> var1) {
		return () -> {
			List<T> list = delegate.get(findOrThrow(var1));
			if (list == null) {
				return Collections.emptyIterator();
			} else {
				Iterator<T> iterator = list.iterator();
				return Iterators.filter(iterator, var1);
			}
		};
	}

	/**
	 * @author hydos
	 */
	@Overwrite
	public Iterator<T> iterator() {
		return instances.isEmpty() ? Collections.emptyIterator() : Iterators.unmodifiableIterator(instances.iterator());
	}

}
